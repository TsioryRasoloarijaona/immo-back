package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.FonctionTotal;
import com.tsix.immocrm.domain.rapport.GraphiqueRapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.domain.rapport.TypeColonne;
import com.tsix.immocrm.domain.rapport.TypeGraphique;
import com.tsix.immocrm.services.rapport.vente.flux.FluxMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.FluxVenteMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.LignesMensuelles;
import com.tsix.immocrm.services.rapport.vente.flux.MesureFlux;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.tsix.immocrm.services.rapport.vente.flux.LignesMensuelles.ANNEE;
import static com.tsix.immocrm.services.rapport.vente.flux.LignesMensuelles.MOIS;

/**
 * N° 5 Objectifs (ENC, CONC, CH) PAR MOIS (FEATURE-tdb-vente-rapports-flux.md §4.1), modèle {@code ComTdbActiviteParMois},
 * sans la section {@code situation} (R1b). Les libellés reprennent les colonnes de la vue que le modèle affiche vraiment.
 */
@Component
public class ActiviteParMoisRapport extends RapportFluxVente {

    static final String CODE = "ComTdbActiviteParMois";
    private static final String PREVU = "Prevu";
    private static final String REEL = "Reel";
    private static final String CUMUL = "Cumul";
    private static final String FRAIS_DOSSIER = "fraisDossier";
    private static final String FRAIS_TRANSFERT = "fraisTransfert";
    private static final String FRAIS_DESISTEMENT = "fraisDesistement";

    /** Un groupe d'en-tête : préfixe des colonnes, mesure réelle, mesure prévue ; quantité = nombre entier. */
    private record Groupe(String prefixe, String libelle, MesureFlux reel, MesureFlux prevu, boolean quantite) {
    }

    private static final List<Groupe> GROUPES = List.of(
            new Groupe("concretisationQuantite", "Concrétisation quantité", MesureFlux.CONCRETISATIONS_NETTES,
                    MesureFlux.PREVU_CONCRETISATION_QUANTITE, true),
            new Groupe("concretisationMontant", "Concrétisation montant (avances encaissées)",
                    MesureFlux.AVANCES_ENCAISSEES, MesureFlux.PREVU_CONCRETISATION_MONTANT, false),
            new Groupe("caTtc", "C.A. TTC", MesureFlux.CA_TTC, MesureFlux.PREVU_CA_TTC, false),
            new Groupe("finalisationQuantite", "Finalisation quantité (livraisons)", MesureFlux.LIVRAISONS,
                    MesureFlux.PREVU_CA_QUANTITE, true),
            new Groupe("encaissements", "Encaissements (*)", MesureFlux.ENCAISSEMENTS, MesureFlux.PREVU_ENCAISSEMENT,
                    false));

    private static final Map<String, MesureFlux> FRAIS = Map.of(FRAIS_DOSSIER, MesureFlux.ENCAISSEMENTS_FRAIS_DOSSIER,
            FRAIS_TRANSFERT, MesureFlux.ENCAISSEMENTS_FRAIS_TRANSFERT, FRAIS_DESISTEMENT,
            MesureFlux.FRAIS_DESISTEMENT_DUS);

    public ActiviteParMoisRapport(FluxVenteMensuel flux, Clock clock) {
        super(CODE, flux, clock);
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        int annee = contexte.annee(ANNEE);
        Set<MesureFlux> mesures = EnumSet.copyOf(FRAIS.values());
        GROUPES.forEach(g -> mesures.addAll(List.of(g.reel(), g.prevu())));
        FluxMensuel f = calculer(contexte, annee, annee, mesures);
        List<SectionRapport> sections = List.of(mensuel(f, annee), cumuls(f, annee),
                aCeJour(f, annee, LocalDate.now(contexte.clock())), frais(f, annee),
                remises(contexte.tranchesVisibles()));
        return new ResultatRapport(sections, LignesMensuelles.avertissements(f));
    }

    private static SectionRapport mensuel(FluxMensuel f, int annee) {
        List<ColonneRapport> colonnes = new ArrayList<>(List.of(ColonneRapport.texte(MOIS, "Mois")));
        List<GraphiqueRapport> graphiques = new ArrayList<>();
        for (Groupe g : GROUPES) {
            colonnes.add(colonne(g, PREVU, "Prévu", FonctionTotal.SOMME));
            colonnes.add(colonne(g, REEL, "Réel", FonctionTotal.SOMME));
            graphiques.add(new GraphiqueRapport(TypeGraphique.COLONNES, g.libelle(), MOIS,
                    List.of(g.prefixe() + PREVU, g.prefixe() + REEL)));
        }
        List<Map<String, Object>> lignes = LignesMensuelles.douzeMois((ligne, mois) -> GROUPES.forEach(g -> {
            ligne.put(g.prefixe() + PREVU, f.mois(g.prevu(), annee, mois));
            ligne.put(g.prefixe() + REEL, f.mois(g.reel(), annee, mois));
        }));
        return new SectionRapport("mensuel", "Objectifs commerciaux " + annee, colonnes, lignes, null, null,
                graphiques);
    }

    /** Cumul progressif de janvier au mois ; aucun total (un cumul ne s'additionne pas). */
    private static SectionRapport cumuls(FluxMensuel f, int annee) {
        List<ColonneRapport> colonnes = new ArrayList<>(List.of(ColonneRapport.texte(MOIS, "Mois")));
        List<GraphiqueRapport> graphiques = new ArrayList<>();
        for (Groupe g : GROUPES) {
            colonnes.add(colonne(g, CUMUL + PREVU, "Cumul prévu", FonctionTotal.AUCUNE));
            colonnes.add(colonne(g, CUMUL + REEL, "Cumul réel", FonctionTotal.AUCUNE));
            graphiques.add(new GraphiqueRapport(TypeGraphique.LIGNES, g.libelle() + " (cumul)", MOIS,
                    List.of(g.prefixe() + CUMUL + PREVU, g.prefixe() + CUMUL + REEL)));
        }
        Map<String, BigDecimal> cumul = new LinkedHashMap<>();
        List<Map<String, Object>> lignes = LignesMensuelles.douzeMois((ligne, mois) -> GROUPES.forEach(g -> {
            ligne.put(g.prefixe() + CUMUL + PREVU, cumul.merge(g.prefixe() + PREVU, f.mois(g.prevu(), annee, mois),
                    BigDecimal::add));
            ligne.put(g.prefixe() + CUMUL + REEL, cumul.merge(g.prefixe() + REEL, f.mois(g.reel(), annee, mois),
                    BigDecimal::add));
        }));
        return new SectionRapport("cumuls", "Cumuls " + annee, colonnes, lignes, null, null, graphiques);
    }

    private static SectionRapport aCeJour(FluxMensuel f, int annee, LocalDate aujourdhui) {
        List<Map<String, Object>> lignes = new ArrayList<>();
        for (Groupe g : GROUPES) {
            BigDecimal prevu = prevuACeJour(f, g, annee, aujourdhui);
            BigDecimal reel = f.annee(g.reel(), annee);
            Map<String, Object> ligne = new LinkedHashMap<>();
            ligne.put("mesure", g.libelle());
            ligne.put("prevuACeJour", prevu);
            ligne.put("reel", reel);
            ligne.put("taux", LignesMensuelles.taux(reel, prevu));
            lignes.add(ligne);
        }
        List<ColonneRapport> colonnes = List.of(ColonneRapport.texte("mesure", "Mesure"),
                new ColonneRapport("prevuACeJour", "Prévu à ce jour", TypeColonne.NOMBRE, null, FonctionTotal.AUCUNE,
                        true, 2),
                new ColonneRapport("reel", "Réel", TypeColonne.NOMBRE, null, FonctionTotal.AUCUNE, true, 2),
                ColonneRapport.pourcentage("taux", "Taux de réalisation", null));
        return new SectionRapport("a-ce-jour", "Cumul à ce jour", colonnes, lignes, null, null, List.of());
    }

    /**
     * Année en cours : prévu des mois passés + prévu du mois courant au prorata des jours écoulés (entier pour une
     * quantité) ; autre année : prévu de l'année.
     */
    private static BigDecimal prevuACeJour(FluxMensuel f, Groupe g, int annee, LocalDate aujourdhui) {
        if (annee != aujourdhui.getYear()) {
            return f.annee(g.prevu(), annee);
        }
        BigDecimal prevu = BigDecimal.ZERO;
        for (int mois = 1; mois < aujourdhui.getMonthValue(); mois++) {
            prevu = prevu.add(f.mois(g.prevu(), annee, mois));
        }
        prevu = prevu.add(f.mois(g.prevu(), annee, aujourdhui.getMonthValue())
                .multiply(BigDecimal.valueOf(aujourdhui.getDayOfMonth()))
                .divide(BigDecimal.valueOf(aujourdhui.lengthOfMonth()), 6, RoundingMode.HALF_UP));
        return g.quantite() ? prevu.setScale(0, RoundingMode.DOWN) : prevu;
    }

    private static SectionRapport frais(FluxMensuel f, int annee) {
        List<Map<String, Object>> lignes = LignesMensuelles.douzeMois((ligne, mois) ->
                FRAIS.forEach((nom, m) -> ligne.put(nom, f.mois(m, annee, mois))));
        List<ColonneRapport> colonnes = List.of(ColonneRapport.texte(MOIS, "Mois"),
                ColonneRapport.montant(FRAIS_DOSSIER, "Frais de dossier", null),
                ColonneRapport.montant(FRAIS_TRANSFERT, "Frais de transfert", null),
                ColonneRapport.montant(FRAIS_DESISTEMENT, "Frais de désistement dus", null));
        return new SectionRapport("frais", "Frais encaissés (*) — sans tenir compte des pièces non comptabilisées",
                colonnes, lignes, null, null, List.of());
    }

    private SectionRapport remises(List<UUID> tranches) {
        Map<String, Object> ligne = new LinkedHashMap<>();
        ligne.put("totalRemises", flux.remises(tranches));
        return new SectionRapport("remises", "Remises (dossiers ni annulés ni désistés, toutes dates)",
                List.of(ColonneRapport.montant("totalRemises", "Total des remises", null)), List.of(ligne), null, null,
                List.of());
    }

    private static ColonneRapport colonne(Groupe g, String suffixe, String libelle, FonctionTotal total) {
        return new ColonneRapport(g.prefixe() + suffixe, libelle, g.quantite() ? TypeColonne.NOMBRE : TypeColonne.MONTANT,
                g.libelle(), total, true, 0);
    }
}

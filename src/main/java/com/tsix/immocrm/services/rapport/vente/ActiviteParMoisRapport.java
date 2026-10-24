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
import com.tsix.immocrm.services.rapport.vente.flux.ValorisationTranche;
import com.tsix.immocrm.services.rapport.vente.flux.ValorisationTranche.Population;
import com.tsix.immocrm.services.rapport.vente.flux.ValorisationTranche.Valorisation;
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

import static com.tsix.immocrm.services.rapport.vente.flux.LignesMensuelles.MOIS;

/**
 * N° 5 Objectifs (ENC, CONC, CH) PAR MOIS (FEATURE-tdb-vente-rapports-flux.md §4.1), modèle {@code ComTdbActiviteParMois}.
 * Les libellés reprennent les colonnes de la vue que le modèle affiche vraiment.
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

    private static final String BASE = "base";
    private static final String REALISE_AVANT = "realiseAvant";
    private static final String REALISE_ANNEE_MOINS_1 = "realiseAnneeMoins1";
    private static final String REALISE_ANNEE = "realiseAnnee";
    private static final String TOTAL_REALISE = "totalRealise";
    private static final String RESTE = "reste";
    private static final String TAUX_REALISE = "tauxRealise";
    private static final String MESURE = "mesure";

    /** Lignes « réalisé » de la section {@code situation} : base = articles (quantité) ou valorisation (montant). */
    private record Realise(String libelle, MesureFlux mesure, boolean quantite) {
    }

    private static final List<Realise> REALISES = List.of(
            new Realise("Concrétisations nettes", MesureFlux.CONCRETISATIONS_NETTES, true),
            new Realise("Livraisons", MesureFlux.LIVRAISONS, true),
            new Realise("CA TTC", MesureFlux.CA_TTC, false),
            new Realise("Encaissements", MesureFlux.ENCAISSEMENTS, false));

    private final ValorisationTranche valorisation;

    public ActiviteParMoisRapport(FluxVenteMensuel flux, ValorisationTranche valorisation, Clock clock) {
        super(CODE, flux, clock);
        this.valorisation = valorisation;
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        int annee = annee(contexte);
        Set<MesureFlux> mesures = EnumSet.copyOf(FRAIS.values());
        GROUPES.forEach(g -> mesures.addAll(List.of(g.reel(), g.prevu())));
        FluxMensuel f = calculer(contexte, annee, annee, mesures);
        List<SectionRapport> sections = List.of(mensuel(f, annee), cumuls(f, annee),
                aCeJour(f, annee, LocalDate.now(contexte.clock())), situation(contexte, f, annee), frais(f, annee),
                remises(contexte.tranchesVisibles()));
        return new ResultatRapport(sections, LignesMensuelles.avertissements(f));
    }

    /** État du stock : réalisé avant N, en N−1 (Q8 : l'année N−1 seule) et en N ; reste = base − avant − N. */
    private SectionRapport situation(ContexteRapport contexte, FluxMensuel f, int annee) {
        List<Valorisation> stock = valorisation.pour(contexte.tranchesVisibles()).tranches();
        BigDecimal articles = BigDecimal.valueOf(stock.stream().mapToLong(Valorisation::articles).sum());
        BigDecimal valeur = stock.stream().map(Valorisation::prixTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        Set<MesureFlux> mesures = EnumSet.noneOf(MesureFlux.class);
        REALISES.forEach(r -> mesures.add(r.mesure()));
        FluxMensuel avant = calculer(contexte, null, annee - 1, mesures);
        List<Map<String, Object>> lignes = new ArrayList<>();
        for (Realise r : REALISES) {
            BigDecimal base = r.quantite() ? articles : valeur;
            BigDecimal anterieur = avant.total(r.mesure());
            BigDecimal courant = f.annee(r.mesure(), annee);
            BigDecimal total = anterieur.add(courant);
            lignes.add(ligneSituation(r.libelle(), base, List.of(anterieur, avant.annee(r.mesure(), annee - 1), courant,
                    total, base.subtract(total)), LignesMensuelles.taux(total, base)));
        }
        BigDecimal resteARegler = stock.stream().map(v -> v.de(Population.VIVANTS).reste())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // Le reste à régler n'a pas de « réalisé » ; taux 0 si base nulle, comme les autres lignes.
        lignes.add(ligneSituation("Reste à régler", resteARegler, null,
                resteARegler.signum() == 0 ? BigDecimal.ZERO : null));
        List<ColonneRapport> colonnes = new ArrayList<>(List.of(ColonneRapport.texte(MESURE, "Mesure"),
                nombre(BASE, "Base (articles ou valorisation)")));
        colonnes.addAll(List.of(nombre(REALISE_AVANT, "Réalisé avant " + annee),
                nombre(REALISE_ANNEE_MOINS_1, "Réalisé " + (annee - 1)), nombre(REALISE_ANNEE, "Réalisé " + annee),
                nombre(TOTAL_REALISE, "Total réalisé"), nombre(RESTE, "Reste"),
                ColonneRapport.pourcentage(TAUX_REALISE, "Taux de réalisation", null)));
        return new SectionRapport("situation", "Situation du stock", colonnes, lignes, null, null, List.of());
    }

    /** {@code realises} = avant, N−1, N, total, reste (ou {@code null} : colonnes vides). */
    private static Map<String, Object> ligneSituation(String mesure, BigDecimal base, List<BigDecimal> realises,
                                                      BigDecimal taux) {
        Map<String, Object> ligne = new LinkedHashMap<>();
        ligne.put(MESURE, mesure);
        ligne.put(BASE, base);
        List<String> noms = List.of(REALISE_AVANT, REALISE_ANNEE_MOINS_1, REALISE_ANNEE, TOTAL_REALISE, RESTE);
        for (int i = 0; i < noms.size(); i++) {
            ligne.put(noms.get(i), realises == null ? null : realises.get(i));
        }
        ligne.put(TAUX_REALISE, taux);
        return ligne;
    }

    private static ColonneRapport nombre(String nom, String libelle) {
        return new ColonneRapport(nom, libelle, TypeColonne.NOMBRE, null, FonctionTotal.AUCUNE, true, 2);
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
            ligne.put(MESURE, g.libelle());
            ligne.put("prevuACeJour", prevu);
            ligne.put("reel", reel);
            ligne.put("taux", LignesMensuelles.taux(reel, prevu));
            lignes.add(ligne);
        }
        List<ColonneRapport> colonnes = List.of(ColonneRapport.texte(MESURE, "Mesure"),
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

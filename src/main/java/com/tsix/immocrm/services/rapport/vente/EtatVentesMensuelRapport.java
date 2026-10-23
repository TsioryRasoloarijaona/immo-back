package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.ContexteRapport;
import com.tsix.immocrm.domain.rapport.GraphiqueRapport;
import com.tsix.immocrm.domain.rapport.ResultatRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.domain.rapport.TypeGraphique;
import com.tsix.immocrm.services.rapport.vente.flux.FluxMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.FluxVenteMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.LignesMensuelles;
import com.tsix.immocrm.services.rapport.vente.flux.MesureFlux;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tsix.immocrm.services.rapport.vente.flux.LignesMensuelles.ANNEE;

/**
 * N° 14 ÉTAT DES VENTE (FEATURE-tdb-vente-rapports-flux.md §4.3), modèle {@code ComTdbSalesState} : six tableaux années ×
 * mois sur la fenêtre {@code annee − 5 … annee}.
 */
@Component
public class EtatVentesMensuelRapport extends RapportFluxVente {

    static final String CODE = "ComTdbSalesState";
    private static final String TOTAL = "total";
    private static final int FENETRE = 5;

    private record Mesure(String id, String titre, MesureFlux mesure, boolean montant) {
    }

    private static final List<Mesure> MESURES = List.of(
            new Mesure("concretisations", "État des ventes : concrétisations (dont annulées)", MesureFlux.CONCRETISATIONS,
                    false),
            new Mesure("chiffre-affaires", "État des ventes : chiffre d'affaires TTC", MesureFlux.CA_TTC, true),
            new Mesure("livraisons", "État des ventes : livraisons", MesureFlux.LIVRAISONS, false),
            new Mesure("encaissements", "État des ventes : encaissements (*) — sans tenir compte des frais et des "
                    + "pièces non comptabilisées", MesureFlux.ENCAISSEMENTS, true),
            new Mesure("annulations", "État des ventes : annulations", MesureFlux.ANNULATIONS, false),
            new Mesure("desistements", "État des ventes : désistements", MesureFlux.DESISTEMENTS, false));

    public EtatVentesMensuelRapport(FluxVenteMensuel flux, Clock clock) {
        super(CODE, flux, clock);
    }

    @Override
    public ResultatRapport executer(ContexteRapport contexte) {
        int fin = contexte.annee(ANNEE);
        FluxMensuel f = calculer(contexte, fin - FENETRE, fin,
                MESURES.stream().map(Mesure::mesure).collect(Collectors.toSet()));
        List<SectionRapport> sections = MESURES.stream().map(m -> section(m, f, fin)).toList();
        return new ResultatRapport(sections, LignesMensuelles.avertissements(f));
    }

    private static SectionRapport section(Mesure m, FluxMensuel f, int fin) {
        List<Map<String, Object>> lignes = new ArrayList<>();
        for (int annee = fin - FENETRE; annee <= fin; annee++) {
            Map<String, Object> ligne = new LinkedHashMap<>();
            ligne.put(ANNEE, String.valueOf(annee));
            for (int mois = 1; mois <= 12; mois++) {
                ligne.put(colonneMois(mois), f.mois(m.mesure(), annee, mois));
            }
            ligne.put(TOTAL, f.annee(m.mesure(), annee));
            lignes.add(ligne);
        }
        List<ColonneRapport> colonnes = new ArrayList<>(List.of(ColonneRapport.texte(ANNEE, "Année")));
        for (int mois = 1; mois <= 12; mois++) {
            colonnes.add(chiffre(m, colonneMois(mois), LignesMensuelles.libelleMois(mois)));
        }
        colonnes.add(chiffre(m, TOTAL, "Total"));
        return new SectionRapport(m.id(), m.titre(), colonnes, lignes, null, null,
                List.of(new GraphiqueRapport(TypeGraphique.COLONNES, m.titre(), ANNEE, List.of(TOTAL))));
    }

    private static ColonneRapport chiffre(Mesure m, String nom, String libelle) {
        return m.montant() ? ColonneRapport.montant(nom, libelle, null) : ColonneRapport.effectif(nom, libelle, null);
    }

    private static String colonneMois(int mois) {
        return "m%02d".formatted(mois);
    }
}

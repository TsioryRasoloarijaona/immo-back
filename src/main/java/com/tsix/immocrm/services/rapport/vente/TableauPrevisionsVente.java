package com.tsix.immocrm.services.rapport.vente;

import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.FonctionTotal;
import com.tsix.immocrm.domain.rapport.GraphiqueRapport;
import com.tsix.immocrm.domain.rapport.SectionRapport;
import com.tsix.immocrm.domain.rapport.TypeColonne;
import com.tsix.immocrm.domain.rapport.TypeGraphique;
import com.tsix.immocrm.services.rapport.vente.flux.FluxMensuel;
import com.tsix.immocrm.services.rapport.vente.flux.LignesMensuelles;
import com.tsix.immocrm.services.rapport.vente.flux.MesureFlux;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Gabarit de 12 mois prévu / réel (FEATURE-tdb-vente-previsions.md §3.4) de l'écran et du n° 18 : lit un flux déjà
 * calculé, n'ajoute que cumuls et pourcentages ({@code null} quand le dénominateur vaut 0).
 */
final class TableauPrevisionsVente {

    static final String SCHEMA = "schemaFinancement";
    static final String PREVU = "prevu";
    static final String REEL = "reel";
    static final String CUMUL_PREVU = "cumulPrevu";
    static final String CUMUL_REEL = "cumulReel";
    static final String POURCENTAGE_REALISE = "pourcentageRealise";
    static final String POURCENTAGE_CUMULE = "pourcentageCumule";
    static final String RENTABILITE = "rentabilite";
    private static final BigDecimal CENT = BigDecimal.valueOf(100);

    /** Mesures de prévision d'une section : base, ancien prévu (dernière version), prévu. */
    record Prevus(MesureFlux base, MesureFlux ancien, MesureFlux prevu) {
    }

    /** Une section ; {@code reel} et {@code schema} sont des sommes de mesures, {@code schema} vide = sans colonne. */
    record Definition(String id, String titre, TypeColonne unite, Prevus prevus, String libelleReel,
                      List<MesureFlux> reel, List<MesureFlux> schema) {
    }

    private TableauPrevisionsVente() {
    }

    static Set<MesureFlux> mesures(Definition... definitions) {
        Set<MesureFlux> mesures = EnumSet.noneOf(MesureFlux.class);
        for (Definition d : definitions) {
            mesures.addAll(List.of(d.prevus().base(), d.prevus().ancien(), d.prevus().prevu()));
            mesures.addAll(d.reel());
            mesures.addAll(d.schema());
        }
        return mesures;
    }

    static GraphiqueRapport graphique(TypeGraphique type, String titre, String... colonnes) {
        return new GraphiqueRapport(type, titre, LignesMensuelles.MOIS, List.of(colonnes));
    }

    /** Section de douze lignes ; graphiques « Prévu / réel », « Cumuls », puis {@code suivants}. */
    static SectionRapport section(Definition d, FluxMensuel flux, int annee, List<GraphiqueRapport> suivants) {
        BigDecimal[] schema = serie(flux, annee, d.schema());
        BigDecimal[] base = serie(flux, annee, List.of(d.prevus().base()));
        BigDecimal[] ancien = serie(flux, annee, List.of(d.prevus().ancien()));
        BigDecimal[] prevu = serie(flux, annee, List.of(d.prevus().prevu()));
        BigDecimal[] reel = serie(flux, annee, d.reel());
        BigDecimal[] cumulPrevu = cumul(prevu);
        BigDecimal[] cumulReel = cumul(reel);
        List<Map<String, Object>> lignes = LignesMensuelles.douzeMois((ligne, mois) -> {
            int i = mois - 1;
            if (!d.schema().isEmpty()) {
                ligne.put(SCHEMA, schema[i]);
            }
            ligne.put("base", base[i]);
            ligne.put("ancienPrevu", ancien[i]);
            ligne.put(PREVU, prevu[i]);
            ligne.put(REEL, reel[i]);
            ligne.put(CUMUL_PREVU, cumulPrevu[i]);
            ligne.put(CUMUL_REEL, cumulReel[i]);
            ligne.put(POURCENTAGE_REALISE, taux(reel[i], prevu[i]));
            ligne.put(POURCENTAGE_CUMULE, taux(cumulReel[i], cumulPrevu[i]));
            ligne.put(RENTABILITE, taux(reel[i], cumulReel[11]));
        });
        List<GraphiqueRapport> graphiques = Stream.concat(Stream.of(
                graphique(TypeGraphique.COLONNES, "Prévu / réel", PREVU, REEL),
                graphique(TypeGraphique.LIGNES, "Cumuls", CUMUL_PREVU, CUMUL_REEL)), suivants.stream()).toList();
        return new SectionRapport(d.id(), d.titre(), colonnes(d), lignes, null, null, graphiques);
    }

    private static List<ColonneRapport> colonnes(Definition d) {
        List<ColonneRapport> colonnes = new ArrayList<>(List.of(ColonneRapport.texte(LignesMensuelles.MOIS, "Mois")));
        if (!d.schema().isEmpty()) {
            colonnes.add(ColonneRapport.montant(SCHEMA, "Schéma de financement", null));
        }
        colonnes.add(valeur(d, "base", "Prévu de base", FonctionTotal.SOMME));
        colonnes.add(valeur(d, "ancienPrevu", "Ancien prévu", FonctionTotal.SOMME));
        colonnes.add(valeur(d, PREVU, "Prévu", FonctionTotal.SOMME));
        colonnes.add(valeur(d, REEL, d.libelleReel(), FonctionTotal.SOMME));
        colonnes.add(valeur(d, CUMUL_PREVU, "Cumul prévu", FonctionTotal.AUCUNE));
        colonnes.add(valeur(d, CUMUL_REEL, "Cumul réel", FonctionTotal.AUCUNE));
        colonnes.add(ColonneRapport.pourcentage(POURCENTAGE_REALISE, "% réalisé", null));
        colonnes.add(ColonneRapport.pourcentage(POURCENTAGE_CUMULE, "% cumulé", null));
        colonnes.add(ColonneRapport.pourcentage(RENTABILITE, "Rentabilité", null));
        return colonnes;
    }

    private static ColonneRapport valeur(Definition d, String nom, String libelle, FonctionTotal total) {
        return new ColonneRapport(nom, libelle, d.unite(), null, total, true, 0);
    }

    private static BigDecimal[] serie(FluxMensuel flux, int annee, List<MesureFlux> mesures) {
        BigDecimal[] serie = new BigDecimal[12];
        for (int mois = 1; mois <= 12; mois++) {
            int m = mois;
            serie[mois - 1] = mesures.stream().map(x -> flux.mois(x, annee, m)).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return serie;
    }

    private static BigDecimal[] cumul(BigDecimal[] serie) {
        BigDecimal[] cumul = new BigDecimal[serie.length];
        BigDecimal somme = BigDecimal.ZERO;
        for (int i = 0; i < serie.length; i++) {
            somme = somme.add(serie[i]);
            cumul[i] = somme;
        }
        return cumul;
    }

    /** {@code part ÷ base × 100}, 2 décimales ; {@code null} si la base vaut 0 (non calculable, décision Q1). */
    static BigDecimal taux(BigDecimal part, BigDecimal base) {
        return base.signum() == 0 ? null : part.multiply(CENT).divide(base, 2, RoundingMode.HALF_UP);
    }
}

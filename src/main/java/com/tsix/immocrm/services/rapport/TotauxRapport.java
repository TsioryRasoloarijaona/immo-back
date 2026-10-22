package com.tsix.immocrm.services.rapport;

import com.tsix.immocrm.domain.rapport.ColonneRapport;
import com.tsix.immocrm.domain.rapport.FonctionTotal;
import com.tsix.immocrm.domain.rapport.SectionRapport;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Totaux et sous-totaux d'une section, en décimal exact (FEATURE-tdb-vente-rapports-socle.md §4.3) : seul calcul. */
public final class TotauxRapport {

    private TotauxRapport() {
    }

    /** Vide si la section n'a aucune ligne ou aucun total (null sur le fil) ; les valeurs imposées l'emportent. */
    public static Map<String, Object> totaux(SectionRapport section) {
        if (section.lignes().isEmpty()) {
            return Map.of();
        }
        Map<String, Object> totaux = calculer(section.colonnes(), section.lignes());
        if (section.totaux() != null) {
            totaux.putAll(section.totaux());
        }
        return totaux;
    }

    /** Une entrée par valeur de {@code groupePar}, dans l'ordre des lignes ; vide sans regroupement. */
    public static Map<String, Map<String, Object>> sousTotaux(SectionRapport section) {
        if (section.groupePar() == null) {
            return Map.of();
        }
        Map<String, List<Map<String, Object>>> groupes = new LinkedHashMap<>();
        for (Map<String, Object> ligne : section.lignes()) {
            groupes.computeIfAbsent(String.valueOf(ligne.get(section.groupePar())), g -> new ArrayList<>())
                    .add(ligne);
        }
        Map<String, Map<String, Object>> sousTotaux = new LinkedHashMap<>();
        groupes.forEach((groupe, lignes) -> sousTotaux.put(groupe, calculer(section.colonnes(), lignes)));
        return sousTotaux;
    }

    private static Map<String, Object> calculer(List<ColonneRapport> colonnes, List<Map<String, Object>> lignes) {
        Map<String, Object> totaux = new LinkedHashMap<>();
        for (ColonneRapport colonne : colonnes) {
            if (colonne.fonctionTotal() != FonctionTotal.AUCUNE) {
                List<BigDecimal> valeurs = lignes.stream().map(l -> decimal(l.get(colonne.nom())))
                        .filter(Objects::nonNull).toList();
                totaux.put(colonne.nom(), agreger(colonne.fonctionTotal(), valeurs));
            }
        }
        return totaux;
    }

    static BigDecimal agreger(FonctionTotal fonction, List<BigDecimal> valeurs) {
        BigDecimal somme = valeurs.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return switch (fonction) {
            case SOMME -> somme;
            case MOYENNE -> valeurs.isEmpty() ? null
                    : somme.divide(BigDecimal.valueOf(valeurs.size()), MathContext.DECIMAL64).stripTrailingZeros();
            case MIN -> valeurs.stream().min(BigDecimal::compareTo).orElse(null);
            case MAX -> valeurs.stream().max(BigDecimal::compareTo).orElse(null);
            case AUCUNE -> null;
        };
    }

    private static BigDecimal decimal(Object valeur) {
        if (valeur instanceof BigDecimal b) {
            return b;
        }
        return valeur instanceof Number n ? new BigDecimal(n.toString()) : null;
    }
}

package com.tsix.immocrm.domain;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * « Générer » des prévisions de base : le stock réparti en cloche sur les mois de chaque période
 * ({@code ProjectPrevision.GetGaussGraph(…, Quantity)}, FEATURE-patrimoine-tranche-regles.md §3, §11.2-§11.3).
 */
public final class PrevisionsCloche {

    private static final String CONCRETISATION = "Concretisation";
    private static final String FINALISATION = "Finalisation";
    private static final String LES_DEUX = "Concrétisation et Finalisation";

    /** Période de concrétisation ou de finalisation, bornes comprises. */
    public record Periode(LocalDateTime debut, LocalDateTime fin, double ecartType) {
    }

    /** Ligne générée : un mois, ses deux quantités et la note OMRAN (« Concretisation » sans accent). */
    public record Mois(YearMonth mois, int concretisation, int finalisation, String notes) {
    }

    private PrevisionsCloche() {
    }

    public static List<Mois> generer(Periode concretisation, Periode finalisation, int quantite) {
        Map<YearMonth, Mois> lignes = new LinkedHashMap<>();
        int[] c = repartir(nombreDeMois(concretisation), concretisation.ecartType(), quantite);
        for (int k = 0; k < c.length; k++) {
            YearMonth m = YearMonth.from(concretisation.debut().plusMonths(k));
            lignes.put(m, new Mois(m, c[k], 0, CONCRETISATION));
        }
        int[] f = repartir(nombreDeMois(finalisation), finalisation.ecartType(), quantite);
        for (int k = 0; k < f.length; k++) {
            YearMonth m = YearMonth.from(finalisation.debut().plusMonths(k));
            Mois existant = lignes.get(m);
            lignes.put(m, existant == null ? new Mois(m, 0, f[k], FINALISATION)
                    : new Mois(m, existant.concretisation(), f[k], LES_DEUX));
        }
        return lignes.values().stream().filter(l -> l.concretisation() != 0 || l.finalisation() != 0).toList();
    }

    /** {@code Diff_Dates} : mois comptés par ajouts successifs d'un mois tant que la date ≤ fin. */
    static int nombreDeMois(Periode p) {
        int n = 0;
        for (LocalDateTime d = p.debut(); !d.isAfter(p.fin()); d = d.plusMonths(1)) {
            n++;
        }
        return n;
    }

    /** Arrondi au pair ({@code Math.Round} .NET = {@link Math#rint}), puis écart reporté autour du mois du milieu. */
    static int[] repartir(int n, double ecartType, int quantite) {
        if (n == 0 || quantite <= 0) {
            return new int[0];
        }
        double moyenne = (n - 1) / 2.0 + 1;
        double[] poids = new double[n];
        for (int i = 0; i < n; i++) {
            poids[i] = Math.exp(-Math.pow(i + 1 - moyenne, 2) / (2 * Math.pow(ecartType, 2)))
                    / (ecartType * Math.sqrt(2 * Math.PI));
        }
        double total = Arrays.stream(poids).sum();
        int[] v = new int[n];
        for (int i = 0; i < n; i++) {
            v[i] = (int) Math.rint(quantite * (poids[i] / total));
        }
        int milieu = (int) Math.rint((n - 1) / 2.0);
        int somme = Arrays.stream(v).sum();
        while (somme != quantite) {
            if (somme < quantite) {
                v[milieu] += quantite - somme;
            } else if (!retirerUnPassage(v, milieu, somme - quantite)) {
                v[indiceDuMax(v)]--;
            }
            somme = Arrays.stream(v).sum();
        }
        return v;
    }

    /** Un passage de la boucle de retrait d'OMRAN ; faux s'il n'a rien retiré (garde contre la boucle infinie). */
    private static boolean retirerUnPassage(int[] v, int milieu, int exces) {
        int restant = exces;
        for (int k = milieu; k < v.length && restant > 0; k++) {
            int cible = cibleDuRetrait(v, k);
            if (cible >= 0) {
                v[cible]--;
                restant--;
            }
        }
        return restant < exces;
    }

    /** Mois diminué au rang {@code k}, ou -1 ; le mois précédent du premier mois n'existe pas (OMRAN y plante). */
    private static int cibleDuRetrait(int[] v, int k) {
        int n = v.length;
        if (v[k] >= 1 && ((k > 0 && v[k] >= v[k - 1] + 1) || k + 1 == n)) {
            return k;
        }
        if (k + 1 < n) {
            return v[k + 1] >= 1 && k + 2 < n && v[k + 1] >= v[k + 2] + 1 ? k + 1 : -1;
        }
        return k - 2 > 0 && v[k - 1] >= 1 && v[k - 1] >= v[k - 2] + 1 ? k - 1 : -1;
    }

    private static int indiceDuMax(int[] v) {
        int max = 0;
        for (int i = 1; i < v.length; i++) {
            if (v[i] > v[max]) {
                max = i;
            }
        }
        return max;
    }
}

package com.tsix.immocrm.domain.faisabilite;

import com.tsix.immocrm.exceptions.InvalidRequestException;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Répartition mensuelle d'OMRAN ({@code omran-src/Yes.Rems/Yes.Stl/Utils.cs:GetGaussGraph}, l.1050-1285), en double
 * comme OMRAN puis arrondie à l'échelle 6 ; le reliquat d'arrondi va à la plus grande ligne (Σ exacte, invariant I6).
 */
public final class RepartitionGauss {

    private static final double RACINE_DEUX_PI = Math.sqrt(2 * Math.PI);
    private static final int ECHELLE = 6;

    private RepartitionGauss() {
    }

    /** {@code Diff_Dates} : mois k ≥ 0 tels que début + k mois ≤ fin, en ajoutant un mois à la date précédente. */
    public static int nombreDeMois(LocalDateTime debut, LocalDateTime fin) {
        if (debut == null || fin == null) {
            return 0;
        }
        int n = 0;
        for (LocalDateTime d = debut; !d.isAfter(fin); d = d.plusMonths(1)) {
            n++;
        }
        return n;
    }

    /** Variante montants (travaux, garantie) : 1 → tout, 2 → moitiés, 3-4 → formules OMRAN (σ ≤ 1), ≥ 5 → loi normale. */
    public static List<BigDecimal> montants(int n, BigDecimal sigma, BigDecimal total) {
        if (n <= 0) {
            return List.of();
        }
        if (n == 1) {
            return List.of(total);
        }
        double s = sigma(sigma);
        double m = total.doubleValue();
        double[] v = switch (n) {
            case 2 -> new double[]{m / 2, m / 2};
            case 3 -> trois(s, m);
            case 4 -> quatre(s, m);
            default -> normale(n, s, m);
        };
        List<BigDecimal> lignes = new ArrayList<>();
        for (double x : v) {
            lignes.add(BigDecimal.valueOf(x).setScale(ECHELLE, RoundingMode.HALF_UP));
        }
        return avecReliquat(lignes, total);
    }

    private static double[] trois(double s, double m) {
        exigerSigmaAuPlus1(s, 3);
        double g = m * phi((s - 1.5) / s, s);
        double c = m - 2 * g;
        return c < g ? new double[]{(m - g) / 2, g, (m - g) / 2} : new double[]{g, c, g};
    }

    private static double[] quatre(double s, double m) {
        exigerSigmaAuPlus1(s, 4);
        double g = m * phi((s - 2) / s, s);
        double c = (m - 2 * g) / 2;
        return c < g ? new double[]{c, g, g, c} : new double[]{g, c, c, g};
    }

    /** Moyenne ENTIÈRE ⌊(n−1)/2⌋ (division entière d'OMRAN), écart-type σ. */
    private static double[] normale(int n, double s, double m) {
        int mu = (n - 1) / 2;
        double[] v = new double[n];
        for (int i = 0; i < n; i++) {
            v[i] = m * phi((i - mu) / s, s);
        }
        return v;
    }

    /**
     * Variante quantités (concrétisation, finalisation) : {@code q} articles répartis, arrondi au pair puis ajusté
     * comme OMRAN (l.1190-1256) ; OMRAN boucle sans fin si l'ajustement ne converge pas : ici 400 au-delà de 2n + q tours.
     */
    public static int[] quantites(int n, BigDecimal sigma, int q) {
        if (n <= 0) {
            return new int[0];
        }
        double s = sigma(sigma);
        double mu = (n - 1) / 2.0 + 1;
        double[] parts = new double[n];
        double somme = 0;
        for (int i = 0; i < n; i++) {
            parts[i] = phi((i + 1 - mu) / s, s);
            somme += parts[i];
        }
        int[] u = new int[n];
        for (int i = 0; i < n; i++) {
            u[i] = (int) Math.rint(q * parts[i] / somme);
        }
        int centre = (int) Math.rint((n - 1) / 2.0);
        for (int tour = 0; tour <= 2 * n + q; tour++) {
            int total = somme(u);
            if (total == q) {
                return u;
            }
            if (total < q) {
                u[centre] += q - total;
            } else {
                retirer(u, centre, total - q);
            }
        }
        throw new InvalidRequestException("Répartition impossible : %d article(s) sur %d mois.".formatted(q, n));
    }

    /** Un passage de la boucle de retrait d'OMRAN, de {@code centre} vers la fin ; un voisin absent compte comme « non ». */
    private static void retirer(int[] u, int centre, int exces) {
        int n = u.length;
        for (int k = centre; k < n && exces > 0; k++) {
            if (u[k] >= 1 && ((k > 0 && u[k] >= u[k - 1] + 1) || k + 1 == n)) {
                u[k]--;
                exces--;
            } else if (k + 1 < n) {
                if (u[k + 1] >= 1 && k + 2 < n && u[k + 1] >= u[k + 2] + 1) {
                    u[k + 1]--;
                    exces--;
                }
            } else if (k - 1 >= 0 && u[k - 1] >= 1 && k - 2 > 0 && u[k - 1] >= u[k - 2] + 1) {
                u[k - 1]--;
                exces--;
            }
        }
    }

    /** Ajoute {@code total − Σ} à la plus grande ligne : la somme stockée égale la source. */
    public static List<BigDecimal> avecReliquat(List<BigDecimal> lignes, BigDecimal total) {
        if (lignes.isEmpty()) {
            return lignes;
        }
        List<BigDecimal> resultat = new ArrayList<>(lignes);
        int max = 0;
        for (int i = 1; i < resultat.size(); i++) {
            if (resultat.get(i).compareTo(resultat.get(max)) > 0) {
                max = i;
            }
        }
        BigDecimal somme = resultat.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        resultat.set(max, resultat.get(max).add(total.subtract(somme, MathContext.DECIMAL128))
                .setScale(ECHELLE, RoundingMode.HALF_UP));
        return resultat;
    }

    private static int somme(int[] u) {
        int s = 0;
        for (int x : u) {
            s += x;
        }
        return s;
    }

    private static double phi(double z, double s) {
        return Math.exp(-0.5 * z * z) / (s * RACINE_DEUX_PI);
    }

    private static void exigerSigmaAuPlus1(double s, int n) {
        if (s > 1) {
            throw new InvalidRequestException(
                    "la valeur de l'écart type doit être entre 0.5 et 1 pour un nombre de mois egal à " + n);
        }
    }

    /** Écart-type absent = 2 (constructeur OMRAN) ; nul ou négatif → 400. */
    private static double sigma(BigDecimal sigma) {
        double s = sigma == null ? 2 : sigma.doubleValue();
        if (s <= 0) {
            throw new InvalidRequestException("la valeur de l'écart type doit être supérieur ou égal à 0.5");
        }
        return s;
    }
}

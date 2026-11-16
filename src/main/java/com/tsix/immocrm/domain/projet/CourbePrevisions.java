package com.tsix.immocrm.domain.projet;

import com.tsix.immocrm.domain.projet.EntreesSimulation.Point;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/** Points d'avancement d'un lot : courbe générée, temps d'un point, retenue d'un point par mois (FEATURE-lot-previsions.md E1, P1, R6). */
public final class CourbePrevisions {

    static final MathContext PRECISION = MathContext.DECIMAL128;

    private CourbePrevisions() {
    }

    /** Courbe par défaut : un point par mois entamé entre les bornes, le dernier à 1 ; aucun mois → un point à 1 à la fin. */
    public static List<Point> generer(LocalDate debut, LocalDate fin) {
        int n = 0;
        while (debut.plusMonths(n).isBefore(fin)) {
            n++;
        }
        if (n == 0) {
            return List.of(new Point(fin, BigDecimal.ONE, BigDecimal.ZERO));
        }
        BigDecimal pas = BigDecimal.ONE.divide(BigDecimal.valueOf(n), PRECISION);
        long jours = ChronoUnit.DAYS.between(debut, fin);
        List<Point> points = new ArrayList<>();
        BigDecimal avancement = BigDecimal.ZERO;
        for (int i = 1; i <= n; i++) {
            avancement = i == n ? BigDecimal.ONE : avancement.add(pas);
            LocalDate date = debut.plusMonths(i).minusDays(1);
            points.add(new Point(date, avancement, BigDecimal.valueOf(ChronoUnit.DAYS.between(debut, date))
                    .divide(BigDecimal.valueOf(jours), PRECISION)));
        }
        return points;
    }

    /** {@code PointTime} d'une date saisie : fraction de la durée écoulée, 0 si durée nulle ou date avant le début. */
    public static BigDecimal temps(LocalDate date, LocalDate debut, int duree) {
        if (duree == 0 || date.isBefore(debut)) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(ChronoUnit.DAYS.between(debut, date)).divide(BigDecimal.valueOf(duree), PRECISION);
    }

    /** Début + durée × {@code PointTime}, au jour le plus proche. */
    static LocalDate redater(LocalDate debut, int duree, BigDecimal temps) {
        return plusJours(debut, BigDecimal.valueOf(duree).multiply(temps));
    }

    /** {@code AddDays(double)} d'OMRAN ramené au jour : partie entière des jours (l'heure tombe). */
    static LocalDate plusJours(LocalDate date, BigDecimal jours) {
        return date.plusDays(jours.setScale(6, RoundingMode.HALF_UP).setScale(0, RoundingMode.FLOOR).longValue());
    }

    /** Points triés par date (après {@code datation}), le plus avancé de chaque mois. */
    static List<Point> maximumParMois(List<Point> points, UnaryOperator<Point> datation) {
        Map<YearMonth, Point> parMois = new LinkedHashMap<>();
        points.stream().map(datation).sorted(Comparator.comparing(Point::date)).forEach(p -> parMois.merge(YearMonth.from(p.date()), p,
                (a, b) -> b.avancement().compareTo(a.avancement()) > 0 ? b : a));
        return List.copyOf(parMois.values());
    }
}

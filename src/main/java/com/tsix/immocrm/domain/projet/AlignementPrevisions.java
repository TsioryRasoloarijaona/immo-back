package com.tsix.immocrm.domain.projet;

import com.tsix.immocrm.domain.projet.EntreesSimulation.DecompteReel;
import com.tsix.immocrm.domain.projet.EntreesSimulation.Planning;
import com.tsix.immocrm.domain.projet.EntreesSimulation.Point;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.tsix.immocrm.domain.projet.CourbePrevisions.PRECISION;
import static com.tsix.immocrm.domain.projet.CourbePrevisions.maximumParMois;
import static com.tsix.immocrm.domain.projet.CourbePrevisions.plusJours;
import static com.tsix.immocrm.domain.projet.CourbePrevisions.redater;
import static com.tsix.immocrm.domain.projet.SimulationPaiementsLot.ecart;
import static com.tsix.immocrm.domain.projet.SimulationPaiementsLot.jour;
import static com.tsix.immocrm.domain.projet.SimulationPaiementsLot.pointsPrevision;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;

/** « Aligner les prévisions au réel » (OMRAN {@code Market.cs}, FEATURE-lot-previsions-reel.md §4 A2-A5), début renseigné. */
public final class AlignementPrevisions {

    /** {@code dernierReel} null sans décompte réel ; {@code ecartJours} = −{@code GetDeltaTime}. */
    public record Ecarts(LocalDate finActuelle, LocalDate finSysteme, LocalDate dernierReel, BigDecimal ecartAvancement,
                         BigDecimal ecartJours) {
    }

    private AlignementPrevisions() {
    }

    public static Ecarts ecarts(EntreesSimulation e) {
        Planning p = e.planning();
        BigDecimal delta = ecart(e, pointsPrevision(e));
        LocalDate dernier = dernierReel(e).orElse(null);
        BigDecimal ecartAvancement = e.decomptesReels().stream()
                .max(Comparator.comparing(DecompteReel::avancement).thenComparing(DecompteReel::date))
                .map(r -> r.avancement().subtract(prevuA(r.date(), e)))
                .orElse(ZERO);
        return new Ecarts(p.fin(), plusJours(dernier == null ? p.fin() : dernier, delta), dernier, ecartAvancement,
                delta.negate().setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * A5 : décomptes réels (avancement réel), puis points d'avancement supérieur à l'avancement du lot redatés vers
     * {@code finChoisie} ({@code CalculateReportedDate}), le plus avancé de chaque mois ; triés par date.
     */
    public static List<Point> aligner(EntreesSimulation e, LocalDate finChoisie) {
        List<Point> points = pointsPrevision(e);
        LocalDate ddec = dernierReel(e).orElse(e.planning().debut());
        BigDecimal dd = jour(ddec);
        BigDecimal dr = dd.subtract(ecart(e, points));
        BigDecimal df = jour(e.planning().fin());
        BigDecimal dfp = jour(finChoisie);
        BigDecimal ar = e.montants().avancement();
        List<Point> prevus = maximumParMois(points.stream().filter(pt -> pt.avancement().compareTo(ar) > 0).toList(),
                pt -> df.compareTo(dr) == 0 ? pt : new Point(plusJours(LocalDate.EPOCH, dfp.subtract(df.subtract(jour(pt.date()))
                        .multiply(dfp.subtract(dd)).divide(df.subtract(dr), PRECISION))), pt.avancement(), pt.temps()));
        return Stream.concat(e.decomptesReels().stream().map(r -> new Point(r.date(), r.avancement(), ZERO)), prevus.stream())
                .sorted(Comparator.comparing(Point::date)).toList();
    }

    /** M5 : date = début + durée × temps (jours tronqués). */
    public static LocalDate dateDuTemps(LocalDate debut, int duree, BigDecimal temps) {
        return redater(debut, duree, temps);
    }

    private static Optional<LocalDate> dernierReel(EntreesSimulation e) {
        return e.decomptesReels().stream().map(DecompteReel::date).max(Comparator.naturalOrder());
    }

    /** {@code GetAchievmentPrevisionAtDate} : avancement prévu à la date {@code d} (points redatés début + durée × temps). */
    static BigDecimal prevuA(LocalDate d, EntreesSimulation e) {
        Planning p = e.planning();
        if (d.isBefore(p.debut())) {
            return ZERO;
        }
        if (d.isAfter(p.fin())) {
            return ONE;
        }
        List<Point> points = pointsPrevision(e).stream()
                .map(pt -> new Point(redater(p.debut(), p.duree(), pt.temps()), pt.avancement(), pt.temps())).toList();
        Optional<BigDecimal> duMois = points.stream().filter(pt -> YearMonth.from(pt.date()).equals(YearMonth.from(d)))
                .map(Point::avancement).max(Comparator.naturalOrder());
        if (duMois.isPresent()) {
            return duMois.get();
        }
        Point avant = points.stream().filter(pt -> !pt.date().isAfter(d)).max(Comparator.comparing(Point::date))
                .orElse(new Point(p.debut(), ZERO, ZERO));
        Point apres = points.stream().filter(pt -> !pt.date().isBefore(d)).min(Comparator.comparing(Point::date))
                .orElse(new Point(p.fin(), ONE, ONE));
        long jours = ChronoUnit.DAYS.between(avant.date(), apres.date());
        if (jours == 0) {
            return ZERO;
        }
        return avant.avancement().add(apres.avancement().subtract(avant.avancement())
                .multiply(BigDecimal.valueOf(ChronoUnit.DAYS.between(avant.date(), d.plusDays(decalage(d, p, avant, apres, jours)))))
                .divide(BigDecimal.valueOf(jours), PRECISION));
    }

    // Décalage OMRAN (Market.cs l.1985-1993) : jours moyens par mois entre les deux points, borné à la fin du mois de d.
    private static long decalage(LocalDate d, Planning p, Point avant, Point apres, long jours) {
        long mois = ChronoUnit.MONTHS.between(YearMonth.from(avant.date()), YearMonth.from(apres.date()));
        long decalage = mois == 0 ? 0 : jours / mois;
        if (d.equals(p.debut())) {
            return 0;
        }
        return d.plusDays(decalage).getMonthValue() > d.getMonthValue() ? 30L - d.getDayOfMonth() : decalage;
    }
}

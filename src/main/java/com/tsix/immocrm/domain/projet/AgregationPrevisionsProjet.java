package com.tsix.immocrm.domain.projet;

import com.tsix.immocrm.dto.projet.PrevisionsLotDto.PointPrevisionDto;
import com.tsix.immocrm.dto.projet.PrevisionsProjetDto.AvancementsPrevisionDto;
import com.tsix.immocrm.dto.projet.PrevisionsProjetDto.DecaissementPrevisionDto;
import com.tsix.immocrm.dto.projet.PrevisionsProjetDto.DecaissementsPrevisionDto;
import com.tsix.immocrm.dto.projet.PrevisionsProjetDto.PointAvancementDto;
import com.tsix.immocrm.dto.projet.PrevisionsProjetDto.TotalNetAPayerDto;
import com.tsix.immocrm.dto.projet.PrevisionsProjetDto.TotauxNetAPayerDto;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.math.BigDecimal.ZERO;

/**
 * Séries de prévision d'une liste de lots, sommées (FEATURE-n4-previsions-chantier.md R4-R8). Un seul lot de poids 1 = le graphe
 * de sa fiche (R3) : la fiche lot passe par ici aussi.
 */
public final class AgregationPrevisionsProjet {

    public record Montant(LocalDate date, String libelle, BigDecimal montant) {
    }

    /** Avancements d'un lot par mois (valeur null = point masqué) ; cartes vides pour un multi-fournisseurs. */
    public record SeriesLot(UUID lot, String description, List<Montant> estimation, List<Montant> previsions, List<Montant> reel,
                            NavigableMap<YearMonth, BigDecimal> avancementEstimation,
                            NavigableMap<YearMonth, BigDecimal> avancementPrevision,
                            NavigableMap<YearMonth, BigDecimal> avancementReel, LocalDate debut, LocalDate fin) {
    }

    private static final List<Function<SeriesLot, NavigableMap<YearMonth, BigDecimal>>> AVANCEMENTS =
            List.of(SeriesLot::avancementEstimation, SeriesLot::avancementPrevision, SeriesLot::avancementReel);

    private final List<SeriesLot> lots;
    private final Map<UUID, BigDecimal> poids;

    /** {@code poids} : lots non multi-fournisseurs seulement (voir {@link #poids(Map)}). */
    public AgregationPrevisionsProjet(List<SeriesLot> lots, Map<UUID, BigDecimal> poids) {
        this.lots = lots;
        this.poids = poids;
    }

    /** R7 : HT prévisionnel / Σ ; Σ = 0 → poids 0. */
    public static Map<UUID, BigDecimal> poids(Map<UUID, BigDecimal> htNonMulti) {
        BigDecimal total = htNonMulti.values().stream().reduce(ZERO, BigDecimal::add);
        Map<UUID, BigDecimal> poids = new HashMap<>();
        htNonMulti.forEach((lot, ht) -> poids.put(lot, total.signum() == 0 ? ZERO : ht.divide(total, MathContext.DECIMAL64)));
        return poids;
    }

    /** R8 : du mois précédant le premier point au dernier, sommes non arrondies. */
    public List<PointPrevisionDto> mois() {
        TreeMap<YearMonth, BigDecimal[]> sommes = new TreeMap<>();
        for (SeriesLot s : lots) {
            cumuler(sommes, s.estimation(), 0);
            cumuler(sommes, s.previsions(), 1);
            cumuler(sommes, s.reel(), 2);
            // L'avancement ajusté ne crée aucun mois (R7-bis) : côté lot, ses mois sont déjà ceux des lignes P.
            Stream.of(s.avancementEstimation(), s.avancementReel())
                    .forEach(a -> a.keySet().forEach(m -> sommes.computeIfAbsent(m, k -> zeros())));
        }
        if (sommes.isEmpty()) {
            return List.of();
        }
        List<PointPrevisionDto> mois = new ArrayList<>();
        for (YearMonth m = sommes.firstKey().minusMonths(1); !m.isAfter(sommes.lastKey()); m = m.plusMonths(1)) {
            BigDecimal[] v = sommes.getOrDefault(m, zeros());
            mois.add(new PointPrevisionDto(m.atDay(1), v[0], auMois(AVANCEMENTS.get(0), m), v[1], auMois(AVANCEMENTS.get(1), m),
                    v[2], auMois(AVANCEMENTS.get(2), m)));
        }
        return mois;
    }

    /** R4 à R6 ; le Réel n'a pas de total et son reste est ΣP (code OMRAN). */
    public TotauxNetAPayerDto totaux(LocalDate aujourdhui) {
        BigDecimal estimation = somme(SeriesLot::estimation, null);
        BigDecimal estimationDue = somme(SeriesLot::estimation, aujourdhui);
        BigDecimal p = somme(SeriesLot::previsions, null);
        BigDecimal pDue = somme(SeriesLot::previsions, aujourdhui);
        BigDecimal r = somme(SeriesLot::reel, null);
        return new TotauxNetAPayerDto(
                new TotalNetAPayerDto(estimation, estimationDue, estimation.subtract(estimationDue),
                        avancementA(AVANCEMENTS.get(0), aujourdhui)),
                new TotalNetAPayerDto(p.add(r), pDue.add(r), p.subtract(pDue), avancementA(AVANCEMENTS.get(1), aujourdhui)),
                new TotalNetAPayerDto(null, r, p, avancementA(AVANCEMENTS.get(2), aujourdhui)));
    }

    /** Lignes triées par date ; la Prévision ajustée = P puis R. */
    public DecaissementsPrevisionDto decaissements() {
        return new DecaissementsPrevisionDto(lignes(SeriesLot::estimation, null),
                Stream.concat(lignes(SeriesLot::previsions, "PREVISION").stream(), lignes(SeriesLot::reel, "REEL").stream())
                        .toList(),
                lignes(SeriesLot::reel, null));
    }

    /** R7 : un point par mois où la valeur change, du début des lots non multi à leur fin (ou au dernier point). */
    public AvancementsPrevisionDto avancements() {
        return new AvancementsPrevisionDto(liste(AVANCEMENTS.get(0)), liste(AVANCEMENTS.get(1)), liste(AVANCEMENTS.get(2)));
    }

    private List<PointAvancementDto> liste(Function<SeriesLot, NavigableMap<YearMonth, BigDecimal>> serie) {
        List<SeriesLot> ponderes = lots.stream().filter(s -> poids.containsKey(s.lot())).toList();
        LocalDate debut = borne(ponderes, SeriesLot::debut, false);
        if (debut == null) {
            return List.of();
        }
        YearMonth fin = YearMonth.from(Objects.requireNonNull(borne(ponderes, SeriesLot::fin, true))).plusMonths(1);
        for (SeriesLot s : ponderes) {
            if (!serie.apply(s).isEmpty() && serie.apply(s).lastKey().isAfter(fin)) {
                fin = serie.apply(s).lastKey();
            }
        }
        List<PointAvancementDto> liste = new ArrayList<>();
        BigDecimal precedente = null;
        for (YearMonth m = YearMonth.from(debut); !m.isAfter(fin); m = m.plusMonths(1)) {
            BigDecimal v = valeur(serie, m);
            if (precedente == null || v.compareTo(precedente) != 0) {
                liste.add(new PointAvancementDto(m.atDay(1), v));
            }
            precedente = v;
        }
        return liste;
    }

    /** {@code Project.GetAchievmentPrevisionAtDate} : 0 avant le 1ᵉʳ du mois du début, 1 après le mois suivant la fin. */
    private BigDecimal avancementA(Function<SeriesLot, NavigableMap<YearMonth, BigDecimal>> serie, LocalDate aujourdhui) {
        LocalDate debut = borne(lots, SeriesLot::debut, false);
        if (poids.isEmpty() || debut == null) {
            return ZERO;
        }
        if (aujourdhui.isBefore(debut.withDayOfMonth(1))) {
            return ZERO;
        }
        LocalDate fin = Objects.requireNonNull(borne(lots, SeriesLot::fin, true));
        if (aujourdhui.isAfter(fin.withDayOfMonth(1).plusMonths(1))) {
            return BigDecimal.ONE;
        }
        return valeur(serie, YearMonth.from(aujourdhui));
    }

    /** Point du graphe : null si aucun lot n'a de valeur ce mois-là, sinon la somme pondérée (R7). */
    private BigDecimal auMois(Function<SeriesLot, NavigableMap<YearMonth, BigDecimal>> serie, YearMonth m) {
        return lots.stream().anyMatch(s -> serie.apply(s).get(m) != null) ? valeur(serie, m) : null;
    }

    /** Σ poids × dernière valeur connue du lot à {@code m} (0 avant son premier point). */
    private BigDecimal valeur(Function<SeriesLot, NavigableMap<YearMonth, BigDecimal>> serie, YearMonth m) {
        BigDecimal v = ZERO;
        for (SeriesLot s : lots) {
            BigDecimal p = poids.get(s.lot());
            if (p != null) {
                v = v.add(p.multiply(serie.apply(s).headMap(m, true).descendingMap().values().stream()
                        .filter(Objects::nonNull).findFirst().orElse(ZERO)));
            }
        }
        return v;
    }

    /** Plus petite (ou plus grande) date connue ; la fin manquante d'un lot vaut son début. */
    private static LocalDate borne(List<SeriesLot> lots, Function<SeriesLot, LocalDate> date, boolean max) {
        Comparator<LocalDate> ordre = max ? Comparator.reverseOrder() : Comparator.naturalOrder();
        return lots.stream().map(s -> date.apply(s) != null ? date.apply(s) : s.debut()).filter(Objects::nonNull)
                .min(ordre).orElse(null);
    }

    private BigDecimal somme(Function<SeriesLot, List<Montant>> serie, LocalDate avant) {
        return lots.stream().flatMap(s -> serie.apply(s).stream())
                .filter(l -> avant == null || l.date().isBefore(avant))
                .map(Montant::montant).reduce(ZERO, BigDecimal::add);
    }

    private List<DecaissementPrevisionDto> lignes(Function<SeriesLot, List<Montant>> serie, String nature) {
        return lots.stream().flatMap(s -> serie.apply(s).stream().map(l -> new DecaissementPrevisionDto(l.date(), s.lot(),
                        s.description(), l.libelle(), l.montant(), nature)))
                .sorted(Comparator.comparing(DecaissementPrevisionDto::date)).toList();
    }

    private static void cumuler(TreeMap<YearMonth, BigDecimal[]> sommes, List<Montant> lignes, int rang) {
        for (Montant l : lignes) {
            BigDecimal[] v = sommes.computeIfAbsent(YearMonth.from(l.date()), m -> zeros());
            v[rang] = v[rang].add(l.montant());
        }
    }

    private static BigDecimal[] zeros() {
        return new BigDecimal[]{ZERO, ZERO, ZERO};
    }
}

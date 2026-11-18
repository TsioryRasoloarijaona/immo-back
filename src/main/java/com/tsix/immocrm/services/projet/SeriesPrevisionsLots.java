package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.projet.AgregationPrevisionsProjet.Montant;
import com.tsix.immocrm.domain.projet.AgregationPrevisionsProjet.SeriesLot;
import com.tsix.immocrm.domain.projet.CalculLot;
import com.tsix.immocrm.domain.projet.EntreesSimulation;
import com.tsix.immocrm.domain.projet.MontantsLot;
import com.tsix.immocrm.domain.projet.SimulationPaiementsLot;
import com.tsix.immocrm.entities.prj.PrjMarket;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.UUID;

import static com.tsix.immocrm.services.projet.FacturesDecomptes.date;
import static com.tsix.immocrm.services.projet.FacturesDecomptes.montant;
import static com.tsix.immocrm.services.projet.LectureSimulationLot.jour;
import static com.tsix.immocrm.services.projet.LectureSimulationLot.zero;

/** Séries datées des prévisions de lots (fiche lot et projet, FEATURE-n4-previsions-chantier.md R3) : une requête par table. */
@Component
class SeriesPrevisionsLots {

    private static final String PREVISIONS = """
            SELECT PrjMarketId, PaymentDate, Amount, Achievement, Description FROM PrjMarketPaymentPrevision
            WHERE PrjMarketId IN (:lots) AND PaymentDate IS NOT NULL ORDER BY PaymentDate ASC""";

    // Réel d'OMRAN (GetPaymentReal lit BuyDocumentToPayView, filtrée CanBePayed = 1) : hors avoirs, sans filtre société.
    private static final String REEL = """
            SELECT d.PrjMarketId, d.MaturityDate, d.AmountToPay, d.Code FROM BuyDocument d
            WHERE d.PrjMarketId IN (:lots) AND d.CanBePayed = 1 AND d.DocType <> 'REFUND' AND d.MaturityDate IS NOT NULL
            ORDER BY d.MaturityDate ASC, d.Code ASC""";

    private static final String DECOMPTES_FACTURES = "select w.prjMarket.id, w.id, w.docDate from PrjWorkingOut w"
            + " where w.prjMarket.id in :lots and w.buyDocument is not null and w.docDate is not null";

    /** Un lot lu : son calcul (montants de base) et ses séries. */
    record Lot(PrjMarket marche, CalculLot calcul, SeriesLot series) {
    }

    private final MontantsListeLots montants;
    private final LectureSimulationLot simulation;
    private final AvancementDecomptes avancement;
    private final EntityManager em;
    private final Clock clock;

    SeriesPrevisionsLots(MontantsListeLots montants, LectureSimulationLot simulation, AvancementDecomptes avancement,
                         EntityManager em, Clock clock) {
        this.montants = montants;
        this.simulation = simulation;
        this.avancement = avancement;
        this.em = em;
        this.clock = clock;
    }

    List<Lot> lire(List<PrjMarket> lots) {
        if (lots.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = lots.stream().map(PrjMarket::getId).toList();
        Map<UUID, CalculLot> calculs = montants.calculer(lots);
        Map<UUID, EntreesSimulation> entrees = simulation.entrees(lots, calculs);
        Map<UUID, List<Object[]>> previsions = simulation.parLot(PREVISIONS, ids, r -> r);
        Map<UUID, List<Montant>> reels = simulation.parLot(REEL, ids, r -> new Montant(date(r[0]), (String) r[2],
                zero(montant(r[1]))));
        Map<UUID, NavigableMap<YearMonth, BigDecimal>> avancementsReels = avancementsReels(
                lots.stream().filter(m -> !multi(m)).map(PrjMarket::getId).toList());
        LocalDate aujourdhui = LocalDate.now(clock);
        return lots.stream().map(m -> {
            UUID id = m.getId();
            return new Lot(m, calculs.get(id), series(m, entrees.get(id), aujourdhui, previsions.getOrDefault(id, List.of()),
                    reels.getOrDefault(id, List.of()), avancementsReels.getOrDefault(id, new TreeMap<>())));
        }).toList();
    }

    private static SeriesLot series(PrjMarket m, EntreesSimulation e, LocalDate aujourdhui, List<Object[]> lignesP,
                                    List<Montant> reel, NavigableMap<YearMonth, BigDecimal> avancementReel) {
        boolean avecAvancement = !multi(m);
        SimulationPaiementsLot.Estimation estimation = SimulationPaiementsLot.estimation(e, aujourdhui);
        NavigableMap<YearMonth, BigDecimal> avancementEstimation = new TreeMap<>();
        if (avecAvancement) {
            // Points triés par date : la dernière valeur du mois l'emporte ; 0 après le début de base = null (E8).
            LocalDate debutBase = e.planning().debutBase();
            estimation.avancements().forEach(a -> avancementEstimation.put(YearMonth.from(a.date()),
                    a.avancement().signum() == 0 && debutBase != null && a.date().isAfter(debutBase) ? null : a.avancement()));
        }
        List<Montant> previsions = new ArrayList<>();
        NavigableMap<YearMonth, BigDecimal> avancementPrevision = new TreeMap<>();
        for (Object[] r : lignesP) {
            previsions.add(new Montant(date(r[0]), (String) r[3], zero(montant(r[1]))));
            BigDecimal achevement = montant(r[2]);
            if (avecAvancement && achevement != null && achevement.signum() >= 0) {
                avancementPrevision.merge(YearMonth.from(date(r[0])), achevement, BigDecimal::max);
            }
        }
        return new SeriesLot(m.getId(), m.getDescription(),
                estimation.lignes().stream().map(l -> new Montant(l.date(), l.libelle(), l.montant())).toList(),
                previsions, reel, avancementEstimation, avancementPrevision, avancementReel, jour(m.getStartDate()),
                jour(m.getEndDate()));
    }

    /** Avancement décompté de chaque décompte facturé, le plus haut du mois. */
    private Map<UUID, NavigableMap<YearMonth, BigDecimal>> avancementsReels(List<UUID> lots) {
        Map<UUID, NavigableMap<YearMonth, BigDecimal>> parLot = new HashMap<>();
        if (lots.isEmpty()) {
            return parLot;
        }
        Map<UUID, AvancementDecomptes.Avancement> parDecompte = avancement.parDecompte(lots);
        for (Object[] r : em.createQuery(DECOMPTES_FACTURES, Object[].class).setParameter("lots", lots).getResultList()) {
            AvancementDecomptes.Avancement a = parDecompte.get(r[1]);
            if (a != null) {
                parLot.computeIfAbsent((UUID) r[0], k -> new TreeMap<>())
                        .merge(YearMonth.from((LocalDateTime) r[2]), MontantsLot.fraction(a.avaPct()), BigDecimal::max);
            }
        }
        return parLot;
    }

    private static boolean multi(PrjMarket m) {
        return Boolean.TRUE.equals(m.getIsMultiSupplier());
    }
}

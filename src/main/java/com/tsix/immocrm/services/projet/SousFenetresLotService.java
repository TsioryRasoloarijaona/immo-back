package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.achat.SoldeDocumentAchat;
import com.tsix.immocrm.domain.projet.MontantsLot;
import com.tsix.immocrm.dto.projet.AvanceLotDto;
import com.tsix.immocrm.dto.projet.AvancesLotDto;
import com.tsix.immocrm.dto.projet.AvenantLotDto;
import com.tsix.immocrm.dto.projet.AvoirLotDto;
import com.tsix.immocrm.dto.projet.PrevisionsLotDto;
import com.tsix.immocrm.dto.projet.PrevisionsLotDto.PointPrevisionDto;
import com.tsix.immocrm.dto.projet.RemboursementsLotDto;
import com.tsix.immocrm.dto.projet.RemboursementsLotDto.EcheanceRgLotDto;
import com.tsix.immocrm.dto.projet.RemboursementsLotDto.ReceptionLotDto;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.services.perimetre.PerimetreProjetService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import static com.tsix.immocrm.services.projet.FacturesDecomptes.date;
import static com.tsix.immocrm.services.projet.FacturesDecomptes.montant;
import static com.tsix.immocrm.services.projet.FacturesDecomptes.uuid;

/** Sous-fenêtres de la fiche lot en lecture (FEATURE-lot-sous-fenetres.md) ; chaque route lit d'abord le lot visible. */
@Service
@Transactional(readOnly = true)
public class SousFenetresLotService {

    private static final String MARKET_ADVANCE = "MARKET_ADVANCE";
    private static final String MARKET_WARRANTY = "MARKET_WARRANTY";

    // Pièces payables du lot, des seules sociétés habilitées (§3 règle 2) ; réglé par SoldeDocumentAchat (règle 6).
    private static final String PIECES_DU_LOT = """
             FROM BuyDocument d
            LEFT JOIN BuyDocumentStatus st ON st.PKey = d.BuyDocumentStatusId
            LEFT JOIN StlCurrency c ON c.PKey = d.StlCurrencyId
            WHERE d.PrjMarketId = :lot AND d.CanBePayed = 1 AND d.CfgCompanyId IN (:societes) AND\s""";
    private static final String PAR_DATE = " ORDER BY d.DocDate DESC, d.Code ASC";

    private static final String PIECES = """
            SELECT d.PKey, d.Code, d.BuyDocumentStatusId, st.Description, st.ColorName, d.DocDate, d.AmountToPay,
                   (""" + SoldeDocumentAchat.SQL_REGLE + "), c.Code, d.Notes, d.ReferenceType" + PIECES_DU_LOT
            + "d.DocType = 'DOCUMENT_TO_PAY' AND d.ReferenceType IN (:references)" + PAR_DATE;

    private static final String AVOIRS = """
            SELECT d.PKey, d.DocDate, d.Code, st.Description, st.ColorName, d.PersonToContact,
                   d.AmountHT, d.AmountVat, d.AmountTTC, d.OfficeOrderRef""" + PIECES_DU_LOT + "d.DocType = 'REFUND'" + PAR_DATE;

    private static final String AVENANTS = """
            SELECT c.PKey, c.Sorting, c.DocDate, c.Code, c.Description, c.AmountHT, c.AmountVat, c.AmountTTC, c.AmountToPay,
                   (SELECT STRING_AGG(f.Code, ', ') FROM BuyDocument f WHERE f.PrjClauseId = c.PKey AND f.DocType = 'INVOICE')
            FROM PrjClause c WHERE c.PrjMarketId = :lot ORDER BY c.DocDate DESC, c.Code ASC""";

    private static final String ECHEANCES = """
            SELECT PKey, Sorting, Description, Delay, EcheanceDate, Ratio FROM PrjMarketWarrantyLine
            WHERE PrjMarketId = :lot ORDER BY Sorting ASC, EcheanceDate ASC""";

    private static final String RECEPTIONS = """
            SELECT r.PKey, r.sorting, r.Code, r.DocDate, r.RequiredBySupplier, r.MeetingPlace, t.Description, k.Description, k.IsFinal
            FROM PrjMarketReception r
            LEFT JOIN PrjMarketReceptionType t ON t.PKey = r.PrjMarketReceptionTypeId
            LEFT JOIN PrjMarketReceptionKind k ON k.PKey = r.PrjMarketReceptionKindId
            WHERE r.PrjMarketId = :lot ORDER BY r.sorting ASC, r.Code ASC""";

    private static final String PREVISIONS = """
            SELECT PaymentDate, Amount, Achievement FROM PrjMarketPaymentPrevision
            WHERE PrjMarketId = :lot AND PaymentDate IS NOT NULL""";

    // Réel d'OMRAN (GetPaymentReal lit BuyDocumentToPayView, filtrée CanBePayed = 1) : hors avoirs, sans filtre société.
    private static final String REEL = """
            SELECT d.MaturityDate, d.AmountToPay FROM BuyDocument d
            WHERE d.PrjMarketId = :lot AND d.CanBePayed = 1 AND d.DocType <> 'REFUND' AND d.MaturityDate IS NOT NULL""";

    private static final String DECOMPTES_FACTURES = "select w.id, w.docDate from PrjWorkingOut w"
            + " where w.prjMarket.id = :lot and w.buyDocument is not null and w.docDate is not null";

    private final LotsProjetService lots;
    private final PerimetreProjetService perimetre;
    private final AvancementDecomptes avancement;
    private final EntityManager em;

    SousFenetresLotService(LotsProjetService lots, PerimetreProjetService perimetre, AvancementDecomptes avancement,
                           EntityManager em) {
        this.lots = lots;
        this.perimetre = perimetre;
        this.avancement = avancement;
        this.em = em;
    }

    public AvancesLotDto avances(UUID id) {
        Map<String, List<AvanceLotDto>> parReference = pieces(lots.visible(id).getId(), MARKET_ADVANCE, "WORKINGOUT_ADVANCE");
        return new AvancesLotDto(parReference.get(MARKET_ADVANCE), parReference.get("WORKINGOUT_ADVANCE"));
    }

    public List<AvoirLotDto> avoirs(UUID id) {
        return lignes(societesHabilitees(AVOIRS, lots.visible(id).getId())).stream().map(r -> new AvoirLotDto(uuid(r[0]), date(r[1]),
                (String) r[2], (String) r[3], (String) r[4], (String) r[5], montant(r[6]), montant(r[7]), montant(r[8]),
                (String) r[9])).toList();
    }

    public List<AvenantLotDto> avenants(UUID id) {
        return lignes(duLot(AVENANTS, lots.visible(id).getId())).stream().map(r -> new AvenantLotDto(uuid(r[0]),
                entier(r[1]), date(r[2]), (String) r[3], (String) r[4], montant(r[5]), montant(r[6]), montant(r[7]),
                montant(r[8]), (String) r[9])).toList();
    }

    public RemboursementsLotDto remboursements(UUID id) {
        UUID lot = lots.visible(id).getId();
        List<EcheanceRgLotDto> echeances = lignes(duLot(ECHEANCES, lot)).stream().map(r -> new EcheanceRgLotDto(uuid(r[0]),
                entier(r[1]), (String) r[2], entier(r[3]), date(r[4]), MontantsLot.pourcentage(montant(r[5])))).toList();
        List<ReceptionLotDto> receptions = lignes(duLot(RECEPTIONS, lot)).stream().map(r -> new ReceptionLotDto(uuid(r[0]),
                entier(r[1]), (String) r[2], date(r[3]), vrai(r[4]), (String) r[5], (String) r[6], (String) r[7],
                vrai(r[8]))).toList();
        Map<String, List<AvanceLotDto>> parReference = pieces(lot, MARKET_WARRANTY, "MARKET_PROPORTION");
        return new RemboursementsLotDto(echeances, receptions, parReference.get(MARKET_WARRANTY),
                parReference.get("MARKET_PROPORTION"));
    }

    // Série « Estimation » non calculée (simulation de décomptes OMRAN) : voir RAPPORT, lot L3a-bis.
    public PrevisionsLotDto previsions(UUID id) {
        PrjMarket lot = lots.visible(id);
        boolean avecAvancement = !Boolean.TRUE.equals(lot.getIsMultiSupplier());
        TreeMap<YearMonth, PointMois> points = new TreeMap<>();
        for (Object[] r : lignes(duLot(PREVISIONS, lot.getId()))) {
            PointMois p = points.computeIfAbsent(mois(r[0]), m -> new PointMois());
            p.prevision = p.prevision.add(zero(montant(r[1])));
            BigDecimal achevement = montant(r[2]);
            if (avecAvancement && achevement != null && achevement.signum() >= 0) {
                p.avancementPrevision = max(p.avancementPrevision, achevement);
            }
        }
        for (Object[] r : lignes(duLot(REEL, lot.getId()))) {
            PointMois p = points.computeIfAbsent(mois(r[0]), m -> new PointMois());
            p.reel = p.reel.add(zero(montant(r[1])));
        }
        if (avecAvancement) {
            avancementsReels(lot.getId(), points);
        }
        return serie(lot.getId(), points);
    }

    private void avancementsReels(UUID lot, TreeMap<YearMonth, PointMois> points) {
        Map<UUID, AvancementDecomptes.Avancement> parDecompte = avancement.parDecompte(List.of(lot));
        for (Object[] r : em.createQuery(DECOMPTES_FACTURES, Object[].class).setParameter("lot", lot).getResultList()) {
            AvancementDecomptes.Avancement a = parDecompte.get((UUID) r[0]);
            if (a != null) {
                PointMois p = points.computeIfAbsent(YearMonth.from((LocalDateTime) r[1]), m -> new PointMois());
                p.avancementReel = max(p.avancementReel, MontantsLot.fraction(a.avaPct()));
            }
        }
    }

    private static PrevisionsLotDto serie(UUID lot, TreeMap<YearMonth, PointMois> points) {
        if (points.isEmpty()) {
            return new PrevisionsLotDto(lot, null, List.of());
        }
        List<PointPrevisionDto> mois = new ArrayList<>();
        for (YearMonth m = points.firstKey().minusMonths(1); !m.isAfter(points.lastKey()); m = m.plusMonths(1)) {
            PointMois p = points.getOrDefault(m, new PointMois());
            // Sommes non arrondies : la moitié des montants prévus a plus de 2 décimales, Σ des mois = Σ de la table.
            mois.add(new PointPrevisionDto(m.atDay(1), BigDecimal.ZERO, null, p.prevision, p.avancementPrevision, p.reel,
                    p.avancementReel));
        }
        return new PrevisionsLotDto(lot, mois.getFirst().mois(), mois);
    }

    /** Pièces DOCUMENT_TO_PAY du lot par ReferenceType (chaque type demandé présent, liste vide au besoin). */
    private Map<String, List<AvanceLotDto>> pieces(UUID lot, String... references) {
        Map<String, List<AvanceLotDto>> parReference = new HashMap<>();
        for (String reference : references) {
            parReference.put(reference, new ArrayList<>());
        }
        for (Object[] r : lignes(societesHabilitees(PIECES, lot).setParameter("references", List.of(references)))) {
            SoldeDocumentAchat solde = SoldeDocumentAchat.de(montant(r[6]), montant(r[7]));
            parReference.get((String) r[10]).add(new AvanceLotDto(uuid(r[0]), (String) r[1], uuidOuNull(r[2]),
                    (String) r[3], (String) r[4], date(r[5]), solde.montantAPayer(), solde.montantRegle(),
                    solde.resteARegler(), (String) r[8], (String) r[9]));
        }
        return parReference;
    }

    private Query societesHabilitees(String sql, UUID lot) {
        Set<UUID> societes = perimetre.achat(List.of()).societes();
        // SQL Server refuse IN () : un ensemble vide devient un identifiant qui n'existe pas.
        return duLot(sql, lot).setParameter("societes", societes.isEmpty() ? List.of(new UUID(0, 0)) : societes);
    }

    private Query duLot(String sql, UUID lot) {
        return em.createNativeQuery(sql).setParameter("lot", lot);
    }

    private static List<Object[]> lignes(Query requete) {
        List<?> brutes = requete.getResultList();
        return brutes.stream().map(Object[].class::cast).toList();
    }

    private static final class PointMois {
        private BigDecimal prevision = BigDecimal.ZERO;
        private BigDecimal reel = BigDecimal.ZERO;
        private BigDecimal avancementPrevision;
        private BigDecimal avancementReel;
    }

    private static YearMonth mois(Object brut) {
        return YearMonth.from(date(brut));
    }

    private static BigDecimal max(BigDecimal actuel, BigDecimal valeur) {
        return actuel == null || valeur.compareTo(actuel) > 0 ? valeur : actuel;
    }

    private static BigDecimal zero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static Integer entier(Object brut) {
        return brut instanceof Number n ? n.intValue() : null;
    }

    private static boolean vrai(Object brut) {
        return Boolean.TRUE.equals(brut) || (brut instanceof Number n && n.intValue() == 1);
    }

    private static UUID uuidOuNull(Object brut) {
        return brut == null ? null : uuid(brut);
    }
}

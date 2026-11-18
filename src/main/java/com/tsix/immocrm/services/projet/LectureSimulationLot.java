package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.projet.CalculLot;
import com.tsix.immocrm.domain.projet.EntreesLot;
import com.tsix.immocrm.domain.projet.EntreesSimulation;
import com.tsix.immocrm.domain.projet.EntreesSimulation.DecompteReel;
import com.tsix.immocrm.domain.projet.EntreesSimulation.Echeance;
import com.tsix.immocrm.domain.projet.EntreesSimulation.LigneFixe;
import com.tsix.immocrm.domain.projet.EntreesSimulation.Point;
import com.tsix.immocrm.domain.projet.EntreesSimulation.RetenueStockee;
import com.tsix.immocrm.domain.projet.EntreesSimulation.RetenuesStockees;
import com.tsix.immocrm.domain.projet.MontantsLot;
import com.tsix.immocrm.domain.projet.SimulationPaiementsLot;
import com.tsix.immocrm.entities.prj.PrjMarket;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

import static com.tsix.immocrm.services.projet.FacturesDecomptes.date;
import static com.tsix.immocrm.services.projet.FacturesDecomptes.montant;
import static com.tsix.immocrm.services.projet.FacturesDecomptes.uuid;
import static java.math.BigDecimal.ZERO;

/** Entrées de {@link SimulationPaiementsLot} pour un lot (FEATURE-lot-previsions.md §3.1), lues en base. */
@Component
class LectureSimulationLot {

    // Chaque requête rend le lot en 1ʳᵉ colonne (retirée par parLot) : une requête par table pour tous les lots.
    private static final String ECHEANCES = """
            SELECT PrjMarketId, Ratio, EcheanceDate, Delay, Description FROM PrjMarketWarrantyLine
            WHERE PrjMarketId IN (:lots) ORDER BY Sorting ASC, EcheanceDate ASC""";

    // P2 : décomptes facturés et leurs retenues stockées (C1, C3) ; multi-fournisseur : factures (seule leur date sert).
    private static final String DECOMPTES = """
            SELECT w.PrjMarketId, w.PKey, w.DocDate, w.AmountTTC,
                w.AdvanceRatio, (SELECT SUM(a.AmountTTC) FROM BuyDocument a WHERE a.PrjMarketId = w.PrjMarketId
                    AND a.DocType = 'DOCUMENT_TO_PAY' AND a.ReferenceType = 'MARKET_ADVANCE'
                    AND CAST(a.DocDate AS date) <= CAST(w.DocDate AS date)), w.AdvanceAmount, w.AdvanceBasedOnAmount,
                w.WarrantyRatio, 0, w.WarrantyAmount, w.WarrantyBasedOnAmount,
                w.proportionRatio, 0, w.proportionAmount, w.ProrataBasedOnAmount,
                w.insuranceDECRatio, w.InsuranceDECAmountMax, w.insuranceDECAmount, w.DECBasedOnAmount,
                w.insuranceTRCRatio, w.InsuranceTRCAmountMax, w.insuranceTRCAmount, w.TRCBasedOnAmount
            FROM PrjWorkingOut w
            WHERE w.PrjMarketId IN (:lots) AND w.BuyDocumentId IS NOT NULL AND w.DocDate IS NOT NULL
            ORDER BY w.DocDate ASC, w.Sorting ASC""";
    private static final String FACTURES = """
            SELECT PrjMarketId, PKey, DocDate, 0 FROM BuyDocument
            WHERE PrjMarketId IN (:lots) AND DocType = 'INVOICE' AND DocDate IS NOT NULL ORDER BY DocDate ASC""";

    private static final String AVANCES = """
            SELECT PrjMarketId, AmountTTC FROM BuyDocument
            WHERE PrjMarketId IN (:lots) AND DocType = 'DOCUMENT_TO_PAY' AND ReferenceType = 'MARKET_ADVANCE'""";

    private static final String AVENANTS = """
            SELECT c.PrjMarketId, c.DocDate, c.AmountToPay FROM PrjClause c
            WHERE c.PrjMarketId IN (:lots) AND c.DocDate IS NOT NULL
              AND NOT EXISTS (SELECT 1 FROM BuyDocument f WHERE f.PrjClauseId = c.PKey AND f.DocType = 'INVOICE')""";

    // P10 : PrevisionAmount de BuyDocumentOrderView, recalculé sur les pièces liées (formules d'AgregatsBonCommande).
    private static final String BONS_COMMANDE = """
            SELECT o.PrjMarketId, o.DocDate, o.Description, x.Prevision, o.AmountHT - ISNULL(f.H, 0), o.AmountTTC - ISNULL(f.T, 0)
            FROM BuyDocument o
            JOIN BuyDocumentStatus s ON s.PKey = o.BuyDocumentStatusId AND s.IsOkForPayement = 1
            OUTER APPLY (SELECT SUM(d.AmountToPay) AS P FROM BuyDocument d
                         WHERE d.BuyOrderId = o.PKey AND d.ReferenceType = 'ORDER_ADVANCE') a
            OUTER APPLY (SELECT SUM(d.AmountToPay) AS P, SUM(d.AmountHT) AS H, SUM(d.AmountTTC) AS T FROM BuyDocument d
                         WHERE d.BuyOrderId = o.PKey AND d.DocType = 'INVOICE') f
            CROSS APPLY (SELECT ISNULL(o.AmountToPay, 0) - ISNULL(a.P, 0) - ISNULL(f.P, 0) AS Prevision) x
            WHERE o.DocType = 'ORDER' AND o.PrjMarketId IN (:lots) AND o.DocDate IS NOT NULL AND x.Prevision > 0""";

    private final MontantsListeLots montants;
    private final AvancementDecomptes avancement;
    private final EntityManager em;

    LectureSimulationLot(MontantsListeLots montants, AvancementDecomptes avancement, EntityManager em) {
        this.montants = montants;
        this.avancement = avancement;
        this.em = em;
    }

    List<SimulationPaiementsLot.Ligne> paiements(PrjMarket lot) {
        return SimulationPaiementsLot.paiements(entrees(lot));
    }

    EntreesSimulation entrees(PrjMarket lot) {
        return Objects.requireNonNull(entrees(List.of(lot), montants.calculer(List.of(lot))).get(lot.getId()));
    }

    /** Entrées de plusieurs lots, une requête par table ; {@code calculs} = {@link MontantsListeLots#calculer} des mêmes lots. */
    Map<UUID, EntreesSimulation> entrees(List<PrjMarket> lots, Map<UUID, CalculLot> calculs) {
        List<UUID> ids = lots.stream().map(PrjMarket::getId).toList();
        List<UUID> multi = ids.stream().filter(id -> calculs.get(id).drapeaux().multi()).toList();
        List<UUID> simples = ids.stream().filter(id -> !multi.contains(id)).toList();
        Map<UUID, AvancementDecomptes.Avancement> parDecompte = simples.isEmpty() ? Map.of() : avancement.parDecompte(simples);
        Map<UUID, List<Point>> prevision = points(TablePoints.PREVISION, ids);
        Map<UUID, List<Point>> base = points(TablePoints.BASE, ids);
        Map<UUID, List<Echeance>> echeances = parLot(ECHEANCES, ids, r -> new Echeance(zero(montant(r[0])), date(r[1]),
                r[2] instanceof Number n ? n.intValue() : 0, (String) r[3]));
        Map<UUID, List<DecompteReel>> reels = new HashMap<>(reels(DECOMPTES, simples, parDecompte));
        reels.putAll(reels(FACTURES, multi, Map.of()));
        Map<UUID, List<BigDecimal>> avances = parLot(AVANCES, ids, r -> zero(montant(r[0])));
        Map<UUID, List<LigneFixe>> avenants = parLot(AVENANTS, ids,
                r -> new LigneFixe(date(r[0]), null, zero(montant(r[1])), ZERO, ZERO));
        Map<UUID, List<LigneFixe>> commandes = parLot(BONS_COMMANDE, multi, r -> new LigneFixe(date(r[0]), (String) r[1],
                zero(montant(r[2])), zero(montant(r[3])), zero(montant(r[4]))));
        Map<UUID, EntreesSimulation> entrees = new HashMap<>();
        for (PrjMarket lot : lots) {
            UUID id = lot.getId();
            CalculLot calcul = calculs.get(id);
            EntreesLot.Drapeaux d = calcul.drapeaux();
            entrees.put(id, new EntreesSimulation(new EntreesSimulation.Etat(d.multi(), d.annule(), d.annule() || d.termine(),
                    d.gestionnaire(), d.blocage()), planning(lot), calcul.montantsPrevisions(), calcul.retenuesPrevisions(),
                    prevision.getOrDefault(id, List.of()), base.getOrDefault(id, List.of()), echeances.getOrDefault(id, List.of()),
                    reels.getOrDefault(id, List.of()), avances.getOrDefault(id, List.of()), avenants.getOrDefault(id, List.of()),
                    commandes.getOrDefault(id, List.of())));
        }
        return entrees;
    }

    private static EntreesSimulation.Planning planning(PrjMarket lot) {
        LocalDate debutBase = jour(lot.getStartDateManual());
        int dureeBase = entier(lot.getDelayManual());
        LocalDate finBase = lot.getEndDateManual() != null || debutBase == null ? jour(lot.getEndDateManual())
                : debutBase.plusDays(dureeBase);
        return new EntreesSimulation.Planning(jour(lot.getStartDate()), entier(lot.getDelay()), debutBase, dureeBase, finBase,
                jour(lot.getContractSignatureDate()), entier(lot.getPaymentDelay()), lot.getWarrantyInMonths());
    }

    private Map<UUID, List<Point>> points(TablePoints table, List<UUID> lots) {
        return parLot(table.lireSimulation, lots, r -> new Point(date(r[0]), zero(montant(r[1])), zero(montant(r[2]))));
    }

    private Map<UUID, List<DecompteReel>> reels(String sql, List<UUID> lots,
                                                Map<UUID, AvancementDecomptes.Avancement> parDecompte) {
        return parLot(sql, lots, r -> {
            AvancementDecomptes.Avancement a = parDecompte.get(uuid(r[0]));
            RetenuesStockees retenues = r.length > 3 ? new RetenuesStockees(retenue(r, 3), retenue(r, 7), retenue(r, 11),
                    retenue(r, 15), retenue(r, 19)) : RetenuesStockees.AUCUNES;
            return new DecompteReel(date(r[1]), a == null ? ZERO : zero(MontantsLot.fraction(a.avaPct())), zero(montant(r[2])),
                    retenues);
        });
    }

    /** Colonnes {@code i} à {@code i + 3} : taux, plafond, montant, « basé sur le montant ». */
    private static RetenueStockee retenue(Object[] r, int i) {
        return new RetenueStockee(zero(montant(r[i])), zero(montant(r[i + 1])), zero(montant(r[i + 2])),
                r[i + 3] instanceof Boolean b ? b : r[i + 3] instanceof Number n && n.intValue() != 0);
    }

    /** Lignes regroupées par lot (1ʳᵉ colonne, retirée), dans l'ordre de la requête ; aucune requête sans lot (IN () refusé). */
    <T> Map<UUID, List<T>> parLot(String sql, List<UUID> lots, Function<Object[], T> ligne) {
        Map<UUID, List<T>> parLot = new HashMap<>();
        if (lots.isEmpty()) {
            return parLot;
        }
        for (Object brute : em.createNativeQuery(sql).setParameter("lots", lots).getResultList()) {
            Object[] r = (Object[]) brute;
            parLot.computeIfAbsent(uuid(r[0]), k -> new ArrayList<>()).add(ligne.apply(Arrays.copyOfRange(r, 1, r.length)));
        }
        return parLot;
    }

    static LocalDate jour(LocalDateTime date) {
        return date == null ? null : date.toLocalDate();
    }

    private static int entier(Integer valeur) {
        return valeur == null ? 0 : valeur;
    }

    static BigDecimal zero(BigDecimal valeur) {
        return valeur == null ? ZERO : valeur;
    }
}

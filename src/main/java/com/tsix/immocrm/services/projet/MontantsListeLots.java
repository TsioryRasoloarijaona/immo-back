package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.projet.CoutPrevisionnelLot;
import com.tsix.immocrm.domain.projet.CoutPrevisionnelLot.Composantes;
import com.tsix.immocrm.domain.projet.CoutPrevisionnelLot.Paire;
import com.tsix.immocrm.domain.projet.CoutPrevisionnelLot.Taux;
import com.tsix.immocrm.entities.prj.PrjMarket;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Lit en base les composantes de {@link CoutPrevisionnelLot} d'un lot de lots (une requête par composante, 2 000 lots au plus
 * par requête) ; formules des sous-vues de {@code PrjMarketMonitoringView}, voir FEATURE-lots-liste-parite.md R1.
 */
@Component
class MontantsListeLots {

    private static final int LOT_MAX = 2000;
    private static final String LIGNE_HT = "UnitPriceHT * (1 + FODECRatio) * Quantity";
    private static final String LIGNE_TTC = LIGNE_HT + " * (1 + VatRatio)";
    private static final String SOMMES_LIGNES = "SELECT PrjMarketId, SUM(" + LIGNE_HT + "), SUM(" + LIGNE_TTC + ") FROM ";
    private static final String DES_LOTS = " WHERE PrjMarketId IN (:lots) GROUP BY PrjMarketId";

    private static final String BORDEREAU = SOMMES_LIGNES + "PrjMarketLine" + DES_LOTS;
    private static final String ESTIMATION = SOMMES_LIGNES + "PrjMarketLineEstimation" + DES_LOTS;

    // PrjWorkingOutLineView : dépassement d'une ligne = cumul au-delà du bordereau, moins celui des décomptes de rang inférieur.
    private static final String DEPASSEMENTS = """
            WITH L AS (
                SELECT w.PrjMarketId AS Lot, w.BuyDocumentId AS Facture, l.PrjMarketLineId AS Ligne, w.Sorting AS Rang,
                    l.UnitPriceHT * l.Quantity AS B, l.UnitPriceHT * (1 + l.FODECRatio) * l.Quantity AS H,
                    l.UnitPriceHT * (1 + l.FODECRatio) * l.Quantity * (1 + l.VatRatio) AS T,
                    m.UnitPriceHT * m.Quantity AS BM, m.UnitPriceHT * (1 + m.FODECRatio) * m.Quantity AS HM,
                    m.UnitPriceHT * (1 + m.FODECRatio) * m.Quantity * (1 + m.VatRatio) AS TM
                FROM PrjWorkingOutLine l
                JOIN PrjWorkingOut w ON w.PKey = l.PrjWorkingOutId
                JOIN PrjMarketLine m ON m.PKey = l.PrjMarketLineId
                WHERE w.PrjMarketId IN (:lots)),
            C AS (
                SELECT L.*,
                    SUM(B) OVER (PARTITION BY Ligne ORDER BY Rang RANGE UNBOUNDED PRECEDING) - SUM(B) OVER (PARTITION BY Ligne, Rang) AS BO,
                    SUM(H) OVER (PARTITION BY Ligne ORDER BY Rang RANGE UNBOUNDED PRECEDING) - SUM(H) OVER (PARTITION BY Ligne, Rang) AS HO,
                    SUM(T) OVER (PARTITION BY Ligne ORDER BY Rang RANGE UNBOUNDED PRECEDING) - SUM(T) OVER (PARTITION BY Ligne, Rang) AS TO_
                FROM L),
            D AS (
                SELECT Lot, Facture,
                    IIF(BO + B > BM, H + HO - HM, 0) - IIF(BO > BM, HO - HM, 0) AS DH,
                    IIF(BO + B > BM, T + TO_ - TM, 0) - IIF(BO > BM, TO_ - TM, 0) AS DT
                FROM C)
            SELECT Lot, SUM(DH), SUM(DT), SUM(IIF(Facture IS NULL, 0, DH)) FROM D GROUP BY Lot""";

    private static final String SUPPLEMENTAIRES = """
            SELECT w.PrjMarketId, SUM(e.UnitPriceHT * (1 + e.FODECRatio) * e.Quantity),
                SUM(e.UnitPriceHT * (1 + e.FODECRatio) * e.Quantity * (1 + e.VatRatio)),
                SUM(IIF(w.BuyDocumentId IS NULL, 0, e.UnitPriceHT * (1 + e.FODECRatio) * e.Quantity))
            FROM PrjWorkingOutLineExtra e JOIN PrjWorkingOut w ON w.PKey = e.PrjWorkingOutId
            WHERE w.PrjMarketId IN (:lots) GROUP BY w.PrjMarketId""";

    private static final String DECOMPTES = """
            SELECT w.PrjMarketId, SUM(w.FluctuationsToPayHT), SUM(w.FluctuationsToPay),
                (SELECT SUM(r.Amount) FROM PrjWorkingOutRetainedLine r JOIN PrjWorkingOut w2 ON w2.PKey = r.PrjWorkingOutId
                 WHERE w2.PrjMarketId = w.PrjMarketId)
            FROM PrjWorkingOut w WHERE w.PrjMarketId IN (:lots) GROUP BY w.PrjMarketId""";

    // Régularisations : hors deux genres de pièce exclus par la vue ; NULL exclu comme dans la vue (NOT IN).
    private static final String PIECES = """
            SELECT d.PrjMarketId,
                SUM(IIF(d.CanBePayed = 1 AND d.StrEntityId NOT IN ('A71B4F91-AEFC-4C70-87F7-82A6C7365CB5',
                    'E7999F70-7507-40D9-AA78-A2825C42B602'),
                    (ISNULL(d.AmountRegul, 0) + ISNULL(d.FiscalStamp, 0)) * IIF(d.DocType = 'REFUND', -1, 1), 0)),
                SUM(IIF(d.DocType = 'INVOICE' AND d.StrEntityId <> 'E7999F70-7507-40D9-AA78-A2825C42B622', d.AmountHT, 0)),
                SUM(IIF(d.DocType = 'REFUND', d.AmountHT, 0)),
                SUM(IIF(d.DocType = 'ORDER' AND s.IsOkForPayement = 1, d.AmountHT, 0)),
                SUM(IIF(d.DocType = 'ORDER' AND s.IsOkForPayement = 1, d.AmountTTC, 0)),
                SUM(IIF(d.BuyOrderId IS NULL AND d.DocType IN ('INVOICE', 'REFUND'),
                    d.AmountHT * IIF(d.DocType = 'REFUND', -1, 1), 0)),
                SUM(IIF(d.BuyOrderId IS NULL AND d.DocType IN ('INVOICE', 'REFUND'),
                    d.AmountTTC * IIF(d.DocType = 'REFUND', -1, 1), 0))
            FROM BuyDocument d LEFT JOIN BuyDocumentStatus s ON s.PKey = d.BuyDocumentStatusId
            WHERE d.PrjMarketId IN (:lots) GROUP BY d.PrjMarketId""";

    // Lot multi-fournisseur : dépassement d'une commande validée (comme l'offert) = Σ de ses factures au-delà de son montant.
    private static final String DEPASSEMENTS_COMMANDES = """
            SELECT o.PrjMarketId, SUM(IIF(f.HT > o.AmountHT, f.HT - o.AmountHT, 0)),
                SUM(IIF(f.HT - o.AmountHT <= 0, 0, f.TTC - o.AmountTTC))
            FROM BuyDocument o
            JOIN BuyDocumentStatus s ON s.PKey = o.BuyDocumentStatusId AND s.IsOkForPayement = 1
            JOIN (SELECT BuyOrderId, SUM(AmountHT) AS HT, SUM(AmountTTC) AS TTC FROM BuyDocument
                  WHERE DocType = 'INVOICE' AND BuyOrderId IS NOT NULL GROUP BY BuyOrderId) f ON f.BuyOrderId = o.PKey
            WHERE o.DocType = 'ORDER' AND o.PrjMarketId IN (:lots) GROUP BY o.PrjMarketId""";

    private static final String REGLE = """
            SELECT d.PrjMarketId, SUM(l.Amount) FROM StlSettlementLine l JOIN BuyDocument d ON d.PKey = l.BuyDocumentId
            WHERE d.PrjMarketId IN (:lots) GROUP BY d.PrjMarketId""";

    private static final BigDecimal[] RIEN = new BigDecimal[8];

    private final EntityManager em;

    MontantsListeLots(EntityManager em) {
        this.em = em;
    }

    Map<UUID, CoutPrevisionnelLot> calculer(List<PrjMarket> marches) {
        Map<UUID, CoutPrevisionnelLot> parLot = new HashMap<>();
        for (int debut = 0; debut < marches.size(); debut += LOT_MAX) {
            List<PrjMarket> tranche = marches.subList(debut, Math.min(marches.size(), debut + LOT_MAX));
            parLot.putAll(calculerTranche(tranche));
        }
        return parLot;
    }

    private Map<UUID, CoutPrevisionnelLot> calculerTranche(List<PrjMarket> marches) {
        List<UUID> ids = marches.stream().map(PrjMarket::getId).toList();
        Map<UUID, BigDecimal[]> bordereau = sommes(BORDEREAU, ids);
        Map<UUID, BigDecimal[]> estimation = sommes(ESTIMATION, ids);
        Map<UUID, BigDecimal[]> depassements = sommes(DEPASSEMENTS, ids);
        Map<UUID, BigDecimal[]> supplementaires = sommes(SUPPLEMENTAIRES, ids);
        Map<UUID, BigDecimal[]> decomptes = sommes(DECOMPTES, ids);
        Map<UUID, BigDecimal[]> pieces = sommes(PIECES, ids);
        Map<UUID, BigDecimal[]> commandes = sommes(DEPASSEMENTS_COMMANDES, ids);
        Map<UUID, BigDecimal[]> regle = sommes(REGLE, ids);
        return marches.stream().collect(Collectors.toMap(PrjMarket::getId, m -> {
            Function<Map<UUID, BigDecimal[]>, BigDecimal[]> de = s -> s.getOrDefault(m.getId(), RIEN);
            boolean multi = Boolean.TRUE.equals(m.getIsMultiSupplier());
            BigDecimal[] p = de.apply(pieces);
            BigDecimal[] w = de.apply(depassements);
            BigDecimal[] e = de.apply(supplementaires);
            BigDecimal[] d = de.apply(decomptes);
            Paire offert = multi ? new Paire(p[4], p[5]) : paire(de.apply(bordereau), 1);
            Paire depassement = multi ? paire(de.apply(commandes), 1) : paire(w, 1);
            Paire extras = multi ? new Paire(p[6], p[7]) : paire(e, 1);
            return CoutPrevisionnelLot.calculer(new Composantes(multi, offert, estime(m, de.apply(estimation)), depassement,
                    extras, paire(d, 1), taux(m), d[3], p[1], de.apply(regle)[1], p[2],
                    multi ? depassement.ht() : w[3], multi ? extras.ht() : e[3], p[3]));
        }));
    }

    /** {@code EstimatedPrice*} : Σ bordereau estimatif s'il est non nul, sinon le forfait saisi. */
    private static Paire estime(PrjMarket m, BigDecimal[] lignes) {
        Paire estimation = paire(lignes, 1);
        return estimation.ht().signum() != 0 ? estimation
                : new Paire(m.getEstimatedPriceHTManual(), m.getEstimatedPriceTTCManual());
    }

    private static Taux taux(PrjMarket m) {
        return new Taux(m.getProportionRatio(), m.getInsuranceDECRatio(), m.getInsuranceDECAmount(), m.getInsuranceTRCRatio(),
                m.getInsuranceTRCAmount());
    }

    private static Paire paire(BigDecimal[] v, int i) {
        return new Paire(v[i], v[i + 1]);
    }

    private Map<UUID, BigDecimal[]> sommes(String sql, List<UUID> ids) {
        List<?> lignes = em.createNativeQuery(sql).setParameter("lots", ids).getResultList();
        Map<UUID, BigDecimal[]> parLot = new HashMap<>();
        for (Object ligne : lignes) {
            Object[] r = (Object[]) ligne;
            BigDecimal[] v = new BigDecimal[RIEN.length];
            for (int i = 1; i < r.length; i++) {
                v[i] = r[i] == null ? null : new BigDecimal(r[i].toString());
            }
            parLot.put(UUID.fromString(String.valueOf(r[0])), v);
        }
        return parLot;
    }
}

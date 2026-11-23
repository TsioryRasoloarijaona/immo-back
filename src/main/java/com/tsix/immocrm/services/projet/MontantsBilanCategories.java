package com.tsix.immocrm.services.projet;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.services.projet.travaux.ValeursNatives.decimal;
import static com.tsix.immocrm.services.projet.travaux.ValeursNatives.uuid;

/** Pièces payables des lots ventilées par catégorie de la pièce (R1 {@code PrjTdbBilan}, FEATURE-n5b-b-rapports-projet.md). */
@Component
class MontantsBilanCategories {

    private static final int LOT_MAX = 2000;
    // Réglé = toutes les lignes de règlement de la pièce, comme MontantsListeLots.REGLEMENTS.
    private static final String SQL_PIECES = """
            SELECT d.PrjMarketId, vc.StlCategoryId, SUM(d.AmountTTC * vc.VentilationRate),
                   SUM(d.AmountToPay * vc.VentilationRate), SUM(ISNULL(r.R, 0) * vc.VentilationRate)
            FROM BuyDocument d JOIN BuyVentilationCategory vc ON vc.BuyDocumentId = d.PKey
            OUTER APPLY (SELECT SUM(l.Amount) AS R FROM StlSettlementLine l WHERE l.BuyDocumentId = d.PKey) r
            WHERE d.CanBePayed = 1 AND d.PrjMarketId IN (:lots)
            GROUP BY d.PrjMarketId, vc.StlCategoryId""";

    private static final String SQL_CUMULS = """
            SELECT PrjMarketId, SUM(AmountTTC) FROM BuyDocument WHERE CanBePayed = 1 AND PrjMarketId IN (:lots)
            GROUP BY PrjMarketId""";

    private final EntityManager em;

    MontantsBilanCategories(EntityManager em) {
        this.em = em;
    }

    /** Cumul travaux, à payer et réglé d'un lot pour une catégorie. */
    record Pieces(UUID lot, UUID categorie, BigDecimal cumul, BigDecimal aPayer, BigDecimal regle) {
    }

    List<Pieces> pieces(List<UUID> lots) {
        return lire(SQL_PIECES, lots).stream()
                .map(r -> new Pieces(uuid(r[0]), uuid(r[1]), decimal(r[2]), decimal(r[3]), decimal(r[4]))).toList();
    }

    /** Cumul travaux total (non ventilé) par lot. */
    Map<UUID, BigDecimal> cumuls(List<UUID> lots) {
        Map<UUID, BigDecimal> parLot = new HashMap<>();
        lire(SQL_CUMULS, lots).forEach(r -> parLot.put(uuid(r[0]), decimal(r[1])));
        return parLot;
    }

    private List<Object[]> lire(String sql, List<UUID> lots) {
        List<Object[]> lignes = new ArrayList<>();
        for (int i = 0; i < lots.size(); i += LOT_MAX) {
            for (Object o : em.createNativeQuery(sql)
                    .setParameter("lots", lots.subList(i, Math.min(lots.size(), i + LOT_MAX))).getResultList()) {
                lignes.add((Object[]) o);
            }
        }
        return lignes;
    }
}

package com.tsix.immocrm.fixtures;

import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureLotFiche.jour;
import static com.tsix.immocrm.fixtures.FixtureLotPrevisions.ENTITE_DECOMPTE;

/**
 * Lot L3a-ter (FEATURE-lot-previsions-reel.md §A, §C) : réel d'un lot = bordereau, décompte facturé (avancement = Σ HT des
 * décomptes / HT du bordereau), taux stockés du décompte, avances datées. SQL natif lié ; facture avant décompte (FK de la CI).
 * Appeler {@link FixtureLotPrevisions#preparer()} avant : la facture porte le genre {@code PrjInvoiceDebitWorkingOut}.
 */
@Component
public class FixturePrevisionsReel {

    /** Taux stockés d'un décompte ({@code PrjWorkingOut}), fractions 0-1 ; plafonds DEC / TRC en montant. */
    public record TauxStockes(String avance, String garantie, String prorata, String dec, String decMax, String trc,
                              String trcMax) {

        public static TauxStockes de(String avance, String garantie) {
            return new TauxStockes(avance, garantie, "0", "0", "0", "0", "0");
        }
    }

    private final EntityManager em;
    private final JdbcTemplate jdbc;

    public FixturePrevisionsReel(EntityManager em, JdbcTemplate jdbc) {
        this.em = em;
        this.jdbc = jdbc;
    }

    private static String s(UUID id) {
        return id == null ? null : id.toString();
    }

    private static BigDecimal n(String valeur) {
        return valeur == null ? null : new BigDecimal(valeur);
    }

    private void maj(String sql, Object... parametres) {
        em.flush();
        jdbc.update(sql, parametres);
        em.clear();
    }

    /** Ligne de bordereau unique : quantité 1, PU HT {@code ht}, TVA 20 % → marché ajusté HT = {@code ht}. */
    public UUID bordereau(UUID lot, String ht) {
        UUID id = UUID.randomUUID();
        BigDecimal pu = n(ht);
        maj("""
                INSERT INTO PrjMarketLine (PKey, Code, Description, PrjMarketId, Sorting, Unity, Quantity, UnitPriceHT,
                    VatRatio, UnitPriceTTC, FODECRatio, IsStock) VALUES (?, '1', 'Ligne 1', ?, 1, 'U', 1, ?, 0.2, ?, 0, 0)""",
                s(id), s(lot), pu, pu.multiply(new BigDecimal("1.2")));
        return id;
    }

    /**
     * Décompte réel facturé du lot, daté {@code date} : facture {@code INVOICE} payable du lot (genre décompte, même date,
     * HT = {@code ht}, TTC = à payer = {@code factureTTC}), puis décompte HT {@code ht}, TTC = net à payer = {@code ttc},
     * retenues stockées à 0, taux stockés {@code taux}.
     */
    public UUID decompteFacture(UUID lot, String code, int numero, LocalDate date, String ht, String ttc, String factureTTC,
                                TauxStockes taux) {
        UUID facture = UUID.randomUUID();
        maj("""
                INSERT INTO BuyDocument (PKey, Code, DocType, ReferenceType, DocDate, MaturityDate, PrjMarketId, StrEntityId,
                    AmountHT, AmountVat, AmountTTC, AmountToPay, CanBePayed)
                VALUES (?, ?, 'INVOICE', '', ?, ?, ?, ?, ?, ?, ?, ?, 1)""",
                s(facture), "F-" + code, jour(date), jour(date), s(lot), s(ENTITE_DECOMPTE), n(ht),
                n(factureTTC).subtract(n(ht)), n(factureTTC), n(factureTTC));
        UUID id = UUID.randomUUID();
        List<Object> v = new ArrayList<>(List.of(s(id), code, numero, jour(date), s(lot), s(facture), n(ht), n(ttc), n(ttc),
                n(taux.avance()), n(taux.garantie()), n(taux.prorata()), n(taux.dec()), n(taux.decMax()), n(taux.trc()),
                n(taux.trcMax())));
        maj("""
                INSERT INTO PrjWorkingOut (PKey, Code, Sorting, DocDate, PrjMarketId, BuyDocumentId, AmountHT, AmountTTC,
                    AmountToPay, AdvanceRatio, WarrantyRatio, proportionRatio, insuranceDECRatio, InsuranceDECAmountMax,
                    insuranceTRCRatio, InsuranceTRCAmountMax, AdvanceAmount, WorkingOutAdvanceAmount, WarrantyAmount,
                    proportionAmount, insuranceDECAmount, insuranceTRCAmount, nonRefundableRetainer, extraAmount,
                    ProrataRefundAmount, StockAmountToPay, FluctuationsToPay, AdvanceBasedOnAmount, WarrantyBasedOnAmount,
                    ProrataBasedOnAmount, DECBasedOnAmount, TRCBasedOnAmount)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)""",
                v.toArray());
        return id;
    }

    /** {@code WorkingOutAdvanceAmount} du décompte : avance sur décompte déjà amortie par lui. */
    public void avanceAmortie(UUID decompte, String montant) {
        maj("UPDATE PrjWorkingOut SET WorkingOutAdvanceAmount = ? WHERE PKey = ?", n(montant), s(decompte));
    }

    /** Pièce à payer d'avance du lot ({@code MARKET_ADVANCE} ou {@code WORKINGOUT_ADVANCE}), datée, payable, HT = TTC. */
    public UUID avance(UUID lot, String code, String reference, LocalDate date, String ttc) {
        UUID id = UUID.randomUUID();
        maj("""
                INSERT INTO BuyDocument (PKey, Code, DocType, ReferenceType, DocDate, MaturityDate, PrjMarketId, AmountHT,
                    AmountVat, AmountTTC, AmountToPay, CanBePayed)
                VALUES (?, ?, 'DOCUMENT_TO_PAY', ?, ?, ?, ?, ?, 0, ?, ?, 1)""",
                s(id), code, reference, jour(date), jour(date), s(lot), n(ttc), n(ttc), n(ttc));
        return id;
    }
}

package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.fixtures.FixtureTerrains;
import jakarta.persistence.Tuple;
import org.assertj.core.api.SoftAssertions;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;

/**
 * Colonnes attendues d'une ligne relue en SQL natif (FEATURE-achat-regle-commune.md R9) : comparaison colonne par colonne,
 * typée par la valeur attendue (UUID, montant, entier, horodatage, texte, booléen, NULL).
 */
final class ColonnesEcrites {

    /** Montants à 0 d'une pièce écrite par les deux écrivains (§2 « 23 montants »). */
    static final List<String> MONTANTS_A_ZERO = List.of("AmountVat", "FiscalStamp", "AmountRegul", "StockAmountToPay",
            "StockAmountToPayHT", "WorkingOutAdvanceAmount", "RetainedAmount", "ProrataRefundAmount", "FluctuationsToPay",
            "FluctuationsToPayHT", "AdvanceRatio", "AdvanceAmount", "AdvanceRest", "WarrantyRatio", "WarrantyAmount",
            "ProportionRatio", "ProportionAmount", "InsuranceDECRatio", "InsuranceDECAmount", "InsuranceDECRest",
            "InsuranceTRCRatio", "InsuranceTRCAmount", "InsuranceTRCRest");

    /** Colonnes que ni l'un ni l'autre écrivain ne remplit. */
    static final List<String> NULLES = List.of("ComFolderId", "PrjProjectId", "PrjMarketId", "BuyFolderId", "BuyOrderId",
            "BuyQuoteId", "BuyInvoiceId", "InvoiceSourceId", "RntFolderId", "SynFolderId", "State", "IsRejected",
            "AmountToPay_OLD", "StatusValidityDate");

    private final Map<String, Object> attendu = new LinkedHashMap<>();

    ColonnesEcrites avec(String colonne, Object valeur) {
        attendu.put(colonne, valeur);
        return this;
    }

    ColonnesEcrites montantsAZero() {
        MONTANTS_A_ZERO.forEach(c -> attendu.put(c, BigDecimal.ZERO));
        return this;
    }

    ColonnesEcrites nulles(List<String> colonnes) {
        colonnes.forEach(c -> attendu.put(c, null));
        return this;
    }

    void verifier(Tuple ligne, String quoi) {
        SoftAssertions soft = new SoftAssertions();
        attendu.forEach((colonne, valeur) -> comparer(soft, quoi + "." + colonne, ligne.get(colonne), valeur));
        soft.assertAll();
    }

    private static void comparer(SoftAssertions soft, String nom, Object lu, Object valeur) {
        switch (valeur) {
            case null -> soft.assertThat(lu).as(nom).isNull();
            case UUID u -> soft.assertThat(guid(lu)).as(nom).isEqualTo(guid(u));
            case BigDecimal b -> soft.assertThat(decimal(lu)).as(nom).isNotNull().isEqualByComparingTo(b);
            case Integer i -> soft.assertThat(lu == null ? null : ((Number) lu).intValue()).as(nom).isEqualTo(i);
            case LocalDateTime d -> soft.assertThat(FixtureTerrains.horodatage(lu)).as(nom).isEqualTo(d);
            default -> soft.assertThat(lu).as(nom).isEqualTo(valeur);
        }
    }
}

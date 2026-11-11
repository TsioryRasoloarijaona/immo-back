package com.tsix.immocrm.domain.achat;

import java.math.BigDecimal;
import java.util.stream.Stream;

/** Net à payer d'une pièce d'achat, règle OMRAN {@code Invoice.cs:TotalRetainedAmount} (FEATURE-achat-regle-commune.md §1.5). */
public final class NetAPayerAchat {

    /** Composantes du net, noms des colonnes {@code BuyDocument} ; {@code avoirsNonFinanciers} = Σ des avoirs non financiers. */
    public record Composantes(BigDecimal amountRegul, BigDecimal fiscalStamp, BigDecimal stockAmountToPay,
                              BigDecimal prorataRefundAmount, BigDecimal fluctuationsToPay, BigDecimal proportionAmount,
                              BigDecimal warrantyAmount, BigDecimal advanceAmount, BigDecimal insuranceTRCAmount,
                              BigDecimal insuranceDECAmount, BigDecimal workingOutAdvanceAmount, BigDecimal retainedAmount,
                              BigDecimal avoirsNonFinanciers) {

        private static final BigDecimal Z = BigDecimal.ZERO;
        public static final Composantes AUCUNE = new Composantes(Z, Z, Z, Z, Z, Z, Z, Z, Z, Z, Z, Z, Z);
    }

    private NetAPayerAchat() {
    }

    public static BigDecimal calculer(BigDecimal ttc, Composantes c) {
        BigDecimal ajouts = Stream.of(c.amountRegul(), c.fiscalStamp(), c.stockAmountToPay(), c.prorataRefundAmount(),
                c.fluctuationsToPay()).reduce(ttc, BigDecimal::add);
        return Stream.of(c.proportionAmount(), c.warrantyAmount(), c.advanceAmount(), c.insuranceTRCAmount(),
                c.insuranceDECAmount(), c.workingOutAdvanceAmount(), c.retainedAmount(), c.avoirsNonFinanciers())
                .reduce(ajouts, BigDecimal::subtract);
    }
}

package com.tsix.immocrm.domain.projet;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Net à payer d'un décompte recalculé depuis ses montants ; seul calcul du net, de l'écart et de son seuil.
 * Un terme NULL vaut 0. Voir FEATURE-projet-lots-decomptes.md §2.
 */
public record NetAPayerDecompte(BigDecimal recalcule, BigDecimal ecart, boolean significatif) {

    static final BigDecimal SEUIL = new BigDecimal("0.02");

    /** Montants d'un décompte, dans l'ordre de la formule. */
    public record Montants(BigDecimal ttc, BigDecimal avance, BigDecimal avanceDecompte, BigDecimal retenueGarantie,
                           BigDecimal retenuesNonRemboursables, BigDecimal travauxSupplementaires,
                           BigDecimal remboursementProrata, BigDecimal stockAPayer, BigDecimal fluctuationsAPayer,
                           BigDecimal netStocke) {
    }

    public static NetAPayerDecompte de(Montants m) {
        BigDecimal net = z(m.ttc())
                .subtract(z(m.avance()))
                .subtract(z(m.avanceDecompte()))
                .subtract(z(m.retenueGarantie()))
                .subtract(z(m.retenuesNonRemboursables()))
                .add(z(m.travauxSupplementaires()))
                .add(z(m.remboursementProrata()))
                .add(z(m.stockAPayer()))
                .add(z(m.fluctuationsAPayer()))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal ecart = z(m.netStocke()).subtract(net);
        return new NetAPayerDecompte(net, ecart, ecart.abs().compareTo(SEUIL) > 0);
    }

    private static BigDecimal z(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}

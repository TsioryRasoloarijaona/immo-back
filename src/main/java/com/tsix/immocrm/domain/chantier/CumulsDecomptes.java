package com.tsix.immocrm.domain.chantier;

import java.math.BigDecimal;

/** Sommes des montants <b>stockés</b> des décomptes d'un lot (jamais recalculés). */
public record CumulsDecomptes(long nombre, BigDecimal ht, BigDecimal ttc, BigDecimal travauxSupplementaires,
                              BigDecimal retenueGarantie, BigDecimal netAPayer) {

    public static final CumulsDecomptes AUCUN = new CumulsDecomptes(0, BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

    public static CumulsDecomptes de(long nombre, BigDecimal ht, BigDecimal ttc, BigDecimal extras,
                                     BigDecimal rg, BigDecimal net) {
        return new CumulsDecomptes(nombre, Montants.arrondi(ht), Montants.arrondi(ttc), Montants.arrondi(extras),
                Montants.arrondi(rg), Montants.arrondi(net));
    }
}

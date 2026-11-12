package com.tsix.immocrm.domain.achat;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Une ligne de {@code BuySupplierPositionView} : un crédit ou un débit daté (date nulle possible). */
public record MouvementPosition(LocalDate date, BigDecimal montant, boolean credit) {
}

package com.tsix.immocrm.dto.dossier;

import java.math.BigDecimal;

/**
 * One category row in {@link MontantsDossierDto#parCategorie()}. {@code regle}/{@code reste} are
 * computed from the linked {@code StlSettlementLine}s of every échéance in the category, never
 * stored.
 */
public record CategorieMontantDto(
        String categorie,
        BigDecimal montant,
        int nombreLignes,
        BigDecimal regle,
        BigDecimal reste
) {
}

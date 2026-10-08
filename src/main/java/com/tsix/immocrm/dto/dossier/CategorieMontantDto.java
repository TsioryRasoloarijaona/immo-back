package com.tsix.immocrm.dto.dossier;

import java.math.BigDecimal;

/** {@code regle}/{@code reste} : calculés depuis les {@code StlSettlementLine} des échéances de la catégorie, jamais stockés. */
public record CategorieMontantDto(
        String categorie,
        BigDecimal montant,
        int nombreLignes,
        BigDecimal regle,
        BigDecimal reste
) {
}

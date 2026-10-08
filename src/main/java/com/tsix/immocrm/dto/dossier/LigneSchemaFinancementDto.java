package com.tsix.immocrm.dto.dossier;

import java.math.BigDecimal;

/**
 * Une ligne du schéma de financement simulé, telle qu'elle serait persistée en échéance.
 * {@code exclureDuPrix} = {@code ComSettlementCategory.ExcludeFromPrice} : vrai pour FRAIS SYNDIC, qui
 * n'entre pas dans le TTC soumis à la règle de prix.
 */
public record LigneSchemaFinancementDto(
        String categorie,
        BigDecimal montant,
        Boolean exclureDuPrix
) {
}

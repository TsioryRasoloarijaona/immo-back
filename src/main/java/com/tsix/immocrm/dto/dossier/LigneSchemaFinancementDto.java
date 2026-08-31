package com.tsix.immocrm.dto.dossier;

import java.math.BigDecimal;

/**
 * One computed line of the "Schéma de financement" preview — same shape the line would have once
 * persisted as a {@code ComDocument} échéance if the dossier were actually created (see
 * {@code DossierService#genererSchemaFinancement}).
 *
 * @param categorie     code {@code ComSettlementCategory} — "AUTO-FINANCEMENT", "AVANCE" ou
 *                      "FRAIS SYNDIC".
 * @param montant       montant calculé de la ligne.
 * @param exclureDuPrix reflète {@code ComSettlementCategory.ExcludeFromPrice} — {@code true} pour
 *                      FRAIS SYNDIC (n'entre pas dans le TTC soumis à la règle de prix).
 */
public record LigneSchemaFinancementDto(
        String categorie,
        BigDecimal montant,
        Boolean exclureDuPrix
) {
}

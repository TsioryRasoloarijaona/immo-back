package com.tsix.immocrm.dto.referentiel;

import java.util.UUID;

/**
 * {@code StlSettlementType} — table distincte de {@code ComPaymentMethod} (modes de paiement du
 * dossier), sans lien avec un règlement.
 */
public record TypeReglementDto(
        UUID id,
        String code,
        String description,
        /** {@code StlSettlementType.IsBankMandatory} — indicatif, non vérifié côté serveur. */
        Boolean banqueObligatoire,
        /** {@code StlSettlementType.IsReferenceMandatory} — vérifié côté serveur (400 si absente). */
        Boolean referenceObligatoire
) {
}

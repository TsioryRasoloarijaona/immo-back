package com.tsix.immocrm.dto.dossier;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response of {@code POST /api/dossiers/simulation-financement} — le "Schéma de financement"
 * calculé, non persisté. Consulter aussi {@code GET /api/dossiers/{id}/echeancier} pour la
 * version réellement persistée une fois le dossier créé.
 */
public record SchemaFinancementDto(
        List<LigneSchemaFinancementDto> lignes,
        BigDecimal totalGeneral
) {
}

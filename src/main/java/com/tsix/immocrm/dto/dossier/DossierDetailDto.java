package com.tsix.immocrm.dto.dossier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Detail shape for {@code GET /api/dossiers/{id}}, including {@code etapesAtteignables}
 * (computed from {@link com.tsix.immocrm.domain.TransitionsValidees}, never stored) and the
 * dossier's linked articles (plural — confirmed a Dossier can have several).
 *
 * NOTE: field naming is not yet backed by a shared API spec doc
 * (API-endpoints-vente-prospection-patrimoine.md is not present in this repo) — derived
 * directly from {@code ComFolder}. Adjust once a spec is available.
 */
public record DossierDetailDto(
        UUID id,
        String code,
        LocalDateTime dateDocument,
        String statut,
        List<String> etapesAtteignables,
        UUID clientId,
        String clientNom,
        UUID commercialId,
        String commercialNom,
        UUID avocatId,
        String avocatNom,
        List<ArticleSummaryDto> articles,
        BigDecimal prixAvantRemise,
        BigDecimal remise,
        BigDecimal remiseMontant,
        BigDecimal prixApresRemise,
        BigDecimal prix,
        BigDecimal montantAvance,
        BigDecimal totalAPayer,
        String notes,
        BigDecimal probabiliteFinalisation,
        String modePaie
) {
}

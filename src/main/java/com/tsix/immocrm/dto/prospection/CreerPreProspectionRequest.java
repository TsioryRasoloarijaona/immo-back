package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** Voir {@code instructions/FEATURE-creation-pre-prospection.md} §3. */
public record CreerPreProspectionRequest(
        @Schema(example = "Karim Bennani", requiredMode = Schema.RequiredMode.REQUIRED)
        String nom,
        @Schema(example = "0612345678")
        String telephone,
        @Schema(description = "Email (`TempEMail`, 255 max).", example = "k.bennani@example.com")
        String email,
        @Schema(description = "Notes libres (`Notes`, 1 000 caractères max).")
        String notes,
        UUID trancheId,
        @Schema(description = "Article/lot visé (StkItem). Optionnel ; exige `trancheId` et doit lui "
                + "appartenir (400 sinon).")
        UUID articleId,
        @Schema(description = "Nature/étape (ComProspectionKind → `ComProspectionKindId`). Optionnel.")
        UUID etapeId,
        @Schema(description = "Source de contact (ComProspectionOrigin). Optionnel.")
        UUID origineId,
        @Schema(description = "Support d'origine (ComProspectionOriginSupport). Optionnel.")
        UUID origineSupportId,
        @Schema(description = "Type (ComActionMessageType → `ComActionMessageTypeId`). Optionnel.")
        UUID typeContactId,
        @Schema(description = "Action commerciale / campagne (ComAction). Optionnel.")
        UUID actionCommercialeId,
        UUID vocationId
) {
}

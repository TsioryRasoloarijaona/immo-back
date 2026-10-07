package com.tsix.immocrm.dto.prospection;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Les cinq champs qu'impose l'écran OMRAN (notes, origine, type, nature, tranche) rendent 400
 * s'ils manquent. Pas de champs de clôture : appeler {@code PATCH .../archiver} ensuite.
 */
public record CreerProspectionRequest(
        @Schema(description = "Prospect (CfgTier) déjà existant à rattacher. Obligatoire.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID prospectId,
        UUID commercialId,
        @Schema(description = "Tranche/projet (CfgTranche → `StkHierarchyId`), champ « Tranche ». "
                + "**Obligatoire** (aligné sur l'écran légataire).",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID trancheId,
        @Schema(description = "Article/lot principal (StkItem). Optionnel ; s'il est fourni il DOIT "
                + "appartenir à `trancheId` (400 sinon).")
        UUID articleId,
        @Schema(description = "Source de contact (ComProspectionOrigin → `ComProspectionOriginId`), "
                + "champ « Source de contact ». **Obligatoire**.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID origineId,
        @Schema(description = "Support d'origine (ComProspectionOriginSupport). Optionnel.")
        UUID origineSupportId,
        @Schema(description = "Vocation (StkVocation), p.ex. VENTE. Optionnel — à ne pas confondre "
                + "avec le champ « Type » qui est `typeContactId`.")
        UUID vocationId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        UUID typeContactId,
        @Schema(description = "Action commerciale (ComAction). Optionnel.")
        UUID actionCommercialeId,
        @Schema(description = "Nature/étape (ComProspectionKind → `ComProspectionKindId`), champ "
                + "« Nature ». **Obligatoire**.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID etapeId,
        BigDecimal surface,
        BigDecimal surfaceJusqua,
        BigDecimal prix,
        BigDecimal prixJusqua,
        BigDecimal avance,
        BigDecimal financementFondsPropres,
        BigDecimal financementCredit,
        BigDecimal financementAutre,
        Boolean ancienClient,
        @Schema(description = "Notes libres, champ « Notes ». **Obligatoire** (non vide) — aligné "
                + "sur l'écran légataire.", requiredMode = Schema.RequiredMode.REQUIRED)
        String notes
) {
}

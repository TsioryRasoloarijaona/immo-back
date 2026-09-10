package com.tsix.immocrm.dto.dossier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Article summary embedded in {@link DossierDetailDto#articles()}.
 *
 * NOTE (confirmed): a Dossier can have several articles — e.g. real dossier
 * "MBR 2022/00205" links both "MB 18 1 04" (main lot) and "MB 18 1 04 TER" (its annex) —
 * so this is a list, never a single article.
 */
public record ArticleSummaryDto(
        UUID id,
        String code,
        String description,
        BigDecimal area,
        BigDecimal price,

        // ------------------------------------------------------------------
        // Contexte patrimonial de l'article — voir
        // instructions/FEATURE-contexte-dossier.md §2.2. Les quatre référentiels sont
        // renseignés sur 100 % des 358 articles de dossier réels.
        // ------------------------------------------------------------------

        @Schema(description = "Identifiant de la catégorie de l'article. 100 %.")
        UUID categorieId,

        @Schema(description = "Code de la catégorie.", example = "APPT")
        String categorieCode,

        @Schema(description = "Libellé de la catégorie.", example = "Appartement")
        String categorieLibelle,

        @Schema(description = "Identifiant du type de l'article. 100 %.")
        UUID typeId,

        @Schema(description = "Code du type.", example = "T3")
        String typeCode,

        @Schema(description = "Libellé du type.")
        String typeLibelle,

        @Schema(description = "Identifiant de l'étage. 100 %.")
        UUID etageId,

        @Schema(description = "Code de l'étage.", example = "RDC")
        String etageCode,

        @Schema(description = "Libellé de l'étage.", example = "Rez de chaussée")
        String etageLibelle,

        @Schema(description = "Identifiant de l'immeuble. 100 %.")
        UUID immeubleId,

        @Schema(description = "Code de l'immeuble.", example = "IMM01")
        String immeubleCode,

        @Schema(description = "Libellé de l'immeuble.")
        String immeubleLibelle
) {
}

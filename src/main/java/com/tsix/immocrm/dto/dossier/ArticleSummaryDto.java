package com.tsix.immocrm.dto.dossier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/** Un article de {@link DossierDetailDto#articles()} : un dossier peut en porter plusieurs (lot et annexe). */
public record ArticleSummaryDto(
        UUID id,
        String code,
        String description,
        BigDecimal area,
        BigDecimal price,

        // Contexte patrimonial — voir FEATURE-contexte-dossier.md §2.2.

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
        String immeubleLibelle,

        // « Tit. foncier » et « Présentation » de l'écran légataire — voir FEATURE-fiche-dossier-alignement-ecran.md §4.

        @Schema(example = "R/12345")
        String titreFoncier,

        @Schema(description = "Code du type de présentation du lot (`StkPresentationType`) — la "
                + "case « Présentation » de l'écran légataire. **23,2 %**.", example = "C")
        String presentationCode,

        @Schema(description = "Libellé du type de présentation. **23,2 %**.")
        String presentationLibelle
) {
}

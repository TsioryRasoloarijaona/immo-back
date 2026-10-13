package com.tsix.immocrm.dto.dossier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Schéma de financement simulé, non persisté. */
public record SchemaFinancementDto(
        List<LigneSchemaFinancementDto> lignes,
        BigDecimal totalGeneral,
        @Schema(example = "1200000.00")
        BigDecimal prixAvantRemise,
        @Schema(example = "60000.00")
        BigDecimal montantRemise,
        @Schema(example = "5")
        BigDecimal remisePercent,
        @Schema(example = "1140000.00")
        BigDecimal prixApresRemise,
        @Schema(description = "Sous-articles libres du lot absents d'articleIds, rattachés d'office à la création et comptés "
                + "dans les prix.")
        List<AnnexeAutomatique> annexesAutomatiques
) {

    @Schema(description = "Sous-article du lot (StkItem.ParentId) rattaché automatiquement, comme le logiciel de bureau.")
    public record AnnexeAutomatique(
            UUID id,
            @Schema(example = "APF-1-08 TER")
            String code,
            String description,
            @Schema(description = "Superficie (m²).", example = "12")
            BigDecimal surface,
            @Schema(description = "Prix de référence TTC compté dans le prix des sous-articles.", example = "50000")
            BigDecimal prix
    ) {
    }
}

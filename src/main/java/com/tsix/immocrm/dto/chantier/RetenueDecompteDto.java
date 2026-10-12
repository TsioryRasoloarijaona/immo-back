package com.tsix.immocrm.dto.chantier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Retenue forfaitaire d'un décompte (PrjWorkingOutRetainedLine).")
public record RetenueDecompteDto(
        @Schema(description = "Identifiant.") UUID id,
        @Schema(description = "Libellé.", example = "FOURNITURE FAUX CADRES") String libelle,
        @Schema(description = "Montant retenu.") BigDecimal montant) {
}

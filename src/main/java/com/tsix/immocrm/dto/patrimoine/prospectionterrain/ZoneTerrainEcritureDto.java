package com.tsix.immocrm.dto.patrimoine.prospectionterrain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Ligne de zone d'un terrain brut (LndLandZone) ; surfaces : mêmes noms que SurfacesTerrain, sans voies.")
public record ZoneTerrainEcritureDto(
        @Schema(description = "LndLandZone.PKey ; absent = nouvelle zone, id d'une autre prospection → 400.") UUID id,
        @Schema(description = "Obligatoire (Identification).") String identification,
        @Schema(description = "Area (m²).") BigDecimal superficie,
        BigDecimal cos,
        @Schema(description = "Emprise = COS × superficie.") BigDecimal superficieCos,
        BigDecimal cuf,
        @Schema(description = "Plancher couvert = superficie × CUF ; donné seul → CUF = plancher / (superficie × COS).")
        BigDecimal superficieCuf,
        BigDecimal coefVendable,
        BigDecimal superficieVendable,
        BigDecimal coefCommerce,
        BigDecimal superficieCommerce,
        BigDecimal coefBureau,
        BigDecimal superficieBureau,
        BigDecimal coefParking,
        BigDecimal superficieParking,
        @Schema(description = "AlinmentStop.") Integer arreteAlignement,
        @Schema(description = "MaxHeight.") Integer hauteurMax,
        @Schema(description = "1000 caractères au plus.") String notes
) {
}

package com.tsix.immocrm.dto.suivicommerciaux;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Objectif de personnes jointes par jour et par commercial.")
public record ObjectifCommercialDto(
        @Schema(description = "Entier de 1 à 1000 ; 60 tant qu'aucun Administrateur ne l'a réglé.", example = "60")
        @NotNull @Min(1) @Max(1000) Integer objectifPersonnesJointesJour) {
}

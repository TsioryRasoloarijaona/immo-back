package com.tsix.immocrm.dto.suivicommerciaux;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** {@code BigDecimal} et non {@code Integer} : Jackson tronquerait 45.7 en 45 avant la validation (400 attendu). */
@Schema(description = "Objectif de personnes jointes par jour et par commercial.")
public record ObjectifCommercialDto(
        @Schema(type = "integer", description = "Entier de 1 à 1000 ; 60 tant qu'aucun Administrateur ne l'a réglé.",
                example = "60")
        @NotNull @Digits(integer = 4, fraction = 0) @Min(1) @Max(1000) BigDecimal objectifPersonnesJointesJour) {

    public static ObjectifCommercialDto de(int valeur) {
        return new ObjectifCommercialDto(BigDecimal.valueOf(valeur));
    }
}

package com.tsix.immocrm.dto.suivicommerciaux;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Grille commercial × jour du suivi des commerciaux et ses totaux.")
public record SyntheseSuiviCommerciauxDto(
        @Schema(description = "Objectif de personnes jointes par jour en vigueur.", example = "60") int objectif,
        @Schema(description = "Une ligne par commercial et par jour de la période.") List<LigneSyntheseSuiviDto> lignes,
        TotauxSynthese totaux) {

    @Schema(description = "Sommes des lignes par commercial, par jour, et générale.")
    public record TotauxSynthese(List<LigneSyntheseSuiviDto> parCommercial, List<LigneSyntheseSuiviDto> parJour,
                                 LigneSyntheseSuiviDto general) {
    }
}

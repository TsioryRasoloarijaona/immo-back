package com.tsix.immocrm.dto.projet;

import com.tsix.immocrm.dto.projet.BordereauDto.TotauxBordereauDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Bordereau estimatif d'un lot (PrjMarketLineEstimation hors stock), en lecture seule.")
public record BordereauEstimatifDto(
        @Schema(description = "Lot (PrjMarket.PKey).") UUID lotId,
        @Schema(description = "Toujours vrai : bordereau verrouillé (cadenas OMRAN).") boolean verrouille,
        @Schema(description = "Groupes du bordereau estimatif.") List<GroupeBordereauDto> groupes,
        @Schema(description = "Lignes, tri par ordre puis code.") List<LigneBordereauDto> lignes,
        @Schema(description = "Totaux des lignes.") TotauxBordereauDto totaux
) {
}

package com.tsix.immocrm.dto.dossier.modifications;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Onglet Modifications : les demandes et les montants du dossier qui en découlent.")
public record ModificationsDto(
        @Schema(description = "Demandes, la plus récente en tête.")
        List<ModificationDto> modifications,
        @Schema(description = "`ComFolder.ModificationsAmount` : Σ prix TTC des demandes acceptées ou réalisées.")
        BigDecimal montantTTC,
        @Schema(description = "`ComFolder.ModificationsAmountHT` : Σ prix HT des demandes acceptées ou réalisées.")
        BigDecimal montantHT,
        @Schema(description = "« Intégrer le montant des modifications dans le calcul du prix de vente » (`PriceWithModification`).")
        boolean integreAuPrix,
        @Schema(description = "Hors prix : montant des modifications − Σ lignes de schéma « Modifications » ; null si intégré au prix.")
        BigDecimal ecartSchema
) {
}

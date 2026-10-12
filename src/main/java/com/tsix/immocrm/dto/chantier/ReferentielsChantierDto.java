package com.tsix.immocrm.dto.chantier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Listes de choix des filtres du module Chantier.")
public record ReferentielsChantierDto(
        @Schema(description = "Types de lot (PrjMarketType), triés par code numérique.") List<ReferentielChantierDto> typesLot,
        @Schema(description = "Statuts de lot, par entité puis ordre.") List<StatutLotDto> statutsLot,
        @Schema(description = "Statuts de facture (BuyDocumentStatus), triés par ordre.")
        List<ReferentielChantierDto> statutsFacture,
        @Schema(description = "Catégories (StlCategory) portées par au moins un lot du périmètre de l'appelant.")
        List<ReferentielChantierDto> categories) {
}

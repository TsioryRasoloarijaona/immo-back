package com.tsix.immocrm.dto.administration.tiers;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Écran Arpent ouvert au clic sur la ligne.")
public record LienOngletTiersDto(
        @Schema(description = "Écran.", allowableValues = {"dossier", "prospection", "encaissement"}) String ecran,
        @Schema(description = "PKey de l'objet ouvert.") UUID id) {
}

package com.tsix.immocrm.dto.achat.config;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Image de la bibliothèque OMRAN (CfgImage), sans ses octets.")
public record ImageStatutDto(
        @Schema(description = "CfgImage.PKey.") UUID id,
        @Schema(description = "Code de l'image.", example = "ENCOURS") String code,
        @Schema(description = "Taille des octets (ImageData), 0 si vide.", example = "4512") long taille
) {
}

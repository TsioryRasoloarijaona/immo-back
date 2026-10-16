package com.tsix.immocrm.dto.modele;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Image déposée pour les modèles : le contenu la cite par `<img data-image=\"id\">`, sans `src`.")
public record ImageModeleDto(
        @Schema(description = "Identifiant de l'image (AppModeleImage.PKey), valeur de `data-image`.")
        UUID id,
        @Schema(description = "Type vérifié sur les octets : `image/png` ou `image/jpeg`.", example = "image/png")
        String contentType,
        @Schema(description = "Taille en octets (500 Ko au plus).", example = "48213")
        long taille) {
}

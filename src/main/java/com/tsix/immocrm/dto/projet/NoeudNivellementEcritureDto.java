package com.tsix.immocrm.dto.projet;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Saisie d'un nœud de nivellement : parentId à la création, code à la modification.")
public record NoeudNivellementEcritureDto(
        @Schema(description = "Création : nœud parent du même projet ; null = nouvelle racine. Ignoré en modification.")
        UUID parentId,
        @Schema(description = "Modification : nouveau code (obligatoire, 255 caractères). Ignoré à la création.",
                maxLength = 255)
        String code) {
}

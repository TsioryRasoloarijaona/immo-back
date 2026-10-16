package com.tsix.immocrm.dto.modele;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Liste répétable (↻) : ses champs ne sont permis que dans un élément `data-repeter=\"cle\"`.")
public record ListeChampsDto(
        @Schema(description = "Clé de la liste, valeur de `data-repeter`.", example = "echeancesSolde")
        String cle,
        @Schema(description = "Libellé français de la liste.", example = "Échéances du solde")
        String libelle,
        @Schema(description = "Champs d'un élément de la liste.")
        List<ChampModeleDto> champs) {
}

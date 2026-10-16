package com.tsix.immocrm.dto.modele;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Groupe de champs simples du catalogue (Dossier, Client, Article…).")
public record GroupeChampsDto(
        @Schema(description = "Libellé du groupe.", example = "Client")
        String libelle,
        @Schema(description = "Champs du groupe, dans l'ordre d'affichage.")
        List<ChampModeleDto> champs) {
}

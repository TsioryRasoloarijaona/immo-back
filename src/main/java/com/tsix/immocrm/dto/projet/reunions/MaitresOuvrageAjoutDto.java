package com.tsix.immocrm.dto.projet.reunions;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Ajout de maîtres d'ouvrage ; un contact déjà présent est ignoré.")
public record MaitresOuvrageAjoutDto(
        @Schema(description = "Contacts (CfgTier.PKey).", requiredMode = Schema.RequiredMode.REQUIRED) List<UUID> contactIds) {
}

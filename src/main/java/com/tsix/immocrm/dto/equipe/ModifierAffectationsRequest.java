package com.tsix.immocrm.dto.equipe;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Ajout et retrait de projets pour un commercial ; jamais un remplacement total.")
public record ModifierAffectationsRequest(

        @Schema(description = "Projets à ajouter (déjà affecté : ignoré).")
        List<UUID> ajouter,

        @Schema(description = "Projets à retirer (non affecté : ignoré).")
        List<UUID> retirer
) {
}

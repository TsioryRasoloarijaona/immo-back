package com.tsix.immocrm.dto.administration;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Passage de statut d'une tâche.")
public record ChangerStatutTacheRequest(
        @Schema(description = "Statut d'arrivée, obligatoire.") UUID statutId,
        @Schema(description = "Commentaire, obligatoire si la transition l'exige (commentaireRequis).") String commentaire) {
}

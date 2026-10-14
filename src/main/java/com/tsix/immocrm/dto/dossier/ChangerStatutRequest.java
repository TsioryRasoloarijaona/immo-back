package com.tsix.immocrm.dto.dossier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * {@code date} : écrite sur la date associée au statut cible, ignorée s'il n'en a pas. {@code note} : va dans
 * {@code ComFolder.Notes}, sauf pour ANNULATION et DESISTEMENT où elle devient {@code CancelDescription}.
 */
@Schema(description = "Changement de statut d'un dossier de vente.")
public record ChangerStatutRequest(
        @Schema(description = "Code du statut cible (StrStatus.Code).", example = "CONCRETISATION")
        String statut,
        @Schema(description = "Date écrite sur la date associée au statut cible ; ignorée s'il n'en a pas.")
        LocalDateTime date,
        @Schema(description = "Note ; la cause, obligatoire, pour ANNULATION et DESISTEMENT.")
        String note,
        @Schema(description = "Administrateur seulement : ne pas vérifier le seuil de versement (tracé dans l'historique).")
        Boolean ignorerSeuil
) {
}

package com.tsix.immocrm.dto.prospection;

import java.util.UUID;

/**
 * Pas de table de transitions : seule l'existence de l'étape est vérifiée. {@code note}, si
 * fourni, écrase {@code ComProspection.Notes} au lieu de s'y ajouter.
 */
public record ChangerEtapeRequest(
        UUID etapeId,
        String note
) {
}

package com.tsix.immocrm.dto.dossier;

import java.time.LocalDateTime;

/**
 * {@code date} : écrite sur la colonne jalon du statut cible ({@link com.tsix.immocrm.domain.EtapeMilestone}),
 * ignorée s'il n'en a pas. {@code note} : va dans {@code ComFolder.Notes}, sauf pour ANNULATION et
 * DESISTEMENT où elle devient {@code ComFolder.CancelDescription} (la cause, alors obligatoire).
 */
public record ChangerStatutRequest(
        String statut,
        LocalDateTime date,
        String note
) {
}

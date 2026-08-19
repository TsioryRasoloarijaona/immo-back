package com.tsix.immocrm.dto.dossier;

/**
 * One reachable next {@code statut} from the Dossier's current status, along with which
 * milestone field (if any) {@code PATCH /api/dossiers/{id}/statut}'s {@code date} will be
 * written to when transitioning there. See {@link com.tsix.immocrm.domain.EtapeMilestone}.
 */
public record EtapeSuivanteDto(
        String statut,
        String champDate,
        String libelleChampDate,
        String confiance
) {
}

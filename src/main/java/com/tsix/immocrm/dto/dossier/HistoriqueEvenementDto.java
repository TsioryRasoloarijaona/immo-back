package com.tsix.immocrm.dto.dossier;

import java.time.LocalDateTime;

/**
 * One entry of {@code GET /api/dossiers/{id}/historique} — a unified, chronological trace of
 * everything that happened on a Dossier: status changes (with their note), échéances created,
 * règlements posted, pièces jointes added. See {@code DossierHistoriqueService}.
 */
public record HistoriqueEvenementDto(
        LocalDateTime date,
        String type,
        String utilisateur,
        String description
) {
    /** {@link #type} values. */
    public static final String STATUT = "STATUT";
    public static final String ECHEANCE = "ECHEANCE";
    public static final String REGLEMENT = "REGLEMENT";
    public static final String PIECE_JOINTE = "PIECE_JOINTE";
}

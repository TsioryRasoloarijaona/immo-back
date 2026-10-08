package com.tsix.immocrm.dto.dossier;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record HistoriqueEvenementDto(
        LocalDateTime date,
        String type,
        @Schema(description = "Auteur de l'événement (libellé du compte). Null sur les lignes legacy sans auteur.")
        String utilisateur,
        String description
) {
    /** Valeurs de {@link #type}. */
    public static final String STATUT = "STATUT";
    public static final String ECHEANCE = "ECHEANCE";
    public static final String REGLEMENT = "REGLEMENT";
    public static final String PIECE_JOINTE = "PIECE_JOINTE";
}

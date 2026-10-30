package com.tsix.immocrm.dto.administration;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Les 10 restrictions d'un rôle, stockées sans être appliquées. Jamais null à la lecture : NULL en base = true.")
public record RestrictionsRoleDto(
        @Schema(description = "Restriction sur l'ajout des fichiers attachés.") Boolean ajoutPiecesJointes,
        @Schema(description = "Restriction sur la modification des fichiers attachés.") Boolean modificationPiecesJointes,
        @Schema(description = "Restriction sur la suppression des fichiers attachés.") Boolean suppressionPiecesJointes,
        @Schema(description = "Restriction sur l'ajout des actions.") Boolean ajoutActions,
        @Schema(description = "Restriction sur la modification des actions.") Boolean modificationActions,
        @Schema(description = "Restriction sur la suppression des actions.") Boolean suppressionActions,
        @Schema(description = "Restriction sur les éditions des rapports, lue par l'accès aux rapports.") Boolean rapports,
        @Schema(description = "Restriction sur les audits.") Boolean audits,
        @Schema(description = "Restriction sur les champs spécifiques.") Boolean champsSpecifiques,
        @Schema(description = "Restriction sur la modification des séquences.") Boolean sequences) {
}

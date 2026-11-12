package com.tsix.immocrm.dto.achat;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Changement de statut d'une pièce d'achat.")
public record TransitionPieceAchatRequest(
        @Schema(description = "Statut d'arrivée (un statutId de actions.transitions) ; obligatoire.") UUID statutId,
        @Schema(description = "Note du passage (≤ 1000 caractères) ; obligatoire si la transition l'exige, facultative sinon.") String note) {
}

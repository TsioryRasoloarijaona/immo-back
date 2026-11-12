package com.tsix.immocrm.dto.achat;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Actions d'écriture ouvertes au compte sur la pièce ; mêmes règles que les routes d'écriture.")
public record ActionsPieceAchatDto(
        @Schema(description = "Droit de suppression, nature supportée, pièce non gérée ailleurs ; liens bloquants connus au "
                + "clic (409).") boolean peutSupprimer,
        @Schema(description = "Facture, droit de modification de facture porté et pièce non gérée ailleurs.")
        boolean peutAffecter,
        @Schema(description = "Statuts d'arrivée accessibles au compte depuis le statut courant, par ordre de transition ; "
                + "vide sans transition.") List<TransitionPermiseAchatDto> transitions) {

    @Schema(description = "Statut d'arrivée d'une transition permise au compte.")
    public record TransitionPermiseAchatDto(
            @Schema(description = "Id du statut d'arrivée (BuyDocumentStatus), à envoyer en statutId.") UUID statutId,
            @Schema(description = "Code du statut d'arrivée.") String statutCode,
            @Schema(description = "Libellé du statut d'arrivée.") String statutDescription,
            @Schema(description = "Vrai si la transition exige une note (NotesMandatory).") boolean noteObligatoire) {
    }
}

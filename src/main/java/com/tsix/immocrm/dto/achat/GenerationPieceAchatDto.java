package com.tsix.immocrm.dto.achat;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Pièce générable depuis celle-ci : ouvre le formulaire prérempli (modele avec sources = cette pièce).")
public record GenerationPieceAchatDto(
        @Schema(description = "Nature de la pièce à générer : ORDER, DELIVRY ou INVOICE.", example = "DELIVRY")
        String natureCible,
        @Schema(description = "Libellé du bouton.", example = "Générer Bon de livraison") String libelle) {
}

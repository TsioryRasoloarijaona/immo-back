package com.tsix.immocrm.dto.dossier.reinitialisation;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Effet de la réinitialisation depuis le stock : état avant et après.")
public record ReinitialisationDto(
        EtatPrixDto avant,
        EtatPrixDto apres,
        @Schema(description = "Vrai si au moins une valeur change ou si un sous-article est ajouté.")
        boolean modifie,
        @Schema(description = "Codes des sous-articles du stock absents du dossier, ajoutés par la réinitialisation.")
        List<String> sousArticlesAjoutes
) {
}

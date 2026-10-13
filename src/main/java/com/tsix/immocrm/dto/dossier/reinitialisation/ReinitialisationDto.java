package com.tsix.immocrm.dto.dossier.reinitialisation;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Effet de la réinitialisation depuis le stock : état avant et après.")
public record ReinitialisationDto(
        EtatPrixDto avant,
        EtatPrixDto apres,
        @Schema(description = "Vrai si au moins une valeur change.")
        boolean modifie
) {
}

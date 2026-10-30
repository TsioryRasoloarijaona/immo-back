package com.tsix.immocrm.dto.administration;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Fiche d'un e-mail : les champs de la ligne, plus la copie et le corps.")
public record EmailDetailDto(
        @JsonUnwrapped LigneEmailDto ligne,
        @Schema(description = "Destinataires en copie.") String copie,
        @Schema(description = "Destinataires en copie cachée.") String copieCachee,
        @Schema(description = "Corps brut, balises comprises : à afficher en texte, jamais à injecter.") String corps,
        @Schema(description = "Vrai si le corps est du HTML (IsBodyHtml).") Boolean html) {
}

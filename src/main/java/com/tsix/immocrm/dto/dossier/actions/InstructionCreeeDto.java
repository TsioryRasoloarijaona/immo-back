package com.tsix.immocrm.dto.dossier.actions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Instruction créée, avec le sort du mail au commercial.")
public record InstructionCreeeDto(
        ActionDossierDto instruction,
        @Schema(description = "Vrai si le mail est parti ; faux si le commercial n'a pas d'email ou si l'envoi est inactif.")
        boolean emailEnvoye
) {
}

package com.tsix.immocrm.dto.projet.reunions;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Saisie d'un contact de réunion (maître d'ouvrage ou contact d'intervenant).")
public record ContactSaisieDto(
        @Schema(description = "Contact (CfgTier.PKey) : obligatoire à l'ajout d'un contact d'intervenant, ignoré sinon.")
        UUID contactId,
        @Schema(description = "Présent ; absent = non.") Boolean present,
        @Schema(description = "À notifier ; absent = non.") Boolean aNotifier) {
}

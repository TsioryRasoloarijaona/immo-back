package com.tsix.immocrm.dto.projet.reunions;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Contact présent à une réunion : maître d'ouvrage (BulMeetingContact) ou contact d'un intervenant "
        + "(BulDelegateContact).")
public record ContactReunionDto(
        @Schema(description = "PKey de la ligne.") UUID id,
        @Schema(description = "O.D.T.") Integer ordre,
        @Schema(description = "Contact (CfgTier.PKey).") UUID contactId,
        @Schema(description = "Raison sociale du contact.") String contact,
        @Schema(description = "Présent à la réunion (IsPresent).") boolean present,
        @Schema(description = "À notifier (Notify) ; la notification elle-même est hors lot.") boolean aNotifier) {
}

package com.tsix.immocrm.dto.notification;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Compteur de la cloche après l'opération.")
public record NonLuesDto(
        @Schema(description = "Notifications non lues restantes du compte appelant.", example = "3")
        long nonLues
) {
}

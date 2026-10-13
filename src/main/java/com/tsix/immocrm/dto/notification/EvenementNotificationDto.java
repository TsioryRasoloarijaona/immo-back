package com.tsix.immocrm.dto.notification;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Données d'un évènement SSE `notification` du flux `/api/notifications/flux`.")
public record EvenementNotificationDto(
        @Schema(description = "Compte destinataire (jamais envoyé à un autre compte).")
        UUID admUserId,
        NotificationDto notification,
        @Schema(description = "Non lues du compte, cette notification comprise.")
        long nonLues
) {
}

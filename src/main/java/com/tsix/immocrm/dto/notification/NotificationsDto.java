package com.tsix.immocrm.dto.notification;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Une page des notifications du compte appelant, plus récentes d'abord.")
public record NotificationsDto(
        List<NotificationDto> notifications,
        @Schema(description = "Numéro de page (à partir de 0).")
        int page,
        @Schema(description = "Taille de page demandée.")
        int taille,
        @Schema(description = "Nombre total de notifications du compte.")
        long total,
        @Schema(description = "Nombre total de notifications non lues du compte (pastille de la cloche).")
        long nonLues
) {
}

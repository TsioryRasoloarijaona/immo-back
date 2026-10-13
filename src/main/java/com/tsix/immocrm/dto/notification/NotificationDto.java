package com.tsix.immocrm.dto.notification;

import com.tsix.immocrm.domain.TypeNotification;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Une notification de la cloche, adressée au compte appelant.")
public record NotificationDto(
        UUID id,
        @Schema(description = "`LEADS_ATTRIBUES`, `LEADS_A_DISTRIBUER` ou `RESUME_DEMI_JOURNEE`.")
        TypeNotification type,
        @Schema(example = "4 nouveaux leads à trier")
        String titre,
        @Schema(example = "Imane Bennis, Abden Sayem, Majda el haitmy et 1 autre.")
        String message,
        @Schema(description = "Route du front à ouvrir au clic ; null si aucune.", example = "/tri")
        String lien,
        @Schema(description = "Création (heure de Casablanca).")
        LocalDateTime creeLe,
        @Schema(description = "Lecture ; null = non lue.")
        LocalDateTime lueLe
) {
}

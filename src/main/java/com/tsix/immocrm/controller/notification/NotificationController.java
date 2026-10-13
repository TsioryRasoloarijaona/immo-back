package com.tsix.immocrm.controller.notification;

import com.tsix.immocrm.dto.notification.EvenementNotificationDto;
import com.tsix.immocrm.dto.notification.NonLuesDto;
import com.tsix.immocrm.dto.notification.NotificationsDto;
import com.tsix.immocrm.services.notification.FluxNotifications;
import com.tsix.immocrm.services.notification.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications")
@ApiResponses({
        @ApiResponse(responseCode = "403", description = "Appelant sans compte applicatif (clé d'API) : pas de cloche.")
})
public class NotificationController {

    private final NotificationService notifications;
    private final FluxNotifications flux;

    public NotificationController(NotificationService notifications, FluxNotifications flux) {
        this.notifications = notifications;
        this.flux = flux;
    }

    @GetMapping
    @Operation(summary = "Notifications du compte appelant, plus récentes d'abord, et nombre de non lues")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Une page, éventuellement vide."),
            @ApiResponse(responseCode = "400", description = "`page` négative ou `size` hors de 1..100.")
    })
    public NotificationsDto lister(
            @Parameter(description = "Page, à partir de 0.") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page, 1 à 100.") @RequestParam(defaultValue = "20") int size) {
        return notifications.lister(page, size);
    }

    @PostMapping("/{id}/lue")
    @Operation(summary = "Marque une notification du compte appelant comme lue")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Non lues restantes (déjà lue : sans effet)."),
            @ApiResponse(responseCode = "404", description = "Aucune notification de ce compte avec cet id.")
    })
    public NonLuesDto marquerLue(@Parameter(description = "Notification.") @PathVariable UUID id) {
        return notifications.marquerLue(id);
    }

    @PostMapping("/tout-lu")
    @Operation(summary = "Marque toutes les notifications du compte appelant comme lues")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Compteur remis à zéro.")})
    public NonLuesDto marquerToutLu() {
        return notifications.marquerToutLu();
    }

    @GetMapping(value = "/flux", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Flux temps réel (SSE) des nouvelles notifications du compte appelant")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Flux ouvert ; `data` de chaque évènement `notification`.",
            content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
                    schema = @Schema(implementation = EvenementNotificationDto.class)))})
    public SseEmitter flux() {
        return flux.abonner(NotificationService.appelant());
    }
}

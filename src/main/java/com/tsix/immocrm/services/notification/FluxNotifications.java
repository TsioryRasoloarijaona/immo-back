package com.tsix.immocrm.services.notification;

import com.tsix.immocrm.dto.notification.EvenementNotificationDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registre en mémoire des flux SSE ouverts, par compte (plusieurs onglets possibles). Une seule instance de back :
 * deux instances ne verraient chacune que leurs propres abonnés (FEATURE-repartition-notifications.md §3.3).
 */
@Component
public class FluxNotifications {

    static final String EVENEMENT = "notification";
    // Au-delà, le navigateur (EventSource) se reconnecte de lui-même ; borne les flux oubliés.
    private static final long DUREE_MS = Duration.ofMinutes(30).toMillis();

    private static final Logger log = LoggerFactory.getLogger(FluxNotifications.class);

    private final Map<UUID, Set<SseEmitter>> abonnes = new ConcurrentHashMap<>();

    public SseEmitter abonner(UUID admUserId) {
        SseEmitter flux = new SseEmitter(DUREE_MS);
        Set<SseEmitter> duCompte = abonnes.computeIfAbsent(admUserId, k -> ConcurrentHashMap.newKeySet());
        duCompte.add(flux);
        Runnable retirer = () -> retirer(admUserId, flux);
        flux.onCompletion(retirer);
        flux.onTimeout(retirer);
        flux.onError(e -> retirer.run());
        // Premier octet tout de suite : en-têtes envoyés, EventSource passe à « ouvert ».
        envoyer(admUserId, flux, SseEmitter.event().comment("connecte"));
        return flux;
    }

    /** Chaque évènement part sur les seuls flux de son compte destinataire. */
    public void diffuser(List<EvenementNotificationDto> evenements) {
        for (EvenementNotificationDto e : evenements) {
            for (SseEmitter flux : abonnes.getOrDefault(e.admUserId(), Set.of())) {
                envoyer(e.admUserId(), flux, SseEmitter.event().name(EVENEMENT).data(e, MediaType.APPLICATION_JSON));
            }
        }
    }

    /** Garde les flux ouverts derrière un proxy et détecte les onglets fermés. */
    @Scheduled(fixedRate = 30_000L, initialDelay = 30_000L)
    public void ping() {
        abonnes.forEach((compte, flux) -> flux.forEach(f -> envoyer(compte, f, SseEmitter.event().comment("ping"))));
    }

    int nombreDeFlux(UUID admUserId) {
        return abonnes.getOrDefault(admUserId, Set.of()).size();
    }

    private void envoyer(UUID compte, SseEmitter flux, SseEmitter.SseEventBuilder evenement) {
        try {
            flux.send(evenement);
        } catch (IOException | IllegalStateException e) {
            log.debug("Flux de notifications fermé pour {} : {}", compte, e.getMessage());
            retirer(compte, flux);
        }
    }

    private void retirer(UUID compte, SseEmitter flux) {
        abonnes.computeIfPresent(compte, (k, ensemble) -> {
            ensemble.remove(flux);
            return ensemble.isEmpty() ? null : ensemble;
        });
    }
}

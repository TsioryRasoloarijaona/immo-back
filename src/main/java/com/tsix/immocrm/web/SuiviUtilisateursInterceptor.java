package com.tsix.immocrm.web;

import com.tsix.immocrm.security.ApiKeyAuthenticationFilter;
import com.tsix.immocrm.security.AuthenticatedUser;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Usage d'Arpent par utilisateur pour Grafana : compteur par login × modèle de route et jauge de dernière activité.
 * Ne retient que login, type d'accès, méthode et modèle de route (FEATURE-suivi-utilisateurs.md §4).
 */
@Component
public class SuiviUtilisateursInterceptor implements HandlerInterceptor {

    static final String COMPTEUR = "arpent.utilisateur.requetes";
    static final String JAUGE = "arpent.utilisateur.derniere.activite.secondes";
    static final String FLUX_NOTIFICATIONS = "/api/notifications/flux";
    private static final String UTILISATEUR = "utilisateur";
    private static final String TYPE = "type";

    private final MeterRegistry registry;
    private final Clock horloge;
    private final Map<String, AtomicLong> dernieresActivites = new ConcurrentHashMap<>();

    public SuiviUtilisateursInterceptor(MeterRegistry registry, Clock horloge) {
        this.registry = registry;
        this.horloge = horloge;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        // Compté au début et au seul dispatch REQUEST : le flux SSE dure des heures, et ses redispatchs ASYNC
        // repasseraient ici.
        if (request.getDispatcherType() != DispatcherType.REQUEST || !(handler instanceof HandlerMethod)) {
            return true;
        }
        Authentication authentification = SecurityContextHolder.getContext().getAuthentication();
        if (authentification == null || !(authentification.getPrincipal() instanceof AuthenticatedUser principal)
                || !(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE) instanceof String uri)) {
            return true;
        }
        enregistrer(principal.username(), typeAcces(request), request.getMethod(), uri);
        return true;
    }

    private void enregistrer(String utilisateur, String type, String methode, String uri) {
        Counter.builder(COMPTEUR)
                .description("Requêtes authentifiées par utilisateur, méthode et modèle de route")
                .tag(UTILISATEUR, utilisateur)
                .tag(TYPE, type)
                .tag("methode", methode)
                .tag("uri", uri)
                .register(registry)
                .increment();
        // Un onglet ouvert garde le flux SSE vivant : ce n'est pas une activité.
        if (!FLUX_NOTIFICATIONS.equals(uri)) {
            derniereActivite(utilisateur, type).set(horloge.millis() / 1000);
        }
    }

    private AtomicLong derniereActivite(String utilisateur, String type) {
        return dernieresActivites.computeIfAbsent(utilisateur + '\u0000' + type, cle -> {
            AtomicLong valeur = new AtomicLong();
            Gauge.builder(JAUGE, valeur, AtomicLong::get)
                    .description("Epoch (secondes) de la dernière requête de l'utilisateur, flux SSE exclu")
                    .tag(UTILISATEUR, utilisateur)
                    .tag(TYPE, type)
                    .register(registry);
            return valeur;
        });
    }

    // Une clé invalide est refusée en 401 avant d'arriver ici : en-tête présent = client machine authentifié.
    private static String typeAcces(HttpServletRequest request) {
        return StringUtils.hasText(request.getHeader(ApiKeyAuthenticationFilter.EN_TETE)) ? "cle_api" : "session";
    }
}

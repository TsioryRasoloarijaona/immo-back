package com.tsix.immocrm.web;

import com.tsix.immocrm.security.ApiKeyAuthenticationFilter;
import com.tsix.immocrm.security.AuthenticatedUser;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.DispatcherType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SuiviUtilisateursInterceptorTest {

    private static final Instant MAINTENANT = Instant.parse("2026-10-16T09:30:00Z");
    private static final String ROUTE_PROSPECTION = "/api/prospections/{id}";

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final SuiviUtilisateursInterceptor intercepteur =
            new SuiviUtilisateursInterceptor(registry, Clock.fixed(MAINTENANT, ZoneOffset.UTC));
    private HandlerMethod handler;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        handler = new HandlerMethod(this, SuiviUtilisateursInterceptorTest.class.getDeclaredMethod("setUp"));
    }

    @AfterEach
    void viderContexte() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requeteAuthentifiee_compteLeModeleDeRoute_pasLeCheminReel() {
        connecter("karim");
        MockHttpServletRequest requete = requete("GET", "/api/prospections/6f1c2b9e-0000-4000-8000-000000000001",
                ROUTE_PROSPECTION);

        intercepteur.preHandle(requete, new MockHttpServletResponse(), handler);

        assertThat(registry.get(SuiviUtilisateursInterceptor.COMPTEUR)
                .tag("utilisateur", "karim").tag("type", "session")
                .tag("methode", "GET").tag("uri", ROUTE_PROSPECTION)
                .counter().count()).isEqualTo(1.0);
    }

    @Test
    void requeteAuthentifiee_poseLaDerniereActiviteEnSecondes() {
        connecter("karim");

        intercepteur.preHandle(requete("GET", "/api/dossiers", "/api/dossiers"), new MockHttpServletResponse(), handler);

        Gauge jauge = registry.get(SuiviUtilisateursInterceptor.JAUGE).tag("utilisateur", "karim").gauge();
        assertThat(jauge.value()).isEqualTo((double) MAINTENANT.getEpochSecond());
    }

    @Test
    void fluxDeNotifications_estCompte_maisNeRafraichitPasLaDerniereActivite() {
        connecter("sara");
        String flux = SuiviUtilisateursInterceptor.FLUX_NOTIFICATIONS;

        intercepteur.preHandle(requete("GET", flux, flux), new MockHttpServletResponse(), handler);

        assertThat(registry.find(SuiviUtilisateursInterceptor.COMPTEUR).tag("uri", flux).counter())
                .isNotNull()
                .extracting(c -> c.count()).isEqualTo(1.0);
        assertThat(registry.find(SuiviUtilisateursInterceptor.JAUGE).gauges()).isEmpty();
    }

    @Test
    void cleApi_estEtiqueteeCommeClientMachine() {
        connecter("n8n");
        MockHttpServletRequest requete = requete("GET", "/api/dossiers", "/api/dossiers");
        requete.addHeader(ApiKeyAuthenticationFilter.EN_TETE, "cle-de-test");

        intercepteur.preHandle(requete, new MockHttpServletResponse(), handler);

        assertThat(registry.find(SuiviUtilisateursInterceptor.COMPTEUR).tag("type", "cle_api").counter()).isNotNull();
    }

    @Test
    void requeteAnonyme_nEstPasComptee() {
        intercepteur.preHandle(requete("GET", "/api/dossiers", "/api/dossiers"), new MockHttpServletResponse(), handler);

        assertThat(registry.find(SuiviUtilisateursInterceptor.COMPTEUR).counters()).isEmpty();
    }

    @Test
    void redispatchAsynchrone_nEstPasRecompte() {
        connecter("karim");
        MockHttpServletRequest requete = requete("GET", "/api/dossiers", "/api/dossiers");
        requete.setDispatcherType(DispatcherType.ASYNC);

        intercepteur.preHandle(requete, new MockHttpServletResponse(), handler);

        assertThat(registry.find(SuiviUtilisateursInterceptor.COMPTEUR).counters()).isEmpty();
    }

    @Test
    void handlerHorsControleur_nEstPasCompte() {
        connecter("karim");

        intercepteur.preHandle(requete("GET", "/api/dossiers", "/api/dossiers"), new MockHttpServletResponse(),
                new Object());

        assertThat(registry.find(SuiviUtilisateursInterceptor.COMPTEUR).counters()).isEmpty();
    }

    private static void connecter(String login) {
        AuthenticatedUser principal = new AuthenticatedUser(null, login, null, false, true, 0);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private static MockHttpServletRequest requete(String methode, String chemin, String modele) {
        MockHttpServletRequest requete = new MockHttpServletRequest(methode, chemin);
        requete.setDispatcherType(DispatcherType.REQUEST);
        requete.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, modele);
        return requete;
    }
}

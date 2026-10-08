package com.tsix.immocrm.web;

import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Point de contrôle unique du périmètre et de la visibilité par entité sur l'accès par id, pour tout appelant, quelle
 * que soit la profondeur de la route ; hors visibilité = 404, jamais 403 (FEATURE-perimetre-commercial.md §6.5, §9bis.1,
 * §9ter.1). Les lots {@code POST /api/dossiers/documents/…} portent leurs ids dans le corps : voir DocumentLotService.
 */
@Component
public class PerimetreInterceptor implements HandlerInterceptor {

    /** FEATURE-perimetre-commercial.md §6.5. */
    private static final Set<String> COLLECTIONS_SCOPEES =
            Set.of("dossiers", "prospections", "pre-prospections", "encaissements");

    private final PerimetreCommercialService perimetreService;
    private final ObjectProvider<RequestMappingHandlerAdapter> handlerAdapter;
    private volatile HttpMessageConverter<Object> convertisseurProbleme;

    public PerimetreInterceptor(PerimetreCommercialService perimetreService,
                                 ObjectProvider<RequestMappingHandlerAdapter> handlerAdapter) {
        this.perimetreService = perimetreService;
        this.handlerAdapter = handlerAdapter;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws IOException {
        // Une préflight ne désigne aucune ressource et arrive sans cookie : la refuser casserait toute
        // requête cross-origin non simple du front.
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }

        String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        Object variables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (pattern == null || !(variables instanceof Map)) {
            return true;
        }

        // Le motif et les variables déjà décodées par Spring, jamais getRequestURI() (non décodé : %2D contournait).
        // ["", "api", "<collection>", "{id}", ...] ; sans variable au segment 3 : saut structurel, le seul autorisé.
        String[] segments = pattern.split("/");
        if (segments.length < 4 || !segments[3].startsWith("{")) {
            return true;
        }
        String collection = segments[2];
        if (!COLLECTIONS_SCOPEES.contains(collection)) {
            return true;
        }

        Optional<UUID> resolu = parseUuid(((Map<String, String>) variables).get(nomVariable(segments[3])));
        if (resolu.isEmpty()) {
            // Échouer en fermeture. JAMAIS « return true » au motif que le contrôleur échouerait aussi :
            // Spring trime avant de convertir, et %20 devant l'id a servi la ressource (§9ter.1).
            refuser(collection, response);
            return false;
        }
        UUID id = resolu.orElseThrow();

        switch (collection) {
            case "dossiers" -> perimetreService.verifierAccesDossier(id);
            case "prospections", "pre-prospections" -> perimetreService.verifierAccesProspection(id);
            case "encaissements" -> perimetreService.verifierAccesEncaissement(id);
            default -> throw new IllegalStateException("Collection scopée non traitée: " + collection);
        }
        return true;
    }

    /**
     * 404 écrit par le convertisseur de Spring (même enveloppe que {@link GlobalExceptionHandler}), sans exception
     * (qui deviendrait un 500). {@code instance} = la collection, pas le chemin : l'id invalide est une chaîne
     * arbitraire de l'appelant, à ne jamais renvoyer telle quelle.
     */
    private void refuser(String collection, HttpServletResponse response) throws IOException {
        ProblemDetail probleme = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, "Ressource introuvable: identifiant invalide.");
        // Spring pose lui-même `instance` quand un @ExceptionHandler renvoie un ProblemDetail
        // (AbstractMessageConverterMethodProcessor) ; hors de ce chemin, c'est à nous de le faire.
        probleme.setInstance(URI.create("/api/" + collection));

        response.setStatus(HttpStatus.NOT_FOUND.value());
        convertisseurProbleme().write(probleme, MediaType.APPLICATION_PROBLEM_JSON,
                new ServletServerHttpResponse(response));
    }

    /** Résolu paresseusement : injecter l'adapter directement crée un cycle (intercepteur enregistré par un WebMvcConfigurer). */
    @SuppressWarnings("unchecked")
    private HttpMessageConverter<Object> convertisseurProbleme() {
        HttpMessageConverter<Object> convertisseur = this.convertisseurProbleme;
        if (convertisseur == null) {
            convertisseur = handlerAdapter.getObject().getMessageConverters().stream()
                    .filter(c -> c.canWrite(ProblemDetail.class, MediaType.APPLICATION_PROBLEM_JSON))
                    .map(c -> (HttpMessageConverter<Object>) c)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Aucun HttpMessageConverter ne sait écrire un ProblemDetail en application/problem+json"));
            this.convertisseurProbleme = convertisseur;
        }
        return convertisseur;
    }

    /** {@code "{id}"} ou {@code "{id:regex}"} → {@code "id"}. */
    private static String nomVariable(String segmentDuMotif) {
        String nom = segmentDuMotif.substring(1, segmentDuMotif.endsWith("}")
                ? segmentDuMotif.length() - 1 : segmentDuMotif.length());
        int separateurRegex = nom.indexOf(':');
        return separateurRegex >= 0 ? nom.substring(0, separateurRegex) : nom;
    }

    /** {@code Optional} délibéré : oublier le cas « pas d'id résoluble » laisserait passer sans contrôle. */
    private static Optional<UUID> parseUuid(String valeur) {
        if (valeur == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(valeur));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}

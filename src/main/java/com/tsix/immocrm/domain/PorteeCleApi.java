package com.tsix.immocrm.domain;

import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ce qu'une clé d'API a le droit d'appeler — {@code instructions/FEATURE-cle-api-n8n.md} §4. Seule
 * limite d'un compte de service (sans habilitation = tout voir), donc fermée : route absente = 403.
 */
public enum PorteeCleApi {

    /** Ce que fait n8n : créer des pré-prospections, à l'unité ou par import CSV. */
    PRE_PROSPECTIONS_ECRIRE(List.of(
            route(HttpMethod.POST, "/api/pre-prospections"),
            route(HttpMethod.POST, "/api/pre-prospections/import"),
            route(HttpMethod.POST, "/api/pre-prospections/import/apercu")), List.of()),

    /** Relire ce qu'on a créé, ou retrouver un lead avant d'en créer un second. */
    PRE_PROSPECTIONS_LIRE(List.of(
            route(HttpMethod.GET, "/api/pre-prospections"),
            route(HttpMethod.GET, "/api/pre-prospections/groupes"),
            route(HttpMethod.GET, "/api/prospections/*")), List.of()),

    /**
     * Résoudre les identifiants (tranche, origine, nature, commercial) par leur code. Le réajustement des
     * superficies expose clients et prix : exclu, même pour un compte de service Administrateur.
     */
    REFERENTIELS_LIRE(List.of(
            route(HttpMethod.GET, "/api/referentiels/**"),
            route(HttpMethod.GET, "/api/projets/**"),
            route(HttpMethod.GET, "/api/tranches/**"),
            route(HttpMethod.GET, "/api/commerciaux")),
            List.of(route(HttpMethod.GET, "/api/tranches/*/reajustement-superficies")));

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    /** Une route autorisée : méthode HTTP + motif Ant sur le chemin ({@code *} = un segment). */
    public record Route(HttpMethod methode, String motif) {
        boolean couvre(String methode, String chemin) {
            return this.methode.matches(methode) && MATCHER.match(motif, chemin);
        }
    }

    private static Route route(HttpMethod methode, String motif) {
        return new Route(methode, motif);
    }

    private final List<Route> routes;
    private final List<Route> exclues;

    PorteeCleApi(List<Route> routes, List<Route> exclues) {
        this.routes = routes;
        this.exclues = exclues;
    }

    public List<Route> routes() {
        return routes;
    }

    /** Une route exclue l'emporte sur un motif large ({@code /api/tranches/**}). */
    public boolean couvre(String methode, String chemin) {
        return routes.stream().anyMatch(r -> r.couvre(methode, chemin))
                && exclues.stream().noneMatch(r -> r.couvre(methode, chemin));
    }

    /** Vrai si l'une des portées couvre la requête. */
    public static boolean autorise(Set<PorteeCleApi> portees, String methode, String chemin) {
        return portees.stream().anyMatch(p -> p.couvre(methode, chemin));
    }

    /** Colonne {@code Portees} ({@code "A,B"}) → ensemble ; un code inconnu est ignoré. */
    public static Set<PorteeCleApi> depuisColonne(String colonne) {
        if (colonne == null || colonne.isBlank()) {
            return EnumSet.noneOf(PorteeCleApi.class);
        }
        return Arrays.stream(colonne.split(","))
                .map(String::trim)
                .map(PorteeCleApi::parCode)
                .flatMap(Optional::stream)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(PorteeCleApi.class)));
    }

    public static String versColonne(Set<PorteeCleApi> portees) {
        return portees.stream().sorted().map(Enum::name).collect(Collectors.joining(","));
    }

    public static Optional<PorteeCleApi> parCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(p -> p.name().equalsIgnoreCase(code.trim())).findFirst();
    }
}

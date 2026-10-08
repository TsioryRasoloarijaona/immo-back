package com.tsix.immocrm.web;

import java.util.Set;

/**
 * Demande brute {@code ?colonnes=} de la requête en cours. {@code ThreadLocal} car {@code MappingJacksonValue} est typé
 * Jackson 2 alors que Spring Boot 4 sérialise en Jackson 3. Brute et non résolue : seul le filtre de chaque catalogue
 * connaît ses défauts. {@code null} = hors requête (tout sérialisé) ; {@code noms() == null} = défaut du catalogue.
 */
public final class ColonnesSelection {

    /** {@code noms} nul si {@code ?colonnes=} est absent : le catalogue décide. */
    public record Demande(Set<String> noms) {
    }

    private static final ThreadLocal<Demande> COURANTE = new ThreadLocal<>();

    private ColonnesSelection() {
    }

    /** {@code null} hors requête HTTP. */
    public static Demande courante() {
        return COURANTE.get();
    }

    static void definir(Demande demande) {
        COURANTE.set(demande);
    }

    static void effacer() {
        COURANTE.remove();
    }
}

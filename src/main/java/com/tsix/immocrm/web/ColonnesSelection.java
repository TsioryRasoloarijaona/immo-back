package com.tsix.immocrm.web;

import java.util.Set;

/**
 * Porte, le temps d'une requête, la sélection de colonnes demandée via {@code ?colonnes=}.
 *
 * <h2>Pourquoi un {@code ThreadLocal} et pas {@code MappingJacksonValue}</h2>
 *
 * <p>C'est le mécanisme habituel pour un filtrage Jackson par requête, mais il est inutilisable
 * ici : Spring&nbsp;7 n'expose qu'un seul {@code MappingJacksonValue}, et il est typé sur le
 * {@code FilterProvider} de <strong>Jackson&nbsp;2</strong>. Or Spring Boot&nbsp;4.1 sérialise
 * avec <strong>Jackson&nbsp;3</strong> ({@code tools.jackson}) — Jackson&nbsp;2 n'est présent
 * que transitivement, pour {@code jjwt}. Les deux univers ne se rencontrent pas.
 *
 * <p>La sélection est donc déposée ici par {@link ColonnesSelectionFilter} et lue par
 * {@link SelectionColonnesPropertyFilter}, qui est enregistré une fois pour toutes sur le mapper.
 *
 * <p>{@code null} signifie « hors requête HTTP » et vaut « tout sérialiser » — cas d'un appel
 * hors filtre servlet. Pendant une requête, la sélection est <em>toujours</em> définie : sans
 * paramètre, c'est la sélection par défaut, qui reproduit exactement les colonnes servies avant
 * ce lot.
 *
 * <p>Le nettoyage est garanti par un {@code finally} dans le filtre — un {@code ThreadLocal}
 * oublié sur un thread de pool contaminerait les requêtes suivantes.
 */
public final class ColonnesSelection {

    private static final ThreadLocal<Set<String>> COURANTE = new ThreadLocal<>();

    private ColonnesSelection() {
    }

    /** Noms de colonnes à sérialiser, ou {@code null} pour tout sérialiser. */
    public static Set<String> courante() {
        return COURANTE.get();
    }

    static void definir(Set<String> noms) {
        COURANTE.set(noms);
    }

    static void effacer() {
        COURANTE.remove();
    }
}

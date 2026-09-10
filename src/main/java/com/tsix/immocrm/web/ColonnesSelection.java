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
 * <h2>Ce qui est stocké, et pourquoi ce n'est pas la sélection résolue</h2>
 *
 * <p>Tant qu'une seule liste était filtrable, ce {@code ThreadLocal} portait directement les noms
 * à sérialiser — sélection par défaut comprise. <strong>Ce montage ne survit pas à un second
 * catalogue.</strong> Le filtre servlet ne sait pas quel DTO le contrôleur va renvoyer : y
 * déposer les colonnes par défaut de la prospection ferait revenir la liste des prospects
 * presque vide, aucun de ses noms ({@code prenom}, {@code ville}…) n'y figurant.
 *
 * <p>Ce qui est stocké est donc la <em>demande brute</em>, indépendante de tout catalogue. C'est
 * {@link SelectionColonnesPropertyFilter} — instancié une fois par DTO filtrable, avec les
 * défauts de son propre catalogue — qui l'interprète.
 *
 * <p>Trois états, volontairement distincts :
 * <ul>
 *   <li>{@code null} — hors requête HTTP : <strong>tout est sérialisé</strong> ;</li>
 *   <li>{@link Demande#noms()} à {@code null} — requête sans {@code ?colonnes=} :
 *       <strong>chaque catalogue applique son défaut</strong> ;</li>
 *   <li>noms renseignés — sélection explicite du client.</li>
 * </ul>
 *
 * <p>Le nettoyage est garanti par un {@code finally} dans le filtre — un {@code ThreadLocal}
 * oublié sur un thread de pool contaminerait les requêtes suivantes.
 */
public final class ColonnesSelection {

    /**
     * Demande de colonnes d'une requête.
     *
     * @param noms noms bruts passés par le client, ou {@code null} si {@code ?colonnes=} est
     *             absent — auquel cas c'est le catalogue qui décide, pas cette classe.
     */
    public record Demande(Set<String> noms) {
    }

    private static final ThreadLocal<Demande> COURANTE = new ThreadLocal<>();

    private ColonnesSelection() {
    }

    /** Demande de la requête en cours, ou {@code null} hors requête HTTP. */
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

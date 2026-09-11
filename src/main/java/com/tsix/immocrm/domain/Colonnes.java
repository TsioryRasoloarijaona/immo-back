package com.tsix.immocrm.domain;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Résolution commune des catalogues de colonnes — voir {@link Colonne}.
 *
 * <p>Écrite une fois pour les trois catalogues plutôt que recopiée dans chacun : la logique
 * (défauts, liste blanche, ajout systématique de l'identifiant, 400 sur nom inconnu) est
 * identique, et c'est le genre de duplication qui diverge dès qu'un seul catalogue évolue.
 */
public final class Colonnes {

    private Colonnes() {
    }

    /** Index nom → constante, insensible à la casse. */
    public static <C extends Enum<C> & Colonne> Map<String, C> index(Class<C> type) {
        return Arrays.stream(type.getEnumConstants())
                .collect(Collectors.toMap(c -> c.nom().toLowerCase(Locale.ROOT), Function.identity()));
    }

    /**
     * Sélection appliquée quand {@code ?colonnes=} est absent : exactement les colonnes servies
     * avant l'introduction du paramètre. C'est ce qui rend chaque lot additif — une intégration
     * qui ignore {@code colonnes} ne voit aucun changement.
     */
    public static <C extends Enum<C> & Colonne> Set<C> parDefaut(Class<C> type) {
        return Arrays.stream(type.getEnumConstants())
                .filter(Colonne::parDefaut)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Noms par défaut, forme attendue par le filtre Jackson. */
    public static <C extends Enum<C> & Colonne> Set<String> nomsParDefaut(Class<C> type) {
        return parDefaut(type).stream()
                .map(Colonne::nom)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Tous les noms valides, dans l'ordre de déclaration — pour le message d'erreur du 400. */
    public static <C extends Enum<C> & Colonne> List<String> nomsValides(Class<C> type) {
        return Arrays.stream(type.getEnumConstants()).map(Colonne::nom).toList();
    }

    /**
     * Résout une sélection reçue du client.
     *
     * <p>{@code colonneId} est toujours ajoutée, même si elle n'est pas demandée : sans elle la
     * ligne n'est pas cliquable et le front ne peut plus appeler la fiche.
     *
     * @throws ColonneInconnueException si un nom ne figure pas au catalogue — plutôt que de
     *         l'ignorer en silence, ce qui laisserait le front croire à une colonne vide.
     */
    public static <C extends Enum<C> & Colonne> Set<C> resoudre(Class<C> type, List<String> demandees,
                                                                C colonneId) {
        if (demandees == null || demandees.isEmpty()) {
            return parDefaut(type);
        }
        Map<String, C> parNom = index(type);
        Set<C> resolues = new LinkedHashSet<>();
        resolues.add(colonneId);
        for (String demandee : demandees) {
            if (demandee == null || demandee.isBlank()) {
                continue;
            }
            C c = parNom.get(demandee.trim().toLowerCase(Locale.ROOT));
            if (c == null) {
                throw new ColonneInconnueException(demandee.trim(), nomsValides(type));
            }
            resolues.add(c);
        }
        return resolues;
    }

    // ------------------------------------------------------------------
    // Groupement et agrégats — voir instructions/FEATURE-groupement-listes.md
    //
    // Même raison d'être que les méthodes ci-dessus : écrites une fois pour les trois catalogues.
    // La liste blanche n'est plus `parDefaut` mais le Chemin déclaré, donc une colonne sans chemin
    // est rejetée exactement comme un nom inconnu — du point de vue du client, demander une
    // colonne qui n'existe pas et en demander une qui ne se groupe pas sont la même erreur.
    // ------------------------------------------------------------------

    /** Colonnes groupables du catalogue, dans l'ordre de déclaration. */
    public static <C extends Enum<C> & Colonne> List<C> groupables(Class<C> type) {
        return Arrays.stream(type.getEnumConstants())
                .filter(c -> c.chemin() != null && c.chemin().groupable())
                .toList();
    }

    /** Colonnes agrégeables du catalogue, dans l'ordre de déclaration. */
    public static <C extends Enum<C> & Colonne> List<C> agregeables(Class<C> type) {
        return Arrays.stream(type.getEnumConstants())
                .filter(c -> c.chemin() != null && c.chemin().agregeable())
                .toList();
    }

    private static <C extends Enum<C> & Colonne> List<String> noms(List<C> colonnes) {
        return colonnes.stream().map(Colonne::nom).toList();
    }

    /**
     * Résout la colonne de {@code ?groupBy=}.
     *
     * @throws ColonneInconnueException si le nom est absent du catalogue <em>ou</em> présent mais
     *         non groupable. Le message porte la liste des colonnes groupables — pas celle de
     *         toutes les colonnes, qui enverrait le client réessayer avec une autre valeur refusée.
     */
    public static <C extends Enum<C> & Colonne> C resoudreGroupement(Class<C> type, String demandee) {
        if (demandee == null || demandee.isBlank()) {
            throw new ColonneInconnueException("", noms(groupables(type)));
        }
        C c = index(type).get(demandee.trim().toLowerCase(Locale.ROOT));
        if (c == null || c.chemin() == null || !c.chemin().groupable()) {
            throw new ColonneInconnueException(demandee.trim(), noms(groupables(type)));
        }
        return c;
    }

    /**
     * Résout la sélection de {@code ?agregats=}. Absente ou vide → aucun agrégat, seul l'effectif
     * du groupe est renvoyé.
     *
     * @throws ColonneInconnueException si un nom est absent du catalogue ou non agrégeable
     */
    public static <C extends Enum<C> & Colonne> Set<C> resoudreAgregats(Class<C> type,
                                                                       List<String> demandees) {
        if (demandees == null || demandees.isEmpty()) {
            return Set.of();
        }
        Map<String, C> parNom = index(type);
        Set<C> resolues = new LinkedHashSet<>();
        for (String demandee : demandees) {
            if (demandee == null || demandee.isBlank()) {
                continue;
            }
            C c = parNom.get(demandee.trim().toLowerCase(Locale.ROOT));
            if (c == null || c.chemin() == null || !c.chemin().agregeable()) {
                throw new ColonneInconnueException(demandee.trim(), noms(agregeables(type)));
            }
            resolues.add(c);
        }
        return resolues;
    }
}

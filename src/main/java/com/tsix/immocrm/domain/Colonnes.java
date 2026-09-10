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
}

package com.tsix.immocrm.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Résolution commune des catalogues de colonnes — voir {@link Colonne}. */
public final class Colonnes {

    private Colonnes() {
    }

    /** Index nom → constante, insensible à la casse. */
    public static <C extends Enum<C> & Colonne> Map<String, C> index(Class<C> type) {
        return Arrays.stream(type.getEnumConstants())
                .collect(Collectors.toMap(c -> c.nom().toLowerCase(Locale.ROOT), Function.identity()));
    }

    /** Sélection sans {@code ?colonnes=} : les colonnes servies avant le paramètre, d'où des lots additifs. */
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
     * {@code colonneId} est toujours ajoutée : sans elle la ligne n'ouvre plus la fiche.
     *
     * @throws ColonneInconnueException si un nom ne figure pas au catalogue (pas d'oubli silencieux)
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
    // Une colonne sans chemin est rejetée exactement comme un nom inconnu.
    // ------------------------------------------------------------------

    public static <C extends Enum<C> & Colonne> List<C> groupables(Class<C> type) {
        return Arrays.stream(type.getEnumConstants())
                .filter(c -> c.chemin() != null && c.chemin().groupable())
                .toList();
    }

    public static <C extends Enum<C> & Colonne> List<C> agregeables(Class<C> type) {
        return Arrays.stream(type.getEnumConstants())
                .filter(c -> c.chemin() != null && c.chemin().agregeable())
                .toList();
    }

    private static <C extends Enum<C> & Colonne> List<String> noms(List<C> colonnes) {
        return colonnes.stream().map(Colonne::nom).toList();
    }

    /**
     * @throws ColonneInconnueException si le nom est absent ou non groupable ; le message liste
     *         les seules colonnes groupables
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
     * {@code prix} (nom nu) = {@link FonctionAgregat#DEFAUT}, sous la clé {@code prix} ; {@code prix:AVG}
     * revient sous la clé {@code prix:AVG}. Les deux coexistent, d'où le dédoublonnage par jeton et
     * non par colonne. Absente ou vide → seul l'effectif du groupe.
     *
     * @throws FonctionAgregatInconnueException si la fonction n'existe pas ou que la colonne ne l'autorise pas
     */
    public static <C extends Enum<C> & Colonne> List<AgregatDemande> resoudreAgregats(
            Class<C> type, List<String> demandees) {
        if (demandees == null || demandees.isEmpty()) {
            return List.of();
        }
        Map<String, C> parNom = index(type);
        List<AgregatDemande> resolus = new ArrayList<>();
        Set<String> jetonsVus = new LinkedHashSet<>();
        for (String demandee : demandees) {
            if (demandee != null && !demandee.isBlank() && jetonsVus.add(demandee.trim())) {
                resolus.add(resoudreAgregat(type, parNom, demandee.trim()));
            }
        }
        return resolus;
    }

    private static <C extends Enum<C> & Colonne> AgregatDemande resoudreAgregat(
            Class<C> type, Map<String, C> parNom, String jeton) {
        int separateur = jeton.indexOf(FonctionAgregat.SEPARATEUR);
        String nom = (separateur < 0 ? jeton : jeton.substring(0, separateur)).trim();
        String suffixe = separateur < 0 ? null : jeton.substring(separateur + 1).trim();

        C colonne = parNom.get(nom.toLowerCase(Locale.ROOT));
        if (colonne == null) {
            throw new ColonneInconnueException(nom, noms(agregeables(type)));
        }
        List<FonctionAgregat> autorisees = FonctionAgregat.pour(colonne.chemin());
        FonctionAgregat fonction = FonctionAgregat.DEFAUT;
        if (suffixe != null) {
            try {
                fonction = FonctionAgregat.valueOf(suffixe.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException inconnue) {
                throw new FonctionAgregatInconnueException(jeton, nom, autorisees);
            }
        }
        // Couvre aussi la colonne sans chemin : sa liste d'autorisées est vide.
        if (!autorisees.contains(fonction)) {
            throw new FonctionAgregatInconnueException(jeton, nom, autorisees);
        }
        return new AgregatDemande(jeton, colonne.nom(), colonne.chemin(), fonction);
    }
}

package com.tsix.immocrm.services.modele;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Liste répétable (↻) : ses champs ne sont permis que dans un élément {@code data-repeter} de cette liste. */
public record ListeChamps<T, E>(String cle, String libelle, Function<T, List<E>> elements, List<Champ<E>> champs) {

    /** {@link HashMap} et non {@code Map.of} : une valeur absente reste une entrée nulle. */
    List<Map<String, Object>> extraire(T source) {
        return valeursDe(elements.apply(source));
    }

    /** Valeurs d'éléments lus hors d'une fiche (lot de plusieurs fiches). */
    List<Map<String, Object>> valeursDe(List<E> liste) {
        if (liste == null) {
            return List.of();
        }
        return liste.stream().map(element -> {
            Map<String, Object> valeurs = new HashMap<>();
            champs.forEach(c -> valeurs.put(c.cle(), c.extracteur().apply(element)));
            return valeurs;
        }).toList();
    }
}

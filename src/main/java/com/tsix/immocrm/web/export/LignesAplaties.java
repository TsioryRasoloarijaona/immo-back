package com.tsix.immocrm.web.export;

import com.tsix.immocrm.dto.referentiel.ReferentielItemDto;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ligne {@code record} mise à plat pour l'export : une référence {id, code, description} devient son libellé, un
 * {@code record} imbriqué ({@code @JsonUnwrapped}) verse ses composantes au premier niveau ; une liste est ignorée.
 */
public final class LignesAplaties {

    private LignesAplaties() {
    }

    public static List<Map<String, Object>> toutes(List<?> lignes) {
        return lignes.stream().map(LignesAplaties::de).toList();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> de(Object ligne) {
        if (ligne instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        Map<String, Object> valeurs = new HashMap<>();
        verser(ligne, valeurs);
        return valeurs;
    }

    public static String libelle(ReferentielItemDto reference) {
        if (reference == null) {
            return null;
        }
        return reference.description() == null || reference.description().isBlank()
                ? reference.code() : reference.description();
    }

    private static void verser(Object ligne, Map<String, Object> valeurs) {
        for (RecordComponent composante : ligne.getClass().getRecordComponents()) {
            Object valeur = lire(composante, ligne);
            if (valeur instanceof ReferentielItemDto reference) {
                valeurs.put(composante.getName(), libelle(reference));
            } else if (valeur != null && valeur.getClass().isRecord()) {
                verser(valeur, valeurs);
            } else if (!(valeur instanceof Collection<?>)) {
                valeurs.putIfAbsent(composante.getName(), valeur);
            }
        }
    }

    private static Object lire(RecordComponent composante, Object ligne) {
        try {
            return composante.getAccessor().invoke(ligne);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Lecture impossible de la colonne " + composante.getName(), e);
        }
    }
}

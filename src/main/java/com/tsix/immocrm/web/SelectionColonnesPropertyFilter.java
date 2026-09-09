package com.tsix.immocrm.web;

import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.PropertyWriter;
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter;

import java.util.Set;

/**
 * Filtre Jackson&nbsp;3 qui ne sérialise que les colonnes demandées par la requête en cours.
 *
 * <p>Enregistré une seule fois sur le mapper (voir {@link ColonnesJacksonConfig}) et sans état :
 * la sélection est lue à chaque propriété dans {@link ColonnesSelection}, alimenté par
 * {@link ColonnesSelectionFilter}.
 *
 * <p>Sélection absente ⇒ <strong>tout est sérialisé</strong>. C'est le cas de la quasi-totalité
 * des requêtes, y compris celles qui embarquent le DTO sans rien demander
 * ({@code GET /api/hotline/recherche}, {@code GET /api/pre-prospections}).
 */
class SelectionColonnesPropertyFilter extends SimpleBeanPropertyFilter {

    @Override
    protected boolean include(BeanPropertyWriter writer) {
        return inclus(writer.getName());
    }

    @Override
    protected boolean include(PropertyWriter writer) {
        return inclus(writer.getName());
    }

    private static boolean inclus(String nom) {
        Set<String> selection = ColonnesSelection.courante();
        return selection == null || selection.contains(nom);
    }
}

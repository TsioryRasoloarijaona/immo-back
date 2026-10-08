package com.tsix.immocrm.web;

import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.PropertyWriter;
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter;

import java.util.Set;

/**
 * Filtre Jackson 3, une instance par catalogue (voir {@link ColonnesJacksonConfig}), sans état. Hors requête HTTP :
 * tout ; requête sans {@code ?colonnes=} (hotline, pré-prospections compris) : défauts du catalogue.
 */
class SelectionColonnesPropertyFilter extends SimpleBeanPropertyFilter {

    private final Set<String> parDefaut;
    private final String colonneId;

    /**
     * @param parDefaut servies sans {@code ?colonnes=} : doit reproduire exactement le contrat de la liste
     * @param colonneId toujours sérialisée : sans elle la ligne n'est plus cliquable côté front
     */
    SelectionColonnesPropertyFilter(Set<String> parDefaut, String colonneId) {
        this.parDefaut = Set.copyOf(parDefaut);
        this.colonneId = colonneId;
    }

    @Override
    protected boolean include(BeanPropertyWriter writer) {
        return inclus(writer.getName());
    }

    @Override
    protected boolean include(PropertyWriter writer) {
        return inclus(writer.getName());
    }

    private boolean inclus(String nom) {
        ColonnesSelection.Demande demande = ColonnesSelection.courante();
        if (demande == null) {
            return true;
        }
        if (demande.noms() == null) {
            return parDefaut.contains(nom);
        }
        return colonneId.equals(nom) || demande.noms().contains(nom);
    }
}

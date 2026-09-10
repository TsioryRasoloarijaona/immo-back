package com.tsix.immocrm.web;

import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.PropertyWriter;
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter;

import java.util.Set;

/**
 * Filtre Jackson&nbsp;3 qui ne sérialise que les colonnes demandées par la requête en cours.
 *
 * <p><strong>Une instance par DTO filtrable</strong>, construite avec les défauts de son propre
 * catalogue (voir {@link ColonnesJacksonConfig}). C'est ce qui permet à plusieurs listes d'être
 * filtrables sans se marcher dessus : {@link ColonnesSelection} ne porte que la demande brute,
 * commune à la requête, et chaque instance l'interprète selon son catalogue.
 *
 * <p>Sans état mutable : la demande est relue à chaque propriété.
 *
 * <p>Hors requête HTTP ⇒ <strong>tout est sérialisé</strong>. C'est aussi le cas des réponses qui
 * embarquent un DTO filtrable sans rien demander — {@code GET /api/hotline/recherche},
 * {@code GET /api/pre-prospections} — qui reçoivent alors la sélection par défaut de leur
 * catalogue, identique aux colonnes servies avant l'introduction du paramètre.
 */
class SelectionColonnesPropertyFilter extends SimpleBeanPropertyFilter {

    private final Set<String> parDefaut;
    private final String colonneId;

    /**
     * @param parDefaut colonnes servies quand {@code ?colonnes=} est absent — doit reproduire
     *                  exactement l'ancien contrat de la liste, sinon le lot cesse d'être additif
     * @param colonneId nom de la colonne d'identifiant, toujours sérialisée : sans elle la ligne
     *                  n'est plus cliquable côté front
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

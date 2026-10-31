package com.tsix.immocrm.controller.administration;

import com.jayway.jsonpath.JsonPath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Arbre de {@code GET …/habilitations} lu sans supposer sa forme : tout objet portant {@code id} et {@code type} est un nœud, ses
 * enfants peuvent être sous n'importe quelle clé. Les noms {@code type}, {@code coche}, {@code figee}, {@code libelle} et
 * {@code enfants} sont fixés par la réponse 9 du spec, et vérifiés par {@code UtilisateursHabilitationsTest}.
 */
final class ArbreHabilitations {

    record Noeud(String id, String type, boolean coche, boolean figee, List<String> ancetres) {

        boolean sous(UUID ancetre) {
            return ancetres.stream().anyMatch(ancetre.toString()::equalsIgnoreCase);
        }
    }

    private final List<Noeud> noeuds = new ArrayList<>();

    private ArbreHabilitations() {
    }

    static ArbreHabilitations lu(String json) {
        ArbreHabilitations arbre = new ArbreHabilitations();
        arbre.parcourir(JsonPath.parse(json).json(), List.of());
        return arbre;
    }

    private void parcourir(Object valeur, List<String> ancetres) {
        if (valeur instanceof Collection<?> liste) {
            liste.forEach(element -> parcourir(element, ancetres));
        } else if (valeur instanceof Map<?, ?> objet) {
            List<String> chemin = ancetres;
            if (objet.get("id") != null && objet.get("type") != null) {
                String id = objet.get("id").toString();
                noeuds.add(new Noeud(id, objet.get("type").toString(), Boolean.TRUE.equals(objet.get("coche")),
                        Boolean.TRUE.equals(objet.get("figee")), ancetres));
                chemin = new ArrayList<>(ancetres);
                chemin.add(id);
            }
            List<String> sousChemin = chemin;
            objet.values().forEach(enfant -> parcourir(enfant, sousChemin));
        }
    }

    /** Les nœuds cochés, figés compris : ce qu'un écran renverrait sans rien toucher. */
    Noeud[] coches() {
        return noeuds.stream().filter(Noeud::coche).toArray(Noeud[]::new);
    }

    boolean contient(UUID id) {
        return noeuds.stream().anyMatch(n -> id.toString().equalsIgnoreCase(n.id()));
    }

    /** Le nœud de cet id ; échoue s'il manque à l'arbre. */
    Noeud noeud(UUID id) {
        return noeuds.stream().filter(n -> id.toString().equalsIgnoreCase(n.id())).findFirst()
                .orElseThrow(() -> new AssertionError("Noeud absent de l'arbre : " + id));
    }
}

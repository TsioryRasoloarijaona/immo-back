package com.tsix.immocrm.services.modele;

import java.util.List;
import java.util.Map;

/** Valeurs brutes d'une fiche, par clé du catalogue (non formatées : les conditions testent la valeur, pas le texte). */
public record ValeursFiche(Map<String, Object> champs, Map<String, List<Map<String, Object>>> listes) {
}

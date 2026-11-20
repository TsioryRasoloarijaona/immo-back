package com.tsix.immocrm.domain.planification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Textes OMRAN des tâches ({@code GanttCalendarInfo.cs} l.120-165) : {@code Predecessors} = « rang|type » séparés par « ; »,
 * {@code ResourceNames} = PKey d'intervenants séparées par « ; ». Un élément illisible ou inconnu est ignoré.
 */
public final class TexteGantt {

    public record Lien(UUID tache, int type) {
    }

    private static final String SEPARATEUR = ";";

    private TexteGantt() {
    }

    public static List<Lien> lirePredecesseurs(String texte, Map<Integer, UUID> tacheParRang) {
        List<Lien> liens = new ArrayList<>();
        for (String element : morceaux(texte)) {
            String[] parties = element.split("\\|", 2);
            Integer rang = entier(parties[0]);
            UUID tache = rang == null ? null : tacheParRang.get(rang);
            if (tache != null) {
                Integer type = parties.length > 1 ? entier(parties[1]) : null;
                liens.add(new Lien(tache, type == null ? TypeDependance.FIN_DEBUT.valeur() : type));
            }
        }
        return liens;
    }

    /** Null si aucun lien ne cite une tâche encore rangée. */
    public static String ecrirePredecesseurs(Collection<Lien> liens, Map<UUID, Integer> rangParTache) {
        String texte = liens.stream().filter(l -> rangParTache.containsKey(l.tache()))
                .map(l -> rangParTache.get(l.tache()) + "|" + l.type()).collect(Collectors.joining(SEPARATEUR));
        return texte.isEmpty() ? null : texte;
    }

    public static List<UUID> lireIntervenants(String texte) {
        return morceaux(texte).stream().map(TexteGantt::uuid).filter(Objects::nonNull).toList();
    }

    public static String ecrireIntervenants(Collection<UUID> intervenants) {
        String texte = intervenants.stream().map(UUID::toString).collect(Collectors.joining(SEPARATEUR));
        return texte.isEmpty() ? null : texte;
    }

    private static List<String> morceaux(String texte) {
        if (texte == null || texte.isBlank()) {
            return List.of();
        }
        return Arrays.stream(texte.split(SEPARATEUR)).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private static Integer entier(String texte) {
        try {
            return Integer.valueOf(texte.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static UUID uuid(String texte) {
        try {
            return UUID.fromString(texte);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

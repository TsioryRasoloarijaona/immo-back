package com.tsix.immocrm.domain.planification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Rang ({@code Sorting}, ordre de l'arbre en profondeur, à partir de 1) et profondeur ({@code Depth}, 0 = racine) des tâches
 * d'une planification, comme {@code task.RowNumber} / {@code task.Level} d'Infragistics (FEATURE-n9-planification.md R9).
 */
public final class ArbreTaches {

    /** {@code cle} ordonne les sœurs ; un parent absent de la liste fait de la tâche une racine. */
    public record Noeud(UUID id, UUID parentId, double cle, String code) {
    }

    public record Place(UUID id, int rang, int profondeur) {
    }

    private static final Comparator<Noeud> ORDRE = Comparator.comparingDouble(Noeud::cle)
            .thenComparing(Noeud::code, Comparator.nullsLast(Comparator.naturalOrder()));

    private ArbreTaches() {
    }

    public static List<Place> ordonner(Collection<Noeud> noeuds) {
        Set<UUID> ids = noeuds.stream().map(Noeud::id).collect(Collectors.toSet());
        Map<UUID, List<Noeud>> enfants = new HashMap<>();
        noeuds.forEach(n -> enfants.computeIfAbsent(ids.contains(n.parentId()) ? n.parentId() : null, k -> new ArrayList<>())
                .add(n));
        enfants.values().forEach(l -> l.sort(ORDRE));
        List<Place> places = new ArrayList<>();
        parcourir(enfants, null, 0, places, new HashSet<>());
        return places;
    }

    private static void parcourir(Map<UUID, List<Noeud>> enfants, UUID parent, int profondeur, List<Place> places,
                                  Set<UUID> vus) {
        for (Noeud n : enfants.getOrDefault(parent, List.of())) {
            if (vus.add(n.id())) {
                places.add(new Place(n.id(), places.size() + 1, profondeur));
                parcourir(enfants, n.id(), profondeur + 1, places, vus);
            }
        }
    }

    /** La tâche puis tous ses descendants. */
    public static List<UUID> branche(Collection<Noeud> noeuds, UUID racine) {
        List<UUID> branche = new ArrayList<>(List.of(racine));
        for (int i = 0; i < branche.size(); i++) {
            UUID parent = branche.get(i);
            noeuds.stream().filter(n -> parent.equals(n.parentId()) && !branche.contains(n.id())).map(Noeud::id)
                    .forEach(branche::add);
        }
        return branche;
    }

    /** Vrai si {@code candidat} est {@code ancetre} ou l'un de ses descendants : en faire le parent créerait un cycle. */
    public static boolean dansLaBranche(Collection<Noeud> noeuds, UUID ancetre, UUID candidat) {
        Map<UUID, Noeud> parId = noeuds.stream().collect(Collectors.toMap(Noeud::id, Function.identity()));
        Set<UUID> vus = new HashSet<>();
        UUID courant = candidat;
        while (courant != null && vus.add(courant)) {
            if (Objects.equals(courant, ancetre)) {
                return true;
            }
            Noeud n = parId.get(courant);
            courant = n == null ? null : n.parentId();
        }
        return false;
    }
}

package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.domain.planification.ArbreTaches;
import com.tsix.immocrm.domain.planification.TexteGantt;
import com.tsix.immocrm.entities.cfg.CfgGanttTask;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Tâches d'une planification en cours d'édition : prédécesseurs lus avec les rangs d'avant l'écriture, puis rangs, profondeurs
 * et textes « rang|type » réécrits par {@link #renumeroter()} (FEATURE-n9-planification.md R9, R10).
 */
final class TachesEditees {

    private final Map<UUID, CfgGanttTask> taches = new LinkedHashMap<>();
    private final Map<UUID, Double> cles = new HashMap<>();
    private final Map<UUID, List<TexteGantt.Lien>> liens = new HashMap<>();

    TachesEditees(List<CfgGanttTask> lues) {
        Map<Integer, UUID> parRang = new HashMap<>();
        lues.stream().filter(t -> t.getSorting() != null).forEach(t -> parRang.putIfAbsent(t.getSorting(), t.getId()));
        lues.forEach(t -> {
            taches.put(t.getId(), t);
            cles.put(t.getId(), t.getSorting() == null ? Double.MAX_VALUE : t.getSorting());
            liens.put(t.getId(), TexteGantt.lirePredecesseurs(t.getPredecessors(), parRang));
        });
    }

    CfgGanttTask tache(UUID id) {
        CfgGanttTask t = id == null ? null : taches.get(id);
        if (t == null) {
            throw new ResourceNotFoundException("Tâche introuvable : " + id);
        }
        return t;
    }

    boolean contient(UUID id) {
        return taches.containsKey(id);
    }

    Collection<CfgGanttTask> toutes() {
        return taches.values();
    }

    List<CfgGanttTask> enfants(UUID parent) {
        return taches.values().stream().filter(t -> Objects.equals(t.getParentId(), parent)).toList();
    }

    /** Clé de la nouvelle dernière sœur sous {@code parent}. */
    double cleSuivante(UUID parent) {
        return enfants(parent).stream().mapToDouble(t -> cles.get(t.getId())).filter(c -> c < Double.MAX_VALUE).max()
                .orElse(0) + 1;
    }

    void ajouter(CfgGanttTask t, double cle) {
        taches.put(t.getId(), t);
        cles.put(t.getId(), cle);
        liens.put(t.getId(), List.of());
    }

    void cle(UUID id, double cle) {
        cles.put(id, cle);
    }

    void liens(UUID id, List<TexteGantt.Lien> nouveaux) {
        liens.put(id, nouveaux);
    }

    List<UUID> branche(UUID racine) {
        return ArbreTaches.branche(noeuds(), racine);
    }

    boolean dansLaBranche(UUID ancetre, UUID candidat) {
        return ArbreTaches.dansLaBranche(noeuds(), ancetre, candidat);
    }

    void retirer(Collection<UUID> ids) {
        ids.forEach(id -> {
            taches.remove(id);
            cles.remove(id);
            liens.remove(id);
        });
    }

    /** Rang et profondeur recalculés ; textes de prédécesseurs réécrits avec les nouveaux rangs (lien vers une tâche retirée perdu). */
    void renumeroter() {
        Map<UUID, Integer> rangs = new HashMap<>();
        ArbreTaches.ordonner(noeuds()).forEach(p -> {
            rangs.put(p.id(), p.rang());
            CfgGanttTask t = taches.get(p.id());
            if (!Objects.equals(t.getSorting(), p.rang())) {
                t.setSorting(p.rang());
            }
            if (!Objects.equals(t.getDepth(), p.profondeur())) {
                t.setDepth(p.profondeur());
            }
        });
        taches.values().forEach(t -> {
            String texte = TexteGantt.ecrirePredecesseurs(liens.get(t.getId()), rangs);
            if (!Objects.equals(texte, t.getPredecessors())) {
                t.setPredecessors(texte);
            }
        });
    }

    private List<ArbreTaches.Noeud> noeuds() {
        return taches.values().stream()
                .map(t -> new ArbreTaches.Noeud(t.getId(), t.getParentId(), cles.get(t.getId()), t.getCode())).toList();
    }
}

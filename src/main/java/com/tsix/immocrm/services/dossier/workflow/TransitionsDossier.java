package com.tsix.immocrm.services.dossier.workflow;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.domain.TransitionsValidees;
import com.tsix.immocrm.entities.str.StrIntervener;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.repositories.str.StrIntervenerRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import com.tsix.immocrm.repositories.str.StrWorkFlowRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Transitions d'un dossier lues dans {@code StrWorkFlow} ({@code StrStatus.GetNextWorkFlowList}) ; sans aucune
 * ligne pour l'entité dossier (base de test), repli sur {@link TransitionsValidees}. Voir FEATURE-vente-workflow.md §1-2.
 */
@Component
public class TransitionsDossier {

    private static final UUID ENTITE = StatutDossier.CONFIG_ENTITY_ID;

    private final StrWorkFlowRepository workflowRepository;
    private final StrIntervenerRepository intervenerRepository;
    private final StrStatusRepository statusRepository;

    public TransitionsDossier(StrWorkFlowRepository workflowRepository, StrIntervenerRepository intervenerRepository,
                              StrStatusRepository statusRepository) {
        this.workflowRepository = workflowRepository;
        this.intervenerRepository = intervenerRepository;
        this.statusRepository = statusRepository;
    }

    /** Toutes les transitions sortantes, sans filtre d'intervenant ; vide pour un statut de code inconnu en repli. */
    public List<TransitionDossier> sortantes(StrStatus depuis) {
        if (!workflowRepository.existsByStrConfigEntityId(ENTITE)) {
            return repli(depuis);
        }
        Map<UUID, String> codes = statusRepository.findByStrConfigEntityId(ENTITE).stream()
                .filter(s -> s.getCode() != null)
                .collect(Collectors.toMap(StrStatus::getId, StrStatus::getCode, (a, b) -> a));
        return workflowRepository.sortantes(ENTITE, depuis.getId()).stream()
                .filter(w -> codes.containsKey(w.getStatusToId()))
                .map(w -> new TransitionDossier(w.getId(), w.getDescription(), codes.get(w.getStatusToId()),
                        Boolean.TRUE.equals(w.getWithAutomaticTransition())))
                .toList();
    }

    /** Celles que l'intervenant peut exécuter (toutes pour l'Administrateur et en repli). */
    public List<TransitionDossier> autorisees(StrStatus depuis, Intervenant intervenant) {
        List<TransitionDossier> toutes = sortantes(depuis);
        if (intervenant.administrateur()) {
            return toutes;
        }
        Map<UUID, List<StrIntervener>> parTransition = intervenants(toutes);
        return toutes.stream()
                .filter(t -> !t.parametree() || parTransition.getOrDefault(t.workflowId(), List.of()).stream()
                        .anyMatch(intervenant::correspond))
                .toList();
    }

    /** Transition vers {@code cible} ; en repli, un statut courant de code inconnu accepte toute cible. */
    public Optional<TransitionDossier> vers(StrStatus depuis, StatutDossier cible) {
        if (!workflowRepository.existsByStrConfigEntityId(ENTITE) && statutConnu(depuis).isEmpty()) {
            return Optional.of(new TransitionDossier(null, cible.getCode(), cible.getCode(), false));
        }
        return sortantes(depuis).stream()
                .filter(t -> t.codeCible().equalsIgnoreCase(cible.getCode()))
                .findFirst();
    }

    private Map<UUID, List<StrIntervener>> intervenants(List<TransitionDossier> transitions) {
        List<UUID> ids = transitions.stream().map(TransitionDossier::workflowId).filter(Objects::nonNull).toList();
        Map<UUID, List<StrIntervener>> parTransition = new HashMap<>();
        if (ids.isEmpty()) {
            return parTransition;
        }
        for (Object[] ligne : intervenerRepository.deTransitions(ids)) {
            parTransition.computeIfAbsent((UUID) ligne[0], k -> new ArrayList<>()).add((StrIntervener) ligne[1]);
        }
        return parTransition;
    }

    private static List<TransitionDossier> repli(StrStatus depuis) {
        return statutConnu(depuis)
                .map(actuel -> TransitionsValidees.transitionsAutorisees(actuel).stream()
                        .sorted(Comparator.comparing(StatutDossier::ordinal))
                        .map(cible -> new TransitionDossier(null, cible.getCode(), cible.getCode(), false))
                        .toList())
                .orElseGet(List::of);
    }

    static Optional<StatutDossier> statutConnu(StrStatus statut) {
        return statut == null ? Optional.empty() : statutDeCode(statut.getCode());
    }

    /** {@code StrStatus.Code} est alimenté par OMRAN : un code hors {@link StatutDossier} donne vide. */
    static Optional<StatutDossier> statutDeCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(StatutDossier.fromCode(code));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}

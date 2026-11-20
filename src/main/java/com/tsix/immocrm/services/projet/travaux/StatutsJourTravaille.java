package com.tsix.immocrm.services.projet.travaux;

import com.tsix.immocrm.dto.projet.travaux.StatutJourTravailleDto;
import com.tsix.immocrm.entities.str.StrEntity;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.exceptions.ConfigurationManquanteException;
import com.tsix.immocrm.repositories.str.StrEntityRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import com.tsix.immocrm.services.dossier.workflow.IntervenantCourant;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Workflow {@code Str*} de l'entité « GrhWorkedDay » : statuts, statut de départ (D1) et statuts visibles (D2). */
@Component
record StatutsJourTravaille(StrEntityRepository entites, StrStatusRepository statuts, IntervenantCourant intervenant) {

    private static final String ENTITE = "GrhWorkedDay";
    private static final UUID TYPE_NOUVEAU = UUID.fromString("1004DFA9-D4C9-4C61-A3AF-44F174A76D53");

    UUID entite() {
        return entites.findByCodeIn(List.of(ENTITE)).stream().map(StrEntity::getId).min(Comparator.naturalOrder())
                .orElseThrow(() -> new ConfigurationManquanteException("StrEntity GrhWorkedDay absente."));
    }

    List<StrStatus> tous() {
        return statuts.findByStrConfigEntityId(entite()).stream()
                .sorted(Comparator.comparing(StrStatus::getSorting, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /** {@code StrStatus.cs:GetStartStatus} : statut de type « Nouveau » visible de l'appelant, vide sinon. */
    Optional<StrStatus> departVisible() {
        Optional<Set<UUID>> visibles = visibles();
        return tous().stream().filter(s -> TYPE_NOUVEAU.equals(s.getStrStatusTypeId()))
                .filter(s -> visibles.map(v -> v.contains(s.getId())).orElse(true)).findFirst();
    }

    /** Vide = tous les statuts visibles (aucun rôle restreint). */
    Optional<Set<UUID>> visibles() {
        return intervenant.statutsDeLotsEtProjets();
    }

    static StatutJourTravailleDto dto(StrStatus s) {
        return s == null ? null : new StatutJourTravailleDto(s.getId(), s.getDescription(), s.getColorName());
    }
}

package com.tsix.immocrm.services.administration;

import com.tsix.immocrm.dto.administration.StatutTacheDto;
import com.tsix.immocrm.dto.administration.StatutTacheDto.TransitionTacheDto;
import com.tsix.immocrm.entities.adm.AdmTask;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.str.StrEntity;
import com.tsix.immocrm.entities.str.StrHistory;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.entities.str.StrWorkFlow;
import com.tsix.immocrm.exceptions.ConfigurationManquanteException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.InvalidStatutTransitionException;
import com.tsix.immocrm.repositories.str.StrEntityRepository;
import com.tsix.immocrm.repositories.str.StrHistoryRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import com.tsix.immocrm.repositories.str.StrWorkFlowRepository;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Workflow {@code Str*} de l'entité {@code AdmTask}, lu en base ; le statut vide part du statut « Nouveau ». */
@Component
record TransitionsTache(StrEntityRepository entites, StrStatusRepository statuts, StrWorkFlowRepository workflows,
                        StrHistoryRepository historiques, UtilisateurAffiche utilisateur, Clock clock) {

    private static final String ENTITE = "AdmTask";
    private static final UUID TYPE_NOUVEAU = UUID.fromString("1004DFA9-D4C9-4C61-A3AF-44F174A76D53");

    UUID entite() {
        return entites.findByCodeIn(List.of(ENTITE)).stream().findFirst().map(StrEntity::getId)
                .orElseThrow(() -> new ConfigurationManquanteException("StrEntity AdmTask absente."));
    }

    StrStatus statutInitial(UUID entite) {
        return statutsDe(entite).stream().filter(s -> TYPE_NOUVEAU.equals(s.getStrStatusTypeId())).findFirst()
                .orElseThrow(() -> new ConfigurationManquanteException("Aucun statut « Nouveau » pour l'entité AdmTask."));
    }

    List<StatutTacheDto> catalogue() {
        UUID entite = entite();
        List<StrStatus> tous = statutsDe(entite);
        Map<UUID, StrStatus> parId = tous.stream().collect(Collectors.toMap(StrStatus::getId, Function.identity()));
        return tous.stream().map(s -> new StatutTacheDto(s.getId(), s.getCode(), s.getSorting(),
                workflows.sortantes(entite, s.getId()).stream()
                        .map(w -> new TransitionTacheDto(w.getStatusToId(),
                                Champs.de(parId.get(w.getStatusToId()), StrStatus::getCode),
                                Boolean.TRUE.equals(w.getWithComment())))
                        .toList()))
                .toList();
    }

    void executer(AdmTask t, UUID statutId, String commentaire) {
        UUID entite = t.getStrEntityId();
        StrStatus depuis = Objects.requireNonNullElseGet(t.getStatut(), () -> statutInitial(entite));
        List<StrWorkFlow> permises = workflows.sortantes(entite, depuis.getId());
        StrWorkFlow w = permises.stream().filter(p -> p.getStatusToId().equals(statutId)).findFirst()
                .orElseThrow(() -> new InvalidStatutTransitionException(
                        "Transition non permise depuis le statut %s.".formatted(depuis.getDescription()),
                        statuts.findAllById(permises.stream().map(StrWorkFlow::getStatusToId).toList()).stream()
                                .map(StrStatus::getCode).toList()));
        if (Boolean.TRUE.equals(w.getWithComment()) && (commentaire == null || commentaire.isBlank())) {
            throw new InvalidRequestException("commentaire : obligatoire pour cette transition.");
        }
        StrStatus cible = statuts.findById(statutId).orElseThrow(
                () -> new ConfigurationManquanteException("Statut d'arrivée de la transition %s absent.".formatted(w.getCode())));
        t.setStatut(cible);
        historiser(t, w, cible, Objects.requireNonNullElse(commentaire, ""));
    }

    private void historiser(AdmTask t, StrWorkFlow w, StrStatus cible, String notes) {
        UtilisateurAffiche.Auteur auteur = utilisateur.courant();
        LocalDateTime maintenant = LocalDateTime.now(clock);
        StrHistory h = new StrHistory();
        h.setId(UUID.randomUUID());
        h.setDocDate(maintenant);
        h.setUserDate(maintenant);
        h.setDescription("%s: %s(%s & )".formatted(w.getDescription(), w.getCode(),
                Objects.toString(Champs.de(auteur.compte(), AdmUser::getCode), "")));
        h.setNotes(notes);
        h.setObjectId(t.getId());
        h.setAdmUserId(auteur.compteId());
        h.setAdmUserDescription(auteur.nom());
        h.setStrWorkflowId(w.getId());
        h.setStrWorkflowDescription(w.getDescription());
        h.setStrStatusId(cible.getId());
        h.setStrStatusDescription(cible.getDescription());
        h.setStrConfigEntityId(t.getStrEntityId());
        historiques.save(h);
    }

    private List<StrStatus> statutsDe(UUID entite) {
        return statuts.findByStrConfigEntityId(entite).stream()
                .sorted(Comparator.comparing(StrStatus::getSorting, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }
}

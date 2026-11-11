package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.dto.projet.ExecutionTransitionProjetDto;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.prj.PrjProject;
import com.tsix.immocrm.entities.str.StrEntity;
import com.tsix.immocrm.entities.str.StrHistory;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.entities.str.StrWorkFlow;
import com.tsix.immocrm.exceptions.ConflictException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.InvalidStatutTransitionException;
import com.tsix.immocrm.repositories.str.StrHistoryRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import com.tsix.immocrm.repositories.str.StrWorkFlowRepository;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Workflow {@code Str*} du projet ({@code RessourceWithWorkFlow.cs:ExecuteWorkflow}) : toutes les transitions du statut courant,
 * sans filtre d'intervenant ({@code StrStatus.GetNextWorkFlowList}), une ligne {@code StrHistory} par exécution.
 */
@Component
record TransitionsProjet(StrStatusRepository statuts, StrWorkFlowRepository workflows, StrHistoryRepository historiques,
                         UtilisateurAffiche utilisateur, Clock clock) {

    static final String CREATION = "Création";

    List<StrWorkFlow> sortantes(PrjProject p) {
        return workflows.sortantes(Champs.de(p.getStrEntity(), StrEntity::getId), Champs.de(p.getStrStatus(), StrStatus::getId));
    }

    Map<UUID, StrStatus> cibles(List<StrWorkFlow> transitions) {
        return statuts.findAllById(transitions.stream().map(StrWorkFlow::getStatusToId).toList()).stream()
                .collect(Collectors.toMap(StrStatus::getId, Function.identity()));
    }

    void executer(PrjProject p, ExecutionTransitionProjetDto d) {
        List<StrWorkFlow> permises = sortantes(p);
        Map<UUID, StrStatus> cibles = cibles(permises);
        StrStatus depuis = p.getStrStatus();
        StrWorkFlow w = permises.stream().filter(t -> t.getId().equals(d.transitionId())).findFirst()
                .orElseThrow(() -> new InvalidStatutTransitionException(
                        "Transition non permise depuis le statut %s.".formatted(libelle(depuis)),
                        permises.stream().map(t -> cibles.get(t.getStatusToId())).filter(Objects::nonNull)
                                .map(StrStatus::getCode).toList()));
        if (Boolean.TRUE.equals(w.getWithPassword())) {
            throw new ConflictException("Cette transition exige un second utilisateur : non prise en charge.");
        }
        if (Boolean.TRUE.equals(w.getWithComment()) && (d.commentaire() == null || d.commentaire().isBlank())) {
            throw new InvalidRequestException("commentaire : Veuillez saisir un commentaire obligatoirement !");
        }
        StrStatus cible = cibles.get(w.getStatusToId());
        p.setStrStatus(cible);
        String login = Objects.toString(Champs.de(utilisateur.courant().compte(), AdmUser::getCode), "");
        historiser(p, "%s: %s=>%s(%s & )".formatted(w.getDescription(), libelle(depuis), libelle(cible), login), w,
                Objects.requireNonNullElse(d.commentaire(), ""),
                Boolean.TRUE.equals(w.getWithUserDate()) && d.dateUtilisateur() != null ? d.dateUtilisateur() : null);
    }

    /** {@code w} nul : ligne « Création » ({@code RessourceWithWorkFlow.cs:AfterInsert}). */
    void historiser(PrjProject p, String description, StrWorkFlow w, String notes, LocalDateTime dateUtilisateur) {
        UtilisateurAffiche.Auteur auteur = utilisateur.courant();
        LocalDateTime maintenant = LocalDateTime.now(clock);
        StrHistory h = new StrHistory();
        h.setId(UUID.randomUUID());
        h.setDocDate(maintenant);
        h.setDescription(description);
        h.setNotes(notes);
        h.setObjectId(p.getId());
        h.setAdmUserId(auteur.compteId());
        h.setAdmUserDescription(auteur.nom());
        h.setStrWorkflowId(Champs.de(w, StrWorkFlow::getId));
        h.setStrWorkflowDescription(Champs.de(w, StrWorkFlow::getDescription));
        h.setStrStatusId(Champs.de(p.getStrStatus(), StrStatus::getId));
        h.setStrStatusDescription(libelle(p.getStrStatus()));
        h.setUserDate(Objects.requireNonNullElse(dateUtilisateur, maintenant));
        h.setStrConfigEntityId(Champs.de(p.getStrEntity(), StrEntity::getId));
        historiques.save(h);
    }

    private static String libelle(StrStatus s) {
        return Champs.de(s, StrStatus::getDescription);
    }
}

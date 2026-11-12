package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.entities.str.StrEntity;
import com.tsix.immocrm.entities.str.StrHistory;
import com.tsix.immocrm.entities.str.StrIntervener;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.entities.str.StrWorkFlow;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.InvalidStatutTransitionException;
import com.tsix.immocrm.repositories.str.StrHistoryRepository;
import com.tsix.immocrm.repositories.str.StrIntervenerRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import com.tsix.immocrm.repositories.str.StrWorkFlowRepository;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.services.dossier.workflow.Intervenant;
import com.tsix.immocrm.services.dossier.workflow.IntervenantCourant;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Workflow {@code Str*} du lot ({@code StrStatus.cs:GetNextWorkFlowList}) : transitions du statut courant, filtrées par
 * {@code StrWorkFlowIntervener} quand aucun rôle de l'appelant n'est libre (FEATURE-lots-liste-parite.md E7, R7).
 */
@Component
record TransitionsLot(StrStatusRepository statuts, StrWorkFlowRepository workflows, StrIntervenerRepository intervenants,
                      StrHistoryRepository historiques, IntervenantCourant intervenant, UtilisateurAffiche utilisateur,
                      Clock clock) {

    static final String CREATION = "Création";

    /** Seul calcul des transitions permises : lu par le GET et par l'exécution (R7). */
    List<StrWorkFlow> permises(PrjMarket m) {
        // Second valideur (WithPassword) hors lot (2ᵉ passe Q5) : jamais proposée, donc refusée en 409.
        List<StrWorkFlow> sortantes = workflows.sortantes(Champs.de(m.getStrEntity(), StrEntity::getId),
                Champs.de(m.getStrStatus(), StrStatus::getId)).stream()
                .filter(w -> !Boolean.TRUE.equals(w.getWithPassword())).toList();
        Intervenant moi = intervenant.courant();
        // Comme OMRAN, aucun raccourci Administrateur : seule l'exemption de rôle compte (FEATURE-lot-fiche-parite.md E7).
        if (sortantes.isEmpty() || (!moi.roleIds().isEmpty() && intervenants.rolesSansRestriction(moi.roleIds()) > 0)) {
            return sortantes;
        }
        Set<UUID> ouvertes = new HashSet<>();
        for (Object[] r : intervenants.deTransitions(sortantes.stream().map(StrWorkFlow::getId).toList())) {
            if (moi.correspond((StrIntervener) r[1])) {
                ouvertes.add((UUID) r[0]);
            }
        }
        return sortantes.stream().filter(w -> ouvertes.contains(w.getId())).toList();
    }

    Map<UUID, StrStatus> cibles(List<StrWorkFlow> transitions) {
        return statuts.findAllById(transitions.stream().map(StrWorkFlow::getStatusToId).toList()).stream()
                .collect(Collectors.toMap(StrStatus::getId, Function.identity()));
    }

    void executer(PrjMarket m, UUID transitionId, String notes) {
        List<StrWorkFlow> permises = permises(m);
        Map<UUID, StrStatus> cibles = cibles(permises);
        StrStatus depuis = m.getStrStatus();
        StrWorkFlow w = permises.stream().filter(t -> t.getId().equals(transitionId)).findFirst()
                .orElseThrow(() -> new InvalidStatutTransitionException(
                        "Transition non permise depuis le statut %s.".formatted(libelle(depuis)),
                        permises.stream().map(t -> cibles.get(t.getStatusToId())).filter(Objects::nonNull)
                                .map(StrStatus::getCode).toList()));
        if (Boolean.TRUE.equals(w.getWithComment()) && (notes == null || notes.isBlank())) {
            throw new InvalidRequestException("notes : Veuillez saisir un commentaire obligatoirement !");
        }
        StrStatus cible = cibles.get(w.getStatusToId());
        m.setStrStatus(cible);
        String login = Objects.toString(Champs.de(utilisateur.courant().compte(), AdmUser::getCode), "");
        historiser(m, "%s: %s=>%s(%s & )".formatted(w.getDescription(), libelle(depuis), libelle(cible), login), w,
                Objects.requireNonNullElse(notes, ""));
    }

    /** {@code w} nul : ligne « Création » ({@code RessourceWithWorkFlow.cs:AfterInsert}). */
    void historiser(PrjMarket m, String description, StrWorkFlow w, String notes) {
        UtilisateurAffiche.Auteur auteur = utilisateur.courant();
        LocalDateTime maintenant = LocalDateTime.now(clock);
        StrHistory h = new StrHistory();
        h.setId(UUID.randomUUID());
        h.setDocDate(maintenant);
        h.setDescription(description);
        h.setNotes(notes);
        h.setObjectId(m.getId());
        h.setAdmUserId(auteur.compteId());
        h.setAdmUserDescription(auteur.nom());
        h.setStrWorkflowId(Champs.de(w, StrWorkFlow::getId));
        h.setStrWorkflowDescription(Champs.de(w, StrWorkFlow::getDescription));
        h.setStrStatusId(Champs.de(m.getStrStatus(), StrStatus::getId));
        h.setStrStatusDescription(libelle(m.getStrStatus()));
        h.setUserDate(maintenant);
        h.setStrConfigEntityId(Champs.de(m.getStrEntity(), StrEntity::getId));
        historiques.save(h);
    }

    private static String libelle(StrStatus s) {
        return Champs.de(s, StrStatus::getDescription);
    }
}

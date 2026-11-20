package com.tsix.immocrm.services.projet.travaux;

import com.tsix.immocrm.dto.projet.travaux.ExecutionTransitionJourTravailleDto;
import com.tsix.immocrm.dto.projet.travaux.HistoriqueJourTravailleDto;
import com.tsix.immocrm.dto.projet.travaux.TransitionJourTravailleDto;
import com.tsix.immocrm.dto.projet.travaux.TransitionJourTravailleDto.StatutCibleJourTravailleDto;
import com.tsix.immocrm.entities.grh.GrhWorkedDay;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.entities.str.StrWorkFlow;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.str.StrHistoryRepository;
import com.tsix.immocrm.services.workflow.MoteurWorkflowStr;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Workflow {@code Str*} du jour travaillé (FEATURE-n2-travaux-chantier.md § N2b) : statuts, moteur partagé, historique. */
@Component
record TransitionsJourTravaille(StatutsJourTravaille statuts, MoteurWorkflowStr moteur, StrHistoryRepository historiques) {

    /** R-N2b-2 : seul calcul, relu par {@link #executer}. */
    List<TransitionJourTravailleDto> permises(GrhWorkedDay jour) {
        List<StrWorkFlow> permises = moteur.permises(objet(jour));
        Map<UUID, StrStatus> cibles = moteur.cibles(permises);
        return permises.stream().map(w -> dto(w, cibles.get(w.getStatusToId()))).toList();
    }

    void executer(GrhWorkedDay jour, ExecutionTransitionJourTravailleDto e) {
        if (e == null || e.transitionId() == null) {
            throw new InvalidRequestException("transitionId : obligatoire");
        }
        moteur.executer(objet(jour), new MoteurWorkflowStr.Execution(e.transitionId(), e.notes(), e.dateUtilisateur(),
                e.secondValideur()), cible -> { });
    }

    void creation(GrhWorkedDay jour) {
        moteur.historiser(objet(jour), MoteurWorkflowStr.CREATION, null, "");
    }

    void verifierSuppression(GrhWorkedDay jour) {
        moteur.verifierSuppression(objet(jour));
    }

    List<HistoriqueJourTravailleDto> historique(UUID id) {
        return historiques.findByObjectIdInOrderByDocDateDesc(List.of(id)).stream()
                .map(h -> new HistoriqueJourTravailleDto(h.getDocDate(), h.getAdmUserDescription(), h.getDescription(),
                        h.getNotes(), h.getUserDate()))
                .toList();
    }

    private ObjetJourTravaille objet(GrhWorkedDay jour) {
        return new ObjetJourTravaille(jour, statuts);
    }

    private static TransitionJourTravailleDto dto(StrWorkFlow w, StrStatus cible) {
        StatutCibleJourTravailleDto statut = cible == null ? null : new StatutCibleJourTravailleDto(cible.getId(),
                cible.getCode(), MoteurWorkflowStr.libelle(cible), cible.getColorName());
        return new TransitionJourTravailleDto(w.getId(), w.getDescription(), statut, Boolean.TRUE.equals(w.getWithComment()),
                Boolean.TRUE.equals(w.getWithUserDate()), Boolean.TRUE.equals(w.getWithPassword()));
    }
}

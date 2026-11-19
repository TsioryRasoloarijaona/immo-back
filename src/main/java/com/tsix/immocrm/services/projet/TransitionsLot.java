package com.tsix.immocrm.services.projet;

import com.tsix.immocrm.dto.projet.ExecutionTransitionLotDto;
import com.tsix.immocrm.entities.prj.PrjMarket;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.entities.str.StrWorkFlow;
import com.tsix.immocrm.services.workflow.MoteurWorkflowStr;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Workflow {@code Str*} du lot : adaptateur du moteur partagé, sans hook de statut (FEATURE-n6-demandes-achat.md D2). */
@Component
record TransitionsLot(MoteurWorkflowStr moteur) {

    static final String CREATION = MoteurWorkflowStr.CREATION;

    List<StrWorkFlow> permises(PrjMarket m) {
        return moteur.permises(m);
    }

    Map<UUID, StrStatus> cibles(List<StrWorkFlow> transitions) {
        return moteur.cibles(transitions);
    }

    void executer(PrjMarket m, ExecutionTransitionLotDto d) {
        moteur.executer(m, new MoteurWorkflowStr.Execution(d.transitionId(), d.notes(), d.dateUtilisateur(), d.secondValideur()),
                cible -> { });
    }

    void historiser(PrjMarket m, String description, StrWorkFlow w, String notes) {
        moteur.historiser(m, description, w, notes);
    }
}

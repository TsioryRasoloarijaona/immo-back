package com.tsix.immocrm.services.projet.travaux;

import com.tsix.immocrm.entities.grh.GrhWorkedDay;
import com.tsix.immocrm.entities.str.StrEntity;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.services.workflow.ObjetWorkflow;
import com.tsix.immocrm.utils.Champs;

import java.util.Optional;
import java.util.UUID;

/** {@code GrhWorkedDay} ne porte que les colonnes UUID {@code StrEntityId}/{@code StrStatusId} : adaptateur du moteur. */
record ObjetJourTravaille(GrhWorkedDay jour, StatutsJourTravaille statuts) implements ObjetWorkflow {

    @Override
    public UUID getId() {
        return jour.getId();
    }

    @Override
    public StrEntity getStrEntity() {
        return Optional.ofNullable(jour.getStrEntityId()).flatMap(statuts.entites()::findById).orElse(null);
    }

    @Override
    public StrStatus getStrStatus() {
        return Optional.ofNullable(jour.getStrStatusId()).flatMap(statuts.statuts()::findById).orElse(null);
    }

    @Override
    public void setStrStatus(StrStatus statut) {
        jour.setStrStatusId(Champs.de(statut, StrStatus::getId));
    }
}

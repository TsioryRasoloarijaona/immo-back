package com.tsix.immocrm.entities.stk;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Document exigé pour emprunter une transition : une pièce jointe de l'étude de ce type. */
@Getter
@Setter
@Entity
@Table(name = "StkFeasibilityStudyWorkFlowDocumentToAttach")
public class StkFeasibilityStudyWorkFlowDocumentToAttach extends DocumentAJoindreOmran {
    @Column(name = "StkFeasibilityStudyWorkFlowId")
    private UUID workFlowId;

    @Override
    public void setPorteurId(UUID porteurId) {
        this.workFlowId = porteurId;
    }
}

package com.tsix.immocrm.entities.stk;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Document d'un statut d'étude, stocké sans effet (OMRAN ne le lit pas pour l'étude). */
@Getter
@Setter
@Entity
@Table(name = "StkFeasibilityStudyStatusDocumentToAttach")
public class StkFeasibilityStudyStatusDocumentToAttach extends DocumentAJoindreOmran {
    @Column(name = "StkFeasibilityStudyStatusId")
    private UUID statusId;

    @Override
    public void setPorteurId(UUID porteurId) {
        this.statusId = porteurId;
    }
}

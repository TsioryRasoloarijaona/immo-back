package com.tsix.immocrm.entities.stk;

import com.tsix.immocrm.entities.LigneOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Rôle autorisé à emprunter une transition d'étude. */
@Getter
@Setter
@Entity
@Table(name = "StkFeasibilityStudyWorkFlowAdmRole")
public class StkFeasibilityStudyWorkFlowAdmRole extends LigneOmran {
    @Column(name = "WorkFlowId")
    private UUID workFlowId;

    @Column(name = "AdmRoleId")
    private UUID admRoleId;
}

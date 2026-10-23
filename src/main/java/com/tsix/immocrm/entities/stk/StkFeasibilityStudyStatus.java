package com.tsix.immocrm.entities.stk;

import com.tsix.immocrm.entities.AuditTrieOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.util.UUID;

/** Statut d'étude de faisabilité ; {@code MandatoryFields} au format OMRAN « ,Code,Area » (ChampsObligatoiresFaisabilite). */
@Getter
@Setter
@Entity
@Table(name = "StkFeasibilityStudyStatus")
public class StkFeasibilityStudyStatus extends AuditTrieOmran {
    @Size(max = 255)
    @Nationalized
    @Column(name = "ColorName")
    private String colorName;

    @Column(name = "AdmWorkFlowStateTypeId")
    private UUID admWorkFlowStateTypeId;

    @Column(name = "Delay")
    private Integer delay;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "MandatoryFields")
    private String mandatoryFields;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes")
    private String notes;
}

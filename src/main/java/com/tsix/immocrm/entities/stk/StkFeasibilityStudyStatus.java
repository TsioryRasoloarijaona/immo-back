package com.tsix.immocrm.entities.stk;

import com.tsix.immocrm.entities.AuditTrieOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Nationalized;

import java.util.UUID;

/** Statut d'étude de faisabilité, lu seulement (le workflow est un lot ultérieur). */
@Getter
@Setter
@Entity
@Immutable
@Table(name = "StkFeasibilityStudyStatus")
public class StkFeasibilityStudyStatus extends AuditTrieOmran {
    @Size(max = 255)
    @Nationalized
    @Column(name = "ColorName")
    private String colorName;

    @Column(name = "AdmWorkFlowStateTypeId")
    private UUID admWorkFlowStateTypeId;
}

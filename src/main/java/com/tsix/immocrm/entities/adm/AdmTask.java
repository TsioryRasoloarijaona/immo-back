package com.tsix.immocrm.entities.adm;

import com.tsix.immocrm.entities.AuditOmran;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.prj.PrjProject;
import com.tsix.immocrm.entities.str.StrStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;
import java.util.UUID;

/** Tâches et rendez-vous d'OMRAN, distingués par {@code StrEntityId} : ne lire que par {@code AdmTaskRepository}. */
@Getter
@Setter
@Entity
@Table(name = "AdmTask")
public class AdmTask extends AuditOmran {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmTaskTypeId")
    private AdmTaskType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgTierTriggerId")
    private CfgTier declencheur;

    @Column(name = "StartDatePrev")
    private LocalDateTime startDatePrev;

    @Column(name = "DelayPrev")
    private Integer delayPrev;

    @Column(name = "DueDatePrev")
    private LocalDateTime dueDatePrev;

    @Column(name = "StartDate")
    private LocalDateTime startDate;

    @Column(name = "Delay")
    private Integer delay;

    @Column(name = "EndDate")
    private LocalDateTime endDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmModuleId")
    private AdmModule module;

    @Column(name = "StrEntityId")
    private UUID strEntityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StrStatusId")
    private StrStatus statut;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmTaskPriorityId")
    private AdmTaskPriority priorite;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgTierManagerId")
    private CfgTier gestionnaire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgTierAssignmentId")
    private CfgTier assigne;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgCompanyId")
    private CfgCompany cfgCompany;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PrjProjectId")
    private PrjProject prjProject;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes", length = 1000)
    private String notes;

    @Column(name = "ObjectId")
    private UUID objectId;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "ObjectLabel", length = 1000)
    private String objectLabel;

    @Column(name = "ObjectStrEntityId")
    private UUID objectStrEntityId;

    @Column(name = "CfgProspectId")
    private UUID cfgProspectId;
}

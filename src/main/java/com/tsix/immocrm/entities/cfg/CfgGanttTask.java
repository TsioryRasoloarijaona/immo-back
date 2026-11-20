package com.tsix.immocrm.entities.cfg;

import com.tsix.immocrm.entities.AuditOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Tâche d'une planification Gantt ; {@code Predecessors} cite des rangs ({@code Sorting}), réécrits à chaque écriture. */
@Getter
@Setter
@Entity
@Table(name = "CfgGanttTask")
public class CfgGanttTask extends AuditOmran {
    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes")
    private String notes;

    @Column(name = "Sorting")
    private Integer sorting;

    @Column(name = "Depth")
    private Integer depth;

    @Column(name = "StartDate")
    private LocalDateTime startDate;

    @Column(name = "Duration")
    private Integer duration;

    @Column(name = "PercentComplete", precision = 26, scale = 6)
    private BigDecimal percentComplete;

    @Column(name = "AlertBeforeDays")
    private Integer alertBeforeDays;

    @Column(name = "ConstraintType")
    private Integer constraintType;

    @Column(name = "ConstraintDate")
    private LocalDateTime constraintDate;

    @Column(name = "Deadline")
    private LocalDateTime deadline;

    @Size(max = 4000)
    @Nationalized
    @Column(name = "Predecessors")
    private String predecessors;

    @Size(max = 4000)
    @Nationalized
    @Column(name = "ResourceNames")
    private String resourceNames;

    @Column(name = "IsMilestone")
    private Boolean isMilestone;

    @Column(name = "ParentId")
    private UUID parentId;

    @Column(name = "CfgGanttProjectId")
    private UUID cfgGanttProjectId;

    @Column(name = "CfgGanttTaskTypeId")
    private UUID cfgGanttTaskTypeId;

    @Column(name = "PrjMarketId")
    private UUID prjMarketId;

    @Column(name = "StlCategoryId")
    private UUID stlCategoryId;

    @Column(name = "StrEntityLinkedId")
    private UUID strEntityLinkedId;

    @Column(name = "StrWorkFlowStartId")
    private UUID strWorkFlowStartId;

    @Column(name = "StrWorkFlowEndId")
    private UUID strWorkFlowEndId;
}

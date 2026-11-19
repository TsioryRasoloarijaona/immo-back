package com.tsix.immocrm.entities.cfg;

import com.tsix.immocrm.entities.AuditOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Type de tâche de planification, cité par {@code CfgGanttTask.CfgGanttTaskTypeId}. */
@Getter
@Setter
@Entity
@Table(name = "CfgGanttTaskType")
public class CfgGanttTaskType extends AuditOmran {
    @Column(name = "Sorting")
    private Integer sorting;
}

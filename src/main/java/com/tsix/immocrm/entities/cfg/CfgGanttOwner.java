package com.tsix.immocrm.entities.cfg;

import com.tsix.immocrm.entities.AuditOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.util.UUID;

/** Intervenant d'une planification Gantt, cité par les {@code ResourceNames} de ses tâches. */
@Getter
@Setter
@Entity
@Table(name = "CfgGanttOwner")
public class CfgGanttOwner extends AuditOmran {
    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes")
    private String notes;

    @Column(name = "Visible")
    private Boolean visible;

    @Column(name = "CfgGanttProjectId")
    private UUID cfgGanttProjectId;

    @Column(name = "CfgTierId")
    private UUID cfgTierId;
}

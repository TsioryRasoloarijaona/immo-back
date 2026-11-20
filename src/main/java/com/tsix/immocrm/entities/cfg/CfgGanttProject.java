package com.tsix.immocrm.entities.cfg;

import com.tsix.immocrm.entities.AuditOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;
import java.util.UUID;

/** Planification Gantt : modèle, planification d'un projet ({@code ObjectId}) ou version ({@code VersionDate}). */
@Getter
@Setter
@Entity
@Table(name = "CfgGanttProject")
public class CfgGanttProject extends AuditOmran {
    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes")
    private String notes;

    @Column(name = "StartDate")
    private LocalDateTime startDate;

    @Column(name = "VersionDate")
    private LocalDateTime versionDate;

    @Column(name = "CfgGanttProjectMainVersionId")
    private UUID mainVersionId;

    @Column(name = "ObjectId")
    private UUID objectId;

    @Column(name = "StrEntityObjectId")
    private UUID strEntityObjectId;
}

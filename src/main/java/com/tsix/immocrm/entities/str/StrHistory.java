package com.tsix.immocrm.entities.str;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;
import java.util.UUID;

/** Piste d'audit générique du légataire, filtrée par {@code ObjectId}. */
@Getter
@Setter
@Entity
@Table(name = "StrHistory")
public class StrHistory {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Column(name = "DocDate")
    private LocalDateTime docDate;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes", length = 1000)
    private String notes;

    @Column(name = "ObjectId")
    private UUID objectId;

    @Column(name = "AdmUserId")
    private UUID admUserId;

    @Size(max = 255)
    @Nationalized
    @Column(name = "AdmUserDescription")
    private String admUserDescription;

    @Column(name = "StrWorkflowId")
    private UUID strWorkflowId;

    @Size(max = 255)
    @Nationalized
    @Column(name = "StrWorkflowDescription")
    private String strWorkflowDescription;

    @Column(name = "StrStatusId")
    private UUID strStatusId;

    @Size(max = 255)
    @Nationalized
    @Column(name = "StrStatusDescription")
    private String strStatusDescription;

    @Column(name = "UserDate")
    private LocalDateTime userDate;

    @Column(name = "StrConfigEntityId")
    private UUID strConfigEntityId;
}

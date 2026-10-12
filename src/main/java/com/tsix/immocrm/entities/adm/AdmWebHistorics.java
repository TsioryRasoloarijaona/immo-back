package com.tsix.immocrm.entities.adm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/** Journal d'audit d'OMRAN (écran « Audit ») : une ligne par objet écrit. Écrit par {@code EcouteurAuditOmran} seul. */
@Getter
@Setter
@Entity
@Table(name = "AdmWebHistorics")
public class AdmWebHistorics {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Nationalized
    @Column(name = "UserDescription")
    private String userDescription;

    @Column(name = "ActionDateTime")
    private LocalDateTime actionDateTime;

    @Column(name = "ObjectId")
    private UUID objectId;

    @Column(name = "LockEntityId")
    private UUID lockEntityId;

    @Nationalized
    @Column(name = "Action")
    private String action;

    @Nationalized
    @Column(name = "ObjectType")
    private String objectType;

    @Nationalized
    @Column(name = "ObjectTypeLabel")
    private String objectTypeLabel;

    @Nationalized
    @Column(name = "ObjectLabel")
    private String objectLabel;

    @Nationalized
    @Column(name = "Description")
    private String description;

    // Colonne "text" (ANSI), pas "ntext" : en @Nationalized le driver la lie en NVARCHAR et SQL Server
    // refuse la conversion (même piège que ComFolder.ComDocumentDetail).
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "Details")
    private String details;

    @Size(max = 255)
    @Nationalized
    @Column(name = "ParentObjectType")
    private String parentObjectType;

    @Column(name = "ParentObjectId")
    private UUID parentObjectId;

    @Size(max = 255)
    @Column(name = "SystemVersion")
    private String systemVersion;

    @Column(name = "AdmWebSessionsId")
    private UUID admWebSessionsId;
}

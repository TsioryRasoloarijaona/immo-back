package com.tsix.immocrm.entities.str;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.util.UUID;

/**
 * Generic, cross-module workflow status (shared by several "Str*" tables — Dossier statuses
 * are the subset where {@code StrConfigEntityId} identifies the Dossier config entity).
 * Only the fields needed for read/display are mapped; the DB table also carries an
 * IsReadOnly/CanDelete/BoolField001-020 admin-configuration block not used here.
 */
@Getter
@Setter
@Entity
@Table(name = "StrStatus")
public class StrStatus {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;

    @Column(name = "Sorting")
    private Integer sorting;

    @Size(max = 255)
    @Nationalized
    @Column(name = "ColorName")
    private String colorName;

    @Column(name = "StrConfigEntityId")
    private UUID strConfigEntityId;
}

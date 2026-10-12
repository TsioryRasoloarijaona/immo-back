package com.tsix.immocrm.entities;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

/** Couple {@code Code} / {@code Description} en nvarchar(255) d'une table OMRAN auditée. */
@Getter
@Setter
@MappedSuperclass
public abstract class CodeLibelleOmran extends AuditOmran {

    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;
}

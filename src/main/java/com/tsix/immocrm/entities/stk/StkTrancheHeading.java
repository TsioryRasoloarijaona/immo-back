package com.tsix.immocrm.entities.stk;

import com.tsix.immocrm.entities.CodeLibelleOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Nationalized;

import java.util.UUID;

/** Rubrique de tranche (Configuration › Autres d'OMRAN), référentiel en lecture seule. */
@Getter
@Setter
@Entity
@Immutable
@Table(name = "StkTrancheHeading")
public class StkTrancheHeading extends CodeLibelleOmran {

    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Column(name = "Sorting")
    private Integer sorting;

    @Size(max = 500)
    @Nationalized
    @Column(name = "Notes", length = 500)
    private String notes;
}

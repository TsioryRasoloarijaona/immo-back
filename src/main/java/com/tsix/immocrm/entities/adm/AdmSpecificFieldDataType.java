package com.tsix.immocrm.entities.adm;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Nationalized;

import java.util.UUID;

/** Type de donnée d'un champ spécifique (référentiel OMRAN fixe, sans {@code Description}). */
@Getter
@Entity
@Immutable
@Table(name = "AdmSpecificFieldDataType")
public class AdmSpecificFieldDataType {

    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;

    @Column(name = "Sorting")
    private Integer sorting;
}

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

/** Statut générique, partagé par plusieurs modules : ceux des dossiers sont filtrés par {@code StrConfigEntityId}. */
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

    /** Durée de validité du statut en jours ; NULL ou 0 = sans échéance. */
    @Column(name = "Delay")
    private Integer delay;

    // OMRAN « Transforme le prospect en client » ; NULL vaut faux.
    @Column(name = "BoolField011")
    private Boolean transformeProspectEnClient;
}

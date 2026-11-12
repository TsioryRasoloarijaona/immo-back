package com.tsix.immocrm.entities.prj;

import com.tsix.immocrm.entities.AuditOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Nationalized;

import java.util.UUID;

/** Regroupement des lignes de bordereau d'un lot (étage, appartement…). */
@Getter
@Setter
@Entity
@Immutable
@Table(name = "PrjMarketLineGrouping")
public class PrjMarketLineGrouping extends AuditOmran {
    @Column(name = "PrjMarketId")
    private UUID prjMarketId;

    @Column(name = "Sorting")
    private Integer sorting;

    @Column(name = "IsStock")
    private Boolean isStock;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes")
    private String notes;
}

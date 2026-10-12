package com.tsix.immocrm.entities.prj;

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

/** Groupe de lignes du bordereau définitif (un seul niveau rempli en base). */
@Getter
@Setter
@Entity
@Immutable
@Table(name = "PrjMarketLineGrouping")
public class PrjMarketLineGrouping {

    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Column(name = "PrjMarketId")
    private UUID prjMarketId;

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
}

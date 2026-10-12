package com.tsix.immocrm.entities.prj;

import com.tsix.immocrm.entities.CodeLibelleOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/** Groupe de lignes du bordereau définitif (un seul niveau rempli en base). */
@Getter
@Setter
@Entity
@Immutable
@Table(name = "PrjMarketLineGrouping")
public class PrjMarketLineGrouping extends CodeLibelleOmran {

    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Column(name = "PrjMarketId")
    private UUID prjMarketId;

    @Column(name = "Sorting")
    private Integer sorting;
}

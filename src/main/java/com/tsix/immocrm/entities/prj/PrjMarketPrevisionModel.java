package com.tsix.immocrm.entities.prj;

import com.tsix.immocrm.entities.CodeLibelleOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Modèle de simulation lot (FEATURE-lot-previsions-reel.md §4). */
@Getter
@Setter
@Entity
@Table(name = "PrjMarketPrevisionModel")
public class PrjMarketPrevisionModel extends CodeLibelleOmran {

    @Column(name = "StlCategoryId")
    private UUID stlCategoryId;
}

package com.tsix.immocrm.entities.cfg;

import com.tsix.immocrm.entities.LigneOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.util.UUID;

/** Nœud de la définition de nivellement d'un projet ; {@code CfgTrancheId} référence {@code PrjProject}, pas une tranche. */
@Getter
@Setter
@Entity
@Table(name = "CfgTrancheLevelingDefinition")
public class CfgTrancheLevelingDefinition extends LigneOmran {
    @Column(name = "ParentId")
    private UUID parentId;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;

    @Column(name = "Sorting")
    private Integer sorting;

    @Column(name = "CfgTrancheId")
    private UUID cfgTrancheId;
}

package com.tsix.immocrm.entities.cfg;

import com.tsix.immocrm.entities.stk.StkItemCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Onglet Plancher couvert de la tranche : Σ {@code Area} = {@code CfgTranche.CoveredFloorArea}. */
@Getter
@Setter
@Entity
@Table(name = "CfgTrancheCoveredFloor")
public class CfgTrancheCoveredFloor extends LigneTrancheOmran {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkItemCategoryId")
    private StkItemCategory stkItemCategory;

    @Column(name = "Area", precision = 26, scale = 16)
    private BigDecimal area;
}

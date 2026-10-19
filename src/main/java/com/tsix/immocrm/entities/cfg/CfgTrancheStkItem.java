package com.tsix.immocrm.entities.cfg;

import com.tsix.immocrm.entities.stk.StkItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Onglet Terrains de la tranche. */
@Getter
@Setter
@Entity
@Table(name = "CfgTrancheStkItem")
public class CfgTrancheStkItem extends LigneTrancheOmran {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkItemId")
    private StkItem stkItem;

    @Column(name = "Ratio", precision = 26, scale = 6)
    private BigDecimal ratio;

    @Column(name = "Sorting")
    private Integer sorting;
}

package com.tsix.immocrm.entities.stk;

import com.tsix.immocrm.entities.LigneOmran;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/** Colonnes communes à {@code FSPrevision} et {@code StkFSPrevisionStkTranche} : une catégorie de l'étude et ses montants. */
@Getter
@Setter
@MappedSuperclass
public abstract class LignePrevisionOmran extends LigneOmran {
    @Column(name = "FeasibilityStudyId")
    private UUID feasibilityStudyId;

    @Column(name = "StlCategoryId")
    private UUID stlCategoryId;

    @Column(name = "Amount", precision = 26, scale = 6)
    private BigDecimal amount;

    @Column(name = "AmountHT", precision = 26, scale = 6)
    private BigDecimal amountHt;

    @Column(name = "Ratio", precision = 26, scale = 6)
    private BigDecimal ratio;

    @Column(name = "VatRatio", precision = 26, scale = 6)
    private BigDecimal vatRatio;

    @Column(name = "Undivided")
    private Boolean undivided;
}

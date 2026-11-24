package com.tsix.immocrm.entities.stl;

import com.tsix.immocrm.entities.LigneOmran;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.util.UUID;

/** Ligne de retenue à la source d'un règlement. */
@Getter
@Setter
@Entity
@Table(name = "StlDeduction")
public class StlDeduction extends LigneOmran {
    @Column(name = "StlSettlementId")
    private UUID stlSettlementId;

    @Column(name = "StlDeductionTypeId")
    private UUID stlDeductionTypeId;

    @Column(name = "AmountBase", precision = 26, scale = 6)
    private BigDecimal amountBase;

    @Column(name = "RsRatio", precision = 26, scale = 6)
    private BigDecimal rsRatio;

    @Column(name = "AmountRS", precision = 26, scale = 6)
    private BigDecimal amountRS;

    @Column(name = "Amount", precision = 26, scale = 6)
    private BigDecimal amount;

    @Nationalized
    @Column(name = "Notes")
    private String notes;
}

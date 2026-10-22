package com.tsix.immocrm.entities.stk;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Ligne de trésorerie d'une étude ; dérivée, réécrite à chaque recalcul ; table sans colonne d'audit. */
@Getter
@Setter
@Entity
@Table(name = "StkFSPrevisionLine")
public class StkFSPrevisionLine {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Column(name = "DocDate")
    private LocalDateTime docDate;

    @Column(name = "DateMonth")
    private Integer dateMonth;

    @Column(name = "DateYear")
    private Integer dateYear;

    @Column(name = "Amount", precision = 26, scale = 6)
    private BigDecimal amount;

    @Column(name = "StkFeasibilityStudyId")
    private UUID stkFeasibilityStudyId;

    @Column(name = "StkFeasibilityStudyCfgTrancheId")
    private UUID stkFeasibilityStudyCfgTrancheId;

    @Size(max = 255)
    @Nationalized
    @Column(name = "PrevisionType")
    private String previsionType;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes")
    private String notes;

    @Column(name = "IsCredit")
    private Boolean isCredit;

    @Column(name = "AmountHT", precision = 26, scale = 6)
    private BigDecimal amountHt;

    @Column(name = "VatRatio", precision = 26, scale = 6)
    private BigDecimal vatRatio;
}

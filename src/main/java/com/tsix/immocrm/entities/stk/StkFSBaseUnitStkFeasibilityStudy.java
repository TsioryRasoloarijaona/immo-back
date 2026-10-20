package com.tsix.immocrm.entities.stk;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/** Unité de base d'une étude (onglet Unités de base) ; table sans colonne d'audit. */
@Getter
@Setter
@Entity
@Table(name = "StkFSBaseUnitStkFeasibilityStudy")
public class StkFSBaseUnitStkFeasibilityStudy {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Column(name = "Sorting")
    private Integer sorting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FeasibilityStudyId")
    private StkFeasibilityStudy feasibilityStudy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkFSBaseUnitId")
    private StkFSBaseUnit stkFSBaseUnit;

    @Column(name = "Quantity", precision = 26, scale = 6)
    private BigDecimal quantity;
}

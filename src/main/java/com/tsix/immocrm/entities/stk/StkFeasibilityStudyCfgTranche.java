package com.tsix.immocrm.entities.stk;

import com.tsix.immocrm.entities.LigneOmran;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Tranche d'une étude (onglet Tranches) : Code et Sorting, sans Description. */
@Getter
@Setter
@Entity
@Table(name = "StkFeasibilityStudyCfgTranche")
public class StkFeasibilityStudyCfgTranche extends LigneOmran {
    @Column(name = "Sorting")
    private Integer sorting;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FeasibilityStudyId")
    private StkFeasibilityStudy feasibilityStudy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgTrancheId")
    private CfgTranche cfgTranche;

    @Column(name = "WorkStartDate")
    private LocalDateTime workStartDate;

    @Column(name = "WorkEndDate")
    private LocalDateTime workEndDate;

    @Column(name = "MarketingStartDate")
    private LocalDateTime marketingStartDate;

    @Column(name = "EndDateConcretisation")
    private LocalDateTime endDateConcretisation;

    @Column(name = "StartDateFinalisation")
    private LocalDateTime startDateFinalisation;

    @Column(name = "MarketingEndDate")
    private LocalDateTime marketingEndDate;

    @Column(name = "DeliveryDate")
    private LocalDateTime deliveryDate;

    @Column(name = "OccupancyPermitsDate")
    private LocalDateTime occupancyPermitsDate;

    @Column(name = "Weight", precision = 26, scale = 6)
    private BigDecimal weight;
}

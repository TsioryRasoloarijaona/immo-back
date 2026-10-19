package com.tsix.immocrm.entities.cfg;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Ligne mensuelle des prévisions de base ; liée à la tranche par {@code StkHierarchyId}, sans trace OMRAN. */
@Getter
@Setter
@Entity
@Table(name = "CfgPrevision")
public class CfgPrevision {
    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkHierarchyId")
    private CfgTranche cfgTranche;

    @Column(name = "DocDate")
    private LocalDateTime docDate;

    @Column(name = "\"Year\"")
    private Integer year;

    @Column(name = "\"Month\"")
    private Integer month;

    @Column(name = "Quantity")
    private Integer quantity;

    @Column(name = "AchievementAmount", precision = 26, scale = 6)
    private BigDecimal achievementAmount;

    @Column(name = "TurnoverQuantity")
    private Integer turnoverQuantity;

    @Column(name = "TurnoverAmount", precision = 26, scale = 6)
    private BigDecimal turnoverAmount;

    @Column(name = "Amount", precision = 26, scale = 6)
    private BigDecimal amount;

    @Column(name = "Turnover", precision = 26, scale = 6)
    private BigDecimal turnover;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes")
    private String notes;
}

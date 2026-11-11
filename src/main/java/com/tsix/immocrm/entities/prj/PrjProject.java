package com.tsix.immocrm.entities.prj;

import com.tsix.immocrm.entities.AuditOmran;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.str.StrEntity;
import com.tsix.immocrm.entities.str.StrStatus;
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
import java.util.UUID;

/** Projet du module Projet (pas un programme {@code CfgProject}) : « Projet de facturation pour les dépenses ». */
@Getter
@Setter
@Entity
@Table(name = "PrjProject")
public class PrjProject extends AuditOmran {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgCompanyId")
    private CfgCompany cfgCompany;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes")
    private String notes;

    @Size(max = 500)
    @Nationalized
    @Column(name = "DescriptionLong")
    private String descriptionLong;

    @Size(max = 500)
    @Nationalized
    @Column(name = "Address")
    private String address;

    @Column(name = "IsVentilated")
    private Boolean isVentilated;

    @Column(name = "defaultVentilationRate", precision = 26, scale = 6)
    private BigDecimal defaultVentilationRate;

    @Column(name = "IsACommon")
    private Boolean isACommon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StrEntityId")
    private StrEntity strEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StrStatusId")
    private StrStatus strStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgTierManagerId")
    private CfgTier cfgTierManager;

    @Column(name = "ProvisionalWorkStartDate")
    private LocalDateTime provisionalWorkStartDate;

    @Column(name = "ProvisionalCompletingWorkDate")
    private LocalDateTime provisionalCompletingWorkDate;

    @Column(name = "RealWorkStartDate")
    private LocalDateTime realWorkStartDate;

    @Column(name = "RealCompletingWorkDate")
    private LocalDateTime realCompletingWorkDate;

    @Column(name = "StkLevelingId")
    private UUID stkLevelingId;

    @Column(name = "ReferenceId")
    private UUID referenceId;

    @Column(name = "ManageProrataPrjMarketId")
    private UUID manageProrataPrjMarketId;
}

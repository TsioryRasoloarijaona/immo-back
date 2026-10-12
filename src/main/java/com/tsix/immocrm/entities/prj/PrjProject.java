package com.tsix.immocrm.entities.prj;

import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.str.StrEntity;
import com.tsix.immocrm.entities.str.StrStatus;
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
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;
import java.util.UUID;

/** Projet de travaux du module Chantier (« Projet » d'OMRAN), distinct de {@code CfgProject} : aucun lien en base. */
@Getter
@Setter
@Entity
@Immutable
@Table(name = "PrjProject")
public class PrjProject {

    @Id
    @Column(name = "PKey", nullable = false)
    private UUID id;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;

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

    @Column(name = "StartDate")
    private LocalDateTime startDate;

    @Column(name = "Delay")
    private Integer delay;

    @Column(name = "EndDate")
    private LocalDateTime endDate;

    @Column(name = "ProvisionalWorkStartDate")
    private LocalDateTime provisionalWorkStartDate;

    @Column(name = "ProvisionalCompletingWorkDate")
    private LocalDateTime provisionalCompletingWorkDate;

    @Column(name = "RealWorkStartDate")
    private LocalDateTime realWorkStartDate;

    @Column(name = "RealCompletingWorkDate")
    private LocalDateTime realCompletingWorkDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgCompanyId")
    private CfgCompany cfgCompany;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgTierManagerId")
    private CfgTier cfgTierManager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StrStatusId")
    private StrStatus strStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StrEntityId")
    private StrEntity strEntity;

    @Size(max = 255)
    @Nationalized
    @Column(name = "UserCreate")
    private String userCreate;

    @Column(name = "UserCreateDateTime")
    private LocalDateTime userCreateDateTime;
}

package com.tsix.immocrm.entities.lnd;

import com.tsix.immocrm.entities.adm.AdmCity;
import com.tsix.immocrm.entities.adm.AdmCountry;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkOrientation;
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

/**
 * Prospection terrain ({@code Yes.Lnd/Prospection.cs}). {@code Adress} et {@code AlinmentStop} gardent l'orthographe des
 * colonnes ; colonnes mortes non mappées (FEATURE-patrimoine-prospections-terrain.md §1.1).
 */
@Getter
@Setter
@Entity
@Table(name = "LndLand")
public class LndLand extends SurfacesFoncieresOmran {
    @Size(max = 255)
    @Nationalized
    @Column(name = "Code")
    private String code;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Description")
    private String description;

    @Column(name = "DocDate")
    private LocalDateTime docDate;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Anomalies", length = 1000)
    private String anomalies;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Adress")
    private String adress;

    @Column(name = "RegistryDate")
    private LocalDateTime registryDate;

    @Size(max = 255)
    @Nationalized
    @Column(name = "RegistryNumber")
    private String registryNumber;

    @Size(max = 255)
    @Nationalized
    @Column(name = "RegistryPlace")
    private String registryPlace;

    @Column(name = "ImmatriculationDate")
    private LocalDateTime immatriculationDate;

    @Size(max = 255)
    @Nationalized
    @Column(name = "ImmatriculationCPF")
    private String immatriculationCPF;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LndCategoryId")
    private StkItemCategory categorie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LndTypeId")
    private StkItemType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LndVocationId")
    private LndVocation lndVocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LndLevelId")
    private LndLevel lndLevel;

    @Column(name = "IsInArchive")
    private Boolean isInArchive;

    @Column(name = "ProspectionUnitCost", precision = 26, scale = 6)
    private BigDecimal prospectionUnitCost;

    @Column(name = "ProspectionPrice", precision = 26, scale = 6)
    private BigDecimal prospectionPrice;

    @Column(name = "ProspectionPriceProposed", precision = 26, scale = 6)
    private BigDecimal prospectionPriceProposed;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Owner")
    private String owner;

    @Column(name = "StatusValidityDate")
    private LocalDateTime statusValidityDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgMiddlemanId")
    private CfgTier cfgMiddleman;

    @Column(name = "EquipementsCoef", precision = 26, scale = 6)
    private BigDecimal equipementsCoef;

    @Column(name = "EquipementsArea", precision = 26, scale = 6)
    private BigDecimal equipementsArea;

    @Column(name = "AreaNet", precision = 26, scale = 6)
    private BigDecimal areaNet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmCountryId")
    private AdmCountry admCountry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AdmCityId")
    private AdmCity admCity;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "GmapLink", length = 1000)
    private String gmapLink;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgLawyerId")
    private CfgTier cfgLawyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkOrientationId")
    private StkOrientation stkOrientation;

    @Column(name = "IsTransformedToLand")
    private Boolean isTransformedToLand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StrStatusId")
    private StrStatus strStatus;

    @Column(name = "StrEntityId")
    private UUID strEntityId;

    @Column(name = "AreaEquipmentTransferred", precision = 26, scale = 6)
    private BigDecimal areaEquipmentTransferred;

    @Column(name = "AreaRegularized", precision = 26, scale = 6)
    private BigDecimal areaRegularized;

    @Column(name = "AreaCeded", precision = 26, scale = 6)
    private BigDecimal areaCeded;

    @Column(name = "IsFinished")
    private Boolean isFinished;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "ZoneIdentification", length = 1000)
    private String zoneIdentification;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "DealerConnectionConditions", length = 1000)
    private String dealerConnectionConditions;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "LandBase", length = 1000)
    private String landBase;

    @Column(name = "IsTopographicSurvey")
    private Boolean isTopographicSurvey;
}

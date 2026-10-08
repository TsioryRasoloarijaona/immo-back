package com.tsix.immocrm.entities.stk;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "StkItem")
public class StkItem {
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

    @Size(max = 500)
    @Nationalized
    @Column(name = "Address", length = 500)
    private String address;

    @Column(name = "Area", precision = 26, scale = 6)
    private BigDecimal area;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Notes", length = 1000)
    private String notes;

    @Column(name = "ComSaleUnitCost", precision = 26, scale = 6)
    private BigDecimal comSaleUnitCost;

    @Column(name = "ComSaleMinAdvance", precision = 26, scale = 6)
    private BigDecimal comSaleMinAdvance;

    @Column(name = "ComSalePrice", precision = 26, scale = 6)
    private BigDecimal comSalePrice;

    @Column(name = "SalePrice", precision = 26, scale = 6)
    private BigDecimal salePrice;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Contenance", length = 1000)
    private String contenance;

    @Column(name = "IsUnderItem")
    private Boolean isUnderItem;

    @Column(name = "AreaNetOld", precision = 26, scale = 6)
    private BigDecimal areaNetOld;

    @Column(name = "AreaFloor", precision = 26, scale = 6)
    private BigDecimal areaFloor;

    @Column(name = "AlwaysAffected")
    private Boolean alwaysAffected;

    @Size(max = 50)
    @Nationalized
    @Column(name = "ElectricityRef", length = 50)
    private String electricityRef;

    @Size(max = 50)
    @Nationalized
    @Column(name = "ElectricityMeter", length = 50)
    private String electricityMeter;

    @Size(max = 50)
    @Nationalized
    @Column(name = "ElectricityMeterIndex", length = 50)
    private String electricityMeterIndex;

    @Size(max = 50)
    @Nationalized
    @Column(name = "ElectricityMeterPolice", length = 50)
    private String electricityMeterPolice;

    @Size(max = 50)
    @Nationalized
    @Column(name = "GasMeter", length = 50)
    private String gasMeter;

    @Size(max = 50)
    @Nationalized
    @Column(name = "GasMeterIndex", length = 50)
    private String gasMeterIndex;

    @Size(max = 50)
    @Nationalized
    @Column(name = "GasMeterPolice", length = 50)
    private String gasMeterPolice;

    @Size(max = 50)
    @Nationalized
    @Column(name = "WaterMeterPolice", length = 50)
    private String waterMeterPolice;

    @Size(max = 50)
    @Nationalized
    @Column(name = "WaterMeter", length = 50)
    private String waterMeter;

    @Size(max = 50)
    @Nationalized
    @Column(name = "WaterMeterIndex", length = 50)
    private String waterMeterIndex;

    @Column(name = "DocDate")
    private LocalDateTime docDate;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "Anomalies", length = 1000)
    private String anomalies;

    @Column(name = "Cos", precision = 26, scale = 6)
    private BigDecimal cos;

    @Column(name = "Cuf", precision = 26, scale = 6)
    private BigDecimal cuf;

    @Column(name = "AlinmentStop")
    private Integer alinmentStop;

    @Column(name = "NeighborStop")
    private Integer neighborStop;

    @Column(name = "MaxHeight")
    private Integer maxHeight;

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

    @Column(name = "PurchaseDate")
    private LocalDateTime purchaseDate;

    @Column(name = "PurchaseUnitCost", precision = 26, scale = 6)
    private BigDecimal purchaseUnitCost;

    @Column(name = "PurchaseCost", precision = 26, scale = 6)
    private BigDecimal purchaseCost;

    @Column(name = "AmountHT", precision = 26, scale = 6)
    private BigDecimal amountHT;

    @Column(name = "CautionNumber")
    private Integer cautionNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ParentId")
    private StkItem parent;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "StkItemTypeId", nullable = false)
    private StkItemType stkItemType;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "StkItemCategoryId", nullable = false)
    private StkItemCategory stkItemCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgSupplierId")
    private CfgTier cfgSupplier;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "StkVocationId", nullable = false)
    private StkVocation stkVocation;

    @Size(max = 255)
    @Nationalized
    @Column(name = "UserCreate")
    private String userCreate;

    @Column(name = "UserCreateDateTime")
    private LocalDateTime userCreateDateTime;

    @Size(max = 255)
    @Nationalized
    @Column(name = "UserUpdate")
    private String userUpdate;

    @Column(name = "UserUpdateDateTime")
    private LocalDateTime userUpdateDateTime;

    @Column(name = "ComFolderId")
    private UUID comFolderId;

    @Size(max = 255)
    @Nationalized
    @Column(name = "WaterRef")
    private String waterRef;

    @Size(max = 255)
    @Nationalized
    @Column(name = "GasRef")
    private String gasRef;

    @Size(max = 255)
    @Nationalized
    @Column(name = "TitleNumberMother")
    private String titleNumberMother;

    @Size(max = 255)
    @Nationalized
    @Column(name = "TitleNumber")
    private String titleNumber;

    @Size(max = 255)
    @Nationalized
    @Column(name = "TitleName")
    private String titleName;

    @Column(name = "TitleDate")
    private LocalDateTime titleDate;

    @Size(max = 255)
    @Nationalized
    @Column(name = "TitleNotes")
    private String titleNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgProjectId")
    private CfgProject cfgProject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkHierarchyId")
    private CfgTranche stkHierarchy;

    @Column(name = "DeliveryDate")
    private LocalDateTime deliveryDate;

    @Size(max = 255)
    @Nationalized
    @Column(name = "PropertyCalled")
    private String propertyCalled;

    @Size(max = 255)
    @Nationalized
    @Column(name = "FractionDivided")
    private String fractionDivided;

    @Column(name = "DividedPart", precision = 26, scale = 6)
    private BigDecimal dividedPart;

    @Column(name = "UndividedPart", precision = 26, scale = 6)
    private BigDecimal undividedPart;

    @Column(name = "Vat", precision = 26, scale = 6)
    private BigDecimal vat;

    @Column(name = "AreaCos", precision = 26, scale = 6)
    private BigDecimal areaCos;

    @Column(name = "AreaCuf", precision = 26, scale = 6)
    private BigDecimal areaCuf;

    @Column(name = "ComSaleUnitCostFloor", precision = 26, scale = 6)
    private BigDecimal comSaleUnitCostFloor;

    @Column(name = "SalePriceFloor", precision = 26, scale = 6)
    private BigDecimal salePriceFloor;

    @Column(name = "ComSalePriceFloor", precision = 26, scale = 6)
    private BigDecimal comSalePriceFloor;

    @Column(name = "IsUnderItemOrigin")
    private Boolean isUnderItemOrigin;

    @Size(max = 255)
    @Nationalized
    @Column(name = "FractionDividedLand")
    private String fractionDividedLand;

    @Column(name = "DiscountMax", precision = 26, scale = 6)
    private BigDecimal discountMax;

    @Column(name = "EquipementsCoef", precision = 26, scale = 6)
    private BigDecimal equipementsCoef;

    @Column(name = "EquipementsArea", precision = 26, scale = 6)
    private BigDecimal equipementsArea;

    @Column(name = "SalableArea", precision = 26, scale = 6)
    private BigDecimal salableArea;

    @Column(name = "CoefSaleAbleArea", precision = 26, scale = 6)
    private BigDecimal coefSaleAbleArea;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgMiddlemanId")
    private CfgTier cfgMiddleman;

    @Column(name = "ProspectionUnitCost", precision = 26, scale = 6)
    private BigDecimal prospectionUnitCost;

    @Column(name = "ProspectionPrice", precision = 26, scale = 6)
    private BigDecimal prospectionPrice;

    @Column(name = "ProspectionPriceProposed", precision = 26, scale = 6)
    private BigDecimal prospectionPriceProposed;

    @Column(name = "UnitCostNonBuiltPrice", precision = 26, scale = 6)
    private BigDecimal unitCostNonBuiltPrice;

    @Size(max = 255)
    @Nationalized
    @Column(name = "Owner")
    private String owner;

    @Size(max = 1000)
    @Nationalized
    @Column(name = "GmapLink", length = 1000)
    private String gmapLink;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgLawyerId")
    private CfgTier cfgLawyer;

    @Column(name = "ComSaleUnitCostFloorHT", precision = 26, scale = 6)
    private BigDecimal comSaleUnitCostFloorHT;

    @Column(name = "ComSaleUnitCostHT", precision = 26, scale = 6)
    private BigDecimal comSaleUnitCostHT;

    @Size(max = 255)
    @Nationalized
    @Column(name = "GroupLandQuota")
    private String groupLandQuota;

    @Column(name = "SalePriceHT", precision = 26, scale = 6)
    private BigDecimal salePriceHT;

    @Column(name = "ComSalePriceHT", precision = 26, scale = 6)
    private BigDecimal comSalePriceHT;

    @Column(name = "SalePriceFloorHT", precision = 26, scale = 6)
    private BigDecimal salePriceFloorHT;

    @Column(name = "ComSalePriceFloorHT", precision = 26, scale = 6)
    private BigDecimal comSalePriceFloorHT;

    @Column(name = "IsAvailableForRentInWebSite")
    private Boolean isAvailableForRentInWebSite;

    @Column(name = "IsAvailableForSaleInWebSite")
    private Boolean isAvailableForSaleInWebSite;

    @Column(name = "BuildingPermissionDate")
    private LocalDateTime buildingPermissionDate;

    @Column(name = "IsTitled")
    private Boolean isTitled;

    @Column(name = "InteriorArea", precision = 26, scale = 6)
    private BigDecimal interiorArea;

    @Column(name = "WallsArea", precision = 26, scale = 6)
    private BigDecimal wallsArea;

    @Column(name = "CommonPartsArea", precision = 26, scale = 6)
    private BigDecimal commonPartsArea;

    @Column(name = "DiscountMax2", precision = 26, scale = 6)
    private BigDecimal discountMax2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgOwnerId")
    private CfgTier cfgOwner;

    @Column(name = "MapLatitude", precision = 20, scale = 16)
    private BigDecimal mapLatitude;

    @Column(name = "MapLongitude", precision = 20, scale = 16)
    private BigDecimal mapLongitude;

    @Column(name = "CommissionRate", precision = 20, scale = 16)
    private BigDecimal commissionRate;

    @Column(name = "IsReversmentBasedOnPayment")
    private Boolean isReversmentBasedOnPayment;

    @Column(name = "PrecisionActDate")
    private LocalDateTime precisionActDate;

    @Column(name = "PossessionDate")
    private LocalDateTime possessionDate;

    @Column(name = "LastSettlementDate")
    private LocalDateTime lastSettlementDate;

    @Column(name = "ImmatriculationDateDeposit")
    private LocalDateTime immatriculationDateDeposit;

    @Column(name = "ImmatriculationDateWithdrawal")
    private LocalDateTime immatriculationDateWithdrawal;

    @Column(name = "StudyReportDate")
    private LocalDateTime studyReportDate;

    @Column(name = "ReservationDate")
    private LocalDateTime reservationDate;

    @Column(name = "ReservationOwnerDate")
    private LocalDateTime reservationOwnerDate;

    @Column(name = "ReservationRegistryDate")
    private LocalDateTime reservationRegistryDate;

    @Column(name = "AgreementSavingDate")
    private LocalDateTime agreementSavingDate;

    @Column(name = "AgreementSavingOwnerDate")
    private LocalDateTime agreementSavingOwnerDate;

    @Column(name = "AgreementRegistryDate")
    private LocalDateTime agreementRegistryDate;

    @Column(name = "CreditContractSignatureDate")
    private LocalDateTime creditContractSignatureDate;

    @Column(name = "CreditContractSignatureBankDate")
    private LocalDateTime creditContractSignatureBankDate;

    @Column(name = "CreditContractRegistryDate")
    private LocalDateTime creditContractRegistryDate;

    @Column(name = "DepositCPF1Date")
    private LocalDateTime depositCPF1Date;

    @Column(name = "OwnershipCertificate1Date")
    private LocalDateTime ownershipCertificate1Date;

    @Column(name = "RaisedHandDate")
    private LocalDateTime raisedHandDate;

    @Column(name = "RaisedHandRegistryDate")
    private LocalDateTime raisedHandRegistryDate;

    @Column(name = "RaisedHand1Date")
    private LocalDateTime raisedHand1Date;

    @Column(name = "RaisedHand1RegistryDate")
    private LocalDateTime raisedHand1RegistryDate;

    @Column(name = "DepositCPFDate")
    private LocalDateTime depositCPFDate;

    @Column(name = "OwnershipCertificateDate")
    private LocalDateTime ownershipCertificateDate;

    @Column(name = "AreaEquipmentTransferred", precision = 26, scale = 6)
    private BigDecimal areaEquipmentTransferred;

    @Column(name = "AreaRegularized", precision = 26, scale = 6)
    private BigDecimal areaRegularized;

    @Column(name = "AreaCeded", precision = 26, scale = 6)
    private BigDecimal areaCeded;

    @Column(name = "IsFinished")
    private Boolean isFinished;

    @Column(name = "CommerceCoef", precision = 26, scale = 6)
    private BigDecimal commerceCoef;

    @Column(name = "CommerceArea", precision = 26, scale = 6)
    private BigDecimal commerceArea;

    @Column(name = "OfficeCoef", precision = 26, scale = 6)
    private BigDecimal officeCoef;

    @Column(name = "OfficeArea", precision = 26, scale = 6)
    private BigDecimal officeArea;

    @Column(name = "HabitatArea", precision = 26, scale = 6)
    private BigDecimal habitatArea;

    @Column(name = "UndergroundParkingArea", precision = 26, scale = 6)
    private BigDecimal undergroundParkingArea;

    @Column(name = "UndergroundParkingCoef", precision = 26, scale = 6)
    private BigDecimal undergroundParkingCoef;

    // Fiche de réservation : "imm {StkBuilding.Code}, porte {Description}, au {StkFloor.Description}".
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkBuildingId")
    private StkBuilding stkBuilding;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkFloorId")
    private StkFloor stkFloor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkKindId")
    private StkKind stkKind;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkPresentationTypeId")
    private StkPresentationType stkPresentationType;
}
package com.tsix.immocrm.entities.stk;

import com.tsix.immocrm.entities.AuditOmran;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Étude de faisabilité (FEATURE-patrimoine-faisabilite.md §1). Piège OMRAN : {@code OtherCostsExecutionHT} et
 * {@code OtherCostsExecutionVatAmount} portent les autres frais de TERRAIN ; {@code Delay*} sans suffixe = durée de la phase.
 */
@Getter
@Setter
@Entity
@Table(name = "StkFeasibilityStudy")
public class StkFeasibilityStudy extends AuditOmran {
    @Column(name = "DocDate")
    private LocalDateTime docDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CfgCompanyId")
    private CfgCompany cfgCompany;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkItemId")
    private StkItem stkItem;

    @Column(name = "LndLandId")
    private UUID lndLandId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StkFeasibilityStudyStatusId")
    private StkFeasibilityStudyStatus status;

    @Column(name = "StrEntityId")
    private UUID strEntityId;

    @Column(name = "IsLocked")
    private Boolean isLocked;

    @Size(max = 255)
    @Column(name = "LandDescription")
    private String landDescription;

    @Column(name = "NumberOfItemForSale")
    private Integer numberOfItemForSale;

    @Column(name = "UseLandProportionForTVACalculation")
    private Boolean useLandProportionForTvaCalculation;

    @Column(name = "DefaultVat", precision = 26, scale = 6)
    private BigDecimal defaultVat;

    @Column(name = "InterestVatRatio", precision = 26, scale = 6)
    private BigDecimal interestVatRatio;

    // ------------------------------------------------------------------ bloc A : surfaces
    @Column(name = "Area", precision = 26, scale = 6)
    private BigDecimal area;

    @Column(name = "EquipementsCoef", precision = 26, scale = 6)
    private BigDecimal equipementsCoef;

    @Column(name = "EquipementsAmount", precision = 26, scale = 6)
    private BigDecimal equipementsAmount;

    @Column(name = "AreaNet", precision = 26, scale = 6)
    private BigDecimal areaNet;

    @Column(name = "OccupationSolRatio", precision = 26, scale = 6)
    private BigDecimal occupationSolRatio;

    @Column(name = "OccupationGroundArea", precision = 26, scale = 6)
    private BigDecimal occupationGroundArea;

    @Column(name = "Cuf", precision = 26, scale = 6)
    private BigDecimal cuf;

    @Column(name = "TotalCoveredArea", precision = 26, scale = 6)
    private BigDecimal totalCoveredArea;

    @Column(name = "CoefSaleableArea", precision = 26, scale = 6)
    private BigDecimal coefSaleableArea;

    @Column(name = "SalableArea", precision = 26, scale = 6)
    private BigDecimal salableArea;

    @Column(name = "NoBuiltArea", precision = 26, scale = 6)
    private BigDecimal noBuiltArea;

    // ------------------------------------------------------------------ bloc A : coûts du terrain
    @Column(name = "LandVatRatio", precision = 26, scale = 6)
    private BigDecimal landVatRatio;

    @Column(name = "PurchaseUnitCost", precision = 26, scale = 6)
    private BigDecimal purchaseUnitCost;

    @Column(name = "PurchaseUnitCostHT", precision = 26, scale = 6)
    private BigDecimal purchaseUnitCostHt;

    @Column(name = "PurchaseUnitCostVatAmount", precision = 26, scale = 6)
    private BigDecimal purchaseUnitCostVatAmount;

    @Column(name = "PriceArea", precision = 26, scale = 6)
    private BigDecimal priceArea;

    @Column(name = "PriceAreaHT", precision = 26, scale = 6)
    private BigDecimal priceAreaHt;

    @Column(name = "PriceAreaVatAmount", precision = 26, scale = 6)
    private BigDecimal priceAreaVatAmount;

    @Column(name = "RegistryCostsRatio", precision = 26, scale = 6)
    private BigDecimal registryCostsRatio;

    @Column(name = "RegistryCosts", precision = 26, scale = 6)
    private BigDecimal registryCosts;

    @Column(name = "RegistryCostsHT", precision = 26, scale = 6)
    private BigDecimal registryCostsHt;

    @Column(name = "RegistryCostsVatAmount", precision = 26, scale = 6)
    private BigDecimal registryCostsVatAmount;

    @Column(name = "OtherCostsLandRatio", precision = 26, scale = 6)
    private BigDecimal otherCostsLandRatio;

    @Column(name = "OtherCostsLand", precision = 26, scale = 6)
    private BigDecimal otherCostsLand;

    @Column(name = "OtherCostsExecutionHT", precision = 26, scale = 6)
    private BigDecimal otherCostsLandHt;

    @Column(name = "OtherCostsExecutionVatAmount", precision = 26, scale = 6)
    private BigDecimal otherCostsLandVatAmount;

    @Column(name = "TotalCostLand", precision = 26, scale = 6)
    private BigDecimal totalCostLand;

    // ------------------------------------------------------------------ bloc A : construction
    @Column(name = "UnitCostConstructionBuilt", precision = 26, scale = 6)
    private BigDecimal unitCostConstructionBuilt;

    @Column(name = "CostAreaBuilt", precision = 26, scale = 6)
    private BigDecimal costAreaBuilt;

    @Column(name = "UnitCostConstructionNoBuilt", precision = 26, scale = 6)
    private BigDecimal unitCostConstructionNoBuilt;

    @Column(name = "CostAreaNoBuilt", precision = 26, scale = 6)
    private BigDecimal costAreaNoBuilt;

    @Column(name = "otherCostsExecutionRatio", precision = 26, scale = 6)
    private BigDecimal otherCostsExecutionRatio;

    @Column(name = "OtherCostsExecution", precision = 26, scale = 6)
    private BigDecimal otherCostsExecution;

    // ------------------------------------------------------------------ phases (durées et délais en mois)
    @Column(name = "StartDatePurchaseLand")
    private LocalDateTime startDatePurchaseLand;

    @Column(name = "EndDatePurchaseLand")
    private LocalDateTime endDatePurchaseLand;

    @Column(name = "StartDatePeriodeOfValidation")
    private LocalDateTime startDatePeriodeOfValidation;

    @Column(name = "EndDatePeriodeOfValidation")
    private LocalDateTime endDatePeriodeOfValidation;

    @Column(name = "StartDatePermissionBatir")
    private LocalDateTime startDatePermissionBatir;

    @Column(name = "EndDatePermissionBatir")
    private LocalDateTime endDatePermissionBatir;

    @Column(name = "StartDateStudy")
    private LocalDateTime startDateStudy;

    @Column(name = "EndDateStudy")
    private LocalDateTime endDateStudy;

    @Column(name = "StartDateExpenses")
    private LocalDateTime startDateExpenses;

    @Column(name = "EndDateExpenses")
    private LocalDateTime endDateExpenses;

    @Column(name = "StartDateConcretisation")
    private LocalDateTime startDateConcretisation;

    @Column(name = "EndDateConcretisation")
    private LocalDateTime endDateConcretisation;

    @Column(name = "StartDateFinalization")
    private LocalDateTime startDateFinalization;

    @Column(name = "EndDateFinalization")
    private LocalDateTime endDateFinalization;

    @Column(name = "DelayPurchaseLand")
    private Integer delayPurchaseLand;

    @Column(name = "DelayPeriodeOfValidation")
    private Integer delayPeriodeOfValidation;

    @Column(name = "DelayPermissionBatir")
    private Integer delayPermissionBatir;

    @Column(name = "DelayStudy")
    private Integer delayStudy;

    @Column(name = "DelayExpenses")
    private Integer delayExpenses;

    @Column(name = "DelayConcretisation")
    private Integer delayConcretisation;

    @Column(name = "DelayFinalization")
    private Integer delayFinalization;

    @Column(name = "DelayEndDatePurchaseLandStartDatePeriodeOfValidation")
    private Integer delayPurchaseLandToValidation;

    @Column(name = "DelayEndDatePeriodeOfValidationStartDatePermissionBatir")
    private Integer delayValidationToPermission;

    @Column(name = "DelayStartDatePermissionBatirStartDateStudy")
    private Integer delayPermissionToStudy;

    @Column(name = "DelayEndDatePermissionBatirStartDateExpenses")
    private Integer delayPermissionToExpenses;

    @Column(name = "DelayStartDateExpenseStartDateConcretisations")
    private Integer delayExpensesToConcretisation;

    @Column(name = "DelayStartDateExpenseStartDateFinalization")
    private Integer delayExpensesToFinalization;

    @Column(name = "EcartTypeExpenses", precision = 26, scale = 6)
    private BigDecimal ecartTypeExpenses;

    @Column(name = "EcartTypeConcretisation", precision = 26, scale = 6)
    private BigDecimal ecartTypeConcretisation;

    @Column(name = "EcartTypeFinalization", precision = 26, scale = 6)
    private BigDecimal ecartTypeFinalization;

    @Column(name = "BackTimeOnDays")
    private Integer backTimeOnDays;

    @Column(name = "BackGuaranteeRate", precision = 26, scale = 6)
    private BigDecimal backGuaranteeRate;

    @Column(name = "BackGuaranteeApplicationRate", precision = 26, scale = 6)
    private BigDecimal backGuaranteeApplicationRate;

    // ------------------------------------------------------------------ bilan : entrées (pourcentage, IS, prix de vente) et colonnes écrites par le recalcul
    @Column(name = "Turnover", precision = 26, scale = 6)
    private BigDecimal turnover;

    @Column(name = "TotalCostExecution", precision = 26, scale = 6)
    private BigDecimal totalCostExecution;

    @Column(name = "TotalCostExecutionHT", precision = 26, scale = 6)
    private BigDecimal totalCostExecutionHt;

    @Column(name = "TotalCost", precision = 26, scale = 6)
    private BigDecimal totalCost;

    @Column(name = "TotalCostHT", precision = 26, scale = 6)
    private BigDecimal totalCostHt;

    @Column(name = "UnitBackCost", precision = 26, scale = 6)
    private BigDecimal unitBackCost;

    @Column(name = "UnitSalePrice", precision = 26, scale = 6)
    private BigDecimal unitSalePrice;

    @Column(name = "percentageConcretisation", precision = 26, scale = 6)
    private BigDecimal percentageConcretisation;

    @Column(name = "ConretisationAmount", precision = 26, scale = 6)
    private BigDecimal conretisationAmount;

    @Column(name = "FinalizationAmount", precision = 26, scale = 6)
    private BigDecimal finalizationAmount;

    @Column(name = "TotalCostFunding", precision = 26, scale = 6)
    private BigDecimal totalCostFunding;

    @Column(name = "AutofinancementRate", precision = 26, scale = 6)
    private BigDecimal autofinancementRate;

    @Column(name = "AutofinancementAmount", precision = 26, scale = 6)
    private BigDecimal autofinancementAmount;

    @Column(name = "ClientfinancementRate", precision = 26, scale = 6)
    private BigDecimal clientfinancementRate;

    @Column(name = "CreditfinancementRate", precision = 26, scale = 6)
    private BigDecimal creditfinancementRate;

    @Column(name = "GrossProfitMargin", precision = 26, scale = 6)
    private BigDecimal grossProfitMargin;

    @Column(name = "ISRatio", precision = 26, scale = 6)
    private BigDecimal impotRatio;

    @Column(name = "ISAmount", precision = 26, scale = 6)
    private BigDecimal impotAmount;

    @Column(name = "NetProfitMargin", precision = 26, scale = 6)
    private BigDecimal netProfitMargin;

    @Column(name = "ProfitRatioByProjectCost", precision = 26, scale = 6)
    private BigDecimal profitRatioByProjectCost;

    @Column(name = "ProfitRatioByTurnover", precision = 26, scale = 6)
    private BigDecimal profitRatioByTurnover;

    @Column(name = "ProfitRatioByAutofinancementAmount", precision = 26, scale = 6)
    private BigDecimal profitRatioByAutofinancementAmount;
}

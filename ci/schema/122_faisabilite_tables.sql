-- Lot P8a (FEATURE-patrimoine-faisabilite.md §1, §8 ter.13) : etudes de faisabilite, types de yesRemsProd.
-- Idempotent ; applicable aussi a yesRemsTest (aucune de ces tables n'y existe). AdmWorkFlowStateType est deja en CI.
IF OBJECT_ID('dbo.AdmWorkFlowStateType', 'U') IS NULL
CREATE TABLE dbo.AdmWorkFlowStateType (
    PKey uniqueidentifier NOT NULL,
    Code nvarchar(255) COLLATE French_CI_AS NULL,
    Description nvarchar(255) COLLATE French_CI_AS NULL,
    Sorting int NULL,
    UserCreate nvarchar(255) COLLATE French_CI_AS NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) COLLATE French_CI_AS NULL,
    UserUpdateDateTime datetime NULL,
    CONSTRAINT AdmWorkFlowStateType_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

IF OBJECT_ID('dbo.AdmParamsString', 'U') IS NULL
CREATE TABLE dbo.AdmParamsString (
    Code nvarchar(255) COLLATE French_CI_AS NOT NULL,
    CodeValue nvarchar(1000) COLLATE French_CI_AS NULL,
    PKey uniqueidentifier NULL,
    CONSTRAINT AdmParamsString_PK PRIMARY KEY CLUSTERED (Code ASC)
);
GO

IF OBJECT_ID('dbo.SpecificFunction', 'U') IS NULL
CREATE TABLE dbo.SpecificFunction (
    PKey uniqueidentifier NOT NULL,
    Sorting int NULL,
    Code nvarchar(255) COLLATE French_CI_AS NULL,
    Description nvarchar(255) COLLATE French_CI_AS NULL,
    IsTotal bit NULL,
    SaleAbleArea bit NULL,
    CoveredArea bit NULL,
    AreaNet bit NULL,
    AreaBrut bit NULL,
    AreaCos bit NULL,
    UserCreate nvarchar(255) COLLATE French_CI_AS NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) COLLATE French_CI_AS NULL,
    UserUpdateDateTime datetime NULL,
    CONSTRAINT SpecificFunction_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

IF OBJECT_ID('dbo.StkFSBaseUnit', 'U') IS NULL
CREATE TABLE dbo.StkFSBaseUnit (
    PKey uniqueidentifier NOT NULL,
    Sorting int NULL,
    Code varchar(255) COLLATE French_CI_AS NULL,
    Description varchar(255) COLLATE French_CI_AS NULL,
    StkItemCategoryId uniqueidentifier NULL,
    IsArea bit NULL,
    SpecificFunctionId uniqueidentifier NULL,
    CONSTRAINT StkFSBaseUnit_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

IF OBJECT_ID('dbo.StkFeasibilityStudyStatus', 'U') IS NULL
CREATE TABLE dbo.StkFeasibilityStudyStatus (
    PKey uniqueidentifier NOT NULL,
    Code nvarchar(255) COLLATE French_CI_AS NULL,
    Description nvarchar(255) COLLATE French_CI_AS NULL,
    Sorting int NULL,
    Delay int NULL,
    MandatoryFields nvarchar(1000) COLLATE French_CI_AS NULL,
    Notes nvarchar(1000) COLLATE French_CI_AS NULL,
    ColorName nvarchar(255) COLLATE French_CI_AS NULL,
    CfgImageId uniqueidentifier NULL,
    AdmWorkFlowStateTypeId uniqueidentifier NULL,
    UserCreate nvarchar(255) COLLATE French_CI_AS NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) COLLATE French_CI_AS NULL,
    UserUpdateDateTime datetime NULL,
    CONSTRAINT StkFeasibilityStudyStatus_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

IF OBJECT_ID('dbo.PrvType', 'U') IS NULL
CREATE TABLE dbo.PrvType (
    PKey uniqueidentifier NOT NULL,
    Code nvarchar(255) COLLATE French_CI_AS NULL,
    Description nvarchar(255) COLLATE French_CI_AS NULL,
    Notes nvarchar(1000) COLLATE French_CI_AS NULL,
    UserCreate nvarchar(255) COLLATE French_CI_AS NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) COLLATE French_CI_AS NULL,
    UserUpdateDateTime datetime NULL,
    CONSTRAINT PrvType_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

IF OBJECT_ID('dbo.PrvVersion', 'U') IS NULL
CREATE TABLE dbo.PrvVersion (
    PKey uniqueidentifier NOT NULL,
    Code nvarchar(255) COLLATE French_CI_AS NULL,
    Description nvarchar(255) COLLATE French_CI_AS NULL,
    DocDate datetime NULL,
    Notes nvarchar(1000) COLLATE French_CI_AS NULL,
    UserCreate nvarchar(255) COLLATE French_CI_AS NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) COLLATE French_CI_AS NULL,
    UserUpdateDateTime datetime NULL,
    CONSTRAINT PrvVersion_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

IF OBJECT_ID('dbo.StkFeasibilityStudy', 'U') IS NULL
CREATE TABLE dbo.StkFeasibilityStudy (
    PKey uniqueidentifier NOT NULL,
    Code nvarchar(255) COLLATE French_CI_AS NULL,
    Description nvarchar(255) COLLATE French_CI_AS NULL,
    Area decimal(26, 6) NULL,
    OccupationSolRatio decimal(26, 6) NULL,
    OccupationGroundArea decimal(26, 6) NULL,
    Cuf decimal(26, 6) NULL,
    TotalCoveredArea decimal(26, 6) NULL,
    CoefSaleableArea decimal(26, 6) NULL,
    SalableArea decimal(26, 6) NULL,
    NoBuiltArea decimal(26, 6) NULL,
    PurchaseUnitCost decimal(26, 6) NULL,
    PriceArea decimal(26, 6) NULL,
    RegistryCostsRatio decimal(26, 6) NULL,
    RegistryCosts decimal(26, 6) NULL,
    OtherCostsLandRatio decimal(26, 6) NULL,
    OtherCostsLand decimal(26, 6) NULL,
    TotalCostLand decimal(26, 6) NULL,
    UnitCostConstructionBuilt decimal(26, 6) NULL,
    CostAreaBuilt decimal(26, 6) NULL,
    UnitCostConstructionNoBuilt decimal(26, 6) NULL,
    CostAreaNoBuilt decimal(26, 6) NULL,
    OtherCostsExecution decimal(26, 6) NULL,
    otherCostsExecutionRatio decimal(26, 6) NULL,
    TotalCostExecution decimal(26, 6) NULL,
    UnitBackCost decimal(26, 6) NULL,
    UnitSalePrice decimal(26, 6) NULL,
    Turnover decimal(26, 6) NULL,
    percentageConcretisation decimal(26, 6) NULL,
    FinalizationAmount decimal(26, 6) NULL,
    ConretisationAmount decimal(26, 6) NULL,
    StartDateExpenses datetime NULL,
    EndDateExpenses datetime NULL,
    StartDateConcretisation datetime NULL,
    EndDateConcretisation datetime NULL,
    StartDateFinalization datetime NULL,
    EndDateFinalization datetime NULL,
    StartDatePeriodeOfValidation datetime NULL,
    EndDatePeriodeOfValidation datetime NULL,
    StartDatePermissionBatir datetime NULL,
    EndDatePermissionBatir datetime NULL,
    StartDateStudy datetime NULL,
    EndDateStudy datetime NULL,
    EcartTypeExpenses decimal(26, 6) NULL,
    EcartTypeConcretisation decimal(26, 6) NULL,
    EcartTypeFinalization decimal(26, 6) NULL,
    UserCreate nvarchar(255) COLLATE French_CI_AS NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) COLLATE French_CI_AS NULL,
    UserUpdateDateTime datetime NULL,
    TotalCostFunding decimal(26, 6) NULL,
    BackGuaranteeRate decimal(26, 6) NULL,
    BackGuaranteeApplicationRate decimal(26, 6) NULL,
    BackTimeOnDays int NULL,
    LndLandId uniqueidentifier NULL,
    StkItemId uniqueidentifier NULL,
    NumberOfItemForSale int NULL,
    AutofinancementRate decimal(26, 6) NULL,
    ClientfinancementRate decimal(26, 6) NULL,
    CreditfinancementRate decimal(26, 6) NULL,
    CfgCompanyId uniqueidentifier NULL,
    ISRatio decimal(26, 6) NULL,
    ISAmount decimal(26, 6) NULL,
    NetProfitMargin decimal(26, 6) NULL,
    GrossProfitMargin decimal(26, 6) NULL,
    TotalCost decimal(26, 6) NULL,
    AutofinancementAmount decimal(26, 6) NULL,
    ProfitRatioByProjectCost decimal(26, 6) NULL,
    ProfitRatioByTurnover decimal(26, 6) NULL,
    ProfitRatioByAutofinancementAmount decimal(26, 6) NULL,
    TotalCostExecutionHT decimal(26, 6) NULL,
    TotalCostHT decimal(26, 6) NULL,
    DefaultVat decimal(26, 6) NULL,
    EquipementsCoef decimal(26, 6) NULL,
    EquipementsAmount decimal(26, 6) NULL,
    InterestVatRatio decimal(26, 6) NULL,
    LandDescription varchar(255) COLLATE French_CI_AS NULL,
    AreaNet decimal(26, 6) NULL,
    StartDatePurchaseLand datetime NULL,
    EndDatePurchaseLand datetime NULL,
    DelayPurchaseLand int NULL,
    DelayPeriodeOfValidation int NULL,
    DelayPermissionBatir int NULL,
    DelayStudy int NULL,
    DelayExpenses int NULL,
    DelayConcretisation int NULL,
    DelayFinalization int NULL,
    DelayEndDatePurchaseLandStartDatePeriodeOfValidation int NULL,
    DelayEndDatePeriodeOfValidationStartDatePermissionBatir int NULL,
    DelayStartDatePermissionBatirStartDateStudy int NULL,
    DelayEndDatePermissionBatirStartDateExpenses int NULL,
    DelayStartDateExpenseStartDateConcretisations int NULL,
    DelayStartDateExpenseStartDateFinalization int NULL,
    LandVatRatio decimal(26, 6) NULL,
    PriceAreaHT decimal(26, 6) NULL,
    PurchaseUnitCostHT decimal(26, 6) NULL,
    PurchaseUnitCostVatAmount decimal(26, 6) NULL,
    PriceAreaVatAmount decimal(26, 6) NULL,
    RegistryCostsHT decimal(26, 6) NULL,
    RegistryCostsVatAmount decimal(26, 6) NULL,
    OtherCostsExecutionHT decimal(26, 6) NULL,
    OtherCostsExecutionVatAmount decimal(26, 6) NULL,
    DocDate datetime NULL,
    StkFeasibilityStudyStatusId uniqueidentifier NULL,
    StrEntityId uniqueidentifier NULL,
    UseLandProportionForTVACalculation bit NULL,
    IsLocked bit NULL,
    PrvTypeId uniqueidentifier NULL,
    PrvVersionId uniqueidentifier NULL,
    CONSTRAINT StkFeasibilityStudy_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

IF OBJECT_ID('dbo.StkFSBaseUnitStkFeasibilityStudy', 'U') IS NULL
CREATE TABLE dbo.StkFSBaseUnitStkFeasibilityStudy (
    PKey uniqueidentifier NOT NULL,
    Sorting int NULL,
    FeasibilityStudyId uniqueidentifier NULL,
    StkFSBaseUnitId uniqueidentifier NULL,
    Quantity decimal(26, 6) NULL,
    CONSTRAINT StkFSBaseUnitStkFeasibilityStudy_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

IF OBJECT_ID('dbo.StkFeasibilityStudyCfgTranche', 'U') IS NULL
CREATE TABLE dbo.StkFeasibilityStudyCfgTranche (
    PKey uniqueidentifier NOT NULL,
    Sorting int NULL,
    Code nvarchar(255) COLLATE French_CI_AS NULL,
    FeasibilityStudyId uniqueidentifier NULL,
    CfgTrancheId uniqueidentifier NULL,
    UserCreate nvarchar(255) COLLATE French_CI_AS NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) COLLATE French_CI_AS NULL,
    UserUpdateDateTime datetime NULL,
    WorkStartDate datetime NULL,
    WorkEndDate datetime NULL,
    MarketingStartDate datetime NULL,
    EndDateConcretisation datetime NULL,
    StartDateFinalisation datetime NULL,
    MarketingEndDate datetime NULL,
    DeliveryDate datetime NULL,
    OccupancyPermitsDate datetime NULL,
    Weight decimal(26, 6) NULL,
    CONSTRAINT StkFeasibilityStudyCfgTranche_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

IF OBJECT_ID('dbo.StkFeasibilityStudyCfgTrancheStkBaseUnit', 'U') IS NULL
CREATE TABLE dbo.StkFeasibilityStudyCfgTrancheStkBaseUnit (
    PKey uniqueidentifier NOT NULL,
    Quantity decimal(26, 6) NULL,
    FeasibilityStudyId uniqueidentifier NULL,
    StkBaseUnitId uniqueidentifier NULL,
    StkFeasibilityStudyCfgTrancheId uniqueidentifier NULL,
    UserCreate nvarchar(255) COLLATE French_CI_AS NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) COLLATE French_CI_AS NULL,
    UserUpdateDateTime datetime NULL,
    Weight decimal(26, 6) NULL,
    CONSTRAINT StkFeasibilityStudyCfgTrancheStkBaseUnit_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

IF OBJECT_ID('dbo.StkFeasibilityStudyStatusHistory', 'U') IS NULL
CREATE TABLE dbo.StkFeasibilityStudyStatusHistory (
    PKey uniqueidentifier NOT NULL,
    DocDate datetime NULL,
    StkFeasibilityStudyStatusId uniqueidentifier NULL,
    StkFeasibilityStudyId uniqueidentifier NULL,
    AdmUserId uniqueidentifier NULL,
    CONSTRAINT StkFeasibilityStudyStatusHistory_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

IF OBJECT_ID('dbo.StkFeasibilityStudyStatusDocumentToAttach', 'U') IS NULL
CREATE TABLE dbo.StkFeasibilityStudyStatusDocumentToAttach (
    PKey uniqueidentifier NOT NULL,
    StkFeasibilityStudyStatusId uniqueidentifier NULL,
    IsMandatory bit NULL,
    WithTitle bit NULL,
    UserCreate nvarchar(255) COLLATE French_CI_AS NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) COLLATE French_CI_AS NULL,
    UserUpdateDateTime datetime NULL,
    AdmAttachedFileTypeId uniqueidentifier NULL,
    CONSTRAINT StkFeasibilityStudyStatusDocumentToAttach_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

IF OBJECT_ID('dbo.StkFeasibilityStudyWorkFlow', 'U') IS NULL
CREATE TABLE dbo.StkFeasibilityStudyWorkFlow (
    PKey uniqueidentifier NOT NULL,
    Sorting int NULL,
    StartStatusId uniqueidentifier NULL,
    EndStatusId uniqueidentifier NULL,
    UserCreate nvarchar(255) COLLATE French_CI_AS NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) COLLATE French_CI_AS NULL,
    UserUpdateDateTime datetime NULL,
    MandatoryFields nvarchar(255) COLLATE French_CI_AS NULL,
    NotesMandatory bit NULL,
    StrEntityId uniqueidentifier NULL,
    WithNotification bit NULL,
    Description nvarchar(255) COLLATE French_CI_AS NULL,
    SendEmailToUsers bit NULL,
    CONSTRAINT StkFeasibilityStudyWorkFlow_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

IF OBJECT_ID('dbo.StkFeasibilityStudyWorkFlowAdmRole', 'U') IS NULL
CREATE TABLE dbo.StkFeasibilityStudyWorkFlowAdmRole (
    PKey uniqueidentifier NOT NULL,
    WorkFlowId uniqueidentifier NULL,
    AdmRoleId uniqueidentifier NULL,
    UserCreate nvarchar(255) COLLATE French_CI_AS NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) COLLATE French_CI_AS NULL,
    UserUpdateDateTime datetime NULL,
    CONSTRAINT StkFeasibilityStudyWorkFlowAdmRole_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

IF OBJECT_ID('dbo.StkFeasibilityStudyWorkFlowDocumentToAttach', 'U') IS NULL
CREATE TABLE dbo.StkFeasibilityStudyWorkFlowDocumentToAttach (
    PKey uniqueidentifier NOT NULL,
    StkFeasibilityStudyWorkFlowId uniqueidentifier NULL,
    IsMandatory bit NULL,
    WithTitle bit NULL,
    UserCreate nvarchar(255) COLLATE French_CI_AS NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) COLLATE French_CI_AS NULL,
    UserUpdateDateTime datetime NULL,
    AdmAttachedFileTypeId uniqueidentifier NULL,
    CONSTRAINT StkFeasibilityStudyWorkFlowDocumentToAttach_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

IF OBJECT_ID('dbo.PrjMarketPrevisionModel', 'U') IS NULL
CREATE TABLE dbo.PrjMarketPrevisionModel (
    PKey uniqueidentifier NOT NULL,
    Code nvarchar(255) COLLATE French_CI_AS NULL,
    Description nvarchar(255) COLLATE French_CI_AS NULL,
    StlCategoryId uniqueidentifier NULL,
    CONSTRAINT PrjMarketPrevisionModel_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

IF OBJECT_ID('dbo.FSFunding', 'U') IS NULL
CREATE TABLE dbo.FSFunding (
    PKey uniqueidentifier NOT NULL,
    paymentDate datetime NULL,
    amount decimal(26, 6) NULL,
    monthsCount int NULL,
    monthsStep int NULL,
    gracePeriod int NULL,
    FeasibilityStudyId uniqueidentifier NULL,
    UserCreate nvarchar(255) COLLATE French_CI_AS NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) COLLATE French_CI_AS NULL,
    UserUpdateDateTime datetime NULL,
    chargeRatio decimal(26, 6) NULL,
    Description nvarchar(255) COLLATE French_CI_AS NULL,
    CONSTRAINT FSFunding_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

IF OBJECT_ID('dbo.FSReimbursement', 'U') IS NULL
CREATE TABLE dbo.FSReimbursement (
    PKey uniqueidentifier NOT NULL,
    paymentDate datetime NULL,
    amountBase decimal(26, 6) NULL,
    amountInterest decimal(26, 6) NULL,
    amountToPay decimal(26, 6) NULL,
    interestRate decimal(26, 6) NULL,
    FSFundingId uniqueidentifier NULL,
    UserCreate nvarchar(255) COLLATE French_CI_AS NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) COLLATE French_CI_AS NULL,
    UserUpdateDateTime datetime NULL,
    CONSTRAINT FSReimbursement_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

IF OBJECT_ID('dbo.FSPrevision', 'U') IS NULL
CREATE TABLE dbo.FSPrevision (
    PKey uniqueidentifier NOT NULL,
    Amount decimal(26, 6) NULL,
    Ratio decimal(26, 6) NULL,
    Note nvarchar(255) COLLATE French_CI_AS NULL,
    FeasibilityStudyId uniqueidentifier NULL,
    StlCategoryId uniqueidentifier NULL,
    UserCreate nvarchar(255) COLLATE French_CI_AS NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) COLLATE French_CI_AS NULL,
    UserUpdateDateTime datetime NULL,
    GlobalRatio decimal(26, 6) NULL,
    UnityId uniqueidentifier NULL,
    ParentId uniqueidentifier NULL,
    AmountHT decimal(26, 6) NULL,
    VatRatio decimal(26, 6) NULL,
    Undivided bit NULL,
    ApplyOnDate datetime NULL,
    IsForProject bit NULL,
    PrjMarketPrevisionModelId uniqueidentifier NULL,
    CONSTRAINT FSPrevision_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

IF OBJECT_ID('dbo.StkFSPrevisionLine', 'U') IS NULL
CREATE TABLE dbo.StkFSPrevisionLine (
    PKey uniqueidentifier NOT NULL,
    DocDate datetime NULL,
    DateMonth int NULL,
    DateYear int NULL,
    Amount decimal(26, 6) NULL,
    StkFeasibilityStudyId uniqueidentifier NULL,
    StkFeasibilityStudyCfgTrancheId uniqueidentifier NULL,
    PrevisionType nvarchar(255) COLLATE French_CI_AS NULL,
    Notes nvarchar(1000) COLLATE French_CI_AS NULL,
    IsCredit bit NULL,
    AmountHT decimal(26, 6) NULL,
    VatRatio decimal(26, 6) NULL,
    CONSTRAINT StkFSPrevisionLine_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

IF OBJECT_ID('dbo.StkFSPrevisionStkTranche', 'U') IS NULL
CREATE TABLE dbo.StkFSPrevisionStkTranche (
    PKey uniqueidentifier NOT NULL,
    Amount decimal(26, 6) NULL,
    Ratio decimal(26, 6) NULL,
    FeasibilityStudyId uniqueidentifier NULL,
    StlCategoryId uniqueidentifier NULL,
    StkTrancheId uniqueidentifier NULL,
    UserCreate nvarchar(255) COLLATE French_CI_AS NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) COLLATE French_CI_AS NULL,
    UserUpdateDateTime datetime NULL,
    AmountHT decimal(26, 6) NULL,
    VatRatio decimal(26, 6) NULL,
    Undivided bit NULL,
    CONSTRAINT StkFSPrevisionStkTranche_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

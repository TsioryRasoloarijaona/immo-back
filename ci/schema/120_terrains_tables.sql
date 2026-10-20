-- Lot P7a (FEATURE-patrimoine-terrains.md §6.1) : tables de la facture de frais de terrain, types de yesRemsProd.
-- Idempotent ; applicable aussi a yesRemsTest (BuyDocumentLine y est absente, deja en CI par 010).
IF OBJECT_ID('dbo.BuyDocumentLine', 'U') IS NULL
CREATE TABLE dbo.BuyDocumentLine (
    Pkey uniqueidentifier NOT NULL,
    Code nvarchar(255) COLLATE French_CI_AS NULL,
    Description nvarchar(255) COLLATE French_CI_AS NULL,
    Unity nvarchar(255) COLLATE French_CI_AS NULL,
    Quantity decimal(26, 6) NULL,
    DiscountRatio decimal(26, 16) NULL,
    VatRatio decimal(26, 6) NULL,
    Notes nvarchar(1000) COLLATE French_CI_AS NULL,
    BuyDocumentId uniqueidentifier NULL,
    Sorting int NULL,
    UnitPriceHT decimal(26, 6) NULL,
    UnitPriceTTC decimal(26, 6) NULL,
    PrjMarketLineId uniqueidentifier NULL,
    BuyItemId uniqueidentifier NULL,
    QuantityOld decimal(26, 6) NULL,
    QuantityOrigin decimal(26, 6) NULL,
    Coefficient decimal(26, 6) NULL,
    StlCategoryId uniqueidentifier NULL,
    FODECRatio decimal(26, 16) NULL,
    UserCreate nvarchar(255) COLLATE French_CI_AS NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) COLLATE French_CI_AS NULL,
    UserUpdateDateTime datetime NULL,
    BuyConsultationLineId uniqueidentifier NULL,
    CONSTRAINT BuyDocumentLine_PK PRIMARY KEY CLUSTERED (Pkey ASC)
);
GO

IF OBJECT_ID('dbo.BuyVentilationCategory', 'U') IS NULL
CREATE TABLE dbo.BuyVentilationCategory (
    PKey uniqueidentifier NOT NULL,
    StlCategoryId uniqueidentifier NULL,
    BuyDocumentId uniqueidentifier NULL,
    VentilationRate decimal(26, 6) NULL,
    CONSTRAINT BuyVentilationCategory_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

IF OBJECT_ID('dbo.BuyDocumentStatusHistory', 'U') IS NULL
CREATE TABLE dbo.BuyDocumentStatusHistory (
    PKey uniqueidentifier NOT NULL,
    DocDate datetime NULL,
    BuyDocumentStatusId uniqueidentifier NULL,
    BuyDocumentId uniqueidentifier NULL,
    AdmUserId uniqueidentifier NULL,
    Notes nvarchar(1000) COLLATE French_CI_AS NULL,
    buyDocumentWorkFlowId uniqueidentifier NULL,
    CONSTRAINT BuyDocumentStatusHistory_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

IF OBJECT_ID('dbo.BuyDocumentWorkFlow', 'U') IS NULL
CREATE TABLE dbo.BuyDocumentWorkFlow (
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
    SupplierMandatoryFields nvarchar(1000) COLLATE French_CI_AS NULL,
    StrEntityId uniqueidentifier NULL,
    IsValidatedByCfgTierTrigger bit NULL,
    WithNotification bit NULL,
    Description nvarchar(255) COLLATE French_CI_AS NULL,
    SendEmailToUsers bit NULL,
    NotifyCfgTierTrigger bit NULL,
    SendEMailToCfgTierTrigger bit NULL,
    StrWorkFlowId uniqueidentifier NULL,
    CONSTRAINT BuyDocumentWorkFlow_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

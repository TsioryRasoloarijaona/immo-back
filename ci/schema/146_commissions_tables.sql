-- Lot COM (FEATURE-vente-commissions.md) : lignes de commission et ventilation par projet, types de yesRemsProd.
-- Idempotent ; applicable aussi a yesRemsTest.
IF OBJECT_ID('dbo.ComCommissionLine', 'U') IS NULL
CREATE TABLE dbo.ComCommissionLine (
    PKey uniqueidentifier NOT NULL,
    DocDate datetime NULL,
    CfgEmployeeId uniqueidentifier NULL,
    ComFolderId uniqueidentifier NULL,
    ComCommissionId uniqueidentifier NULL,
    CommissionType nvarchar(255) COLLATE French_CI_AS NULL,
    CommissionAmount decimal(26, 16) NULL,
    StkItemCategoryId uniqueidentifier NULL,
    StkItemId uniqueidentifier NULL,
    CfgTrancheId uniqueidentifier NULL,
    UserCreate nvarchar(255) COLLATE French_CI_AS NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) COLLATE French_CI_AS NULL,
    UserUpdateDateTime datetime NULL,
    BuyDocumentId uniqueidentifier NULL,
    CalculationMethod nvarchar(255) COLLATE French_CI_AS NULL,
    StrFolderStatusId uniqueidentifier NULL,
    CONSTRAINT ComCommissionLine_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

IF OBJECT_ID('dbo.BuyVentilationExpense', 'U') IS NULL
CREATE TABLE dbo.BuyVentilationExpense (
    Pkey uniqueidentifier NOT NULL,
    PrjProjectId uniqueidentifier NULL,
    BuyDocumentId uniqueidentifier NULL,
    VentilationRate decimal(26, 6) NULL,
    CONSTRAINT BuyVentilationExpense_PK PRIMARY KEY CLUSTERED (Pkey ASC)
);
GO

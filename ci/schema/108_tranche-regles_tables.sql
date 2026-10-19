-- Lot P1c (FEATURE-patrimoine-tranche-regles.md §9.1, §9.4) : commissions, finalisations et previsions de base,
-- types de yesRemsProd. Idempotent ; applicable aussi a yesRemsTest (tables neuves seulement).
IF OBJECT_ID('dbo.ComSalesCommission', 'U') IS NULL
CREATE TABLE dbo.ComSalesCommission (
    PKey uniqueidentifier NOT NULL,
    StartDate datetime NULL,
    EndDate datetime NULL,
    SaleCommissionAmount decimal(26, 16) NULL,
    DeliveryCommissionAmount decimal(26, 16) NULL,
    SaleCommissionAmountExtern decimal(26, 16) NULL,
    DeliveryCommissionAmountExtern decimal(26, 16) NULL,
    StkItemCategoryId uniqueidentifier NULL,
    CfgTierId uniqueidentifier NULL,
    CfgTrancheId uniqueidentifier NULL,
    UserCreate nvarchar(255) COLLATE French_CI_AS NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) COLLATE French_CI_AS NULL,
    UserUpdateDateTime datetime NULL,
    ComActionId uniqueidentifier NULL,
    DeliveryCommissionAmount2 decimal(26, 6) NULL,
    DeliveryCommissionAmountExtern2 decimal(26, 6) NULL,
    SaleCommissionPercent decimal(26, 6) NULL,
    SaleCommissionBasedOnAmount bit NULL,
    SaleCommissionPercentExtern decimal(26, 6) NULL,
    SaleCommissionExternBasedOnAmount bit NULL,
    DeliveryCommissionPercent decimal(26, 6) NULL,
    DeliveryCommissionBasedOnAmount bit NULL,
    DeliveryCommissionPercentExtern decimal(26, 6) NULL,
    DeliveryCommissionExternBasedOnAmount bit NULL,
    DeliveryCommissionPercent2 decimal(26, 6) NULL,
    DeliveryCommission2BasedOnAmount bit NULL,
    DeliveryCommissionPercentExtern2 decimal(26, 6) NULL,
    DeliveryCommissionExtern2BasedOnAmount bit NULL,
    ComEmployeeId uniqueidentifier NULL,
    CONSTRAINT ComSalesCommission_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

IF OBJECT_ID('dbo.CfgTrancheFolderEntity', 'U') IS NULL
CREATE TABLE dbo.CfgTrancheFolderEntity (
    PKey uniqueidentifier NOT NULL,
    CfgTrancheId uniqueidentifier NULL,
    StrFolderEntityId uniqueidentifier NULL,
    StrStatusDeliveryId1 uniqueidentifier NULL,
    StrStatusDeliveryId2 uniqueidentifier NULL,
    UserCreate nvarchar(255) COLLATE French_CI_AS NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) COLLATE French_CI_AS NULL,
    UserUpdateDateTime datetime NULL,
    CONSTRAINT CfgTrancheFolderEntity_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

IF OBJECT_ID('dbo.CfgPrevision', 'U') IS NULL
CREATE TABLE dbo.CfgPrevision (
    PKey uniqueidentifier NOT NULL,
    [Month] int NULL,
    [Year] int NULL,
    Quantity int NULL,
    Amount decimal(26, 6) NULL,
    Notes nvarchar(1000) COLLATE French_CI_AS NULL,
    StkHierarchyId uniqueidentifier NULL,
    AchievementAmount decimal(26, 6) NULL,
    TurnoverQuantity int NULL,
    TurnoverAmount decimal(26, 6) NULL,
    Turnover decimal(26, 6) NULL,
    DocDate datetime NULL,
    CONSTRAINT CfgPrevision_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

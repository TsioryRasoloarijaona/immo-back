-- Lot projet-lots-decomptes (FEATURE-projet-lots-decomptes.md) : 6 tables de lignes du module Projet, definition de yesRemsProd, avec leurs FK.
IF OBJECT_ID('dbo.PrjMarketLineGrouping', 'U') IS NULL
CREATE TABLE dbo.PrjMarketLineGrouping (
    PKey uniqueidentifier NOT NULL CONSTRAINT PrjMarketLineGrouping_PK PRIMARY KEY,
    Code nvarchar(255) NULL,
    Description nvarchar(255) NULL,
    Sorting int NULL,
    ParentId uniqueidentifier NULL,
    PrjMarketId uniqueidentifier NULL,
    UserCreate nvarchar(255) NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) NULL,
    UserUpdateDateTime datetime NULL,
    IsStock bit NULL,
    Notes nvarchar(1000) NULL
);
GO

IF OBJECT_ID('dbo.PrjMarketLine', 'U') IS NULL
CREATE TABLE dbo.PrjMarketLine (
    PKey uniqueidentifier NOT NULL CONSTRAINT PrjMarketLine_PK PRIMARY KEY,
    Code nvarchar(255) NULL,
    Description nvarchar(255) NULL,
    EstimatedUnitPriceHT decimal(26, 6) NULL,
    VatRatio decimal(26, 6) NULL,
    EstimatedUnitPriceTTC decimal(26, 6) NULL,
    Notes nvarchar(1000) NULL,
    PrjMarketId uniqueidentifier NULL,
    Sorting int NULL,
    Unity nvarchar(50) NULL,
    Quantity decimal(26, 6) NULL,
    UnitPriceHT decimal(26, 16) NULL,
    UnitPriceTTC decimal(26, 6) NULL,
    BuyItemId uniqueidentifier NULL,
    WeightingRatio decimal(26, 6) NULL,
    PrjMarketLineGroupingId uniqueidentifier NULL,
    IsStock bit NULL,
    Groupping1Id uniqueidentifier NULL,
    Groupping2Id uniqueidentifier NULL,
    Groupping3Id uniqueidentifier NULL,
    StlCategoryId uniqueidentifier NULL,
    FODECRatio decimal(26, 16) NULL,
    UserCreate nvarchar(255) NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) NULL,
    UserUpdateDateTime datetime NULL,
    BuyConsultationLineId uniqueidentifier NULL
);
GO

IF OBJECT_ID('dbo.PrjMarketLineEstimation', 'U') IS NULL
CREATE TABLE dbo.PrjMarketLineEstimation (
    PKey uniqueidentifier NOT NULL CONSTRAINT PrjMarketLineEstimation_PK PRIMARY KEY,
    Code nvarchar(255) NULL,
    Description nvarchar(255) NULL,
    UnitPriceHT decimal(26, 6) NULL,
    VatRatio decimal(26, 6) NULL,
    UnitPriceTTC decimal(26, 6) NULL,
    Notes nvarchar(1000) NULL,
    PrjMarketId uniqueidentifier NULL,
    Sorting int NULL,
    Unity nvarchar(50) NULL,
    Quantity decimal(26, 6) NULL,
    BuyItemId uniqueidentifier NULL,
    WeightingRatio decimal(26, 6) NULL,
    PrjMarketLineGroupingId uniqueidentifier NULL,
    IsStock bit NULL,
    Groupping1Id uniqueidentifier NULL,
    Groupping2Id uniqueidentifier NULL,
    Groupping3Id uniqueidentifier NULL,
    StlCategoryId uniqueidentifier NULL,
    FODECRatio decimal(26, 16) NULL,
    UserCreate nvarchar(255) NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) NULL,
    UserUpdateDateTime datetime NULL,
    BuyConsultationLineId uniqueidentifier NULL
);
GO

IF OBJECT_ID('dbo.PrjWorkingOutLine', 'U') IS NULL
CREATE TABLE dbo.PrjWorkingOutLine (
    Pkey uniqueidentifier NOT NULL CONSTRAINT PrjWorkingOutLine_PK PRIMARY KEY,
    Sorting int NULL,
    Code nvarchar(255) NULL,
    Description nvarchar(255) NULL,
    Unity nvarchar(255) NULL,
    QuantityOrigin decimal(26, 6) NULL,
    Coefficient decimal(26, 6) NULL,
    Quantity decimal(26, 6) NULL,
    QuantityOld decimal(26, 6) NULL,
    UnitPriceHT decimal(26, 6) NULL,
    VatRatio decimal(26, 6) NULL,
    UnitPriceTTC decimal(26, 6) NULL,
    Notes nvarchar(1000) NULL,
    PrjWorkingOutId uniqueidentifier NULL,
    PrjMarketLineId uniqueidentifier NULL,
    BuyItemId uniqueidentifier NULL,
    StlCategoryId uniqueidentifier NULL,
    FODECRatio decimal(26, 16) NULL,
    UserCreate nvarchar(255) NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) NULL,
    UserUpdateDateTime datetime NULL,
    BuyConsultationLineId uniqueidentifier NULL
);
GO

IF OBJECT_ID('dbo.PrjWorkingOutLineExtra', 'U') IS NULL
CREATE TABLE dbo.PrjWorkingOutLineExtra (
    Pkey uniqueidentifier NOT NULL CONSTRAINT PrjWorkingOutLineExtra_PK PRIMARY KEY,
    Sorting int NULL,
    Code nvarchar(255) NULL,
    Description nvarchar(500) NULL,
    Unity nvarchar(255) NULL,
    Quantity decimal(26, 6) NULL,
    UnitPriceHT decimal(26, 6) NULL,
    VatRatio decimal(26, 6) NULL,
    UnitPriceTTC decimal(26, 6) NULL,
    Notes nvarchar(1000) NULL,
    PrjWorkingOutId uniqueidentifier NULL,
    UserCreate nvarchar(255) NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) NULL,
    UserUpdateDateTime datetime NULL,
    StlCategoryId uniqueidentifier NULL,
    PrjMarketLineId uniqueidentifier NULL,
    FODECRatio decimal(26, 16) NULL
);
GO

IF OBJECT_ID('dbo.PrjWorkingOutRetainedLine', 'U') IS NULL
CREATE TABLE dbo.PrjWorkingOutRetainedLine (
    PKey uniqueidentifier NOT NULL CONSTRAINT PrjWorkingOutRetainedLine_PK PRIMARY KEY,
    PrjWorkingOutId uniqueidentifier NOT NULL,
    Sorting int NULL,
    Description nvarchar(500) NULL,
    Amount decimal(26, 6) NULL
);
GO

IF OBJECT_ID('dbo.BuyItem_PrjMarketLine_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjMarketLine ADD CONSTRAINT BuyItem_PrjMarketLine_FK1 FOREIGN KEY (BuyItemId) REFERENCES dbo.BuyItem (PKey) ON DELETE SET NULL;
GO
IF OBJECT_ID('dbo.PrjMarket_PrjMarketLine_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjMarketLine ADD CONSTRAINT PrjMarket_PrjMarketLine_FK1 FOREIGN KEY (PrjMarketId) REFERENCES dbo.PrjMarket (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.PrjMarketLine_StlCategory_FK', 'F') IS NULL
ALTER TABLE dbo.PrjMarketLine ADD CONSTRAINT PrjMarketLine_StlCategory_FK FOREIGN KEY (StlCategoryId) REFERENCES dbo.StlCategory (PKey);
GO
IF OBJECT_ID('dbo.PrjMarketLineEstimation_BuyItem_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjMarketLineEstimation ADD CONSTRAINT PrjMarketLineEstimation_BuyItem_FK1 FOREIGN KEY (BuyItemId) REFERENCES dbo.BuyItem (PKey) ON DELETE SET NULL;
GO
IF OBJECT_ID('dbo.PrjMarketLineEstimation_PrjMarket_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjMarketLineEstimation ADD CONSTRAINT PrjMarketLineEstimation_PrjMarket_FK1 FOREIGN KEY (PrjMarketId) REFERENCES dbo.PrjMarket (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.PrjMarketLineEstimation_StlCategory_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjMarketLineEstimation ADD CONSTRAINT PrjMarketLineEstimation_StlCategory_FK1 FOREIGN KEY (StlCategoryId) REFERENCES dbo.StlCategory (PKey);
GO
IF OBJECT_ID('dbo.PrjMarketLineGrouping_PrjMarket_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjMarketLineGrouping ADD CONSTRAINT PrjMarketLineGrouping_PrjMarket_FK1 FOREIGN KEY (PrjMarketId) REFERENCES dbo.PrjMarket (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.BuyItem_PrjWorkingOutLine_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjWorkingOutLine ADD CONSTRAINT BuyItem_PrjWorkingOutLine_FK1 FOREIGN KEY (BuyItemId) REFERENCES dbo.BuyItem (PKey);
GO
IF OBJECT_ID('dbo.PrjMarketLine_PrjWorkingOutLine_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjWorkingOutLine ADD CONSTRAINT PrjMarketLine_PrjWorkingOutLine_FK1 FOREIGN KEY (PrjMarketLineId) REFERENCES dbo.PrjMarketLine (PKey);
GO
IF OBJECT_ID('dbo.PrjWorkingOut_PrjWorkingOutLine_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjWorkingOutLine ADD CONSTRAINT PrjWorkingOut_PrjWorkingOutLine_FK1 FOREIGN KEY (PrjWorkingOutId) REFERENCES dbo.PrjWorkingOut (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.PrjWorkingOutLine_StlCategory_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjWorkingOutLine ADD CONSTRAINT PrjWorkingOutLine_StlCategory_FK1 FOREIGN KEY (StlCategoryId) REFERENCES dbo.StlCategory (PKey);
GO
IF OBJECT_ID('dbo.PrjWorkingOutLineExtra__PrjMarketLine_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjWorkingOutLineExtra ADD CONSTRAINT PrjWorkingOutLineExtra__PrjMarketLine_FK1 FOREIGN KEY (PrjMarketLineId) REFERENCES dbo.PrjMarketLine (PKey);
GO
IF OBJECT_ID('dbo.PrjWorkingOutLineExtra__PrjWorkingOut_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjWorkingOutLineExtra ADD CONSTRAINT PrjWorkingOutLineExtra__PrjWorkingOut_FK1 FOREIGN KEY (PrjWorkingOutId) REFERENCES dbo.PrjWorkingOut (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.PrjWorkingOutLineExtra_StlCategory_FK', 'F') IS NULL
ALTER TABLE dbo.PrjWorkingOutLineExtra ADD CONSTRAINT PrjWorkingOutLineExtra_StlCategory_FK FOREIGN KEY (StlCategoryId) REFERENCES dbo.StlCategory (PKey);
GO
IF OBJECT_ID('dbo.PrjWorkingOutRetainedLine_PrjWorkingOut_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjWorkingOutRetainedLine ADD CONSTRAINT PrjWorkingOutRetainedLine_PrjWorkingOut_FK1 FOREIGN KEY (PrjWorkingOutId) REFERENCES dbo.PrjWorkingOut (PKey);
GO

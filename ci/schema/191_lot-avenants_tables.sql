-- FEATURE-lot-avenants-avances.md §6 : lignes d'avenant, réserves et tiers invités d'une réception.
-- Définitions de yesRemsProd (celles de FixtureLotAvenants.TABLES) ; idempotent, applicable à yesRemsTest.
IF OBJECT_ID('dbo.PrjClauseLine', 'U') IS NULL
CREATE TABLE dbo.PrjClauseLine (Pkey uniqueidentifier NOT NULL PRIMARY KEY, Sorting int NULL,
    Code nvarchar(255) NULL, Description nvarchar(500) NULL, Unity nvarchar(255) NULL, Quantity decimal(26, 6) NULL,
    UnitPriceHT decimal(26, 6) NULL, VatRatio decimal(26, 6) NULL, UnitPriceTTC decimal(26, 6) NULL,
    Notes nvarchar(1000) NULL, PrjClauseId uniqueidentifier NULL, UserCreate nvarchar(255) NULL,
    UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL,
    StlCategoryId uniqueidentifier NULL, PrjMarketLineId uniqueidentifier NULL, FODECRatio decimal(26, 16) NULL,
    BuyConsultationLineId uniqueidentifier NULL);
GO
IF OBJECT_ID('dbo.PrjMarketReceptionReserve', 'U') IS NULL
CREATE TABLE dbo.PrjMarketReceptionReserve (PKey uniqueidentifier NOT NULL PRIMARY KEY, sorting int NULL,
    Description nvarchar(255) NULL, IsFixed bit NULL, PrjMarketReceptionId uniqueidentifier NULL,
    UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL,
    UserUpdateDateTime datetime NULL);
GO
IF OBJECT_ID('dbo.PrjMarketReceptionCfgTier', 'U') IS NULL
CREATE TABLE dbo.PrjMarketReceptionCfgTier (PKey uniqueidentifier NOT NULL PRIMARY KEY, sorting int NULL,
    PrjMarketReceptionId uniqueidentifier NULL, CfgTierId uniqueidentifier NULL);
GO

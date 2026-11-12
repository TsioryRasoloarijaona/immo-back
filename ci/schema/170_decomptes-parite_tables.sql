-- Lot decomptes-parite (FEATURE-decomptes-parite.md) : lignes d'approvisionnement d'un décompte, définition de yesRemsProd.
IF OBJECT_ID('dbo.PrjWorkingOutStockLine', 'U') IS NULL
CREATE TABLE dbo.PrjWorkingOutStockLine (
    Pkey uniqueidentifier NOT NULL CONSTRAINT PrjWorkingOutStockLine_PK PRIMARY KEY,
    Sorting int NULL,
    Code nvarchar(255) NULL,
    Description nvarchar(500) NULL,
    Unity nvarchar(255) NULL,
    Quantity decimal(26, 6) NULL,
    UnitPriceHT decimal(26, 6) NULL,
    VatRatio decimal(26, 6) NULL,
    UnitPriceTTC decimal(26, 6) NULL,
    PrjWorkingOutId uniqueidentifier NULL,
    PrjStockLineId uniqueidentifier NULL,
    UserCreate nvarchar(255) NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) NULL,
    UserUpdateDateTime datetime NULL,
    FODECRatio decimal(26, 16) NULL,
    Notes nvarchar(2500) NULL
);
GO

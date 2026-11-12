-- Regroupements du bordereau estimatif d'un lot (FEATURE-lot-fiche-parite.md E8), colonnes de yesRemsProd.
IF OBJECT_ID('dbo.PrjMarketLineGroupingEstimation', 'U') IS NULL
CREATE TABLE dbo.PrjMarketLineGroupingEstimation (
    PKey uniqueidentifier NOT NULL CONSTRAINT PrjMarketLineGroupingEstimation_PK PRIMARY KEY,
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
    Notes nvarchar(1000) NULL);
GO

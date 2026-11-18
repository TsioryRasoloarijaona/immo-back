-- FEATURE-n4-previsions-chantier.md N4b : types de travaux (GrhWorkedDayType) et colonnes de PrjProjectBuildingPrevision
-- (165 n'en crée que 2), types et clés étrangères de yesRemsProd. Idempotent, applicable à yesRemsTest.
IF OBJECT_ID('dbo.GrhWorkedDayType', 'U') IS NULL
CREATE TABLE dbo.GrhWorkedDayType (
    PKey uniqueidentifier NOT NULL,
    Code nvarchar(255) NULL,
    Description nvarchar(255) NULL,
    Notes nvarchar(500) NULL,
    UserCreate nvarchar(255) NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) NULL,
    UserUpdateDateTime datetime NULL,
    GroupDescription nvarchar(255) NULL,
    Unity nvarchar(255) NULL,
    UnitPriceHT decimal(26, 6) NULL,
    VatRatio decimal(26, 6) NULL,
    Unity1 nvarchar(255) NULL,
    UnitPriceHT1 decimal(26, 6) NULL,
    VatRatio1 decimal(26, 6) NULL,
    CONSTRAINT GrhWorkedDayType_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

IF COL_LENGTH('dbo.PrjProjectBuildingPrevision', 'GrhWorkedDayTypeId') IS NULL
ALTER TABLE dbo.PrjProjectBuildingPrevision ADD GrhWorkedDayTypeId uniqueidentifier NULL;
GO
IF COL_LENGTH('dbo.PrjProjectBuildingPrevision', 'Quantity') IS NULL
ALTER TABLE dbo.PrjProjectBuildingPrevision ADD Quantity decimal(26, 6) NULL;
GO
IF COL_LENGTH('dbo.PrjProjectBuildingPrevision', 'UnitPriceHT') IS NULL
ALTER TABLE dbo.PrjProjectBuildingPrevision ADD UnitPriceHT decimal(26, 6) NULL;
GO
IF COL_LENGTH('dbo.PrjProjectBuildingPrevision', 'VatRatio') IS NULL
ALTER TABLE dbo.PrjProjectBuildingPrevision ADD VatRatio decimal(26, 6) NULL;
GO
IF COL_LENGTH('dbo.PrjProjectBuildingPrevision', 'Notes') IS NULL
ALTER TABLE dbo.PrjProjectBuildingPrevision ADD Notes nvarchar(1000) NULL;
GO
IF COL_LENGTH('dbo.PrjProjectBuildingPrevision', 'UserCreate') IS NULL
ALTER TABLE dbo.PrjProjectBuildingPrevision ADD UserCreate nvarchar(255) NULL;
GO
IF COL_LENGTH('dbo.PrjProjectBuildingPrevision', 'UserCreateDateTime') IS NULL
ALTER TABLE dbo.PrjProjectBuildingPrevision ADD UserCreateDateTime datetime NULL;
GO
IF COL_LENGTH('dbo.PrjProjectBuildingPrevision', 'UserUpdate') IS NULL
ALTER TABLE dbo.PrjProjectBuildingPrevision ADD UserUpdate nvarchar(255) NULL;
GO
IF COL_LENGTH('dbo.PrjProjectBuildingPrevision', 'UserUpdateDateTime') IS NULL
ALTER TABLE dbo.PrjProjectBuildingPrevision ADD UserUpdateDateTime datetime NULL;
GO

IF OBJECT_ID('dbo.PrjProjectBuildingPrevision__GrhWorkedDayType_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjProjectBuildingPrevision ADD CONSTRAINT PrjProjectBuildingPrevision__GrhWorkedDayType_FK1
    FOREIGN KEY (GrhWorkedDayTypeId) REFERENCES dbo.GrhWorkedDayType (PKey) ON DELETE CASCADE;
GO

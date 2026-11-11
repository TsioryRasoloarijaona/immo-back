-- Lot projets-config-parite (FEATURE-projets-config-parite.md) : colonnes de production des tables reduites par 165, PV de
-- reunion qui cite un critere, et StkLeveling (deja en CI par 010, absente de yesRemsTest). Idempotent.
IF COL_LENGTH('dbo.PrjProjectCfgTrancheVentilation', 'CfgTrancheId') IS NULL
ALTER TABLE dbo.PrjProjectCfgTrancheVentilation ADD CfgTrancheId uniqueidentifier NULL;
GO
IF COL_LENGTH('dbo.PrjProjectCfgTrancheVentilation', 'VentilationRate') IS NULL
ALTER TABLE dbo.PrjProjectCfgTrancheVentilation ADD VentilationRate decimal(26, 6) NULL;
GO
IF COL_LENGTH('dbo.PrjProjectProgressCriteriaLine', 'Sorting') IS NULL
ALTER TABLE dbo.PrjProjectProgressCriteriaLine ADD Sorting int NULL;
GO
IF COL_LENGTH('dbo.PrjProjectProgressCriteriaLine', 'Description') IS NULL
ALTER TABLE dbo.PrjProjectProgressCriteriaLine ADD Description nvarchar(255) NULL;
GO
IF COL_LENGTH('dbo.PrjProjectProgressCriteriaLine', 'StlCategoryId') IS NULL
ALTER TABLE dbo.PrjProjectProgressCriteriaLine ADD StlCategoryId uniqueidentifier NULL;
GO
IF COL_LENGTH('dbo.PrjProjectProgressCriteriaLine', 'UserCreate') IS NULL
ALTER TABLE dbo.PrjProjectProgressCriteriaLine ADD UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL;
GO
IF OBJECT_ID('dbo.BulMeetingPrjProjectProgressCriteriaLine', 'U') IS NULL
CREATE TABLE dbo.BulMeetingPrjProjectProgressCriteriaLine (PKey uniqueidentifier NOT NULL PRIMARY KEY,
    Achievement decimal(26, 6) NULL, BulMeetingId uniqueidentifier NULL,
    PrjProjectProgressCriteriaLineId uniqueidentifier NULL, CfgTrancheLevelingdefinitionId uniqueidentifier NULL,
    UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL);
GO
IF OBJECT_ID('dbo.StkLeveling', 'U') IS NULL
CREATE TABLE dbo.StkLeveling (PKey uniqueidentifier NOT NULL PRIMARY KEY, Code nvarchar(255) NULL,
    UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL);
GO

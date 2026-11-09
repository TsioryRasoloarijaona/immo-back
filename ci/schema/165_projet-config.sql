-- Lot projet-config (FEATURE-projet-config.md §8.4) : tables qui referencent PrjProject ou PrjMarketType, comptees avant
-- une suppression. Reduites a PKey + la colonne de reference, cles etrangeres de yesRemsProd (noms et actions).
-- BuyLoan et BuySelection existent deja en CI (010) : creees ici pour yesRemsTest seulement.
IF OBJECT_ID('dbo.BuyLoan', 'U') IS NULL
CREATE TABLE dbo.BuyLoan (PKey uniqueidentifier NOT NULL PRIMARY KEY, PrjProjectId uniqueidentifier NULL);
GO
IF OBJECT_ID('dbo.BuySelection', 'U') IS NULL
CREATE TABLE dbo.BuySelection (PKey uniqueidentifier NOT NULL PRIMARY KEY, PrjProjectId uniqueidentifier NULL,
    PrjMarketTypeId uniqueidentifier NULL);
GO
IF OBJECT_ID('dbo.GrhWorkedDay', 'U') IS NULL
CREATE TABLE dbo.GrhWorkedDay (Pkey uniqueidentifier NOT NULL PRIMARY KEY, PrjProjectId uniqueidentifier NULL);
GO
IF OBJECT_ID('dbo.PrjProjectCfgTrancheVentilation', 'U') IS NULL
CREATE TABLE dbo.PrjProjectCfgTrancheVentilation (PKey uniqueidentifier NOT NULL PRIMARY KEY,
    PrjProjectId uniqueidentifier NULL);
GO
IF OBJECT_ID('dbo.BuyFolderPrjProjectVentilation', 'U') IS NULL
CREATE TABLE dbo.BuyFolderPrjProjectVentilation (PKey uniqueidentifier NOT NULL PRIMARY KEY,
    PrjProjectId uniqueidentifier NULL);
GO
IF OBJECT_ID('dbo.BulMeeting', 'U') IS NULL
CREATE TABLE dbo.BulMeeting (PKey uniqueidentifier NOT NULL PRIMARY KEY, PrjProjectId uniqueidentifier NULL);
GO
IF OBJECT_ID('dbo.PrjAchivementVersion', 'U') IS NULL
CREATE TABLE dbo.PrjAchivementVersion (PKey uniqueidentifier NOT NULL PRIMARY KEY, PrjProjectId uniqueidentifier NULL);
GO
IF OBJECT_ID('dbo.PrjCoefficientCost', 'U') IS NULL
CREATE TABLE dbo.PrjCoefficientCost (PKey uniqueidentifier NOT NULL PRIMARY KEY, PrjProjectId uniqueidentifier NULL);
GO
IF OBJECT_ID('dbo.PrjGanttTask', 'U') IS NULL
CREATE TABLE dbo.PrjGanttTask (Pkey uniqueidentifier NOT NULL PRIMARY KEY, PrjProjectId uniqueidentifier NULL);
GO
IF OBJECT_ID('dbo.PrjPrevision', 'U') IS NULL
CREATE TABLE dbo.PrjPrevision (PKey uniqueidentifier NOT NULL PRIMARY KEY, PrjProjectId uniqueidentifier NULL);
GO
IF OBJECT_ID('dbo.PrjPrevisionVersion', 'U') IS NULL
CREATE TABLE dbo.PrjPrevisionVersion (PKey uniqueidentifier NOT NULL PRIMARY KEY, PrjProjectId uniqueidentifier NULL);
GO
IF OBJECT_ID('dbo.PrjProjectAchievementPrevision', 'U') IS NULL
CREATE TABLE dbo.PrjProjectAchievementPrevision (PKey uniqueidentifier NOT NULL PRIMARY KEY,
    PrjProjectId uniqueidentifier NULL);
GO
IF OBJECT_ID('dbo.PrjProjectAchievementReal', 'U') IS NULL
CREATE TABLE dbo.PrjProjectAchievementReal (PKey uniqueidentifier NOT NULL PRIMARY KEY,
    PrjProjectId uniqueidentifier NULL);
GO
IF OBJECT_ID('dbo.PrjProjectBuildingPrevision', 'U') IS NULL
CREATE TABLE dbo.PrjProjectBuildingPrevision (PKey uniqueidentifier NOT NULL PRIMARY KEY,
    PrjProjectId uniqueidentifier NULL);
GO
IF OBJECT_ID('dbo.PrjProjectProgressCriteriaLine', 'U') IS NULL
CREATE TABLE dbo.PrjProjectProgressCriteriaLine (PKey uniqueidentifier NOT NULL PRIMARY KEY,
    PrjProjectId uniqueidentifier NULL);
GO
-- En production, la cle etrangere de CfgTrancheId pointe PrjProject (et non CfgTranche).
IF OBJECT_ID('dbo.CfgTrancheLevelingDefinition', 'U') IS NULL
CREATE TABLE dbo.CfgTrancheLevelingDefinition (PKey uniqueidentifier NOT NULL PRIMARY KEY,
    CfgTrancheId uniqueidentifier NULL);
GO
IF OBJECT_ID('dbo.BuyDocumentWorkFlowDocumentToAttach', 'U') IS NULL
CREATE TABLE dbo.BuyDocumentWorkFlowDocumentToAttach (PKey uniqueidentifier NOT NULL PRIMARY KEY,
    PrjMarketTypeId uniqueidentifier NULL);
GO

IF OBJECT_ID('dbo.GrhWorkedDay__PrjProject_FK1', 'F') IS NULL
ALTER TABLE dbo.GrhWorkedDay ADD CONSTRAINT GrhWorkedDay__PrjProject_FK1
    FOREIGN KEY (PrjProjectId) REFERENCES dbo.PrjProject (PKey);
GO
IF OBJECT_ID('dbo.PrjProjectCfgTrancheVentilation_PrjProject_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjProjectCfgTrancheVentilation ADD CONSTRAINT PrjProjectCfgTrancheVentilation_PrjProject_FK1
    FOREIGN KEY (PrjProjectId) REFERENCES dbo.PrjProject (PKey);
GO
IF OBJECT_ID('dbo.BuyFolderPrjProjectVentilation__PrjProject_FK1', 'F') IS NULL
ALTER TABLE dbo.BuyFolderPrjProjectVentilation ADD CONSTRAINT BuyFolderPrjProjectVentilation__PrjProject_FK1
    FOREIGN KEY (PrjProjectId) REFERENCES dbo.PrjProject (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.PrjProject_BulMeeting_FK1', 'F') IS NULL
ALTER TABLE dbo.BulMeeting ADD CONSTRAINT PrjProject_BulMeeting_FK1
    FOREIGN KEY (PrjProjectId) REFERENCES dbo.PrjProject (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.PrjProject_PrjAchivementVersion_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjAchivementVersion ADD CONSTRAINT PrjProject_PrjAchivementVersion_FK1
    FOREIGN KEY (PrjProjectId) REFERENCES dbo.PrjProject (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.PrjProject_PrjCoefficientCost_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjCoefficientCost ADD CONSTRAINT PrjProject_PrjCoefficientCost_FK1
    FOREIGN KEY (PrjProjectId) REFERENCES dbo.PrjProject (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.PrjProject_PrjGanttTask_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjGanttTask ADD CONSTRAINT PrjProject_PrjGanttTask_FK1
    FOREIGN KEY (PrjProjectId) REFERENCES dbo.PrjProject (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.PrjProjectId_PrjPrevision_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjPrevision ADD CONSTRAINT PrjProjectId_PrjPrevision_FK1
    FOREIGN KEY (PrjProjectId) REFERENCES dbo.PrjProject (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.PrjProject_PrjPrevisionVersion_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjPrevisionVersion ADD CONSTRAINT PrjProject_PrjPrevisionVersion_FK1
    FOREIGN KEY (PrjProjectId) REFERENCES dbo.PrjProject (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.PrjProjectAchievementPrevision__PrjMarket_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjProjectAchievementPrevision ADD CONSTRAINT PrjProjectAchievementPrevision__PrjMarket_FK1
    FOREIGN KEY (PrjProjectId) REFERENCES dbo.PrjProject (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.PrjProjectAchievementReal__PrjProject_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjProjectAchievementReal ADD CONSTRAINT PrjProjectAchievementReal__PrjProject_FK1
    FOREIGN KEY (PrjProjectId) REFERENCES dbo.PrjProject (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.PrjProjectBuildingPrevision__PrjProject_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjProjectBuildingPrevision ADD CONSTRAINT PrjProjectBuildingPrevision__PrjProject_FK1
    FOREIGN KEY (PrjProjectId) REFERENCES dbo.PrjProject (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.PrjMarketWarrantyLine_PrjProject_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjProjectProgressCriteriaLine ADD CONSTRAINT PrjMarketWarrantyLine_PrjProject_FK1
    FOREIGN KEY (PrjProjectId) REFERENCES dbo.PrjProject (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.CfgTrancheLevelingDefinition_PrjProject_FK1', 'F') IS NULL
ALTER TABLE dbo.CfgTrancheLevelingDefinition ADD CONSTRAINT CfgTrancheLevelingDefinition_PrjProject_FK1
    FOREIGN KEY (CfgTrancheId) REFERENCES dbo.PrjProject (PKey);
GO
IF OBJECT_ID('dbo.BuyDocumentWorkFlowDocumentToAttach__PrjMarketType_FK1', 'F') IS NULL
ALTER TABLE dbo.BuyDocumentWorkFlowDocumentToAttach ADD CONSTRAINT BuyDocumentWorkFlowDocumentToAttach__PrjMarketType_FK1
    FOREIGN KEY (PrjMarketTypeId) REFERENCES dbo.PrjMarketType (PKey);
GO

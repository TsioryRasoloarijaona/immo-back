-- Planification et nivellement de projet (FEATURE-n9-planification.md) : définitions de yesRemsProd, celles de
-- FixtureReferentielsPlanification.tables(). Idempotent, applicable à yesRemsTest.
IF COL_LENGTH(N'dbo.CfgTrancheLevelingDefinition', N'ParentId') IS NULL
    ALTER TABLE dbo.CfgTrancheLevelingDefinition ADD ParentId uniqueidentifier NULL;
IF COL_LENGTH(N'dbo.CfgTrancheLevelingDefinition', N'Code') IS NULL
    ALTER TABLE dbo.CfgTrancheLevelingDefinition ADD Code nvarchar(255) NULL;
IF COL_LENGTH(N'dbo.CfgTrancheLevelingDefinition', N'Sorting') IS NULL
    ALTER TABLE dbo.CfgTrancheLevelingDefinition ADD Sorting int NULL;
IF COL_LENGTH(N'dbo.CfgTrancheLevelingDefinition', N'UserCreate') IS NULL
    ALTER TABLE dbo.CfgTrancheLevelingDefinition ADD UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL,
        UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL;
IF OBJECT_ID(N'dbo.GrhWorkedDay', N'U') IS NOT NULL AND COL_LENGTH(N'dbo.GrhWorkedDay', N'CfgTrancheLevelingDefinitionId') IS NULL
    ALTER TABLE dbo.GrhWorkedDay ADD CfgTrancheLevelingDefinitionId uniqueidentifier NULL;
GO
IF OBJECT_ID(N'dbo.StkLevelingLine', N'U') IS NULL
CREATE TABLE dbo.StkLevelingLine (PKey uniqueidentifier NOT NULL PRIMARY KEY, Code nvarchar(255) NULL,
    Sorting int NULL, StkLevelId uniqueidentifier NULL,
    UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL);
GO
IF OBJECT_ID(N'dbo.PrjMarketHierarchy', N'U') IS NULL
CREATE TABLE dbo.PrjMarketHierarchy (PKey uniqueidentifier NOT NULL PRIMARY KEY,
    PrjMarketId uniqueidentifier NOT NULL, StkHierarchyId uniqueidentifier NOT NULL,
    UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL);
GO
IF OBJECT_ID(N'dbo.CfgGanttTaskType', N'U') IS NULL
CREATE TABLE dbo.CfgGanttTaskType (PKey uniqueidentifier NOT NULL PRIMARY KEY, Code nvarchar(255) NULL,
    Description nvarchar(255) NULL, Sorting int NULL,
    UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL);
GO
IF OBJECT_ID(N'dbo.CfgGanttTask', N'U') IS NULL
CREATE TABLE dbo.CfgGanttTask (PKey uniqueidentifier NOT NULL PRIMARY KEY, Code nvarchar(255) NULL,
    Description nvarchar(255) NULL, Notes nvarchar(1000) NULL, Sorting int NULL, Depth int NULL,
    StartDate datetime NULL, Duration int NULL, PercentComplete decimal(26, 6) NULL, AlertBeforeDays int NULL,
    ConstraintType int NULL, ConstraintDate datetime NULL, Deadline datetime NULL,
    Predecessors nvarchar(4000) NULL, ResourceNames nvarchar(4000) NULL, IsMilestone bit NULL,
    ParentId uniqueidentifier NULL, CfgGanttProjectId uniqueidentifier NULL,
    CfgGanttTaskTypeId uniqueidentifier NULL, PrjMarketId uniqueidentifier NULL,
    UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL,
    StlCategoryId uniqueidentifier NULL, StrEntityLinkedId uniqueidentifier NULL,
    StrWorkFlowStartId uniqueidentifier NULL, StrWorkFlowEndId uniqueidentifier NULL);
GO
IF OBJECT_ID(N'dbo.CfgGanttTask_CfgGanttTaskType_FK1', N'F') IS NULL
ALTER TABLE dbo.CfgGanttTask ADD CONSTRAINT CfgGanttTask_CfgGanttTaskType_FK1
    FOREIGN KEY (CfgGanttTaskTypeId) REFERENCES dbo.CfgGanttTaskType (PKey);
GO
IF OBJECT_ID(N'dbo.CfgGanttProject', N'U') IS NULL
CREATE TABLE dbo.CfgGanttProject (PKey uniqueidentifier NOT NULL PRIMARY KEY, Code nvarchar(255) NULL,
    Description nvarchar(255) NULL, StartDate datetime NULL, VersionDate datetime NULL, Notes nvarchar(1000) NULL,
    UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL,
    CfgGanttProjectMainVersionId uniqueidentifier NULL, ObjectId uniqueidentifier NULL, StrEntityObjectId uniqueidentifier NULL);
GO
IF OBJECT_ID(N'dbo.CfgGanttOwner', N'U') IS NULL
CREATE TABLE dbo.CfgGanttOwner (PKey uniqueidentifier NOT NULL PRIMARY KEY, Visible bit NULL,
    CfgGanttProjectId uniqueidentifier NULL, CfgTierId uniqueidentifier NULL,
    UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL,
    Code nvarchar(255) NULL, Description nvarchar(255) NULL, Notes nvarchar(1000) NULL);
GO
IF OBJECT_ID(N'dbo.CfgGanttProject__CfgGanttProjectMainVersion_FK1', N'F') IS NULL
ALTER TABLE dbo.CfgGanttProject ADD CONSTRAINT CfgGanttProject__CfgGanttProjectMainVersion_FK1
    FOREIGN KEY (CfgGanttProjectMainVersionId) REFERENCES dbo.CfgGanttProject (PKey);
GO
IF OBJECT_ID(N'dbo.CfgGanttTask__CfgGanttProject_FK1', N'F') IS NULL
ALTER TABLE dbo.CfgGanttTask ADD CONSTRAINT CfgGanttTask__CfgGanttProject_FK1
    FOREIGN KEY (CfgGanttProjectId) REFERENCES dbo.CfgGanttProject (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID(N'dbo.CfgGanttOwner__CfgGanttProject_FK1', N'F') IS NULL
ALTER TABLE dbo.CfgGanttOwner ADD CONSTRAINT CfgGanttOwner__CfgGanttProject_FK1
    FOREIGN KEY (CfgGanttProjectId) REFERENCES dbo.CfgGanttProject (PKey) ON DELETE CASCADE;
GO

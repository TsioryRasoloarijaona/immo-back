-- Lot admin-type-priorite-tache (FEATURE-admin-type-priorite-tache.md) : cles etrangeres de yesRemsProd, NO ACTION.
-- CI seulement : AdmTask reduite aux colonnes du lot, que FixtureTachesAdmin ne laisse pas dans yesRemsTest.
IF OBJECT_ID('dbo.AdmTask', 'U') IS NULL
CREATE TABLE dbo.AdmTask (
    PKey uniqueidentifier NOT NULL CONSTRAINT PK_AdmTask PRIMARY KEY,
    AdmTaskTypeId uniqueidentifier NULL,
    AdmTaskPriorityId uniqueidentifier NULL
);
GO

IF OBJECT_ID('dbo.AdmTask__AdmTaskType_FK1', 'F') IS NULL
ALTER TABLE dbo.AdmTask ADD CONSTRAINT AdmTask__AdmTaskType_FK1
    FOREIGN KEY (AdmTaskTypeId) REFERENCES dbo.AdmTaskType (PKey);
GO

IF OBJECT_ID('dbo.AdmTask__AdmTaskPriority_FK1', 'F') IS NULL
ALTER TABLE dbo.AdmTask ADD CONSTRAINT AdmTask__AdmTaskPriority_FK1
    FOREIGN KEY (AdmTaskPriorityId) REFERENCES dbo.AdmTaskPriority (PKey);
GO

-- Lot admin-taches (FEATURE-admin-taches.md) : cles etrangeres de yesRemsProd, NO ACTION.
IF OBJECT_ID('dbo.AdmTask__AdmModule_FK1', 'F') IS NULL
ALTER TABLE dbo.AdmTask ADD CONSTRAINT AdmTask__AdmModule_FK1 FOREIGN KEY (AdmModuleId) REFERENCES dbo.AdmModule (PKey);
GO
IF OBJECT_ID('dbo.AdmTask__CfgCompany_FK1', 'F') IS NULL
ALTER TABLE dbo.AdmTask ADD CONSTRAINT AdmTask__CfgCompany_FK1 FOREIGN KEY (CfgCompanyId) REFERENCES dbo.CfgCompany (PKey);
GO
IF OBJECT_ID('dbo.AdmTask_ComProspection_FK1', 'F') IS NULL
ALTER TABLE dbo.AdmTask ADD CONSTRAINT AdmTask_ComProspection_FK1 FOREIGN KEY (CfgProspectId) REFERENCES dbo.CfgTier (PKey);
GO
IF OBJECT_ID('dbo.AdmTask__CfgTierAssignment_FK1', 'F') IS NULL
ALTER TABLE dbo.AdmTask ADD CONSTRAINT AdmTask__CfgTierAssignment_FK1 FOREIGN KEY (CfgTierAssignmentId) REFERENCES dbo.CfgTier (PKey);
GO
IF OBJECT_ID('dbo.AdmTask__CfgTierManager_FK1', 'F') IS NULL
ALTER TABLE dbo.AdmTask ADD CONSTRAINT AdmTask__CfgTierManager_FK1 FOREIGN KEY (CfgTierManagerId) REFERENCES dbo.CfgTier (PKey);
GO
IF OBJECT_ID('dbo.AdmTask__CfgTierTrigger_FK1', 'F') IS NULL
ALTER TABLE dbo.AdmTask ADD CONSTRAINT AdmTask__CfgTierTrigger_FK1 FOREIGN KEY (CfgTierTriggerId) REFERENCES dbo.CfgTier (PKey);
GO
IF OBJECT_ID('dbo.AdmTask_PrjProject_FK1', 'F') IS NULL
ALTER TABLE dbo.AdmTask ADD CONSTRAINT AdmTask_PrjProject_FK1 FOREIGN KEY (PrjProjectId) REFERENCES dbo.PrjProject (PKey);
GO
IF OBJECT_ID('dbo.AdmTask__StrEntity_FK1', 'F') IS NULL
ALTER TABLE dbo.AdmTask ADD CONSTRAINT AdmTask__StrEntity_FK1 FOREIGN KEY (StrEntityId) REFERENCES dbo.StrEntity (PKey);
GO
IF OBJECT_ID('dbo.AdmTask__StrStatus_FK1', 'F') IS NULL
ALTER TABLE dbo.AdmTask ADD CONSTRAINT AdmTask__StrStatus_FK1 FOREIGN KEY (StrStatusId) REFERENCES dbo.StrStatus (PKey);
GO

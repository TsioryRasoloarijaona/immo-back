-- Lot P1c (FEATURE-patrimoine-tranche-regles.md §9.1, §9.4) : cles etrangeres des trois tables, comme en production.
IF OBJECT_ID('dbo.ComSalesCommission__CfgTranche_FK1', 'F') IS NULL
ALTER TABLE dbo.ComSalesCommission ADD CONSTRAINT ComSalesCommission__CfgTranche_FK1
    FOREIGN KEY (CfgTrancheId) REFERENCES dbo.CfgTranche (PKey) ON DELETE CASCADE;
GO

IF OBJECT_ID('dbo.ComSalesCommission__StkItemCategory_FK1', 'F') IS NULL
ALTER TABLE dbo.ComSalesCommission ADD CONSTRAINT ComSalesCommission__StkItemCategory_FK1
    FOREIGN KEY (StkItemCategoryId) REFERENCES dbo.StkItemCategory (PKey);
GO

IF OBJECT_ID('dbo.ComSalesCommission__CfgTier_FK1', 'F') IS NULL
ALTER TABLE dbo.ComSalesCommission ADD CONSTRAINT ComSalesCommission__CfgTier_FK1
    FOREIGN KEY (CfgTierId) REFERENCES dbo.CfgTier (PKey);
GO

IF OBJECT_ID('dbo.ComSalesCommission__ComEmployee_FK1', 'F') IS NULL
ALTER TABLE dbo.ComSalesCommission ADD CONSTRAINT ComSalesCommission__ComEmployee_FK1
    FOREIGN KEY (ComEmployeeId) REFERENCES dbo.CfgTier (PKey);
GO

IF OBJECT_ID('dbo.ComSalesCommission_ComAction_FK1', 'F') IS NULL
ALTER TABLE dbo.ComSalesCommission ADD CONSTRAINT ComSalesCommission_ComAction_FK1
    FOREIGN KEY (ComActionId) REFERENCES dbo.ComAction (PKey);
GO

IF OBJECT_ID('dbo.CfgTrancheFolderEntity__CfgTranche_FK1', 'F') IS NULL
ALTER TABLE dbo.CfgTrancheFolderEntity ADD CONSTRAINT CfgTrancheFolderEntity__CfgTranche_FK1
    FOREIGN KEY (CfgTrancheId) REFERENCES dbo.CfgTranche (PKey) ON DELETE CASCADE;
GO

IF OBJECT_ID('dbo.StrEntity', 'U') IS NOT NULL
    AND OBJECT_ID('dbo.CfgTrancheFolderEntity__StrFolderEntity_FK1', 'F') IS NULL
ALTER TABLE dbo.CfgTrancheFolderEntity ADD CONSTRAINT CfgTrancheFolderEntity__StrFolderEntity_FK1
    FOREIGN KEY (StrFolderEntityId) REFERENCES dbo.StrEntity (PKey);
GO

IF OBJECT_ID('dbo.CfgTrancheFolderEntity__StrStatusDeliveryId1_FK1', 'F') IS NULL
ALTER TABLE dbo.CfgTrancheFolderEntity ADD CONSTRAINT CfgTrancheFolderEntity__StrStatusDeliveryId1_FK1
    FOREIGN KEY (StrStatusDeliveryId1) REFERENCES dbo.StrStatus (PKey);
GO

IF OBJECT_ID('dbo.CfgTrancheFolderEntity__StrStatusDeliveryId2_FK1', 'F') IS NULL
ALTER TABLE dbo.CfgTrancheFolderEntity ADD CONSTRAINT CfgTrancheFolderEntity__StrStatusDeliveryId2_FK1
    FOREIGN KEY (StrStatusDeliveryId2) REFERENCES dbo.StrStatus (PKey);
GO

IF OBJECT_ID('dbo.CfgPrevision_CfgTranche_FK1', 'F') IS NULL
ALTER TABLE dbo.CfgPrevision ADD CONSTRAINT CfgPrevision_CfgTranche_FK1
    FOREIGN KEY (StkHierarchyId) REFERENCES dbo.CfgTranche (PKey);
GO

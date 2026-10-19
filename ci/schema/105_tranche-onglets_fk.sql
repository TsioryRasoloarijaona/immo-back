-- Lot P1b (FEATURE-patrimoine-tranche-onglets.md §8.1) : cles etrangeres du plancher couvert, comme en production.
IF OBJECT_ID('dbo.CfgTrancheCoveredFloor_CfgTranche_FK1', 'F') IS NULL
ALTER TABLE dbo.CfgTrancheCoveredFloor ADD CONSTRAINT CfgTrancheCoveredFloor_CfgTranche_FK1
    FOREIGN KEY (CfgTrancheId) REFERENCES dbo.CfgTranche (PKey) ON DELETE CASCADE;
GO

IF OBJECT_ID('dbo.CfgTrancheCoveredFloor_StkItemCategory_FK1', 'F') IS NULL
ALTER TABLE dbo.CfgTrancheCoveredFloor ADD CONSTRAINT CfgTrancheCoveredFloor_StkItemCategory_FK1
    FOREIGN KEY (StkItemCategoryId) REFERENCES dbo.StkItemCategory (PKey);
GO

-- Lot P3 (FEATURE-patrimoine-plans.md) : cles etrangeres des plans d'etage, noms et regles de production (NO ACTION).
IF OBJECT_ID('dbo.StkHierarchyPlan_CfgTranche_FK1', 'F') IS NULL
ALTER TABLE dbo.StkHierarchyPlan ADD CONSTRAINT StkHierarchyPlan_CfgTranche_FK1
    FOREIGN KEY (StkHierarchyId) REFERENCES dbo.CfgTranche (PKey);
GO

IF OBJECT_ID('dbo.StkHierarchyPlan_StkFloor_FK1', 'F') IS NULL
ALTER TABLE dbo.StkHierarchyPlan ADD CONSTRAINT StkHierarchyPlan_StkFloor_FK1
    FOREIGN KEY (StkFloorId) REFERENCES dbo.StkFloor (PKey);
GO

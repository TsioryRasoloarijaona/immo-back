-- Lot P2a (FEATURE-patrimoine-article-fiche.md §9.3) : cles etrangeres des lignes de la fiche article, comme en production.
IF OBJECT_ID('dbo.StkItemVue_StkItem_FK1', 'F') IS NULL
ALTER TABLE dbo.StkItemVue ADD CONSTRAINT StkItemVue_StkItem_FK1
    FOREIGN KEY (StkItemId) REFERENCES dbo.StkItem (PKey) ON DELETE CASCADE;
GO

IF OBJECT_ID('dbo.StkItemVue_StkVue_FK1', 'F') IS NULL
ALTER TABLE dbo.StkItemVue ADD CONSTRAINT StkItemVue_StkVue_FK1
    FOREIGN KEY (StkVueId) REFERENCES dbo.StkVue (PKey) ON DELETE CASCADE;
GO

IF OBJECT_ID('dbo.StkItemContenance_StkItem_FK1', 'F') IS NULL
ALTER TABLE dbo.StkItemContenance ADD CONSTRAINT StkItemContenance_StkItem_FK1
    FOREIGN KEY (StkItemId) REFERENCES dbo.StkItem (PKey) ON DELETE CASCADE;
GO

IF OBJECT_ID('dbo.StkItemContenance_StkContenance_FK1', 'F') IS NULL
ALTER TABLE dbo.StkItemContenance ADD CONSTRAINT StkItemContenance_StkContenance_FK1
    FOREIGN KEY (StkContenanceId) REFERENCES dbo.StkContenance (PKey) ON DELETE CASCADE;
GO

IF OBJECT_ID('dbo.StkPresentationTypeContenance_StkPresentationType_FK1', 'F') IS NULL
ALTER TABLE dbo.StkPresentationTypeContenance ADD CONSTRAINT StkPresentationTypeContenance_StkPresentationType_FK1
    FOREIGN KEY (StkPresentationTypeId) REFERENCES dbo.StkPresentationType (PKey) ON DELETE CASCADE;
GO

IF OBJECT_ID('dbo.StkPresentationTypeContenance_StkContenance_FK1', 'F') IS NULL
ALTER TABLE dbo.StkPresentationTypeContenance ADD CONSTRAINT StkPresentationTypeContenance_StkContenance_FK1
    FOREIGN KEY (StkContenanceId) REFERENCES dbo.StkContenance (PKey);
GO

IF OBJECT_ID('dbo.StkHierarchyPath_StkItem_FK1', 'F') IS NULL
ALTER TABLE dbo.StkHierarchyPath ADD CONSTRAINT StkHierarchyPath_StkItem_FK1
    FOREIGN KEY (StkItemId) REFERENCES dbo.StkItem (PKey) ON DELETE CASCADE;
GO

IF OBJECT_ID('dbo.StkHierarchyPath_StkFloor_FK1', 'F') IS NULL
ALTER TABLE dbo.StkHierarchyPath ADD CONSTRAINT StkHierarchyPath_StkFloor_FK1
    FOREIGN KEY (StkFloorId) REFERENCES dbo.StkFloor (PKey) ON DELETE CASCADE;
GO

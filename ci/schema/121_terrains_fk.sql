-- Lot P7a (FEATURE-patrimoine-terrains.md §6.1) : cles etrangeres des trois tables nouvelles, actions de production.
-- Celles de BuyDocumentLine sont deja dans 020.
IF OBJECT_ID('dbo.BuyVentilationCategory_BuyDocument_FK1', 'F') IS NULL
ALTER TABLE dbo.BuyVentilationCategory ADD CONSTRAINT BuyVentilationCategory_BuyDocument_FK1
    FOREIGN KEY (BuyDocumentId) REFERENCES dbo.BuyDocument (PKey) ON DELETE CASCADE;
GO

IF OBJECT_ID('dbo.BuyVentilationCategory_StlCategory_FK1', 'F') IS NULL
ALTER TABLE dbo.BuyVentilationCategory ADD CONSTRAINT BuyVentilationCategory_StlCategory_FK1
    FOREIGN KEY (StlCategoryId) REFERENCES dbo.StlCategory (PKey) ON DELETE CASCADE;
GO

IF OBJECT_ID('dbo.BuyDocumentStatusHistory__BuyDocument_FK1', 'F') IS NULL
ALTER TABLE dbo.BuyDocumentStatusHistory ADD CONSTRAINT BuyDocumentStatusHistory__BuyDocument_FK1
    FOREIGN KEY (BuyDocumentId) REFERENCES dbo.BuyDocument (PKey) ON DELETE CASCADE;
GO

IF OBJECT_ID('dbo.BuyDocumentStatusHistory__BuyDocumentStatus_FK1', 'F') IS NULL
ALTER TABLE dbo.BuyDocumentStatusHistory ADD CONSTRAINT BuyDocumentStatusHistory__BuyDocumentStatus_FK1
    FOREIGN KEY (BuyDocumentStatusId) REFERENCES dbo.BuyDocumentStatus (PKey) ON DELETE CASCADE;
GO

IF OBJECT_ID('dbo.BuyDocumentStatusHistory__AdmUser_FK1', 'F') IS NULL
ALTER TABLE dbo.BuyDocumentStatusHistory ADD CONSTRAINT BuyDocumentStatusHistory__AdmUser_FK1
    FOREIGN KEY (AdmUserId) REFERENCES dbo.AdmUser (PKey) ON DELETE CASCADE;
GO

IF OBJECT_ID('dbo.BuyDocumentWorkFlow__StrEntity_FK1', 'F') IS NULL
ALTER TABLE dbo.BuyDocumentWorkFlow ADD CONSTRAINT BuyDocumentWorkFlow__StrEntity_FK1
    FOREIGN KEY (StrEntityId) REFERENCES dbo.StrEntity (PKey) ON DELETE CASCADE;
GO

IF OBJECT_ID('dbo.BuyDocumentWorkFlow__BuyDocumentStatus_Start_FK1', 'F') IS NULL
ALTER TABLE dbo.BuyDocumentWorkFlow ADD CONSTRAINT BuyDocumentWorkFlow__BuyDocumentStatus_Start_FK1
    FOREIGN KEY (StartStatusId) REFERENCES dbo.BuyDocumentStatus (PKey);
GO

IF OBJECT_ID('dbo.BuyDocumentWorkFlow__BuyDocumentStatus_End_FK1', 'F') IS NULL
ALTER TABLE dbo.BuyDocumentWorkFlow ADD CONSTRAINT BuyDocumentWorkFlow__BuyDocumentStatus_End_FK1
    FOREIGN KEY (EndStatusId) REFERENCES dbo.BuyDocumentStatus (PKey);
GO

IF OBJECT_ID('dbo.BuyDocumentWorkFlow__StrWorkFlow_FK1', 'F') IS NULL
ALTER TABLE dbo.BuyDocumentWorkFlow ADD CONSTRAINT BuyDocumentWorkFlow__StrWorkFlow_FK1
    FOREIGN KEY (StrWorkFlowId) REFERENCES dbo.StrWorkFlow (PKey);
GO

-- Lot COM (FEATURE-vente-commissions.md) : cles etrangeres de yesRemsProd, actions de production.
-- ComCommissionLine__ComCommission_FK1 omise : la table d'en-tete ComCommission est hors lot.
IF OBJECT_ID('dbo.ComCommissionLine_ComFolder_FK1', 'F') IS NULL
ALTER TABLE dbo.ComCommissionLine ADD CONSTRAINT ComCommissionLine_ComFolder_FK1
    FOREIGN KEY (ComFolderId) REFERENCES dbo.ComFolder (PKey);
GO

IF OBJECT_ID('dbo.ComCommissionLine_CfgEmployee_FK1', 'F') IS NULL
ALTER TABLE dbo.ComCommissionLine ADD CONSTRAINT ComCommissionLine_CfgEmployee_FK1
    FOREIGN KEY (CfgEmployeeId) REFERENCES dbo.CfgTier (PKey);
GO

IF OBJECT_ID('dbo.ComCommissionLine_CfgTranche_FK1', 'F') IS NULL
ALTER TABLE dbo.ComCommissionLine ADD CONSTRAINT ComCommissionLine_CfgTranche_FK1
    FOREIGN KEY (CfgTrancheId) REFERENCES dbo.CfgTranche (PKey);
GO

IF OBJECT_ID('dbo.ComCommissionLine_StkItem_FK1', 'F') IS NULL
ALTER TABLE dbo.ComCommissionLine ADD CONSTRAINT ComCommissionLine_StkItem_FK1
    FOREIGN KEY (StkItemId) REFERENCES dbo.StkItem (PKey);
GO

IF OBJECT_ID('dbo.ComCommissionLine_StkItemCategory_FK1', 'F') IS NULL
ALTER TABLE dbo.ComCommissionLine ADD CONSTRAINT ComCommissionLine_StkItemCategory_FK1
    FOREIGN KEY (StkItemCategoryId) REFERENCES dbo.StkItemCategory (PKey);
GO

IF OBJECT_ID('dbo.ComCommissionLine__StrFolderStatus_FK1', 'F') IS NULL
ALTER TABLE dbo.ComCommissionLine ADD CONSTRAINT ComCommissionLine__StrFolderStatus_FK1
    FOREIGN KEY (StrFolderStatusId) REFERENCES dbo.StrStatus (PKey);
GO

IF OBJECT_ID('dbo.ComCommissionLine_BuyDocument_FK1', 'F') IS NULL
ALTER TABLE dbo.ComCommissionLine ADD CONSTRAINT ComCommissionLine_BuyDocument_FK1
    FOREIGN KEY (BuyDocumentId) REFERENCES dbo.BuyDocument (PKey) ON DELETE SET NULL;
GO

IF OBJECT_ID('dbo.BuyDocument_BuyVentilationExpense_FK1', 'F') IS NULL
ALTER TABLE dbo.BuyVentilationExpense ADD CONSTRAINT BuyDocument_BuyVentilationExpense_FK1
    FOREIGN KEY (BuyDocumentId) REFERENCES dbo.BuyDocument (PKey) ON DELETE CASCADE;
GO

IF OBJECT_ID('dbo.FK_BuyVentilationExpense_PrjProject', 'F') IS NULL
ALTER TABLE dbo.BuyVentilationExpense ADD CONSTRAINT FK_BuyVentilationExpense_PrjProject
    FOREIGN KEY (PrjProjectId) REFERENCES dbo.PrjProject (PKey) ON DELETE CASCADE;
GO

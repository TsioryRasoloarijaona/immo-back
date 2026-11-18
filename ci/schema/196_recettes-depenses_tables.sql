-- FEATURE-n3-recettes-depenses.md : avoirs / facture d'origine (StlDocumentLine.StlInvoiceLineId) et catégories
-- (StlVentilationCategory) des listes Recettes, colonnes de yesRemsProd. Idempotent, applicable à yesRemsTest.
IF COL_LENGTH('dbo.StlDocumentLine', 'StlInvoiceLineId') IS NULL
ALTER TABLE dbo.StlDocumentLine ADD StlInvoiceLineId uniqueidentifier NULL;
GO

-- Société et module de la pièce de recette (périmètre D4) ; déjà présents en CI (010_tables.sql).
IF COL_LENGTH('dbo.StlDocument', 'CfgCompanyId') IS NULL
ALTER TABLE dbo.StlDocument ADD CfgCompanyId uniqueidentifier NULL;
GO

IF COL_LENGTH('dbo.StlDocument', 'AdmModuleId') IS NULL
ALTER TABLE dbo.StlDocument ADD AdmModuleId uniqueidentifier NULL;
GO

IF OBJECT_ID('dbo.StlVentilationCategory', 'U') IS NULL
CREATE TABLE dbo.StlVentilationCategory (
    PKey uniqueidentifier NOT NULL,
    VentilationRate decimal(26, 6) NULL,
    StlDocumentId uniqueidentifier NULL,
    StlCategoryId uniqueidentifier NULL,
    CONSTRAINT StlVentilationCategory_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

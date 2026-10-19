-- Lot P5 (FEATURE-entites-juridiques.md §7.1) : cles etrangeres des tables de 106, noms et actions de yesRemsProd.
-- CfgCompany -> CfgGroupShareholder (CASCADE) est deja dans 020. Idempotent.
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'CfgCompanyShareholder_CfgCompany_FK')
ALTER TABLE dbo.CfgCompanyShareholder WITH CHECK ADD CONSTRAINT CfgCompanyShareholder_CfgCompany_FK
    FOREIGN KEY (CfgCompanyId) REFERENCES dbo.CfgCompany (PKey) ON DELETE CASCADE;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'CfgGroupShareholderLine_CfgGroupShareholder_FK1')
ALTER TABLE dbo.CfgGroupShareholderLine WITH CHECK ADD CONSTRAINT CfgGroupShareholderLine_CfgGroupShareholder_FK1
    FOREIGN KEY (CfgGroupShareholderId) REFERENCES dbo.CfgGroupShareholder (PKey) ON DELETE CASCADE;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'CfgGroupShareholderLine_CfgShareholder_FK1')
ALTER TABLE dbo.CfgGroupShareholderLine WITH CHECK ADD CONSTRAINT CfgGroupShareholderLine_CfgShareholder_FK1
    FOREIGN KEY (CfgShareholderId) REFERENCES dbo.CfgShareholder (PKey) ON DELETE CASCADE;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = 'CfgCompanyShareholder__CfgShareholder_FK1')
ALTER TABLE dbo.CfgCompanyShareholder WITH CHECK ADD CONSTRAINT CfgCompanyShareholder__CfgShareholder_FK1
    FOREIGN KEY (CfgShareholderId) REFERENCES dbo.CfgShareholder (PKey);
GO

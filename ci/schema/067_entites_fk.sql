-- ============================================================================
-- Lot P5 : cles etrangeres de production des tables de 066 (noms et regles ON DELETE exacts).
-- CfgCompany -> CfgGroupShareholder (CASCADE) est deja dans 020.
-- ============================================================================
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

ALTER TABLE [dbo].[CfgGroupShareholderLine] WITH CHECK ADD CONSTRAINT [CfgGroupShareholderLine_CfgGroupShareholder_FK1] FOREIGN KEY([CfgGroupShareholderId])
REFERENCES [dbo].[CfgGroupShareholder] ([PKey])
ON DELETE CASCADE
GO

ALTER TABLE [dbo].[CfgGroupShareholderLine] WITH CHECK ADD CONSTRAINT [CfgGroupShareholderLine_CfgShareholder_FK1] FOREIGN KEY([CfgShareholderId])
REFERENCES [dbo].[CfgShareholder] ([PKey])
ON DELETE CASCADE
GO

ALTER TABLE [dbo].[CfgCompanyShareholder] WITH CHECK ADD CONSTRAINT [CfgCompanyShareholder_CfgCompany_FK] FOREIGN KEY([CfgCompanyId])
REFERENCES [dbo].[CfgCompany] ([PKey])
ON DELETE CASCADE
GO

ALTER TABLE [dbo].[CfgCompanyShareholder] WITH CHECK ADD CONSTRAINT [CfgCompanyShareholder__CfgShareholder_FK1] FOREIGN KEY([CfgShareholderId])
REFERENCES [dbo].[CfgShareholder] ([PKey])
GO

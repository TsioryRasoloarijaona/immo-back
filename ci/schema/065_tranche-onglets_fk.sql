-- ============================================================================
-- Lot P1b : cles etrangeres de production (noms exacts, CASCADE vers la tranche).
-- ============================================================================
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

ALTER TABLE [dbo].[CfgTrancheCoveredFloor] WITH CHECK ADD CONSTRAINT [CfgTrancheCoveredFloor_CfgTranche_FK1] FOREIGN KEY([CfgTrancheId])
REFERENCES [dbo].[CfgTranche] ([PKey])
ON DELETE CASCADE
GO

ALTER TABLE [dbo].[CfgTrancheCoveredFloor] WITH CHECK ADD CONSTRAINT [CfgTrancheCoveredFloor_StkItemCategory_FK1] FOREIGN KEY([StkItemCategoryId])
REFERENCES [dbo].[StkItemCategory] ([PKey])
GO

ALTER TABLE [dbo].[CfgTrancheSettlementCategory] WITH CHECK ADD CONSTRAINT [CfgTrancheSettlementCategory__ComSettlementCategory_FK1] FOREIGN KEY([ComSettlementCategoryId])
REFERENCES [dbo].[ComSettlementCategory] ([PKey])
ON DELETE CASCADE
GO

ALTER TABLE [dbo].[CfgTrancheSettlementCategory] WITH CHECK ADD CONSTRAINT [CfgTrancheSettlementCategory__Cfgtranche_FK1] FOREIGN KEY([CfgtrancheId])
REFERENCES [dbo].[CfgTranche] ([PKey])
ON DELETE CASCADE
GO

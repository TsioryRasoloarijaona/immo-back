-- ============================================================================
-- Lot P1b (FEATURE-patrimoine-tranche-onglets.md) : plancher couvert et frais de vente de la tranche.
-- Types exacts de yesRemsProd. Piege : la colonne de tranche des frais s'ecrit CfgtrancheId (t minuscule).
-- ============================================================================
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[CfgTrancheCoveredFloor](
	[PKey] [uniqueidentifier] NOT NULL,
	[CfgTrancheId] [uniqueidentifier] NULL,
	[StkItemCategoryId] [uniqueidentifier] NULL,
	[Area] [decimal](26, 16) NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [CfgTrancheCoveredFloor_PK] PRIMARY KEY CLUSTERED ([PKey] ASC)
)
GO

CREATE TABLE [dbo].[CfgTrancheSettlementCategory](
	[PKey] [uniqueidentifier] NOT NULL,
	[Sorting] [int] NULL,
	[ComSettlementCategoryId] [uniqueidentifier] NULL,
	[CfgtrancheId] [uniqueidentifier] NULL,
	[AmountHT] [decimal](26, 6) NULL,
	[VatRatio] [decimal](26, 6) NULL,
	[AmountTTC] [decimal](26, 6) NULL,
	[Notes] [nvarchar](500) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[PercentOf] [decimal](26, 6) NULL,
 CONSTRAINT [CfgTrancheSettlementCategory_PK] PRIMARY KEY CLUSTERED ([PKey] ASC)
)
GO

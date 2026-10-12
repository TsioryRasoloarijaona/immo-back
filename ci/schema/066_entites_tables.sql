-- ============================================================================
-- Lot P5 (FEATURE-entites-juridiques.md) : actionnaires, lignes de groupe, repartition.
-- Types exacts de yesRemsProd. CfgCompany et CfgGroupShareholder sont deja dans 010.
-- ============================================================================
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[CfgShareholder](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[Notes] [nvarchar](500) COLLATE French_CI_AS NULL,
 CONSTRAINT [CfgShareholder_PK] PRIMARY KEY CLUSTERED ([PKey] ASC)
)
GO

CREATE TABLE [dbo].[CfgGroupShareholderLine](
	[PKey] [uniqueidentifier] NOT NULL,
	[CfgGroupShareholderId] [uniqueidentifier] NULL,
	[CfgShareholderId] [uniqueidentifier] NULL,
	[SharingNumber] [decimal](26, 6) NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [CfgGroupShareholderLine_PK] PRIMARY KEY CLUSTERED ([PKey] ASC)
)
GO

CREATE TABLE [dbo].[CfgCompanyShareholder](
	[PKey] [uniqueidentifier] NOT NULL,
	[CfgCompanyId] [uniqueidentifier] NULL,
	[SharingNumber] [int] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[CfgShareholderId] [uniqueidentifier] NULL,
 CONSTRAINT [CfgCompanyShareholder_PK] PRIMARY KEY CLUSTERED ([PKey] ASC)
)
GO

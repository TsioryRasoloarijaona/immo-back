-- FEATURE-lot-fiche-suivi.md : avenants du lot (PrjClause), lus par la liste et la fiche des lots.
-- Définition de yesRemsProd ; déjà créée par 010 en CI, idempotent pour yesRemsTest.
IF OBJECT_ID('dbo.PrjClause', 'U') IS NULL
CREATE TABLE [dbo].[PrjClause](
	[PKey] [uniqueidentifier] NOT NULL,
	[Sorting] [int] NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[DocDate] [datetime] NULL,
	[DocRef] [nvarchar](255) COLLATE French_CI_AS NULL,
	[SupplierDescription] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[AmountHT] [decimal](26, 6) NULL,
	[AmountVat] [decimal](26, 6) NULL,
	[AmountTTC] [decimal](26, 6) NULL,
	[AmountToPay] [decimal](26, 6) NULL,
	[WarrantyBasedOnAmount] [bit] NULL,
	[WarrantyRatio] [decimal](26, 6) NULL,
	[WarrantyAmount] [decimal](26, 6) NULL,
	[DECBasedOnAmount] [bit] NULL,
	[InsuranceDECRatio] [decimal](26, 6) NULL,
	[InsuranceDECAmount] [decimal](26, 6) NULL,
	[TRCBasedOnAmount] [bit] NULL,
	[InsuranceTRCAmount] [decimal](26, 6) NULL,
	[InsuranceTRCRatio] [decimal](26, 6) NULL,
	[ProrataBasedOnAmount] [bit] NULL,
	[ProportionRatio] [decimal](26, 6) NULL,
	[ProportionAmount] [decimal](26, 6) NULL,
	[CfgTierId] [uniqueidentifier] NULL,
	[PrjMarketId] [uniqueidentifier] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [PrjClause_PK] PRIMARY KEY CLUSTERED ([PKey] ASC)
);
GO

-- ============================================================================
-- Schema de financement saisi sur une offre dans OMRAN (instructions/
-- FEATURE-encaissements-offres-lot2.md A1-A3), absent de la fermeture des cles
-- etrangeres de 010 - ECRIT A LA MAIN, colonnes et types de yesRemsProd.
--
-- StlQuoteDocument : lu par la fiche offre et par le controle VerifSchema,
--   supprime avec l'offre (CASCADE en production, comme ici). Cles etrangeres
--   vers CfgCompany et AdmModule omises.
--
-- Idempotent. A appliquer APRES 070. Aussi applicable tel quel sur yesRemsTest.
-- ============================================================================

IF OBJECT_ID('dbo.StlQuoteDocument', 'U') IS NULL
    CREATE TABLE [dbo].[StlQuoteDocument](
        [PKey] [uniqueidentifier] NOT NULL,
        [Code] [nvarchar](255) COLLATE French_CI_AS NULL,
        [DocDate] [smalldatetime] NULL,
        [Amount] [decimal](26, 6) NULL,
        [ComSettlementCategoryId] [uniqueidentifier] NULL,
        [Description] [nvarchar](255) COLLATE French_CI_AS NULL,
        [StlQuoteId] [uniqueidentifier] NULL,
        [CfgCompanyId] [uniqueidentifier] NULL,
        [AdmModuleId] [uniqueidentifier] NULL,
        [Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
        [UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
        [UserCreateDateTime] [datetime] NULL,
        [UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
        [UserUpdateDateTime] [datetime] NULL,
        [AmountHT] [decimal](26, 6) NULL,
        [AmountTTC] [decimal](26, 6) NULL,
        [VATRatio] [decimal](26, 6) NULL,
        [DiscountRatio] [decimal](26, 6) NULL,
     CONSTRAINT [StlQuoteDocument_PK] PRIMARY KEY CLUSTERED ([PKey] ASC),
     CONSTRAINT [StlQuoteDocument_StlQuote_FK1] FOREIGN KEY ([StlQuoteId]) REFERENCES [dbo].[StlQuote] ([PKey]) ON DELETE CASCADE,
     CONSTRAINT [StlQuoteDocument_ComSettlementCategory_FK1] FOREIGN KEY ([ComSettlementCategoryId]) REFERENCES [dbo].[ComSettlementCategory] ([PKey])
    );
GO

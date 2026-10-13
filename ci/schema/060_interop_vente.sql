-- ============================================================================
-- Tables lues par le code de vente hors fermeture des cles etrangeres des
-- entites JPA, et leur semis - ECRIT A LA MAIN (comme 040 et 050).
--
-- AdmParamsGuid : ReferencesOmran lit le parametre DefaultCurrency (devise des
--   reglements, instructions/FEATURE-interop-omran-ecritures.md, regle 2.4).
-- StlDocumentLine : ControleSortieDossier refuse l'annulation d'un dossier dont
--   une facture n'a pas d'avoir (instructions/FEATURE-vente-socle.md B5) ; lue en
--   SQL natif, aucune entite JPA ne la mappe. Colonnes reduites a celles lues,
--   cles etrangeres omises (StlItem, RntDocument hors perimetre).
--
-- Idempotent. A appliquer APRES 050. Aussi applicable tel quel sur yesRemsTest.
-- ============================================================================

IF OBJECT_ID('dbo.AdmParamsGuid', 'U') IS NULL
    CREATE TABLE [dbo].[AdmParamsGuid](
        [Code] [nvarchar](255) COLLATE French_CI_AS NOT NULL,
        [CodeValue] [uniqueidentifier] NULL,
        [UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
        [UserCreateDateTime] [datetime] NULL,
        [UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
        [UserUpdateDateTime] [datetime] NULL,
        [PKey] [uniqueidentifier] NULL,
        [StrConfigEntityId] [uniqueidentifier] NULL,
     CONSTRAINT [AdmParamsGuid_PK] PRIMARY KEY CLUSTERED ([Code] ASC)
    );
GO

IF OBJECT_ID('dbo.StlDocumentLine', 'U') IS NULL
    CREATE TABLE [dbo].[StlDocumentLine](
        [Pkey] [uniqueidentifier] NOT NULL,
        [Code] [nvarchar](255) COLLATE French_CI_AS NULL,
        [Description] [nvarchar](255) COLLATE French_CI_AS NULL,
        [StlDocumentId] [uniqueidentifier] NULL,
        [ComDocumentId] [uniqueidentifier] NULL,
        [ComFolderId] [uniqueidentifier] NULL,
        [TotalHT] [decimal](26, 6) NULL,
        [TotalVat] [decimal](26, 6) NULL,
        [TotalTTC] [decimal](26, 6) NULL,
     CONSTRAINT [StlDocumentLine_PK] PRIMARY KEY CLUSTERED ([Pkey] ASC)
    );
GO

-- Devise par defaut = MAD, comme en production (PKey semee par 040).
IF NOT EXISTS (SELECT 1 FROM dbo.AdmParamsGuid WHERE Code = 'DefaultCurrency')
    INSERT INTO dbo.AdmParamsGuid (Code, CodeValue, PKey)
    VALUES (N'DefaultCurrency', '24EB36A2-2E81-4A12-AF02-6259CFA25BD6', '5DE193B6-E125-4CF0-A503-19AEBF15849B');
GO

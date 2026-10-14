-- ============================================================================
-- Tables du dossier de vente lues ou ecrites par le lot C (instructions/
-- FEATURE-vente-echeancier.md) et absentes de la fermeture des cles etrangeres
-- de 010 - ECRIT A LA MAIN, colonnes et types de yesRemsProd.
--
-- ComModification, ComGiftReason, ComGift, ComGiftQuantity, ComFolderGift :
--   onglets Modifications et Cadeaux de la fiche dossier.
-- StlPaymentAuthorizationLine : lue en SQL natif pour refuser la suppression
--   d'une echeance en decompte ; colonnes reduites, cles etrangeres vers
--   StlPaymentAuthorization, RntDocument et StlDocument omises.
--
-- Idempotent. A appliquer APRES 060. Aussi applicable tel quel sur yesRemsTest.
-- ============================================================================

IF OBJECT_ID('dbo.ComModification', 'U') IS NULL
    CREATE TABLE [dbo].[ComModification](
        [PKey] [uniqueidentifier] NOT NULL,
        [Description] [nvarchar](255) COLLATE French_CI_AS NULL,
        [DemandDate] [smalldatetime] NULL,
        [TechnicalValidationDate] [smalldatetime] NULL,
        [Cost] [decimal](26, 6) NULL,
        [CustomerValidationDate] [smalldatetime] NULL,
        [CommercialAmount] [decimal](26, 6) NULL,
        [Status] [nvarchar](255) COLLATE French_CI_AS NULL,
        [Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
        [ComFolderId] [uniqueidentifier] NULL,
        [CommercialAmountHT] [decimal](26, 6) NULL,
     CONSTRAINT [ComModification_PK] PRIMARY KEY CLUSTERED ([PKey] ASC),
     CONSTRAINT [ComModification_ComFolder_FK1] FOREIGN KEY ([ComFolderId]) REFERENCES [dbo].[ComFolder] ([PKey])
    );
GO

IF OBJECT_ID('dbo.ComGiftReason', 'U') IS NULL
    CREATE TABLE [dbo].[ComGiftReason](
        [PKey] [uniqueidentifier] NOT NULL,
        [Code] [nvarchar](255) COLLATE French_CI_AS NULL,
        [Description] [nvarchar](255) COLLATE French_CI_AS NULL,
        [UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
        [UserCreateDateTime] [datetime] NULL,
        [UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
        [UserUpdateDateTime] [datetime] NULL,
     CONSTRAINT [ComGiftReason_PK] PRIMARY KEY CLUSTERED ([PKey] ASC)
    );
GO

IF OBJECT_ID('dbo.ComGift', 'U') IS NULL
    CREATE TABLE [dbo].[ComGift](
        [PKey] [uniqueidentifier] NOT NULL,
        [Code] [nvarchar](255) COLLATE French_CI_AS NULL,
        [Description] [nvarchar](255) COLLATE French_CI_AS NULL,
        [Sorting] [int] NULL,
        [UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
        [UserCreateDateTime] [datetime] NULL,
        [UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
        [UserUpdateDateTime] [datetime] NULL,
        [Price] [decimal](26, 6) NULL,
     CONSTRAINT [ComGift_PK] PRIMARY KEY CLUSTERED ([PKey] ASC)
    );
GO

IF OBJECT_ID('dbo.ComGiftQuantity', 'U') IS NULL
    CREATE TABLE [dbo].[ComGiftQuantity](
        [PKey] [uniqueidentifier] NOT NULL,
        [ComGiftId] [uniqueidentifier] NULL,
        [ReceptionDate] [datetime] NULL,
        [Quantity] [int] NULL,
        [UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
        [UserCreateDateTime] [datetime] NULL,
        [UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
        [UserUpdateDateTime] [datetime] NULL,
     CONSTRAINT [ComGiftQuantity_PK] PRIMARY KEY CLUSTERED ([PKey] ASC),
     CONSTRAINT [ComGiftQuantity_ComGift_FK1] FOREIGN KEY ([ComGiftId]) REFERENCES [dbo].[ComGift] ([PKey])
    );
GO

IF OBJECT_ID('dbo.ComFolderGift', 'U') IS NULL
    CREATE TABLE [dbo].[ComFolderGift](
        [PKey] [uniqueidentifier] NOT NULL,
        [ComGiftId] [uniqueidentifier] NULL,
        [ComFolderId] [uniqueidentifier] NOT NULL,
        [Delivered] [bit] NULL,
        [UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
        [UserCreateDateTime] [datetime] NULL,
        [UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
        [UserUpdateDateTime] [datetime] NULL,
        [Price] [decimal](26, 6) NULL,
        [IsAvailable] [bit] NULL,
        [DeliveryDate] [datetime] NULL,
        [Notes] [nvarchar](500) COLLATE French_CI_AS NULL,
        [ComGiftReasonId] [uniqueidentifier] NULL,
        [VoucherDate] [datetime] NULL,
        [ComProspectionId] [uniqueidentifier] NULL,
     CONSTRAINT [ComFolderGift_PK] PRIMARY KEY CLUSTERED ([PKey] ASC),
     CONSTRAINT [ComFolderGift_ComGift_FK1] FOREIGN KEY ([ComGiftId]) REFERENCES [dbo].[ComGift] ([PKey]),
     CONSTRAINT [ComFolderGift_ComFolder_FK1] FOREIGN KEY ([ComFolderId]) REFERENCES [dbo].[ComFolder] ([PKey]),
     CONSTRAINT [ComFolderGift__ComGiftReason_FK1] FOREIGN KEY ([ComGiftReasonId]) REFERENCES [dbo].[ComGiftReason] ([PKey]),
     CONSTRAINT [ComFolderGift__ComProspection_FK1] FOREIGN KEY ([ComProspectionId]) REFERENCES [dbo].[ComProspection] ([PKey])
    );
GO

IF OBJECT_ID('dbo.StlPaymentAuthorizationLine', 'U') IS NULL
    CREATE TABLE [dbo].[StlPaymentAuthorizationLine](
        [PKey] [uniqueidentifier] NOT NULL,
        [Amount] [decimal](26, 6) NULL,
        [ComDocumentId] [uniqueidentifier] NULL,
        [StlPaymentAuthorizationId] [uniqueidentifier] NULL,
        [UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
        [UserCreateDateTime] [datetime] NULL,
        [UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
        [UserUpdateDateTime] [datetime] NULL,
        [RntDocumentId] [uniqueidentifier] NULL,
        [StlDocumentId] [uniqueidentifier] NULL,
     CONSTRAINT [StlPaymentAuthorizationLine_PK] PRIMARY KEY CLUSTERED ([PKey] ASC),
     CONSTRAINT [ComDocument_StlPaymentAuthorizationLine_FK1] FOREIGN KEY ([ComDocumentId]) REFERENCES [dbo].[ComDocument] ([PKey])
    );
GO

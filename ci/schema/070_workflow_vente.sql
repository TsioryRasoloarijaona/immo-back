-- ============================================================================
-- Moteur de workflow des dossiers de vente (instructions/FEATURE-vente-workflow.md)
-- - ECRIT A LA MAIN (comme 040 a 060).
--
-- Tables de parametrage Str* lues par services/dossier/workflow, hors fermeture
-- des cles etrangeres des entites JPA : transitions, intervenants (roles),
-- champs obligatoires, pieces a joindre. Colonnes et types de la production,
-- cles etrangeres omises. Creees VIDES : chaque test pose sa configuration ;
-- une table StrWorkFlow vide pour l'entite fait retomber le service sur la
-- table de transitions de repli (domain/TransitionsValidees).
--
-- Idempotent. A appliquer APRES 060. Aussi applicable tel quel sur yesRemsTest.
-- ============================================================================

-- Deja dans la CI (010) ; absente de yesRemsTest. IsPerson : seuls les tiers personnes physiques
-- ont des champs obligatoires de workflow (Yes.Cfg/CfgTier.cs:VerifMadatoriesForWorkflow).
IF OBJECT_ID('dbo.AdmSocialForm', 'U') IS NULL
    CREATE TABLE [dbo].[AdmSocialForm](
        [PKey] [uniqueidentifier] NOT NULL,
        [Code] [nvarchar](255) NULL,
        [Description] [nvarchar](255) NULL,
        [IsPerson] [bit] NULL,
     CONSTRAINT [AdmSocialForm_PK] PRIMARY KEY CLUSTERED ([PKey] ASC)
    );
GO

-- Droits par role (Yes.Adm) : le seuil de versement n'est verifie que pour les roles qui
-- portent le droit Yes.Com.Front.Sales.Monitoring.toolVerifyThreshold (Sale.cs:VerifThreshold).
IF OBJECT_ID('dbo.AdmRight', 'U') IS NULL
    CREATE TABLE [dbo].[AdmRight](
        [PKey] [uniqueidentifier] NOT NULL,
        [Code] [nvarchar](255) NULL,
        [Description] [nvarchar](255) NULL,
        [AdmRoleId] [uniqueidentifier] NULL,
     CONSTRAINT [AdmRight_PK] PRIMARY KEY CLUSTERED ([PKey] ASC)
    );
GO

IF OBJECT_ID('dbo.StrField', 'U') IS NULL
    CREATE TABLE [dbo].[StrField](
        [PKey] [uniqueidentifier] NOT NULL,
        [Sorting] [int] NULL,
        [Code] [nvarchar](255) NULL,
        [Description] [nvarchar](255) NULL,
        [Notes] [nvarchar](1000) NULL,
        [UserCreate] [nvarchar](255) NULL,
        [UserCreateDateTime] [datetime] NULL,
        [UserUpdate] [nvarchar](255) NULL,
        [UserUpdateDateTime] [datetime] NULL,
        [StrConfigEntityId] [uniqueidentifier] NULL,
     CONSTRAINT [StrField_PK] PRIMARY KEY CLUSTERED ([PKey] ASC)
    );
GO

IF OBJECT_ID('dbo.StrWorkFlow', 'U') IS NULL
    CREATE TABLE [dbo].[StrWorkFlow](
        [PKey] [uniqueidentifier] NOT NULL,
        [StatusFromId] [uniqueidentifier] NULL,
        [StatusToId] [uniqueidentifier] NULL,
        [Sorting] [int] NULL,
        [Code] [nvarchar](255) NULL,
        [Description] [nvarchar](255) NULL,
        [Notes] [nvarchar](1000) NULL,
        [UserCreate] [nvarchar](255) NULL,
        [UserCreateDateTime] [datetime] NULL,
        [UserUpdate] [nvarchar](255) NULL,
        [UserUpdateDateTime] [datetime] NULL,
        [WithComment] [bit] NULL,
        [WithPassword] [bit] NULL,
        [StrFieldDateId] [uniqueidentifier] NULL,
        [WithUserDate] [bit] NULL,
        [WithAutomaticTransition] [bit] NULL,
        [StrConfigEntityId] [uniqueidentifier] NULL,
     CONSTRAINT [StrWorkFlow_PK] PRIMARY KEY CLUSTERED ([PKey] ASC)
    );
GO

IF OBJECT_ID('dbo.StrIntervener', 'U') IS NULL
    CREATE TABLE [dbo].[StrIntervener](
        [PKey] [uniqueidentifier] NOT NULL,
        [Code] [nvarchar](255) NULL,
        [Description] [nvarchar](255) NULL,
        [Notes] [nvarchar](1000) NULL,
        [QueryText] [text] NULL,
        [AdmRoleId] [uniqueidentifier] NULL,
        [AdmUserId] [uniqueidentifier] NULL,
        [UserCreate] [nvarchar](255) NULL,
        [UserCreateDateTime] [datetime] NULL,
        [UserUpdate] [nvarchar](255) NULL,
        [UserUpdateDateTime] [datetime] NULL,
        [StrConfigEntityId] [uniqueidentifier] NULL,
     CONSTRAINT [StrIntervener_PK] PRIMARY KEY CLUSTERED ([PKey] ASC)
    );
GO

IF OBJECT_ID('dbo.StrWorkFlowIntervener', 'U') IS NULL
    CREATE TABLE [dbo].[StrWorkFlowIntervener](
        [PKey] [uniqueidentifier] NOT NULL,
        [StrIntervenerId] [uniqueidentifier] NULL,
        [StrWorkFlowId] [uniqueidentifier] NULL,
        [UserCreate] [nvarchar](255) NULL,
        [UserCreateDateTime] [datetime] NULL,
        [UserUpdate] [nvarchar](255) NULL,
        [UserUpdateDateTime] [datetime] NULL,
        [WithPassword] [bit] NULL,
     CONSTRAINT [StrWorkFlowIntervener_PK] PRIMARY KEY CLUSTERED ([PKey] ASC)
    );
GO

IF OBJECT_ID('dbo.StrStatusIntervener', 'U') IS NULL
    CREATE TABLE [dbo].[StrStatusIntervener](
        [PKey] [uniqueidentifier] NOT NULL,
        [StrIntervenerId] [uniqueidentifier] NULL,
        [StrStatusId] [uniqueidentifier] NULL,
        [UserCreate] [nvarchar](255) NULL,
        [UserCreateDateTime] [datetime] NULL,
        [UserUpdate] [nvarchar](255) NULL,
        [UserUpdateDateTime] [datetime] NULL,
     CONSTRAINT [StrStatusIntervener_PK] PRIMARY KEY CLUSTERED ([PKey] ASC)
    );
GO

IF OBJECT_ID('dbo.StrEntityLinkedEntity', 'U') IS NULL
    CREATE TABLE [dbo].[StrEntityLinkedEntity](
        [PKey] [uniqueidentifier] NOT NULL,
        [Sorting] [int] NULL,
        [Code] [nvarchar](255) NULL,
        [Description] [nvarchar](255) NULL,
        [StrLinkedEntityId] [uniqueidentifier] NULL,
        [LinkPropertyName] [nvarchar](255) NULL,
        [LinkType] [nvarchar](255) NULL,
        [UserCreate] [nvarchar](255) NULL,
        [UserCreateDateTime] [datetime] NULL,
        [UserUpdate] [nvarchar](255) NULL,
        [UserUpdateDateTime] [datetime] NULL,
        [StrConfigEntityId] [uniqueidentifier] NULL,
     CONSTRAINT [StrEntityLinkedEntity_PK] PRIMARY KEY CLUSTERED ([PKey] ASC)
    );
GO

IF OBJECT_ID('dbo.StrWorkFlowField', 'U') IS NULL
    CREATE TABLE [dbo].[StrWorkFlowField](
        [PKey] [uniqueidentifier] NOT NULL,
        [StrFieldId] [uniqueidentifier] NULL,
        [StrWorkFlowId] [uniqueidentifier] NULL,
        [UserCreate] [nvarchar](255) NULL,
        [UserCreateDateTime] [datetime] NULL,
        [UserUpdate] [nvarchar](255) NULL,
        [UserUpdateDateTime] [datetime] NULL,
        [StrEntityLinkedEntityId] [uniqueidentifier] NULL,
     CONSTRAINT [StrWorkFlowField_PK] PRIMARY KEY CLUSTERED ([PKey] ASC)
    );
GO

IF OBJECT_ID('dbo.StrStatusMandatoryField', 'U') IS NULL
    CREATE TABLE [dbo].[StrStatusMandatoryField](
        [PKey] [uniqueidentifier] NOT NULL,
        [StrFieldId] [uniqueidentifier] NULL,
        [StrStatusId] [uniqueidentifier] NULL,
        [StrEntityLinkedEntityId] [uniqueidentifier] NULL,
        [UserCreate] [nvarchar](255) NULL,
        [UserCreateDateTime] [datetime] NULL,
        [UserUpdate] [nvarchar](255) NULL,
        [UserUpdateDateTime] [datetime] NULL,
     CONSTRAINT [StrStatusMandatoryField_PK] PRIMARY KEY CLUSTERED ([PKey] ASC)
    );
GO

IF OBJECT_ID('dbo.StrAttachment', 'U') IS NULL
    CREATE TABLE [dbo].[StrAttachment](
        [PKey] [uniqueidentifier] NOT NULL,
        [UserCreate] [nvarchar](255) NULL,
        [UserCreateDateTime] [datetime] NULL,
        [UserUpdate] [nvarchar](255) NULL,
        [UserUpdateDateTime] [datetime] NULL,
        [AdmAttachedFileTypeId] [uniqueidentifier] NULL,
        [IsMandatory] [bit] NULL,
        [StrConfigEntityId] [uniqueidentifier] NULL,
     CONSTRAINT [StrAttachment_PK] PRIMARY KEY CLUSTERED ([PKey] ASC)
    );
GO

IF OBJECT_ID('dbo.StrStatusAttachment', 'U') IS NULL
    CREATE TABLE [dbo].[StrStatusAttachment](
        [PKey] [uniqueidentifier] NOT NULL,
        [StrAttachmentId] [uniqueidentifier] NULL,
        [StrStatusId] [uniqueidentifier] NULL,
        [IsJustToRequiredToBeChecked] [bit] NULL,
        [IsWithTitle] [bit] NULL,
        [UserCreate] [nvarchar](255) NULL,
        [UserCreateDateTime] [datetime] NULL,
        [UserUpdate] [nvarchar](255) NULL,
        [UserUpdateDateTime] [datetime] NULL,
     CONSTRAINT [StrStatusAttachment_PK] PRIMARY KEY CLUSTERED ([PKey] ASC)
    );
GO

IF OBJECT_ID('dbo.StrWorkFlowAttachment', 'U') IS NULL
    CREATE TABLE [dbo].[StrWorkFlowAttachment](
        [PKey] [uniqueidentifier] NOT NULL,
        [StrAttachmentId] [uniqueidentifier] NULL,
        [StrWorkFlowId] [uniqueidentifier] NULL,
        [UserCreate] [nvarchar](255) NULL,
        [UserCreateDateTime] [datetime] NULL,
        [UserUpdate] [nvarchar](255) NULL,
        [UserUpdateDateTime] [datetime] NULL,
        [IsJustToRequiredToBeChecked] [bit] NULL,
        [IsWithTitle] [bit] NULL,
     CONSTRAINT [StrWorkFlowAttachment_PK] PRIMARY KEY CLUSTERED ([PKey] ASC)
    );
GO

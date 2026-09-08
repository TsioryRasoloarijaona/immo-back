-- ============================================================================
-- Genere depuis yesRemsProd (schema seul, aucune donnee) — NE PAS EDITER A LA MAIN.
-- Regeneration : voir ci/schema/README.md
-- Perimetre : cloture transitive des cles etrangeres des tables mappees par les
-- entites JPA. Les vues, procedures et types table de la base legataire sont
-- volontairement exclus : aucune requete native ni @Subselect ne les utilise.
-- ============================================================================
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ActAccount](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Notes] [nvarchar](500) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[ActAccountTypeId] [uniqueidentifier] NULL,
	[IsTierAccountMandatory] [bit] NULL,
	[StrEntityId] [uniqueidentifier] NULL,
	[CfgCompanyId] [uniqueidentifier] NULL,
	[CfgTierId] [uniqueidentifier] NULL,
	[ComFolderId] [uniqueidentifier] NULL,
	[RntFolderId] [uniqueidentifier] NULL,
	[SynFolderId] [uniqueidentifier] NULL,
 CONSTRAINT [ActAccount_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ActAccountType](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Notes] [nvarchar](500) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[IsActive] [bit] NULL,
	[IsPassive] [bit] NULL,
	[IsProduct] [bit] NULL,
	[IsCharge] [bit] NULL,
 CONSTRAINT [ActAccountType_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ActJournal](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Notes] [nvarchar](500) COLLATE French_CI_AS NULL,
	[ActJournalTypeId] [uniqueidentifier] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[StrEntityId] [uniqueidentifier] NULL,
	[CfgCompanyId] [uniqueidentifier] NULL,
 CONSTRAINT [ActJournal_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ActJournalType](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Notes] [nvarchar](500) COLLATE French_CI_AS NULL,
	[IsActAccountMandatory] [bit] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [ActJournalType_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[AdmAttachedFile](
	[PKey] [uniqueidentifier] ROWGUIDCOL  NOT NULL,
	[Category] [nvarchar](255) COLLATE French_CI_AS NULL,
	[CategoryDescription] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Type] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Extention] [nvarchar](255) COLLATE French_CI_AS NULL,
	[CategoryId] [uniqueidentifier] NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[AdmAttachedFileTypeId] [uniqueidentifier] NULL,
	[FoderGroup] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[OpenBeeDocumentId] [int] NULL,
	[IsCheckedButNotAttached] [bit] NULL,
 CONSTRAINT [PK_AdmAttachedFile] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF),
UNIQUE NONCLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[AdmAttachedFileType](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [AdmAttachedFileType_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[AdmCity](
	[PKey] [uniqueidentifier] NOT NULL,
	[AdmCountryId] [uniqueidentifier] NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [AdmCity_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[AdmCountry](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[ISOAlpha3] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description_an] [nvarchar](255) COLLATE French_CI_AS NULL,
	[ISOName] [nvarchar](255) COLLATE French_CI_AS NULL,
	[NationCode] [nvarchar](255) COLLATE French_CI_AS NULL,
	[PhoneCode] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[PhoneFormat] [nvarchar](255) COLLATE French_CI_AS NULL,
	[WebPhoneFormat] [nvarchar](500) COLLATE French_CI_AS NULL,
 CONSTRAINT [AdmCountry_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[AdmEvent](
	[PKey] [uniqueidentifier] NOT NULL,
	[CfgEmloyeeId] [uniqueidentifier] NULL,
	[StrEntityId] [uniqueidentifier] NULL,
	[AdmEventTypeId] [uniqueidentifier] NULL,
	[ComFolderId] [uniqueidentifier] NULL,
	[Subject] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Location] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Notes] [nvarchar](255) COLLATE French_CI_AS NULL,
	[StartHour] [datetime] NULL,
	[EndHour] [datetime] NULL,
	[AllDay] [bit] NULL,
	[Color] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Status] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[CfgTrancheId] [uniqueidentifier] NULL,
	[EventStatus] [nvarchar](255) COLLATE French_CI_AS NULL,
	[ComActionMessageSubCategoryId] [uniqueidentifier] NULL,
 CONSTRAINT [AdmEvent_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[AdmEventType](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [AdmEventType_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[AdmFamilySituation](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Sorting] [int] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[IsHouseHolder] [bit] NULL,
 CONSTRAINT [AdmFamilySituation_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[AdmIdentityDocumentType](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Sorting] [int] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[IsPerson] [bit] NULL,
 CONSTRAINT [AdmIdentityDocumentType_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[AdmLiability](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Sorting] [int] NULL,
	[IsExonarated] [bit] NULL,
	[IsMajorated] [bit] NULL,
	[MajorationValue] [decimal](26, 6) NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [AdmLiability_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[AdmMatrimonialRegime](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Sorting] [int] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [AdmMatrimonialRegime_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[AdmModule](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Sorting] [int] NULL,
	[ObjectForm] [nvarchar](255) COLLATE French_CI_AS NULL,
	[IsActive] [bit] NULL,
 CONSTRAINT [AdmModule_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[AdmNationality](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [AdmNationality_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[AdmProfession](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[IsWithSalary] [bit] NULL,
 CONSTRAINT [AdmProfession_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[AdmRank](
	[PKey] [uniqueidentifier] NOT NULL,
	[Rank] [int] NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
 CONSTRAINT [AdmRank_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[AdmResidenceType](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Sorting] [int] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [AdmResidenceType_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[AdmRole](
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[PKey] [uniqueidentifier] NOT NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[WithReportRestriction] [bit] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[WithDeleteAttachementRestriction] [bit] NULL,
	[WithAuditRestriction] [bit] NULL,
	[WithSpecificFieldsRestriction] [bit] NULL,
	[WithAddActionMessageRestriction] [bit] NULL,
	[WithEditActionMessageRestriction] [bit] NULL,
	[WithDeleteActionMessageRestriction] [bit] NULL,
	[WithEditAttachementRestriction] [bit] NULL,
	[WithAddAttachementRestriction] [bit] NULL,
	[WithSequenceFreeRestriction] [bit] NULL,
 CONSTRAINT [PK_AdmRole] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[AdmSalary](
	[PKey] [uniqueidentifier] NOT NULL,
	[Sorting] [int] NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [AdmSalary_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[AdmSequence](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[SeqYear] [int] NULL,
	[SeqYearNumber] [int] NULL,
	[SeqValue] [int] NULL,
	[SeqValueNumber] [int] NULL,
	[Separator] [nvarchar](255) COLLATE French_CI_AS NULL,
	[BasedOnSystemYear] [bit] NULL,
	[CfgCompanyId] [uniqueidentifier] NULL,
	[TypeCode] [nvarchar](255) COLLATE French_CI_AS NULL,
	[SeqPrefix] [nvarchar](255) COLLATE French_CI_AS NULL,
	[SeqSuffix] [nvarchar](255) COLLATE French_CI_AS NULL,
 CONSTRAINT [AdmSequence_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[AdmSexType](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Sorting] [int] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [AdmSexType_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[AdmSocialForm](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Sorting] [int] NULL,
	[IsPerson] [bit] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [AdmSocialForm_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[AdmTierTitle](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Sorting] [int] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[AdmSexTypeId] [uniqueidentifier] NULL,
 CONSTRAINT [AdmTierTitle_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[AdmUser](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [varchar](max) COLLATE SQL_Latin1_General_CP1_CS_AS NOT NULL,
	[Pass] [nvarchar](max) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[Email] [nvarchar](max) COLLATE French_CI_AS NULL,
	[Phone] [nvarchar](max) COLLATE French_CI_AS NULL,
	[Mobile] [nvarchar](max) COLLATE French_CI_AS NULL,
	[LinesPerPage] [int] NULL,
	[ConfirmDelete] [bit] NULL,
	[LockDelay] [int] NULL,
	[WebRoleId] [uniqueidentifier] NULL,
	[WebCultureId] [uniqueidentifier] NULL,
	[WebAlertDefinitionId] [uniqueidentifier] NULL,
	[Disabled] [bit] NULL,
	[CfgEmployeeId] [uniqueidentifier] NULL,
	[IsVisibleForChat] [bit] NULL,
	[CanDisapearNotification] [bit] NULL,
	[EmailPassword] [nvarchar](255) COLLATE French_CI_AS NULL,
	[EmailPortNumber] [int] NULL,
	[EmailSMTPServer] [nvarchar](255) COLLATE French_CI_AS NULL,
	[EmailSSLActive] [bit] NULL,
	[OpenBeeCode] [nvarchar](255) COLLATE French_CI_AS NULL,
	[OpenBeePass] [nvarchar](255) COLLATE French_CI_AS NULL,
	[IsMobileUser] [bit] NULL,
	[WebPass] [nvarchar](100) COLLATE French_CI_AS NULL,
 CONSTRAINT [PK_AdmUser] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
ALTER TABLE [dbo].[AdmUser] ADD  CONSTRAINT [DF_AdmUser]  DEFAULT (N'') FOR [Pass]
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[AdmUserHabilitation](
	[PKey] [uniqueidentifier] NOT NULL,
	[AdmUserId] [uniqueidentifier] NULL,
	[CfgCompanyId] [uniqueidentifier] NULL,
	[CfgProjectId] [uniqueidentifier] NULL,
	[CfgTrancheId] [uniqueidentifier] NULL,
	[PrjProjectId] [uniqueidentifier] NULL,
 CONSTRAINT [AdmUserHabilitation_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
CREATE NONCLUSTERED INDEX [_dta_index_AdmUserHabilitation_5_483532806__K2_K5] ON [dbo].[AdmUserHabilitation]
(
	[AdmUserId] ASC,
	[CfgTrancheId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[AdmUserRole](
	[PKey] [uniqueidentifier] NOT NULL,
	[AdmRoleId] [uniqueidentifier] NULL,
	[AdmUserId] [uniqueidentifier] NULL,
 CONSTRAINT [AdmUserRole_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[AdmWebAlertDefinitions](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](max) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](max) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](max) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [PK_dbo.AdmWebAlertDefinitions] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[AdmWebCultures](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](max) COLLATE French_CI_AS NULL,
	[DateExtjsShortFormat] [nvarchar](max) COLLATE French_CI_AS NULL,
	[DateExtjsLongFormat] [nvarchar](max) COLLATE French_CI_AS NULL,
	[DateExtjsAltFormat] [nvarchar](max) COLLATE French_CI_AS NULL,
	[DateShortFormat] [nvarchar](max) COLLATE French_CI_AS NULL,
	[DateLongFormat] [nvarchar](max) COLLATE French_CI_AS NULL,
	[DateSeparator] [nvarchar](max) COLLATE French_CI_AS NULL,
	[NumberNegativeFormat] [int] NULL,
	[NumberGrouping] [int] NULL,
	[NumberDecimalCount] [int] NULL,
	[NumberGroupingSymbol] [nvarchar](max) COLLATE French_CI_AS NULL,
	[NumberDecimalSymbol] [nvarchar](max) COLLATE French_CI_AS NULL,
	[NumberNegativeSymbol] [nvarchar](max) COLLATE French_CI_AS NULL,
	[NumberPosifiveSymbol] [nvarchar](max) COLLATE French_CI_AS NULL,
	[PercentageNegativeFormat] [int] NULL,
	[PercentagePositiveFormat] [int] NULL,
	[PercentageGroupingNumber] [int] NULL,
	[PercentageNumberofDecimal] [int] NULL,
	[PercentageSymbol] [nvarchar](max) COLLATE French_CI_AS NULL,
	[PercentageNumberGroupingSymbol] [nvarchar](max) COLLATE French_CI_AS NULL,
	[PercentageDecimalSymbol] [nvarchar](max) COLLATE French_CI_AS NULL,
	[PercentageThousandSymbol] [nvarchar](max) COLLATE French_CI_AS NULL,
	[TimeExtjsFormat] [nvarchar](max) COLLATE French_CI_AS NULL,
	[TimeShortFormat] [nvarchar](max) COLLATE French_CI_AS NULL,
	[TimeLongFormat] [nvarchar](max) COLLATE French_CI_AS NULL,
	[TimeSeparator] [nvarchar](max) COLLATE French_CI_AS NULL,
	[TimeMorningSymbol] [nvarchar](max) COLLATE French_CI_AS NULL,
	[TimeEveningSymbol] [nvarchar](max) COLLATE French_CI_AS NULL,
	[DeviceNegativeFormat] [int] NULL,
	[DevicePositiveFormat] [int] NULL,
	[DeviseGrouping] [int] NULL,
	[DeviseDecimalCount] [int] NULL,
	[DeviseGroupingSymbol] [nvarchar](max) COLLATE French_CI_AS NULL,
	[DeviseDecimalSymbol] [nvarchar](max) COLLATE French_CI_AS NULL,
	[LanguageId] [uniqueidentifier] NOT NULL,
	[UserCreate] [nvarchar](max) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](max) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [PK_dbo.AdmWebCultures] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
CREATE NONCLUSTERED INDEX [IX_LanguageId] ON [dbo].[AdmWebCultures]
(
	[LanguageId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[AdmWebLanguages](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](max) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](max) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](max) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](max) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [PK_dbo.AdmWebLanguages] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[AdmWorkFlowStateType](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Sorting] [int] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [AdmWorkFlowStateType_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[AppFileMigrationFailure](
	[PKey] [uniqueidentifier] NOT NULL,
	[AdmAttachedFileId] [uniqueidentifier] NOT NULL,
	[Attempt] [int] NOT NULL,
	[ErrorType] [nvarchar](255) COLLATE French_CI_AS NULL,
	[ErrorMessage] [nvarchar](max) COLLATE French_CI_AS NULL,
	[StorageKey] [nvarchar](512) COLLATE French_CI_AS NULL,
	[FailedAt] [datetime2](7) NOT NULL,
 CONSTRAINT [PK_AppFileMigrationFailure] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
CREATE NONCLUSTERED INDEX [IX_AppFileMigrationFailure_AdmAttachedFileId] ON [dbo].[AppFileMigrationFailure]
(
	[AdmAttachedFileId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
ALTER TABLE [dbo].[AppFileMigrationFailure] ADD  CONSTRAINT [DF_AppFileMigrationFailure_PKey]  DEFAULT (newid()) FOR [PKey]
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[AppFileObject](
	[AdmAttachedFileId] [uniqueidentifier] NOT NULL,
	[StorageKey] [nvarchar](512) COLLATE French_CI_AS NOT NULL,
	[ContentType] [nvarchar](255) COLLATE French_CI_AS NULL,
	[SizeBytes] [bigint] NOT NULL,
	[Sha256] [char](64) COLLATE French_CI_AS NULL,
	[UploadedAt] [datetime2](7) NOT NULL,
	[UploadedBy] [nvarchar](255) COLLATE French_CI_AS NULL,
	[DeletedAt] [datetime2](7) NULL,
	[DeletedBy] [nvarchar](255) COLLATE French_CI_AS NULL,
 CONSTRAINT [PK_AppFileObject] PRIMARY KEY CLUSTERED
(
	[AdmAttachedFileId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_PADDING ON

GO
CREATE UNIQUE NONCLUSTERED INDEX [UX_AppFileObject_StorageKey] ON [dbo].[AppFileObject]
(
	[StorageKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, IGNORE_DUP_KEY = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[AppUserCredential](
	[PKey] [uniqueidentifier] NOT NULL,
	[AdmUserId] [uniqueidentifier] NOT NULL,
	[PasswordHash] [nvarchar](255) COLLATE French_CI_AS NOT NULL,
	[MustChangePassword] [bit] NOT NULL,
	[ProfileCompleted] [bit] NOT NULL,
	[TokenVersion] [int] NOT NULL,
	[UserCreateDateTime] [datetime2](7) NOT NULL,
	[UserUpdateDateTime] [datetime2](7) NULL,
 CONSTRAINT [PK_AppUserCredential] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF),
 CONSTRAINT [UQ_AppUserCredential_AdmUserId] UNIQUE NONCLUSTERED 
(
	[AdmUserId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[BuyDocument](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[DocDate] [datetime] NULL,
	[AmountHT] [decimal](26, 6) NULL,
	[AmountVat] [decimal](26, 6) NULL,
	[AmountTTC] [decimal](26, 6) NULL,
	[FiscalStamp] [decimal](26, 6) NULL,
	[AmountToPay] [decimal](26, 6) NULL,
	[MaturityDate] [datetime] NULL,
	[DocType] [nvarchar](255) COLLATE French_CI_AS NULL,
	[CfgTierId] [uniqueidentifier] NULL,
	[IsSelected] [bit] NULL,
	[BuyFolderId] [uniqueidentifier] NULL,
	[DocRef] [nvarchar](255) COLLATE French_CI_AS NULL,
	[IsClosed] [bit] NULL,
	[AmountRegul] [decimal](26, 6) NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[ReferenceType] [nvarchar](255) COLLATE French_CI_AS NULL,
	[PrjMarketId] [uniqueidentifier] NULL,
	[AdvanceRatio] [decimal](26, 6) NULL,
	[AdvanceAmount] [decimal](26, 6) NULL,
	[WarrantyRatio] [decimal](26, 6) NULL,
	[WarrantyAmount] [decimal](26, 6) NULL,
	[ProportionRatio] [decimal](26, 6) NULL,
	[ProportionAmount] [decimal](26, 6) NULL,
	[InsuranceDECRatio] [decimal](26, 6) NULL,
	[InsuranceDECAmount] [decimal](26, 6) NULL,
	[InsuranceTRCRatio] [decimal](26, 6) NULL,
	[InsuranceTRCAmount] [decimal](26, 6) NULL,
	[AdvanceRest] [decimal](26, 6) NULL,
	[InsuranceDECRest] [decimal](26, 6) NULL,
	[InsuranceTRCRest] [decimal](26, 6) NULL,
	[SupplierDescription] [nvarchar](255) COLLATE French_CI_AS NULL,
	[CanBePayed] [bit] NULL,
	[LinesCount] [int] NULL,
	[PrevisionDeliveryDate] [smalldatetime] NULL,
	[ParentDocuments] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[BuyLoanId] [uniqueidentifier] NULL,
	[BuyLoanScheduleId] [uniqueidentifier] NULL,
	[State] [nvarchar](255) COLLATE French_CI_AS NULL,
	[PersonToContact] [nvarchar](255) COLLATE French_CI_AS NULL,
	[StkExpenseId] [uniqueidentifier] NULL,
	[CfgCompanyId] [uniqueidentifier] NULL,
	[AdmModuleId] [uniqueidentifier] NULL,
	[StockAmountToPay] [decimal](26, 6) NULL,
	[WorkingOutAdvanceAmount] [decimal](26, 6) NULL,
	[IsRejected] [bit] NULL,
	[RetainedAmount] [decimal](26, 6) NULL,
	[ProrataRefundAmount] [decimal](26, 6) NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[FluctuationsToPay] [decimal](26, 6) NULL,
	[PrjProjectId] [uniqueidentifier] NULL,
	[BuyDocumentStatusId] [uniqueidentifier] NULL,
	[StatusValidityDate] [datetime] NULL,
	[StrEntityId] [uniqueidentifier] NULL,
	[DefaultRSBasedOnRetainedAmounts] [bit] NULL,
	[CfgTierTriggerId] [uniqueidentifier] NULL,
	[InvoiceSourceId] [uniqueidentifier] NULL,
	[ComFolderId] [uniqueidentifier] NULL,
	[PrjWorkingOutId] [uniqueidentifier] NULL,
	[BuyOrderId] [uniqueidentifier] NULL,
	[orderAdvanceRatio] [decimal](26, 6) NULL,
	[BuyQuoteId] [uniqueidentifier] NULL,
	[BuyInvoiceId] [uniqueidentifier] NULL,
	[AmountToPay_OLD] [decimal](26, 6) NULL,
	[AmountBeforeRS_OLD] [decimal](26, 6) NULL,
	[AmountRS_OLD] [decimal](26, 6) NULL,
	[AmountRSBasedOnAmount_OLD] [bit] NULL,
	[RSRatio_OLD] [decimal](26, 6) NULL,
	[IsFinancialRefund] [bit] NULL,
	[Notes2] [nvarchar](max) COLLATE French_CI_AS NULL,
	[PrjMarketTypeId] [uniqueidentifier] NULL,
	[OfficeOrderRef] [nvarchar](255) COLLATE French_CI_AS NULL,
	[IsPropotionCharged] [bit] NULL,
	[StockAmountToPayHT] [decimal](26, 6) NULL,
	[FluctuationsToPayHT] [decimal](26, 6) NULL,
	[BuyConsultationOfferId] [uniqueidentifier] NULL,
	[IsAccounted] [bit] NULL,
	[StlCurrencyId] [uniqueidentifier] NULL,
	[BuySelectionId] [uniqueidentifier] NULL,
	[RntFolderId] [uniqueidentifier] NULL,
	[SynFolderId] [uniqueidentifier] NULL,
	[PrjClauseId] [uniqueidentifier] NULL,
 CONSTRAINT [BuyDocument_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_10_935674381__K1_2_4_14] ON [dbo].[BuyDocument]
(
	[PKey] ASC
)
INCLUDE([Code],[Description],[AmountToPay]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_10_935674381__K1_K34] ON [dbo].[BuyDocument]
(
	[PKey] ASC,
	[PrjMarketId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_10_935674381__K109_K1_9] ON [dbo].[BuyDocument]
(
	[IsPropotionCharged] ASC,
	[PKey] ASC
)
INCLUDE([AmountTTC]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_10_935674381__K19_K34_K1_44] ON [dbo].[BuyDocument]
(
	[DocType] ASC,
	[PrjMarketId] ASC,
	[PKey] ASC
)
INCLUDE([InsuranceTRCAmount]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_10_935674381__K19_K34_K1_K87_6_9_12_14_26_36_38_40_42_44_70_73_74_96] ON [dbo].[BuyDocument]
(
	[DocType] ASC,
	[PrjMarketId] ASC,
	[PKey] ASC,
	[StrEntityId] ASC
)
INCLUDE([AmountHT],[AmountTTC],[FiscalStamp],[AmountToPay],[AmountRegul],[AdvanceAmount],[WarrantyAmount],[ProportionAmount],[InsuranceDECAmount],[InsuranceTRCAmount],[WorkingOutAdvanceAmount],[RetainedAmount],[ProrataRefundAmount],[orderAdvanceRatio]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_10_935674381__K19_K34_K1_K87_6_9_12_26_36_38_40_42_44_70_73_74] ON [dbo].[BuyDocument]
(
	[DocType] ASC,
	[PrjMarketId] ASC,
	[PKey] ASC,
	[StrEntityId] ASC
)
INCLUDE([AmountHT],[AmountTTC],[FiscalStamp],[AmountRegul],[AdvanceAmount],[WarrantyAmount],[ProportionAmount],[InsuranceDECAmount],[InsuranceTRCAmount],[WorkingOutAdvanceAmount],[RetainedAmount],[ProrataRefundAmount]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_10_935674381__K19_K34_K1_K87_K95_6_9_12_14_26_36_38_40_42_44_70_73_74_96] ON [dbo].[BuyDocument]
(
	[DocType] ASC,
	[PrjMarketId] ASC,
	[PKey] ASC,
	[StrEntityId] ASC,
	[BuyOrderId] ASC
)
INCLUDE([AmountHT],[AmountTTC],[FiscalStamp],[AmountToPay],[AmountRegul],[AdvanceAmount],[WarrantyAmount],[ProportionAmount],[InsuranceDECAmount],[InsuranceTRCAmount],[WorkingOutAdvanceAmount],[RetainedAmount],[ProrataRefundAmount],[orderAdvanceRatio]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_10_935674381__K19_K34_K1_K87_K95_K21_K22_K5_6_9_12_14_26_36_38_40_42_44_69_70_73_74_79_96] ON [dbo].[BuyDocument]
(
	[DocType] ASC,
	[PrjMarketId] ASC,
	[PKey] ASC,
	[StrEntityId] ASC,
	[BuyOrderId] ASC,
	[CfgTierId] ASC,
	[IsSelected] ASC,
	[DocDate] ASC
)
INCLUDE([AmountHT],[AmountTTC],[FiscalStamp],[AmountToPay],[AmountRegul],[AdvanceAmount],[WarrantyAmount],[ProportionAmount],[InsuranceDECAmount],[InsuranceTRCAmount],[StockAmountToPay],[WorkingOutAdvanceAmount],[RetainedAmount],[ProrataRefundAmount],[FluctuationsToPay],[orderAdvanceRatio]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_10_935674381__K19_K34_K1_K87_K95_K21_K22_K5_K109_K80_6_9_12_14_26_33_36_38_40_42_44_69_70_73_74_79_96] ON [dbo].[BuyDocument]
(
	[DocType] ASC,
	[PrjMarketId] ASC,
	[PKey] ASC,
	[StrEntityId] ASC,
	[BuyOrderId] ASC,
	[CfgTierId] ASC,
	[IsSelected] ASC,
	[DocDate] ASC,
	[IsPropotionCharged] ASC,
	[PrjProjectId] ASC
)
INCLUDE([AmountHT],[AmountTTC],[FiscalStamp],[AmountToPay],[AmountRegul],[ReferenceType],[AdvanceAmount],[WarrantyAmount],[ProportionAmount],[InsuranceDECAmount],[InsuranceTRCAmount],[StockAmountToPay],[WorkingOutAdvanceAmount],[RetainedAmount],[ProrataRefundAmount],[FluctuationsToPay],[orderAdvanceRatio]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_10_935674381__K19_K95_6] ON [dbo].[BuyDocument]
(
	[DocType] ASC,
	[BuyOrderId] ASC
)
INCLUDE([AmountHT]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_10_935674381__K19_K95_K34_1_5_6_9_12_14_21_22_26_33_36_38_40_42_44_51_69_70_73_74_79_80_87_96_109] ON [dbo].[BuyDocument]
(
	[DocType] ASC,
	[BuyOrderId] ASC,
	[PrjMarketId] ASC
)
INCLUDE([PKey],[DocDate],[AmountHT],[AmountTTC],[FiscalStamp],[AmountToPay],[CfgTierId],[IsSelected],[AmountRegul],[ReferenceType],[AdvanceAmount],[WarrantyAmount],[ProportionAmount],[InsuranceDECAmount],[InsuranceTRCAmount],[CanBePayed],[StockAmountToPay],[WorkingOutAdvanceAmount],[RetainedAmount],[ProrataRefundAmount],[FluctuationsToPay],[PrjProjectId],[StrEntityId],[orderAdvanceRatio],[IsPropotionCharged]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_10_935674381__K33_K34_9] ON [dbo].[BuyDocument]
(
	[ReferenceType] ASC,
	[PrjMarketId] ASC
)
INCLUDE([AmountTTC]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_10_935674381__K33_K34_K1_5_6_9_12_14_19_21_22_26_36_38_40_42_44_51_69_70_73_74_79_80_87_95_96_109] ON [dbo].[BuyDocument]
(
	[ReferenceType] ASC,
	[PrjMarketId] ASC,
	[PKey] ASC
)
INCLUDE([DocDate],[AmountHT],[AmountTTC],[FiscalStamp],[AmountToPay],[DocType],[CfgTierId],[IsSelected],[AmountRegul],[AdvanceAmount],[WarrantyAmount],[ProportionAmount],[InsuranceDECAmount],[InsuranceTRCAmount],[CanBePayed],[StockAmountToPay],[WorkingOutAdvanceAmount],[RetainedAmount],[ProrataRefundAmount],[FluctuationsToPay],[PrjProjectId],[StrEntityId],[BuyOrderId],[orderAdvanceRatio],[IsPropotionCharged]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_10_935674381__K34_K19_K22_K5_K1_6_9_33] ON [dbo].[BuyDocument]
(
	[PrjMarketId] ASC,
	[DocType] ASC,
	[IsSelected] ASC,
	[DocDate] ASC,
	[PKey] ASC
)
INCLUDE([AmountHT],[AmountTTC],[ReferenceType]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_10_935674381__K34_K19_K5] ON [dbo].[BuyDocument]
(
	[PrjMarketId] ASC,
	[DocType] ASC,
	[DocDate] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_10_935674381__K34_K21_K19_K22_K5_6_9] ON [dbo].[BuyDocument]
(
	[PrjMarketId] ASC,
	[CfgTierId] ASC,
	[DocType] ASC,
	[IsSelected] ASC,
	[DocDate] ASC
)
INCLUDE([AmountHT],[AmountTTC]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_10_935674381__K34_K33_1_5_6_9_12_14_19_21_22_26_36_38_40_42_44_51_69_70_73_74_79_87_95_96] ON [dbo].[BuyDocument]
(
	[PrjMarketId] ASC,
	[ReferenceType] ASC
)
INCLUDE([PKey],[DocDate],[AmountHT],[AmountTTC],[FiscalStamp],[AmountToPay],[DocType],[CfgTierId],[IsSelected],[AmountRegul],[AdvanceAmount],[WarrantyAmount],[ProportionAmount],[InsuranceDECAmount],[InsuranceTRCAmount],[CanBePayed],[StockAmountToPay],[WorkingOutAdvanceAmount],[RetainedAmount],[ProrataRefundAmount],[FluctuationsToPay],[StrEntityId],[BuyOrderId],[orderAdvanceRatio]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_10_935674381__K34_K33_9] ON [dbo].[BuyDocument]
(
	[PrjMarketId] ASC,
	[ReferenceType] ASC
)
INCLUDE([AmountTTC]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_10_935674381__K80_K109_K34_K33_K1_5_6_9_12_14_19_21_22_26_36_38_40_42_44_51_69_70_73_74_79_87_95_96] ON [dbo].[BuyDocument]
(
	[PrjProjectId] ASC,
	[IsPropotionCharged] ASC,
	[PrjMarketId] ASC,
	[ReferenceType] ASC,
	[PKey] ASC
)
INCLUDE([DocDate],[AmountHT],[AmountTTC],[FiscalStamp],[AmountToPay],[DocType],[CfgTierId],[IsSelected],[AmountRegul],[AdvanceAmount],[WarrantyAmount],[ProportionAmount],[InsuranceDECAmount],[InsuranceTRCAmount],[CanBePayed],[StockAmountToPay],[WorkingOutAdvanceAmount],[RetainedAmount],[ProrataRefundAmount],[FluctuationsToPay],[StrEntityId],[BuyOrderId],[orderAdvanceRatio]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_10_935674381__K87_K1_K34_K33_5_6_9_12_14_19_21_22_26_36_38_40_42_44_51_69_70_73_74_79_80_95_96_109] ON [dbo].[BuyDocument]
(
	[StrEntityId] ASC,
	[PKey] ASC,
	[PrjMarketId] ASC,
	[ReferenceType] ASC
)
INCLUDE([DocDate],[AmountHT],[AmountTTC],[FiscalStamp],[AmountToPay],[DocType],[CfgTierId],[IsSelected],[AmountRegul],[AdvanceAmount],[WarrantyAmount],[ProportionAmount],[InsuranceDECAmount],[InsuranceTRCAmount],[CanBePayed],[StockAmountToPay],[WorkingOutAdvanceAmount],[RetainedAmount],[ProrataRefundAmount],[FluctuationsToPay],[PrjProjectId],[BuyOrderId],[orderAdvanceRatio],[IsPropotionCharged]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_10_935674381__K95_K19_9] ON [dbo].[BuyDocument]
(
	[BuyOrderId] ASC,
	[DocType] ASC
)
INCLUDE([AmountTTC]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_10_935674381__K95_K19_K34_1_5_6_9_12_14_21_22_26_33_36_38_40_42_44_51_69_70_73_74_79_80_87_96_109] ON [dbo].[BuyDocument]
(
	[BuyOrderId] ASC,
	[DocType] ASC,
	[PrjMarketId] ASC
)
INCLUDE([PKey],[DocDate],[AmountHT],[AmountTTC],[FiscalStamp],[AmountToPay],[CfgTierId],[IsSelected],[AmountRegul],[ReferenceType],[AdvanceAmount],[WarrantyAmount],[ProportionAmount],[InsuranceDECAmount],[InsuranceTRCAmount],[CanBePayed],[StockAmountToPay],[WorkingOutAdvanceAmount],[RetainedAmount],[ProrataRefundAmount],[FluctuationsToPay],[PrjProjectId],[StrEntityId],[orderAdvanceRatio],[IsPropotionCharged]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_5_1675153013__K1_K93_K80_K34_K56_4_31] ON [dbo].[BuyDocument]
(
	[PKey] ASC,
	[ComFolderId] ASC,
	[PrjProjectId] ASC,
	[PrjMarketId] ASC,
	[BuyLoanScheduleId] ASC
)
INCLUDE([Description],[Notes]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_5_1675153013__K19_K34_36] ON [dbo].[BuyDocument]
(
	[DocType] ASC,
	[PrjMarketId] ASC
)
INCLUDE([AdvanceAmount]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_5_1675153013__K19_K34_38] ON [dbo].[BuyDocument]
(
	[DocType] ASC,
	[PrjMarketId] ASC
)
INCLUDE([WarrantyAmount]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_5_1675153013__K19_K34_44] ON [dbo].[BuyDocument]
(
	[DocType] ASC,
	[PrjMarketId] ASC
)
INCLUDE([InsuranceTRCAmount]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_5_1675153013__K19_K34_6] ON [dbo].[BuyDocument]
(
	[DocType] ASC,
	[PrjMarketId] ASC
)
INCLUDE([AmountHT]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_5_1675153013__K19_K34_8] ON [dbo].[BuyDocument]
(
	[DocType] ASC,
	[PrjMarketId] ASC
)
INCLUDE([AmountVat]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_5_1675153013__K33_K34_1_6_8_14_19_36_38_40_42_44] ON [dbo].[BuyDocument]
(
	[ReferenceType] ASC,
	[PrjMarketId] ASC
)
INCLUDE([PKey],[AmountHT],[AmountVat],[AmountToPay],[DocType],[AdvanceAmount],[WarrantyAmount],[ProportionAmount],[InsuranceDECAmount],[InsuranceTRCAmount]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_5_1675153013__K34_K19_14_33] ON [dbo].[BuyDocument]
(
	[PrjMarketId] ASC,
	[DocType] ASC
)
INCLUDE([AmountToPay],[ReferenceType]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_5_1675153013__K34_K19_33] ON [dbo].[BuyDocument]
(
	[PrjMarketId] ASC,
	[DocType] ASC
)
INCLUDE([ReferenceType]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_5_1675153013__K34_K19_40] ON [dbo].[BuyDocument]
(
	[PrjMarketId] ASC,
	[DocType] ASC
)
INCLUDE([ProportionAmount]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_5_1675153013__K34_K19_42] ON [dbo].[BuyDocument]
(
	[PrjMarketId] ASC,
	[DocType] ASC
)
INCLUDE([InsuranceDECAmount]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_5_1675153013__K34_K33_14_19] ON [dbo].[BuyDocument]
(
	[PrjMarketId] ASC,
	[ReferenceType] ASC
)
INCLUDE([AmountToPay],[DocType]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_5_1675153013__K80_K93_K1] ON [dbo].[BuyDocument]
(
	[PrjProjectId] ASC,
	[ComFolderId] ASC,
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_5_935674381__K56_K1] ON [dbo].[BuyDocument]
(
	[BuyLoanScheduleId] ASC,
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_7_935674381__K1] ON [dbo].[BuyDocument]
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_7_935674381__K1_K51_K80_K66_K56] ON [dbo].[BuyDocument]
(
	[PKey] ASC,
	[CanBePayed] ASC,
	[PrjProjectId] ASC,
	[StkExpenseId] ASC,
	[BuyLoanScheduleId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_7_935674381__K51_K80_K1_K56_K66_K87_K68_K21_K67_K93_K118_4_24_25_29_34_119] ON [dbo].[BuyDocument]
(
	[CanBePayed] ASC,
	[PrjProjectId] ASC,
	[PKey] ASC,
	[BuyLoanScheduleId] ASC,
	[StkExpenseId] ASC,
	[StrEntityId] ASC,
	[AdmModuleId] ASC,
	[CfgTierId] ASC,
	[CfgCompanyId] ASC,
	[ComFolderId] ASC,
	[RntFolderId] ASC
)
INCLUDE([Description],[BuyFolderId],[DocRef],[Notes],[PrjMarketId],[SynFolderId]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_7_935674381__K67_K1_K85_K21_K116_K68_K87_K51_K56_K34_K2_K14_4_5_15_19_25_29_80] ON [dbo].[BuyDocument]
(
	[CfgCompanyId] ASC,
	[PKey] ASC,
	[BuyDocumentStatusId] ASC,
	[CfgTierId] ASC,
	[StlCurrencyId] ASC,
	[AdmModuleId] ASC,
	[StrEntityId] ASC,
	[CanBePayed] ASC,
	[BuyLoanScheduleId] ASC,
	[PrjMarketId] ASC,
	[Code] ASC,
	[AmountToPay] ASC
)
INCLUDE([Description],[DocDate],[MaturityDate],[DocType],[DocRef],[Notes],[PrjProjectId]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocument_7_935674381__K87_K1_K68_K21_K93_K118_4_24_25_29_119] ON [dbo].[BuyDocument]
(
	[StrEntityId] ASC,
	[PKey] ASC,
	[AdmModuleId] ASC,
	[CfgTierId] ASC,
	[ComFolderId] ASC,
	[RntFolderId] ASC
)
INCLUDE([Description],[BuyFolderId],[DocRef],[Notes],[SynFolderId]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
ALTER TABLE [dbo].[BuyDocument] ADD  DEFAULT (0) FOR [AdvanceRatio]
GO
ALTER TABLE [dbo].[BuyDocument] ADD  DEFAULT (0) FOR [AdvanceAmount]
GO
ALTER TABLE [dbo].[BuyDocument] ADD  DEFAULT (0) FOR [WarrantyRatio]
GO
ALTER TABLE [dbo].[BuyDocument] ADD  DEFAULT (0) FOR [WarrantyAmount]
GO
ALTER TABLE [dbo].[BuyDocument] ADD  DEFAULT (0) FOR [ProportionRatio]
GO
ALTER TABLE [dbo].[BuyDocument] ADD  DEFAULT (0) FOR [ProportionAmount]
GO
ALTER TABLE [dbo].[BuyDocument] ADD  DEFAULT (0) FOR [InsuranceDECRatio]
GO
ALTER TABLE [dbo].[BuyDocument] ADD  DEFAULT (0) FOR [InsuranceDECAmount]
GO
ALTER TABLE [dbo].[BuyDocument] ADD  DEFAULT (0) FOR [InsuranceTRCRatio]
GO
ALTER TABLE [dbo].[BuyDocument] ADD  DEFAULT (0) FOR [InsuranceTRCAmount]
GO
ALTER TABLE [dbo].[BuyDocument] ADD  DEFAULT (0) FOR [AdvanceRest]
GO
ALTER TABLE [dbo].[BuyDocument] ADD  DEFAULT (0) FOR [InsuranceDECRest]
GO
ALTER TABLE [dbo].[BuyDocument] ADD  DEFAULT (0) FOR [InsuranceTRCRest]
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[BuyDocumentLine](
	[Pkey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Unity] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Quantity] [decimal](26, 6) NULL,
	[DiscountRatio] [decimal](26, 16) NULL,
	[VatRatio] [decimal](26, 6) NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[BuyDocumentId] [uniqueidentifier] NULL,
	[Sorting] [int] NULL,
	[UnitPriceHT] [decimal](26, 6) NULL,
	[UnitPriceTTC] [decimal](26, 6) NULL,
	[PrjMarketLineId] [uniqueidentifier] NULL,
	[BuyItemId] [uniqueidentifier] NULL,
	[QuantityOld] [decimal](26, 6) NULL,
	[QuantityOrigin] [decimal](26, 6) NULL,
	[Coefficient] [decimal](26, 6) NULL,
	[StlCategoryId] [uniqueidentifier] NULL,
	[FODECRatio] [decimal](26, 16) NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[BuyConsultationLineId] [uniqueidentifier] NULL,
 CONSTRAINT [BuyDocumentLine_PK] PRIMARY KEY CLUSTERED 
(
	[Pkey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyDocumentLine_5_148195578__K12_K26_5_7_8_18_27] ON [dbo].[BuyDocumentLine]
(
	[BuyDocumentId] ASC,
	[StlCategoryId] ASC
)
INCLUDE([Quantity],[DiscountRatio],[VatRatio],[UnitPriceHT],[FODECRatio]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[BuyDocumentStatus](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Sorting] [int] NULL,
	[IsOkForPayement] [bit] NULL,
	[IsValidatedOnAccounting] [bit] NULL,
	[Delay] [int] NULL,
	[MandatoryFields] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[ColorName] [nvarchar](255) COLLATE French_CI_AS NULL,
	[CfgImageId] [uniqueidentifier] NULL,
	[AdmWorkFlowStateTypeId] [uniqueidentifier] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[SupplierMandatoryFields] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[IsReadOnly] [bit] NULL,
 CONSTRAINT [BuyDocumentStatus_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[BuyFolder](
	[Pkey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[IsClosed] [bit] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[Notes] [nvarchar](255) COLLATE French_CI_AS NULL,
 CONSTRAINT [BuyFolder_PK] PRIMARY KEY CLUSTERED 
(
	[Pkey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[BuyItem](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[VatRatio] [decimal](26, 6) NULL,
	[StlCategoryId] [uniqueidentifier] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[Notes] [nvarchar](500) COLLATE French_CI_AS NULL,
	[BuyItemUnderFamilyId] [uniqueidentifier] NULL,
	[Unity] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UnitPriceHT] [decimal](26, 6) NULL,
	[FODECRatio] [decimal](26, 6) NULL,
	[CostLogisticHT] [decimal](26, 6) NULL,
	[CostLogisticVatRatio] [decimal](26, 6) NULL,
	[CostIndirectHT] [decimal](26, 6) NULL,
	[CostIndirectVatRatio] [decimal](26, 6) NULL,
	[CostFinacialHT] [decimal](26, 6) NULL,
	[CostFinacialVatRatio] [decimal](26, 6) NULL,
	[SaleUnitPriceHT] [decimal](26, 6) NULL,
	[SaleVatRatio] [decimal](26, 6) NULL,
 CONSTRAINT [BuyItem_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[BuyItemFamily](
	[Pkey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[VatRatio] [decimal](26, 6) NULL,
	[StlCategoryId] [uniqueidentifier] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[Notes] [nvarchar](500) COLLATE French_CI_AS NULL,
	[Sorting] [int] NULL,
 CONSTRAINT [BuyItemFamily_PK] PRIMARY KEY CLUSTERED 
(
	[Pkey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[BuyItemUnderFamily](
	[Pkey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[VatRatio] [decimal](26, 6) NULL,
	[BuyItemFamilyId] [uniqueidentifier] NULL,
	[StlCategoryId] [uniqueidentifier] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[Notes] [nvarchar](500) COLLATE French_CI_AS NULL,
	[Sorting] [int] NULL,
 CONSTRAINT [BuyItemUnderFamily_PK] PRIMARY KEY CLUSTERED 
(
	[Pkey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[BuyLoan](
	[PKey] [uniqueidentifier] NOT NULL,
	[DocDate] [datetime] NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Amount] [decimal](26, 6) NULL,
	[InterestRate] [decimal](26, 6) NULL,
	[Fees] [decimal](26, 6) NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[CfgTierId] [uniqueidentifier] NULL,
	[PrjProjectId] [uniqueidentifier] NULL,
	[FeesPrevention] [decimal](26, 6) NULL,
	[StkItemId] [uniqueidentifier] NULL,
	[CfgCompanyId] [uniqueidentifier] NULL,
	[BuyLoanTypeId] [uniqueidentifier] NULL,
	[BaseVatRatio] [decimal](26, 6) NULL,
	[CommisionVatRatio] [decimal](26, 6) NULL,
	[InterestVatRatio] [decimal](26, 6) NULL,
	[DefaultCommisionAmount] [decimal](26, 6) NULL,
	[IsClosed] [bit] NULL,
	[StrEntityId] [uniqueidentifier] NULL,
 CONSTRAINT [BuyLoan_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[BuyLoanSchedule](
	[PKey] [uniqueidentifier] NOT NULL,
	[AmountBase] [decimal](26, 6) NULL,
	[InterestRate] [decimal](26, 6) NULL,
	[AmountInterest] [decimal](26, 6) NULL,
	[AmountToPay] [decimal](26, 6) NULL,
	[PaymentDate] [smalldatetime] NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[BuySubLoanId] [uniqueidentifier] NULL,
	[Commision] [decimal](26, 6) NULL,
	[AmountBaseHT] [decimal](26, 6) NULL,
	[BaseVatRatio] [decimal](26, 6) NULL,
	[AmountInterestHT] [decimal](26, 6) NULL,
	[InterestVatRatio] [decimal](26, 6) NULL,
	[CommisionHT] [decimal](26, 6) NULL,
	[CommisionVatRatio] [decimal](26, 6) NULL,
	[AmountToPayHT] [decimal](26, 6) NULL,
 CONSTRAINT [BuyLoanSchedule_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyLoanSchedule_5_336720252__K8_K1_2_3_4_5_6_7_9_10_11_12_13_14_15_16] ON [dbo].[BuyLoanSchedule]
(
	[BuySubLoanId] ASC,
	[PKey] ASC
)
INCLUDE([AmountBase],[InterestRate],[AmountInterest],[AmountToPay],[PaymentDate],[Notes],[Commision],[AmountBaseHT],[BaseVatRatio],[AmountInterestHT],[InterestVatRatio],[CommisionHT],[CommisionVatRatio],[AmountToPayHT]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_BuyLoanSchedule_7_336720252__K1_K8_7] ON [dbo].[BuyLoanSchedule]
(
	[PKey] ASC,
	[BuySubLoanId] ASC
)
INCLUDE([Notes]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[BuyLoanType](
	[PKey] [uniqueidentifier] NOT NULL,
	[Sorting] [int] NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[IsSupplierFinancing] [bit] NULL,
	[IsLeasing] [bit] NULL,
	[BaseStlCategoryId] [uniqueidentifier] NULL,
	[InterestStlCategoryId] [uniqueidentifier] NULL,
	[CommissionStlCategoryId] [uniqueidentifier] NULL,
 CONSTRAINT [BuyLoanType_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[BuySelection](
	[PKey] [uniqueidentifier] NOT NULL,
	[Sorting] [int] NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[DescriptionLong] [nvarchar](500) COLLATE French_CI_AS NULL,
	[DefaultVat] [decimal](26, 6) NULL,
	[PriceEstimatedHT] [decimal](26, 6) NULL,
	[PriceEstimatedTTC] [decimal](26, 6) NULL,
	[DocDate] [datetime] NULL,
	[LaunchOfTenderDate] [datetime] NULL,
	[WithdrawalTenderDocumentsDate] [datetime] NULL,
	[TenderMaturityDate] [datetime] NULL,
	[TendersResortDate] [datetime] NULL,
	[BidOpeningDate] [datetime] NULL,
	[TendersReportDate] [datetime] NULL,
	[TenderDate] [datetime] NULL,
	[ContractSignatureDate] [datetime] NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[TextLetterSelection] [nvarchar](max) COLLATE French_CI_AS NULL,
	[IsTheLast] [bit] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[StrEntityId] [uniqueidentifier] NULL,
	[StrStatusId] [uniqueidentifier] NULL,
	[CfgCompanyId] [uniqueidentifier] NULL,
	[StlCurrencyId] [uniqueidentifier] NULL,
	[PrjProjectId] [uniqueidentifier] NULL,
	[PrjMarketId] [uniqueidentifier] NULL,
	[PrjMarketTypeId] [uniqueidentifier] NULL,
	[StlCategoryId] [uniqueidentifier] NULL,
	[BuySelectionKindId] [uniqueidentifier] NULL,
	[CfgTierId] [uniqueidentifier] NULL,
	[GroupId] [uniqueidentifier] NULL,
	[StlSettlementTypeId] [uniqueidentifier] NULL,
	[PaymentDelay] [int] NULL,
	[AdmModuleId] [uniqueidentifier] NULL,
	[CfgTierTriggerId] [uniqueidentifier] NULL,
 CONSTRAINT [BuySelection_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[BuySelectionKind](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](500) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](500) COLLATE French_CI_AS NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[MaxAmount] [decimal](26, 6) NULL,
	[IsConfidential] [bit] NULL,
	[StrEntityManagmentId] [uniqueidentifier] NULL,
	[IsMultiSupplier] [bit] NULL,
	[IsWithoutVerification] [bit] NULL,
 CONSTRAINT [BuySelectionKind_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[BuySubLoan](
	[PKey] [uniqueidentifier] NOT NULL,
	[Amount] [decimal](26, 6) NULL,
	[PaymentDate] [smalldatetime] NULL,
	[MonthsCount] [int] NULL,
	[GracePeriod] [int] NULL,
	[MonthsStep] [int] NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[BuyLoanId] [uniqueidentifier] NULL,
	[AnnualRate] [decimal](26, 6) NULL,
	[AmortizationDeferredDelay] [int] NULL,
	[TotalDeferredDelay] [int] NULL,
	[IsMensualRate] [bit] NULL,
	[IsActoriel] [bit] NULL,
 CONSTRAINT [BuySubLoan_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[CfgCompany](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[FiscalNumber] [nvarchar](255) COLLATE French_CI_AS NULL,
	[CommercialRegister] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Address] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Email] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Mobile] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Phone] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Fax] [nvarchar](255) COLLATE French_CI_AS NULL,
	[WebSite] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Address2] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Address3] [nvarchar](255) COLLATE French_CI_AS NULL,
	[ZipCode] [nvarchar](255) COLLATE French_CI_AS NULL,
	[City] [nvarchar](255) COLLATE French_CI_AS NULL,
	[SignatureCity] [nvarchar](255) COLLATE French_CI_AS NULL,
	[LegalForm] [nvarchar](255) COLLATE French_CI_AS NULL,
	[CreationDate] [datetime] NULL,
	[Capital] [decimal](26, 6) NULL,
	[Cnss] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Patent] [nvarchar](255) COLLATE French_CI_AS NULL,
	[CfgTierId] [uniqueidentifier] NULL,
	[AdmCountryId] [uniqueidentifier] NULL,
	[AdmSignatoryId] [uniqueidentifier] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[TotalSharing] [int] NULL,
	[CfgTechnicalManagerId] [uniqueidentifier] NULL,
	[CfgSalesManagerId] [uniqueidentifier] NULL,
	[DescriptionLong] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[ICE] [nvarchar](255) COLLATE French_CI_AS NULL,
	[SharingNumber] [int] NULL,
	[LiquidationDate] [datetime] NULL,
	[IsActive] [bit] NULL,
	[HomePart] [decimal](26, 16) NULL,
	[CfgGroupShareholderId] [uniqueidentifier] NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[StlCurrencyId] [uniqueidentifier] NULL,
 CONSTRAINT [CfgCompany_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[CfgGroupShareholder](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [CfgGroupShareholder_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[CfgImage](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[ImageData] [image] NULL,
 CONSTRAINT [CfgImage_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[CfgProject](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Address] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Email] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Mobile] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Phone] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Fax] [nvarchar](255) COLLATE French_CI_AS NULL,
	[WebSite] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Address2] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Address3] [nvarchar](255) COLLATE French_CI_AS NULL,
	[ZipCode] [nvarchar](255) COLLATE French_CI_AS NULL,
	[City] [nvarchar](255) COLLATE French_CI_AS NULL,
	[SignatureCity] [nvarchar](255) COLLATE French_CI_AS NULL,
	[AdmCountryId] [uniqueidentifier] NULL,
	[AdmSignatoryId] [uniqueidentifier] NULL,
	[CfgCompanyId] [uniqueidentifier] NULL,
	[WorkStartDate] [datetime] NULL,
	[WorkEndDate] [datetime] NULL,
	[MarketingStartDate] [datetime] NULL,
	[MarketingEndDate] [datetime] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[CfgTechnicalManagerId] [uniqueidentifier] NULL,
	[CfgSalesManagerId] [uniqueidentifier] NULL,
	[DescriptionLong] [nvarchar](500) COLLATE French_CI_AS NULL,
 CONSTRAINT [CfgProject_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[CfgProjectZone](
	[PKey] [uniqueidentifier] NOT NULL,
	[Leveling] [int] NULL,
	[Sorting] [int] NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[CfgProjectId] [uniqueidentifier] NULL,
	[ParentId] [uniqueidentifier] NULL,
	[ImageData] [image] NULL,
 CONSTRAINT [CfgProjectZone_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[CfgRelationType](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [CfgRelationType_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[CfgTier](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Phone] [nvarchar](30) COLLATE French_CI_AS NULL,
	[Phone1] [nvarchar](30) COLLATE French_CI_AS NULL,
	[Phone2] [nvarchar](30) COLLATE French_CI_AS NULL,
	[Fax] [nvarchar](30) COLLATE French_CI_AS NULL,
	[Mobile] [nvarchar](30) COLLATE French_CI_AS NULL,
	[EMail] [nvarchar](100) COLLATE French_CI_AS NULL,
	[WebSite] [nvarchar](100) COLLATE French_CI_AS NULL,
	[SocialReason] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[BankAgency] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Rib] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Iban] [nvarchar](255) COLLATE French_CI_AS NULL,
	[FiscalCode] [nvarchar](255) COLLATE French_CI_AS NULL,
	[FiscalRegistry] [nvarchar](255) COLLATE French_CI_AS NULL,
	[FiscalDouane] [nvarchar](255) COLLATE French_CI_AS NULL,
	[FirstName] [nvarchar](255) COLLATE French_CI_AS NULL,
	[LastName] [nvarchar](255) COLLATE French_CI_AS NULL,
	[BirthPlace] [nvarchar](255) COLLATE French_CI_AS NULL,
	[BirthDate] [smalldatetime] NULL,
	[NbrChildren] [int] NULL,
	[Asset] [decimal](26, 6) NULL,
	[Quality] [nvarchar](100) COLLATE French_CI_AS NULL,
	[IDNumber] [nvarchar](255) COLLATE French_CI_AS NULL,
	[IDDate] [datetime] NULL,
	[IDPlace] [nvarchar](255) COLLATE French_CI_AS NULL,
	[IDValidateDate] [datetime] NULL,
	[IDValidatePlace] [nvarchar](255) COLLATE French_CI_AS NULL,
	[IsProspect] [bit] NULL,
	[CfgBankId] [uniqueidentifier] NULL,
	[CfgCommercialId] [uniqueidentifier] NULL,
	[CfgContactId] [uniqueidentifier] NULL,
	[CodeCountable] [nvarchar](255) COLLATE French_CI_AS NULL,
	[CfgTierTypeId] [uniqueidentifier] NULL,
	[CfgTierKindId] [uniqueidentifier] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[AdmTierTitleId] [uniqueidentifier] NULL,
	[FatherName] [nvarchar](255) COLLATE French_CI_AS NULL,
	[MotherName] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Employer] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Address] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Address2] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Address3] [nvarchar](255) COLLATE French_CI_AS NULL,
	[ZipCode] [nvarchar](255) COLLATE French_CI_AS NULL,
	[City] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Antiquity] [int] NULL,
	[AdmCountryId] [uniqueidentifier] NULL,
	[AdmCountryResidenceId] [uniqueidentifier] NULL,
	[AdmSocialFormId] [uniqueidentifier] NULL,
	[AdmLiabilityId] [uniqueidentifier] NULL,
	[AdmFamilySituationId] [uniqueidentifier] NULL,
	[AdmResidenceTypeId] [uniqueidentifier] NULL,
	[AdmMatrimonialRegimeId] [uniqueidentifier] NULL,
	[AdmIdentityDocumentTypeId] [uniqueidentifier] NULL,
	[AdmSexTypeId] [uniqueidentifier] NULL,
	[AdmNationalityId] [uniqueidentifier] NULL,
	[AdmProfessionId] [uniqueidentifier] NULL,
	[AdmSalaryId] [uniqueidentifier] NULL,
	[MobileIndicatif] [nvarchar](10) COLLATE French_CI_AS NULL,
	[PhoneIndicatif] [nvarchar](10) COLLATE French_CI_AS NULL,
	[Phone1Indicatif] [nvarchar](10) COLLATE French_CI_AS NULL,
	[Phone2Indicatif] [nvarchar](10) COLLATE French_CI_AS NULL,
	[FaxIndicatif] [nvarchar](10) COLLATE French_CI_AS NULL,
	[AdmCityId] [uniqueidentifier] NULL,
	[CodeCountableCollective] [nvarchar](255) COLLATE French_CI_AS NULL,
	[AdmRankId] [uniqueidentifier] NULL,
	[IsOldCustomer] [bit] NULL,
	[IsExtern] [bit] NULL,
	[ICE] [nvarchar](255) COLLATE French_CI_AS NULL,
	[IsRntCustomer] [bit] NULL,
	[IsVisibleInList] [bit] NULL,
	[ActAccountId] [uniqueidentifier] NULL,
	[ActJournalId] [uniqueidentifier] NULL,
	[GrhDepartmentId] [uniqueidentifier] NULL,
	[GrhEmployeeCategoryId] [uniqueidentifier] NULL,
	[GrhEchelonId] [uniqueidentifier] NULL,
	[GrhAgreementTypeId] [uniqueidentifier] NULL,
	[SocialSecurityNumber] [nvarchar](255) COLLATE French_CI_AS NULL,
	[CfgCompanyId] [uniqueidentifier] NULL,
	[GrhPaySlipModelId] [uniqueidentifier] NULL,
	[HireDate] [datetime] NULL,
	[AgreementDate] [datetime] NULL,
	[FireDate] [datetime] NULL,
	[NbrChildrenHandicap] [int] NULL,
	[SalaryBase] [decimal](26, 6) NULL,
	[BaseMonthNumber] [decimal](26, 6) NULL,
	[BaseWorkedDaysNumber] [decimal](26, 6) NULL,
	[BaseVacationDaysNumber] [decimal](26, 6) NULL,
	[StlCurrencyId] [uniqueidentifier] NULL,
	[RsRatio] [decimal](26, 6) NULL,
	[IsFundReader] [bit] NULL,
	[VATRatio] [decimal](26, 6) NULL,
	[PaymentDelay] [int] NULL,
	[GrhEmployeeClassId] [uniqueidentifier] NULL,
	[GrhEmployeeTypeId] [uniqueidentifier] NULL,
	[StlDeductionTypeId] [uniqueidentifier] NULL,
 CONSTRAINT [CfgTier_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_CfgTier_5_866102126__K10_K27_K1_28_83] ON [dbo].[CfgTier]
(
	[EMail] ASC,
	[FirstName] ASC,
	[PKey] ASC
)
INCLUDE([LastName],[CfgTierTypeId]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_CfgTier_5_930102354__K1_2_4] ON [dbo].[CfgTier]
(
	[PKey] ASC
)
INCLUDE([Code],[Description]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_CfgTier_5_930102354__K1_4_5_9] ON [dbo].[CfgTier]
(
	[PKey] ASC
)
INCLUDE([Description],[Phone],[Mobile]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_CfgTier_5_930102354__K1_K4] ON [dbo].[CfgTier]
(
	[PKey] ASC,
	[Description] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_CfgTier_5_930102354__K1_K84_4] ON [dbo].[CfgTier]
(
	[PKey] ASC,
	[AdmTierTitleId] ASC
)
INCLUDE([Description]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_CfgTier_5_930102354__K78_K1_K4] ON [dbo].[CfgTier]
(
	[CfgTierTypeId] ASC,
	[PKey] ASC,
	[Description] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_CfgTier_5_930102354__K78_K112_1_2_4_5_6_7_8_9_10_12_13_14_18_19_20_21_22_23_24_25_39_40_48_49_51_52_53_66_67_69_74_] ON [dbo].[CfgTier]
(
	[CfgTierTypeId] ASC,
	[AdmCityId] ASC
)
INCLUDE([PKey],[Code],[Description],[Phone],[Phone1],[Phone2],[Fax],[Mobile],[EMail],[WebSite],[SocialReason],[Notes],[BankAgency],[Rib],[Iban],[FiscalCode],[FiscalRegistry],[FiscalDouane],[FirstName],[LastName],[BirthPlace],[BirthDate],[Asset],[Quality],[IDNumber],[IDDate],[IDPlace],[IDValidateDate],[IDValidatePlace],[IsProspect],[CfgBankId],[CfgCommercialId],[CfgContactId],[CodeCountable],[CfgTierKindId],[UserCreate],[UserCreateDateTime],[UserUpdate],[UserUpdateDateTime],[AdmTierTitleId],[FatherName],[MotherName],[Employer],[Address],[Address2],[Address3],[ZipCode],[Antiquity],[AdmCountryId],[AdmCountryResidenceId],[AdmSocialFormId],[AdmLiabilityId],[AdmFamilySituationId],[AdmResidenceTypeId],[AdmMatrimonialRegimeId],[AdmIdentityDocumentTypeId],[AdmSexTypeId],[AdmNationalityId],[AdmProfessionId],[AdmSalaryId],[MobileIndicatif],[PhoneIndicatif],[Phone1Indicatif],[Phone2Indicatif],[FaxIndicatif],[IsOldCustomer]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_CfgTier_6_866102126__K1_4_5_9_13_27_28_56_112_113] ON [dbo].[CfgTier]
(
	[PKey] ASC
)
INCLUDE([Description],[Phone],[Mobile],[SocialReason],[FirstName],[LastName],[IDNumber],[MobileIndicatif],[PhoneIndicatif]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_CfgTier_6_866102126__K1_K2_4] ON [dbo].[CfgTier]
(
	[PKey] ASC,
	[Code] ASC
)
INCLUDE([Description]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_CfgTier_7_866102126__K1_2_4_13] ON [dbo].[CfgTier]
(
	[PKey] ASC
)
INCLUDE([Code],[Description],[SocialReason]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_CfgTier_7_930102354__K1_4_5_9_13_24_25_51_107_108] ON [dbo].[CfgTier]
(
	[PKey] ASC
)
INCLUDE([Description],[Phone],[Mobile],[SocialReason],[FirstName],[LastName],[IDNumber],[MobileIndicatif],[PhoneIndicatif]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
ALTER TABLE [dbo].[CfgTier] ADD  DEFAULT (0) FOR [NbrChildren]
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[CfgTierContact](
	[PKey] [uniqueidentifier] NOT NULL,
	[CfgTierId] [uniqueidentifier] NULL,
	[CfgContactId] [uniqueidentifier] NULL,
	[CfgRelationTypeId] [uniqueidentifier] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [CfgTierContact_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[CfgTierKind](
	[PKey] [uniqueidentifier] NOT NULL,
	[Sorting] [int] NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[CfgTierTypeId] [uniqueidentifier] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[CfgCompanyId] [uniqueidentifier] NULL,
 CONSTRAINT [CfgTierKind_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[CfgTierType](
	[PKey] [uniqueidentifier] NOT NULL,
	[Sorting] [int] NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [CfgTierType_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[CfgTranche](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Address] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Email] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Mobile] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Phone] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Fax] [nvarchar](255) COLLATE French_CI_AS NULL,
	[WebSite] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Address2] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Address3] [nvarchar](255) COLLATE French_CI_AS NULL,
	[ZipCode] [nvarchar](255) COLLATE French_CI_AS NULL,
	[City] [nvarchar](255) COLLATE French_CI_AS NULL,
	[CfgProjectId] [uniqueidentifier] NULL,
	[AdmCountryId] [uniqueidentifier] NULL,
	[WorkStartDate] [datetime] NULL,
	[WorkEndDate] [datetime] NULL,
	[MarketingStartDate] [datetime] NULL,
	[MarketingEndDate] [datetime] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[AdvancePercent] [decimal](26, 6) NULL,
	[ConcretisationPercent] [decimal](26, 6) NULL,
	[EcarttypeFinalisation] [decimal](26, 6) NULL,
	[EcarttypeConcretisation] [decimal](26, 6) NULL,
	[EndDateConcretisation] [datetime] NULL,
	[StartDateFinalisation] [datetime] NULL,
	[DeliveryDate] [datetime] NULL,
	[StkLevelingId] [uniqueidentifier] NULL,
	[IsForSale] [bit] NULL,
	[IsForRent] [bit] NULL,
	[IsForProject] [bit] NULL,
	[IsForBuy] [bit] NULL,
	[IsForSav] [bit] NULL,
	[OccupancyPermitsDate] [datetime] NULL,
	[BeforeReservationAmount] [decimal](26, 6) NULL,
	[SyndicAmount] [decimal](26, 6) NULL,
	[SyndicPeriod] [int] NULL,
	[WarrantyDelayAdded] [int] NULL,
	[WarrantyDelay] [int] NULL,
	[AgreementFirstDelay] [int] NULL,
	[AgreementSecondDelay] [int] NULL,
	[PaymentBalanceDelay] [int] NULL,
	[BeforeReservationAmountDiscount] [decimal](26, 6) NULL,
	[AdvancePercentDiscount] [decimal](26, 6) NULL,
	[DefaultExpenseAmountDiscount] [decimal](26, 6) NULL,
	[DefaultExpenseAmount] [decimal](26, 6) NULL,
	[CancellationPenalityPercent] [decimal](26, 6) NULL,
	[CancellationPenalityPercentDiscount] [decimal](26, 6) NULL,
	[ArticleTransferPenality] [decimal](26, 6) NULL,
	[ArticleTransferPenalityDiscount] [decimal](26, 6) NULL,
	[SliceTransferPenality] [decimal](26, 6) NULL,
	[SliceTransferPenalityDiscount] [decimal](26, 6) NULL,
	[CustomerTransferPenality] [decimal](26, 6) NULL,
	[CustomerTransferPenalityDiscount] [decimal](26, 6) NULL,
	[SyndicAmountDiscount] [decimal](26, 6) NULL,
	[OtherExpenseAmount] [decimal](26, 6) NULL,
	[OtherExpenseAmountDiscount] [decimal](26, 6) NULL,
	[CancellationPenalityBasedOnItemPrice] [bit] NULL,
	[AgreementFirstPenaltyPercent] [decimal](26, 6) NULL,
	[AgreementSecondPenaltyPercent] [decimal](26, 6) NULL,
	[CommerceTribunal] [nvarchar](255) COLLATE French_CI_AS NULL,
	[CancellationRightCode] [nvarchar](255) COLLATE French_CI_AS NULL,
	[CfgClaimsManagerId] [uniqueidentifier] NULL,
	[CountItemBasePrevision] [int] NULL,
	[TotalSaleBasePrevision] [decimal](26, 6) NULL,
	[BaseMarketingStartDate] [datetime] NULL,
	[BaseEndDateConcretisation] [datetime] NULL,
	[BaseStartDateFinalisation] [datetime] NULL,
	[BaseMarketingEndDate] [datetime] NULL,
	[BaseEcarttypeConcretisation] [decimal](26, 6) NULL,
	[BaseEcarttypeFinalisation] [decimal](26, 6) NULL,
	[BaseConcretisationPercent] [decimal](26, 6) NULL,
	[DescriptionLong] [nvarchar](500) COLLATE French_CI_AS NULL,
	[Revival1Date] [datetime] NULL,
	[PrevisionMarketingStartDate] [datetime] NULL,
	[PrevisionEndDateConcretisation] [datetime] NULL,
	[PrevisionStartDateFinalisation] [datetime] NULL,
	[PrevisionMarketingEndDate] [datetime] NULL,
	[DelayLawyerMinute] [int] NULL,
	[DelayMinuteAllSettled] [int] NULL,
	[UnitConcretisation] [decimal](26, 6) NULL,
	[UnitFinalisation] [decimal](26, 6) NULL,
	[UnitTurnover] [decimal](26, 6) NULL,
	[CfgTierOtherId] [uniqueidentifier] NULL,
	[CfgTrancheStandingId] [uniqueidentifier] NULL,
	[LndLevelId] [uniqueidentifier] NULL,
	[CoveredFloorArea] [decimal](26, 6) NULL,
	[SavAddress] [nvarchar](255) COLLATE French_CI_AS NULL,
	[IsWithGiftForProposer] [bit] NULL,
	[IsWithGiftForOldCustomer] [bit] NULL,
	[InterventionMinimumDelai] [int] NULL,
	[MaxVisitNumber] [int] NULL,
	[IsWithGiftForProspectionOrigin] [bit] NULL,
	[AccumulatedGifts] [bit] NULL,
	[IsActiveAdvancePercentDiscount] [bit] NULL,
	[ReceptionDate] [datetime] NULL,
	[ActAccountId] [uniqueidentifier] NULL,
	[WithdrawalDelay] [int] NULL,
	[AdvanceAmount] [decimal](26, 6) NULL,
	[AdvanceBasedOnAmount] [bit] NULL,
	[IsForPrm] [bit] NULL,
	[PrjProjectId] [uniqueidentifier] NULL,
	[IsForRom] [bit] NULL,
	[StkTrancheHeadingId] [uniqueidentifier] NULL,
	[RaisedHandDateAFH] [datetime] NULL,
	[TitleDate1] [datetime] NULL,
	[RaisedHandDateBank] [datetime] NULL,
	[TitleDate2] [datetime] NULL,
	[MunicipalityDateDeposit] [datetime] NULL,
	[MunicipalityDateWithdrawal] [datetime] NULL,
	[OTCDateNonApprovedDeposit] [datetime] NULL,
	[OTCDateSettlement] [datetime] NULL,
	[OTCDateApprovedDeposit] [datetime] NULL,
	[OTCDateMutationWithdrawal] [datetime] NULL,
	[SharingDate] [datetime] NULL,
	[SharingDateDeposit] [datetime] NULL,
	[TitleDate] [datetime] NULL,
	[TitleDateVerification] [datetime] NULL,
	[RepartitionDate] [datetime] NULL,
	[UnitTurnoverHT] [decimal](26, 6) NULL,
	[CPFSharingSettlementDate] [datetime] NULL,
	[CPFSharingWithdrawalDate] [datetime] NULL,
	[IsWithVerifMaxEcheanceDelai] [bit] NULL,
	[MaxEcheanceDelai] [int] NULL,
	[IsWithVerifCredit] [bit] NULL,
	[CreditPercent] [decimal](26, 6) NULL,
 CONSTRAINT [CfgTranche_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_CfgTranche_5_1081770911__K1_K14_K3] ON [dbo].[CfgTranche]
(
	[PKey] ASC,
	[CfgProjectId] ASC,
	[Description] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_CfgTranche_5_1081770911__K3_K33_1_2_4_5_6_7_8_9_10_11_12_13_14_15_16_17_18_19_20_21_22_23_24_25_26_27_28_29_30_31_] ON [dbo].[CfgTranche]
(
	[Description] ASC,
	[IsForRent] ASC
)
INCLUDE([PKey],[Code],[Address],[Email],[Mobile],[Phone],[Fax],[WebSite],[Address2],[Address3],[ZipCode],[City],[CfgProjectId],[AdmCountryId],[WorkStartDate],[WorkEndDate],[MarketingStartDate],[MarketingEndDate],[UserCreate],[UserCreateDateTime],[UserUpdate],[UserUpdateDateTime],[AdvancePercent],[ConcretisationPercent],[EcarttypeFinalisation],[EcarttypeConcretisation],[EndDateConcretisation],[StartDateFinalisation],[DeliveryDate],[StkLevelingId],[IsForSale],[IsForProject],[IsForBuy],[IsForSav],[OccupancyPermitsDate],[BeforeReservationAmount],[SyndicAmount],[SyndicPeriod],[WarrantyDelayAdded],[WarrantyDelay],[AgreementFirstDelay],[AgreementSecondDelay],[PaymentBalanceDelay],[BeforeReservationAmountDiscount],[AdvancePercentDiscount],[DefaultExpenseAmountDiscount],[DefaultExpenseAmount],[CancellationPenalityPercent],[CancellationPenalityPercentDiscount],[ArticleTransferPenality],[ArticleTransferPenalityDiscount],[SliceTransferPenality],[SliceTransferPenalityDiscount],[CustomerTransferPenality],[CustomerTransferPenalityDiscount],[SyndicAmountDiscount],[OtherExpenseAmount],[OtherExpenseAmountDiscount],[CancellationPenalityBasedOnItemPrice],[AgreementFirstPenaltyPercent],[AgreementSecondPenaltyPercent],[CommerceTribunal],[CancellationRightCode],[CfgClaimsManagerId],[BaseMarketingStartDate],[BaseEndDateConcretisation],[BaseStartDateFinalisation],[BaseMarketingEndDate],[BaseEcarttypeConcretisation],[BaseEcarttypeFinalisation],[BaseConcretisationPercent],[DescriptionLong],[Revival1Date],[PrevisionMarketingStartDate],[PrevisionEndDateConcretisation],[PrevisionStartDateFinalisation],[PrevisionMarketingEndDate],[DelayLawyerMinute],[DelayMinuteAllSettled],[CfgTierOtherId],[CfgTrancheStandingId],[LndLevelId],[CoveredFloorArea],[SavAddress],[IsWithGiftForProposer],[IsWithGiftForOldCustomer],[InterventionMinimumDelai],[MaxVisitNumber],[IsWithGiftForProspectionOrigin],[AccumulatedGifts],[IsActiveAdvancePercentDiscount],[ReceptionDate],[WithdrawalDelay],[AdvanceAmount],[AdvanceBasedOnAmount]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_CfgTranche_7_422292564__K1_K14_K114_3] ON [dbo].[CfgTranche]
(
	[PKey] ASC,
	[CfgProjectId] ASC,
	[PrjProjectId] ASC
)
INCLUDE([Description]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[CfgTrancheLevel](
	[PKey] [uniqueidentifier] NOT NULL,
	[CfgTrancheId] [uniqueidentifier] NULL,
	[LndLevelId] [uniqueidentifier] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [CfgTrancheLevel_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[CfgTrancheStanding](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [CfgTrancheStanding_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[CfgTrancheStatus](
	[PKey] [uniqueidentifier] NOT NULL,
	[CfgTrancheId] [uniqueidentifier] NULL,
	[ThresholdOperator] [int] NULL,
	[ThresholdRatio] [decimal](26, 6) NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[ComFolderStatusPaymentTypeId] [uniqueidentifier] NULL,
	[StrFolderEntityId] [uniqueidentifier] NULL,
	[StrStatusId] [uniqueidentifier] NULL,
 CONSTRAINT [CfgTrancheStatus_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[CfgTrancheStkItem](
	[PKey] [uniqueidentifier] NOT NULL,
	[CfgTrancheId] [uniqueidentifier] NULL,
	[StkItemId] [uniqueidentifier] NULL,
	[Ratio] [decimal](26, 6) NULL,
	[Sorting] [int] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [CfgTrancheStkItem_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ComAction](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[StartDate] [smalldatetime] NULL,
	[EndDate] [datetime] NULL,
	[ColorName] [nvarchar](255) COLLATE French_CI_AS NULL,
	[IsActive] [bit] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[BuyFolderId] [uniqueidentifier] NULL,
	[CfgEmployeeId] [uniqueidentifier] NULL,
	[Budget] [decimal](26, 6) NULL,
	[CanAddTotalCost] [bit] NULL,
	[TotalCost] [decimal](26, 6) NULL,
 CONSTRAINT [ComAction_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ComActionMessage](
	[PKey] [uniqueidentifier] NOT NULL,
	[DocDate] [datetime] NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[IsIn] [bit] NULL,
	[WithoutRevival] [bit] NULL,
	[AlertValidityDate] [datetime] NULL,
	[UserName] [nvarchar](255) COLLATE French_CI_AS NULL,
	[CfgEmployeeId] [uniqueidentifier] NULL,
	[StkItemId] [uniqueidentifier] NULL,
	[CfgTierId] [uniqueidentifier] NULL,
	[ComProspectionId] [uniqueidentifier] NULL,
	[ComFolderId] [uniqueidentifier] NULL,
	[ComSaleWithdrawalCancellationId] [uniqueidentifier] NULL,
	[ComSaleWithdrawalClientId] [uniqueidentifier] NULL,
	[ComSaleWithdrawalProductId] [uniqueidentifier] NULL,
	[ComActionMessageCategoryId] [uniqueidentifier] NULL,
	[ComActionMessageSubCategoryId] [uniqueidentifier] NULL,
	[ComActionMessageTypeId] [uniqueidentifier] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[IsUnpaid] [bit] NULL,
	[AdmUserId] [uniqueidentifier] NULL,
	[ComActionMessageId] [uniqueidentifier] NULL,
	[ComDocumentId] [uniqueidentifier] NULL,
	[AlertMessage] [nvarchar](max) COLLATE French_CI_AS NULL,
	[IsSystem] [bit] NULL,
	[SavClaimId] [uniqueidentifier] NULL,
	[SavClaimGroupId] [uniqueidentifier] NULL,
	[AdmEventId] [uniqueidentifier] NULL,
	[IsNotRead] [bit] NULL,
	[PrmRequestOfferId] [uniqueidentifier] NULL,
	[RntFolderId] [uniqueidentifier] NULL,
	[SynFolderId] [uniqueidentifier] NULL,
	[IsSent] [bit] NULL,
	[StrFolderStatusId] [uniqueidentifier] NULL,
 CONSTRAINT [ComActionMessage_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
CREATE NONCLUSTERED INDEX [_dta_index_ComActionMessage_5_228911887__K13_K18_K2] ON [dbo].[ComActionMessage]
(
	[ComProspectionId] ASC,
	[ComActionMessageCategoryId] ASC,
	[DocDate] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComActionMessage_5_228911887__K18_K20_K13_K31_K2_K1_K19_30] ON [dbo].[ComActionMessage]
(
	[ComActionMessageCategoryId] ASC,
	[ComActionMessageTypeId] ASC,
	[ComProspectionId] ASC,
	[IsSystem] ASC,
	[DocDate] ASC,
	[PKey] ASC,
	[ComActionMessageSubCategoryId] ASC
)
INCLUDE([AlertMessage]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComActionMessage_6_228911887__K13_K2_K18_K31_K19_K1_K35_10_30] ON [dbo].[ComActionMessage]
(
	[ComProspectionId] ASC,
	[DocDate] ASC,
	[ComActionMessageCategoryId] ASC,
	[IsSystem] ASC,
	[ComActionMessageSubCategoryId] ASC,
	[PKey] ASC,
	[IsNotRead] ASC
)
INCLUDE([CfgEmployeeId],[AlertMessage]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComActionMessage_6_228911887__K13_K31_K18_K2_K19_K1_30] ON [dbo].[ComActionMessage]
(
	[ComProspectionId] ASC,
	[IsSystem] ASC,
	[ComActionMessageCategoryId] ASC,
	[DocDate] ASC,
	[ComActionMessageSubCategoryId] ASC,
	[PKey] ASC
)
INCLUDE([AlertMessage]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComActionMessage_6_228911887__K13_K35_K31_K18_K2_K19_K1_10_30] ON [dbo].[ComActionMessage]
(
	[ComProspectionId] ASC,
	[IsNotRead] ASC,
	[IsSystem] ASC,
	[ComActionMessageCategoryId] ASC,
	[DocDate] ASC,
	[ComActionMessageSubCategoryId] ASC,
	[PKey] ASC
)
INCLUDE([CfgEmployeeId],[AlertMessage]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComActionMessage_6_228911887__K14_K18_K2_K26] ON [dbo].[ComActionMessage]
(
	[ComFolderId] ASC,
	[ComActionMessageCategoryId] ASC,
	[DocDate] ASC,
	[IsUnpaid] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComActionMessage_6_228911887__K18_K13_K1_K2_K14_K26] ON [dbo].[ComActionMessage]
(
	[ComActionMessageCategoryId] ASC,
	[ComProspectionId] ASC,
	[PKey] ASC,
	[DocDate] ASC,
	[ComFolderId] ASC,
	[IsUnpaid] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComActionMessage_6_228911887__K18_K13_K1_K20_K31_K2_K19] ON [dbo].[ComActionMessage]
(
	[ComActionMessageCategoryId] ASC,
	[ComProspectionId] ASC,
	[PKey] ASC,
	[ComActionMessageTypeId] ASC,
	[IsSystem] ASC,
	[DocDate] ASC,
	[ComActionMessageSubCategoryId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComActionMessage_6_228911887__K18_K13_K1_K20_K31_K2_K19_K35_30] ON [dbo].[ComActionMessage]
(
	[ComActionMessageCategoryId] ASC,
	[ComProspectionId] ASC,
	[PKey] ASC,
	[ComActionMessageTypeId] ASC,
	[IsSystem] ASC,
	[DocDate] ASC,
	[ComActionMessageSubCategoryId] ASC,
	[IsNotRead] ASC
)
INCLUDE([AlertMessage]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComActionMessage_7_1156915193__K13_K18] ON [dbo].[ComActionMessage]
(
	[ComProspectionId] ASC,
	[ComActionMessageCategoryId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComActionMessage_7_1156915193__K18_K31_K13_35] ON [dbo].[ComActionMessage]
(
	[ComActionMessageCategoryId] ASC,
	[IsSystem] ASC,
	[ComProspectionId] ASC
)
INCLUDE([IsNotRead]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComActionMessage_7_1156915193__K18_K31_K13_K2_K19_10_30_35] ON [dbo].[ComActionMessage]
(
	[ComActionMessageCategoryId] ASC,
	[IsSystem] ASC,
	[ComProspectionId] ASC,
	[DocDate] ASC,
	[ComActionMessageSubCategoryId] ASC
)
INCLUDE([CfgEmployeeId],[AlertMessage],[IsNotRead]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComActionMessage_7_1156915193__K18_K31_K13_K2_K19_10_35] ON [dbo].[ComActionMessage]
(
	[ComActionMessageCategoryId] ASC,
	[IsSystem] ASC,
	[ComProspectionId] ASC,
	[DocDate] ASC,
	[ComActionMessageSubCategoryId] ASC
)
INCLUDE([CfgEmployeeId],[IsNotRead]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComActionMessage_7_1156915193__K26_K18_K14_K2] ON [dbo].[ComActionMessage]
(
	[IsUnpaid] ASC,
	[ComActionMessageCategoryId] ASC,
	[ComFolderId] ASC,
	[DocDate] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ComActionMessageCategory](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Sorting] [int] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [ComActionMessageCategory_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ComActionMessageSubCategory](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[ComActionMessageCategoryId] [uniqueidentifier] NULL,
	[IsRevivalMandatory] [bit] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[ComActionMessageSubCategoryStatusId] [uniqueidentifier] NULL,
	[UpdateFinancialSchema] [bit] NULL,
	[IsCommercialSetAutomatically] [bit] NULL,
 CONSTRAINT [ComActionMessageSubCategory_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ComActionMessageSubCategoryStatus](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [ComActionMessageSubCategoryStatus_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ComActionMessageType](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[IsSystem] [bit] NULL,
	[IsCommercialSetAutomatically] [bit] NULL,
 CONSTRAINT [ComActionMessageType_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ComBudget](
	[PKey] [uniqueidentifier] NOT NULL,
	[Sorting] [int] NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [ComBudget_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ComDocument](
	[PKey] [uniqueidentifier] NOT NULL,
	[DocDate] [smalldatetime] NULL,
	[Amount] [decimal](26, 6) NULL,
	[ComSettlementCategoryId] [uniqueidentifier] NULL,
	[ExcludeFromPrice] [bit] NULL,
	[AmountHT] [decimal](26, 6) NULL,
	[AmountTTC] [decimal](26, 6) NULL,
	[VATRatio] [decimal](26, 6) NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[QuittanceCode] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[ComFolderId] [uniqueidentifier] NULL,
	[CfgCompanyId] [uniqueidentifier] NULL,
	[AdmModuleId] [uniqueidentifier] NULL,
	[DiscountRatio] [decimal](26, 16) NULL,
	[ComSaleWithdrawalProductId] [uniqueidentifier] NULL,
	[ComSaleWithdrawalClientId] [uniqueidentifier] NULL,
	[ComSaleWithdrawalCancellationId] [uniqueidentifier] NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[StlSettlementTypeId] [uniqueidentifier] NULL,
	[StlAccountId] [uniqueidentifier] NULL,
	[CfgContactId] [uniqueidentifier] NULL,
	[PaymentAuthorizationDate] [datetime] NULL,
	[CfgDebtCollectorId] [uniqueidentifier] NULL,
	[ComPenaltyDocumentId] [uniqueidentifier] NULL,
	[StlRecoveryFolderId] [uniqueidentifier] NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
 CONSTRAINT [ComDocument_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
CREATE NONCLUSTERED INDEX [_dta_index_ComDocument_5_1556200594__K1_K32_K6] ON [dbo].[ComDocument]
(
	[PKey] ASC,
	[ComFolderId] ASC,
	[ComSettlementCategoryId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComDocument_6_720721620__K32_K1_K6_2_39] ON [dbo].[ComDocument]
(
	[ComFolderId] ASC,
	[PKey] ASC,
	[ComSettlementCategoryId] ASC
)
INCLUDE([DocDate],[Notes]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComDocument_6_720721620__K32_K1_K6_K7_3] ON [dbo].[ComDocument]
(
	[ComFolderId] ASC,
	[PKey] ASC,
	[ComSettlementCategoryId] ASC,
	[ExcludeFromPrice] ASC
)
INCLUDE([Amount]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComDocument_6_720721620__K33_K1_K32_K6_2_3_7_8_9_12_22_27_28_29_30_31_34_35_36_37_38_39] ON [dbo].[ComDocument]
(
	[CfgCompanyId] ASC,
	[PKey] ASC,
	[ComFolderId] ASC,
	[ComSettlementCategoryId] ASC
)
INCLUDE([DocDate],[Amount],[ExcludeFromPrice],[AmountHT],[AmountTTC],[VATRatio],[Description],[QuittanceCode],[UserCreate],[UserCreateDateTime],[UserUpdate],[UserUpdateDateTime],[AdmModuleId],[DiscountRatio],[ComSaleWithdrawalProductId],[ComSaleWithdrawalClientId],[ComSaleWithdrawalCancellationId],[Notes]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComDocument_6_720721620__K6_K1_K32] ON [dbo].[ComDocument]
(
	[ComSettlementCategoryId] ASC,
	[PKey] ASC,
	[ComFolderId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComDocument_6_720721620__K6_K1_K32_K7_3] ON [dbo].[ComDocument]
(
	[ComSettlementCategoryId] ASC,
	[PKey] ASC,
	[ComFolderId] ASC,
	[ExcludeFromPrice] ASC
)
INCLUDE([Amount]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComDocument_6_720721620__K7_K32_K1_K6_3] ON [dbo].[ComDocument]
(
	[ExcludeFromPrice] ASC,
	[ComFolderId] ASC,
	[PKey] ASC,
	[ComSettlementCategoryId] ASC
)
INCLUDE([Amount]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComDocument_7_1556200594__K1_6] ON [dbo].[ComDocument]
(
	[PKey] ASC
)
INCLUDE([ComSettlementCategoryId]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComDocument_7_1556200594__K32_K1_K33_K6] ON [dbo].[ComDocument]
(
	[ComFolderId] ASC,
	[PKey] ASC,
	[CfgCompanyId] ASC,
	[ComSettlementCategoryId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComDocument_7_1556200594__K6_K1_K32_K33_K7_3] ON [dbo].[ComDocument]
(
	[ComSettlementCategoryId] ASC,
	[PKey] ASC,
	[ComFolderId] ASC,
	[CfgCompanyId] ASC,
	[ExcludeFromPrice] ASC
)
INCLUDE([Amount]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComDocument_7_1556200594__K7_K32_K1_K33_K6] ON [dbo].[ComDocument]
(
	[ExcludeFromPrice] ASC,
	[ComFolderId] ASC,
	[PKey] ASC,
	[CfgCompanyId] ASC,
	[ComSettlementCategoryId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ComDocumentPaymentMethod](
	[PKey] [uniqueidentifier] NOT NULL,
	[DocDate] [datetime] NULL,
	[AmountHT] [decimal](26, 16) NULL,
	[DiscountRatio] [decimal](26, 16) NULL,
	[VatRatio] [decimal](26, 16) NULL,
	[AmountTTC] [decimal](26, 16) NULL,
	[Amount] [decimal](26, 16) NULL,
	[ComFolderId] [uniqueidentifier] NULL,
	[ComSettlementCategoryId] [uniqueidentifier] NULL,
	[CfgCompanyId] [uniqueidentifier] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
 CONSTRAINT [ComDocumentPaymentMethod_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ComFolder](
	[PKey] [uniqueidentifier] NOT NULL,
	[StkVocationId] [uniqueidentifier] NOT NULL,
	[CfgCustomerId] [uniqueidentifier] NULL,
	[CfgCommercialId] [uniqueidentifier] NULL,
	[StkItemId] [uniqueidentifier] NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[DocDate] [datetime] NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[Area] [decimal](26, 6) NULL,
	[UnitCost] [decimal](26, 6) NULL,
	[ItemPrice] [decimal](26, 6) NULL,
	[UnderItemsPrice] [decimal](26, 6) NULL,
	[StatusValidityDate] [smalldatetime] NULL,
	[Contact] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Usage] [nvarchar](255) COLLATE French_CI_AS NULL,
	[CancelDate] [smalldatetime] NULL,
	[ModificationsAmount] [decimal](26, 6) NULL,
	[PenalityDescription] [nvarchar](255) COLLATE French_CI_AS NULL,
	[PriceBeforeDiscount] [decimal](26, 6) NULL,
	[Discount] [decimal](26, 6) NULL,
	[DiscountAmount] [decimal](26, 6) NULL,
	[PriceAfterDiscount] [decimal](26, 6) NULL,
	[Price] [decimal](26, 6) NULL,
	[VATRatio] [decimal](26, 6) NULL,
	[PriceHT] [decimal](26, 6) NULL,
	[TotalToPay] [decimal](26, 6) NULL,
	[DeliveryDateReal] [smalldatetime] NULL,
	[DeliveryRatio] [decimal](26, 6) NULL,
	[DeliveryMaxNumber] [int] NULL,
	[DeliveryNumber] [int] NULL,
	[DeliveryAmount] [decimal](26, 6) NULL,
	[AgreementDate] [smalldatetime] NULL,
	[AgreementSavingDate] [datetime] NULL,
	[OwnershipCertificateDate] [smalldatetime] NULL,
	[ReceptionDate] [smalldatetime] NULL,
	[PriceWithModification] [bit] NULL,
	[CreditApprovalDate] [smalldatetime] NULL,
	[CreditPossessionDate] [smalldatetime] NULL,
	[AdvanceDate] [smalldatetime] NULL,
	[AgreementAmount] [decimal](26, 6) NULL,
	[AgreementPrice] [decimal](26, 6) NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[ComProspectionId] [uniqueidentifier] NULL,
	[CfgLawyerId] [uniqueidentifier] NULL,
	[PartPercent] [decimal](26, 6) NULL,
	[Revival1] [datetime] NULL,
	[Revival2] [datetime] NULL,
	[Revival1ValidityDate] [datetime] NULL,
	[Revival2ValidityDate] [datetime] NULL,
	[CreditAgency] [nvarchar](255) COLLATE French_CI_AS NULL,
	[CreditBankId] [uniqueidentifier] NULL,
	[InsuranceDate] [datetime] NULL,
	[BankRecordDate] [datetime] NULL,
	[InterventionDate] [datetime] NULL,
	[AgreementDatePrevisionel] [datetime] NULL,
	[IsConfirmedAgreementDate] [bit] NULL,
	[IsOldCustomer] [bit] NULL,
	[DiscountChangeDate] [datetime] NULL,
	[loanCompanySendDate] [datetime] NULL,
	[loanCompanyDecisionDate] [datetime] NULL,
	[ComLoanCompanyDecisionId] [uniqueidentifier] NULL,
	[ComDocumentDetail] [text] COLLATE French_CI_AS NULL,
	[CancelDescription] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[ReservationDate] [datetime] NULL,
	[IsWithGiftChoiceLater] [bit] NULL,
	[CfgSaleManagerId] [uniqueidentifier] NULL,
	[AgreementRegistryDate] [datetime] NULL,
	[UnitCostHT] [decimal](26, 6) NULL,
	[PrecisionActDate] [datetime] NULL,
	[StrEntityId] [uniqueidentifier] NULL,
	[ModificationsAmountHT] [decimal](26, 6) NULL,
	[ItemPriceHT] [decimal](26, 6) NULL,
	[UnderItemsPriceHT] [decimal](26, 6) NULL,
	[PriceBeforeDiscountHT] [decimal](26, 6) NULL,
	[ItemPriceAfterHT] [decimal](26, 6) NULL,
	[UnderItemsPriceAfterHT] [decimal](26, 6) NULL,
	[PriceAfterDiscountHT] [decimal](26, 6) NULL,
	[ItemPriceAfter] [decimal](26, 6) NULL,
	[UnderItemsPriceAfter] [decimal](26, 6) NULL,
	[GlobalArea] [decimal](26, 6) NULL,
	[IsAccounted] [bit] NULL,
	[ReservationDateCustomer] [datetime] NULL,
	[AgreementSavingDateCustomer] [datetime] NULL,
	[DeliveryDateRealCustomer] [smalldatetime] NULL,
	[PrecisionActDateCustomer] [datetime] NULL,
	[ReservationRegistryDate] [datetime] NULL,
	[DeliveryRegistryDate] [smalldatetime] NULL,
	[PrecisionActRegistryDate] [datetime] NULL,
	[ContractPrintDate] [datetime] NULL,
	[CfgSaleManager2Id] [uniqueidentifier] NULL,
	[CfgOwnerId] [uniqueidentifier] NULL,
	[CommissionRate] [decimal](26, 6) NULL,
	[ComPaymentMethodId] [uniqueidentifier] NULL,
	[ProbabilityOfFinalisation] [decimal](26, 6) NULL,
	[RomInterest] [decimal](26, 6) NULL,
	[IsPenaltyDisabled] [bit] NULL,
	[StlQuoteId] [uniqueidentifier] NULL,
	[PrecisionActDateLegalization] [datetime] NULL,
	[PrecisionActDateLawyer] [datetime] NULL,
	[AgreementSavingDateLawyer] [datetime] NULL,
	[AgreementSavingDateForLegalization] [datetime] NULL,
	[AgreementSavingDateLegalization] [datetime] NULL,
	[AgreementSavingDateForRegistry] [datetime] NULL,
	[PrecisionActDateForLegalization] [datetime] NULL,
	[PrecisionActDateForRegistry] [datetime] NULL,
	[FinalTitleDateDeposit] [datetime] NULL,
	[FinalTitleDateWithdrawal] [datetime] NULL,
	[FinalTitleDate] [datetime] NULL,
	[FinalTitleNumber] [nvarchar](255) COLLATE French_CI_AS NULL,
	[StrStatusId] [uniqueidentifier] NULL,
 CONSTRAINT [ComFolder_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
CREATE NONCLUSTERED INDEX [_dta_index_ComFolder_5_1149247149__K1_9] ON [dbo].[ComFolder]
(
	[PKey] ASC
)
INCLUDE([Code]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComFolder_5_1149247149__K1_K8] ON [dbo].[ComFolder]
(
	[PKey] ASC,
	[StkItemId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComFolder_6_601769201__K8_K1_K117] ON [dbo].[ComFolder]
(
	[StkItemId] ASC,
	[PKey] ASC,
	[StrEntityId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComFolder_7_1149247149__K2_K8_K1] ON [dbo].[ComFolder]
(
	[StkVocationId] ASC,
	[StkItemId] ASC,
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComFolder_7_601769201__K1_K8_K117] ON [dbo].[ComFolder]
(
	[PKey] ASC,
	[StkItemId] ASC,
	[StrEntityId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComFolder_7_601769201__K1_K8_K117_K3] ON [dbo].[ComFolder]
(
	[PKey] ASC,
	[StkItemId] ASC,
	[StrEntityId] ASC,
	[CfgCustomerId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComFolder_7_601769201__K8_K117_K1_K107_K158_K3_K5_K113_K138_K85_K92_K84_9_10_11_13_14_15_16_17_29_30_31_32_33_35_37_] ON [dbo].[ComFolder]
(
	[StkItemId] ASC,
	[StrEntityId] ASC,
	[PKey] ASC,
	[ComLoanCompanyDecisionId] ASC,
	[StrStatusId] ASC,
	[CfgCustomerId] ASC,
	[CfgCommercialId] ASC,
	[CfgSaleManagerId] ASC,
	[CfgSaleManager2Id] ASC,
	[CfgLawyerId] ASC,
	[CreditBankId] ASC,
	[ComProspectionId] ASC
)
INCLUDE([Code],[DocDate],[Description],[Area],[UnitCost],[ItemPrice],[UnderItemsPrice],[StatusValidityDate],[PriceBeforeDiscount],[Discount],[DiscountAmount],[PriceAfterDiscount],[Price],[VATRatio],[PriceHT],[DeliveryDateReal],[AgreementDate],[AgreementSavingDate],[OwnershipCertificateDate],[ReceptionDate],[CreditApprovalDate],[CreditPossessionDate],[AdvanceDate],[UserCreate],[UserCreateDateTime],[UserUpdate],[UserUpdateDateTime],[Revival1],[Revival2],[Revival1ValidityDate],[Revival2ValidityDate],[InsuranceDate],[BankRecordDate],[InterventionDate],[AgreementDatePrevisionel],[IsConfirmedAgreementDate],[IsOldCustomer],[DiscountChangeDate],[loanCompanySendDate],[loanCompanyDecisionDate],[ReservationDate],[IsWithGiftChoiceLater],[AgreementRegistryDate],[UnitCostHT],[PrecisionActDate],[ItemPriceHT],[UnderItemsPriceHT],[PriceBeforeDiscountHT],[ItemPriceAfter],[UnderItemsPriceAfter],[GlobalArea],[ReservationDateCustomer],[AgreementSavingDateCustomer],[DeliveryDateRealCustomer],[PrecisionActDateCustomer],[ReservationRegistryDate],[DeliveryRegistryDate],[PrecisionActRegistryDate],[ContractPrintDate],[ProbabilityOfFinalisation],[PrecisionActDateLegalization],[PrecisionActDateLawyer],[AgreementSavingDateLawyer],[AgreementSavingDateForLegalization],[AgreementSavingDateLegalization],[AgreementSavingDateForRegistry],[PrecisionActDateForLegalization],[PrecisionActDateForRegistry],[FinalTitleDateDeposit],[FinalTitleDateWithdrawal],[FinalTitleDate],[FinalTitleNumber]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ComFolderCustomer](
	[PKey] [uniqueidentifier] NOT NULL,
	[CfgTierId] [uniqueidentifier] NOT NULL,
	[ComFolderId] [uniqueidentifier] NOT NULL,
	[PartPercent] [decimal](26, 6) NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [ComFolderCustomer_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ComFolderItem](
	[PKey] [uniqueidentifier] NOT NULL,
	[ComFolderId] [uniqueidentifier] NULL,
	[StkItemId] [uniqueidentifier] NULL,
	[StkItemTypeId] [uniqueidentifier] NULL,
	[StkItemCategoryId] [uniqueidentifier] NULL,
	[StkCostCategoryId] [uniqueidentifier] NULL,
	[ParentId] [uniqueidentifier] NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Address] [nvarchar](500) COLLATE French_CI_AS NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[Contenance] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[AreaNet] [decimal](26, 6) NULL,
	[AreaFloor] [decimal](26, 6) NULL,
	[Area] [decimal](26, 6) NULL,
	[PriceBeforeDiscount] [decimal](26, 6) NULL,
	[Discount] [decimal](26, 6) NULL,
	[Price] [decimal](26, 6) NULL,
	[AlwaysAffected] [bit] NULL,
	[IsUnderItem] [bit] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[StkOrientationId] [uniqueidentifier] NULL,
	[StkFloorId] [uniqueidentifier] NULL,
	[StkBuildingId] [uniqueidentifier] NULL,
	[IsUnderItemOrigin] [bit] NULL,
	[PriceBeforeDiscountHT] [decimal](26, 6) NULL,
	[PriceHT] [decimal](26, 6) NULL,
	[VATRatio] [decimal](26, 6) NULL,
	[IsTitled] [bit] NULL,
 CONSTRAINT [ComFolderItem_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
CREATE NONCLUSTERED INDEX [_dta_index_ComFolderItem_6_761769771__K2_K32_K24_8_9_19] ON [dbo].[ComFolderItem]
(
	[ComFolderId] ASC,
	[IsUnderItemOrigin] ASC,
	[IsUnderItem] ASC
)
INCLUDE([Code],[Description],[Area]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComFolderItem_6_761769771__K24_K32_K2_8_9_19] ON [dbo].[ComFolderItem]
(
	[IsUnderItem] ASC,
	[IsUnderItemOrigin] ASC,
	[ComFolderId] ASC
)
INCLUDE([Code],[Description],[Area]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComFolderItem_7_1309247719__K2_K24_K32_9_19] ON [dbo].[ComFolderItem]
(
	[ComFolderId] ASC,
	[IsUnderItem] ASC,
	[IsUnderItemOrigin] ASC
)
INCLUDE([Description],[Area]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComFolderItem_7_1309247719__K24_K32_K2_19] ON [dbo].[ComFolderItem]
(
	[IsUnderItem] ASC,
	[IsUnderItemOrigin] ASC,
	[ComFolderId] ASC
)
INCLUDE([Area]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComFolderItem_7_1309247719__K32_K24_K2_9_19] ON [dbo].[ComFolderItem]
(
	[IsUnderItemOrigin] ASC,
	[IsUnderItem] ASC,
	[ComFolderId] ASC
)
INCLUDE([Description],[Area]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComFolderItem_7_761769771__K2_8] ON [dbo].[ComFolderItem]
(
	[ComFolderId] ASC
)
INCLUDE([Code]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComFolderItem_7_761769771__K2_9] ON [dbo].[ComFolderItem]
(
	[ComFolderId] ASC
)
INCLUDE([Description]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComFolderItem_7_761769771__K2_K32_K24_19] ON [dbo].[ComFolderItem]
(
	[ComFolderId] ASC,
	[IsUnderItemOrigin] ASC,
	[IsUnderItem] ASC
)
INCLUDE([Area]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ComFolderStatusPaymentType](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Sorting] [int] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [ComFolderStatusPaymentType_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ComLoanCompanyDecision](
	[PKey] [uniqueidentifier] NOT NULL,
	[sorting] [int] NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [ComLoanCompanyDecision_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ComPaymentMethod](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Notes] [nvarchar](500) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [ComPaymentMethod_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ComProspection](
	[PKey] [uniqueidentifier] NOT NULL,
	[DocDate] [datetime] NULL,
	[Area] [decimal](26, 6) NULL,
	[Price] [decimal](26, 6) NULL,
	[AdvanceAmount] [decimal](26, 6) NULL,
	[ComProspectionOriginId] [uniqueidentifier] NULL,
	[CfgProspectId] [uniqueidentifier] NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[AreaTo] [decimal](26, 6) NULL,
	[PriceTo] [decimal](26, 6) NULL,
	[StkVocationId] [uniqueidentifier] NULL,
	[ComProspectionKindId] [uniqueidentifier] NULL,
	[FundingOwnBase] [decimal](26, 6) NULL,
	[FundingCredit] [decimal](26, 6) NULL,
	[FundingOther] [decimal](26, 6) NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[ComActionId] [uniqueidentifier] NULL,
	[ComProspectionOriginSupportId] [uniqueidentifier] NULL,
	[AlertValidityDate] [datetime] NULL,
	[AlertMessage] [nvarchar](255) COLLATE French_CI_AS NULL,
	[CfgCommercialId] [uniqueidentifier] NULL,
	[StkHierarchyId] [uniqueidentifier] NULL,
	[StkItemId] [uniqueidentifier] NULL,
	[IsClosed] [bit] NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[ClosingDate] [datetime] NULL,
	[ComProspectionClosingReasonId] [uniqueidentifier] NULL,
	[ClosingReasonText] [nvarchar](255) COLLATE French_CI_AS NULL,
	[CfgProposerId] [uniqueidentifier] NULL,
	[ComActionMessageTypeId] [uniqueidentifier] NULL,
	[IsOldCustomer] [bit] NULL,
	[TempDescription] [nvarchar](255) COLLATE French_CI_AS NULL,
	[TempEMail] [nvarchar](255) COLLATE French_CI_AS NULL,
	[TempTel] [nvarchar](255) COLLATE French_CI_AS NULL,
	[TempField1] [nvarchar](255) COLLATE French_CI_AS NULL,
	[TempField2] [nvarchar](255) COLLATE French_CI_AS NULL,
	[TempField3] [nvarchar](255) COLLATE French_CI_AS NULL,
	[TempField4] [nvarchar](255) COLLATE French_CI_AS NULL,
	[TempField5] [nvarchar](255) COLLATE French_CI_AS NULL,
	[TempField6] [nvarchar](255) COLLATE French_CI_AS NULL,
	[TempField7] [nvarchar](255) COLLATE French_CI_AS NULL,
	[TempField8] [nvarchar](255) COLLATE French_CI_AS NULL,
	[TempField9] [nvarchar](255) COLLATE French_CI_AS NULL,
	[TempField10] [nvarchar](255) COLLATE French_CI_AS NULL,
	[StrEntityId] [uniqueidentifier] NULL,
	[AdmCountryId] [uniqueidentifier] NULL,
	[MapLatitude] [decimal](20, 16) NULL,
	[MapLongitude] [decimal](20, 16) NULL,
	[MapRadius] [decimal](20, 16) NULL,
	[CfgIntermediateId] [uniqueidentifier] NULL,
	[ComPaymentMethodId] [uniqueidentifier] NULL,
	[ComUtilizationId] [uniqueidentifier] NULL,
	[ComBudgetId] [uniqueidentifier] NULL,
 CONSTRAINT [ComProspection_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
CREATE NONCLUSTERED INDEX [_dta_index_ComProspection_5_162099618__K68_K1_K92_K2_K78_K66_K93_K67_K10_K15_K17_K9_K62_K61_K74_K97_3_5_7_12_13_14_53_54_55_57_] ON [dbo].[ComProspection]
(
	[IsClosed] ASC,
	[PKey] ASC,
	[StrEntityId] ASC,
	[DocDate] ASC,
	[StkHierarchyId] ASC,
	[AdmCountryId] ASC,
	[StkItemId] ASC,
	[CfgProspectId] ASC,
	[StkVocationId] ASC,
	[ComProspectionKindId] ASC,
	[ComProspectionOriginId] ASC,
	[ComProspectionOriginSupportId] ASC,
	[ComActionId] ASC,
	[CfgProposerId] ASC,
	[CfgIntermediateId] ASC
)
INCLUDE([Area],[Price],[AdvanceAmount],[Notes],[AreaTo],[PriceTo],[FundingOwnBase],[FundingCredit],[FundingOther],[UserCreate],[UserCreateDateTime],[UserUpdate],[UserUpdateDateTime],[CfgCommercialId],[Code],[ClosingDate],[ComProspectionClosingReasonId],[ClosingReasonText],[ComActionMessageTypeId],[TempDescription],[TempEMail],[TempTel],[TempField1],[TempField2],[TempField3],[TempField4],[TempField5],[TempField6],[TempField7],[TempField8],[TempField9],[TempField10],[MapLatitude],[MapLongitude],[MapRadius],[ComPaymentMethodId]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComProspection_5_322100188__K9_65] ON [dbo].[ComProspection]
(
	[CfgProspectId] ASC
)
INCLUDE([StkHierarchyId]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComProspection_6_162099618__K1_K61_K9_K62_2] ON [dbo].[ComProspection]
(
	[PKey] ASC,
	[ComActionId] ASC,
	[ComProspectionOriginId] ASC,
	[ComProspectionOriginSupportId] ASC
)
INCLUDE([DocDate]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComProspection_6_162099618__K66_K1_K2_K92_K65_K17_K76_K68_K78_K15_K93_K67_K10_K9_K62_K61_3_5_7_12_13_14_53_54_55_57_] ON [dbo].[ComProspection]
(
	[StkHierarchyId] ASC,
	[PKey] ASC,
	[DocDate] ASC,
	[StrEntityId] ASC,
	[CfgCommercialId] ASC,
	[ComProspectionKindId] ASC,
	[ComActionMessageTypeId] ASC,
	[IsClosed] ASC,
	[StkVocationId] ASC,
	[AdmCountryId] ASC,
	[StkItemId] ASC,
	[CfgProspectId] ASC,
	[ComProspectionOriginId] ASC,
	[ComProspectionOriginSupportId] ASC,
	[ComActionId] ASC
)
INCLUDE([Area],[Price],[AdvanceAmount],[Notes],[AreaTo],[PriceTo],[FundingOwnBase],[FundingCredit],[FundingOther],[UserCreate],[UserCreateDateTime],[UserUpdate],[UserUpdateDateTime],[Code],[ClosingDate],[ComProspectionClosingReasonId],[ClosingReasonText],[CfgProposerId],[TempDescription],[TempEMail],[TempTel],[TempField1],[TempField2],[TempField3],[TempField4],[TempField5],[TempField6],[TempField7],[TempField8],[TempField9],[TempField10],[MapLatitude],[MapLongitude],[MapRadius],[CfgIntermediateId],[ComPaymentMethodId]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComProspection_7_322100188__K1_2] ON [dbo].[ComProspection]
(
	[PKey] ASC
)
INCLUDE([DocDate]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_ComProspection_7_322100188__K1_K61_K8_K60_2] ON [dbo].[ComProspection]
(
	[PKey] ASC,
	[ComProspectionOriginSupportId] ASC,
	[ComProspectionOriginId] ASC,
	[ComActionId] ASC
)
INCLUDE([DocDate]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ComProspectionCategory](
	[PKey] [uniqueidentifier] NOT NULL,
	[ComProspectionId] [uniqueidentifier] NULL,
	[StkItemCategoryId] [uniqueidentifier] NULL,
	[PrmRequestOfferId] [uniqueidentifier] NULL,
 CONSTRAINT [ComProspectionCategory_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ComProspectionCity](
	[PKey] [uniqueidentifier] NOT NULL,
	[AdmCityId] [uniqueidentifier] NULL,
	[ComProspectionId] [uniqueidentifier] NULL,
 CONSTRAINT [ComProspectionCity_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ComProspectionClosingReason](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [ComProspectionClosingReason_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ComProspectionKind](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[IsWithProjectMandatory] [bit] NULL,
	[AlertDelay] [int] NULL,
	[ComActionMessageSubCategoryId] [uniqueidentifier] NULL,
 CONSTRAINT [ComProspectionKind_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ComProspectionOrigin](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Sorting] [int] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[IsWithProposer] [bit] NULL,
	[IsWithGift] [bit] NULL,
	[NewOrigin] [nvarchar](255) COLLATE French_CI_AS NULL,
	[IsWithSupport] [bit] NULL,
 CONSTRAINT [ComProspectionOrigin_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ComProspectionOriginSupport](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[ComProspectionOriginId] [uniqueidentifier] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [ComProspectionOriginSupport_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ComProspectionProfile](
	[PKey] [uniqueidentifier] NOT NULL,
	[Sorting] [int] NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[ComProspectionProfileCategoryId] [uniqueidentifier] NULL,
	[Notes] [nvarchar](500) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [ComProspectionProfile_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ComProspectionProfileCategory](
	[PKey] [uniqueidentifier] NOT NULL,
	[Sorting] [int] NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Notes] [nvarchar](500) COLLATE French_CI_AS NULL,
	[MultiSelect] [bit] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[IsVisible] [bit] NULL,
 CONSTRAINT [ComProspectionProfileCategory_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ComProspectionProject](
	[PKey] [uniqueidentifier] NOT NULL,
	[ComProspectionId] [uniqueidentifier] NULL,
	[StkHierarchyId] [uniqueidentifier] NULL,
	[StkItemId] [uniqueidentifier] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [ComProspectionProject_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
CREATE NONCLUSTERED INDEX [_dta_index_ComProspectionProject_5_1767677345__K2_4] ON [dbo].[ComProspectionProject]
(
	[ComProspectionId] ASC
)
INCLUDE([StkHierarchyId]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ComProspectionSelectedProfile](
	[PKey] [uniqueidentifier] NOT NULL,
	[ComProspectionId] [uniqueidentifier] NULL,
	[ComProspectionProfileId] [uniqueidentifier] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[PrmRequestOfferId] [uniqueidentifier] NULL,
 CONSTRAINT [ComProspectionSelectedProfile_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ComProspectionType](
	[PKey] [uniqueidentifier] NOT NULL,
	[ComProspectionId] [uniqueidentifier] NULL,
	[StkItemTypeId] [uniqueidentifier] NULL,
	[PrmRequestOfferId] [uniqueidentifier] NULL,
 CONSTRAINT [ComProspectionType_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ComSaleWithdrawalCancellation](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[ComSaleId] [uniqueidentifier] NULL,
	[TotalToPay] [decimal](26, 6) NULL,
	[TotalRest] [decimal](26, 6) NULL,
	[TotalSettled] [decimal](26, 6) NULL,
	[StkItemId] [uniqueidentifier] NULL,
	[CfgOldCustomerId] [uniqueidentifier] NULL,
	[NewStkItemTotalToPay] [decimal](26, 6) NULL,
	[StkOldItemId] [uniqueidentifier] NULL,
	[ClaimDate] [datetime] NULL,
	[DecisionDate] [datetime] NULL,
	[ValidityDate] [datetime] NULL,
	[TransfertExpenseAmount] [decimal](26, 6) NULL,
	[ReimburseAmount] [decimal](26, 6) NULL,
	[IsLegalized] [bit] NULL,
	[LegalizationDate] [datetime] NULL,
	[ComSaleWithdrawalReasonId] [uniqueidentifier] NULL,
	[ComProspectionOriginId] [uniqueidentifier] NULL,
	[Discount] [decimal](26, 6) NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[TransfertExpenseAmountAfterDiscount] [decimal](26, 6) NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[StlSettlementTypeId] [uniqueidentifier] NULL,
	[Reference] [varchar](255) COLLATE French_CI_AS NULL,
	[CfgBankId] [uniqueidentifier] NULL,
	[IsAccounted] [bit] NULL,
	[CfgContactId] [uniqueidentifier] NULL,
 CONSTRAINT [ComSaleWithdrawalCancellation_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ComSaleWithdrawalClient](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[ComSaleId] [uniqueidentifier] NULL,
	[NewStkItemTotalToPay] [decimal](26, 6) NULL,
	[TotalToPay] [decimal](26, 6) NULL,
	[TotalRest] [decimal](26, 6) NULL,
	[TotalSettled] [decimal](26, 6) NULL,
	[StkOldItemId] [uniqueidentifier] NULL,
	[CfgCustomerId] [uniqueidentifier] NULL,
	[CfgOldCustomerId] [uniqueidentifier] NULL,
	[ClaimDate] [datetime] NULL,
	[DecisionDate] [datetime] NULL,
	[ValidityDate] [datetime] NULL,
	[TransfertExpenseAmount] [decimal](26, 6) NULL,
	[ReimburseAmount] [decimal](26, 6) NULL,
	[IsLegalized] [bit] NULL,
	[LegalizationDate] [datetime] NULL,
	[ComSaleWithdrawalReasonId] [uniqueidentifier] NULL,
	[ComProspectionOriginId] [uniqueidentifier] NULL,
	[Discount] [decimal](26, 6) NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[TransfertExpenseAmountAfterDiscount] [decimal](26, 6) NULL,
	[PartPercent] [decimal](26, 6) NULL,
	[NewPartPercent] [decimal](26, 6) NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[IsAccounted] [bit] NULL,
 CONSTRAINT [ComSaleWithdrawalClient_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ComSaleWithdrawalProduct](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[ComSaleId] [uniqueidentifier] NULL,
	[TotalToPay] [decimal](26, 6) NULL,
	[TotalRest] [decimal](26, 6) NULL,
	[TotalSettled] [decimal](26, 6) NULL,
	[StkItemId] [uniqueidentifier] NULL,
	[CfgCustomerId] [uniqueidentifier] NULL,
	[NewStkItemTotalToPay] [decimal](26, 6) NULL,
	[StkOldItemId] [uniqueidentifier] NULL,
	[ClaimDate] [datetime] NULL,
	[DecisionDate] [datetime] NULL,
	[ValidityDate] [datetime] NULL,
	[TransfertExpenseAmount] [decimal](26, 6) NULL,
	[ReimburseAmount] [decimal](26, 6) NULL,
	[IsLegalized] [bit] NULL,
	[LegalizationDate] [datetime] NULL,
	[ComSaleWithdrawalReasonId] [uniqueidentifier] NULL,
	[ComProspectionOriginId] [uniqueidentifier] NULL,
	[Discount] [decimal](26, 6) NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[TransfertExpenseAmountAfterDiscount] [decimal](26, 6) NULL,
	[ItemPrice] [decimal](26, 6) NULL,
	[UnderItemsPrice] [decimal](26, 6) NULL,
	[PriceBeforeDiscount] [decimal](26, 6) NULL,
	[PriceAfterDiscount] [decimal](26, 6) NULL,
	[DiscountAmount] [decimal](26, 6) NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[IsAccounted] [bit] NULL,
 CONSTRAINT [ComSaleWithdrawalProduct_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ComSaleWithdrawalReason](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Sorting] [int] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [ComSaleWithdrawalReason_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ComSettlementCategory](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[IsCredit] [bit] NULL,
	[IsSaleExpense] [bit] NULL,
	[StlCategoryId] [uniqueidentifier] NULL,
	[IsSaleAdvance] [bit] NULL,
	[IsSaleCredit] [bit] NULL,
	[IsSaleAutoFinancment] [bit] NULL,
	[IsSaleModification] [bit] NULL,
	[IsSaleResiliation] [bit] NULL,
	[IsSalePenality] [bit] NULL,
	[StkVocationId] [uniqueidentifier] NULL,
	[IsRentCaution] [bit] NULL,
	[IsRentAmount] [bit] NULL,
	[IsRentExpense] [bit] NULL,
	[IsRentResiliation] [bit] NULL,
	[IsSyndic] [bit] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[IsConfermentBasedDate] [bit] NULL,
	[VatRatio] [decimal](26, 16) NULL,
	[ExcludeFromPrice] [bit] NULL,
	[IsSaleBeforeReservation] [bit] NULL,
	[IsInAgrementContext] [bit] NULL,
	[CanUpdateDocuments] [bit] NULL,
	[IsForOthers] [bit] NULL,
	[IsAlwaysPayed] [bit] NULL,
	[ReportDescription] [nvarchar](500) COLLATE French_CI_AS NULL,
	[IsSaleIncome] [bit] NULL,
	[IsWithPenalty] [bit] NULL,
 CONSTRAINT [ComSettlementExpense_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ComUtilization](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [ComUtilization_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[GrhAgreementType](
	[PKey] [uniqueidentifier] NOT NULL,
	[Sorting] [int] NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [GrhAgreementType_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[GrhDepartment](
	[PKey] [uniqueidentifier] NOT NULL,
	[Sorting] [int] NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[GrhManagerId] [uniqueidentifier] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[CfgCompanyId] [uniqueidentifier] NULL,
 CONSTRAINT [GrhDepartment_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[GrhEchelon](
	[PKey] [uniqueidentifier] NOT NULL,
	[Sorting] [int] NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[CfgCompanyId] [uniqueidentifier] NULL,
	[Duration] [decimal](26, 6) NULL,
 CONSTRAINT [GrhEchelon_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[GrhEmployeeCategory](
	[PKey] [uniqueidentifier] NOT NULL,
	[Sorting] [int] NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[CfgCompanyId] [uniqueidentifier] NULL,
	[ColorName] [nvarchar](255) COLLATE French_CI_AS NULL,
 CONSTRAINT [GrhEmployeeCategory_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[GrhEmployeeClass](
	[PKey] [uniqueidentifier] NOT NULL,
	[Sorting] [int] NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[CfgCompanyId] [uniqueidentifier] NULL,
 CONSTRAINT [GrhEmployeeClass_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[GrhEmployeeType](
	[PKey] [uniqueidentifier] NOT NULL,
	[Sorting] [int] NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[CfgCompanyId] [uniqueidentifier] NULL,
 CONSTRAINT [GrhEmployeeType_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[GrhPaySlipModel](
	[PKey] [uniqueidentifier] NOT NULL,
	[ValidityDate] [datetime] NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Notes] [text] COLLATE French_CI_AS NULL,
	[BaseMonthNumber] [decimal](26, 6) NULL,
	[BaseWorkedDaysNumber] [decimal](26, 6) NULL,
	[BaseVacationDaysNumber] [decimal](26, 6) NULL,
	[GrhPaySlipModelKindId] [uniqueidentifier] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [GrhPaySlipModel_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[GrhPaySlipModelKind](
	[PKey] [uniqueidentifier] NOT NULL,
	[Sorting] [int] NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [GrhPaySlipModelKind_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[LndLand](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[DocDate] [datetime] NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[Anomalies] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[Adress] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Area] [decimal](26, 6) NULL,
	[Cos] [decimal](26, 6) NULL,
	[Cuf] [decimal](26, 6) NULL,
	[AlinmentStop] [int] NULL,
	[MaxHeight] [int] NULL,
	[RegistryDate] [smalldatetime] NULL,
	[RegistryNumber] [nvarchar](255) COLLATE French_CI_AS NULL,
	[RegistryPlace] [nvarchar](255) COLLATE French_CI_AS NULL,
	[ImmatriculationDate] [smalldatetime] NULL,
	[ImmatriculationCPF] [nvarchar](255) COLLATE French_CI_AS NULL,
	[PurchaseDate] [smalldatetime] NULL,
	[PurchaseUnitCost] [decimal](26, 6) NULL,
	[PurchaseCost] [decimal](26, 6) NULL,
	[LndCategoryId] [uniqueidentifier] NULL,
	[LndTypeId] [uniqueidentifier] NULL,
	[LndVocationId] [uniqueidentifier] NULL,
	[LndLevelId] [uniqueidentifier] NULL,
	[CfgSupplierId] [uniqueidentifier] NULL,
	[IsInArchive] [bit] NULL,
	[ProspectionUnitCost] [decimal](26, 6) NULL,
	[ProspectionPrice] [decimal](26, 6) NULL,
	[ProspectionPriceProposed] [decimal](26, 6) NULL,
	[Owner] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UnitCostNonBuiltPrice] [decimal](26, 6) NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[StatusValidityDate] [datetime] NULL,
	[CfgMiddlemanId] [uniqueidentifier] NULL,
	[AreaCos] [decimal](26, 6) NULL,
	[AreaCuf] [decimal](26, 6) NULL,
	[EquipementsCoef] [decimal](26, 6) NULL,
	[EquipementsArea] [decimal](26, 6) NULL,
	[AreaNet] [decimal](26, 6) NULL,
	[SalableArea] [decimal](26, 6) NULL,
	[CoefSaleableArea] [decimal](26, 6) NULL,
	[AdmCountryId] [uniqueidentifier] NULL,
	[AdmCityId] [uniqueidentifier] NULL,
	[GmapLink] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[CfgLawyerId] [uniqueidentifier] NULL,
	[StkOrientationId] [uniqueidentifier] NULL,
	[IsTransformedToLand] [bit] NULL,
	[StrStatusId] [uniqueidentifier] NULL,
	[StrEntityId] [uniqueidentifier] NULL,
	[AreaEquipmentTransferred] [decimal](26, 6) NULL,
	[AreaRegularized] [decimal](26, 6) NULL,
	[AreaCeded] [decimal](26, 6) NULL,
	[IsFinished] [bit] NULL,
	[CommerceCoef] [decimal](26, 6) NULL,
	[CommerceArea] [decimal](26, 6) NULL,
	[OfficeCoef] [decimal](26, 6) NULL,
	[OfficeArea] [decimal](26, 6) NULL,
	[HabitatArea] [decimal](26, 6) NULL,
	[UndergroundParkingArea] [decimal](26, 6) NULL,
	[UndergroundParkingCoef] [decimal](26, 6) NULL,
	[ZoneIdentification] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[DealerConnectionConditions] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[LandBase] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[IsTopographicSurvey] [bit] NULL,
 CONSTRAINT [PK_LndLand] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[LndLevel](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [LndLevel_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[LndVocation](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [PK_LndVocation] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
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
 CONSTRAINT [PrjClause_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[PrjMarket](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Delay] [int] NULL,
	[EstimatedPriceTTC] [decimal](26, 6) NULL,
	[InsuranceDECAmount] [decimal](26, 6) NULL,
	[InsuranceTRCAmount] [decimal](26, 6) NULL,
	[WarrantyRatio] [decimal](26, 6) NULL,
	[InsuranceDECRatio] [decimal](26, 6) NULL,
	[InsuranceTRCRatio] [decimal](26, 6) NULL,
	[ProportionRatio] [decimal](26, 6) NULL,
	[AdvanceRatio] [decimal](26, 6) NULL,
	[MassChangeRatio] [decimal](26, 6) NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[PrjMarketTypeId] [uniqueidentifier] NULL,
	[PrjProjectId] [uniqueidentifier] NULL,
	[CfgSupplierId] [uniqueidentifier] NULL,
	[StartDate] [datetime] NULL,
	[EndDate] [datetime] NULL,
	[StlSettlementTypeId] [uniqueidentifier] NULL,
	[PaymentDelay] [int] NULL,
	[StlCategoryId] [uniqueidentifier] NULL,
	[IsMultiSupplier] [bit] NULL,
	[PercentComplete] [decimal](18, 4) NULL,
	[DefaultVat] [decimal](26, 6) NULL,
	[ExecutedQuantityBasedInsert] [bit] NULL,
	[DuplicateInsertAutorised] [bit] NULL,
	[StockPileRatio] [decimal](26, 6) NULL,
	[StkLevelingLineId] [uniqueidentifier] NULL,
	[ApplyDiscountOnAdditionalWork] [bit] NULL,
	[ApplyWithholdingTaxOnAdditionalWork] [bit] NULL,
	[TotalPoints] [int] NULL,
	[WithMarketView] [bit] NULL,
	[LaunchOfTenderDate] [datetime] NULL,
	[TenderMaturityDate] [datetime] NULL,
	[TenderDate] [datetime] NULL,
	[ServiceOrderDate] [datetime] NULL,
	[WorkLaunchDate] [datetime] NULL,
	[WorkCompletionDate] [datetime] NULL,
	[ReattachmentDate] [datetime] NULL,
	[CancellationDate] [datetime] NULL,
	[BidOpeningDate] [datetime] NULL,
	[TendersResortDate] [datetime] NULL,
	[TendersReportDate] [datetime] NULL,
	[ContractSignatureDate] [datetime] NULL,
	[AchievmentBasedOnQuantity] [bit] NULL,
	[WithdrawalTenderDocumentsDate] [datetime] NULL,
	[EstimatedPriceHTManual] [decimal](26, 6) NULL,
	[EstimatedVatAmountManual] [decimal](26, 6) NULL,
	[EstimatedPriceTTCManual] [decimal](26, 6) NULL,
	[StartDateManual] [datetime] NULL,
	[DelayManual] [int] NULL,
	[EndDateManual] [datetime] NULL,
	[CanEditWorkingOutLineByAchivement] [bit] NULL,
	[CancellationCause] [varchar](500) COLLATE French_CI_AS NULL,
	[DescriptionLong] [nvarchar](500) COLLATE French_CI_AS NULL,
	[WarrantyInMonths] [decimal](26, 6) NULL,
	[StrEntityId] [uniqueidentifier] NULL,
	[BlokWarrantyRestitution] [bit] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[IncludedInPaymentPrevision] [bit] NULL,
	[IsClosed] [bit] NULL,
	[Gap] [decimal](26, 6) NULL,
	[StlCurrencyId] [uniqueidentifier] NULL,
	[StrStatusId] [uniqueidentifier] NULL,
 CONSTRAINT [PrjMarket_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
CREATE NONCLUSTERED INDEX [_dta_index_PrjMarket_10_20195122__K1_K30_2_3_86_96] ON [dbo].[PrjMarket]
(
	[PKey] ASC,
	[PrjProjectId] ASC
)
INCLUDE([Code],[Description],[DefaultVat],[WithMarketView]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_PrjMarket_10_20195122__K30] ON [dbo].[PrjMarket]
(
	[PrjProjectId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_PrjMarket_10_20195122__K30_16_123] ON [dbo].[PrjMarket]
(
	[PrjProjectId] ASC
)
INCLUDE([ProportionRatio],[EstimatedPriceTTCManual]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_PrjMarket_10_20195122__K30_K1_2_3_86_96] ON [dbo].[PrjMarket]
(
	[PrjProjectId] ASC,
	[PKey] ASC
)
INCLUDE([Code],[Description],[DefaultVat],[WithMarketView]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_PrjMarket_10_20195122__K30_K1_K58_K29_K57_K34_K77_K137_2_3_4_10_11_12_14_15_16_17_22_24_38_39_59_78_81_86_87_88_91_] ON [dbo].[PrjMarket]
(
	[PrjProjectId] ASC,
	[PKey] ASC,
	[StlSettlementTypeId] ASC,
	[PrjMarketTypeId] ASC,
	[StrStatusId] ASC,
	[CfgSupplierId] ASC,
	[StlCategoryId] ASC,
	[StrEntityId] ASC
)
INCLUDE([Code],[Description],[Delay],[InsuranceDECAmount],[InsuranceTRCAmount],[WarrantyRatio],[InsuranceDECRatio],[InsuranceTRCRatio],[ProportionRatio],[AdvanceRatio],[MassChangeRatio],[Notes],[StartDate],[EndDate],[PaymentDelay],[IsMultiSupplier],[PercentComplete],[DefaultVat],[ExecutedQuantityBasedInsert],[DuplicateInsertAutorised],[StockPileRatio],[StkLevelingLineId],[ApplyDiscountOnAdditionalWork],[ApplyWithholdingTaxOnAdditionalWork],[TotalPoints],[WithMarketView],[LaunchOfTenderDate],[TenderMaturityDate],[TenderDate],[ServiceOrderDate],[WorkLaunchDate],[WorkCompletionDate],[ReattachmentDate],[CancellationDate],[BidOpeningDate],[TendersResortDate],[TendersReportDate],[ContractSignatureDate],[AchievmentBasedOnQuantity],[WithdrawalTenderDocumentsDate],[EstimatedPriceHTManual],[EstimatedPriceTTCManual],[StartDateManual],[DelayManual],[EndDateManual],[CanEditWorkingOutLineByAchivement],[CancellationCause],[DescriptionLong],[WarrantyInMonths],[BlokWarrantyRestitution],[IncludedInPaymentPrevision],[IsClosed]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_PrjMarket_10_20195122__K77_K1] ON [dbo].[PrjMarket]
(
	[StlCategoryId] ASC,
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[PrjMarketType](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
 CONSTRAINT [PrjMarketType_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[PrjProject](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[StartDate] [datetime] NULL,
	[Delay] [int] NULL,
	[EndDate] [datetime] NULL,
	[IsVentilated] [bit] NULL,
	[defaultVentilationRate] [decimal](26, 6) NULL,
	[StkLevelingId] [uniqueidentifier] NULL,
	[CfgCompanyId] [uniqueidentifier] NULL,
	[ReferenceId] [uniqueidentifier] NULL,
	[DescriptionLong] [nvarchar](500) COLLATE French_CI_AS NULL,
	[ManageProrataPrjMarketId] [uniqueidentifier] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[IsACommon] [bit] NULL,
	[CfgTierManagerId] [uniqueidentifier] NULL,
	[StrEntityId] [uniqueidentifier] NULL,
	[StrStatusId] [uniqueidentifier] NULL,
	[ProvisionalWorkStartDate] [datetime] NULL,
	[ProvisionalCompletingWorkDate] [datetime] NULL,
	[RealWorkStartDate] [datetime] NULL,
	[RealCompletingWorkDate] [datetime] NULL,
	[Address] [nvarchar](500) COLLATE French_CI_AS NULL,
 CONSTRAINT [PK_PrjProject] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[PrjWorkingOut](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[DocDate] [datetime] NULL,
	[DocRef] [nvarchar](255) COLLATE French_CI_AS NULL,
	[SupplierDescription] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[ParentDocuments] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[AmountHT] [decimal](26, 6) NULL,
	[AmountVat] [decimal](26, 6) NULL,
	[AmountTTC] [decimal](26, 6) NULL,
	[AmountRegul] [decimal](26, 6) NULL,
	[AmountToPay] [decimal](26, 6) NULL,
	[LinesCount] [int] NULL,
	[CfgTierId] [uniqueidentifier] NULL,
	[PrjMarketId] [uniqueidentifier] NULL,
	[AmountTTCAfterDeductions] [decimal](26, 6) NULL,
	[Sorting] [int] NULL,
	[BuyDocumentId] [uniqueidentifier] NULL,
	[Achievement] [decimal](26, 6) NULL,
	[WarrantyAmount] [decimal](26, 6) NULL,
	[WarrantyRatio] [decimal](26, 6) NULL,
	[AdvanceAmount] [decimal](26, 6) NULL,
	[AdvanceRatio] [decimal](26, 6) NULL,
	[insuranceDECRatio] [decimal](26, 6) NULL,
	[insuranceDECAmount] [decimal](26, 6) NULL,
	[insuranceTRCAmount] [decimal](26, 6) NULL,
	[insuranceTRCRatio] [decimal](26, 6) NULL,
	[proportionRatio] [decimal](26, 6) NULL,
	[proportionAmount] [decimal](26, 6) NULL,
	[extraAmount] [decimal](26, 6) NULL,
	[nonRefundableRetainer] [decimal](26, 6) NULL,
	[amountToBePaid] [decimal](26, 6) NULL,
	[refundableRetainer] [decimal](26, 6) NULL,
	[advanceRest] [decimal](26, 6) NULL,
	[insuranceDECRest] [decimal](26, 6) NULL,
	[insuranceTRCRest] [decimal](26, 6) NULL,
	[StockAmount] [decimal](26, 6) NULL,
	[StockAmountToPay] [decimal](26, 6) NULL,
	[WorkingOutAdvanceAmount] [decimal](26, 6) NULL,
	[WorkingOutAdvanceAmountRestAtCreation] [decimal](26, 6) NULL,
	[WorkingOutAmountTTC] [decimal](26, 6) NULL,
	[StockPileRatio] [decimal](26, 6) NULL,
	[StockAmountOld] [decimal](26, 6) NULL,
	[InsuranceTRCAmountMax] [decimal](26, 6) NULL,
	[InsuranceDECAmountMax] [decimal](26, 6) NULL,
	[ProrataRefundAmount] [decimal](26, 6) NULL,
	[AdvanceBasedOnAmount] [bit] NULL,
	[WarrantyBasedOnAmount] [bit] NULL,
	[TRCBasedOnAmount] [bit] NULL,
	[DECBasedOnAmount] [bit] NULL,
	[ProrataBasedOnAmount] [bit] NULL,
	[FluctuationsOld] [decimal](26, 6) NULL,
	[Fluctuations] [decimal](26, 6) NULL,
	[FluctuationsToPay] [decimal](26, 6) NULL,
	[FinalWorkingOut] [bit] NULL,
	[SourceRatio_OLD] [decimal](26, 6) NULL,
	[RSAmount_OLD] [decimal](26, 6) NULL,
	[DefaultRSBasedOnRetainedAmounts_OLD] [bit] NULL,
	[StockAmountOldHT] [decimal](26, 6) NULL,
	[StockAmountHT] [decimal](26, 6) NULL,
	[StockAmountToPayHT] [decimal](26, 6) NULL,
	[FluctuationsOldHT] [decimal](26, 6) NULL,
	[FluctuationsHT] [decimal](26, 6) NULL,
	[FluctuationsToPayHT] [decimal](26, 6) NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[StlCurrencyId] [uniqueidentifier] NULL,
 CONSTRAINT [PrjWorkingOut_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
CREATE NONCLUSTERED INDEX [_dta_index_PrjWorkingOut_10_957246465__K1_K18_11] ON [dbo].[PrjWorkingOut]
(
	[PKey] ASC,
	[Sorting] ASC
)
INCLUDE([AmountTTC]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_PrjWorkingOut_10_957246465__K16_4] ON [dbo].[PrjWorkingOut]
(
	[PrjMarketId] ASC
)
INCLUDE([DocDate]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_PrjWorkingOut_10_957246465__K16_K1_K18_21_23_41_42_43_47_58_68_71] ON [dbo].[PrjWorkingOut]
(
	[PrjMarketId] ASC,
	[PKey] ASC,
	[Sorting] ASC
)
INCLUDE([WarrantyAmount],[AdvanceAmount],[StockAmount],[StockAmountToPay],[WorkingOutAdvanceAmount],[StockAmountOld],[FluctuationsToPay],[StockAmountToPayHT],[FluctuationsToPayHT]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_PrjWorkingOut_10_957246465__K16_K1_K19_K15_K18_2_3_4_5_6_7_8_9_11_12_14_17_21_22_23_24_25_26_27_28_30_31_33_34_35_] ON [dbo].[PrjWorkingOut]
(
	[PrjMarketId] ASC,
	[PKey] ASC,
	[BuyDocumentId] ASC,
	[CfgTierId] ASC,
	[Sorting] ASC
)
INCLUDE([Code],[Description],[DocDate],[DocRef],[SupplierDescription],[Notes],[ParentDocuments],[AmountHT],[AmountTTC],[AmountRegul],[LinesCount],[AmountTTCAfterDeductions],[WarrantyAmount],[WarrantyRatio],[AdvanceAmount],[AdvanceRatio],[insuranceDECRatio],[insuranceDECAmount],[insuranceTRCAmount],[insuranceTRCRatio],[proportionRatio],[proportionAmount],[nonRefundableRetainer],[amountToBePaid],[refundableRetainer],[advanceRest],[insuranceDECRest],[insuranceTRCRest],[StockAmount],[StockAmountToPay],[WorkingOutAdvanceAmount],[WorkingOutAdvanceAmountRestAtCreation],[WorkingOutAmountTTC],[StockPileRatio],[StockAmountOld],[InsuranceTRCAmountMax],[InsuranceDECAmountMax],[ProrataRefundAmount],[AdvanceBasedOnAmount],[WarrantyBasedOnAmount],[TRCBasedOnAmount],[DECBasedOnAmount],[ProrataBasedOnAmount],[FluctuationsOld],[Fluctuations],[FluctuationsToPay],[FinalWorkingOut],[StockAmountOldHT],[StockAmountHT],[StockAmountToPayHT],[FluctuationsOldHT],[FluctuationsHT],[FluctuationsToPayHT]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_PrjWorkingOut_10_957246465__K16_K18_K1_K19_K15_2_3_4_5_6_7_8_9_11_12_14_17_21_22_23_24_25_26_27_28_30_31_33_34_35_] ON [dbo].[PrjWorkingOut]
(
	[PrjMarketId] ASC,
	[Sorting] ASC,
	[PKey] ASC,
	[BuyDocumentId] ASC,
	[CfgTierId] ASC
)
INCLUDE([Code],[Description],[DocDate],[DocRef],[SupplierDescription],[Notes],[ParentDocuments],[AmountHT],[AmountTTC],[AmountRegul],[LinesCount],[AmountTTCAfterDeductions],[WarrantyAmount],[WarrantyRatio],[AdvanceAmount],[AdvanceRatio],[insuranceDECRatio],[insuranceDECAmount],[insuranceTRCAmount],[insuranceTRCRatio],[proportionRatio],[proportionAmount],[nonRefundableRetainer],[amountToBePaid],[refundableRetainer],[advanceRest],[insuranceDECRest],[insuranceTRCRest],[StockAmount],[StockAmountToPay],[WorkingOutAdvanceAmount],[WorkingOutAdvanceAmountRestAtCreation],[WorkingOutAmountTTC],[StockPileRatio],[StockAmountOld],[InsuranceTRCAmountMax],[InsuranceDECAmountMax],[ProrataRefundAmount],[AdvanceBasedOnAmount],[WarrantyBasedOnAmount],[TRCBasedOnAmount],[DECBasedOnAmount],[ProrataBasedOnAmount],[FluctuationsOld],[Fluctuations],[FluctuationsToPay],[FinalWorkingOut],[StockAmountOldHT],[StockAmountHT],[StockAmountToPayHT],[FluctuationsOldHT],[FluctuationsHT],[FluctuationsToPayHT]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_PrjWorkingOut_5_1472724299__K16_23] ON [dbo].[PrjWorkingOut]
(
	[PrjMarketId] ASC
)
INCLUDE([AdvanceAmount]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_PrjWorkingOut_5_1472724299__K16_K1_21_23_26_27_31_33_43] ON [dbo].[PrjWorkingOut]
(
	[PrjMarketId] ASC,
	[PKey] ASC
)
INCLUDE([WarrantyAmount],[AdvanceAmount],[insuranceDECAmount],[insuranceTRCAmount],[proportionAmount],[nonRefundableRetainer],[WorkingOutAdvanceAmount]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[PrmRequestOffer](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](500) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](500) COLLATE French_CI_AS NULL,
	[DocDate] [datetime] NULL,
	[Address] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Address2] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Address3] [nvarchar](255) COLLATE French_CI_AS NULL,
	[ZipCode] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Budget] [decimal](26, 6) NULL,
	[Area] [decimal](26, 6) NULL,
	[AdmCountryId] [uniqueidentifier] NULL,
	[AdmCityId] [uniqueidentifier] NULL,
	[StrEntityId] [uniqueidentifier] NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[CfgTierId] [uniqueidentifier] NULL,
	[PrmPropertyId] [uniqueidentifier] NULL,
	[CfgCommercialId] [uniqueidentifier] NULL,
	[PrmPropertyCategoryId] [uniqueidentifier] NULL,
	[PrmPropertyTypeId] [uniqueidentifier] NULL,
	[CfgIntermediateId] [uniqueidentifier] NULL,
	[CfgOwnerId] [uniqueidentifier] NULL,
	[CfgCustomerId] [uniqueidentifier] NULL,
	[IsOccupied] [bit] NULL,
	[ReleaseDate] [datetime] NULL,
	[ContractStartDate] [datetime] NULL,
	[ContractEndDate] [datetime] NULL,
	[StrStatusId] [uniqueidentifier] NULL,
	[CommissionRate] [decimal](26, 6) NULL,
	[RntPeriodicityId] [uniqueidentifier] NULL,
	[ComActionId] [uniqueidentifier] NULL,
	[CfgTrancheId] [uniqueidentifier] NULL,
	[ComProspectionKindId] [uniqueidentifier] NULL,
	[ComActionMessageTypeId] [uniqueidentifier] NULL,
	[ComProspectionOriginId] [uniqueidentifier] NULL,
	[ComProspectionOriginSupportId] [uniqueidentifier] NULL,
	[CfgProposerId] [uniqueidentifier] NULL,
	[AdvanceAmount] [decimal](26, 6) NULL,
	[FundingCredit] [decimal](26, 6) NULL,
	[FundingOwnBase] [decimal](26, 6) NULL,
	[MapLatitude] [decimal](20, 16) NULL,
	[MapLongitude] [decimal](20, 16) NULL,
	[IsReversmentBasedOnPayment] [bit] NULL,
 CONSTRAINT [PrmRequestOffer_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[RntDocument](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[PaymentDate] [smalldatetime] NULL,
	[DocDate] [smalldatetime] NULL,
	[PeriodDescription] [nvarchar](255) COLLATE French_CI_AS NULL,
	[DocType] [nvarchar](255) COLLATE French_CI_AS NULL,
	[DayNumber] [int] NULL,
	[VatExonorationNumber] [int] NULL,
	[SynFolderId] [uniqueidentifier] NULL,
	[RntFolderId] [uniqueidentifier] NULL,
	[AmountHT] [decimal](26, 6) NULL,
	[AmountVat] [decimal](26, 6) NULL,
	[AmountTTC] [decimal](26, 6) NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[RntSettlementCategoryId] [uniqueidentifier] NULL,
	[CfgCompanyId] [uniqueidentifier] NULL,
	[AdmModuleId] [uniqueidentifier] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[RntReconductionHistoryId] [uniqueidentifier] NULL,
	[RntPeriodicityId] [uniqueidentifier] NULL,
	[AmountInvoicedCalculated] [decimal](26, 6) NULL,
	[AmountInvoicedRestTTCCalculated] [decimal](26, 6) NULL,
	[AmountRefundedHTCalculated] [decimal](26, 6) NULL,
	[AmountRefundedTTCCalculated] [decimal](26, 6) NULL,
	[CfgDebtCollectorId] [uniqueidentifier] NULL,
	[StlRecoveryFolderId] [uniqueidentifier] NULL,
	[BuyDocumentLineId] [uniqueidentifier] NULL,
	[ComFolderId] [uniqueidentifier] NULL,
	[AmountSettledCalculated] [decimal](26, 6) NULL,
	[AgreementAmountHT] [decimal](26, 6) NULL,
	[AdjustmentAmountHT] [decimal](26, 6) NULL,
	[VatRatio] [decimal](26, 6) NULL,
	[AmountInvoicedRestCalculated] [decimal](26, 6) NULL,
 CONSTRAINT [RntDocument_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K1_K11_K10_6] ON [dbo].[RntDocument]
(
	[PKey] ASC,
	[RntFolderId] ASC,
	[SynFolderId] ASC
)
INCLUDE([PeriodDescription]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K1_K11_K26] ON [dbo].[RntDocument]
(
	[PKey] ASC,
	[RntFolderId] ASC,
	[AmountTTC] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K1_K2_3_4_5_6_7_8_9_11_12_13_14_15_16_17_18_19_20_21_22_23_24_25_26_27_28_29_30_31_32_33_] ON [dbo].[RntDocument]
(
	[PKey] ASC,
	[Code] ASC
)
INCLUDE([Description],[PaymentDate],[DocDate],[PeriodDescription],[DocType],[DayNumber],[VatExonorationNumber],[RntFolderId],[AgreementAmountHT],[AdjustmentAmountHT],[AmountHT],[VatRatio],[AmountVat],[AmountTTC],[Notes],[RntSettlementCategoryId],[CfgCompanyId],[AdmModuleId],[UserCreate],[UserCreateDateTime],[UserUpdate],[UserUpdateDateTime],[RntPeriodicityId]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K1_K20_K23_11] ON [dbo].[RntDocument]
(
	[PKey] ASC,
	[AmountHT] ASC,
	[AmountTTC] ASC
)
INCLUDE([RntFolderId]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K1_K30] ON [dbo].[RntDocument]
(
	[PKey] ASC,
	[RntSettlementCategoryId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K10] ON [dbo].[RntDocument]
(
	[SynFolderId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K10_K1_K11_K30_K41_K46_K2_3_4_6] ON [dbo].[RntDocument]
(
	[SynFolderId] ASC,
	[PKey] ASC,
	[RntFolderId] ASC,
	[RntSettlementCategoryId] ASC,
	[AmountInvoicedCalculated] ASC,
	[Code] ASC
)
INCLUDE([Description],[PaymentDate],[PeriodDescription]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K10_K1_K20_K23_K30_K11_K45] ON [dbo].[RntDocument]
(
	[SynFolderId] ASC,
	[PKey] ASC,
	[AmountHT] ASC,
	[AmountTTC] ASC,
	[RntSettlementCategoryId] ASC,
	[RntFolderId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K10_K1_K30_K11_K4_K20_K47_K48_K45_K23_K17_K14] ON [dbo].[RntDocument]
(
	[SynFolderId] ASC,
	[PKey] ASC,
	[RntSettlementCategoryId] ASC,
	[RntFolderId] ASC,
	[PaymentDate] ASC,
	[AmountSettledCalculated] ASC,
	[AmountTTC] ASC,
	[AmountHT] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K10_K1_K30_K31_K39_K33_K32_K11_K2_3_4_5_6_7_8_9_12_13_14_15_16_17_18_19_20_21_22_23_24_25_] ON [dbo].[RntDocument]
(
	[SynFolderId] ASC,
	[PKey] ASC,
	[RntSettlementCategoryId] ASC,
	[CfgCompanyId] ASC,
	[RntPeriodicityId] ASC,
	[AdmModuleId] ASC,
	[RntFolderId] ASC,
	[Code] ASC
)
INCLUDE([Description],[PaymentDate],[DocDate],[PeriodDescription],[DocType],[DayNumber],[VatExonorationNumber],[AgreementAmountHT],[AdjustmentAmountHT],[AmountHT],[VatRatio],[AmountVat],[AmountTTC],[Notes],[UserCreate],[UserCreateDateTime],[UserUpdate],[UserUpdateDateTime]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K10_K30_K11_K1_K4] ON [dbo].[RntDocument]
(
	[SynFolderId] ASC,
	[RntSettlementCategoryId] ASC,
	[RntFolderId] ASC,
	[PKey] ASC,
	[PaymentDate] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K10_K30_K4_K45_K11_K47_K48] ON [dbo].[RntDocument]
(
	[SynFolderId] ASC,
	[RntSettlementCategoryId] ASC,
	[PaymentDate] ASC,
	[RntFolderId] ASC,
	[AmountSettledCalculated] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K10_K4_K1_K11_K24_K41_K43_K44_K2_3_5_6_8_9_14_15_16_17_20_21_22_23_25_26_29_30_31_45_46] ON [dbo].[RntDocument]
(
	[SynFolderId] ASC,
	[PaymentDate] ASC,
	[PKey] ASC,
	[RntFolderId] ASC,
	[AmountHT] ASC,
	[AmountInvoicedCalculated] ASC,
	[AmountInvoicedRestCalculated] ASC,
	[Code] ASC
)
INCLUDE([Description],[DocDate],[PeriodDescription],[DayNumber],[VatExonorationNumber],[VatRatio],[AmountVat],[AmountTTC],[Notes],[RntSettlementCategoryId],[CfgCompanyId]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K10_K4_K1_K11_K42] ON [dbo].[RntDocument]
(
	[SynFolderId] ASC,
	[PaymentDate] ASC,
	[PKey] ASC,
	[RntFolderId] ASC,
	[AmountInvoicedRestTTCCalculated] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K10_K4_K1_K30_K11_K20_K23_K45_K47_K48] ON [dbo].[RntDocument]
(
	[SynFolderId] ASC,
	[PaymentDate] ASC,
	[PKey] ASC,
	[RntSettlementCategoryId] ASC,
	[RntFolderId] ASC,
	[AmountHT] ASC,
	[AmountTTC] ASC,
	[AmountSettledCalculated] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K10_K4_K1_K30_K11_K20_K47_K48_K45_K23_K17_K14] ON [dbo].[RntDocument]
(
	[SynFolderId] ASC,
	[PaymentDate] ASC,
	[PKey] ASC,
	[RntSettlementCategoryId] ASC,
	[RntFolderId] ASC,
	[AmountHT] ASC,
	[AmountSettledCalculated] ASC,
	[AmountTTC] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K11_30] ON [dbo].[RntDocument]
(
	[RntFolderId] ASC
)
INCLUDE([RntSettlementCategoryId]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K11_K1_K17_K14_K10] ON [dbo].[RntDocument]
(
	[RntFolderId] ASC,
	[PKey] ASC,
	[AmountTTC] ASC,
	[AmountHT] ASC,
	[SynFolderId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K11_K1_K20_K23] ON [dbo].[RntDocument]
(
	[RntFolderId] ASC,
	[PKey] ASC,
	[AmountHT] ASC,
	[AmountTTC] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K11_K1_K20_K23_K10_K30_K17] ON [dbo].[RntDocument]
(
	[RntFolderId] ASC,
	[PKey] ASC,
	[AmountHT] ASC,
	[AmountTTC] ASC,
	[SynFolderId] ASC,
	[RntSettlementCategoryId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K11_K1_K30_K31_K39_K33_K32_K10_K2_3_4_5_6_7_8_9_12_13_14_15_16_17_18_19_20_21_22_23_24_25_] ON [dbo].[RntDocument]
(
	[RntFolderId] ASC,
	[PKey] ASC,
	[RntSettlementCategoryId] ASC,
	[CfgCompanyId] ASC,
	[RntPeriodicityId] ASC,
	[AdmModuleId] ASC,
	[SynFolderId] ASC,
	[Code] ASC
)
INCLUDE([Description],[PaymentDate],[DocDate],[PeriodDescription],[DocType],[DayNumber],[VatExonorationNumber],[AgreementAmountHT],[AdjustmentAmountHT],[AmountHT],[VatRatio],[AmountVat],[AmountTTC],[Notes],[UserCreate],[UserCreateDateTime],[UserUpdate],[UserUpdateDateTime]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K11_K1_K4_28_30_33] ON [dbo].[RntDocument]
(
	[RntFolderId] ASC,
	[PKey] ASC,
	[PaymentDate] ASC
)
INCLUDE([AmountTTC],[RntSettlementCategoryId]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K11_K1_K4_K10_K27_28_30_33] ON [dbo].[RntDocument]
(
	[RntFolderId] ASC,
	[PKey] ASC,
	[PaymentDate] ASC,
	[SynFolderId] ASC
)
INCLUDE([AmountTTC],[RntSettlementCategoryId]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K11_K1_K4_K20_K10_K23_K17_30] ON [dbo].[RntDocument]
(
	[PKey] ASC,
	[RntFolderId] ASC,
	[SynFolderId] ASC,
	[PaymentDate] ASC,
	[AmountHT] ASC,
	[AmountTTC] ASC
)
INCLUDE([RntSettlementCategoryId]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K11_K17_K4_30] ON [dbo].[RntDocument]
(
	[RntFolderId] ASC,
	[AmountTTC] ASC,
	[PaymentDate] ASC
)
INCLUDE([RntSettlementCategoryId]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K11_K23_30] ON [dbo].[RntDocument]
(
	[RntFolderId] ASC,
	[AmountTTC] ASC
)
INCLUDE([RntSettlementCategoryId]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K11_K23_K4_30] ON [dbo].[RntDocument]
(
	[RntFolderId] ASC,
	[AmountTTC] ASC,
	[PaymentDate] ASC
)
INCLUDE([RntSettlementCategoryId]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K11_K23_K4_K10_K1_30] ON [dbo].[RntDocument]
(
	[RntFolderId] ASC,
	[AmountTTC] ASC,
	[PaymentDate] ASC,
	[SynFolderId] ASC,
	[PKey] ASC
)
INCLUDE([RntSettlementCategoryId]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K11_K23_K4_K10_K1_K17_K47_K48_30] ON [dbo].[RntDocument]
(
	[RntFolderId] ASC,
	[AmountTTC] ASC,
	[PaymentDate] ASC,
	[SynFolderId] ASC,
	[PKey] ASC,
	[AmountSettledCalculated] ASC
)
INCLUDE([RntSettlementCategoryId]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K11_K4] ON [dbo].[RntDocument]
(
	[RntFolderId] ASC,
	[PaymentDate] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K11_K45_K4_30] ON [dbo].[RntDocument]
(
	[RntFolderId] ASC,
	[PaymentDate] ASC
)
INCLUDE([RntSettlementCategoryId]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K30_K11_K1_K33_K4_K10_28] ON [dbo].[RntDocument]
(
	[RntSettlementCategoryId] ASC,
	[RntFolderId] ASC,
	[PKey] ASC,
	[PaymentDate] ASC,
	[SynFolderId] ASC
)
INCLUDE([AmountTTC]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K30_K11_K1_K4_K10] ON [dbo].[RntDocument]
(
	[RntSettlementCategoryId] ASC,
	[RntFolderId] ASC,
	[PKey] ASC,
	[PaymentDate] ASC,
	[SynFolderId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K30_K11_K17] ON [dbo].[RntDocument]
(
	[RntSettlementCategoryId] ASC,
	[RntFolderId] ASC,
	[AmountTTC] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K30_K11_K4_K17_K10_K1] ON [dbo].[RntDocument]
(
	[RntSettlementCategoryId] ASC,
	[RntFolderId] ASC,
	[PaymentDate] ASC,
	[AmountTTC] ASC,
	[SynFolderId] ASC,
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K30_K11_K4_K23] ON [dbo].[RntDocument]
(
	[RntSettlementCategoryId] ASC,
	[RntFolderId] ASC,
	[PaymentDate] ASC,
	[AmountTTC] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K30_K11_K47_K48_K10_K1] ON [dbo].[RntDocument]
(
	[RntSettlementCategoryId] ASC,
	[RntFolderId] ASC,
	[AmountSettledCalculated] ASC,
	[SynFolderId] ASC,
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K4_K11_K10_K1_K14_K17_K20_K23_K45_K30_K24_K41_K43_K44_K2_3_5_6_8_9_15_16_21_22_25_26_29_] ON [dbo].[RntDocument]
(
	[PaymentDate] ASC,
	[RntFolderId] ASC,
	[SynFolderId] ASC,
	[PKey] ASC,
	[AmountHT] ASC,
	[AmountTTC] ASC,
	[RntSettlementCategoryId] ASC,
	[AmountInvoicedCalculated] ASC,
	[AmountInvoicedRestCalculated] ASC,
	[Code] ASC
)
INCLUDE([Description],[DocDate],[PeriodDescription],[DayNumber],[VatExonorationNumber],[VatRatio],[AmountVat],[Notes],[CfgCompanyId]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K41_K11_K30_K2_K10_K1_3_4_6] ON [dbo].[RntDocument]
(
	[AmountInvoicedCalculated] ASC,
	[RntFolderId] ASC,
	[RntSettlementCategoryId] ASC,
	[Code] ASC,
	[SynFolderId] ASC,
	[PKey] ASC
)
INCLUDE([Description],[PaymentDate],[PeriodDescription]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K42_K4_K11] ON [dbo].[RntDocument]
(
	[AmountInvoicedRestTTCCalculated] ASC,
	[PaymentDate] ASC,
	[RntFolderId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_5_1602156803__K4D_K1_K11_K20_30] ON [dbo].[RntDocument]
(
	[PaymentDate] DESC,
	[PKey] ASC,
	[RntFolderId] ASC,
	[AmountHT] ASC
)
INCLUDE([RntSettlementCategoryId]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_6_1237631502__K1_26] ON [dbo].[RntDocument]
(
	[PKey] ASC
)
INCLUDE([AmountTTC]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_6_1237631502__K10_K1_3_4_5_6_29_30] ON [dbo].[RntDocument]
(
	[SynFolderId] ASC,
	[PKey] ASC
)
INCLUDE([Description],[PaymentDate],[DocDate],[PeriodDescription],[Notes],[RntSettlementCategoryId]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_6_1237631502__K10_K1_30] ON [dbo].[RntDocument]
(
	[SynFolderId] ASC,
	[PKey] ASC
)
INCLUDE([RntSettlementCategoryId]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_6_1237631502__K11_K1_K30_K10_K54_3_4_5_6_29] ON [dbo].[RntDocument]
(
	[RntFolderId] ASC,
	[PKey] ASC,
	[RntSettlementCategoryId] ASC,
	[SynFolderId] ASC,
	[ComFolderId] ASC
)
INCLUDE([Description],[PaymentDate],[DocDate],[PeriodDescription],[Notes]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_6_1237631502__K11_K1_K54_K30_K10_6] ON [dbo].[RntDocument]
(
	[RntFolderId] ASC,
	[PKey] ASC,
	[ComFolderId] ASC,
	[RntSettlementCategoryId] ASC,
	[SynFolderId] ASC
)
INCLUDE([PeriodDescription]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_6_1237631502__K30_K1_K54_K11_K10_6] ON [dbo].[RntDocument]
(
	[RntSettlementCategoryId] ASC,
	[PKey] ASC,
	[ComFolderId] ASC,
	[RntFolderId] ASC,
	[SynFolderId] ASC
)
INCLUDE([PeriodDescription]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_6_1237631502__K54] ON [dbo].[RntDocument]
(
	[ComFolderId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_7_1154155207__K1_K54_K30_K11_K10] ON [dbo].[RntDocument]
(
	[PKey] ASC,
	[ComFolderId] ASC,
	[RntSettlementCategoryId] ASC,
	[RntFolderId] ASC,
	[SynFolderId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_7_1602156803__K1_K10_K11] ON [dbo].[RntDocument]
(
	[PKey] ASC,
	[SynFolderId] ASC,
	[RntFolderId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntDocument_8_1602156803__K11_K1_K24_K26_K10_K54_K30_K4_K47_K48] ON [dbo].[RntDocument]
(
	[RntFolderId] ASC,
	[PKey] ASC,
	[AmountHT] ASC,
	[AmountTTC] ASC,
	[SynFolderId] ASC,
	[ComFolderId] ASC,
	[RntSettlementCategoryId] ASC,
	[PaymentDate] ASC,
	[AmountSettledCalculated] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [IDX_RntDocument_RntFolderId_PaymentDate_PKey_RntSettlementCategoryId] ON [dbo].[RntDocument]
(
	[RntFolderId] ASC,
	[PaymentDate] ASC
)
INCLUDE([PKey],[RntSettlementCategoryId]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [IDX_RntDocument_RntFolderId_PKey_RentAmountHT_RentVatRatio_RentAmountTTC] ON [dbo].[RntDocument]
(
	[RntFolderId] ASC
)
INCLUDE([PKey],[AmountHT],[VatRatio],[AmountTTC]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[RntFolder](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[DocDate] [datetime] NULL,
	[CfgCustomerId] [uniqueidentifier] NULL,
	[StkItemId] [uniqueidentifier] NULL,
	[StkItemArea] [decimal](26, 6) NULL,
	[StkItemRentHT] [decimal](26, 6) NULL,
	[StkItemRentVatRatio] [decimal](26, 6) NULL,
	[StkItemRentTTC] [decimal](26, 6) NULL,
	[CfgCommercialId] [uniqueidentifier] NULL,
	[IsInvoiceCustomerGroupping] [bit] NULL,
	[Usage] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Contact] [nvarchar](255) COLLATE French_CI_AS NULL,
	[RntPeriodicityId] [uniqueidentifier] NULL,
	[PeriodicityNumber] [int] NULL,
	[PeriodicityReconductionRatio] [decimal](26, 6) NULL,
	[PeriodicityReconductionFrequency] [int] NULL,
	[PeriodicityBeginDate] [smalldatetime] NULL,
	[PeriodicityEndDate] [smalldatetime] NULL,
	[RntReconductionTypeId] [uniqueidentifier] NULL,
	[CautionMonthNumber] [decimal](26, 6) NULL,
	[CautionVatRatio] [decimal](26, 6) NULL,
	[CautionAmountHT] [decimal](26, 6) NULL,
	[CautionAmountTTC] [decimal](26, 6) NULL,
	[RentVatRatio] [decimal](26, 6) NULL,
	[RentMonthAmountHT] [decimal](26, 6) NULL,
	[RentMonthAmountTTC] [decimal](26, 6) NULL,
	[RentPeriodAmountHT] [decimal](26, 6) NULL,
	[RentPeriodAmountTTC] [decimal](26, 6) NULL,
	[RentYearAmountHT] [decimal](26, 6) NULL,
	[RentYearAmountTTC] [decimal](26, 6) NULL,
	[SyndicVatRatio] [decimal](26, 6) NULL,
	[SyndicMonthAmountHT] [decimal](26, 6) NULL,
	[SyndicMonthAmountTTC] [decimal](26, 6) NULL,
	[SyndicPeriodAmountHT] [decimal](26, 6) NULL,
	[SyndicPeriodAmountTTC] [decimal](26, 6) NULL,
	[SyndicYearAmountHT] [decimal](26, 6) NULL,
	[SyndicYearAmountTTC] [decimal](26, 6) NULL,
	[CfgLawyerId] [uniqueidentifier] NULL,
	[AgreementDate1] [datetime] NULL,
	[AgreementDate2] [datetime] NULL,
	[DeliveryDateReal] [datetime] NULL,
	[AgreementSavingDate] [datetime] NULL,
	[InsuranceDate] [datetime] NULL,
	[CancelDate] [smalldatetime] NULL,
	[CancelAmount] [decimal](26, 6) NULL,
	[CancelDescription] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[StrEntityId] [uniqueidentifier] NULL,
	[StkVocationId] [uniqueidentifier] NULL,
	[IsSynPricing] [bit] NULL,
	[NextAugmentationDate] [datetime] NULL,
	[IsImported] [bit] NULL,
	[LawyerTransmissionDate] [datetime] NULL,
	[StartContentiousDate] [datetime] NULL,
	[LastLetterOfIncreaseDate] [datetime] NULL,
	[CfgTierBailiffId] [uniqueidentifier] NULL,
	[ComProspectionId] [uniqueidentifier] NULL,
	[CfgOwnerId] [uniqueidentifier] NULL,
	[CommissionRate] [decimal](26, 6) NULL,
	[RntPeriodicityReverseId] [uniqueidentifier] NULL,
	[IsReversmentBasedOnPayment] [bit] NULL,
	[AdvanceMonthNumber] [decimal](26, 6) NULL,
	[CommissionMonthNumber] [decimal](26, 6) NULL,
	[RegistrationFees] [decimal](26, 6) NULL,
	[IsPenaltyDisabled] [bit] NULL,
	[StlQuoteId] [uniqueidentifier] NULL,
	[StrStatusId] [uniqueidentifier] NULL,
 CONSTRAINT [RntFolder_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
CREATE NONCLUSTERED INDEX [_dta_index_RntFolder_5_1298155720__K1] ON [dbo].[RntFolder]
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntFolder_5_1298155720__K1_K7] ON [dbo].[RntFolder]
(
	[PKey] ASC,
	[StkItemId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntFolder_5_1298155720__K6] ON [dbo].[RntFolder]
(
	[CfgCustomerId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[RntPeriodicity](
	[PKey] [uniqueidentifier] NOT NULL,
	[Sorting] [int] NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[MonthNumber] [int] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[Unity] [nvarchar](255) COLLATE French_CI_AS NULL,
 CONSTRAINT [RntPeriodicity_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[RntReconductionHistory](
	[PKey] [uniqueidentifier] NOT NULL,
	[PeriodicityBeginDate] [smalldatetime] NULL,
	[PeriodicityEndDate] [smalldatetime] NULL,
	[RntFolderId] [uniqueidentifier] NULL,
	[SynFolderId] [uniqueidentifier] NULL,
	[Notes] [nvarchar](500) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [RntReconductionHistory_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
CREATE NONCLUSTERED INDEX [_dta_index_RntReconductionHistory_5_1522156518__K1_K8_9] ON [dbo].[RntReconductionHistory]
(
	[PKey] ASC,
	[PeriodicityBeginDate] ASC
)
INCLUDE([PeriodicityEndDate]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntReconductionHistory_5_1522156518__K32_K1_K8_9] ON [dbo].[RntReconductionHistory]
(
	[SynFolderId] ASC,
	[PKey] ASC,
	[PeriodicityBeginDate] ASC
)
INCLUDE([PeriodicityEndDate]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_RntReconductionHistory_5_1522156518__K8_1_9_31] ON [dbo].[RntReconductionHistory]
(
	[PeriodicityBeginDate] ASC
)
INCLUDE([PKey],[PeriodicityEndDate],[RntFolderId]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[RntReconductionType](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[isAuto] [bit] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [RntReconductionType_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[RntSettlementCategory](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[IsCredit] [bit] NULL,
	[IsRentCaution] [bit] NULL,
	[IsRentAmount] [bit] NULL,
	[IsRentExpense] [bit] NULL,
	[IsRentResiliation] [bit] NULL,
	[IsSyndic] [bit] NULL,
	[StlCategoryId] [uniqueidentifier] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[CanGenerateDocument] [bit] NULL,
	[WithFiscalStamp] [bit] NULL,
	[ReportDescription] [nvarchar](500) COLLATE French_CI_AS NULL,
	[IsWithReversment] [bit] NULL,
 CONSTRAINT [RntSettlementCategory_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[SavClaim](
	[PKey] [uniqueidentifier] NOT NULL,
	[SavClaimGroupId] [uniqueidentifier] NULL,
	[SavComplaintCategoryId] [uniqueidentifier] NULL,
	[SavComplaintTypeId] [uniqueidentifier] NULL,
	[StatusValidityDate] [datetime] NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[CfgSupplierId] [uniqueidentifier] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[PrjMarketId] [uniqueidentifier] NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[DocDate] [datetime] NULL,
	[InterventionDate] [datetime] NULL,
	[ClosingDate] [datetime] NULL,
	[StrStatusId] [uniqueidentifier] NULL,
	[StrEntityId] [uniqueidentifier] NULL,
	[PriceHT] [decimal](26, 6) NULL,
	[VatRatio] [decimal](26, 6) NULL,
	[PriceTTC] [decimal](26, 6) NULL,
	[StlCategoryId] [uniqueidentifier] NULL,
	[StlCurrencyId] [uniqueidentifier] NULL,
	[PrjMarketClaimId] [uniqueidentifier] NULL,
	[CfgClaimManagerId] [uniqueidentifier] NULL,
	[CfgProjectZoneId] [uniqueidentifier] NULL,
	[CfgProjectZone_X] [float] NULL,
	[CfgProjectZone_Y] [float] NULL,
 CONSTRAINT [SavClaim_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[SavClaimGroup](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[DocDate] [datetime] NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[ClosingDate] [smalldatetime] NULL,
	[IsInReception] [bit] NULL,
	[SavOriginId] [uniqueidentifier] NULL,
	[DocRefDate] [datetime] NULL,
	[DocRef] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[WarrantyEndDate] [datetime] NULL,
	[CfgClaimsManagerId] [uniqueidentifier] NULL,
	[ComFolderId] [uniqueidentifier] NULL,
	[StkVocationId] [uniqueidentifier] NULL,
	[RntFolderId] [uniqueidentifier] NULL,
	[CfgCustomerId] [uniqueidentifier] NULL,
	[StkItemId] [uniqueidentifier] NULL,
	[CfgTrancheId] [uniqueidentifier] NULL,
	[CfgCompanyId] [uniqueidentifier] NULL,
	[TransmissionDate] [datetime] NULL,
	[IsIntern] [bit] NULL,
	[CfgTierTriggerId] [uniqueidentifier] NULL,
 CONSTRAINT [SavClaimGroup_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[SavComplaintCategory](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[StlCategoryId] [uniqueidentifier] NULL,
	[Notes] [nvarchar](500) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[IsForService] [bit] NULL,
 CONSTRAINT [PK_SavComplaintCategory] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[SavComplaintType](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[SavComplaintCategoryId] [uniqueidentifier] NULL,
	[ResolutionDelay] [int] NULL,
	[Notes] [nvarchar](500) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [PK_SavComplaintType] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[SavOrigin](
	[PKey] [uniqueidentifier] NOT NULL,
	[Sorting] [int] NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [SavOrigin_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StkBuilding](
	[PKey] [uniqueidentifier] NOT NULL,
	[Sorting] [int] NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [StkBuilding_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
CREATE NONCLUSTERED INDEX [_dta_index_StkBuilding_5_434816611__K1_2_3_4] ON [dbo].[StkBuilding]
(
	[PKey] ASC
)
INCLUDE([Sorting],[Code],[Description]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StkCostCategory](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [StkCostCategory_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StkExpense](
	[Pkey] [uniqueidentifier] NOT NULL,
	[Date] [datetime] NULL,
	[Amount] [decimal](26, 6) NULL,
	[IsValidated] [bit] NOT NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[CfgSupplierId] [uniqueidentifier] NULL,
	[StkExpenseTypeId] [uniqueidentifier] NULL,
	[StkItemId] [uniqueidentifier] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [StkExpense_PK] PRIMARY KEY CLUSTERED 
(
	[Pkey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
ALTER TABLE [dbo].[StkExpense] ADD  DEFAULT ((0)) FOR [IsValidated]
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StkExpenseType](
	[Pkey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[StlCategoryId] [uniqueidentifier] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[PrjMarketTypeId] [uniqueidentifier] NULL,
 CONSTRAINT [StkExpenseType_PK] PRIMARY KEY CLUSTERED 
(
	[Pkey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StkFacade](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [StkFacade_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StkFloor](
	[PKey] [uniqueidentifier] NOT NULL,
	[Sorting] [int] NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [StkFloor_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StkItem](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Address] [nvarchar](500) COLLATE French_CI_AS NULL,
	[Area] [decimal](26, 6) NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[ComSaleUnitCost] [decimal](26, 6) NULL,
	[ComSaleMinAdvance] [decimal](26, 6) NULL,
	[ComSalePrice] [decimal](26, 6) NULL,
	[SalePrice] [decimal](26, 6) NULL,
	[Contenance] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[IsUnderItem] [bit] NULL,
	[AreaNetOld] [decimal](26, 6) NULL,
	[AreaFloor] [decimal](26, 6) NULL,
	[AlwaysAffected] [bit] NULL,
	[ElectricityRef] [nvarchar](50) COLLATE French_CI_AS NULL,
	[ElectricityMeter] [nvarchar](50) COLLATE French_CI_AS NULL,
	[ElectricityMeterIndex] [nvarchar](50) COLLATE French_CI_AS NULL,
	[ElectricityMeterPolice] [nvarchar](50) COLLATE French_CI_AS NULL,
	[GasMeter] [nvarchar](50) COLLATE French_CI_AS NULL,
	[GasMeterIndex] [nvarchar](50) COLLATE French_CI_AS NULL,
	[GasMeterPolice] [nvarchar](50) COLLATE French_CI_AS NULL,
	[WaterMeterPolice] [nvarchar](50) COLLATE French_CI_AS NULL,
	[WaterMeter] [nvarchar](50) COLLATE French_CI_AS NULL,
	[WaterMeterIndex] [nvarchar](50) COLLATE French_CI_AS NULL,
	[DocDate] [datetime] NULL,
	[Anomalies] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[Cos] [decimal](26, 6) NULL,
	[Cuf] [decimal](26, 6) NULL,
	[AlinmentStop] [int] NULL,
	[NeighborStop] [int] NULL,
	[MaxHeight] [int] NULL,
	[RegistryDate] [smalldatetime] NULL,
	[RegistryNumber] [nvarchar](255) COLLATE French_CI_AS NULL,
	[RegistryPlace] [nvarchar](255) COLLATE French_CI_AS NULL,
	[ImmatriculationDate] [smalldatetime] NULL,
	[ImmatriculationCPF] [nvarchar](255) COLLATE French_CI_AS NULL,
	[PurchaseDate] [smalldatetime] NULL,
	[PurchaseUnitCost] [decimal](26, 6) NULL,
	[PurchaseCost] [decimal](26, 6) NULL,
	[AmountHT] [decimal](26, 6) NULL,
	[CautionNumber] [int] NULL,
	[ParentId] [uniqueidentifier] NULL,
	[StkItemTypeId] [uniqueidentifier] NOT NULL,
	[StkItemCategoryId] [uniqueidentifier] NOT NULL,
	[CfgSupplierId] [uniqueidentifier] NULL,
	[StkCostCategoryId] [uniqueidentifier] NULL,
	[StkVocationId] [uniqueidentifier] NOT NULL,
	[StkKindId] [uniqueidentifier] NOT NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[ComFolderId] [uniqueidentifier] NULL,
	[StkPresentationTypeId] [uniqueidentifier] NULL,
	[WaterRef] [nvarchar](255) COLLATE French_CI_AS NULL,
	[GasRef] [nvarchar](255) COLLATE French_CI_AS NULL,
	[TitleNumberMother] [nvarchar](255) COLLATE French_CI_AS NULL,
	[TitleNumber] [nvarchar](255) COLLATE French_CI_AS NULL,
	[TitleName] [nvarchar](255) COLLATE French_CI_AS NULL,
	[TitleDate] [smalldatetime] NULL,
	[TitleNotes] [nvarchar](255) COLLATE French_CI_AS NULL,
	[StkFloorId] [uniqueidentifier] NULL,
	[StkBuildingId] [uniqueidentifier] NULL,
	[StkOrientationId] [uniqueidentifier] NULL,
	[CfgCompanyId] [uniqueidentifier] NULL,
	[CfgProjectId] [uniqueidentifier] NULL,
	[StkHierarchyId] [uniqueidentifier] NULL,
	[DeliveryDate] [datetime] NULL,
	[PropertyCalled] [nvarchar](255) COLLATE French_CI_AS NULL,
	[FractionDivided] [nvarchar](255) COLLATE French_CI_AS NULL,
	[DividedPart] [decimal](26, 6) NULL,
	[UndividedPart] [decimal](26, 6) NULL,
	[Vat] [decimal](26, 6) NULL,
	[AreaCos] [decimal](26, 6) NULL,
	[AreaCuf] [decimal](26, 6) NULL,
	[ComSaleUnitCostFloor] [decimal](26, 6) NULL,
	[SalePriceFloor] [decimal](26, 6) NULL,
	[ComSalePriceFloor] [decimal](26, 6) NULL,
	[IsUnderItemOrigin] [bit] NULL,
	[FractionDividedLand] [nvarchar](255) COLLATE French_CI_AS NULL,
	[DiscountMax] [decimal](26, 6) NULL,
	[EquipementsCoef] [decimal](26, 6) NULL,
	[EquipementsArea] [decimal](26, 6) NULL,
	[SalableArea] [decimal](26, 6) NULL,
	[CoefSaleAbleArea] [decimal](26, 6) NULL,
	[LndVocationId] [uniqueidentifier] NULL,
	[LndLevelId] [uniqueidentifier] NULL,
	[CfgMiddlemanId] [uniqueidentifier] NULL,
	[ProspectionUnitCost] [decimal](26, 6) NULL,
	[ProspectionPrice] [decimal](26, 6) NULL,
	[ProspectionPriceProposed] [decimal](26, 6) NULL,
	[UnitCostNonBuiltPrice] [decimal](26, 6) NULL,
	[Owner] [nvarchar](255) COLLATE French_CI_AS NULL,
	[GmapLink] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[CfgLawyerId] [uniqueidentifier] NULL,
	[AdmCountryId] [uniqueidentifier] NULL,
	[AdmCityId] [uniqueidentifier] NULL,
	[LndLandId] [uniqueidentifier] NULL,
	[StkFacadeId] [uniqueidentifier] NULL,
	[ComSaleUnitCostFloorHT] [decimal](26, 6) NULL,
	[ComSaleUnitCostHT] [decimal](26, 6) NULL,
	[SynFolderId] [uniqueidentifier] NULL,
	[GroupLandQuota] [nvarchar](255) COLLATE French_CI_AS NULL,
	[RntFolderId] [uniqueidentifier] NULL,
	[SalePriceHT] [decimal](26, 6) NULL,
	[ComSalePriceHT] [decimal](26, 6) NULL,
	[SalePriceFloorHT] [decimal](26, 6) NULL,
	[ComSalePriceFloorHT] [decimal](26, 6) NULL,
	[IsAvailableForRentInWebSite] [bit] NULL,
	[IsAvailableForSaleInWebSite] [bit] NULL,
	[CfgCompanySyndicId] [uniqueidentifier] NULL,
	[BuildingPermissionDate] [datetime] NULL,
	[IsTitled] [bit] NULL,
	[InteriorArea] [decimal](26, 6) NULL,
	[WallsArea] [decimal](26, 6) NULL,
	[CommonPartsArea] [decimal](26, 6) NULL,
	[DiscountMax2] [decimal](26, 6) NULL,
	[CfgOwnerId] [uniqueidentifier] NULL,
	[StrEntityId] [uniqueidentifier] NULL,
	[MapLatitude] [decimal](20, 16) NULL,
	[MapLongitude] [decimal](20, 16) NULL,
	[CommissionRate] [decimal](20, 16) NULL,
	[RntPeriodicityId] [uniqueidentifier] NULL,
	[IsReversmentBasedOnPayment] [bit] NULL,
	[PrecisionActDate] [datetime] NULL,
	[PossessionDate] [datetime] NULL,
	[LastSettlementDate] [datetime] NULL,
	[ImmatriculationDateDeposit] [datetime] NULL,
	[ImmatriculationDateWithdrawal] [datetime] NULL,
	[StrStatusId] [uniqueidentifier] NULL,
	[StudyReportDate] [datetime] NULL,
	[ReservationDate] [datetime] NULL,
	[ReservationOwnerDate] [datetime] NULL,
	[ReservationRegistryDate] [datetime] NULL,
	[AgreementSavingDate] [datetime] NULL,
	[AgreementSavingOwnerDate] [datetime] NULL,
	[AgreementRegistryDate] [datetime] NULL,
	[CreditContractSignatureDate] [datetime] NULL,
	[CreditContractSignatureBankDate] [datetime] NULL,
	[CreditContractRegistryDate] [datetime] NULL,
	[DepositCPF1Date] [datetime] NULL,
	[OwnershipCertificate1Date] [datetime] NULL,
	[RaisedHandDate] [datetime] NULL,
	[RaisedHandRegistryDate] [datetime] NULL,
	[RaisedHand1Date] [datetime] NULL,
	[RaisedHand1RegistryDate] [datetime] NULL,
	[DepositCPFDate] [datetime] NULL,
	[OwnershipCertificateDate] [datetime] NULL,
	[AreaEquipmentTransferred] [decimal](26, 6) NULL,
	[AreaRegularized] [decimal](26, 6) NULL,
	[AreaCeded] [decimal](26, 6) NULL,
	[IsFinished] [bit] NULL,
	[CommerceCoef] [decimal](26, 6) NULL,
	[CommerceArea] [decimal](26, 6) NULL,
	[OfficeCoef] [decimal](26, 6) NULL,
	[OfficeArea] [decimal](26, 6) NULL,
	[HabitatArea] [decimal](26, 6) NULL,
	[UndergroundParkingArea] [decimal](26, 6) NULL,
	[UndergroundParkingCoef] [decimal](26, 6) NULL,
 CONSTRAINT [StkItem_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
CREATE NONCLUSTERED INDEX [_dta_index_StkItem_5_1389248004__K47_14] ON [dbo].[StkItem]
(
	[ParentId] ASC
)
INCLUDE([SalePrice]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_StkItem_5_1389248004__K47_K128_K113_K112_K121_K140_K116_K48_K49_K55_K72_K73_K53_K74_K64_K75_1_2_3_4_5_6_11_12_13_14_] ON [dbo].[StkItem]
(
	[ParentId] ASC,
	[CfgCompanySyndicId] ASC,
	[AdmCityId] ASC,
	[AdmCountryId] ASC,
	[RntFolderId] ASC,
	[RntPeriodicityId] ASC,
	[StkFacadeId] ASC,
	[StkItemTypeId] ASC,
	[StkItemCategoryId] ASC,
	[StkVocationId] ASC,
	[StkFloorId] ASC,
	[StkBuildingId] ASC,
	[StkCostCategoryId] ASC,
	[StkOrientationId] ASC,
	[StkPresentationTypeId] ASC,
	[CfgCompanyId] ASC
)
INCLUDE([PKey],[Code],[Description],[Address],[Area],[Notes],[ComSaleUnitCost],[ComSaleMinAdvance],[ComSalePrice],[SalePrice],[Contenance],[IsUnderItem],[AreaFloor],[AlwaysAffected],[ElectricityRef],[ElectricityMeter],[ElectricityMeterIndex],[ElectricityMeterPolice],[GasMeter],[GasMeterIndex],[GasMeterPolice],[WaterMeterPolice],[WaterMeter],[WaterMeterIndex],[DocDate],[Anomalies],[Cos],[Cuf],[AlinmentStop],[NeighborStop],[MaxHeight],[RegistryDate],[RegistryNumber],[RegistryPlace],[ImmatriculationDate],[ImmatriculationCPF],[PurchaseDate],[PurchaseUnitCost],[PurchaseCost],[AmountHT],[CautionNumber],[CfgSupplierId],[StkKindId],[UserCreate],[UserCreateDateTime],[UserUpdate],[UserUpdateDateTime],[ComFolderId],[WaterRef],[GasRef],[TitleNumberMother],[TitleNumber],[TitleName],[TitleDate],[TitleNotes],[StkHierarchyId],[PropertyCalled],[FractionDivided],[DividedPart],[UndividedPart],[Vat],[AreaCos],[AreaCuf],[ComSaleUnitCostFloor],[SalePriceFloor],[ComSalePriceFloor],[IsUnderItemOrigin],[FractionDividedLand],[DiscountMax],[EquipementsArea],[CfgLawyerId],[ComSaleUnitCostFloorHT],[ComSaleUnitCostHT],[SynFolderId],[GroupLandQuota],[SalePriceHT],[ComSalePriceHT],[SalePriceFloorHT],[ComSalePriceFloorHT],[IsAvailableForRentInWebSite],[IsAvailableForSaleInWebSite],[BuildingPermissionDate],[IsTitled],[InteriorArea],[WallsArea],[CommonPartsArea],[DiscountMax2],[CfgOwnerId],[StrEntityId],[MapLatitude],[MapLongitude],[CommissionRate]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_StkItem_5_1389248004__K47_K2_K1_K130_3_5] ON [dbo].[StkItem]
(
	[ParentId] ASC,
	[Code] ASC,
	[PKey] ASC,
	[IsTitled] ASC
)
INCLUDE([Description],[Area]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_StkItem_5_1389248004__K78_K136_K56_K92_K55_K130_K128_K113_K112_K121_K140_K116_K48_K49_K72_K73_1_2_3_4_5_6_11_12_13_] ON [dbo].[StkItem]
(
	[StkHierarchyId] ASC,
	[StrEntityId] ASC,
	[StkKindId] ASC,
	[IsUnderItemOrigin] ASC,
	[StkVocationId] ASC,
	[IsTitled] ASC,
	[CfgCompanySyndicId] ASC,
	[AdmCityId] ASC,
	[AdmCountryId] ASC,
	[RntFolderId] ASC,
	[RntPeriodicityId] ASC,
	[StkFacadeId] ASC,
	[StkItemTypeId] ASC,
	[StkItemCategoryId] ASC,
	[StkFloorId] ASC,
	[StkBuildingId] ASC
)
INCLUDE([PKey],[Code],[Description],[Address],[Area],[Notes],[ComSaleUnitCost],[ComSaleMinAdvance],[ComSalePrice],[SalePrice],[Contenance],[IsUnderItem],[AreaFloor],[AlwaysAffected],[ElectricityRef],[ElectricityMeter],[ElectricityMeterIndex],[ElectricityMeterPolice],[GasMeter],[GasMeterIndex],[GasMeterPolice],[WaterMeterPolice],[WaterMeter],[WaterMeterIndex],[DocDate],[Anomalies],[Cos],[Cuf],[AlinmentStop],[NeighborStop],[MaxHeight],[RegistryDate],[RegistryNumber],[RegistryPlace],[ImmatriculationDate],[ImmatriculationCPF],[PurchaseDate],[PurchaseUnitCost],[PurchaseCost],[AmountHT],[CautionNumber],[ParentId],[CfgSupplierId],[StkCostCategoryId],[UserCreate],[UserCreateDateTime],[UserUpdate],[UserUpdateDateTime],[ComFolderId],[StkPresentationTypeId],[WaterRef],[GasRef],[TitleNumberMother],[TitleNumber],[TitleName],[TitleDate],[TitleNotes],[StkOrientationId],[CfgCompanyId],[PropertyCalled],[FractionDivided],[DividedPart],[UndividedPart],[Vat],[AreaCos],[AreaCuf],[ComSaleUnitCostFloor],[SalePriceFloor],[ComSalePriceFloor],[FractionDividedLand],[DiscountMax],[EquipementsArea],[CfgLawyerId],[ComSaleUnitCostFloorHT],[ComSaleUnitCostHT],[SynFolderId],[GroupLandQuota],[SalePriceHT],[ComSalePriceHT],[SalePriceFloorHT],[ComSalePriceFloorHT],[IsAvailableForRentInWebSite],[IsAvailableForSaleInWebSite],[BuildingPermissionDate],[InteriorArea],[WallsArea],[CommonPartsArea],[DiscountMax2],[CfgOwnerId],[MapLatitude],[MapLongitude],[CommissionRate]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_StkItem_5_1936725952__K1_K2_3_78] ON [dbo].[StkItem]
(
	[PKey] ASC,
	[Code] ASC
)
INCLUDE([Description],[StkHierarchyId]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_StkItem_5_1936725952__K1_K3] ON [dbo].[StkItem]
(
	[PKey] ASC,
	[Description] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_StkItem_5_1936725952__K1_K78] ON [dbo].[StkItem]
(
	[PKey] ASC,
	[StkHierarchyId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_StkItem_5_1936725952__K1_K78_K49_K48_K64_K2_3] ON [dbo].[StkItem]
(
	[PKey] ASC,
	[StkHierarchyId] ASC,
	[StkItemCategoryId] ASC,
	[StkItemTypeId] ASC,
	[StkPresentationTypeId] ASC,
	[Code] ASC
)
INCLUDE([Description]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_StkItem_5_1936725952__K121_K47_K55_K78_K1] ON [dbo].[StkItem]
(
	[RntFolderId] ASC,
	[ParentId] ASC,
	[StkVocationId] ASC,
	[StkHierarchyId] ASC,
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_StkItem_5_1936725952__K121_K47_K55_K78_K1_K64_K48_K49_K2_3_6_45] ON [dbo].[StkItem]
(
	[RntFolderId] ASC,
	[ParentId] ASC,
	[StkVocationId] ASC,
	[StkHierarchyId] ASC,
	[PKey] ASC,
	[StkPresentationTypeId] ASC,
	[StkItemTypeId] ASC,
	[StkItemCategoryId] ASC,
	[Code] ASC
)
INCLUDE([Description],[Notes],[AmountHT]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_StkItem_5_1936725952__K121_K55_K47_K1] ON [dbo].[StkItem]
(
	[RntFolderId] ASC,
	[StkVocationId] ASC,
	[ParentId] ASC,
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_StkItem_5_1936725952__K47_K119_K1_K78_K49_K48_K64_K55_K2_3_6_45] ON [dbo].[StkItem]
(
	[ParentId] ASC,
	[SynFolderId] ASC,
	[PKey] ASC,
	[StkHierarchyId] ASC,
	[StkItemCategoryId] ASC,
	[StkItemTypeId] ASC,
	[StkPresentationTypeId] ASC,
	[StkVocationId] ASC,
	[Code] ASC
)
INCLUDE([Description],[Notes],[AmountHT]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_StkItem_5_1936725952__K47_K3_K1_K121] ON [dbo].[StkItem]
(
	[ParentId] ASC,
	[Description] ASC,
	[PKey] ASC,
	[RntFolderId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_StkItem_5_1936725952__K47_K55_K119_K78_K1_K64_K49_K48_K2_3_6_45] ON [dbo].[StkItem]
(
	[ParentId] ASC,
	[StkVocationId] ASC,
	[SynFolderId] ASC,
	[StkHierarchyId] ASC,
	[PKey] ASC,
	[StkPresentationTypeId] ASC,
	[StkItemCategoryId] ASC,
	[StkItemTypeId] ASC,
	[Code] ASC
)
INCLUDE([Description],[Notes],[AmountHT]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_StkItem_5_1936725952__K47_K64_K48_K49_K1_K78_K121_K55_K2_3_6_45] ON [dbo].[StkItem]
(
	[ParentId] ASC,
	[StkPresentationTypeId] ASC,
	[StkItemTypeId] ASC,
	[StkItemCategoryId] ASC,
	[PKey] ASC,
	[StkHierarchyId] ASC,
	[RntFolderId] ASC,
	[StkVocationId] ASC,
	[Code] ASC
)
INCLUDE([Description],[Notes],[AmountHT]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_StkItem_5_1936725952__K47_K78_K55_K121_K64_K48_K49_K1_K2_3_6_45] ON [dbo].[StkItem]
(
	[ParentId] ASC,
	[StkHierarchyId] ASC,
	[StkVocationId] ASC,
	[RntFolderId] ASC,
	[StkPresentationTypeId] ASC,
	[StkItemTypeId] ASC,
	[StkItemCategoryId] ASC,
	[PKey] ASC,
	[Code] ASC
)
INCLUDE([Description],[Notes],[AmountHT]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_StkItem_5_1936725952__K78_K1_K47_K64_K48_K49_K121_K55_K2_3_6_45] ON [dbo].[StkItem]
(
	[StkHierarchyId] ASC,
	[PKey] ASC,
	[ParentId] ASC,
	[StkPresentationTypeId] ASC,
	[StkItemTypeId] ASC,
	[StkItemCategoryId] ASC,
	[RntFolderId] ASC,
	[StkVocationId] ASC,
	[Code] ASC
)
INCLUDE([Description],[Notes],[AmountHT]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_StkItem_6_1389248004__K1_K2_3_78] ON [dbo].[StkItem]
(
	[PKey] ASC,
	[Code] ASC
)
INCLUDE([Description],[StkHierarchyId]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_StkItem_6_1389248004__K47_K63_K1_K78_K49_K48_K64_K72_K73_K92_K55_K16_K2_3_5_11_13_14_68_85_118_123] ON [dbo].[StkItem]
(
	[ParentId] ASC,
	[ComFolderId] ASC,
	[PKey] ASC,
	[StkHierarchyId] ASC,
	[StkItemCategoryId] ASC,
	[StkItemTypeId] ASC,
	[StkPresentationTypeId] ASC,
	[StkFloorId] ASC,
	[StkBuildingId] ASC,
	[IsUnderItemOrigin] ASC,
	[StkVocationId] ASC,
	[IsUnderItem] ASC,
	[Code] ASC
)
INCLUDE([Description],[Area],[ComSaleUnitCost],[ComSalePrice],[SalePrice],[TitleNumber],[Vat],[ComSaleUnitCostHT],[ComSalePriceHT]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_StkItem_6_1389248004__K47_K92_K5] ON [dbo].[StkItem]
(
	[ParentId] ASC,
	[IsUnderItemOrigin] ASC,
	[Area] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_StkItem_6_1389248004__K78_K1] ON [dbo].[StkItem]
(
	[StkHierarchyId] ASC,
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_StkItem_6_1389248004__K78_K1_3_49] ON [dbo].[StkItem]
(
	[StkHierarchyId] ASC,
	[PKey] ASC
)
INCLUDE([Description],[StkItemCategoryId]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_StkItem_7_1936725952__K16_K63_K55_K64_K78_K1_K73_K72_K48_K49_K47_K92_K2_3_5_11_13_14_68_85_118_123] ON [dbo].[StkItem]
(
	[IsUnderItem] ASC,
	[ComFolderId] ASC,
	[StkVocationId] ASC,
	[StkPresentationTypeId] ASC,
	[StkHierarchyId] ASC,
	[PKey] ASC,
	[StkBuildingId] ASC,
	[StkFloorId] ASC,
	[StkItemTypeId] ASC,
	[StkItemCategoryId] ASC,
	[ParentId] ASC,
	[IsUnderItemOrigin] ASC,
	[Code] ASC
)
INCLUDE([Description],[Area],[ComSaleUnitCost],[ComSalePrice],[SalePrice],[TitleNumber],[Vat],[ComSaleUnitCostHT],[ComSalePriceHT]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_StkItem_7_1936725952__K47_K63_K1_K2_3_68] ON [dbo].[StkItem]
(
	[ParentId] ASC,
	[ComFolderId] ASC,
	[PKey] ASC,
	[Code] ASC
)
INCLUDE([Description],[TitleNumber]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_StkItem_7_1936725952__K47_K63_K1_K2_K92_K5_K78_K73_K72_K64_K48_K49_K55_K16_K11_K14_3_13_68_85_118_123] ON [dbo].[StkItem]
(
	[ParentId] ASC,
	[ComFolderId] ASC,
	[PKey] ASC,
	[Code] ASC,
	[IsUnderItemOrigin] ASC,
	[Area] ASC,
	[StkHierarchyId] ASC,
	[StkBuildingId] ASC,
	[StkFloorId] ASC,
	[StkPresentationTypeId] ASC,
	[StkItemTypeId] ASC,
	[StkItemCategoryId] ASC,
	[StkVocationId] ASC,
	[IsUnderItem] ASC,
	[ComSaleUnitCost] ASC,
	[SalePrice] ASC
)
INCLUDE([Description],[ComSalePrice],[TitleNumber],[Vat],[ComSaleUnitCostHT],[ComSalePriceHT]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_StkItem_7_1936725952__K47_K92_K5] ON [dbo].[StkItem]
(
	[ParentId] ASC,
	[IsUnderItemOrigin] ASC,
	[Area] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_StkItem_7_1936725952__K78_K1] ON [dbo].[StkItem]
(
	[StkHierarchyId] ASC,
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_StkItem_7_1936725952__K78_K64_K1_K73_K48_K49_72] ON [dbo].[StkItem]
(
	[StkHierarchyId] ASC,
	[StkPresentationTypeId] ASC,
	[PKey] ASC,
	[StkBuildingId] ASC,
	[StkItemTypeId] ASC,
	[StkItemCategoryId] ASC
)
INCLUDE([StkFloorId]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StkItemCategory](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[ForUnderItem] [bit] NULL,
	[StkKindId] [uniqueidentifier] NOT NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[WithAreaSale] [bit] NULL,
	[ReportDescription] [nvarchar](500) COLLATE French_CI_AS NULL,
	[StockVariationCoef] [decimal](26, 6) NULL,
	[Notes] [nvarchar](500) COLLATE French_CI_AS NULL,
	[StlCategoryId] [uniqueidentifier] NULL,
 CONSTRAINT [StkItemCategory_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StkItemType](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[ForUnderItem] [bit] NULL,
	[StkKindId] [uniqueidentifier] NOT NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[WithAreaSale] [bit] NULL,
	[ReportDescription] [nvarchar](500) COLLATE French_CI_AS NULL,
	[StlCategoryId] [uniqueidentifier] NULL,
	[StockVariationCoef] [decimal](26, 6) NULL,
	[Notes] [nvarchar](500) COLLATE French_CI_AS NULL,
 CONSTRAINT [StkItemType_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StkKind](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [StkKind_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StkLeveling](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [StkLeveling_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StkLevelingLine](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Sorting] [int] NULL,
	[StkLevelId] [uniqueidentifier] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [StkLevelingLine_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StkOrientation](
	[PKey] [uniqueidentifier] NOT NULL,
	[Sorting] [int] NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [StkOrientation_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StkPresentationType](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[ColorName] [nvarchar](255) COLLATE French_CI_AS NULL,
	[StkHierarchyId] [uniqueidentifier] NULL,
 CONSTRAINT [StkPresentationType_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_StkPresentationType_5_1789249429__K1_K3] ON [dbo].[StkPresentationType]
(
	[PKey] ASC,
	[Description] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StkTrancheHeading](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Sorting] [int] NULL,
	[Notes] [nvarchar](500) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [StkTrancheHeading_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StkVocation](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [StkVocation_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StlAccount](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[CfgBankId] [uniqueidentifier] NULL,
	[StlSettlementKindId] [uniqueidentifier] NULL,
	[BankCode] [nvarchar](5) COLLATE French_CI_AS NULL,
	[AgencyCode] [nvarchar](5) COLLATE French_CI_AS NULL,
	[AccountNumber] [nvarchar](255) COLLATE French_CI_AS NULL,
	[AccountKey] [nvarchar](5) COLLATE French_CI_AS NULL,
	[Iban] [nvarchar](50) COLLATE French_CI_AS NULL,
	[CodeBic] [nvarchar](50) COLLATE French_CI_AS NULL,
	[AgencyAddress] [nvarchar](255) COLLATE French_CI_AS NULL,
	[CompanyAddress] [nvarchar](255) COLLATE French_CI_AS NULL,
	[CreationPlace] [nvarchar](255) COLLATE French_CI_AS NULL,
	[CurrencyCode] [nvarchar](50) COLLATE French_CI_AS NULL,
	[CurrencyLabel] [nvarchar](255) COLLATE French_CI_AS NULL,
	[CfgCompanyId] [uniqueidentifier] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[StlCurrencyId] [uniqueidentifier] NULL,
 CONSTRAINT [PK_StlAccount] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StlAccountPosition](
	[PKey] [uniqueidentifier] NOT NULL,
	[PositionDate] [smalldatetime] NOT NULL,
	[Position] [decimal](26, 6) NOT NULL,
	[StlAccountId] [uniqueidentifier] NOT NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[Notes] [nvarchar](500) COLLATE French_CI_AS NULL,
	[PositionCalculated] [decimal](26, 6) NULL,
	[StrEntityId] [uniqueidentifier] NULL,
 CONSTRAINT [PK_StlAccountPosition] PRIMARY KEY NONCLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StlAcountDeposit](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](500) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](500) COLLATE French_CI_AS NULL,
	[StartDate] [datetime] NULL,
	[EndDate] [datetime] NULL,
	[Notes] [nvarchar](500) COLLATE French_CI_AS NULL,
	[AdmUserId] [uniqueidentifier] NULL,
	[GrhEmployeeId] [uniqueidentifier] NULL,
	[GrhEmployeeClosingId] [uniqueidentifier] NULL,
	[StlDepositId] [uniqueidentifier] NULL,
	[StlDepositToId] [uniqueidentifier] NULL,
	[StlAccountToId] [uniqueidentifier] NULL,
	[StrEntityId] [uniqueidentifier] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[AmountOpening] [decimal](26, 6) NULL,
 CONSTRAINT [StlAcountDeposit_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StlCategory](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[CodeCountable] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[IsCredit] [bit] NULL,
	[ParentId] [uniqueidentifier] NULL,
	[IsWithoutPrevision] [bit] NULL,
	[StlAccountId] [uniqueidentifier] NULL,
	[Sorting] [int] NULL,
	[IsCharge] [bit] NULL,
	[ActAccountId] [uniqueidentifier] NULL,
	[ActAccountForPaymentId] [uniqueidentifier] NULL,
	[IsProduct] [bit] NULL,
 CONSTRAINT [StlCategory_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
CREATE NONCLUSTERED INDEX [_dta_index_StlCategory_10_1765581328__K1_2_4] ON [dbo].[StlCategory]
(
	[PKey] ASC
)
INCLUDE([Code],[Description]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StlCurrency](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[CodeIso] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Notes] [nvarchar](500) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[DescriptionLong] [nvarchar](500) COLLATE French_CI_AS NULL,
	[CurrencyFormat] [nvarchar](255) COLLATE French_CI_AS NULL,
	[CurrencySymbol] [nvarchar](255) COLLATE French_CI_AS NULL,
	[CurrencyDecimalDigits] [int] NULL,
 CONSTRAINT [StlCurrency_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StlDeductionType](
	[PKey] [uniqueidentifier] NOT NULL,
	[Sorting] [int] NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[RASYear] [int] NULL,
	[RASValue] [decimal](26, 6) NULL,
	[IsDeduction] [bit] NULL,
	[IsDeductionTva] [bit] NULL,
	[IsOther] [bit] NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [StlDeductionType_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StlDeposit](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[CfgCompanyId] [uniqueidentifier] NULL,
	[Notes] [nvarchar](500) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[IsBank] [bit] NULL,
	[IsSupplier] [bit] NULL,
	[IsCustomer] [bit] NULL,
 CONSTRAINT [StlDeposit_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StlDocument](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[DocDate] [datetime] NULL,
	[MaturityDate] [datetime] NULL,
	[AmountHT] [decimal](26, 6) NULL,
	[AmountVat] [decimal](26, 6) NULL,
	[AmountTTC] [decimal](26, 6) NULL,
	[AmountToPay] [decimal](26, 6) NULL,
	[AmountRegul] [decimal](26, 6) NULL,
	[FiscalStamp] [decimal](26, 6) NULL,
	[DocType] [nvarchar](255) COLLATE French_CI_AS NULL,
	[DocRef] [nvarchar](255) COLLATE French_CI_AS NULL,
	[ReferenceType] [nvarchar](255) COLLATE French_CI_AS NULL,
	[SupplierDescription] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[IsCredit] [bit] NULL,
	[CfgTierId] [uniqueidentifier] NULL,
	[AdmModuleId] [uniqueidentifier] NULL,
	[CfgCompanyId] [uniqueidentifier] NULL,
	[StrEntityId] [uniqueidentifier] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[LinesCount] [int] NULL,
	[PersonToContact] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Notes1] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[AmountRefundedHT] [decimal](26, 6) NULL,
	[AmountRefundedVat] [decimal](26, 6) NULL,
	[AmountRefundedTTC] [decimal](26, 6) NULL,
	[TotalHT] [decimal](26, 6) NULL,
	[TotalVat] [decimal](26, 6) NULL,
	[TotalTTC] [decimal](26, 6) NULL,
	[CodeProforma] [nvarchar](255) COLLATE French_CI_AS NULL,
	[OrderCode] [nvarchar](255) COLLATE French_CI_AS NULL,
	[IsImported] [bit] NULL,
	[IsAccounted] [bit] NULL,
	[PaymentAuthorizationDate] [datetime] NULL,
	[CfgDebtCollectorId] [uniqueidentifier] NULL,
	[StlPenaltyDocumentId] [uniqueidentifier] NULL,
	[StlRecoveryFolderId] [uniqueidentifier] NULL,
	[StlCurrencyId] [uniqueidentifier] NULL,
	[BuyFolderId] [uniqueidentifier] NULL,
 CONSTRAINT [StlDocument_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
CREATE NONCLUSTERED INDEX [_dta_index_StlDocument_5_1408060102__K1_2] ON [dbo].[StlDocument]
(
	[PKey] ASC
)
INCLUDE([Code]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_StlDocument_5_1408060102__K1_K17] ON [dbo].[StlDocument]
(
	[PKey] ASC,
	[DocType] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_StlDocument_5_1408060102__K1_K17_K6] ON [dbo].[StlDocument]
(
	[PKey] ASC,
	[DocType] ASC,
	[AmountHT] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_StlDocument_5_1408060102__K1_K4_K17_K24_K22] ON [dbo].[StlDocument]
(
	[PKey] ASC,
	[DocDate] ASC,
	[DocType] ASC,
	[AdmModuleId] ASC,
	[IsCredit] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_StlDocument_5_1408060102__K17_K1] ON [dbo].[StlDocument]
(
	[DocType] ASC,
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_StlDocument_5_1408060102__K17_K1_K14] ON [dbo].[StlDocument]
(
	[DocType] ASC,
	[PKey] ASC,
	[AmountToPay] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_StlDocument_5_1408060102__K17_K22_K23_K1_K26_K24_K25_2_3_4_5_6_7_8_14_15_16_18_19_21_27_28_29_30_32_33_36_37_38_39_] ON [dbo].[StlDocument]
(
	[DocType] ASC,
	[IsCredit] ASC,
	[CfgTierId] ASC,
	[PKey] ASC,
	[StrEntityId] ASC,
	[AdmModuleId] ASC,
	[CfgCompanyId] ASC
)
INCLUDE([Code],[Description],[DocDate],[MaturityDate],[AmountHT],[AmountVat],[AmountTTC],[AmountToPay],[AmountRegul],[FiscalStamp],[DocRef],[ReferenceType],[Notes],[UserCreate],[UserCreateDateTime],[UserUpdate],[UserUpdateDateTime],[LinesCount],[PersonToContact],[Notes1],[AmountRefundedHT],[AmountRefundedVat],[AmountRefundedTTC],[TotalHT],[TotalVat],[TotalTTC],[CodeProforma],[OrderCode]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_StlDocument_5_1408060102__K17_K24_K22_1_5_8_16_23] ON [dbo].[StlDocument]
(
	[DocType] ASC,
	[AdmModuleId] ASC,
	[IsCredit] ASC
)
INCLUDE([PKey],[MaturityDate],[AmountTTC],[FiscalStamp],[CfgTierId]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_StlDocument_5_1408060102__K17_K24_K22_K23_1_5_8_16] ON [dbo].[StlDocument]
(
	[DocType] ASC,
	[AdmModuleId] ASC,
	[IsCredit] ASC,
	[CfgTierId] ASC
)
INCLUDE([PKey],[MaturityDate],[AmountTTC],[FiscalStamp]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_StlDocument_5_1408060102__K17_K6_K1] ON [dbo].[StlDocument]
(
	[DocType] ASC,
	[AmountHT] ASC,
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_StlDocument_5_1408060102__K22_K1_K17_K14] ON [dbo].[StlDocument]
(
	[IsCredit] ASC,
	[PKey] ASC,
	[DocType] ASC,
	[AmountToPay] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_StlDocument_5_1408060102__K4_K17_K24_K22_K1_K26_K25_K23_2_3_5_6_7_8_14_15_16_18_19_21_27_28_29_30_32_33_36_37_38_39_] ON [dbo].[StlDocument]
(
	[DocDate] ASC,
	[DocType] ASC,
	[AdmModuleId] ASC,
	[IsCredit] ASC,
	[PKey] ASC,
	[StrEntityId] ASC,
	[CfgCompanyId] ASC,
	[CfgTierId] ASC
)
INCLUDE([Code],[Description],[MaturityDate],[AmountHT],[AmountVat],[AmountTTC],[AmountToPay],[AmountRegul],[FiscalStamp],[DocRef],[ReferenceType],[Notes],[UserCreate],[UserCreateDateTime],[UserUpdate],[UserUpdateDateTime],[LinesCount],[PersonToContact],[Notes1],[AmountRefundedHT],[AmountRefundedVat],[AmountRefundedTTC],[TotalHT],[TotalVat],[TotalTTC],[CodeProforma],[OrderCode],[PaymentAuthorizationDate]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_StlDocument_6_1907537879__K1_2] ON [dbo].[StlDocument]
(
	[PKey] ASC
)
INCLUDE([Code]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_StlDocument_6_1907537879__K1_K14_K8_K17_16] ON [dbo].[StlDocument]
(
	[PKey] ASC,
	[AmountToPay] ASC,
	[AmountTTC] ASC,
	[DocType] ASC
)
INCLUDE([FiscalStamp]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_StlDocument_6_1907537879__K1_K26_6] ON [dbo].[StlDocument]
(
	[PKey] ASC,
	[StrEntityId] ASC
)
INCLUDE([AmountHT]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_StlDocument_6_1907537879__K1_K26_K4_K5_21] ON [dbo].[StlDocument]
(
	[PKey] ASC,
	[StrEntityId] ASC,
	[DocDate] ASC,
	[MaturityDate] ASC
)
INCLUDE([Notes]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StlInternalTransfer](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[DocDate] [datetime] NULL,
	[Amount] [decimal](26, 6) NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[CfgCompanyId] [uniqueidentifier] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[CfgCompanyToId] [uniqueidentifier] NULL,
	[StlAccountId] [uniqueidentifier] NULL,
	[StlAccountToId] [uniqueidentifier] NULL,
	[StlSettlementTypeId] [uniqueidentifier] NULL,
	[StlSettlementTypeToId] [uniqueidentifier] NULL,
	[Reference] [nvarchar](255) COLLATE French_CI_AS NULL,
	[ReferenceTo] [nvarchar](255) COLLATE French_CI_AS NULL,
	[StrEntityId] [uniqueidentifier] NULL,
	[StrStatusId] [uniqueidentifier] NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[CfgTierEmissionId] [uniqueidentifier] NULL,
	[CfgTierTransmissionId] [uniqueidentifier] NULL,
	[TransmissionDate] [datetime] NULL,
	[CfgTierReceptionId] [uniqueidentifier] NULL,
	[ReceptionDate] [datetime] NULL,
	[AmountTo] [decimal](26, 6) NULL,
 CONSTRAINT [StlInternalTransfer_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StlQuote](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[DocDate] [datetime] NULL,
	[StrEntityId] [uniqueidentifier] NULL,
	[CfgTrancheId] [uniqueidentifier] NULL,
	[StkItemId] [uniqueidentifier] NULL,
	[CfgProspectId] [uniqueidentifier] NULL,
	[ComProspectionId] [uniqueidentifier] NULL,
	[CfgCommercialId] [uniqueidentifier] NULL,
	[ComPaymentMethodId] [uniqueidentifier] NULL,
	[ComSettlementCategoryId] [uniqueidentifier] NULL,
	[RntPeriodicityId] [uniqueidentifier] NULL,
	[Area] [decimal](26, 6) NULL,
	[UnitCostHT] [decimal](26, 6) NULL,
	[VATRatio] [decimal](26, 6) NULL,
	[UnitCost] [decimal](26, 6) NULL,
	[ItemPriceHT] [decimal](26, 6) NULL,
	[ItemPrice] [decimal](26, 6) NULL,
	[UnderItemsPriceHT] [decimal](26, 6) NULL,
	[UnderItemsPrice] [decimal](26, 6) NULL,
	[PriceBeforeDiscountHT] [decimal](26, 6) NULL,
	[PriceBeforeDiscount] [decimal](26, 6) NULL,
	[Discount] [decimal](26, 6) NULL,
	[DiscountAmountHT] [decimal](26, 6) NULL,
	[DiscountAmount] [decimal](26, 6) NULL,
	[UnitCostAfterDiscountHT] [decimal](26, 6) NULL,
	[UnitCostAfterDiscount] [decimal](26, 6) NULL,
	[ItemPriceAfterDiscountHT] [decimal](26, 6) NULL,
	[ItemPriceAfterDiscount] [decimal](26, 6) NULL,
	[UnderItemsPriceAfterDiscountHT] [decimal](26, 6) NULL,
	[UnderItemsPriceAfterDiscount] [decimal](26, 6) NULL,
	[PriceAfterDiscountHT] [decimal](26, 6) NULL,
	[PriceAfterDiscount] [decimal](26, 6) NULL,
	[PeriodicityBeginDate] [datetime] NULL,
	[PeriodicityNumber] [int] NULL,
	[PeriodicityEndDate] [datetime] NULL,
	[AmountPeriodicity] [decimal](26, 6) NULL,
	[PeriodicityReconductionRatio] [decimal](26, 6) NULL,
	[PeriodicityReconductionFrequency] [int] NULL,
	[NextAugmentationDate] [datetime] NULL,
	[RentVatRatio] [decimal](26, 6) NULL,
	[RentMonthAmountHT] [decimal](26, 6) NULL,
	[RentMonthAmountTTC] [decimal](26, 6) NULL,
	[RentPeriodAmountHT] [decimal](26, 6) NULL,
	[RentPeriodAmountTTC] [decimal](26, 6) NULL,
	[RentYearAmountHT] [decimal](26, 6) NULL,
	[RentYearAmountTTC] [decimal](26, 6) NULL,
	[CautionMonthNumber] [decimal](26, 6) NULL,
	[CautionVatRatio] [decimal](26, 6) NULL,
	[CautionAmountHT] [decimal](26, 6) NULL,
	[CautionAmountTTC] [decimal](26, 6) NULL,
	[AdvanceMonthNumber] [decimal](26, 6) NULL,
	[CommissionMonthNumber] [decimal](26, 6) NULL,
	[RegistrationFees] [decimal](26, 6) NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[RntReconductionTypeId] [uniqueidentifier] NULL,
	[StkVocationId] [uniqueidentifier] NULL,
	[IsOldCustomer] [bit] NULL,
	[RomInterest] [decimal](26, 6) NULL,
	[AmountTotal] [decimal](26, 6) NULL,
	[DiscountTotal] [decimal](26, 6) NULL,
	[CommissionRate] [decimal](26, 6) NULL,
 CONSTRAINT [StlQuote_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StlQuoteItem](
	[PKey] [uniqueidentifier] NOT NULL,
	[StlQuoteId] [uniqueidentifier] NULL,
	[StkItemId] [uniqueidentifier] NULL,
	[StkItemTypeId] [uniqueidentifier] NULL,
	[StkItemCategoryId] [uniqueidentifier] NULL,
	[StkCostCategoryId] [uniqueidentifier] NULL,
	[ParentId] [uniqueidentifier] NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Contenance] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[Area] [decimal](26, 6) NULL,
	[UnitCostHT] [decimal](26, 6) NULL,
	[VATRatio] [decimal](26, 6) NULL,
	[UnitCost] [decimal](26, 6) NULL,
	[PriceBeforeDiscountHT] [decimal](26, 6) NULL,
	[PriceBeforeDiscount] [decimal](26, 6) NULL,
	[Discount] [decimal](26, 6) NULL,
	[DiscountAmountHT] [decimal](26, 6) NULL,
	[DiscountAmount] [decimal](26, 6) NULL,
	[UnitCostAfterDiscountHT] [decimal](26, 6) NULL,
	[UnitCostAfterDiscount] [decimal](26, 6) NULL,
	[PriceAfterDiscountHT] [decimal](26, 6) NULL,
	[PriceAfterDiscount] [decimal](26, 6) NULL,
	[AlwaysAffected] [bit] NULL,
	[IsUnderItem] [bit] NULL,
	[StkOrientationId] [uniqueidentifier] NULL,
	[StkFloorId] [uniqueidentifier] NULL,
	[StkBuildingId] [uniqueidentifier] NULL,
	[IsUnderItemOrigin] [bit] NULL,
	[IsTitled] [bit] NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[RentAmountHT] [decimal](26, 6) NULL,
	[RentAmountTTC] [decimal](26, 6) NULL,
 CONSTRAINT [StlQuoteItem_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StlRecoveryFolder](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[DocDate] [datetime] NULL,
	[ClosingDate] [datetime] NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[CfgCompanyId] [uniqueidentifier] NULL,
	[ComFolderId] [uniqueidentifier] NULL,
	[RntFolderId] [uniqueidentifier] NULL,
	[CfgCustomerId] [uniqueidentifier] NULL,
	[CfgDebtManagerId] [uniqueidentifier] NULL,
	[CfgDebtCollectorId] [uniqueidentifier] NULL,
	[StatusValidityDate] [datetime] NULL,
	[StrStatusId] [uniqueidentifier] NULL,
	[StrEntityId] [uniqueidentifier] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [StlRecoveryFolder_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StlSettlement](
	[PKey] [uniqueidentifier] NOT NULL,
	[DocDate] [datetime] NULL,
	[EcheanceDate] [smalldatetime] NULL,
	[SettlementDate] [smalldatetime] NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Amount] [decimal](26, 6) NULL,
	[IsCredit] [bit] NULL,
	[Status] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Notes] [nvarchar](max) COLLATE French_CI_AS NULL,
	[CfgBankId] [uniqueidentifier] NULL,
	[StlSettlementTypeId] [uniqueidentifier] NULL,
	[StlAccountId] [uniqueidentifier] NULL,
	[TierId] [uniqueidentifier] NULL,
	[TierType] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Category] [nvarchar](255) COLLATE French_CI_AS NULL,
	[CategoryId] [uniqueidentifier] NULL,
	[ItemNumber] [nvarchar](max) COLLATE French_CI_AS NULL,
	[Group1Label] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Group2Label] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Group3Label] [nvarchar](255) COLLATE French_CI_AS NULL,
	[StlCategoryId] [uniqueidentifier] NULL,
	[ItemDescription] [nvarchar](max) COLLATE French_CI_AS NULL,
	[ReceiptCode] [nvarchar](255) COLLATE French_CI_AS NULL,
	[CfgCompanyId] [uniqueidentifier] NULL,
	[AdmModuleId] [uniqueidentifier] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[StlAccountPositionId] [uniqueidentifier] NULL,
	[CfgTierOtherId] [uniqueidentifier] NULL,
	[IsDcoumentDeliveredToCenter] [bit] NULL,
	[CfgLawyerId] [uniqueidentifier] NULL,
	[DeliveredToCenterDate] [datetime] NULL,
	[AmountBase] [decimal](26, 6) NULL,
	[AmountRS] [decimal](26, 6) NULL,
	[RsRatio] [decimal](26, 6) NULL,
	[RSAmountSettled] [bit] NULL,
	[TierPayerId] [uniqueidentifier] NULL,
	[BuyLoanScheduleId] [uniqueidentifier] NULL,
	[BuySubLoanId] [uniqueidentifier] NULL,
	[StlInternalTransferId] [uniqueidentifier] NULL,
	[RSSettlementDate] [datetime] NULL,
	[RSSheetCode] [nvarchar](255) COLLATE French_CI_AS NULL,
	[PropositionSheetCode] [nvarchar](255) COLLATE French_CI_AS NULL,
	[SlipCode] [nvarchar](255) COLLATE French_CI_AS NULL,
	[SlipDate] [datetime] NULL,
	[IsAccounted] [bit] NULL,
	[StlSlipId] [uniqueidentifier] NULL,
	[StlDepositId] [uniqueidentifier] NULL,
	[StlCurrencyId] [uniqueidentifier] NULL,
	[ConvertValue] [decimal](26, 6) NULL,
	[AmountBaseConverted] [decimal](26, 6) NULL,
	[AmountStamp] [decimal](26, 6) NULL,
	[StlAcountDepositId] [uniqueidentifier] NULL,
	[StlCurrencyAffectationId] [uniqueidentifier] NULL,
	[ConvertUnit] [int] NULL,
	[BuySettlementId_old] [uniqueidentifier] NULL,
	[BuyLoanId] [uniqueidentifier] NULL,
	[StrEntityId] [uniqueidentifier] NULL,
	[BuyFolderId] [uniqueidentifier] NULL,
 CONSTRAINT [StlSettlement_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
CREATE NONCLUSTERED INDEX [_dta_index_StlSettlement_5_706101556__K1_8_43] ON [dbo].[StlSettlement]
(
	[PKey] ASC
)
INCLUDE([IsCredit],[AmountBase]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_StlSettlement_5_706101556__K1_K13_K9_8] ON [dbo].[StlSettlement]
(
	[PKey] ASC,
	[StlSettlementTypeId] ASC,
	[Status] ASC
)
INCLUDE([IsCredit]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_StlSettlement_5_706101556__K1_K38_8] ON [dbo].[StlSettlement]
(
	[PKey] ASC,
	[StlAccountPositionId] ASC
)
INCLUDE([IsCredit]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_StlSettlement_5_706101556__K1_K48_8_43] ON [dbo].[StlSettlement]
(
	[PKey] ASC,
	[BuyLoanScheduleId] ASC
)
INCLUDE([IsCredit],[AmountBase]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_StlSettlement_5_706101556__K1_K49_8] ON [dbo].[StlSettlement]
(
	[PKey] ASC,
	[BuySubLoanId] ASC
)
INCLUDE([IsCredit]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_StlSettlement_5_706101556__K1_K49_K48_43] ON [dbo].[StlSettlement]
(
	[PKey] ASC,
	[BuySubLoanId] ASC,
	[BuyLoanScheduleId] ASC
)
INCLUDE([AmountBase]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_StlSettlement_5_706101556__K1_K50_8] ON [dbo].[StlSettlement]
(
	[PKey] ASC,
	[StlInternalTransferId] ASC
)
INCLUDE([IsCredit]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_StlSettlement_5_706101556__K16_K1_K32_8_43] ON [dbo].[StlSettlement]
(
	[TierId] ASC,
	[PKey] ASC,
	[CfgCompanyId] ASC
)
INCLUDE([IsCredit],[AmountBase]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_StlSettlement_5_706101556__K32_K13_K1_K16_2_3_4_5_7_8_9_43_44_45_46] ON [dbo].[StlSettlement]
(
	[CfgCompanyId] ASC,
	[StlSettlementTypeId] ASC,
	[PKey] ASC,
	[TierId] ASC
)
INCLUDE([DocDate],[EcheanceDate],[SettlementDate],[Code],[Amount],[IsCredit],[Status],[AmountBase],[AmountRS],[RsRatio],[RSAmountSettled]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_StlSettlement_5_706101556__K49_K48_K38_K50_K1_K28_8] ON [dbo].[StlSettlement]
(
	[BuySubLoanId] ASC,
	[BuyLoanScheduleId] ASC,
	[StlAccountPositionId] ASC,
	[StlInternalTransferId] ASC,
	[PKey] ASC,
	[StlCategoryId] ASC
)
INCLUDE([IsCredit]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_StlSettlement_6_642101328__K1_63] ON [dbo].[StlSettlement]
(
	[PKey] ASC
)
INCLUDE([ConvertValue]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_StlSettlement_6_642101328__K1_8] ON [dbo].[StlSettlement]
(
	[PKey] ASC
)
INCLUDE([IsCredit]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_StlSettlement_6_642101328__K13_K8_K9] ON [dbo].[StlSettlement]
(
	[StlSettlementTypeId] ASC,
	[IsCredit] ASC,
	[Status] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_StlSettlement_6_642101328__K38_K1_K33_K16_K32_K28_K49_K65_K71_K50_K43_7_63] ON [dbo].[StlSettlement]
(
	[StlAccountPositionId] ASC,
	[PKey] ASC,
	[AdmModuleId] ASC,
	[TierId] ASC,
	[CfgCompanyId] ASC,
	[StlCategoryId] ASC,
	[BuySubLoanId] ASC,
	[AmountStamp] ASC,
	[StrEntityId] ASC,
	[StlInternalTransferId] ASC,
	[AmountBase] ASC
)
INCLUDE([Amount],[ConvertValue]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_StlSettlement_6_642101328__K50_K38_K49_K1_K65_K43_K71_K33_K16_K32_K28_7_63] ON [dbo].[StlSettlement]
(
	[StlInternalTransferId] ASC,
	[StlAccountPositionId] ASC,
	[BuySubLoanId] ASC,
	[PKey] ASC,
	[AmountStamp] ASC,
	[AmountBase] ASC,
	[StrEntityId] ASC,
	[AdmModuleId] ASC,
	[TierId] ASC,
	[CfgCompanyId] ASC,
	[StlCategoryId] ASC
)
INCLUDE([Amount],[ConvertValue]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_StlSettlement_7_642101328__K32_K1_K38_K50_K48_K49_K70_K47_K41_K39_K16_K61_K60_K14_K13_K33_2_3_4_5_6_7_8_9_10_12_17_] ON [dbo].[StlSettlement]
(
	[CfgCompanyId] ASC,
	[PKey] ASC,
	[StlAccountPositionId] ASC,
	[StlInternalTransferId] ASC,
	[BuyLoanScheduleId] ASC,
	[BuySubLoanId] ASC,
	[BuyLoanId] ASC,
	[TierPayerId] ASC,
	[CfgLawyerId] ASC,
	[CfgTierOtherId] ASC,
	[TierId] ASC,
	[StlDepositId] ASC,
	[StlSlipId] ASC,
	[StlAccountId] ASC,
	[StlSettlementTypeId] ASC,
	[AdmModuleId] ASC
)
INCLUDE([DocDate],[EcheanceDate],[SettlementDate],[Code],[Description],[Amount],[IsCredit],[Status],[Notes],[CfgBankId],[TierType],[Category],[CategoryId],[ItemNumber],[Group1Label],[Group2Label],[Group3Label],[StlCategoryId],[ItemDescription],[ReceiptCode],[UserCreate],[UserCreateDateTime],[UserUpdate],[UserUpdateDateTime],[IsDcoumentDeliveredToCenter],[DeliveredToCenterDate],[AmountBase],[AmountRS],[RsRatio],[RSAmountSettled],[RSSettlementDate],[RSSheetCode],[PropositionSheetCode],[SlipCode],[SlipDate],[IsAccounted],[StlCurrencyId],[ConvertValue],[AmountBaseConverted],[AmountStamp],[StlAcountDepositId],[StlCurrencyAffectationId],[ConvertUnit],[BuySettlementId_old],[StrEntityId]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_StlSettlement_7_642101328__K49_K1_K28_K33_K16_K32_K65_K71_7] ON [dbo].[StlSettlement]
(
	[BuySubLoanId] ASC,
	[PKey] ASC,
	[StlCategoryId] ASC,
	[AdmModuleId] ASC,
	[TierId] ASC,
	[CfgCompanyId] ASC,
	[AmountStamp] ASC,
	[StrEntityId] ASC
)
INCLUDE([Amount]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_StlSettlement_7_642101328__K65_K50_K38_K49_K1_K43_K71_K33_K16_K32_K28_7_63] ON [dbo].[StlSettlement]
(
	[AmountStamp] ASC,
	[StlInternalTransferId] ASC,
	[StlAccountPositionId] ASC,
	[BuySubLoanId] ASC,
	[PKey] ASC,
	[AmountBase] ASC,
	[StrEntityId] ASC,
	[AdmModuleId] ASC,
	[TierId] ASC,
	[CfgCompanyId] ASC,
	[StlCategoryId] ASC
)
INCLUDE([Amount],[ConvertValue]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_StlSettlement_7_706101556__K13_K1_K9_8] ON [dbo].[StlSettlement]
(
	[StlSettlementTypeId] ASC,
	[PKey] ASC,
	[Status] ASC
)
INCLUDE([IsCredit]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StlSettlementKind](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Notes] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[IsCashAccount] [bit] NULL,
 CONSTRAINT [StlSettlementKind_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StlSettlementLine](
	[PKey] [uniqueidentifier] NOT NULL,
	[Amount] [decimal](26, 6) NULL,
	[StlSettlementId] [uniqueidentifier] NULL,
	[BuyDocumentId] [uniqueidentifier] NULL,
	[StlDocumentId] [uniqueidentifier] NULL,
	[RntDocumentId] [uniqueidentifier] NULL,
	[ComDocumentId] [uniqueidentifier] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [StlSettlementLine_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
CREATE NONCLUSTERED INDEX [_dta_index_StlSettlementLine_6_2136446735__K3_2] ON [dbo].[StlSettlementLine]
(
	[StlSettlementId] ASC
)
INCLUDE([Amount]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_StlSettlementLine_6_2136446735__K3_K1_K7_K6_K5_K4_2] ON [dbo].[StlSettlementLine]
(
	[StlSettlementId] ASC,
	[PKey] ASC,
	[ComDocumentId] ASC,
	[RntDocumentId] ASC,
	[StlDocumentId] ASC,
	[BuyDocumentId] ASC
)
INCLUDE([Amount]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_StlSettlementLine_6_2136446735__K3_K6_K5_2] ON [dbo].[StlSettlementLine]
(
	[StlSettlementId] ASC,
	[RntDocumentId] ASC,
	[StlDocumentId] ASC
)
INCLUDE([Amount]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_StlSettlementLine_6_2136446735__K4_K7_K3_K5_K6_2] ON [dbo].[StlSettlementLine]
(
	[BuyDocumentId] ASC,
	[ComDocumentId] ASC,
	[StlSettlementId] ASC,
	[StlDocumentId] ASC,
	[RntDocumentId] ASC
)
INCLUDE([Amount]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_StlSettlementLine_6_2136446735__K5_K3_K1_K4_2] ON [dbo].[StlSettlementLine]
(
	[StlDocumentId] ASC,
	[StlSettlementId] ASC,
	[PKey] ASC,
	[BuyDocumentId] ASC
)
INCLUDE([Amount]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_StlSettlementLine_6_2136446735__K6_K5_K1_K4_K7_K3_K2] ON [dbo].[StlSettlementLine]
(
	[RntDocumentId] ASC,
	[StlDocumentId] ASC,
	[PKey] ASC,
	[BuyDocumentId] ASC,
	[ComDocumentId] ASC,
	[StlSettlementId] ASC,
	[Amount] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_StlSettlementLine_6_2136446735__K7_K3_K5_2] ON [dbo].[StlSettlementLine]
(
	[ComDocumentId] ASC,
	[StlSettlementId] ASC,
	[StlDocumentId] ASC
)
INCLUDE([Amount]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
CREATE NONCLUSTERED INDEX [_dta_index_StlSettlementLine_7_814014031__K4_2] ON [dbo].[StlSettlementLine]
(
	[BuyDocumentId] ASC
)
INCLUDE([Amount]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StlSettlementRelation](
	[PKey] [uniqueidentifier] NOT NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[StlCategoryId] [uniqueidentifier] NULL
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StlSettlementType](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[IsReferenceUnique] [bit] NULL,
	[IsReferenceMandatory] [bit] NULL,
	[IsBankMandatory] [bit] NULL,
	[IsNotContabilized] [bit] NULL,
	[Notes] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[IsTierUnique] [bit] NULL,
	[RefFormat] [nvarchar](255) COLLATE French_CI_AS NULL,
	[IsLawyerMandatory] [bit] NULL,
	[ActAccountCreditId] [uniqueidentifier] NULL,
	[ActAccountDebitId] [uniqueidentifier] NULL,
	[IsDeposit] [bit] NULL,
	[IsAccount] [bit] NULL,
 CONSTRAINT [PK_StlSettlementType] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StlSlip](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[EmissionDate] [datetime] NULL,
	[ReceptionDate] [datetime] NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[CfgTierEmissionId] [uniqueidentifier] NULL,
	[CfgTierTransmissionId] [uniqueidentifier] NULL,
	[StlAccountId] [uniqueidentifier] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[TransmissionDate] [datetime] NULL,
	[StrEntityId] [uniqueidentifier] NULL,
	[StrStatusId] [uniqueidentifier] NULL,
	[CfgCompanyId] [uniqueidentifier] NULL,
	[StlDepositId] [uniqueidentifier] NULL,
 CONSTRAINT [StlSlip_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StrEntity](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[DescriptionLong] [text] COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[AdmModuleId] [uniqueidentifier] NULL,
	[StlCategoryId] [uniqueidentifier] NULL,
	[IsCredit] [bit] NULL,
	[WithWorkFlow] [bit] NULL,
	[IsCharge] [bit] NULL,
	[QueryString] [text] COLLATE French_CI_AS NULL,
	[ReportsQueryString] [nvarchar](max) COLLATE French_CI_AS NULL,
	[ActAccountId] [uniqueidentifier] NULL,
	[IsAccountable] [bit] NULL,
	[ObjectPath] [nvarchar](255) COLLATE French_CI_AS NULL,
	[ObjectForm] [nvarchar](255) COLLATE French_CI_AS NULL,
	[ActDefaultAccountCode] [nvarchar](255) COLLATE French_CI_AS NULL,
	[IsProduct] [bit] NULL,
	[StrTierFieldId] [uniqueidentifier] NULL,
	[BoolField001Label] [nvarchar](255) COLLATE French_CI_AS NULL,
	[BoolField002Label] [nvarchar](255) COLLATE French_CI_AS NULL,
	[BoolField003Label] [nvarchar](255) COLLATE French_CI_AS NULL,
	[BoolField004Label] [nvarchar](255) COLLATE French_CI_AS NULL,
	[BoolField005Label] [nvarchar](255) COLLATE French_CI_AS NULL,
	[BoolField006Label] [nvarchar](255) COLLATE French_CI_AS NULL,
	[BoolField007Label] [nvarchar](255) COLLATE French_CI_AS NULL,
	[BoolField008Label] [nvarchar](255) COLLATE French_CI_AS NULL,
	[BoolField009Label] [nvarchar](255) COLLATE French_CI_AS NULL,
	[BoolField010Label] [nvarchar](255) COLLATE French_CI_AS NULL,
	[BoolField011Label] [nvarchar](255) COLLATE French_CI_AS NULL,
	[BoolField012Label] [nvarchar](255) COLLATE French_CI_AS NULL,
	[BoolField013Label] [nvarchar](255) COLLATE French_CI_AS NULL,
	[BoolField014Label] [nvarchar](255) COLLATE French_CI_AS NULL,
	[BoolField015Label] [nvarchar](255) COLLATE French_CI_AS NULL,
	[BoolField016Label] [nvarchar](255) COLLATE French_CI_AS NULL,
	[BoolField017Label] [nvarchar](255) COLLATE French_CI_AS NULL,
	[BoolField018Label] [nvarchar](255) COLLATE French_CI_AS NULL,
	[BoolField019Label] [nvarchar](255) COLLATE French_CI_AS NULL,
	[BoolField020Label] [nvarchar](255) COLLATE French_CI_AS NULL,
 CONSTRAINT [StrEntity_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
CREATE NONCLUSTERED INDEX [_dta_index_StrEntity_5_1517964484__K1_2_3] ON [dbo].[StrEntity]
(
	[PKey] ASC
)
INCLUDE([Code],[Description]) WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_PADDING ON

GO
CREATE NONCLUSTERED INDEX [_dta_index_StrEntity_5_1517964484__K1_K3] ON [dbo].[StrEntity]
(
	[PKey] ASC,
	[Description] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StrField](
	[PKey] [uniqueidentifier] NOT NULL,
	[Sorting] [int] NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[StrConfigEntityId] [uniqueidentifier] NULL,
 CONSTRAINT [StrField_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StrFolderStatusHistory](
	[PKey] [uniqueidentifier] NOT NULL,
	[DocDate] [datetime] NULL,
	[ComFolderId] [uniqueidentifier] NULL,
	[AdmUserId] [uniqueidentifier] NULL,
	[CfgTierId] [uniqueidentifier] NULL,
	[StkItemId] [uniqueidentifier] NULL,
	[ComSaleWithdrawalClientId] [uniqueidentifier] NULL,
	[ComSaleWithdrawalProductId] [uniqueidentifier] NULL,
	[ComSaleWithdrawalCancellationId] [uniqueidentifier] NULL,
	[RntFolderId] [uniqueidentifier] NULL,
	[SynFolderId] [uniqueidentifier] NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[StrFolderStatusId] [uniqueidentifier] NULL,
 CONSTRAINT [ComFolderStatusHistory_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StrHistory](
	[PKey] [uniqueidentifier] NOT NULL,
	[DocDate] [datetime] NULL,
	[Description] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[ObjectId] [uniqueidentifier] NULL,
	[AdmUserId] [uniqueidentifier] NULL,
	[AdmUserDescription] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[StrWorkflowId] [uniqueidentifier] NULL,
	[StrWorkflowDescription] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[StrStatusId] [uniqueidentifier] NULL,
	[StrStatusDescription] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[UserDate] [datetime] NULL,
	[StrConfigEntityId] [uniqueidentifier] NULL,
 CONSTRAINT [StrHistory_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StrStatus](
	[PKey] [uniqueidentifier] NOT NULL,
	[StrStatusTypeId] [uniqueidentifier] NULL,
	[Sorting] [int] NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[CfgImageId] [uniqueidentifier] NULL,
	[ColorName] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Delay] [int] NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[IsReadOnly] [bit] NULL,
	[CanDelete] [bit] NULL,
	[BoolField001] [bit] NULL,
	[BoolField002] [bit] NULL,
	[BoolField003] [bit] NULL,
	[BoolField004] [bit] NULL,
	[BoolField005] [bit] NULL,
	[BoolField006] [bit] NULL,
	[BoolField007] [bit] NULL,
	[BoolField008] [bit] NULL,
	[BoolField009] [bit] NULL,
	[BoolField010] [bit] NULL,
	[BoolField011] [bit] NULL,
	[BoolField012] [bit] NULL,
	[BoolField013] [bit] NULL,
	[BoolField014] [bit] NULL,
	[BoolField015] [bit] NULL,
	[BoolField016] [bit] NULL,
	[BoolField017] [bit] NULL,
	[BoolField018] [bit] NULL,
	[BoolField019] [bit] NULL,
	[BoolField020] [bit] NULL,
	[StrFieldDateId] [uniqueidentifier] NULL,
	[StrConfigEntityId] [uniqueidentifier] NULL,
 CONSTRAINT [StrStatus_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[StrStatusType](
	[PKey] [uniqueidentifier] NOT NULL,
	[Sorting] [int] NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
 CONSTRAINT [StrStatusType_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[SynFolder](
	[PKey] [uniqueidentifier] NOT NULL,
	[Code] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[DocDate] [datetime] NULL,
	[CfgCustomerId] [uniqueidentifier] NULL,
	[StkItemId] [uniqueidentifier] NULL,
	[StkItemArea] [decimal](26, 6) NULL,
	[StkItemSyndicHT] [decimal](26, 6) NULL,
	[StkItemSyndicVatRatio] [decimal](26, 6) NULL,
	[StkItemSyndicTTC] [decimal](26, 6) NULL,
	[CfgCommercialId] [uniqueidentifier] NULL,
	[IsInvoiceCustomerGroupping] [bit] NULL,
	[RntFolderId] [uniqueidentifier] NULL,
	[ComFolderId] [uniqueidentifier] NULL,
	[Usage] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Contact] [nvarchar](255) COLLATE French_CI_AS NULL,
	[RntPeriodicityId] [uniqueidentifier] NULL,
	[PeriodicityNumber] [int] NULL,
	[PeriodicityReconductionRatio] [decimal](26, 6) NULL,
	[PeriodicityReconductionFrequency] [int] NULL,
	[PeriodicityBeginDate] [smalldatetime] NULL,
	[PeriodicityEndDate] [smalldatetime] NULL,
	[RntReconductionTypeId] [uniqueidentifier] NULL,
	[SyndicVatRatio] [decimal](26, 6) NULL,
	[SyndicMonthAmountHT] [decimal](26, 6) NULL,
	[SyndicMonthAmountTTC] [decimal](26, 6) NULL,
	[SyndicPeriodAmountHT] [decimal](26, 6) NULL,
	[SyndicPeriodAmountTTC] [decimal](26, 6) NULL,
	[SyndicYearAmountHT] [decimal](26, 6) NULL,
	[SyndicYearAmountTTC] [decimal](26, 6) NULL,
	[CfgLawyerId] [uniqueidentifier] NULL,
	[AgreementDate1] [datetime] NULL,
	[AgreementDate2] [datetime] NULL,
	[DeliveryDateReal] [datetime] NULL,
	[AgreementSavingDate] [datetime] NULL,
	[InsuranceDate] [datetime] NULL,
	[CancelDate] [smalldatetime] NULL,
	[CancelAmount] [decimal](26, 6) NULL,
	[CancelDescription] [nvarchar](255) COLLATE French_CI_AS NULL,
	[Notes] [nvarchar](1000) COLLATE French_CI_AS NULL,
	[UserCreate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserCreateDateTime] [datetime] NULL,
	[UserUpdate] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserUpdateDateTime] [datetime] NULL,
	[StrEntityId] [uniqueidentifier] NULL,
	[StkVocationId] [uniqueidentifier] NULL,
	[IsSynPricing] [bit] NULL,
	[NextAugmentationDate] [datetime] NULL,
	[LawyerTransmissionDate] [datetime] NULL,
	[StartContentiousDate] [datetime] NULL,
	[LastLetterOfIncreaseDate] [datetime] NULL,
	[CfgTierBailiffId] [uniqueidentifier] NULL,
	[IsPenaltyDisabled] [bit] NULL,
	[StrStatusId] [uniqueidentifier] NULL,
 CONSTRAINT [SynFolder_PK] PRIMARY KEY CLUSTERED 
(
	[PKey] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
)

GO
CREATE NONCLUSTERED INDEX [_dta_index_SynFolder_6_1621632870__K1_K7] ON [dbo].[SynFolder]
(
	[PKey] ASC,
	[StkItemId] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, FILLFACTOR = 90, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF)
GO


-- ============================================================================
-- Lot P0 (FEATURE-patrimoine-audit-actions.md) : journal d'audit OMRAN et notes.
-- Types exacts de yesRemsProd. Details est en text (ANSI), pas en nvarchar.
-- ============================================================================
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[AdmWebHistorics](
	[PKey] [uniqueidentifier] NOT NULL,
	[UserDescription] [nvarchar](max) COLLATE French_CI_AS NULL,
	[ActionDateTime] [datetime] NULL,
	[ObjectId] [uniqueidentifier] NULL,
	[LockEntityId] [uniqueidentifier] NULL,
	[Action] [nvarchar](max) COLLATE French_CI_AS NULL,
	[ObjectType] [nvarchar](max) COLLATE French_CI_AS NULL,
	[ObjectTypeLabel] [nvarchar](max) COLLATE French_CI_AS NULL,
	[ObjectLabel] [nvarchar](max) COLLATE French_CI_AS NULL,
	[Description] [nvarchar](max) COLLATE French_CI_AS NULL,
	[Details] [text] COLLATE French_CI_AS NULL,
	[ParentObjectType] [nvarchar](255) COLLATE French_CI_AS NULL,
	[ParentObjectId] [uniqueidentifier] NULL,
	[SystemVersion] [varchar](255) COLLATE French_CI_AS NULL,
	[AdmWebSessionsId] [uniqueidentifier] NULL,
 CONSTRAINT [PK_dbo.AdmWebHistorics] PRIMARY KEY CLUSTERED ([PKey] ASC)
)
GO

CREATE NONCLUSTERED INDEX [IX_AdmWebHistorics_ObjectId] ON [dbo].[AdmWebHistorics] ([ObjectId] ASC)
GO

CREATE TABLE [dbo].[AdmMessage](
	[PKey] [uniqueidentifier] NOT NULL,
	[DocDate] [datetime] NULL,
	[Notes] [nvarchar](max) COLLATE French_CI_AS NULL,
	[CategoryId] [uniqueidentifier] NULL,
	[Description] [nvarchar](255) COLLATE French_CI_AS NULL,
	[UserName] [nvarchar](255) COLLATE French_CI_AS NULL,
	[IsIn] [bit] NULL,
	[Origin] [nvarchar](255) COLLATE French_CI_AS NULL,
	[ReminderDate] [datetime] NULL,
 CONSTRAINT [AdmMessage_PK] PRIMARY KEY CLUSTERED ([PKey] ASC)
)
GO

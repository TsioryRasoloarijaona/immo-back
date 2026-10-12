-- ============================================================================
-- Lot P0 : seule FK de production sur ces tables, AdmWebHistorics -> AdmWebSessions.
-- AdmWebSessions n'est pas dans le schema de CI : la FK n'est posee que si elle existe
-- (voir README.md). AdmMessage.CategoryId n'a aucune FK en production (porteur polymorphe).
-- ============================================================================
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

IF OBJECT_ID('dbo.AdmWebSessions') IS NOT NULL
    EXEC('ALTER TABLE [dbo].[AdmWebHistorics] WITH CHECK ADD CONSTRAINT [AdmWebHistorics__AdmWebSessions_FK1] FOREIGN KEY([AdmWebSessionsId]) REFERENCES [dbo].[AdmWebSessions] ([PKey])')
GO

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

CREATE TRIGGER [dbo].[trgAfterComFolderInsert] ON [dbo].[ComFolder]
AFTER INSERT
AS
IF EXISTS (select * from
		(
		select count(*) AS CCC, StkItem.Code from ComFolder
		inner join StkItem on ComFolder.StkItemId = StkItem.PKey
		inner join StrStatus on ComFolder.StrStatusId = StrStatus.PKey
		where StrStatus.BoolField003 = 0
		and StrStatus.BoolField002 = 0
		and StkItem.PKey in (select inserted.StkItemId from inserted)
		and ComFolder.PKey not in (select inserted.PKey from inserted)
		group By StkItem.Code
		) bbb
		where CCC > 0)
OR EXISTS (select * from
		(
		select count(*) AS CCC, StkItem.Code from RntFolder
		inner join StkItem on RntFolder.StkItemId = StkItem.PKey
		inner join StrStatus on RntFolder.StrStatusId = StrStatus.PKey
		where StrStatus.BoolField003 = 0
		and StrStatus.BoolField002 = 0
		and StkItem.PKey in (select inserted.StkItemId from inserted)
		group By StkItem.Code
		) bbb
		where CCC > 0)
BEGIN
RAISERROR ('YESERR002', 16, 1);
ROLLBACK TRANSACTION;
RETURN 
END;

GO
ALTER TABLE [dbo].[ComFolder] ENABLE TRIGGER [trgAfterComFolderInsert]
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE TRIGGER [dbo].[trgAfterComFolderUpdate] ON [dbo].[ComFolder]
AFTER UPDATE
AS
IF EXISTS (select * from
		(
		select count(*) AS CCC, StkItem.Code from ComFolder
		inner join StkItem on ComFolder.StkItemId = StkItem.PKey
		inner join StrStatus on ComFolder.StrStatusId = StrStatus.PKey
		where StrStatus.BoolField003 = 0
		and StrStatus.BoolField002 = 0
		and StkItem.PKey in (
			select inserted.StkItemId from inserted
			inner join StrStatus on inserted.StrStatusId = StrStatus.PKey
			where StrStatus.BoolField003 = 0
			and StrStatus.BoolField002 = 0
			)
		and ComFolder.PKey not in (
			select inserted.PKey from inserted
			inner join StrStatus on inserted.StrStatusId = StrStatus.PKey
			where StrStatus.BoolField003 = 0
			and StrStatus.BoolField002 = 0
			)
		group By StkItem.Code
		) bbb
		where CCC > 0)
OR EXISTS (select * from
		(
		select count(*) AS CCC, StkItem.Code from RntFolder
		inner join StkItem on RntFolder.StkItemId = StkItem.PKey
		inner join StrStatus on RntFolder.StrStatusId = StrStatus.PKey
		where StrStatus.BoolField003 = 0
		and StrStatus.BoolField002 = 0
		and StkItem.PKey in (
			select inserted.StkItemId from inserted
			inner join StrStatus on inserted.StrStatusId = StrStatus.PKey
			where StrStatus.BoolField003 = 0
			and StrStatus.BoolField002 = 0
			)
		group By StkItem.Code
		) bbb
		where CCC > 0)
BEGIN
RAISERROR ('YESERR002', 16, 1);
ROLLBACK TRANSACTION;
RETURN 
END;

GO
ALTER TABLE [dbo].[ComFolder] ENABLE TRIGGER [trgAfterComFolderUpdate]
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE TRIGGER [dbo].[trgAfterRntFolderInsert] ON [dbo].[RntFolder]
AFTER INSERT
AS
IF EXISTS (select * from
		(
		select count(*) AS CCC, StkItem.Code from ComFolder
		inner join StkItem on ComFolder.StkItemId = StkItem.PKey
		inner join StrStatus on ComFolder.StrStatusId = StrStatus.PKey
		where StrStatus.BoolField003 = 0
		and StrStatus.BoolField002 = 0
		and StkItem.PKey in (select inserted.StkItemId from inserted)
		group By StkItem.Code
		) bbb
		where CCC > 0)
OR EXISTS (select * from
		(
		select count(*) AS CCC, StkItem.Code from RntFolder
		inner join StkItem on RntFolder.StkItemId = StkItem.PKey
		inner join StrStatus on RntFolder.StrStatusId = StrStatus.PKey
		where StrStatus.BoolField003 = 0
		and StrStatus.BoolField002 = 0
		and StkItem.PKey in (select inserted.StkItemId from inserted)
		and RntFolder.PKey not in (select inserted.PKey from inserted)
		group By StkItem.Code
		) bbb
		where CCC > 0)
BEGIN
RAISERROR ('YESERR003', 16, 1);
ROLLBACK TRANSACTION;
RETURN 
END;

GO
ALTER TABLE [dbo].[RntFolder] ENABLE TRIGGER [trgAfterRntFolderInsert]
GO
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE TRIGGER [dbo].[trgAfterRntFolderUpdate] ON [dbo].[RntFolder]
AFTER UPDATE
AS
IF EXISTS (select * from
		(
		select count(*) AS CCC, StkItem.Code from ComFolder
		inner join StkItem on ComFolder.StkItemId = StkItem.PKey
		inner join StrStatus on ComFolder.StrStatusId = StrStatus.PKey
		where StrStatus.BoolField003 = 0
		and StrStatus.BoolField002 = 0
		and StkItem.PKey in (
			select inserted.StkItemId from inserted
			inner join StrStatus on inserted.StrStatusId = StrStatus.PKey
			where StrStatus.BoolField003 = 0
			and StrStatus.BoolField002 = 0
			)

		group By StkItem.Code
		) bbb
		where CCC > 0)
OR EXISTS (select * from
		(
		select count(*) AS CCC, StkItem.Code from RntFolder
		inner join StkItem on RntFolder.StkItemId = StkItem.PKey
		inner join StrStatus on RntFolder.StrStatusId = StrStatus.PKey
		where StrStatus.BoolField003 = 0
		and StrStatus.BoolField002 = 0
		and StkItem.PKey in (
			select inserted.StkItemId from inserted
			inner join StrStatus on inserted.StrStatusId = StrStatus.PKey
			where StrStatus.BoolField003 = 0
			and StrStatus.BoolField002 = 0
			)
		and RntFolder.PKey not in (
			select inserted.PKey from inserted
			inner join StrStatus on inserted.StrStatusId = StrStatus.PKey
			where StrStatus.BoolField003 = 0
			and StrStatus.BoolField002 = 0
			)		group By StkItem.Code
		) bbb
		where CCC > 0)
BEGIN
RAISERROR ('YESERR002', 16, 1);
ROLLBACK TRANSACTION;
RETURN 
END;

GO
ALTER TABLE [dbo].[RntFolder] ENABLE TRIGGER [trgAfterRntFolderUpdate]
GO


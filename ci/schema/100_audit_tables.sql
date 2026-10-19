-- Lot P0 (FEATURE-patrimoine-audit-actions.md) : journal d'audit OMRAN et notes, types de yesRemsProd.
-- Idempotent ; inutile sur yesRemsTest, ou les deux tables existent deja.
IF OBJECT_ID('dbo.AdmWebHistorics', 'U') IS NULL
CREATE TABLE dbo.AdmWebHistorics (
    PKey uniqueidentifier NOT NULL,
    UserDescription nvarchar(max) NULL,
    ActionDateTime datetime NULL,
    ObjectId uniqueidentifier NULL,
    LockEntityId uniqueidentifier NULL,
    [Action] nvarchar(max) NULL,
    ObjectType nvarchar(max) NULL,
    ObjectTypeLabel nvarchar(max) NULL,
    ObjectLabel nvarchar(max) NULL,
    Description nvarchar(max) NULL,
    Details text NULL,
    ParentObjectType nvarchar(255) NULL,
    ParentObjectId uniqueidentifier NULL,
    SystemVersion varchar(255) NULL,
    AdmWebSessionsId uniqueidentifier NULL,
    CONSTRAINT [PK_dbo.AdmWebHistorics] PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

IF OBJECT_ID('dbo.AdmMessage', 'U') IS NULL
CREATE TABLE dbo.AdmMessage (
    PKey uniqueidentifier NOT NULL,
    DocDate datetime NULL,
    Notes nvarchar(max) NULL,
    CategoryId uniqueidentifier NULL,
    Description nvarchar(255) NULL,
    UserName nvarchar(255) NULL,
    IsIn bit NULL,
    Origin nvarchar(255) NULL,
    ReminderDate datetime NULL,
    CONSTRAINT AdmMessage_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

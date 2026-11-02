-- Lot admin-fiches-verrouillees (FEATURE-admin-fiches-verrouillees.md) : definition de yesRemsProd, sans FK.
IF OBJECT_ID('dbo.AdmWebLockEntities', 'U') IS NULL
CREATE TABLE dbo.AdmWebLockEntities (
    PKey uniqueidentifier NOT NULL CONSTRAINT AdmWebLockEntities_PK PRIMARY KEY,
    Code nvarchar(max) NULL,
    Description nvarchar(max) NULL,
    LockDate datetime NOT NULL,
    SessionId uniqueidentifier NOT NULL,
    DataObjectId uniqueidentifier NOT NULL,
    State int NOT NULL
);
GO

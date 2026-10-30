-- Lot admin-type-priorite-tache (FEATURE-admin-type-priorite-tache.md) : definitions de yesRemsProd.
IF OBJECT_ID('dbo.AdmTaskType', 'U') IS NULL
CREATE TABLE dbo.AdmTaskType (
    PKey uniqueidentifier NOT NULL CONSTRAINT PK_AdmTaskType PRIMARY KEY,
    Code nvarchar(255) NULL,
    Description nvarchar(255) NULL,
    Notes nvarchar(1000) NULL,
    UserCreate nvarchar(255) NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) NULL,
    UserUpdateDateTime datetime NULL
);
GO

IF OBJECT_ID('dbo.AdmTaskPriority', 'U') IS NULL
CREATE TABLE dbo.AdmTaskPriority (
    PKey uniqueidentifier NOT NULL CONSTRAINT PK_AdmTaskPriority PRIMARY KEY,
    Sorting int NULL,
    Code nvarchar(255) NULL,
    Description nvarchar(255) NULL,
    Notes nvarchar(1000) NULL,
    UserCreate nvarchar(255) NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) NULL,
    UserUpdateDateTime datetime NULL
);
GO

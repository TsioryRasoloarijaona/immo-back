-- Lot P5 (FEATURE-entites-juridiques.md §7.1) : actionnaires, lignes de groupe et repartition, types de yesRemsProd.
-- Idempotent ; CfgGroupShareholder, CfgCompany, StlCurrency, AdmSequence et AdmUserHabilitation sont dans 010.
IF OBJECT_ID('dbo.CfgShareholder', 'U') IS NULL
CREATE TABLE dbo.CfgShareholder (
    PKey uniqueidentifier NOT NULL,
    Code nvarchar(255) NULL,
    Description nvarchar(255) NULL,
    UserCreate nvarchar(255) NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) NULL,
    UserUpdateDateTime datetime NULL,
    Notes nvarchar(500) NULL,
    CONSTRAINT CfgShareholder_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

IF OBJECT_ID('dbo.CfgGroupShareholderLine', 'U') IS NULL
CREATE TABLE dbo.CfgGroupShareholderLine (
    PKey uniqueidentifier NOT NULL,
    CfgGroupShareholderId uniqueidentifier NULL,
    CfgShareholderId uniqueidentifier NULL,
    SharingNumber decimal(26, 6) NULL,
    UserCreate nvarchar(255) NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) NULL,
    UserUpdateDateTime datetime NULL,
    CONSTRAINT CfgGroupShareholderLine_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

IF OBJECT_ID('dbo.CfgCompanyShareholder', 'U') IS NULL
CREATE TABLE dbo.CfgCompanyShareholder (
    PKey uniqueidentifier NOT NULL,
    CfgCompanyId uniqueidentifier NULL,
    SharingNumber int NULL,
    UserCreate nvarchar(255) NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) NULL,
    UserUpdateDateTime datetime NULL,
    CfgShareholderId uniqueidentifier NULL,
    CONSTRAINT CfgCompanyShareholder_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

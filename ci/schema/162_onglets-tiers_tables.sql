-- Lot admin-onglets-tiers (FEATURE-admin-onglets-tiers.md) : definition de yesRemsProd, sans FK.
IF OBJECT_ID('dbo.GrhVacationType', 'U') IS NULL
CREATE TABLE dbo.GrhVacationType (
    PKey uniqueidentifier NOT NULL CONSTRAINT GrhVacationType_PK PRIMARY KEY,
    Code nvarchar(255) NULL,
    Description nvarchar(255) NULL,
    Notes nvarchar(500) NULL,
    UserCreate nvarchar(255) NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) NULL,
    UserUpdateDateTime datetime NULL,
    IsCharged bit NULL,
    IsChargedWorkedDay bit NULL
);
GO

IF OBJECT_ID('dbo.GrhVacation', 'U') IS NULL
CREATE TABLE dbo.GrhVacation (
    PKey uniqueidentifier NOT NULL CONSTRAINT GrhVacation_PK PRIMARY KEY,
    Code nvarchar(255) NULL,
    Description nvarchar(255) NULL,
    DocDate datetime NULL,
    DateFrom datetime NULL,
    DateTo datetime NULL,
    DaysNumber decimal(26, 6) NULL,
    Notes nvarchar(500) NULL,
    GrhVacationTypeId uniqueidentifier NULL,
    GrhEmployeeId uniqueidentifier NULL,
    CfgCompanyId uniqueidentifier NULL,
    UserCreate nvarchar(255) NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) NULL,
    UserUpdateDateTime datetime NULL,
    StrEntityId uniqueidentifier NULL,
    StrStatusId uniqueidentifier NULL,
    GrhVacationGroupGrhEmployeeId uniqueidentifier NULL
);
GO

-- StlDocument de yesRemsTest est reduite : colonnes lues par l'onglet Recettes (la CI a la table complete).
IF COL_LENGTH('dbo.StlDocument', 'CfgTierId') IS NULL
ALTER TABLE dbo.StlDocument ADD CfgTierId uniqueidentifier NULL;
GO
IF COL_LENGTH('dbo.StlDocument', 'MaturityDate') IS NULL
ALTER TABLE dbo.StlDocument ADD MaturityDate datetime NULL;
GO
IF COL_LENGTH('dbo.StlDocument', 'DocRef') IS NULL
ALTER TABLE dbo.StlDocument ADD DocRef nvarchar(255) NULL;
GO
IF COL_LENGTH('dbo.StlDocument', 'AmountToPay') IS NULL
ALTER TABLE dbo.StlDocument ADD AmountToPay decimal(26, 6) NULL;
GO
IF COL_LENGTH('dbo.StlDocument', 'Notes') IS NULL
ALTER TABLE dbo.StlDocument ADD Notes nvarchar(1000) NULL;
GO

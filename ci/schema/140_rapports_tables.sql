-- Rapports (FEATURE-tdb-vente-rapports-socle.md §3.2) : AdmReportData (sans ReportFile, jamais lu) et AdmReportDataRole,
-- DDL de yesRemsProd. Applicable à yesRemsTest, qui n'a pas ces tables (AdmRole.WithReportRestriction y est déjà). Idempotent.
IF OBJECT_ID('dbo.AdmReportData', 'U') IS NULL
    CREATE TABLE dbo.AdmReportData (
        PKey uniqueidentifier NOT NULL CONSTRAINT AdmReportData_PK PRIMARY KEY,
        Code nvarchar(255) NULL,
        Description nvarchar(255) NULL,
        [Type] nvarchar(1000) NULL,
        IsSystem bit NULL,
        Sorting int NULL,
        GroupName nvarchar(255) NULL,
        CfgImageId uniqueidentifier NULL,
        CategoryId uniqueidentifier NULL,
        Category nvarchar(255) NULL,
        OpenBeeSendActivated bit NULL,
        OpenBeeFolderId int NULL,
        CONSTRAINT U_Code UNIQUE (Code, CategoryId));
GO
IF OBJECT_ID('dbo.AdmReportDataRole', 'U') IS NULL
    CREATE TABLE dbo.AdmReportDataRole (
        PKey uniqueidentifier NOT NULL CONSTRAINT AdmReportDataRole_PK PRIMARY KEY,
        AdmRoleId uniqueidentifier NULL,
        AdmReportDataId uniqueidentifier NULL,
        UserCreate nvarchar(255) NULL,
        UserCreateDateTime datetime NULL,
        UserUpdate nvarchar(255) NULL,
        UserUpdateDateTime datetime NULL);
GO
IF COL_LENGTH('dbo.AdmRole', 'WithReportRestriction') IS NULL
    ALTER TABLE dbo.AdmRole ADD WithReportRestriction bit NULL;
GO

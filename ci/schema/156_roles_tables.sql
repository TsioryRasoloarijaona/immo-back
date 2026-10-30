-- Lot admin-roles (FEATURE-admin-roles.md) : definitions de yesRemsProd. AdmReportDataRole vient de 140_rapports_tables.sql.
IF OBJECT_ID('dbo.AdmRightEntity', 'U') IS NULL
CREATE TABLE dbo.AdmRightEntity (
    PKey uniqueidentifier NOT NULL CONSTRAINT AdmRightEntity_PK PRIMARY KEY,
    Code nvarchar(255) NULL,
    Description nvarchar(255) NULL,
    Sorting int NULL,
    Depth int NULL,
    Kind nvarchar(255) NULL
);
GO
IF OBJECT_ID('dbo.BuyDocumentWorkFlowAdmRole', 'U') IS NULL
CREATE TABLE dbo.BuyDocumentWorkFlowAdmRole (
    PKey uniqueidentifier NOT NULL CONSTRAINT BuyDocumentWorkFlowAdmRole_PK PRIMARY KEY,
    WorkFlowId uniqueidentifier NULL,
    AdmRoleId uniqueidentifier NULL,
    UserCreate nvarchar(255) NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) NULL,
    UserUpdateDateTime datetime NULL
);
GO
IF OBJECT_ID('dbo.AdmWebRights', 'U') IS NULL
CREATE TABLE dbo.AdmWebRights (
    PKey uniqueidentifier NOT NULL CONSTRAINT AdmWebRights_PK PRIMARY KEY,
    NodeKey nvarchar(max) NULL,
    RoleId uniqueidentifier NOT NULL
);
GO

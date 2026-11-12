-- Tables lues par les écritures d'achat (FEATURE-achat-fiche-ecritures.md E-B3, E-B4), colonnes de yesRemsProd.
IF OBJECT_ID('dbo.AdmParamsBoolean', 'U') IS NULL
CREATE TABLE dbo.AdmParamsBoolean (
    Code nvarchar(255) NOT NULL CONSTRAINT AdmParamsBoolean_PK PRIMARY KEY,
    CodeValue bit NULL,
    PKey uniqueidentifier NULL);
GO
IF OBJECT_ID('dbo.ActEntry', 'U') IS NULL
CREATE TABLE dbo.ActEntry (
    PKey uniqueidentifier NOT NULL CONSTRAINT ActEntry_PK PRIMARY KEY,
    Code nvarchar(255) NULL,
    DocCode nvarchar(255) NULL,
    Description nvarchar(255) NULL,
    Notes nvarchar(500) NULL,
    DocDate datetime NULL,
    CfgCompanyId uniqueidentifier NULL,
    AdmModuleId uniqueidentifier NULL,
    StlSettlementId uniqueidentifier NULL,
    ComFolderId uniqueidentifier NULL,
    ComSaleWithdrawalCancellationId uniqueidentifier NULL,
    ComSaleWithdrawalProductId uniqueidentifier NULL,
    ComSaleWithdrawalClientId uniqueidentifier NULL,
    BuyDocumentId uniqueidentifier NULL,
    StlDocumentId uniqueidentifier NULL,
    UserCreate nvarchar(255) NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) NULL,
    UserUpdateDateTime datetime NULL,
    StrEntityId uniqueidentifier NULL,
    StrEntityRelationId uniqueidentifier NULL,
    RelationId uniqueidentifier NULL,
    RelationCode nvarchar(255) NULL,
    RelationDescription nvarchar(255) NULL,
    StlCurrencyId uniqueidentifier NULL,
    ActNumber int NULL);
GO
IF OBJECT_ID('dbo.StlAccountingEntry', 'U') IS NULL
CREATE TABLE dbo.StlAccountingEntry (
    PKey uniqueidentifier NOT NULL CONSTRAINT StlAccountingEntry_PK PRIMARY KEY,
    DocDate datetime NULL,
    DocNumber nvarchar(255) NULL,
    DocNumberRef nvarchar(255) NULL,
    DocLabel nvarchar(255) NULL,
    CodeCountable nvarchar(255) NULL,
    CodeCountableCollective nvarchar(255) NULL,
    CodeCountableJournal nvarchar(255) NULL,
    Notes nvarchar(500) NULL,
    AmountCredit decimal(26, 6) NULL,
    AmountDebit decimal(26, 6) NULL,
    CfgCompanyId uniqueidentifier NULL,
    CfgTierId uniqueidentifier NULL,
    BuyDocumentId uniqueidentifier NULL,
    StlDocumentId uniqueidentifier NULL,
    StlSettlementId uniqueidentifier NOT NULL,
    UserCreate nvarchar(255) NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) NULL,
    UserUpdateDateTime datetime NULL);
GO

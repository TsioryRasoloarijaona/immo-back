-- Lot N6a demandes internes d'achat (FEATURE-n6-demandes-achat.md) : colonnes de BuySelection et table BuySelectionKind
-- (completes en CI par 010, reduites dans yesRemsTest), filles de la DIA absentes partout. Idempotent.
IF OBJECT_ID('dbo.BuySelectionKind', 'U') IS NULL
CREATE TABLE dbo.BuySelectionKind (
    PKey uniqueidentifier NOT NULL CONSTRAINT BuySelectionKind_PK PRIMARY KEY,
    Code nvarchar(500) NULL, Description nvarchar(500) NULL, Notes nvarchar(1000) NULL,
    UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL,
    MaxAmount decimal(26, 6) NULL, IsConfidential bit NULL, StrEntityManagmentId uniqueidentifier NULL, IsMultiSupplier bit NULL,
    IsWithoutVerification bit NULL);
GO
IF COL_LENGTH('dbo.BuySelection', 'Sorting') IS NULL ALTER TABLE dbo.BuySelection ADD Sorting int NULL;
IF COL_LENGTH('dbo.BuySelection', 'Code') IS NULL ALTER TABLE dbo.BuySelection ADD Code nvarchar(255) NULL;
IF COL_LENGTH('dbo.BuySelection', 'Description') IS NULL ALTER TABLE dbo.BuySelection ADD Description nvarchar(255) NULL;
IF COL_LENGTH('dbo.BuySelection', 'DescriptionLong') IS NULL ALTER TABLE dbo.BuySelection ADD DescriptionLong nvarchar(500) NULL;
IF COL_LENGTH('dbo.BuySelection', 'DefaultVat') IS NULL ALTER TABLE dbo.BuySelection ADD DefaultVat decimal(26, 6) NULL;
IF COL_LENGTH('dbo.BuySelection', 'PriceEstimatedHT') IS NULL ALTER TABLE dbo.BuySelection ADD PriceEstimatedHT decimal(26, 6) NULL;
IF COL_LENGTH('dbo.BuySelection', 'PriceEstimatedTTC') IS NULL ALTER TABLE dbo.BuySelection ADD PriceEstimatedTTC decimal(26, 6) NULL;
IF COL_LENGTH('dbo.BuySelection', 'DocDate') IS NULL ALTER TABLE dbo.BuySelection ADD DocDate datetime NULL;
IF COL_LENGTH('dbo.BuySelection', 'LaunchOfTenderDate') IS NULL ALTER TABLE dbo.BuySelection ADD LaunchOfTenderDate datetime NULL;
IF COL_LENGTH('dbo.BuySelection', 'WithdrawalTenderDocumentsDate') IS NULL
    ALTER TABLE dbo.BuySelection ADD WithdrawalTenderDocumentsDate datetime NULL;
IF COL_LENGTH('dbo.BuySelection', 'TenderMaturityDate') IS NULL ALTER TABLE dbo.BuySelection ADD TenderMaturityDate datetime NULL;
IF COL_LENGTH('dbo.BuySelection', 'TendersResortDate') IS NULL ALTER TABLE dbo.BuySelection ADD TendersResortDate datetime NULL;
IF COL_LENGTH('dbo.BuySelection', 'BidOpeningDate') IS NULL ALTER TABLE dbo.BuySelection ADD BidOpeningDate datetime NULL;
IF COL_LENGTH('dbo.BuySelection', 'TendersReportDate') IS NULL ALTER TABLE dbo.BuySelection ADD TendersReportDate datetime NULL;
IF COL_LENGTH('dbo.BuySelection', 'TenderDate') IS NULL ALTER TABLE dbo.BuySelection ADD TenderDate datetime NULL;
IF COL_LENGTH('dbo.BuySelection', 'ContractSignatureDate') IS NULL ALTER TABLE dbo.BuySelection ADD ContractSignatureDate datetime NULL;
IF COL_LENGTH('dbo.BuySelection', 'Notes') IS NULL ALTER TABLE dbo.BuySelection ADD Notes nvarchar(1000) NULL;
IF COL_LENGTH('dbo.BuySelection', 'TextLetterSelection') IS NULL ALTER TABLE dbo.BuySelection ADD TextLetterSelection nvarchar(max) NULL;
IF COL_LENGTH('dbo.BuySelection', 'IsTheLast') IS NULL ALTER TABLE dbo.BuySelection ADD IsTheLast bit NULL;
IF COL_LENGTH('dbo.BuySelection', 'UserCreate') IS NULL ALTER TABLE dbo.BuySelection ADD UserCreate nvarchar(255) NULL;
IF COL_LENGTH('dbo.BuySelection', 'UserCreateDateTime') IS NULL ALTER TABLE dbo.BuySelection ADD UserCreateDateTime datetime NULL;
IF COL_LENGTH('dbo.BuySelection', 'UserUpdate') IS NULL ALTER TABLE dbo.BuySelection ADD UserUpdate nvarchar(255) NULL;
IF COL_LENGTH('dbo.BuySelection', 'UserUpdateDateTime') IS NULL ALTER TABLE dbo.BuySelection ADD UserUpdateDateTime datetime NULL;
IF COL_LENGTH('dbo.BuySelection', 'StrEntityId') IS NULL ALTER TABLE dbo.BuySelection ADD StrEntityId uniqueidentifier NULL;
IF COL_LENGTH('dbo.BuySelection', 'StrStatusId') IS NULL ALTER TABLE dbo.BuySelection ADD StrStatusId uniqueidentifier NULL;
IF COL_LENGTH('dbo.BuySelection', 'CfgCompanyId') IS NULL ALTER TABLE dbo.BuySelection ADD CfgCompanyId uniqueidentifier NULL;
IF COL_LENGTH('dbo.BuySelection', 'StlCurrencyId') IS NULL ALTER TABLE dbo.BuySelection ADD StlCurrencyId uniqueidentifier NULL;
IF COL_LENGTH('dbo.BuySelection', 'PrjMarketId') IS NULL ALTER TABLE dbo.BuySelection ADD PrjMarketId uniqueidentifier NULL;
IF COL_LENGTH('dbo.BuySelection', 'StlCategoryId') IS NULL ALTER TABLE dbo.BuySelection ADD StlCategoryId uniqueidentifier NULL;
IF COL_LENGTH('dbo.BuySelection', 'BuySelectionKindId') IS NULL ALTER TABLE dbo.BuySelection ADD BuySelectionKindId uniqueidentifier NULL;
IF COL_LENGTH('dbo.BuySelection', 'CfgTierId') IS NULL ALTER TABLE dbo.BuySelection ADD CfgTierId uniqueidentifier NULL;
IF COL_LENGTH('dbo.BuySelection', 'GroupId') IS NULL ALTER TABLE dbo.BuySelection ADD GroupId uniqueidentifier NULL;
IF COL_LENGTH('dbo.BuySelection', 'StlSettlementTypeId') IS NULL ALTER TABLE dbo.BuySelection ADD StlSettlementTypeId uniqueidentifier NULL;
IF COL_LENGTH('dbo.BuySelection', 'PaymentDelay') IS NULL ALTER TABLE dbo.BuySelection ADD PaymentDelay int NULL;
IF COL_LENGTH('dbo.BuySelection', 'AdmModuleId') IS NULL ALTER TABLE dbo.BuySelection ADD AdmModuleId uniqueidentifier NULL;
IF COL_LENGTH('dbo.BuySelection', 'CfgTierTriggerId') IS NULL ALTER TABLE dbo.BuySelection ADD CfgTierTriggerId uniqueidentifier NULL;
GO
IF OBJECT_ID('dbo.BuySelectionSupplier', 'U') IS NULL
CREATE TABLE dbo.BuySelectionSupplier (
    PKey uniqueidentifier NOT NULL CONSTRAINT BuySelectionSupplier_PK PRIMARY KEY,
    OfficeOrderRef nvarchar(255) NULL, SelectionLetterSendDate datetime NULL, TenderAcquisitionDate datetime NULL,
    TenderDepositDate datetime NULL, IsRejected bit NULL, Notes nvarchar(1000) NULL,
    UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL,
    BuySelectionId uniqueidentifier NULL
        CONSTRAINT BuySelectionSupplier_BuySelection_FK REFERENCES dbo.BuySelection (PKey) ON DELETE CASCADE,
    CfgTierId uniqueidentifier NULL);
GO
IF OBJECT_ID('dbo.BuySelectionGroup', 'U') IS NULL
CREATE TABLE dbo.BuySelectionGroup (
    PKey uniqueidentifier NOT NULL CONSTRAINT BuySelectionGroup_PK PRIMARY KEY,
    Sorting int NULL, Code nvarchar(255) NULL, Description nvarchar(255) NULL, Notes nvarchar(1000) NULL,
    UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL,
    ParentId uniqueidentifier NULL,
    BuySelectionId uniqueidentifier NULL
        CONSTRAINT BuySelectionGroup_BuySelection_FK REFERENCES dbo.BuySelection (PKey) ON DELETE CASCADE);
GO
IF OBJECT_ID('dbo.BuySelectionPaymentTerm', 'U') IS NULL
CREATE TABLE dbo.BuySelectionPaymentTerm (
    PKey uniqueidentifier NOT NULL CONSTRAINT BuySelectionPaymentTerm_PK PRIMARY KEY,
    Sorting int NULL, Code nvarchar(255) NULL, Ratio decimal(26, 6) NULL,
    UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL,
    BuySelectionId uniqueidentifier NULL
        CONSTRAINT BuySelectionPaymentTerm_BuySelection_FK REFERENCES dbo.BuySelection (PKey) ON DELETE CASCADE);
GO

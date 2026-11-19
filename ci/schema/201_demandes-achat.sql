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
-- Bordereau de la DIA : 194 crée une forme réduite des deux tables ; colonnes de la prod ajoutées ici.
IF OBJECT_ID('dbo.BuySelectionLine', 'U') IS NULL
CREATE TABLE dbo.BuySelectionLine (
    PKey uniqueidentifier NOT NULL CONSTRAINT BuySelectionLine_PK PRIMARY KEY,
    BuySelectionId uniqueidentifier NULL, StlCategoryId uniqueidentifier NULL, BuyItemId uniqueidentifier NULL,
    BuySelectionGroupId uniqueidentifier NULL);
GO
IF OBJECT_ID('dbo.BuySelectionStockLine', 'U') IS NULL
CREATE TABLE dbo.BuySelectionStockLine (
    PKey uniqueidentifier NOT NULL CONSTRAINT BuySelectionStockLine_PK PRIMARY KEY,
    BuyItemId uniqueidentifier NULL, BuySelectionId uniqueidentifier NULL);
GO
IF COL_LENGTH('dbo.BuySelectionLine', 'Sorting') IS NULL ALTER TABLE dbo.BuySelectionLine ADD Sorting int NULL;
IF COL_LENGTH('dbo.BuySelectionLine', 'Code') IS NULL ALTER TABLE dbo.BuySelectionLine ADD Code nvarchar(255) NULL;
IF COL_LENGTH('dbo.BuySelectionLine', 'Description') IS NULL ALTER TABLE dbo.BuySelectionLine ADD Description nvarchar(255) NULL;
IF COL_LENGTH('dbo.BuySelectionLine', 'Unity') IS NULL ALTER TABLE dbo.BuySelectionLine ADD Unity nvarchar(50) NULL;
IF COL_LENGTH('dbo.BuySelectionLine', 'Quantity') IS NULL ALTER TABLE dbo.BuySelectionLine ADD Quantity decimal(26, 6) NULL;
IF COL_LENGTH('dbo.BuySelectionLine', 'UnitPriceHT') IS NULL ALTER TABLE dbo.BuySelectionLine ADD UnitPriceHT decimal(26, 6) NULL;
IF COL_LENGTH('dbo.BuySelectionLine', 'FODECRatio') IS NULL ALTER TABLE dbo.BuySelectionLine ADD FODECRatio decimal(26, 16) NULL;
IF COL_LENGTH('dbo.BuySelectionLine', 'VatRatio') IS NULL ALTER TABLE dbo.BuySelectionLine ADD VatRatio decimal(26, 6) NULL;
IF COL_LENGTH('dbo.BuySelectionLine', 'UnitPriceTTC') IS NULL ALTER TABLE dbo.BuySelectionLine ADD UnitPriceTTC decimal(26, 6) NULL;
IF COL_LENGTH('dbo.BuySelectionLine', 'Notes') IS NULL ALTER TABLE dbo.BuySelectionLine ADD Notes nvarchar(1000) NULL;
IF COL_LENGTH('dbo.BuySelectionLine', 'UserCreate') IS NULL ALTER TABLE dbo.BuySelectionLine ADD UserCreate nvarchar(255) NULL;
IF COL_LENGTH('dbo.BuySelectionLine', 'UserCreateDateTime') IS NULL ALTER TABLE dbo.BuySelectionLine ADD UserCreateDateTime datetime NULL;
IF COL_LENGTH('dbo.BuySelectionLine', 'UserUpdate') IS NULL ALTER TABLE dbo.BuySelectionLine ADD UserUpdate nvarchar(255) NULL;
IF COL_LENGTH('dbo.BuySelectionLine', 'UserUpdateDateTime') IS NULL ALTER TABLE dbo.BuySelectionLine ADD UserUpdateDateTime datetime NULL;
IF COL_LENGTH('dbo.BuySelectionStockLine', 'Sorting') IS NULL ALTER TABLE dbo.BuySelectionStockLine ADD Sorting int NULL;
IF COL_LENGTH('dbo.BuySelectionStockLine', 'Code') IS NULL ALTER TABLE dbo.BuySelectionStockLine ADD Code nvarchar(255) NULL;
IF COL_LENGTH('dbo.BuySelectionStockLine', 'Description') IS NULL ALTER TABLE dbo.BuySelectionStockLine ADD Description nvarchar(255) NULL;
IF COL_LENGTH('dbo.BuySelectionStockLine', 'Unity') IS NULL ALTER TABLE dbo.BuySelectionStockLine ADD Unity nvarchar(255) NULL;
IF COL_LENGTH('dbo.BuySelectionStockLine', 'Quantity') IS NULL ALTER TABLE dbo.BuySelectionStockLine ADD Quantity decimal(26, 6) NULL;
IF COL_LENGTH('dbo.BuySelectionStockLine', 'UnitPriceHT') IS NULL ALTER TABLE dbo.BuySelectionStockLine ADD UnitPriceHT decimal(26, 6) NULL;
IF COL_LENGTH('dbo.BuySelectionStockLine', 'FODECRatio') IS NULL ALTER TABLE dbo.BuySelectionStockLine ADD FODECRatio decimal(26, 16) NULL;
IF COL_LENGTH('dbo.BuySelectionStockLine', 'VatRatio') IS NULL ALTER TABLE dbo.BuySelectionStockLine ADD VatRatio decimal(26, 6) NULL;
IF COL_LENGTH('dbo.BuySelectionStockLine', 'UnitPriceTTC') IS NULL ALTER TABLE dbo.BuySelectionStockLine ADD UnitPriceTTC decimal(26, 6) NULL;
IF COL_LENGTH('dbo.BuySelectionStockLine', 'Notes') IS NULL ALTER TABLE dbo.BuySelectionStockLine ADD Notes nvarchar(255) NULL;
IF COL_LENGTH('dbo.BuySelectionStockLine', 'UserCreate') IS NULL ALTER TABLE dbo.BuySelectionStockLine ADD UserCreate nvarchar(255) NULL;
IF COL_LENGTH('dbo.BuySelectionStockLine', 'UserCreateDateTime') IS NULL ALTER TABLE dbo.BuySelectionStockLine ADD UserCreateDateTime datetime NULL;
IF COL_LENGTH('dbo.BuySelectionStockLine', 'UserUpdate') IS NULL ALTER TABLE dbo.BuySelectionStockLine ADD UserUpdate nvarchar(255) NULL;
IF COL_LENGTH('dbo.BuySelectionStockLine', 'UserUpdateDateTime') IS NULL ALTER TABLE dbo.BuySelectionStockLine ADD UserUpdateDateTime datetime NULL;
GO
IF OBJECT_ID('dbo.BuySelectionLine_BuySelection_FK', 'F') IS NULL
ALTER TABLE dbo.BuySelectionLine ADD CONSTRAINT BuySelectionLine_BuySelection_FK
    FOREIGN KEY (BuySelectionId) REFERENCES dbo.BuySelection (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.BuySelectionStockLine_BuySelection_FK', 'F') IS NULL
ALTER TABLE dbo.BuySelectionStockLine ADD CONSTRAINT BuySelectionStockLine_BuySelection_FK
    FOREIGN KEY (BuySelectionId) REFERENCES dbo.BuySelection (PKey) ON DELETE CASCADE;
GO

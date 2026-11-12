-- FEATURE-lot-sous-fenetres.md §4.4, §4.6 : échéances et réceptions de retenue de garantie, prévisions du lot.
-- Définitions de yesRemsProd (celles de FixtureLotSousFenetres.TABLES) ; idempotent, applicable à yesRemsTest.
IF OBJECT_ID('dbo.PrjMarketWarrantyLine', 'U') IS NULL
CREATE TABLE dbo.PrjMarketWarrantyLine (PKey uniqueidentifier NOT NULL PRIMARY KEY, Sorting int NULL,
    Description nvarchar(255) NULL, Delay int NULL, Ratio decimal(26, 6) NULL, EcheanceDate datetime NULL,
    PrjMarketId uniqueidentifier NULL, UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL);
GO
IF OBJECT_ID('dbo.PrjMarketReceptionType', 'U') IS NULL
CREATE TABLE dbo.PrjMarketReceptionType (PKey uniqueidentifier NOT NULL PRIMARY KEY, sorting int NULL,
    Code nvarchar(255) NULL, Description nvarchar(255) NULL, UserCreate nvarchar(255) NULL,
    UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL);
GO
IF OBJECT_ID('dbo.PrjMarketReceptionKind', 'U') IS NULL
CREATE TABLE dbo.PrjMarketReceptionKind (PKey uniqueidentifier NOT NULL PRIMARY KEY, sorting int NULL,
    Code nvarchar(255) NULL, Description nvarchar(255) NULL, UserCreate nvarchar(255) NULL,
    UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL,
    IsFinal bit NULL);
GO
IF OBJECT_ID('dbo.PrjMarketReception', 'U') IS NULL
CREATE TABLE dbo.PrjMarketReception (PKey uniqueidentifier NOT NULL PRIMARY KEY, sorting int NULL,
    Code nvarchar(255) NULL, Description nvarchar(255) NULL, DocDate datetime NULL, MeetingPlace nvarchar(255) NULL,
    RequiredBySupplier bit NULL, PrjMarketId uniqueidentifier NULL, PrjMarketReceptionTypeId uniqueidentifier NULL,
    PrjMarketReceptionKindId uniqueidentifier NULL, UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL);
GO
IF OBJECT_ID('dbo.PrjMarketPaymentPrevision', 'U') IS NULL
CREATE TABLE dbo.PrjMarketPaymentPrevision (PKey uniqueidentifier NOT NULL PRIMARY KEY, PaymentDate datetime NULL,
    Amount decimal(26, 6) NULL, Description nvarchar(255) NULL, PrjMarketId uniqueidentifier NULL,
    Achievement decimal(26, 6) NULL, AmountHT decimal(26, 6) NULL, AmountTTC decimal(26, 6) NULL,
    StrEntityId uniqueidentifier NULL);
GO
IF OBJECT_ID('dbo.PrjMarketPrevision', 'U') IS NULL
CREATE TABLE dbo.PrjMarketPrevision (PKey uniqueidentifier NOT NULL PRIMARY KEY, PointTime decimal(26, 16) NULL,
    PointDate smalldatetime NULL, PointAchivement decimal(26, 16) NULL, PointPayment decimal(26, 6) NULL,
    PointAmount decimal(26, 6) NULL, PrjMarketId uniqueidentifier NULL);
GO
IF OBJECT_ID('dbo.PrjMarketUserPrevision', 'U') IS NULL
CREATE TABLE dbo.PrjMarketUserPrevision (PKey uniqueidentifier NOT NULL PRIMARY KEY, PointTime decimal(26, 16) NULL,
    PointDate smalldatetime NULL, PointAchivement decimal(26, 16) NULL, PointPayment decimal(26, 6) NULL,
    PointAmount decimal(26, 6) NULL, PrjMarketId uniqueidentifier NULL);
GO

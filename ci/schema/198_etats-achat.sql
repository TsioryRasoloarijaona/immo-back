-- FEATURE-n7-etats-achat.md N7b : sources des rapports Achat (prêts, échéances, prévisions), colonnes de yesRemsProd.
-- Idempotent, applicable à yesRemsTest. BuyLoan, BuySubLoan et BuyLoanSchedule existent déjà en CI (010_tables.sql).
IF OBJECT_ID('dbo.BuySubLoan', 'U') IS NULL
CREATE TABLE dbo.BuySubLoan (
    PKey uniqueidentifier NOT NULL PRIMARY KEY,
    Amount decimal(26, 6) NULL,
    PaymentDate smalldatetime NULL,
    MonthsCount int NULL,
    GracePeriod int NULL,
    MonthsStep int NULL,
    Notes nvarchar(1000) NULL,
    BuyLoanId uniqueidentifier NULL,
    AnnualRate decimal(26, 6) NULL,
    AmortizationDeferredDelay int NULL,
    TotalDeferredDelay int NULL,
    IsMensualRate bit NULL,
    IsActoriel bit NULL
);
GO

IF OBJECT_ID('dbo.BuyLoanSchedule', 'U') IS NULL
CREATE TABLE dbo.BuyLoanSchedule (
    PKey uniqueidentifier NOT NULL PRIMARY KEY,
    AmountBase decimal(26, 6) NULL,
    InterestRate decimal(26, 6) NULL,
    AmountInterest decimal(26, 6) NULL,
    AmountToPay decimal(26, 6) NULL,
    PaymentDate smalldatetime NULL,
    Notes nvarchar(1000) NULL,
    BuySubLoanId uniqueidentifier NULL,
    Commision decimal(26, 6) NULL,
    AmountBaseHT decimal(26, 6) NULL,
    BaseVatRatio decimal(26, 6) NULL,
    AmountInterestHT decimal(26, 6) NULL,
    InterestVatRatio decimal(26, 6) NULL,
    CommisionHT decimal(26, 6) NULL,
    CommisionVatRatio decimal(26, 6) NULL,
    AmountToPayHT decimal(26, 6) NULL
);
GO

IF OBJECT_ID('dbo.StlCategoryPrevision', 'U') IS NULL
CREATE TABLE dbo.StlCategoryPrevision (
    PKey uniqueidentifier NOT NULL PRIMARY KEY,
    PrevisionYear int NULL,
    PrevisionMonth int NULL,
    PrevisionAmount decimal(26, 6) NULL,
    StlCategoryId uniqueidentifier NULL,
    CfgCompanyId uniqueidentifier NULL
);
GO

-- 165_projet-config.sql réduit BuyLoan (yesRemsTest) et PrjPrevision à 2 colonnes : compléter comme en prod.
IF COL_LENGTH('dbo.BuyLoan', 'DocDate') IS NULL ALTER TABLE dbo.BuyLoan ADD DocDate datetime NULL;
IF COL_LENGTH('dbo.BuyLoan', 'Code') IS NULL ALTER TABLE dbo.BuyLoan ADD Code nvarchar(255) NULL;
IF COL_LENGTH('dbo.BuyLoan', 'Description') IS NULL ALTER TABLE dbo.BuyLoan ADD Description nvarchar(255) NULL;
IF COL_LENGTH('dbo.BuyLoan', 'Amount') IS NULL ALTER TABLE dbo.BuyLoan ADD Amount decimal(26, 6) NULL;
IF COL_LENGTH('dbo.BuyLoan', 'InterestRate') IS NULL ALTER TABLE dbo.BuyLoan ADD InterestRate decimal(26, 6) NULL;
IF COL_LENGTH('dbo.BuyLoan', 'Fees') IS NULL ALTER TABLE dbo.BuyLoan ADD Fees decimal(26, 6) NULL;
IF COL_LENGTH('dbo.BuyLoan', 'Notes') IS NULL ALTER TABLE dbo.BuyLoan ADD Notes nvarchar(1000) NULL;
IF COL_LENGTH('dbo.BuyLoan', 'CfgTierId') IS NULL ALTER TABLE dbo.BuyLoan ADD CfgTierId uniqueidentifier NULL;
IF COL_LENGTH('dbo.BuyLoan', 'FeesPrevention') IS NULL ALTER TABLE dbo.BuyLoan ADD FeesPrevention decimal(26, 6) NULL;
IF COL_LENGTH('dbo.BuyLoan', 'StkItemId') IS NULL ALTER TABLE dbo.BuyLoan ADD StkItemId uniqueidentifier NULL;
IF COL_LENGTH('dbo.BuyLoan', 'CfgCompanyId') IS NULL ALTER TABLE dbo.BuyLoan ADD CfgCompanyId uniqueidentifier NULL;
IF COL_LENGTH('dbo.BuyLoan', 'BuyLoanTypeId') IS NULL ALTER TABLE dbo.BuyLoan ADD BuyLoanTypeId uniqueidentifier NULL;
IF COL_LENGTH('dbo.BuyLoan', 'BaseVatRatio') IS NULL ALTER TABLE dbo.BuyLoan ADD BaseVatRatio decimal(26, 6) NULL;
IF COL_LENGTH('dbo.BuyLoan', 'CommisionVatRatio') IS NULL ALTER TABLE dbo.BuyLoan ADD CommisionVatRatio decimal(26, 6) NULL;
IF COL_LENGTH('dbo.BuyLoan', 'InterestVatRatio') IS NULL ALTER TABLE dbo.BuyLoan ADD InterestVatRatio decimal(26, 6) NULL;
IF COL_LENGTH('dbo.BuyLoan', 'DefaultCommisionAmount') IS NULL
    ALTER TABLE dbo.BuyLoan ADD DefaultCommisionAmount decimal(26, 6) NULL;
IF COL_LENGTH('dbo.BuyLoan', 'IsClosed') IS NULL ALTER TABLE dbo.BuyLoan ADD IsClosed bit NULL;
IF COL_LENGTH('dbo.BuyLoan', 'StrEntityId') IS NULL ALTER TABLE dbo.BuyLoan ADD StrEntityId uniqueidentifier NULL;
GO

IF COL_LENGTH('dbo.PrjPrevision', 'PrevisionYear') IS NULL ALTER TABLE dbo.PrjPrevision ADD PrevisionYear int NULL;
IF COL_LENGTH('dbo.PrjPrevision', 'PrevisionMonth') IS NULL ALTER TABLE dbo.PrjPrevision ADD PrevisionMonth int NULL;
IF COL_LENGTH('dbo.PrjPrevision', 'PrevisionAmount') IS NULL ALTER TABLE dbo.PrjPrevision ADD PrevisionAmount decimal(26, 6) NULL;
GO

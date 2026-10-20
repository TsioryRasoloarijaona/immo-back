-- Lot P6 (FEATURE-patrimoine-referentiels.md §5.2) : referentiels ecrits par le lot, types de yesRemsProd.
-- Idempotent ; applicable aussi a yesRemsTest (LndVocation et StkExpenseType y manquent, ils existent deja en CI).
IF OBJECT_ID('dbo.LndVocation', 'U') IS NULL
CREATE TABLE dbo.LndVocation (
    PKey uniqueidentifier NOT NULL,
    Code nvarchar(255) COLLATE French_CI_AS NULL,
    Description nvarchar(255) COLLATE French_CI_AS NULL,
    UserCreate nvarchar(255) COLLATE French_CI_AS NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) COLLATE French_CI_AS NULL,
    UserUpdateDateTime datetime NULL,
    CONSTRAINT PK_LndVocation PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

IF OBJECT_ID('dbo.StkExpenseType', 'U') IS NULL
CREATE TABLE dbo.StkExpenseType (
    Pkey uniqueidentifier NOT NULL,
    Code nvarchar(255) COLLATE French_CI_AS NULL,
    Description nvarchar(255) COLLATE French_CI_AS NULL,
    StlCategoryId uniqueidentifier NULL,
    UserCreate nvarchar(255) COLLATE French_CI_AS NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) COLLATE French_CI_AS NULL,
    UserUpdateDateTime datetime NULL,
    PrjMarketTypeId uniqueidentifier NULL,
    CONSTRAINT StkExpenseType_PK PRIMARY KEY CLUSTERED (Pkey ASC)
);
GO

IF OBJECT_ID('dbo.StkItemExonaration', 'U') IS NULL
CREATE TABLE dbo.StkItemExonaration (
    PKey uniqueidentifier NOT NULL,
    DocYear int NULL,
    UnitCost decimal(26, 6) NULL,
    Notes nvarchar(500) COLLATE French_CI_AS NULL,
    CfgTrancheId uniqueidentifier NULL,
    UserCreate nvarchar(255) COLLATE French_CI_AS NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) COLLATE French_CI_AS NULL,
    UserUpdateDateTime datetime NULL,
    CONSTRAINT StkItemExonaration_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

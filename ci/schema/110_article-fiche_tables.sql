-- Lot P2a (FEATURE-patrimoine-article-fiche.md §9.3, §11.12) : tables de la fiche article, types de yesRemsProd.
-- Idempotent ; applicable aussi a yesRemsTest (les trois referentiels y manquent, ils existent deja en CI).
IF OBJECT_ID('dbo.StkOrientation', 'U') IS NULL
CREATE TABLE dbo.StkOrientation (
    PKey uniqueidentifier NOT NULL,
    Sorting int NULL,
    Code nvarchar(255) COLLATE French_CI_AS NULL,
    Description nvarchar(255) COLLATE French_CI_AS NULL,
    UserCreate nvarchar(255) COLLATE French_CI_AS NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) COLLATE French_CI_AS NULL,
    UserUpdateDateTime datetime NULL,
    CONSTRAINT StkOrientation_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

IF OBJECT_ID('dbo.StkCostCategory', 'U') IS NULL
CREATE TABLE dbo.StkCostCategory (
    PKey uniqueidentifier NOT NULL,
    Code nvarchar(255) COLLATE French_CI_AS NULL,
    Description nvarchar(255) COLLATE French_CI_AS NULL,
    UserCreate nvarchar(255) COLLATE French_CI_AS NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) COLLATE French_CI_AS NULL,
    UserUpdateDateTime datetime NULL,
    CONSTRAINT StkCostCategory_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

IF OBJECT_ID('dbo.StkFacade', 'U') IS NULL
CREATE TABLE dbo.StkFacade (
    PKey uniqueidentifier NOT NULL,
    Code nvarchar(255) COLLATE French_CI_AS NULL,
    Description nvarchar(255) COLLATE French_CI_AS NULL,
    UserCreate nvarchar(255) COLLATE French_CI_AS NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) COLLATE French_CI_AS NULL,
    UserUpdateDateTime datetime NULL,
    CONSTRAINT StkFacade_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

IF OBJECT_ID('dbo.StkVue', 'U') IS NULL
CREATE TABLE dbo.StkVue (
    PKey uniqueidentifier NOT NULL,
    Code nvarchar(255) COLLATE French_CI_AS NULL,
    Description nvarchar(255) COLLATE French_CI_AS NULL,
    UserCreate nvarchar(255) COLLATE French_CI_AS NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) COLLATE French_CI_AS NULL,
    UserUpdateDateTime datetime NULL,
    CONSTRAINT StkVue_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

IF OBJECT_ID('dbo.StkItemVue', 'U') IS NULL
CREATE TABLE dbo.StkItemVue (
    PKey uniqueidentifier NOT NULL,
    StkItemId uniqueidentifier NULL,
    StkVueId uniqueidentifier NULL,
    UserCreate nvarchar(255) COLLATE French_CI_AS NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) COLLATE French_CI_AS NULL,
    UserUpdateDateTime datetime NULL,
    CONSTRAINT StkItemVue_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

IF OBJECT_ID('dbo.StkContenance', 'U') IS NULL
CREATE TABLE dbo.StkContenance (
    PKey uniqueidentifier NOT NULL,
    Code nvarchar(255) COLLATE French_CI_AS NULL,
    Description nvarchar(255) COLLATE French_CI_AS NULL,
    Sorting int NULL,
    UserCreate nvarchar(255) COLLATE French_CI_AS NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) COLLATE French_CI_AS NULL,
    UserUpdateDateTime datetime NULL,
    Option1 nvarchar(255) COLLATE French_CI_AS NULL,
    IsOption1WithQuantity bit NULL,
    Option2 nvarchar(255) COLLATE French_CI_AS NULL,
    IsOption2WithQuantity bit NULL,
    Option3 nvarchar(255) COLLATE French_CI_AS NULL,
    IsOption3WithQuantity bit NULL,
    CONSTRAINT StkContenance_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

IF OBJECT_ID('dbo.StkItemContenance', 'U') IS NULL
CREATE TABLE dbo.StkItemContenance (
    PKey uniqueidentifier NOT NULL,
    StkItemId uniqueidentifier NULL,
    StkContenanceId uniqueidentifier NULL,
    Option1 nvarchar(255) COLLATE French_CI_AS NULL,
    IsWithOption1 bit NULL,
    QuantityOption1 int NULL,
    Option2 nvarchar(255) COLLATE French_CI_AS NULL,
    IsWithOption2 bit NULL,
    QuantityOption2 int NULL,
    Option3 nvarchar(255) COLLATE French_CI_AS NULL,
    IsWithOption3 bit NULL,
    QuantityOption3 int NULL,
    UserCreate nvarchar(255) COLLATE French_CI_AS NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) COLLATE French_CI_AS NULL,
    UserUpdateDateTime datetime NULL,
    itemsNumber int NULL,
    CONSTRAINT StkItemContenance_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

IF OBJECT_ID('dbo.StkPresentationTypeContenance', 'U') IS NULL
CREATE TABLE dbo.StkPresentationTypeContenance (
    PKey uniqueidentifier NOT NULL,
    StkPresentationTypeId uniqueidentifier NULL,
    StkContenanceId uniqueidentifier NULL,
    ItemsNumber int NULL,
    UserCreate nvarchar(255) COLLATE French_CI_AS NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) COLLATE French_CI_AS NULL,
    UserUpdateDateTime datetime NULL,
    Option1 nvarchar(255) COLLATE French_CI_AS NULL,
    IsWithOption1 bit NULL,
    QuantityOption1 int NULL,
    Option2 nvarchar(255) COLLATE French_CI_AS NULL,
    IsWithOption2 bit NULL,
    QuantityOption2 int NULL,
    Option3 nvarchar(255) COLLATE French_CI_AS NULL,
    IsWithOption3 bit NULL,
    QuantityOption3 int NULL,
    CONSTRAINT StkPresentationTypeContenance_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

IF OBJECT_ID('dbo.StkHierarchyPath', 'U') IS NULL
CREATE TABLE dbo.StkHierarchyPath (
    PKey uniqueidentifier NOT NULL,
    SvgPath text COLLATE French_CI_AS NULL,
    StkItemId uniqueidentifier NULL,
    StkFloorId uniqueidentifier NULL,
    CONSTRAINT StkHierarchyPath_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

-- Nomenclature d'achat (FEATURE-n8-nomenclature.md D3) : tables citant un article, réduites aux colonnes lues.
-- Les trois tables BuyItem* sont déjà dans 010_tables.sql (CI) ; créées ici pour yesRemsTest seulement.
IF OBJECT_ID('dbo.BuyItemFamily', 'U') IS NULL
CREATE TABLE dbo.BuyItemFamily (
    Pkey uniqueidentifier NOT NULL CONSTRAINT BuyItemFamily_PK PRIMARY KEY,
    Code nvarchar(255) NULL,
    Description nvarchar(255) NULL,
    VatRatio decimal(26, 6) NULL,
    StlCategoryId uniqueidentifier NULL,
    UserCreate nvarchar(255) NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) NULL,
    UserUpdateDateTime datetime NULL,
    Notes nvarchar(500) NULL,
    Sorting int NULL);
GO
IF OBJECT_ID('dbo.BuyItemUnderFamily', 'U') IS NULL
CREATE TABLE dbo.BuyItemUnderFamily (
    Pkey uniqueidentifier NOT NULL CONSTRAINT BuyItemUnderFamily_PK PRIMARY KEY,
    Code nvarchar(255) NULL,
    Description nvarchar(255) NULL,
    VatRatio decimal(26, 6) NULL,
    BuyItemFamilyId uniqueidentifier NULL,
    StlCategoryId uniqueidentifier NULL,
    UserCreate nvarchar(255) NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) NULL,
    UserUpdateDateTime datetime NULL,
    Notes nvarchar(500) NULL,
    Sorting int NULL);
GO
IF OBJECT_ID('dbo.BuyItem', 'U') IS NULL
CREATE TABLE dbo.BuyItem (
    PKey uniqueidentifier NOT NULL CONSTRAINT BuyItem_PK PRIMARY KEY,
    Code nvarchar(255) NULL,
    Description nvarchar(255) NULL,
    VatRatio decimal(26, 6) NULL,
    StlCategoryId uniqueidentifier NULL,
    UserCreate nvarchar(255) NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) NULL,
    UserUpdateDateTime datetime NULL,
    Notes nvarchar(500) NULL,
    BuyItemUnderFamilyId uniqueidentifier NULL,
    Unity nvarchar(255) NULL,
    UnitPriceHT decimal(26, 6) NULL,
    FODECRatio decimal(26, 6) NULL,
    CostLogisticHT decimal(26, 6) NULL,
    CostLogisticVatRatio decimal(26, 6) NULL,
    CostIndirectHT decimal(26, 6) NULL,
    CostIndirectVatRatio decimal(26, 6) NULL,
    CostFinacialHT decimal(26, 6) NULL,
    CostFinacialVatRatio decimal(26, 6) NULL,
    SaleUnitPriceHT decimal(26, 6) NULL,
    SaleVatRatio decimal(26, 6) NULL);
GO
IF OBJECT_ID('dbo.BuySelectionLine', 'U') IS NULL
CREATE TABLE dbo.BuySelectionLine (
    PKey uniqueidentifier NOT NULL CONSTRAINT BuySelectionLine_PK PRIMARY KEY,
    BuySelectionId uniqueidentifier NULL,
    StlCategoryId uniqueidentifier NULL,
    BuyItemId uniqueidentifier NULL,
    BuySelectionGroupId uniqueidentifier NULL);
GO
IF OBJECT_ID('dbo.BuySelectionStockLine', 'U') IS NULL
CREATE TABLE dbo.BuySelectionStockLine (
    PKey uniqueidentifier NOT NULL CONSTRAINT BuySelectionStockLine_PK PRIMARY KEY,
    BuyItemId uniqueidentifier NULL,
    BuySelectionId uniqueidentifier NULL);
GO
IF OBJECT_ID('dbo.GrhWorkedDay', 'U') IS NOT NULL AND COL_LENGTH('dbo.GrhWorkedDay', 'BuyItemId') IS NULL
ALTER TABLE dbo.GrhWorkedDay ADD BuyItemId uniqueidentifier NULL;
GO

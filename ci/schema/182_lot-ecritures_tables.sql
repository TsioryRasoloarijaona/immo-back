-- FEATURE-lot-ecritures.md R12 : ventilation d'un lot par catégorie de dépense.
-- Définition de yesRemsProd (celle de FixtureBordereauxEcriture.TABLE_VENTILATION) ; idempotent, applicable à yesRemsTest.
IF OBJECT_ID('dbo.PrjMarketVentilationCategory', 'U') IS NULL
CREATE TABLE dbo.PrjMarketVentilationCategory (PKey uniqueidentifier NOT NULL PRIMARY KEY,
    StlCategoryId uniqueidentifier NULL, PrjMarketId uniqueidentifier NULL, VentilationRate decimal(26, 6) NULL,
    UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL,
    UserUpdateDateTime datetime NULL);
GO

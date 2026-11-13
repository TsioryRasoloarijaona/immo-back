-- Clés de yesRemsProd de la table de 182_lot-ecritures_tables.sql.
IF OBJECT_ID('dbo.PrjMarketVentilationCategory_PrjMarket_FK', 'F') IS NULL
ALTER TABLE dbo.PrjMarketVentilationCategory ADD CONSTRAINT PrjMarketVentilationCategory_PrjMarket_FK
    FOREIGN KEY (PrjMarketId) REFERENCES dbo.PrjMarket (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.PrjMarketVentilationCategory_StlCategory_FK', 'F') IS NULL
ALTER TABLE dbo.PrjMarketVentilationCategory ADD CONSTRAINT PrjMarketVentilationCategory_StlCategory_FK
    FOREIGN KEY (StlCategoryId) REFERENCES dbo.StlCategory (PKey);
GO

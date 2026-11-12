-- Clé de yesRemsProd : CASCADE vers le lot.
IF OBJECT_ID('dbo.PrjMarketLineGroupingEstimation_PrjMarket_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjMarketLineGroupingEstimation ADD CONSTRAINT PrjMarketLineGroupingEstimation_PrjMarket_FK1
    FOREIGN KEY (PrjMarketId) REFERENCES dbo.PrjMarket (PKey) ON DELETE CASCADE;
GO

-- Lot decomptes-parite : FK de PrjWorkingOutStockLine vers le décompte (NO_ACTION, comme yesRemsProd) ; PrjStockLine hors schéma CI.
IF OBJECT_ID('dbo.PrjWorkingOutStockLine_PrjWorkingOut_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjWorkingOutStockLine ADD CONSTRAINT PrjWorkingOutStockLine_PrjWorkingOut_FK1 FOREIGN KEY (PrjWorkingOutId) REFERENCES dbo.PrjWorkingOut (PKey);
GO

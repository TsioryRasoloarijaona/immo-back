-- Clés de yesRemsProd des tables de 191_lot-avenants_tables.sql.
IF OBJECT_ID('dbo.PrjClauseLine__PrjClause_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjClauseLine ADD CONSTRAINT PrjClauseLine__PrjClause_FK1
    FOREIGN KEY (PrjClauseId) REFERENCES dbo.PrjClause (PKey);
GO
IF OBJECT_ID('dbo.PrjClauseLine__PrjMarketLine_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjClauseLine ADD CONSTRAINT PrjClauseLine__PrjMarketLine_FK1
    FOREIGN KEY (PrjMarketLineId) REFERENCES dbo.PrjMarketLine (PKey);
GO
IF OBJECT_ID('dbo.PrjClauseLine__StlCategory_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjClauseLine ADD CONSTRAINT PrjClauseLine__StlCategory_FK1
    FOREIGN KEY (StlCategoryId) REFERENCES dbo.StlCategory (PKey);
GO
IF OBJECT_ID('dbo.PrjMarketReceptionReserve__PrjMarketReception_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjMarketReceptionReserve ADD CONSTRAINT PrjMarketReceptionReserve__PrjMarketReception_FK1
    FOREIGN KEY (PrjMarketReceptionId) REFERENCES dbo.PrjMarketReception (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.PrjMarketReceptionCfgTier__PrjMarketReception_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjMarketReceptionCfgTier ADD CONSTRAINT PrjMarketReceptionCfgTier__PrjMarketReception_FK1
    FOREIGN KEY (PrjMarketReceptionId) REFERENCES dbo.PrjMarketReception (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.PrjMarketReceptionCfgTier__CfgTier_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjMarketReceptionCfgTier ADD CONSTRAINT PrjMarketReceptionCfgTier__CfgTier_FK1
    FOREIGN KEY (CfgTierId) REFERENCES dbo.CfgTier (PKey) ON DELETE CASCADE;
GO

-- Clés de yesRemsProd des tables de 180_lot-sous-fenetres_tables.sql (PrjMarketUserPrevision n'en a aucune).
IF OBJECT_ID('dbo.PrjMarketWarrantyLine_PrjMarket_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjMarketWarrantyLine ADD CONSTRAINT PrjMarketWarrantyLine_PrjMarket_FK1
    FOREIGN KEY (PrjMarketId) REFERENCES dbo.PrjMarket (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.PrjMarketReception__PrjMarket_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjMarketReception ADD CONSTRAINT PrjMarketReception__PrjMarket_FK1
    FOREIGN KEY (PrjMarketId) REFERENCES dbo.PrjMarket (PKey);
GO
IF OBJECT_ID('dbo.PrjMarketReception__PrjMarketReceptionKind_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjMarketReception ADD CONSTRAINT PrjMarketReception__PrjMarketReceptionKind_FK1
    FOREIGN KEY (PrjMarketReceptionKindId) REFERENCES dbo.PrjMarketReceptionKind (PKey);
GO
IF OBJECT_ID('dbo.PrjMarketReception__PrjMarketReceptionType_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjMarketReception ADD CONSTRAINT PrjMarketReception__PrjMarketReceptionType_FK1
    FOREIGN KEY (PrjMarketReceptionTypeId) REFERENCES dbo.PrjMarketReceptionType (PKey);
GO
IF OBJECT_ID('dbo.PrjMarketPaymentPrevision__PrjMarketId_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjMarketPaymentPrevision ADD CONSTRAINT PrjMarketPaymentPrevision__PrjMarketId_FK1
    FOREIGN KEY (PrjMarketId) REFERENCES dbo.PrjMarket (PKey) ON DELETE CASCADE;
GO
IF OBJECT_ID('dbo.PrjMarketPaymentPrevision__StrEntity_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjMarketPaymentPrevision ADD CONSTRAINT PrjMarketPaymentPrevision__StrEntity_FK1
    FOREIGN KEY (StrEntityId) REFERENCES dbo.StrEntity (PKey);
GO
IF OBJECT_ID('dbo.PrjMarket_PrjMarketPrevision_FK1', 'F') IS NULL
ALTER TABLE dbo.PrjMarketPrevision ADD CONSTRAINT PrjMarket_PrjMarketPrevision_FK1
    FOREIGN KEY (PrjMarketId) REFERENCES dbo.PrjMarket (PKey) ON DELETE CASCADE;
GO

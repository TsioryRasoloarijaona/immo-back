-- Clés de yesRemsProd : NO ACTION vers le bon de commande, CASCADE vers la facture.
IF OBJECT_ID('dbo.BuyOrderInvoice__BuyOrder_FK1', 'F') IS NULL
ALTER TABLE dbo.BuyOrderInvoice ADD CONSTRAINT BuyOrderInvoice__BuyOrder_FK1
    FOREIGN KEY (BuyOrderId) REFERENCES dbo.BuyDocument (PKey);
GO
IF OBJECT_ID('dbo.BuyOrderInvoice__BuyInvoice_FK1', 'F') IS NULL
ALTER TABLE dbo.BuyOrderInvoice ADD CONSTRAINT BuyOrderInvoice__BuyInvoice_FK1
    FOREIGN KEY (BuyInvoiceId) REFERENCES dbo.BuyDocument (PKey) ON DELETE CASCADE;
GO

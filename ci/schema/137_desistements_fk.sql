-- Clés des désistements (FEATURE-desistements.md §7 bis.6), noms de production ; déjà en CI (020_foreign_keys.sql).
IF OBJECT_ID('dbo.ComSaleWithdrawalCancellation_ComFolder_FK', 'F') IS NULL
    ALTER TABLE dbo.ComSaleWithdrawalCancellation ADD CONSTRAINT ComSaleWithdrawalCancellation_ComFolder_FK
        FOREIGN KEY (ComSaleId) REFERENCES dbo.ComFolder (PKey);
GO
IF OBJECT_ID('dbo.ComSaleWithdrawalCancellation_ComSaleWithdrawalReason_FK', 'F') IS NULL
    ALTER TABLE dbo.ComSaleWithdrawalCancellation ADD CONSTRAINT ComSaleWithdrawalCancellation_ComSaleWithdrawalReason_FK
        FOREIGN KEY (ComSaleWithdrawalReasonId) REFERENCES dbo.ComSaleWithdrawalReason (PKey);
GO
IF OBJECT_ID('dbo.ComSaleWithdrawalCancellation_StkItem_FK', 'F') IS NULL
    ALTER TABLE dbo.ComSaleWithdrawalCancellation ADD CONSTRAINT ComSaleWithdrawalCancellation_StkItem_FK
        FOREIGN KEY (StkItemId) REFERENCES dbo.StkItem (PKey);
GO
IF OBJECT_ID('dbo.ComSaleWithdrawalCancellation_StlSettlementType_FK1', 'F') IS NULL
    ALTER TABLE dbo.ComSaleWithdrawalCancellation ADD CONSTRAINT ComSaleWithdrawalCancellation_StlSettlementType_FK1
        FOREIGN KEY (StlSettlementTypeId) REFERENCES dbo.StlSettlementType (PKey);
GO
IF OBJECT_ID('dbo.ComDocument_ComSaleWithdrawalCancellation_FK', 'F') IS NULL
    ALTER TABLE dbo.ComDocument ADD CONSTRAINT ComDocument_ComSaleWithdrawalCancellation_FK
        FOREIGN KEY (ComSaleWithdrawalCancellationId) REFERENCES dbo.ComSaleWithdrawalCancellation (PKey);
GO

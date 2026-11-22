-- Lot N3-bis a : clés de StlSettlementHistory (CI seulement ; yesRemsTest n'a pas de FK). Idempotent.
IF OBJECT_ID('dbo.FK_StlSettlementHistory_StlSettlement', 'F') IS NULL
ALTER TABLE dbo.StlSettlementHistory ADD CONSTRAINT FK_StlSettlementHistory_StlSettlement
    FOREIGN KEY (StlSettlementId) REFERENCES dbo.StlSettlement (PKey) ON DELETE CASCADE;
IF OBJECT_ID('dbo.FK_StlSettlementHistory_StlSettlementType', 'F') IS NULL
ALTER TABLE dbo.StlSettlementHistory ADD CONSTRAINT FK_StlSettlementHistory_StlSettlementType
    FOREIGN KEY (StlSettlementTypeId) REFERENCES dbo.StlSettlementType (PKey);
GO

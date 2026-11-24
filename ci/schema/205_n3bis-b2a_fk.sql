-- Lot N3-bis b2a : clés de StlDeduction, NO ACTION comme en prod (CI seulement ; yesRemsTest n'a pas de FK). Idempotent.
IF OBJECT_ID('dbo.StlDeduction__StlSettlement_FK1', 'F') IS NULL
ALTER TABLE dbo.StlDeduction ADD CONSTRAINT StlDeduction__StlSettlement_FK1
    FOREIGN KEY (StlSettlementId) REFERENCES dbo.StlSettlement (PKey);
IF OBJECT_ID('dbo.StlDeduction__StlDeductionType_FK1', 'F') IS NULL
ALTER TABLE dbo.StlDeduction ADD CONSTRAINT StlDeduction__StlDeductionType_FK1
    FOREIGN KEY (StlDeductionTypeId) REFERENCES dbo.StlDeductionType (PKey);
GO

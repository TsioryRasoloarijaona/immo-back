-- Lot P7b (FEATURE-patrimoine-prospections-terrain.md §6.1) : cle etrangere de LndLandZone, action de production.
-- Celles de LndLand sont deja dans 020.
IF OBJECT_ID('dbo.LndLandZone__Prospection_FK1', 'F') IS NULL
ALTER TABLE dbo.LndLandZone ADD CONSTRAINT LndLandZone__Prospection_FK1
    FOREIGN KEY (ProspectionId) REFERENCES dbo.LndLand (PKey);
GO

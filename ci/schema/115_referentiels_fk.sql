-- Lot P6 (FEATURE-patrimoine-referentiels.md §5.2) : cle etrangere de la quote-part exoneree, comme en production.
IF OBJECT_ID('dbo.StkItemExonaration__CfgTranche_FK1', 'F') IS NULL
ALTER TABLE dbo.StkItemExonaration ADD CONSTRAINT StkItemExonaration__CfgTranche_FK1
    FOREIGN KEY (CfgTrancheId) REFERENCES dbo.CfgTranche (PKey) ON DELETE CASCADE;
GO

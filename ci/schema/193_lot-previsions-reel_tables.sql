-- FEATURE-lot-previsions-reel.md §6 : lignes des modèles de simulation lot.
-- Définition de yesRemsProd (celle de FixtureModelesSimulation) ; idempotent, applicable à yesRemsTest.
IF OBJECT_ID('dbo.PrjMarketPrevisionModelLine', 'U') IS NULL
CREATE TABLE dbo.PrjMarketPrevisionModelLine (PKey uniqueidentifier NOT NULL PRIMARY KEY,
    PointTime decimal(26, 6) NULL, PointAchivement decimal(26, 6) NULL, PointPayment decimal(26, 6) NULL,
    PrjMarketPrevisionModelId uniqueidentifier NULL
        CONSTRAINT PrjMarketPrevisionModelLine__PrjMarketPrevisionModel_FK1
        REFERENCES dbo.PrjMarketPrevisionModel (PKey) ON DELETE CASCADE);
GO

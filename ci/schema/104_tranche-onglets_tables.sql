-- Lot P1b (FEATURE-patrimoine-tranche-onglets.md §8.1) : onglet Plancher couvert, types de yesRemsProd.
-- Idempotent ; inutile sur yesRemsTest, ou la table existe deja.
IF OBJECT_ID('dbo.CfgTrancheCoveredFloor', 'U') IS NULL
CREATE TABLE dbo.CfgTrancheCoveredFloor (
    PKey uniqueidentifier NOT NULL,
    CfgTrancheId uniqueidentifier NULL,
    StkItemCategoryId uniqueidentifier NULL,
    Area decimal(26, 16) NULL,
    UserCreate nvarchar(255) COLLATE French_CI_AS NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) COLLATE French_CI_AS NULL,
    UserUpdateDateTime datetime NULL,
    CONSTRAINT CfgTrancheCoveredFloor_PK PRIMARY KEY CLUSTERED (PKey ASC)
);
GO

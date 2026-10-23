-- FEATURE-patrimoine-rapports.md §3 (transferts de produit) : colonnes lues par MouvementsStockRapport, présentes en
-- production et dans 010_tables.sql, absentes de la table réduite créée pour yesRemsTest par 112. Sans effet en CI.
IF COL_LENGTH('dbo.ComSaleWithdrawalProduct', 'ComSaleId') IS NULL
    ALTER TABLE dbo.ComSaleWithdrawalProduct ADD ComSaleId uniqueidentifier NULL;
GO
IF COL_LENGTH('dbo.ComSaleWithdrawalProduct', 'ValidityDate') IS NULL
    ALTER TABLE dbo.ComSaleWithdrawalProduct ADD ValidityDate datetime NULL;
GO

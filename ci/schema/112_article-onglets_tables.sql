-- FEATURE-patrimoine-article-onglets.md §9.3 : déjà présent en CI (010_tables.sql), sans effet ici ; à appliquer à
-- yesRemsTest, dont SynFolder n'a que 5 colonnes et qui n'a pas ComSaleWithdrawalProduct. RntFolderItem n'est pas
-- reprise (branche omise de l'onglet Dossiers).
IF COL_LENGTH('dbo.SynFolder', 'StkItemId') IS NULL
    ALTER TABLE dbo.SynFolder ADD StkItemId uniqueidentifier NULL;
GO
IF COL_LENGTH('dbo.SynFolder', 'CfgCustomerId') IS NULL
    ALTER TABLE dbo.SynFolder ADD CfgCustomerId uniqueidentifier NULL;
GO
IF OBJECT_ID('dbo.ComSaleWithdrawalProduct', 'U') IS NULL
    CREATE TABLE dbo.ComSaleWithdrawalProduct (
        PKey uniqueidentifier NOT NULL CONSTRAINT ComSaleWithdrawalProduct_PK PRIMARY KEY,
        Code nvarchar(255) NULL,
        StkItemId uniqueidentifier NULL,
        StkOldItemId uniqueidentifier NULL,
        CfgCustomerId uniqueidentifier NULL,
        IsLegalized bit NULL,
        LegalizationDate datetime NULL,
        UserCreate nvarchar(255) NULL,
        UserCreateDateTime datetime NULL,
        UserUpdate nvarchar(255) NULL,
        UserUpdateDateTime datetime NULL);
GO

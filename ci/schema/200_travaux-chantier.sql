-- FEATURE-n2-travaux-chantier.md N2a : colonnes de GrhWorkedDay (165 et 194 n'en créent que 3), types de yesRemsProd.
-- Idempotent, applicable à yesRemsTest. Sans clé étrangère : les jours de test référencent des lignes de fixture.
IF COL_LENGTH('dbo.GrhWorkedDay', 'DocDate') IS NULL ALTER TABLE dbo.GrhWorkedDay ADD DocDate datetime NULL;
GO
IF COL_LENGTH('dbo.GrhWorkedDay', 'Code') IS NULL ALTER TABLE dbo.GrhWorkedDay ADD Code nvarchar(255) NULL;
GO
IF COL_LENGTH('dbo.GrhWorkedDay', 'Description') IS NULL ALTER TABLE dbo.GrhWorkedDay ADD Description nvarchar(255) NULL;
GO
IF COL_LENGTH('dbo.GrhWorkedDay', 'Notes') IS NULL ALTER TABLE dbo.GrhWorkedDay ADD Notes nvarchar(1000) NULL;
GO
IF COL_LENGTH('dbo.GrhWorkedDay', 'Unity') IS NULL ALTER TABLE dbo.GrhWorkedDay ADD Unity nvarchar(255) NULL;
GO
IF COL_LENGTH('dbo.GrhWorkedDay', 'Quantity') IS NULL ALTER TABLE dbo.GrhWorkedDay ADD Quantity decimal(26, 6) NULL;
GO
IF COL_LENGTH('dbo.GrhWorkedDay', 'CfgEmployeeId') IS NULL ALTER TABLE dbo.GrhWorkedDay ADD CfgEmployeeId uniqueidentifier NULL;
GO
IF COL_LENGTH('dbo.GrhWorkedDay', 'CfgCustomerId') IS NULL ALTER TABLE dbo.GrhWorkedDay ADD CfgCustomerId uniqueidentifier NULL;
GO
IF COL_LENGTH('dbo.GrhWorkedDay', 'CfgCompanyId') IS NULL ALTER TABLE dbo.GrhWorkedDay ADD CfgCompanyId uniqueidentifier NULL;
GO
IF COL_LENGTH('dbo.GrhWorkedDay', 'GrhWorkedDayTypeId') IS NULL
ALTER TABLE dbo.GrhWorkedDay ADD GrhWorkedDayTypeId uniqueidentifier NULL;
GO
IF COL_LENGTH('dbo.GrhWorkedDay', 'UserCreate') IS NULL ALTER TABLE dbo.GrhWorkedDay ADD UserCreate nvarchar(255) NULL;
GO
IF COL_LENGTH('dbo.GrhWorkedDay', 'UserCreateDateTime') IS NULL
ALTER TABLE dbo.GrhWorkedDay ADD UserCreateDateTime datetime NULL;
GO
IF COL_LENGTH('dbo.GrhWorkedDay', 'UserUpdate') IS NULL ALTER TABLE dbo.GrhWorkedDay ADD UserUpdate nvarchar(255) NULL;
GO
IF COL_LENGTH('dbo.GrhWorkedDay', 'UserUpdateDateTime') IS NULL
ALTER TABLE dbo.GrhWorkedDay ADD UserUpdateDateTime datetime NULL;
GO
IF COL_LENGTH('dbo.GrhWorkedDay', 'UnitPriceHT') IS NULL ALTER TABLE dbo.GrhWorkedDay ADD UnitPriceHT decimal(26, 6) NULL;
GO
IF COL_LENGTH('dbo.GrhWorkedDay', 'Quantity1') IS NULL ALTER TABLE dbo.GrhWorkedDay ADD Quantity1 decimal(26, 6) NULL;
GO
IF COL_LENGTH('dbo.GrhWorkedDay', 'UnitPriceHT1') IS NULL ALTER TABLE dbo.GrhWorkedDay ADD UnitPriceHT1 decimal(26, 6) NULL;
GO
IF COL_LENGTH('dbo.GrhWorkedDay', 'StlInvoiceCode') IS NULL ALTER TABLE dbo.GrhWorkedDay ADD StlInvoiceCode nvarchar(255) NULL;
GO
IF COL_LENGTH('dbo.GrhWorkedDay', 'StlInvoiceProformaCode') IS NULL
ALTER TABLE dbo.GrhWorkedDay ADD StlInvoiceProformaCode nvarchar(255) NULL;
GO
IF COL_LENGTH('dbo.GrhWorkedDay', 'StrEntityId') IS NULL ALTER TABLE dbo.GrhWorkedDay ADD StrEntityId uniqueidentifier NULL;
GO
IF COL_LENGTH('dbo.GrhWorkedDay', 'CfgTrancheLevelingDefinitionId') IS NULL
ALTER TABLE dbo.GrhWorkedDay ADD CfgTrancheLevelingDefinitionId uniqueidentifier NULL;
GO
IF COL_LENGTH('dbo.GrhWorkedDay', 'TimeStart') IS NULL ALTER TABLE dbo.GrhWorkedDay ADD TimeStart datetime NULL;
GO
IF COL_LENGTH('dbo.GrhWorkedDay', 'TimeEnd') IS NULL ALTER TABLE dbo.GrhWorkedDay ADD TimeEnd datetime NULL;
GO
IF COL_LENGTH('dbo.GrhWorkedDay', 'Unity1') IS NULL ALTER TABLE dbo.GrhWorkedDay ADD Unity1 nvarchar(255) NULL;
GO
IF COL_LENGTH('dbo.GrhWorkedDay', 'VatRatio') IS NULL ALTER TABLE dbo.GrhWorkedDay ADD VatRatio decimal(26, 6) NULL;
GO
IF COL_LENGTH('dbo.GrhWorkedDay', 'VatRatio1') IS NULL ALTER TABLE dbo.GrhWorkedDay ADD VatRatio1 decimal(26, 6) NULL;
GO
IF COL_LENGTH('dbo.GrhWorkedDay', 'StrStatusId') IS NULL ALTER TABLE dbo.GrhWorkedDay ADD StrStatusId uniqueidentifier NULL;
GO
IF COL_LENGTH('dbo.GrhWorkedDay', 'CfgProjectZoneId') IS NULL
ALTER TABLE dbo.GrhWorkedDay ADD CfgProjectZoneId uniqueidentifier NULL;
GO
IF COL_LENGTH('dbo.GrhWorkedDay', 'CfgProjectZone_X') IS NULL ALTER TABLE dbo.GrhWorkedDay ADD CfgProjectZone_X float NULL;
GO
IF COL_LENGTH('dbo.GrhWorkedDay', 'CfgProjectZone_Y') IS NULL ALTER TABLE dbo.GrhWorkedDay ADD CfgProjectZone_Y float NULL;
GO

-- Lot admin-taches (FEATURE-admin-taches.md) : AdmTask complete, en definition de yesRemsProd.
-- La table cree par 149 n'a que trois colonnes ; yesRemsTest les a deja, chaque ajout est conditionnel.
IF COL_LENGTH('dbo.AdmTask', 'Code') IS NULL ALTER TABLE dbo.AdmTask ADD Code nvarchar(255) NULL;
IF COL_LENGTH('dbo.AdmTask', 'Description') IS NULL ALTER TABLE dbo.AdmTask ADD Description nvarchar(255) NULL;
IF COL_LENGTH('dbo.AdmTask', 'CfgTierTriggerId') IS NULL ALTER TABLE dbo.AdmTask ADD CfgTierTriggerId uniqueidentifier NULL;
IF COL_LENGTH('dbo.AdmTask', 'StartDatePrev') IS NULL ALTER TABLE dbo.AdmTask ADD StartDatePrev datetime NULL;
IF COL_LENGTH('dbo.AdmTask', 'DelayPrev') IS NULL ALTER TABLE dbo.AdmTask ADD DelayPrev int NULL;
IF COL_LENGTH('dbo.AdmTask', 'DueDatePrev') IS NULL ALTER TABLE dbo.AdmTask ADD DueDatePrev datetime NULL;
IF COL_LENGTH('dbo.AdmTask', 'StartDate') IS NULL ALTER TABLE dbo.AdmTask ADD StartDate datetime NULL;
IF COL_LENGTH('dbo.AdmTask', 'Delay') IS NULL ALTER TABLE dbo.AdmTask ADD Delay int NULL;
IF COL_LENGTH('dbo.AdmTask', 'EndDate') IS NULL ALTER TABLE dbo.AdmTask ADD EndDate datetime NULL;
IF COL_LENGTH('dbo.AdmTask', 'AdmModuleId') IS NULL ALTER TABLE dbo.AdmTask ADD AdmModuleId uniqueidentifier NULL;
IF COL_LENGTH('dbo.AdmTask', 'StrEntityId') IS NULL ALTER TABLE dbo.AdmTask ADD StrEntityId uniqueidentifier NULL;
IF COL_LENGTH('dbo.AdmTask', 'StrStatusId') IS NULL ALTER TABLE dbo.AdmTask ADD StrStatusId uniqueidentifier NULL;
IF COL_LENGTH('dbo.AdmTask', 'CfgTierManagerId') IS NULL ALTER TABLE dbo.AdmTask ADD CfgTierManagerId uniqueidentifier NULL;
IF COL_LENGTH('dbo.AdmTask', 'CfgTierAssignmentId') IS NULL ALTER TABLE dbo.AdmTask ADD CfgTierAssignmentId uniqueidentifier NULL;
IF COL_LENGTH('dbo.AdmTask', 'CfgCompanyId') IS NULL ALTER TABLE dbo.AdmTask ADD CfgCompanyId uniqueidentifier NULL;
IF COL_LENGTH('dbo.AdmTask', 'PrjProjectId') IS NULL ALTER TABLE dbo.AdmTask ADD PrjProjectId uniqueidentifier NULL;
IF COL_LENGTH('dbo.AdmTask', 'Notes') IS NULL ALTER TABLE dbo.AdmTask ADD Notes nvarchar(1000) NULL;
IF COL_LENGTH('dbo.AdmTask', 'UserCreate') IS NULL ALTER TABLE dbo.AdmTask ADD UserCreate nvarchar(255) NULL;
IF COL_LENGTH('dbo.AdmTask', 'UserCreateDateTime') IS NULL ALTER TABLE dbo.AdmTask ADD UserCreateDateTime datetime NULL;
IF COL_LENGTH('dbo.AdmTask', 'UserUpdate') IS NULL ALTER TABLE dbo.AdmTask ADD UserUpdate nvarchar(255) NULL;
IF COL_LENGTH('dbo.AdmTask', 'UserUpdateDateTime') IS NULL ALTER TABLE dbo.AdmTask ADD UserUpdateDateTime datetime NULL;
IF COL_LENGTH('dbo.AdmTask', 'ObjectId') IS NULL ALTER TABLE dbo.AdmTask ADD ObjectId uniqueidentifier NULL;
IF COL_LENGTH('dbo.AdmTask', 'ObjectLabel') IS NULL ALTER TABLE dbo.AdmTask ADD ObjectLabel nvarchar(1000) NULL;
IF COL_LENGTH('dbo.AdmTask', 'ObjectStrEntityId') IS NULL ALTER TABLE dbo.AdmTask ADD ObjectStrEntityId uniqueidentifier NULL;
IF COL_LENGTH('dbo.AdmTask', 'CfgProspectId') IS NULL ALTER TABLE dbo.AdmTask ADD CfgProspectId uniqueidentifier NULL;
GO

-- Réunions de chantier (FEATURE-n1-reunions-chantier.md) : colonnes de BulMeeting (165 n'en crée que deux) et tables Bul*,
-- colonnes de yesRemsProd ; puis les lignes de référence de production. Idempotent, applicable à yesRemsTest.
IF COL_LENGTH(N'dbo.BulMeeting', N'MeetingDate') IS NULL ALTER TABLE dbo.BulMeeting ADD MeetingDate datetime NULL;
IF COL_LENGTH(N'dbo.BulMeeting', N'NextDate') IS NULL ALTER TABLE dbo.BulMeeting ADD NextDate datetime NULL;
IF COL_LENGTH(N'dbo.BulMeeting', N'CfgEmployeeId') IS NULL ALTER TABLE dbo.BulMeeting ADD CfgEmployeeId uniqueidentifier NULL;
IF COL_LENGTH(N'dbo.BulMeeting', N'Code') IS NULL ALTER TABLE dbo.BulMeeting ADD Code nvarchar(255) NULL;
IF COL_LENGTH(N'dbo.BulMeeting', N'Sorting') IS NULL ALTER TABLE dbo.BulMeeting ADD Sorting int NULL;
IF COL_LENGTH(N'dbo.BulMeeting', N'Note') IS NULL ALTER TABLE dbo.BulMeeting ADD Note nvarchar(500) NULL;
IF COL_LENGTH(N'dbo.BulMeeting', N'UserCreate') IS NULL ALTER TABLE dbo.BulMeeting ADD UserCreate nvarchar(255) NULL;
IF COL_LENGTH(N'dbo.BulMeeting', N'UserCreateDateTime') IS NULL ALTER TABLE dbo.BulMeeting ADD UserCreateDateTime datetime NULL;
IF COL_LENGTH(N'dbo.BulMeeting', N'UserUpdate') IS NULL ALTER TABLE dbo.BulMeeting ADD UserUpdate nvarchar(255) NULL;
IF COL_LENGTH(N'dbo.BulMeeting', N'UserUpdateDateTime') IS NULL ALTER TABLE dbo.BulMeeting ADD UserUpdateDateTime datetime NULL;
IF COL_LENGTH(N'dbo.BulMeeting', N'BulMeetingTypeId') IS NULL ALTER TABLE dbo.BulMeeting ADD BulMeetingTypeId uniqueidentifier NULL;
GO
IF OBJECT_ID(N'dbo.BulMeetingType', N'U') IS NULL
CREATE TABLE dbo.BulMeetingType (PKey uniqueidentifier NOT NULL PRIMARY KEY, Sorting int NULL,
    Code nvarchar(25) NULL, Description nvarchar(25) NULL,
    UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL);
GO
IF OBJECT_ID(N'dbo.BulObservationStatus', N'U') IS NULL
CREATE TABLE dbo.BulObservationStatus (PKey uniqueidentifier NOT NULL PRIMARY KEY, Code nvarchar(255) NULL,
    Description nvarchar(255) NULL, ColorName nvarchar(255) NULL, Sorting int NULL,
    UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL,
    IsColsed bit NULL);
GO
IF OBJECT_ID(N'dbo.PrjMeetingConfigColor', N'U') IS NULL
CREATE TABLE dbo.PrjMeetingConfigColor (PKey uniqueidentifier NOT NULL PRIMARY KEY, Sorting int NULL,
    Description nvarchar(255) NULL, ColorName nvarchar(255) NULL, StartFrom decimal(26, 6) NULL, EndTo decimal(26, 6) NULL,
    UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL);
GO
IF OBJECT_ID(N'dbo.BulMeetingLine', N'U') IS NULL
CREATE TABLE dbo.BulMeetingLine (PKey uniqueidentifier NOT NULL PRIMARY KEY, Sorting int NULL, DueDate datetime NULL,
    Description varchar(500) NULL, Note varchar(500) NULL, IsClosed bit NULL, CfgSupplierId uniqueidentifier NULL,
    PrjMarketId uniqueidentifier NULL, BulMeetingId uniqueidentifier NULL, BulMeetingLineId uniqueidentifier NULL,
    BulObservationStatusId uniqueidentifier NULL,
    UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL,
    IsInternal bit NULL);
GO
IF OBJECT_ID(N'dbo.BulMeetingContact', N'U') IS NULL
CREATE TABLE dbo.BulMeetingContact (PKey uniqueidentifier NOT NULL PRIMARY KEY, Sorting int NULL,
    BulMeetingId uniqueidentifier NULL, ContactId uniqueidentifier NULL, IsPresent bit NULL, Notify bit NULL);
GO
IF OBJECT_ID(N'dbo.BulDelegate', N'U') IS NULL
CREATE TABLE dbo.BulDelegate (PKey uniqueidentifier NOT NULL PRIMARY KEY, Sorting int NULL,
    PrjMarketId uniqueidentifier NULL, BulMeetingId uniqueidentifier NULL,
    UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL, UserUpdateDateTime datetime NULL,
    Notes nvarchar(500) NULL, Effectif int NULL, CfgSupplierId uniqueidentifier NULL);
GO
IF OBJECT_ID(N'dbo.BulDelegateContact', N'U') IS NULL
CREATE TABLE dbo.BulDelegateContact (PKey uniqueidentifier NOT NULL PRIMARY KEY, Sorting int NULL,
    BulDelegateId uniqueidentifier NULL, ContactId uniqueidentifier NULL, IsPresent bit NULL, Notify bit NULL);
GO
IF NOT EXISTS (SELECT 1 FROM dbo.BulMeetingType WHERE PKey = 'DBD25148-C431-4A8E-91AA-26777A9E1EFF')
INSERT INTO dbo.BulMeetingType (PKey, Sorting, Code, Description)
VALUES ('DBD25148-C431-4A8E-91AA-26777A9E1EFF', 1, N'RDCHA', N'Réunion de chantier');
IF NOT EXISTS (SELECT 1 FROM dbo.BulMeetingType WHERE PKey = 'CA331B6B-35BD-41F9-B0FD-7161E89C200A')
INSERT INTO dbo.BulMeetingType (PKey, Sorting, Code, Description)
VALUES ('CA331B6B-35BD-41F9-B0FD-7161E89C200A', 2, N'RDCOO', N'Réunion de coordination');
GO
IF NOT EXISTS (SELECT 1 FROM dbo.BulObservationStatus WHERE PKey = '25800C35-3FA5-4637-BADC-CCBDB914E1A6')
INSERT INTO dbo.BulObservationStatus (PKey, Sorting, Code, Description, ColorName, IsColsed)
VALUES ('25800C35-3FA5-4637-BADC-CCBDB914E1A6', 0, N'A activer', N'A activer', N'RED', 0);
IF NOT EXISTS (SELECT 1 FROM dbo.BulObservationStatus WHERE PKey = '2051F23D-9C48-4FDD-B442-A0477EB4A542')
INSERT INTO dbo.BulObservationStatus (PKey, Sorting, Code, Description, ColorName, IsColsed)
VALUES ('2051F23D-9C48-4FDD-B442-A0477EB4A542', 1, N'En cours', N'En cours', N'RED', 0);
IF NOT EXISTS (SELECT 1 FROM dbo.BulObservationStatus WHERE PKey = '07160A7C-F898-4B87-ADD8-2D6A9DA1EF70')
INSERT INTO dbo.BulObservationStatus (PKey, Sorting, Code, Description, ColorName, IsColsed)
VALUES ('07160A7C-F898-4B87-ADD8-2D6A9DA1EF70', 2, N'Attention', N'Attention', N'RED', 0);
IF NOT EXISTS (SELECT 1 FROM dbo.BulObservationStatus WHERE PKey = '940185E6-9F44-4632-AF1B-D053A79CFC2B')
INSERT INTO dbo.BulObservationStatus (PKey, Sorting, Code, Description, ColorName, IsColsed)
VALUES ('940185E6-9F44-4632-AF1B-D053A79CFC2B', 3, N'Urgent', N'Urgent', N'RED', 0);
IF NOT EXISTS (SELECT 1 FROM dbo.BulObservationStatus WHERE PKey = '1A2361FE-3B04-436B-87F2-E4DEE03F47AB')
INSERT INTO dbo.BulObservationStatus (PKey, Sorting, Code, Description, ColorName, IsColsed)
VALUES ('1A2361FE-3B04-436B-87F2-E4DEE03F47AB', 4, N'Fait', N'Fait', N'RED', 1);
GO
IF NOT EXISTS (SELECT 1 FROM dbo.PrjMeetingConfigColor WHERE PKey = '90260ECE-F402-40F9-927F-3DDA7E81AE4A')
INSERT INTO dbo.PrjMeetingConfigColor (PKey, Sorting, Description, ColorName, StartFrom, EndTo)
VALUES ('90260ECE-F402-40F9-927F-3DDA7E81AE4A', 0, N'Faible', N'YELLOW', 0, 0.2);
IF NOT EXISTS (SELECT 1 FROM dbo.PrjMeetingConfigColor WHERE PKey = '0BB03196-FC62-416E-A1D7-734585565B19')
INSERT INTO dbo.PrjMeetingConfigColor (PKey, Sorting, Description, ColorName, StartFrom, EndTo)
VALUES ('0BB03196-FC62-416E-A1D7-734585565B19', 0, N'Moyenne', N'BLUE', 0.2, 0.6);
IF NOT EXISTS (SELECT 1 FROM dbo.PrjMeetingConfigColor WHERE PKey = 'D527DBAA-EE7C-4316-AC0F-6C1725A6A8D1')
INSERT INTO dbo.PrjMeetingConfigColor (PKey, Sorting, Description, ColorName, StartFrom, EndTo)
VALUES ('D527DBAA-EE7C-4316-AC0F-6C1725A6A8D1', 0, N'Haute', N'GREEN', 0.6, 1);
GO

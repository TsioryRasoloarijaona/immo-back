-- FEATURE-patrimoine-relances-champs.md (P9d) : champs spécifiques OMRAN (GEN0007), colonnes de production ; absentes de
-- 010_tables.sql et de yesRemsTest. Les 8 types de données sont posés par les tests (PKey de production).
IF OBJECT_ID('dbo.AdmSpecificFieldEntity', 'U') IS NULL
CREATE TABLE dbo.AdmSpecificFieldEntity (
    PKey uniqueidentifier NOT NULL PRIMARY KEY,
    Code nvarchar(255) NULL,
    Description nvarchar(255) NULL,
    Sorting int NULL,
    UserCreate nvarchar(255) NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) NULL,
    UserUpdateDateTime datetime NULL);
GO
IF OBJECT_ID('dbo.AdmSpecificFieldDataType', 'U') IS NULL
CREATE TABLE dbo.AdmSpecificFieldDataType (
    PKey uniqueidentifier NOT NULL PRIMARY KEY,
    Code nvarchar(255) NULL,
    Sorting int NULL,
    UserCreate nvarchar(255) NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) NULL,
    UserUpdateDateTime datetime NULL);
GO
IF OBJECT_ID('dbo.AdmSpecificField', 'U') IS NULL
CREATE TABLE dbo.AdmSpecificField (
    PKey uniqueidentifier NOT NULL PRIMARY KEY,
    AdmSpecificFieldEntityId uniqueidentifier NULL,
    Category nvarchar(255) NULL,
    Name nvarchar(255) NULL,
    Caption nvarchar(255) NULL,
    AdmSpecificFieldDataTypeId uniqueidentifier NULL,
    DefaultValue nvarchar(255) NULL,
    Required bit NULL,
    UserCreate nvarchar(255) NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) NULL,
    UserUpdateDateTime datetime NULL,
    DateValue datetime NULL,
    BoolValue bit NULL,
    DecimalValue decimal(26,6) NULL,
    IntValue int NULL,
    CategorySorting int NULL,
    Sorting int NULL,
    ComboValue nvarchar(255) NULL);
GO
IF OBJECT_ID('dbo.AdmSpecificFieldValue', 'U') IS NULL
CREATE TABLE dbo.AdmSpecificFieldValue (
    PKey uniqueidentifier NOT NULL PRIMARY KEY,
    AdmSpecificFieldId uniqueidentifier NULL,
    NameSpaceId uniqueidentifier NULL,
    Value nvarchar(1000) NULL,
    DateValue datetime NULL,
    BoolValue bit NULL,
    DecimalValue decimal(26,6) NULL,
    IntValue int NULL,
    ComboValue nvarchar(255) NULL,
    UserCreate nvarchar(255) NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) NULL,
    UserUpdateDateTime datetime NULL);
GO

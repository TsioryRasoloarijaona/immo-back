-- Lot admin-utilisateurs (FEATURE-admin-utilisateurs.md) : definition de yesRemsProd.
IF OBJECT_ID('dbo.StlDepositUser', 'U') IS NULL
CREATE TABLE dbo.StlDepositUser (
    PKey uniqueidentifier NOT NULL CONSTRAINT StlDepositUser_PK PRIMARY KEY,
    AdmUserId uniqueidentifier NULL,
    StlDepositId uniqueidentifier NULL,
    UserCreate nvarchar(255) NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) NULL,
    UserUpdateDateTime datetime NULL
);
GO

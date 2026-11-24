-- Lot N3-bis b2a : lignes de retenue à la source d'un règlement (schéma de prod). Idempotent.
-- StlDeductionType est déjà créée par 010_tables.sql.
IF OBJECT_ID('dbo.StlDeduction', 'U') IS NULL
CREATE TABLE dbo.StlDeduction (
    PKey uniqueidentifier NOT NULL CONSTRAINT StlDeduction_PK PRIMARY KEY,
    AmountBase decimal(26, 6) NULL,
    StlSettlementId uniqueidentifier NULL,
    RsRatio decimal(26, 6) NULL,
    AmountRS decimal(26, 6) NULL,
    Amount decimal(26, 6) NULL,
    Notes nvarchar(max) NULL,
    UserCreate nvarchar(255) NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) NULL,
    UserUpdateDateTime datetime NULL,
    StlDeductionTypeId uniqueidentifier NULL
);
GO

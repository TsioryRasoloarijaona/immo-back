-- Désistements (FEATURE-desistements.md §7 bis.6) : déjà en CI (010_tables.sql), sans effet ici ; à appliquer à
-- yesRemsTest, qui n'a ni ComSaleWithdrawalCancellation ni ComSaleWithdrawalReason. DDL de yesRemsProd. Idempotent.
IF OBJECT_ID('dbo.ComSaleWithdrawalReason', 'U') IS NULL
    CREATE TABLE dbo.ComSaleWithdrawalReason (
        PKey uniqueidentifier NOT NULL CONSTRAINT ComSaleWithdrawalReason_PK PRIMARY KEY,
        Code nvarchar(255) NULL,
        Description nvarchar(255) NULL,
        Sorting int NULL,
        UserCreate nvarchar(255) NULL,
        UserCreateDateTime datetime NULL,
        UserUpdate nvarchar(255) NULL,
        UserUpdateDateTime datetime NULL);
GO
IF OBJECT_ID('dbo.ComSaleWithdrawalCancellation', 'U') IS NULL
    CREATE TABLE dbo.ComSaleWithdrawalCancellation (
        PKey uniqueidentifier NOT NULL CONSTRAINT ComSaleWithdrawalCancellation_PK PRIMARY KEY,
        Code nvarchar(255) NULL,
        ComSaleId uniqueidentifier NULL,
        TotalToPay decimal(26, 6) NULL,
        TotalRest decimal(26, 6) NULL,
        TotalSettled decimal(26, 6) NULL,
        StkItemId uniqueidentifier NULL,
        CfgOldCustomerId uniqueidentifier NULL,
        NewStkItemTotalToPay decimal(26, 6) NULL,
        StkOldItemId uniqueidentifier NULL,
        ClaimDate datetime NULL,
        DecisionDate datetime NULL,
        ValidityDate datetime NULL,
        TransfertExpenseAmount decimal(26, 6) NULL,
        ReimburseAmount decimal(26, 6) NULL,
        IsLegalized bit NULL,
        LegalizationDate datetime NULL,
        ComSaleWithdrawalReasonId uniqueidentifier NULL,
        ComProspectionOriginId uniqueidentifier NULL,
        Discount decimal(26, 6) NULL,
        UserCreate nvarchar(255) NULL,
        UserCreateDateTime datetime NULL,
        UserUpdate nvarchar(255) NULL,
        UserUpdateDateTime datetime NULL,
        TransfertExpenseAmountAfterDiscount decimal(26, 6) NULL,
        Notes nvarchar(1000) NULL,
        StlSettlementTypeId uniqueidentifier NULL,
        Reference varchar(255) NULL,
        CfgBankId uniqueidentifier NULL,
        IsAccounted bit NULL,
        CfgContactId uniqueidentifier NULL);
GO
IF COL_LENGTH('dbo.ComDocument', 'ComSaleWithdrawalCancellationId') IS NULL
    ALTER TABLE dbo.ComDocument ADD ComSaleWithdrawalCancellationId uniqueidentifier NULL;
GO

-- Lot N3-bis a saisie des décaissements (FEATURE-n3bis-saisie-reglements.md D9) : historique des échéances d'un
-- règlement, absent de 010 et de yesRemsTest. Définition de yesRemsProd. Idempotent.
IF OBJECT_ID('dbo.StlSettlementHistory', 'U') IS NULL
CREATE TABLE dbo.StlSettlementHistory (
    PKey uniqueidentifier NOT NULL CONSTRAINT StlSettlementHistory_PK PRIMARY KEY,
    AdmUserId uniqueidentifier NULL, DocDate datetime NULL, EcheanceDate datetime NULL,
    StlSettlementId uniqueidentifier NULL, StlSettlementTypeId uniqueidentifier NULL, Reference nvarchar(255) NULL,
    UserCreate nvarchar(255) NULL, UserCreateDateTime datetime NULL, UserUpdate nvarchar(255) NULL,
    UserUpdateDateTime datetime NULL);
GO

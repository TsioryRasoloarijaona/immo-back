-- Documents à joindre d'un statut d'achat (FEATURE-statuts-achat-parite.md R8), colonnes de yesRemsProd.
IF OBJECT_ID('dbo.BuyDocumentStatusDocumentToAttach', 'U') IS NULL
CREATE TABLE dbo.BuyDocumentStatusDocumentToAttach (
    PKey uniqueidentifier NOT NULL CONSTRAINT BuyDocumentStatusDocumentToAttach_PK PRIMARY KEY,
    BuyDocumentStatusId uniqueidentifier NULL,
    IsMandatory bit NULL,
    WithTitle bit NULL,
    UserCreate nvarchar(255) NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) NULL,
    UserUpdateDateTime datetime NULL,
    AdmAttachedFileTypeId uniqueidentifier NULL);
GO

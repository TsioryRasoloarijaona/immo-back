-- Liens bon de commande ↔ facture (FEATURE-achat-fiche-parite.md E4, Q4), colonnes de yesRemsProd.
IF OBJECT_ID('dbo.BuyOrderInvoice', 'U') IS NULL
CREATE TABLE dbo.BuyOrderInvoice (
    PKey uniqueidentifier NOT NULL CONSTRAINT BuyOrderInvoice_PK PRIMARY KEY,
    BuyOrderId uniqueidentifier NULL,
    BuyInvoiceId uniqueidentifier NULL,
    UserCreate nvarchar(255) NULL,
    UserCreateDateTime datetime NULL,
    UserUpdate nvarchar(255) NULL,
    UserUpdateDateTime datetime NULL);
GO

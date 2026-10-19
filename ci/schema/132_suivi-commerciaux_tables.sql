-- Suivi des commerciaux (FEATURE-suivi-commerciaux.md §7.6) : AppParametre, comme db/migration/018. Idempotent.
IF OBJECT_ID('dbo.AppParametre', 'U') IS NULL
CREATE TABLE dbo.AppParametre (
    PKey uniqueidentifier NOT NULL,
    Cle nvarchar(100) NOT NULL,
    Valeur nvarchar(500) NULL,
    CONSTRAINT PK_AppParametre PRIMARY KEY CLUSTERED (PKey ASC),
    CONSTRAINT UQ_AppParametre_Cle UNIQUE (Cle)
);
GO

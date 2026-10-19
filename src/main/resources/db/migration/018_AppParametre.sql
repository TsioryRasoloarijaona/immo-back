-- 018 : AppParametre, paramètres d'application réglables par l'Administrateur (Cle unique, Valeur texte).
-- Premier usage : objectifPersonnesJointesJour (FEATURE-suivi-commerciaux.md R7, §7.6) ; ligne absente = défaut du code.
-- A appliquer sur la source de vérité ET la copie de lecture (yesRems, yesRemsDev) ; idempotent.

IF OBJECT_ID('dbo.AppParametre', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.AppParametre
    (
        PKey   uniqueidentifier NOT NULL,
        Cle    nvarchar(100)    NOT NULL,
        Valeur nvarchar(500)    NULL,

        CONSTRAINT PK_AppParametre PRIMARY KEY CLUSTERED (PKey ASC),
        CONSTRAINT UQ_AppParametre_Cle UNIQUE (Cle)
    );

    PRINT '018: table AppParametre creee.';
END
ELSE
BEGIN
    PRINT '018: table AppParametre deja presente, rien a faire.';
END
GO

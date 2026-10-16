/*
 * 017 — AppModeleDocument / AppModeleDocumentVersion : modèles de documents modifiables
 *
 * Un modèle par (Categorie, Code) ; chaque enregistrement ajoute une version, jamais d'écrasement.
 * VersionCouranteId sans clé étrangère : la référence circulaire empêcherait le rapatriement
 * (MERGE) de recopier un modèle neuf vers la copie de lecture. Voir
 * instructions/FEATURE-modeles-documents.md §3.
 *
 * A appliquer sur chaque base servie par l'API : la source de vérité ET la copie de lecture
 * (yesRemsProd en local, yesRemsDev sur le VPS, yesRemsTest pour la suite).
 *
 * Idempotent : ne fait rien si les tables existent déjà.
 */

IF OBJECT_ID('dbo.AppModeleDocument', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.AppModeleDocument
    (
        PKey              uniqueidentifier NOT NULL,
        Categorie         nvarchar(40)     NOT NULL,
        Code              nvarchar(60)     NOT NULL,
        Libelle           nvarchar(200)    NOT NULL,
        Ordre             int              NOT NULL,
        Actif             bit              NOT NULL,
        VersionCouranteId uniqueidentifier NULL,
        CreeLe            datetime2(7)     NOT NULL,
        CreePar           nvarchar(255)    NOT NULL,
        ModifieLe         datetime2(7)     NOT NULL,
        ModifiePar        nvarchar(255)    NOT NULL,

        CONSTRAINT PK_AppModeleDocument
            PRIMARY KEY CLUSTERED (PKey),

        CONSTRAINT UQ_AppModeleDocument_Categorie_Code
            UNIQUE (Categorie, Code)
    );

    PRINT '017: table AppModeleDocument creee.';
END
ELSE
BEGIN
    PRINT '017: table AppModeleDocument deja presente, rien a faire.';
END
GO

IF OBJECT_ID('dbo.AppModeleDocumentVersion', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.AppModeleDocumentVersion
    (
        PKey                uniqueidentifier NOT NULL,
        AppModeleDocumentId uniqueidentifier NOT NULL,
        Numero              int              NOT NULL,
        Contenu             nvarchar(max)    NOT NULL,
        MiseEnPage          nvarchar(400)    NOT NULL,
        CreeLe              datetime2(7)     NOT NULL,
        CreePar             nvarchar(255)    NOT NULL,

        CONSTRAINT PK_AppModeleDocumentVersion
            PRIMARY KEY CLUSTERED (PKey),

        CONSTRAINT UQ_AppModeleDocumentVersion_Modele_Numero
            UNIQUE (AppModeleDocumentId, Numero),

        CONSTRAINT FK_AppModeleDocumentVersion_AppModeleDocument
            FOREIGN KEY (AppModeleDocumentId) REFERENCES dbo.AppModeleDocument (PKey)
    );

    PRINT '017: table AppModeleDocumentVersion creee.';
END
ELSE
BEGIN
    PRINT '017: table AppModeleDocumentVersion deja presente, rien a faire.';
END
GO

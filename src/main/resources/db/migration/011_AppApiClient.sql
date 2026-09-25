/*
 * 011 — AppApiClient : clés d'API des clients machine (n8n, agents)
 *
 * Une clé = un secret aléatoire montré une seule fois, dont seul le hash SHA-256 est stocké,
 * rattaché à un compte de service AdmUser (auteur des écritures, périmètre) et limité à une
 * portée (codes PorteeCleApi, séparés par des virgules). Voir
 * instructions/FEATURE-cle-api-n8n.md.
 *
 * A appliquer sur chaque base servie par l'API (yesRemsProd en local, yesRemsDev sur le VPS,
 * yesRemsTest pour la suite). Le compte de service et la clé se créent ensuite par
 * POST /api/cles-api, en tant qu'Administrateur — jamais à la main dans cette table.
 *
 * Idempotent : ne fait rien si la table existe déjà.
 */

IF OBJECT_ID('dbo.AppApiClient', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.AppApiClient
    (
        PKey               uniqueidentifier NOT NULL,
        Nom                nvarchar(100)    NOT NULL,
        CleHash            nvarchar(64)     NOT NULL,
        ClePrefixe         nvarchar(12)     NOT NULL,
        AdmUserId          uniqueidentifier NOT NULL,
        Portees            nvarchar(1000)   NOT NULL,
        Actif              bit              NOT NULL,
        DernierUsage       datetime2(0)     NULL,
        UserCreate         nvarchar(255)    NULL,
        UserCreateDateTime datetime2(7)     NOT NULL,
        RevoquePar         nvarchar(255)    NULL,
        RevoqueDateTime    datetime2(7)     NULL,

        CONSTRAINT PK_AppApiClient
            PRIMARY KEY CLUSTERED (PKey),

        -- Un nom identifie un client ; le hash identifie la clé à chaque requête.
        CONSTRAINT UQ_AppApiClient_Nom
            UNIQUE (Nom),
        CONSTRAINT UQ_AppApiClient_CleHash
            UNIQUE (CleHash),

        CONSTRAINT FK_AppApiClient_AdmUser
            FOREIGN KEY (AdmUserId) REFERENCES dbo.AdmUser (PKey)
    );

    PRINT '011: table AppApiClient creee.';
END
ELSE
BEGIN
    PRINT '011: table AppApiClient deja presente, rien a faire.';
END
GO

/*
 * 016 — AppNotification : notifications de l'application (cloche)
 *
 * Une ligne par notification et par compte destinataire (AdmUser) : leads attribués, leads
 * à distribuer, résumé de la demi-journée. LueLe NULL = non lue. Voir
 * instructions/FEATURE-repartition-notifications.md §3.3.
 *
 * A appliquer sur chaque base servie par l'API : la source de vérité ET la copie de lecture
 * (yesRemsProd en local, yesRemsDev sur le VPS, yesRemsTest pour la suite).
 *
 * Idempotent : ne fait rien si la table existe déjà.
 */

IF OBJECT_ID('dbo.AppNotification', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.AppNotification
    (
        PKey      uniqueidentifier NOT NULL,
        AdmUserId uniqueidentifier NOT NULL,
        Type      nvarchar(40)     NOT NULL,
        Titre     nvarchar(200)    NOT NULL,
        Message   nvarchar(1000)   NOT NULL,
        Lien      nvarchar(300)    NULL,
        CreeLe    datetime2(7)     NOT NULL,
        LueLe     datetime2(7)     NULL,

        CONSTRAINT PK_AppNotification
            PRIMARY KEY CLUSTERED (PKey),

        CONSTRAINT FK_AppNotification_AdmUser
            FOREIGN KEY (AdmUserId) REFERENCES dbo.AdmUser (PKey)
    );

    -- Cloche : non lues d'un compte, plus récentes d'abord.
    CREATE NONCLUSTERED INDEX IX_AppNotification_AdmUserId_LueLe_CreeLe
        ON dbo.AppNotification (AdmUserId ASC, LueLe ASC, CreeLe ASC);

    PRINT '016: table AppNotification creee.';
END
ELSE
BEGIN
    PRINT '016: table AppNotification deja presente, rien a faire.';
END
GO

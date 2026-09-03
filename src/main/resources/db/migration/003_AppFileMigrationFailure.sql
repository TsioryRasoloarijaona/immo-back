/*
 * 003 — AppFileMigrationFailure
 *
 * Journal d'échecs du script de migration des binaires vers S3.
 *
 * La reprise ne s'appuie PAS sur cette table : la liste de travail est reconstruite à
 * chaque exécution par anti-jointure sur `AppFileObject` (une ligne y est écrite
 * uniquement après un envoi vérifié, donc sa présence vaut "fait"). Ce journal sert
 * au diagnostic et de critère d'arrêt : la phase 3 (vidage des binaires legacy) est
 * conditionnée à l'absence d'échec non résolu.
 *
 * Idempotent : ne fait rien si la table existe déjà.
 */

IF OBJECT_ID('dbo.AppFileMigrationFailure', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.AppFileMigrationFailure
    (
        PKey              uniqueidentifier NOT NULL CONSTRAINT DF_AppFileMigrationFailure_PKey DEFAULT NEWID(),
        AdmAttachedFileId uniqueidentifier NOT NULL,

        -- Numéro de tentative au sein de l'exécution qui a échoué (1..N).
        Attempt           int              NOT NULL,

        -- Classe d'erreur (ex. 'ClientError', 'EndpointConnectionError', 'pyodbc.Error').
        ErrorType         nvarchar(255)    NULL,
        ErrorMessage      nvarchar(max)    NULL,

        -- Clé S3 visée, pour rejouer ou nettoyer manuellement le cas échéant.
        StorageKey        nvarchar(512)    NULL,

        FailedAt          datetime2(7)     NOT NULL,

        CONSTRAINT PK_AppFileMigrationFailure PRIMARY KEY CLUSTERED (PKey)
    );

    -- Plusieurs échecs possibles pour un même fichier (exécutions successives) : index
    -- non unique, pour retrouver l'historique d'un id précis.
    CREATE NONCLUSTERED INDEX IX_AppFileMigrationFailure_AdmAttachedFileId
        ON dbo.AppFileMigrationFailure (AdmAttachedFileId);

    PRINT '003: table AppFileMigrationFailure creee.';
END
ELSE
BEGIN
    PRINT '003: table AppFileMigrationFailure deja presente, rien a faire.';
END
GO

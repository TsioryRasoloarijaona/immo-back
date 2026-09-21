/*
 * 009 — AppSyncEtat : filigranes de la synchronisation descendante (Change Tracking)
 *
 * Un filigrane PAR TABLE suivie : une table qui echoue (FK, indisponibilite) est rejouee
 * depuis son propre point au cycle suivant, sans bloquer les autres. Les compteurs de la
 * derniere passe ne servent qu'au diagnostic.
 *
 * A APPLIQUER SUR LES COPIES DE LECTURE UNIQUEMENT : yesRemsProd (banc local) et yesRemsDev
 * (VPS). JAMAIS sur la source (115) — l'etat appartient au consommateur, pas a la verite.
 *
 * DEMARRAGE SUR UNE COPIE FRAICHEMENT RESTAUREE DEPUIS LA SOURCE : pre-remplir les filigranes
 * a la version courante evite une baseline integrale qui ne changerait rien :
 *
 *   INSERT INTO copie.dbo.AppSyncEtat (TableNom, DerniereVersion, DerniereExecution)
 *   SELECT t.name, CHANGE_TRACKING_CURRENT_VERSION(), SYSDATETIME()
 *   FROM sys.change_tracking_tables ctt JOIN sys.tables t ON t.object_id = ctt.object_id;
 *   -- a executer CONNECTE A LA SOURCE (les fonctions CT repondent pour la base courante),
 *   -- en visant la copie par nommage a trois parties ou en recopiant les valeurs.
 *
 * Sans pre-remplissage, le premier cycle fait une baseline complete de chaque table : correct,
 * jamais destructif (la baseline ne supprime rien), juste long.
 *
 * Idempotent : ne fait rien si la table existe deja.
 */

IF OBJECT_ID('dbo.AppSyncEtat') IS NULL
BEGIN
    CREATE TABLE dbo.AppSyncEtat (
        TableNom           sysname      NOT NULL CONSTRAINT PK_AppSyncEtat PRIMARY KEY,
        DerniereVersion    bigint       NULL,
        DerniereExecution  datetime2(0) NULL
    );
    PRINT '009: table AppSyncEtat creee.';
END
ELSE
    PRINT '009: AppSyncEtat existe deja, rien a faire.';

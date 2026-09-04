/*
 * 004 — Suppression de AdmAttachedFile.AttachedFile et du groupe de fichiers FILESTREAM
 *
 * POURQUOI : SQL Server sur Linux ne prend pas FILESTREAM en charge. Une base qui
 * DÉCLARE un groupe de fichiers FILESTREAM est refusée au RESTORE, même si ce groupe
 * est vide — c'est la déclaration qui est rejetée, pas son contenu. Sans ce script, la
 * sauvegarde ne peut pas être restaurée sur le VPS.
 *
 * Après la migration vers S3, la colonne n'a plus d'utilité : les binaires sont dans
 * le stockage objet et `AppFileObject` porte les pointeurs.
 *
 * ORDRE : ce script vient APRÈS la migration ET la purge :
 *     1. python migrate_blobs_to_s3.py --prefix prod                 (envoi)
 *     2. python migrate_blobs_to_s3.py --prefix prod --verify        (contrôle)
 *     3. migration/purge_attached_file.sql                           (vidage)
 *     4. CHECKPOINT + sp_filestream_force_garbage_collection         (récupération)
 *     5. CE SCRIPT
 *
 * GARDE-FOU : le script REFUSE de s'exécuter s'il reste le moindre binaire. Sur une
 * base non migrée (`yesRems` en développement, par exemple), il échoue bruyamment
 * plutôt que de détruire des données — c'est voulu. Un échec visible vaut mieux qu'une
 * suppression silencieuse.
 *
 * IMPACT JAVA — à traiter avant de déployer le backend sur une base ainsi migrée :
 *   - `AdmAttachedFile.attachedFile` mappe une colonne qui n'existera plus ;
 *   - `AdmAttachedFileSummary.hasFile` porte une @Formula qui NOMME cette colonne :
 *     toute requête de LISTE échouerait, pas seulement les téléchargements.
 * Voir instructions/FEATURE-s3-storage.md §3.2 et §3.3.
 *
 * Idempotent : relançable sans effet si la colonne et le groupe sont déjà supprimés.
 */

SET NOCOUNT ON;
SET XACT_ABORT ON;

DECLARE @restants INT = 0;
DECLARE @sql      NVARCHAR(MAX);
DECLARE @db       SYSNAME = DB_NAME();
DECLARE @fichier  SYSNAME;
DECLARE @groupe   SYSNAME;

/* ---------------------------------------------------------------- 1. la colonne */

IF COL_LENGTH('dbo.AdmAttachedFile', 'AttachedFile') IS NOT NULL
BEGIN
    -- SQL dynamique : référencer la colonne en clair ferait échouer l'analyse du
    -- script une fois celle-ci supprimée, donc dès la deuxième exécution.
    EXEC sp_executesql
        N'SELECT @n = COUNT(*) FROM dbo.AdmAttachedFile WHERE AttachedFile IS NOT NULL',
        N'@n INT OUTPUT', @n = @restants OUTPUT;

    IF @restants > 0
    BEGIN
        RAISERROR('004 REFUSE : %d binaire(s) encore presents dans AdmAttachedFile.AttachedFile. Lancer la migration S3, --verify, puis migration/purge_attached_file.sql avant ce script.', 16, 1, @restants);
        RETURN;
    END

    EXEC sp_executesql N'ALTER TABLE dbo.AdmAttachedFile DROP COLUMN AttachedFile';
    PRINT '004: colonne AdmAttachedFile.AttachedFile supprimee.';
END
ELSE
BEGIN
    PRINT '004: colonne AdmAttachedFile.AttachedFile deja absente.';
END

/* ------------------------------------- 2. le rattachement de la TABLE au groupe */
/*
 * Étape indispensable et facile à manquer : supprimer la colonne ne détache pas la
 * table du groupe de fichiers. `AdmAttachedFile` conserve sa clause FILESTREAM_ON
 * (visible dans sys.tables.filestream_data_space_id), et tant qu'elle est là, SQL
 * Server refuse de retirer le fichier avec « ne peut pas être supprimée parce qu'elle
 * n'est pas vide » — message trompeur, le conteneur étant bel et bien vide.
 *
 * QUOTED_IDENTIFIER doit être actif : la syntaxe attend le littéral "NULL" entre
 * guillemets doubles.
 */

IF EXISTS (SELECT 1 FROM sys.tables
           WHERE object_id = OBJECT_ID('dbo.AdmAttachedFile')
             AND filestream_data_space_id IS NOT NULL)
BEGIN
    SET QUOTED_IDENTIFIER ON;
    EXEC sp_executesql N'ALTER TABLE dbo.AdmAttachedFile SET (FILESTREAM_ON = "NULL")';
    PRINT '004: table AdmAttachedFile detachee du groupe de fichiers FILESTREAM.';

    CHECKPOINT;
    EXEC sp_filestream_force_garbage_collection @dbname = @db;
END

/* --------------------------------------------------- 3. le groupe de fichiers */
/*
 * Ne se fait qu'une fois la dernière colonne FILESTREAM de la base supprimee — ici
 * AttachedFile etait la seule (verifie sur sys.columns.is_filestream).
 */

IF EXISTS (SELECT 1 FROM sys.columns WHERE is_filestream = 1)
BEGIN
    PRINT '004: d''autres colonnes FILESTREAM subsistent, groupe de fichiers conserve.';
END
ELSE
BEGIN
    SELECT TOP 1 @fichier = df.name, @groupe = fg.name
    FROM sys.database_files df
    JOIN sys.filegroups fg ON fg.data_space_id = df.data_space_id
    WHERE fg.type = 'FD';

    IF @fichier IS NULL
    BEGIN
        PRINT '004: aucun groupe de fichiers FILESTREAM, rien a faire.';
    END
    ELSE
    BEGIN
        SET @sql = N'ALTER DATABASE ' + QUOTENAME(@db) + N' REMOVE FILE ' + QUOTENAME(@fichier) + N';';
        EXEC sp_executesql @sql;
        PRINT '004: fichier FILESTREAM ' + @fichier + ' retire.';

        SET @sql = N'ALTER DATABASE ' + QUOTENAME(@db) + N' REMOVE FILEGROUP ' + QUOTENAME(@groupe) + N';';
        EXEC sp_executesql @sql;
        PRINT '004: groupe de fichiers ' + @groupe + ' retire. Base restaurable sur SQL Server pour Linux.';
    END
END
GO

/*
 * NOTE — `PKey` conserve sa propriete ROWGUIDCOL et sa contrainte d'unicite dediee
 * (`UQ__AdmAttac__...`), toutes deux exigees autrefois par FILESTREAM. Elles sont
 * desormais inutiles mais parfaitement supportees sur Linux, et les retirer toucherait
 * a la cle primaire pour rien. Laissees en place a dessein.
 */

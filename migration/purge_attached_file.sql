/*
 * PURGE DES BINAIRES — vide AdmAttachedFile.AttachedFile après migration vérifiée.
 *
 * ============================ OPÉRATION DESTRUCTRICE ============================
 * Ce script efface 8 Go de contrats, chèques et pièces d'identité de la base. Après
 * exécution, le stockage objet est la SEULE copie de ces fichiers.
 * ===============================================================================
 *
 * VOLONTAIREMENT HORS DE src/main/resources/db/migration/ : ce répertoire suit la
 * convention Flyway. Un script de purge qui s'y trouverait serait exécuté
 * automatiquement au démarrage du backend le jour où Flyway serait adopté. Il n'a
 * rien à y faire — ce n'est pas du DDL, c'est une opération de données ponctuelle.
 *
 * PRÉREQUIS ABSOLU — lancer d'abord la vérification exhaustive, qui compare taille et
 * empreinte SHA-256 de chaque objet entre la base et le stockage :
 *
 *     python migrate_blobs_to_s3.py --prefix prod --verify
 *
 * Elle doit afficher « VÉRIFICATION CONFORME » et sortir en code 0. Les garde-fous
 * ci-dessous recontrôlent la cohérence côté base, mais ils ne peuvent PAS savoir si
 * les objets sont réellement lisibles sur le stockage — seul --verify le prouve.
 *
 * EXÉCUTION — la confirmation est explicite et se fait EN ÉDITANT CE FICHIER, à la
 * ligne @confirmation ci-dessous : remplacer 'NON' par 'OUI'.
 *
 * Exécuté tel quel, le script n'écrit rien : il se contente d'afficher les compteurs
 * puis s'arrête. C'est voulu — lancer le fichier par mégarde ne peut rien détruire.
 *
 * Depuis n'importe quel client SQL (DataGrip, SSMS, Azure Data Studio) ou en ligne de
 * commande :
 *
 *     sqlcmd -S "<hote>" -U <user> -P <pwd> -d yesRemsProd -C -b \
 *            -i migration/purge_attached_file.sql
 *
 * PENSER À REMETTRE 'NON' APRÈS USAGE.
 *
 * ---------------------------------------------------------------------------------
 * NOTE TECHNIQUE — pourquoi tout ce qui touche AttachedFile passe par du SQL dynamique
 *
 * SQL Server analyse un lot ENTIER avant de l'exécuter, et la résolution des noms de
 * colonnes n'est pas différée (contrairement aux noms de tables). Un simple
 * `IF COL_LENGTH(...) IS NULL RETURN;` en tête ne protège donc de rien : le lot est
 * rejeté à la compilation avec « Nom de colonne non valide : AttachedFile » dès que la
 * colonne a été supprimée par 004_drop_attachedfile_filestream.sql. D'où sp_executesql
 * pour les quatre requêtes qui la nomment.
 * ---------------------------------------------------------------------------------
 */

SET NOCOUNT ON;

--  ┌────────────────────────────────────────────────────────────────────────────┐
--  │  Mettre N'OUI' pour autoriser la purge. Toute autre valeur = simple etat.   │
--  └────────────────────────────────────────────────────────────────────────────┘
DECLARE @confirmation NVARCHAR(20) = N'NON';

DECLARE @restants INT;
DECLARE @echecs   INT;
DECLARE @ecarts   INT;
DECLARE @aPurger  INT;
DECLARE @octets   BIGINT;
DECLARE @vides    INT;

IF COL_LENGTH('dbo.AdmAttachedFile', 'AttachedFile') IS NULL
BEGIN
    PRINT 'Colonne AdmAttachedFile.AttachedFile absente (004_drop_attachedfile_filestream.sql';
    PRINT 'deja applique). Plus aucun binaire en base : rien a purger.';
    RETURN;
END

-- 1. Tout fichier encore porteur d'un binaire doit avoir sa ligne AppFileObject.
EXEC sp_executesql N'
    SELECT @n = COUNT(*)
    FROM dbo.AdmAttachedFile a
    WHERE a.AttachedFile IS NOT NULL
      AND NOT EXISTS (SELECT 1 FROM dbo.AppFileObject o WHERE o.AdmAttachedFileId = a.PKey)',
    N'@n INT OUTPUT', @n = @restants OUTPUT;

-- 2. Aucun échec de migration non résolu.
SELECT @echecs = COUNT(*) FROM dbo.AppFileMigrationFailure;

-- 3. Aucun écart de taille entre la source et ce qui a été enregistré.
EXEC sp_executesql N'
    SELECT @n = COUNT(*)
    FROM dbo.AppFileObject o
    JOIN dbo.AdmAttachedFile a ON a.PKey = o.AdmAttachedFileId
    WHERE a.AttachedFile IS NOT NULL
      AND o.SizeBytes <> DATALENGTH(a.AttachedFile)',
    N'@n INT OUTPUT', @n = @ecarts OUTPUT;

EXEC sp_executesql N'
    SELECT @n = COUNT(*), @o = ISNULL(SUM(CAST(DATALENGTH(a.AttachedFile) AS BIGINT)), 0)
    FROM dbo.AdmAttachedFile a
    JOIN dbo.AppFileObject o ON o.AdmAttachedFileId = a.PKey
    WHERE a.AttachedFile IS NOT NULL
      AND o.DeletedAt IS NULL
      AND o.SizeBytes = DATALENGTH(a.AttachedFile)',
    N'@n INT OUTPUT, @o BIGINT OUTPUT', @n = @aPurger OUTPUT, @o = @octets OUTPUT;

PRINT '--- Etat avant purge ---';
PRINT '  fichiers non migres        : ' + CAST(@restants AS VARCHAR);
PRINT '  echecs journalises         : ' + CAST(@echecs   AS VARCHAR);
PRINT '  ecarts de taille           : ' + CAST(@ecarts   AS VARCHAR);
PRINT '  binaires purgeables        : ' + CAST(@aPurger  AS VARCHAR)
    + ' (' + CAST(CAST(@octets / 1048576.0 AS DECIMAL(12,1)) AS VARCHAR) + ' Mo)';

IF @confirmation <> N'OUI'
BEGIN
    PRINT '';
    PRINT 'Purge NON executee (mode etat seul).';
    PRINT 'Pour l autoriser : editer ce fichier et mettre @confirmation = N''OUI''.';
    RETURN;
END

IF @restants > 0 OR @echecs > 0 OR @ecarts > 0
BEGIN
    RAISERROR('Purge REFUSEE : migration incomplete ou incoherente (voir les compteurs ci-dessus). Relancer la passe de migration, puis --verify.', 16, 1);
    RETURN;
END

IF @aPurger = 0
BEGIN
    PRINT 'Aucun binaire a purger — deja fait.';
    RETURN;
END

BEGIN TRANSACTION;

    EXEC sp_executesql N'
        UPDATE a
        SET a.AttachedFile = NULL
        FROM dbo.AdmAttachedFile a
        JOIN dbo.AppFileObject o ON o.AdmAttachedFileId = a.PKey
        WHERE a.AttachedFile IS NOT NULL
          AND o.DeletedAt IS NULL
          AND o.SizeBytes = DATALENGTH(a.AttachedFile);
        SELECT @n = @@ROWCOUNT;',
        N'@n INT OUTPUT', @n = @vides OUTPUT;

    PRINT '  binaires vides             : ' + CAST(@vides AS VARCHAR);

COMMIT TRANSACTION;

PRINT '';
PRINT 'Purge terminee.';
PRINT '';
PRINT 'ETAPE SUIVANTE OBLIGATOIRE — AttachedFile est une colonne FILESTREAM : son';
PRINT 'contenu ne vit pas dans le .mdf mais dans un groupe de fichiers a part. Mettre';
PRINT 'la colonne a NULL ne fait que MARQUER les fichiers ; tant que le ramasse-miettes';
PRINT 'n a pas tourne, les 8 Go restent sur le disque ET dans la sauvegarde.';
PRINT '';
PRINT 'A lancer jusqu a ce que num_marked_for_collection_items soit a 0 (2 passes en';
PRINT 'general, la 1re marque, la 2e collecte) :';
PRINT '';
PRINT '    CHECKPOINT;';
PRINT '    EXEC sp_filestream_force_garbage_collection @dbname = N''yesRemsProd'';';
PRINT '';
PRINT 'Controle : sys.database_files doit afficher 0 Mo pour le fichier FILESTREAM.';
PRINT 'En mode SIMPLE le ramasse-miettes suffit ; en FULL il faut d abord une';
PRINT 'sauvegarde du journal.';
PRINT '';
PRINT 'Puis, pour une restauration sur SQL Server pour Linux (FILESTREAM non supporte) :';
PRINT '    src/main/resources/db/migration/004_drop_attachedfile_filestream.sql';
GO

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
 * EXÉCUTION — la confirmation explicite est obligatoire ; sans elle sqlcmd s'arrête
 * sur une variable non définie, ce qui est le comportement voulu :
 *
 *     sqlcmd -S "<hote>" -U <user> -P <pwd> -d yesRemsProd -C -b \
 *            -v CONFIRMER=OUI -i migration/purge_attached_file.sql
 */

SET NOCOUNT ON;

DECLARE @confirmation NVARCHAR(20) = N'$(CONFIRMER)';
DECLARE @restants     INT;
DECLARE @echecs       INT;
DECLARE @ecarts       INT;
DECLARE @aPurger      INT;
DECLARE @octets       BIGINT;

-- 1. Tout fichier encore porteur d'un binaire doit avoir sa ligne AppFileObject.
SELECT @restants = COUNT(*)
FROM dbo.AdmAttachedFile a
WHERE a.AttachedFile IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM dbo.AppFileObject o WHERE o.AdmAttachedFileId = a.PKey);

-- 2. Aucun échec de migration non résolu.
SELECT @echecs = COUNT(*) FROM dbo.AppFileMigrationFailure;

-- 3. Aucun écart de taille entre la source et ce qui a été enregistré.
SELECT @ecarts = COUNT(*)
FROM dbo.AppFileObject o
JOIN dbo.AdmAttachedFile a ON a.PKey = o.AdmAttachedFileId
WHERE a.AttachedFile IS NOT NULL
  AND o.SizeBytes <> DATALENGTH(a.AttachedFile);

SELECT @aPurger = COUNT(*),
       @octets  = ISNULL(SUM(CAST(DATALENGTH(a.AttachedFile) AS BIGINT)), 0)
FROM dbo.AdmAttachedFile a
JOIN dbo.AppFileObject o ON o.AdmAttachedFileId = a.PKey
WHERE a.AttachedFile IS NOT NULL
  AND o.DeletedAt IS NULL
  AND o.SizeBytes = DATALENGTH(a.AttachedFile);

PRINT '--- Etat avant purge ---';
PRINT '  fichiers non migres        : ' + CAST(@restants AS VARCHAR);
PRINT '  echecs journalises         : ' + CAST(@echecs   AS VARCHAR);
PRINT '  ecarts de taille           : ' + CAST(@ecarts   AS VARCHAR);
PRINT '  binaires purgeables        : ' + CAST(@aPurger  AS VARCHAR)
    + ' (' + CAST(CAST(@octets / 1048576.0 AS DECIMAL(12,1)) AS VARCHAR) + ' Mo)';

IF @confirmation <> N'OUI'
BEGIN
    RAISERROR('Purge annulee : relancer avec -v CONFIRMER=OUI apres avoir lu l en-tete du script.', 16, 1);
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

    UPDATE a
    SET a.AttachedFile = NULL
    FROM dbo.AdmAttachedFile a
    JOIN dbo.AppFileObject o ON o.AdmAttachedFileId = a.PKey
    WHERE a.AttachedFile IS NOT NULL
      AND o.DeletedAt IS NULL
      AND o.SizeBytes = DATALENGTH(a.AttachedFile);

    PRINT '  binaires vides             : ' + CAST(@@ROWCOUNT AS VARCHAR);

COMMIT TRANSACTION;

SELECT 'Restant avec binaire' AS Controle, COUNT(*) AS Valeur FROM dbo.AdmAttachedFile WHERE AttachedFile IS NOT NULL
UNION ALL
SELECT 'Lignes AppFileObject', COUNT(*) FROM dbo.AppFileObject;

PRINT '';
PRINT 'Purge terminee.';
PRINT '';
PRINT 'Note sur la taille du .bak : BACKUP DATABASE ne copie que les extents alloues,';
PRINT 'donc la sauvegarde sera nettement plus legere sans avoir a reduire le fichier.';
PRINT 'Le .mdf, lui, garde sa taille sur disque. Pour la recuperer aussi (facultatif,';
PRINT 'a faire hors production, cela fragmente les index) :';
PRINT '';
PRINT '    DBCC SHRINKFILE (yesRemsProd, TRUNCATEONLY);';
GO

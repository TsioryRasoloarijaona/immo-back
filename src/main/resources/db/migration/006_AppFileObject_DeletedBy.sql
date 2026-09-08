/*
 * 006 — AppFileObject.DeletedBy
 *
 * Trace de l'auteur d'une suppression de piece jointe, symetrique de UploadedBy.
 *
 * CONTEXTE : DeletedAt existe depuis la migration 002 et toute la chaine de
 * lecture la respecte deja (la @Formula AdmAttachedFileSummary.hasFile la teste,
 * findByAdmAttachedFileIdAndDeletedAtIsNull l'exclut du telechargement), mais
 * AUCUN code ne l'ecrivait : 3 138 lignes en base reelle, zero supprimee. Les
 * endpoints DELETE de FEATURE-suppression-pieces-jointes.md l'alimentent enfin,
 * et « quand » sans « par qui » ne suffit pas pour un retrait de document.
 *
 * POURQUOI UNE COLONNE PLUTOT QUE AdmAttachedFile.UserUpdate : cette derniere est
 * legataire et peuplee a 3 410/3 410 par l'application Windows, avec la
 * semantique « dernier modificateur ». L'ecraser detruirait une donnee existante
 * et rendrait la trace ambigue. AppFileObject est notre table applicative.
 *
 * Nullable, sans valeur par defaut : les lignes anterieures n'ont jamais ete
 * supprimees (DeletedAt IS NULL partout), il n'y a donc rien a retro-remplir.
 *
 * A REJOUER SUR CHAQUE BASE : yesRemsProd (copie locale), yesRemsDev (VPS),
 * yesRemsCiCheck (suite de tests — la colonne est aussi ajoutee a
 * ci/schema/010_tables.sql, donc une base reconstruite depuis ce script l'a deja).
 *
 * Idempotent : ne fait rien si la colonne existe deja.
 */

IF NOT EXISTS (SELECT 1 FROM sys.columns
               WHERE object_id = OBJECT_ID('dbo.AppFileObject')
                 AND name = 'DeletedBy')
BEGIN
    ALTER TABLE dbo.AppFileObject
        ADD DeletedBy nvarchar(255) NULL;

    PRINT '006: colonne AppFileObject.DeletedBy ajoutee.';
END
ELSE
BEGIN
    PRINT '006: colonne AppFileObject.DeletedBy deja presente, rien a faire.';
END
GO

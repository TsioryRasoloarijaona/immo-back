-- ============================================================================
-- Rattrapage des alertes de relance ecrites par l'API sans ligne AdmAlertData.
--
-- ATTENTION — ce script modifie des DONNEES metier de deux tables legataires
-- (ComActionMessage, AdmAlertData). Il n'est pas joue automatiquement.
--
-- OU ET QUAND L'APPLIQUER
-- -----------------------
-- Sur la SOURCE DE VERITE (192.168.11.115, base YesRems) uniquement, APRES avoir
-- mis AdmAlertData sous change tracking :
--
--   ALTER TABLE dbo.AdmAlertData ENABLE CHANGE_TRACKING WITH (TRACK_COLUMNS_UPDATED = OFF);
--
-- Les copies (yesRemsDev des deux VPS) recoivent ensuite le resultat par le
-- cycle de synchronisation suivant — une seule fois, sans rien rejouer a la
-- main sur les copies. Voir instructions/FEATURE-alerte-admalertdata.md.
--
-- LE PROBLEME
-- -----------
-- POST /api/prospections/{id}/interactions ecrivait AlertValidityDate sur
-- l'interaction mais n'appliquait aucun des trois ordres du logiciel de bureau
-- (trace d'audit, 11 occurrences identiques, rejouee en direct le 26/09/2026) :
--
--   1. UPDATE ComActionMessage SET WithoutRevival = 1   -- les relances precedentes
--   2. UPDATE AdmAlertData SET Active = 0               -- leurs alertes
--   3. INSERT INTO AdmAlertData (..., Active = 1)       -- la nouvelle alerte
--
-- Or l'ecran d'OMRAN lit AdmAlertData (ComSaleProspectionView.LastActiveAlertDate
-- = MAX(AlertDate) WHERE Active = 1). Nos alertes n'y existaient donc pas, et
-- celles qu'elles auraient du eteindre restaient affichees.
--
-- MESURE AVANT CORRECTIF (source 115, 26/09/2026)
-- ------------------------------------------------
--   12 interactions a alerte sans ligne AdmAlertData, toutes de septembre 2026,
--   sur 9 prospections. 11 encore armees, 7 avec une action posterieure qui
--   aurait du les eteindre. Le code ne produit plus ces lignes ; ce script
--   repare celles deja ecrites.
--
-- LA REGLE APPLIQUEE (celle du legataire, rejouee retroactivement)
-- ----------------------------------------------------------------
-- Sur chaque prospection concernee, parmi les interactions a alerte non
-- rattachees a une echeance (ComDocumentId IS NULL) ni a un statut de dossier
-- (StrFolderStatusId IS NULL) :
--   - seule la PLUS RECENTE (DocDate) reste armee, et seulement si la
--     prospection est encore ouverte (IsClosed = 0 : une vente ou un archivage
--     desarme tout) ;
--   - toutes les autres passent WithoutRevival = 1 et Active = 0 ;
--   - les rappels d'impaye (IsUnpaid = 1) ne sont pas desarmes (predicat OMRAN).
-- Les lignes AdmAlertData manquantes sont creees au format constant releve sur
-- 39 599 lignes : Description 'Alerte', Groupping 'Prospection', Label NULL,
-- Code = Pkey en minuscules, RightKey = categorie « communication ».
--
-- Le script est idempotent : rejoue, il ne trouve plus rien a faire.
-- ============================================================================
SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRANSACTION;

-- Les prospections a rattraper : celles qui portent au moins une interaction a
-- alerte sans ligne AdmAlertData.
IF OBJECT_ID('tempdb..#cibles') IS NOT NULL DROP TABLE #cibles;
SELECT DISTINCT m.ComProspectionId
INTO #cibles
FROM dbo.ComActionMessage m
LEFT JOIN dbo.AdmAlertData a ON a.RelatedKey = m.PKey
WHERE m.AlertValidityDate IS NOT NULL
  AND m.ComProspectionId IS NOT NULL
  AND m.ComDocumentId IS NULL
  AND a.Pkey IS NULL;

-- Toutes les interactions a alerte de ces prospections, avec leur rang
-- (1 = la plus recente) et l'etat que la regle leur assigne.
IF OBJECT_ID('tempdb..#etat') IS NOT NULL DROP TABLE #etat;
SELECT m.PKey,
       m.ComProspectionId,
       m.AlertValidityDate,
       m.AlertMessage,
       ISNULL(m.IsUnpaid, 0) AS IsUnpaid,
       ROW_NUMBER() OVER (PARTITION BY m.ComProspectionId ORDER BY m.DocDate DESC, m.PKey) AS rang,
       CASE WHEN ROW_NUMBER() OVER (PARTITION BY m.ComProspectionId ORDER BY m.DocDate DESC, m.PKey) = 1
                 AND ISNULL(p.IsClosed, 0) = 0
            THEN 1 ELSE 0 END AS armee
INTO #etat
FROM dbo.ComActionMessage m
JOIN #cibles c ON c.ComProspectionId = m.ComProspectionId
JOIN dbo.ComProspection p ON p.PKey = m.ComProspectionId
WHERE m.AlertValidityDate IS NOT NULL
  AND m.ComDocumentId IS NULL
  AND m.StrFolderStatusId IS NULL;

DECLARE @prospections int;
SELECT @prospections = COUNT(*) FROM #cibles;
PRINT 'Prospections concernees : ' + CAST(@prospections AS varchar(10));

-- 1. Desarmement des interactions (WithoutRevival = 1), rappels d'impaye epargnes.
UPDATE m
SET m.WithoutRevival = 1
FROM dbo.ComActionMessage m
JOIN #etat e ON e.PKey = m.PKey
WHERE e.armee = 0
  AND e.IsUnpaid = 0
  AND ISNULL(m.WithoutRevival, 0) = 0;
PRINT 'ComActionMessage.WithoutRevival passes a 1 : ' + CAST(@@ROWCOUNT AS varchar(10));

-- 2. Extinction des alertes existantes qui auraient du l'etre (Active = 0).
UPDATE a
SET a.Active = 0
FROM dbo.AdmAlertData a
JOIN #etat e ON e.PKey = a.RelatedKey
WHERE e.armee = 0
  AND e.IsUnpaid = 0
  AND ISNULL(a.Active, 0) = 1;
PRINT 'AdmAlertData.Active passes a 0 : ' + CAST(@@ROWCOUNT AS varchar(10));

-- 3. Creation des lignes manquantes, actives ou non selon la regle.
INSERT INTO dbo.AdmAlertData (Pkey, Label, Code, Description, Message, Groupping, AlertDate, RelatedKey, RightKey, Active)
SELECT n.Pkey,
       NULL,
       LOWER(CONVERT(nvarchar(36), n.Pkey)),
       N'Alerte',
       LEFT(CAST(e.AlertMessage AS nvarchar(max)), 1000),
       N'Prospection',
       e.AlertValidityDate,
       e.PKey,
       N'69f8e7d6-1111-1111-1111-1aa8b1f02008',
       CASE WHEN e.armee = 1 OR (e.armee = 0 AND e.IsUnpaid = 1) THEN 1 ELSE 0 END
FROM #etat e
CROSS APPLY (SELECT NEWID() AS Pkey) n
WHERE NOT EXISTS (SELECT 1 FROM dbo.AdmAlertData a WHERE a.RelatedKey = e.PKey);
PRINT 'AdmAlertData creees : ' + CAST(@@ROWCOUNT AS varchar(10));

-- Controle : plus aucune interaction a alerte sans ligne, et au plus une alerte
-- active par prospection concernee.
IF EXISTS (SELECT 1 FROM #etat e WHERE NOT EXISTS (SELECT 1 FROM dbo.AdmAlertData a WHERE a.RelatedKey = e.PKey))
BEGIN
    RAISERROR('Il reste des interactions a alerte sans ligne AdmAlertData', 16, 1);
END
IF EXISTS (
    SELECT e.ComProspectionId
    FROM #etat e JOIN dbo.AdmAlertData a ON a.RelatedKey = e.PKey
    WHERE a.Active = 1 AND e.IsUnpaid = 0
    GROUP BY e.ComProspectionId HAVING COUNT(*) > 1)
BEGIN
    RAISERROR('Une prospection concernee garde plus d''une alerte active', 16, 1);
END

COMMIT TRANSACTION;

IF OBJECT_ID('tempdb..#etat') IS NOT NULL DROP TABLE #etat;
IF OBJECT_ID('tempdb..#cibles') IS NOT NULL DROP TABLE #cibles;

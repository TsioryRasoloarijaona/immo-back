-- ============================================================================
-- Extinction des relances encore actives sur des prospections FERMEES.
--
-- ATTENTION — ce script modifie des DONNEES metier de deux tables legataires
-- (ComActionMessage, AdmAlertData). Il n'est pas joue automatiquement.
--
-- OU L'APPLIQUER
-- --------------
-- Sur la SOURCE DE VERITE (192.168.11.115 = 197.230.172.136, base YesRems), avec
-- AdmAlertData sous change tracking (fait le 26/09/2026). Les copies des VPS
-- recoivent le resultat au cycle de synchronisation suivant.
--
-- LA REGLE (aide en ligne OMRAN, page PROSPECTIONS, bouton Archiver)
-- -------------------------------------------------------------------
--   « En cliquant sur bouton Archiver le systeme affecte une date d'archivage et
--     il desactive toutes les relances associees a cette prospection. »
-- et regle generale GEN0005 4.2 :
--   « Lorsqu'on passe d'ecran Prospection vers l'ecran Dossier de vente, toutes
--     les alertes qui ont ete creees pour la prospection seront desactivees. »
-- Une prospection fermee (IsClosed = 1) ne doit donc porter aucune relance armee
-- ni aucune alerte active. Le predicat est celui que le client envoie (trace
-- d'audit) :
--   UPDATE ComActionMessage SET WithoutRevival=1
--     WHERE AlertValidityDate IS NOT NULL AND ComDocumentId IS NULL
--       AND ComProspectionId = ... AND IsUnpaid=0
--   UPDATE AdmAlertData SET Active=0 WHERE RelatedKey IN (messages de la
--     prospection sans statut de dossier (StrFolderStatusId IS NULL)
--     et sans echeance (ComDocumentId IS NULL))
--
-- MESURE AVANT CORRECTIF (source 115, 27/09/2026)
-- ------------------------------------------------
--   583 alertes actives sur 583 prospections fermees (une chacune),
--   582 relances encore armees (WithoutRevival = 0) sur ces memes prospections,
--   fermetures de 2022 a 2026 plus 112 sans date de fermeture,
--   0 rappel d'impaye concerne, 574 alertes echues et 9 a une date future.
--   Total des alertes actives avant : 4 891 ; attendu apres : 4 308.
--
-- D'ou viennent-elles : 123 ont ete posees AVANT la fermeture et ont echappe au
-- desarmement (fermetures anciennes, 92 sans date ni motif). Les 460 autres ont
-- ete posees APRES l'archivage, par des commerciaux, 358 fois plus de 30 jours
-- apres : le legataire laisse ajouter une action a alerte sur une prospection
-- archivee (INJOIGNABLE, NON INTERESSE) sans la rouvrir, et n'eteint jamais cette
-- alerte puisque la fermeture est deja passee. Aucune n'a ete ecrite par l'API.
--
-- Une prospection fermee ne porte pas de relance : c'est la regle. Un suivi
-- reel se fait en la desarchivant (PATCH /desarchiver), pas en laissant une
-- alerte sur une fiche fermee. @garderAlertesFutures = 1 epargne toutefois les
-- alertes dont la date n'est pas encore passee (9 le 27/09), si l'on prefere
-- laisser les commerciaux les traiter d'abord.
--
-- Le script est idempotent : rejoue, il ne trouve rien a faire.
-- ============================================================================
SET NOCOUNT ON;
SET XACT_ABORT ON;

DECLARE @garderAlertesFutures bit = 0;   -- 1 : ne pas toucher aux alertes dont la date est a venir

BEGIN TRANSACTION;

DECLARE @avant int, @apres int;
SELECT @avant = COUNT(*) FROM dbo.AdmAlertData WHERE Active = 1;
PRINT 'Alertes actives avant : ' + CAST(@avant AS varchar(10));

-- 1. Relances des prospections fermees : WithoutRevival = 1 (rappels d'impaye epargnes).
UPDATE m
SET m.WithoutRevival = 1
FROM dbo.ComActionMessage m
JOIN dbo.ComProspection p ON p.PKey = m.ComProspectionId
WHERE p.IsClosed = 1
  AND m.AlertValidityDate IS NOT NULL
  AND m.ComDocumentId IS NULL
  AND ISNULL(m.IsUnpaid, 0) = 0
  AND ISNULL(m.WithoutRevival, 0) = 0
  AND (@garderAlertesFutures = 0 OR m.AlertValidityDate < CAST(GETDATE() AS date));
PRINT 'ComActionMessage.WithoutRevival passes a 1 : ' + CAST(@@ROWCOUNT AS varchar(10));

-- 2. Leurs alertes : Active = 0.
UPDATE a
SET a.Active = 0
FROM dbo.AdmAlertData a
JOIN dbo.ComActionMessage m ON m.PKey = a.RelatedKey
JOIN dbo.ComProspection p ON p.PKey = m.ComProspectionId
WHERE p.IsClosed = 1
  AND a.Active = 1
  AND m.ComDocumentId IS NULL
  AND m.StrFolderStatusId IS NULL
  AND ISNULL(m.IsUnpaid, 0) = 0
  AND (@garderAlertesFutures = 0 OR a.AlertDate < CAST(GETDATE() AS date));
PRINT 'AdmAlertData.Active passes a 0 : ' + CAST(@@ROWCOUNT AS varchar(10));

-- Controle : plus aucune alerte active (echue, si les futures sont gardees) sur une
-- prospection fermee.
IF EXISTS (
    SELECT 1 FROM dbo.AdmAlertData a
    JOIN dbo.ComActionMessage m ON m.PKey = a.RelatedKey
    JOIN dbo.ComProspection p ON p.PKey = m.ComProspectionId
    WHERE p.IsClosed = 1 AND a.Active = 1 AND m.ComDocumentId IS NULL
      AND m.StrFolderStatusId IS NULL AND ISNULL(m.IsUnpaid, 0) = 0
      AND (@garderAlertesFutures = 0 OR a.AlertDate < CAST(GETDATE() AS date)))
BEGIN
    RAISERROR('Il reste des alertes actives sur des prospections fermees', 16, 1);
END

SELECT @apres = COUNT(*) FROM dbo.AdmAlertData WHERE Active = 1;
PRINT 'Alertes actives apres : ' + CAST(@apres AS varchar(10));

COMMIT TRANSACTION;

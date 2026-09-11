-- ============================================================================
-- Rattrapage des interactions ecrites par l'API sans les drapeaux du legataire.
--
-- ATTENTION — ce script modifie des DONNEES metier d'une table legataire. Il
-- n'est pas joue automatiquement : appliquez-le a la main, sur chaque base.
--
-- LE PROBLEME
-- -----------
-- POST /api/prospections/{id}/interactions ne posait aucun des quatre drapeaux
-- que le logiciel de bureau renseigne, ni CfgTierId. Or la vue legataire
-- ComSaleProspectionView calcule :
--
--   LastCommunicationDate   = MAX(DocDate)  WHERE ... AND IsSystem = 0
--   LastCommunicationResult = TOP 1 ...     WHERE ... AND IsSystem = 0
--
-- En SQL, NULL ne satisfait pas `= 0`. Les interactions saisies par notre API
-- etaient donc ECARTEES du « dernier echange » — dans l'ecran d'Omran comme
-- dans notre propre liste, qui lit ces deux colonnes depuis la vue.
--
-- MESURE AVANT CORRECTIF (yesRems, 11/09/2026)
-- --------------------------------------------
--   IsSystem = 0      43 540   interactions humaines
--   IsSystem = 1       4 431   alertes generees par le legataire
--   IsSystem NULL         14   <- les notres, sur 8 prospections
--
-- Les 14 sont identifiables sans ambiguite : UserCreate NULL et note ecrite
-- dans Description au lieu d'AlertMessage (l'inversion corrigee par
-- FIX-interaction-note.md). L'une d'elles s'intitule meme « Test avec vrais
-- referentiels backend. ».
--
-- Le code ne produit plus ces lignes depuis ce lot ; ce script repare celles
-- deja ecrites.
--
-- CE QUE LE SCRIPT NE FAIT PAS
-- ----------------------------
-- Il ne touche NI Description NI AlertMessage. Reinverser la note a posteriori
-- demanderait de distinguer les lignes d'avant le correctif de celles d'apres,
-- et une erreur y serait une perte de texte saisi par un commercial. Ces 14
-- notes restent donc lisibles la ou elles sont.
-- ============================================================================

-- Controle AVANT : doit renvoyer 14 (ou 0 si deja applique).
SELECT COUNT(*) AS a_corriger
FROM dbo.ComActionMessage
WHERE IsSystem IS NULL;
GO

-- Les quatre drapeaux. Cible volontairement etroite — IsSystem IS NULL est la
-- signature exacte des lignes ecrites par l'API : aucune ligne du legataire
-- n'est dans ce cas, les 47 971 autres portent 0 ou 1.
UPDATE dbo.ComActionMessage
SET IsSystem       = 0,
    IsNotRead      = ISNULL(IsNotRead, 0),
    IsUnpaid       = ISNULL(IsUnpaid, 0),
    WithoutRevival = ISNULL(WithoutRevival, 0)
WHERE IsSystem IS NULL;
GO

-- Le tiers, deduit de la prospection portee par l'interaction. Renseigne sur
-- 96,5 % des interactions humaines du legataire ; on s'aligne, sans inventer
-- de valeur quand la prospection n'a pas de prospect.
UPDATE m
SET m.CfgTierId = p.CfgProspectId
FROM dbo.ComActionMessage m
INNER JOIN dbo.ComProspection p ON p.PKey = m.ComProspectionId
WHERE m.CfgTierId IS NULL
  AND m.ComProspectionId IS NOT NULL
  AND p.CfgProspectId IS NOT NULL
  AND m.UserCreate IS NULL;   -- signature des lignes API, pour ne pas toucher au legataire
GO

-- Controle APRES : doit renvoyer 0.
SELECT COUNT(*) AS restant
FROM dbo.ComActionMessage
WHERE IsSystem IS NULL;
GO

-- Verification metier : les prospections dont le dernier echange etait fausse
-- doivent maintenant en avoir un.
SELECT COUNT(DISTINCT m.ComProspectionId) AS prospections_rattrapees
FROM dbo.ComActionMessage m
WHERE m.UserCreate IS NULL AND m.ComProspectionId IS NOT NULL;
GO

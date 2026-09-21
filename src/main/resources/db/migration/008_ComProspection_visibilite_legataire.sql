-- ============================================================================
-- Rattrapage des prospections invisibles dans les ecrans du logiciel de bureau.
--
-- ATTENTION — ce script modifie des DONNEES metier d'une table legataire. Il
-- n'est pas joue automatiquement : appliquez-le a la main, base par base.
--
-- LE PROBLEME, ETABLI SUR LA REQUETE REELLE DU LOGICIEL
-- -----------------------------------------------------
-- Relevee dans sa trace d'audit pendant que l'utilisateur consultait l'ecran :
--
--   SELECT * FROM (SELECT * FROM ComSaleProspectionView) AS ComProspection
--    WHERE (1 = 1)
--      AND (StrEntityId = @P1)
--      AND (StkHierarchyId IN ('c273dec7-...', '5eee12f0-...'))
--      AND ((ComFolderId IS NULL) OR (... StatusIsCancellation=1 OR StatusIsClosing=1))
--      AND (IsClosed = 0)
--      AND (DocDate >= @P2) AND (DocDate < @P3)
--
-- Et la vue elle-meme se termine par :
--
--   WHERE ComProspection.StrEntityId <> '<pre-prospection>'
--
-- Une ligne creee par l'API echouait sur DEUX de ces conditions :
--
--   StrEntityId NULL  -> ecartee par le `<>` de la vue ET par le `=` de l'ecran
--   IsClosed    NULL  -> ecartee par `IsClosed = 0`
--
-- En SQL, NULL ne satisfait aucune comparaison. La ligne etait bien ecrite en
-- base et n'apparaissait dans aucun ecran.
--
-- MESURE (yesRems, 18/09/2026) — StrEntityId explique 100 % de l'appartenance
-- a la vue :
--
--   StrEntityId = <vente ou location>   14 036 lignes   dont 14 036 dans la vue
--   StrEntityId = <pre-prospection>      3 641 lignes   dont      0 dans la vue
--   StrEntityId IS NULL                      5 lignes   dont      0 dans la vue
--
-- Les 5 lignes a NULL sont les seules concernees par ce script : deux injectees
-- par n8n_api, les autres par notre API et un script de test. Les 3 641
-- pre-prospections sont exclues a dessein — c'est leur nature, pas un defaut.
--
-- CORRIGE A LA SOURCE
-- -------------------
-- ProspectionService.creerProspection pose desormais StrEntityId (« Prospection
-- vente ») et IsClosed = 0. Deux tests de CreationConformiteLegataireTest
-- l'empechent de revenir, dont un qui asserte la presence dans la vue plutot
-- que la valeur d'une colonne. Ce script ne concerne que l'existant.
-- ============================================================================

SET NOCOUNT ON;
GO

PRINT '--- Avant ---';
SELECT ISNULL(UserCreate, '(API / script)') AS origine, COUNT(*) AS lignes_invisibles
FROM dbo.ComProspection
WHERE StrEntityId IS NULL
GROUP BY UserCreate;
GO

-- ---------------------------------------------------------------------------
-- 1. Le type de document : « Prospection vente »
--
--    Resolu par code plutot qu'en dur. L'identifiant est le meme dans yesRems,
--    yesRemsProd et yesRemsTest — c'est du referentiel legataire restaure de la
--    meme source — mais le resoudre evite d'avoir a le verifier ailleurs.
--
--    SEULES les lignes a NULL sont touchees. Une pre-prospection porte
--    volontairement l'autre identifiant : ne pas la convertir en vente.
-- ---------------------------------------------------------------------------
UPDATE p
   SET p.StrEntityId = e.PKey
  FROM dbo.ComProspection p
 CROSS JOIN (SELECT TOP 1 PKey FROM dbo.StrEntity WHERE Code = 'ComProspection') e
 WHERE p.StrEntityId IS NULL;

PRINT '1. Type de document pose sur : ' + CAST(@@ROWCOUNT AS varchar(10)) + ' ligne(s)';
GO

-- ---------------------------------------------------------------------------
-- 2. Le drapeau de cloture, second filtre de l'ecran
-- ---------------------------------------------------------------------------
UPDATE dbo.ComProspection
   SET IsClosed      = 0,
       IsOldCustomer = ISNULL(IsOldCustomer, 0)
 WHERE IsClosed IS NULL
   AND UserCreate IS NULL;

PRINT '2. IsClosed pose sur : ' + CAST(@@ROWCOUNT AS varchar(10)) + ' ligne(s) de l''API';
GO

-- ---------------------------------------------------------------------------
-- 3. Verification : les lignes reparees sont-elles entrees dans la vue ?
-- ---------------------------------------------------------------------------
PRINT '';
PRINT '--- Apres ---';
SELECT COUNT(*) AS reste_sans_type FROM dbo.ComProspection WHERE StrEntityId IS NULL;

SELECT p.Code, p.IsClosed,
       CASE WHEN EXISTS (SELECT 1 FROM dbo.ComSaleProspectionView v WHERE v.PKey = p.PKey)
            THEN 'visible' ELSE 'TOUJOURS INVISIBLE' END AS etat
  FROM dbo.ComProspection p
 WHERE p.UserCreate IS NULL
 ORDER BY p.DocDate DESC;
GO

-- ---------------------------------------------------------------------------
-- HORS PERIMETRE, et volontairement
--
-- 936 prospections creees DANS le logiciel de bureau entre 2024 et 2026 ont
-- IsClosed a NULL (Hamza ATCHANY 591, Macire Kamara 171, Issam EL KANOUN 159).
-- Elles portent un StrEntityId valide, donc elles sont dans la vue, mais le
-- filtre `IsClosed = 0` de l'ecran les ecarte. Les corriger ferait apparaitre
-- 936 lignes chez le client : c'est une decision metier, pas une correction
-- technique, et ces lignes ne viennent pas de nous.
--
--   SELECT TOP 20 Code, UserCreate, UserCreateDateTime
--     FROM dbo.ComProspection
--    WHERE IsClosed IS NULL AND UserCreate IS NOT NULL
--    ORDER BY UserCreateDateTime DESC;
-- ---------------------------------------------------------------------------

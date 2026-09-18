-- ============================================================================
-- Rattrapage des prospections invisibles dans la vue de travail du legataire.
--
-- ATTENTION — ce script modifie des DONNEES metier d'une table legataire. Il
-- n'est pas joue automatiquement : appliquez-le a la main, base par base, et
-- lisez d'abord la section 2, qui a des consequences visibles pour le client.
--
-- LE PROBLEME
-- -----------
-- ComSaleProspectionView, la vue sur laquelle s'appuient les ecrans du logiciel
-- de bureau, ecarte systematiquement les lignes dont IsClosed est NULL. Mesure
-- sur yesRems le 18/09/2026, jointure sur PKey :
--
--   IsClosed = 0      7 418 lignes   dont 4 712 dans la vue
--   IsClosed = 1      9 325 lignes   dont 9 324 dans la vue
--   IsClosed IS NULL    938 lignes   dont     0 dans la vue   <---
--
-- Zero sur 938. En SQL, NULL ne satisfait aucune comparaison : une prospection
-- laissee a NULL est bien ecrite en base et n'apparait dans aucune liste.
-- Meme mecanisme que 007_ComActionMessage_drapeaux_api.sql.
--
-- DEUX POPULATIONS, ET ELLES N'APPELLENT PAS LA MEME DECISION
-- -----------------------------------------------------------
--   UserCreate NULL        2 lignes   <- creees par notre API
--   UserCreate renseigne 936 lignes   <- creees dans le LOGICIEL DE BUREAU,
--                                        par de vrais utilisateurs, de
--                                        juillet 2024 a juillet 2026
--                                        (Hamza ATCHANY 591, Macire Kamara 171,
--                                         Issam EL KANOUN 159, ...)
--
-- Le defaut de l'API est corrige a la source : ProspectionService pose
-- desormais IsClosed = 0 a la creation, et CreationConformiteLegataireTest
-- l'empeche de revenir. La section 1 ne concerne donc que l'existant.
--
-- La section 2 est d'une autre nature : ces 936 lignes ne viennent pas de nous.
-- Les corriger FERA APPARAITRE 936 prospections dans les ecrans du client, qui
-- ne les y voyaient pas. C'est probablement souhaitable — ce sont des donnees
-- reelles que leur propre logiciel leur cache — mais c'est une decision metier,
-- pas une correction technique. Elle est donc laissee en commentaire.
-- ============================================================================

SET NOCOUNT ON;
GO

-- ---------------------------------------------------------------------------
-- 0. Etat avant
-- ---------------------------------------------------------------------------
PRINT '--- Avant ---';
SELECT CASE WHEN UserCreate IS NULL THEN 'API (UserCreate NULL)' ELSE 'Logiciel de bureau' END AS origine,
       COUNT(*) AS lignes_a_null
FROM dbo.ComProspection
WHERE IsClosed IS NULL
GROUP BY CASE WHEN UserCreate IS NULL THEN 'API (UserCreate NULL)' ELSE 'Logiciel de bureau' END;
GO

-- ---------------------------------------------------------------------------
-- 1. Les lignes creees par l'API — sans effet de bord pour le client
--
--    Identification : UserCreate NULL. Le logiciel de bureau renseigne
--    toujours cette colonne ; notre API ne l'a jamais posee.
-- ---------------------------------------------------------------------------
UPDATE dbo.ComProspection
   SET IsClosed      = 0,
       IsOldCustomer = ISNULL(IsOldCustomer, 0)
 WHERE IsClosed IS NULL
   AND UserCreate IS NULL;

PRINT '1. Prospections de l''API rattrapees : ' + CAST(@@ROWCOUNT AS varchar(10));
GO

-- ---------------------------------------------------------------------------
-- 2. Les lignes creees dans le logiciel de bureau — DECISION METIER
--
--    Decommentez seulement apres accord : cela fera apparaitre 936
--    prospections dans les ecrans du client. Verifiez d'abord un echantillon
--    avec la requete ci-dessous, pour juger si ces lignes ont vocation a etre
--    visibles ou si leur NULL traduit un etat particulier du legataire.
-- ---------------------------------------------------------------------------

-- SELECT TOP 20 Code, UserCreate, UserCreateDateTime, CfgCommercialId
--   FROM dbo.ComProspection WHERE IsClosed IS NULL AND UserCreate IS NOT NULL
--  ORDER BY UserCreateDateTime DESC;

-- UPDATE dbo.ComProspection
--    SET IsClosed = 0
--  WHERE IsClosed IS NULL
--    AND UserCreate IS NOT NULL;

-- ---------------------------------------------------------------------------
-- 3. Verification
-- ---------------------------------------------------------------------------
PRINT '';
PRINT '--- Apres ---';
SELECT COUNT(*) AS reste_a_null FROM dbo.ComProspection WHERE IsClosed IS NULL;
GO

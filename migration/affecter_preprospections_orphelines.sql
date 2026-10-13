/*
 * AFFECTATION DES PRÉ-PROSPECTIONS ORPHELINES — voir instructions/FEATURE-repartition-notifications.md
 *
 * Une pré-prospection (StrEntity.Code = 'ComPreProspection') sans commercial (CfgCommercialId NULL)
 * mais avec au moins une action manuelle (ComActionMessage, IsSystem <> 1) saisie par un compte
 * Commercial / Commercial Aide / Commercial Chef rattaché à un employé reçoit cet employé (compte
 * actif seulement, sauf @inclureDesactives = 1).
 * Plusieurs commerciaux sur la même fiche : celui de l'action la plus récente (il suit le lead).
 * Les actions d'un Administrateur ou de la Direction Générale n'affectent rien.
 *
 * Base visée : la base d'ÉCRITURE (en local yesRems ; la copie yesRemsProd suit par la synchro).
 *
 * Exécuté tel quel, le script n'écrit rien : il affiche ce qu'il ferait. Pour écrire, éditer la
 * ligne @confirmation ci-dessous (N'OUI'), lancer, puis remettre N'NON'.
 *
 *     sqlcmd -S localhost,49687 -U <user> -P <pwd> -d yesRems -C -I -b
 *            -i "<chemin>\immo-crm\migration\affecter_preprospections_orphelines.sql"
 *
 * Rejouable : une fiche déjà affectée n'est plus orpheline, elle n'est plus touchée.
 */

SET NOCOUNT ON;

DECLARE @confirmation NVARCHAR(20) = N'NON';
-- 1 = affecter aussi aux comptes désactivés (commerciaux partis) ; 0 = laisser ces fiches à la répartition.
DECLARE @inclureDesactives BIT = 0;
DECLARE @auteur NVARCHAR(255) = N'Script affectation orphelines';

IF OBJECT_ID('tempdb..#affectation') IS NOT NULL DROP TABLE #affectation;

WITH actionsCommerciales AS (
    SELECT p.PKey                AS ComProspectionId,
           u.CfgEmployeeId       AS CfgCommercialId,
           ROW_NUMBER() OVER (PARTITION BY p.PKey
                              ORDER BY m.DocDate DESC, m.UserCreateDateTime DESC, m.PKey) AS rang
    FROM dbo.ComProspection p
    JOIN dbo.StrEntity e          ON e.PKey = p.StrEntityId AND e.Code = 'ComPreProspection'
    JOIN dbo.ComActionMessage m   ON m.ComProspectionId = p.PKey AND ISNULL(m.IsSystem, 0) = 0
    JOIN dbo.AdmUser u            ON u.PKey = m.AdmUserId
                                 AND u.CfgEmployeeId IS NOT NULL
                                 AND (@inclureDesactives = 1 OR ISNULL(u.Disabled, 0) = 0)
    WHERE p.CfgCommercialId IS NULL
      AND EXISTS (SELECT 1
                  FROM dbo.AdmUserRole ur
                  JOIN dbo.AdmRole r ON r.PKey = ur.AdmRoleId
                  WHERE ur.AdmUserId = u.PKey
                    AND r.Description IN (N'Commercial', N'Commercial Aide', N'Commercial Chef'))
)
SELECT ComProspectionId, CfgCommercialId
INTO #affectation
FROM actionsCommerciales
WHERE rang = 1;

DECLARE @orphelines INT = (
    SELECT COUNT(*)
    FROM dbo.ComProspection p
    JOIN dbo.StrEntity e ON e.PKey = p.StrEntityId AND e.Code = 'ComPreProspection'
    WHERE p.CfgCommercialId IS NULL);
DECLARE @aAffecter INT = (SELECT COUNT(*) FROM #affectation);

PRINT '--- Etat ---';
PRINT '  pre-prospections sans commercial : ' + CAST(@orphelines AS VARCHAR);
PRINT '  dont a affecter (action commerciale) : ' + CAST(@aAffecter AS VARCHAR);

SELECT p.Code                       AS Fiche,
       emp.Description              AS CommercialAffecte
FROM #affectation a
JOIN dbo.ComProspection p ON p.PKey = a.ComProspectionId
JOIN dbo.CfgTier emp      ON emp.PKey = a.CfgCommercialId
ORDER BY emp.Description, p.Code;

IF @confirmation <> N'OUI'
BEGIN
    PRINT '';
    PRINT 'Affectation NON executee (apercu seul).';
    PRINT 'Pour l ecrire : editer ce fichier et mettre @confirmation = N''OUI''.';
    RETURN;
END

BEGIN TRANSACTION;

    UPDATE p
    SET p.CfgCommercialId    = a.CfgCommercialId,
        p.UserUpdate         = @auteur,
        p.UserUpdateDateTime = GETDATE()
    FROM dbo.ComProspection p
    JOIN #affectation a ON a.ComProspectionId = p.PKey
    WHERE p.CfgCommercialId IS NULL;

    PRINT '  fiches affectees : ' + CAST(@@ROWCOUNT AS VARCHAR);

COMMIT TRANSACTION;

DROP TABLE #affectation;
GO

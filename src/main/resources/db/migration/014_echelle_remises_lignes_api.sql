-- Remises ecrites par l'API en pourcentage/TTC, remises dans la convention OMRAN (ratio 0-1 du HT ; HT). Voir FEATURE-echelle-remises.md.
-- ComFolder : APF 2026/00394, 00400, 00404 ; ARS 2026/00405, 00406, 00407, 00409, 00410, 00411.
-- StlQuote : 26/00317, 26/00322, 26/00315 du 10/09/2026 (code en double : cible par PKey). Idempotent.
SET NOCOUNT ON;
SET XACT_ABORT ON;

IF OBJECT_ID('tempdb..#dossiers') IS NOT NULL DROP TABLE #dossiers;
CREATE TABLE #dossiers (PKey uniqueidentifier PRIMARY KEY);
INSERT INTO #dossiers (PKey) VALUES
    ('F5AFB81A-F1BD-48CB-85CE-06679917EDCA'),
    ('85FF3B77-4A72-4F49-9DCE-33C616C439BB'),
    ('AF942A45-5B52-48DA-B2E6-F024B7563332'),
    ('E892B9BA-15DB-4520-AB0D-CB7D61DC6875'),
    ('756A1F5D-6E53-4B3E-9BB9-EA98339F673D'),
    ('AA8DF82B-A2C4-4206-A2A4-62636A4C52E6'),
    ('620C0A88-EDA3-4AE7-91E8-A4E489ACBADE'),
    ('F80312B8-DDE7-4388-9E0D-7A26F64B43BF'),
    ('78D8DB6B-FE4B-4596-AF3B-21F99B235821');

IF OBJECT_ID('tempdb..#offres') IS NOT NULL DROP TABLE #offres;
CREATE TABLE #offres (PKey uniqueidentifier PRIMARY KEY);
INSERT INTO #offres (PKey) VALUES
    ('DEA0F75A-6D3B-4E40-B4AC-D7F5A7C8B7F7'),
    ('D146ABDD-70AB-4FA9-8CEE-3EB49D7E182A'),
    ('428E8007-DABE-465B-8E0C-8DB0FF191970');

PRINT 'Avant :';
SELECT f.Code, f.Discount, f.DiscountAmount, f.PriceBeforeDiscountHT, f.PriceAfterDiscountHT
FROM dbo.ComFolder f JOIN #dossiers d ON d.PKey = f.PKey
ORDER BY f.Code ASC;
SELECT q.Code, q.DocDate, q.Discount, q.DiscountAmountHT, q.DiscountAmount, q.PriceBeforeDiscountHT
FROM dbo.StlQuote q JOIN #offres o ON o.PKey = q.PKey
ORDER BY q.DocDate ASC;

BEGIN TRANSACTION;

-- Dossier : tout se deduit des prix HT, inchanges ; rejoue, le script reecrit les memes valeurs.
UPDATE f
SET f.DiscountAmount = f.PriceBeforeDiscountHT - f.PriceAfterDiscountHT,
    f.Discount = CASE WHEN f.PriceBeforeDiscountHT <> 0
                      THEN ROUND((f.PriceBeforeDiscountHT - f.PriceAfterDiscountHT) / f.PriceBeforeDiscountHT, 6)
                      ELSE 0 END
FROM dbo.ComFolder f
JOIN #dossiers d ON d.PKey = f.PKey
WHERE f.PriceBeforeDiscountHT IS NOT NULL
  AND f.PriceAfterDiscountHT IS NOT NULL;
PRINT 'ComFolder mis a jour : ' + CAST(@@ROWCOUNT AS varchar(10));

-- Offre : montants HT/TTC deja dans la convention OMRAN, seul Discount est en pourcentage.
-- Sans base HT (26/00322, prix nuls), le pourcentage est divise par 100 une seule fois (garde >= 1).
UPDATE q
SET q.Discount = CASE WHEN q.PriceBeforeDiscountHT <> 0
                      THEN ROUND(q.DiscountAmountHT / q.PriceBeforeDiscountHT, 6)
                      ELSE q.Discount / 100 END
FROM dbo.StlQuote q
JOIN #offres o ON o.PKey = q.PKey
WHERE (q.PriceBeforeDiscountHT <> 0
       AND q.Discount <> ROUND(q.DiscountAmountHT / q.PriceBeforeDiscountHT, 6))
   OR (ISNULL(q.PriceBeforeDiscountHT, 0) = 0 AND ABS(q.Discount) >= 1);
PRINT 'StlQuote mis a jour : ' + CAST(@@ROWCOUNT AS varchar(10));

IF EXISTS (SELECT 1 FROM dbo.ComFolder f JOIN #dossiers d ON d.PKey = f.PKey WHERE ABS(f.Discount) >= 1)
   OR EXISTS (SELECT 1 FROM dbo.StlQuote q JOIN #offres o ON o.PKey = q.PKey WHERE ABS(q.Discount) >= 1)
BEGIN
    -- THROW et non RAISERROR : seul THROW respecte XACT_ABORT et annule la transaction.
    ROLLBACK TRANSACTION;
    THROW 50000, 'Une remise ciblee est encore hors de l''echelle 0-1 : rien n''a ete modifie', 1;
END

COMMIT TRANSACTION;

PRINT 'Apres :';
SELECT f.Code, f.Discount, f.DiscountAmount, f.PriceBeforeDiscountHT, f.PriceAfterDiscountHT
FROM dbo.ComFolder f JOIN #dossiers d ON d.PKey = f.PKey
ORDER BY f.Code ASC;
SELECT q.Code, q.DocDate, q.Discount, q.DiscountAmountHT, q.DiscountAmount, q.PriceBeforeDiscountHT
FROM dbo.StlQuote q JOIN #offres o ON o.PKey = q.PKey
ORDER BY q.DocDate ASC;

DROP TABLE #offres;
DROP TABLE #dossiers;

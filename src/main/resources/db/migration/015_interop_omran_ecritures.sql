-- Reprise des lignes ecrites par le CRM avant FEATURE-interop-omran-ecritures.md (section 6) : colonnes
-- qu'OMRAN remplit toujours et sans lesquelles ses vues (ComSaleFolderView, StlSettlementView,
-- ComImpayedView) ignorent nos dossiers, echeances et reglements.
-- NON joue automatiquement. A appliquer par l'utilisateur sur la SOURCE (115), apres essai sur une copie :
-- ComFolder, ComDocument, StlSettlement, StkItem, ComFolderItem, StrFolderStatusHistory et StrHistory
-- sont suivies (change tracking) sur la base source locale : les copies suivent par la synchronisation.
-- Verifier sur 115 : SELECT t.name FROM sys.change_tracking_tables c JOIN sys.tables t ON t.object_id = c.object_id
-- Idempotent : les cibles sont les lignes encore incompletes (StrEntityId / AdmModuleId NULL) ; rejoue, rien ne bouge.
-- Comptes attendus sur yesRemsProd au 12/10/2026 : 30 dossiers (11 sans statut, 9 sans montants, 1 sans
-- article), 67 echeances (2 sans categorie, non modifiees), 11 reglements, 30 lignes d'historique,
-- 5 sous-articles a rattacher.
SET NOCOUNT ON;
SET XACT_ABORT ON;

DECLARE @auteur nvarchar(255) = N'Arpent (reprise)';
DECLARE @eAigu nchar(1) = NCHAR(233);
DECLARE @eGrave nchar(1) = NCHAR(232);
DECLARE @nonDefinie nvarchar(255) = N'Non d' + NCHAR(233) + N'finie';
DECLARE @crlf nvarchar(2) = NCHAR(13) + NCHAR(10);

-- References lues par Code, jamais en dur.
DECLARE @entiteDossier uniqueidentifier = (SELECT TOP 1 PKey FROM dbo.StrEntity WHERE Code = N'ComFolder' ORDER BY PKey ASC);
DECLARE @entiteEncaissement uniqueidentifier = (SELECT TOP 1 PKey FROM dbo.StrEntity WHERE Code = N'StlSettlementIn' ORDER BY PKey ASC);
DECLARE @entiteDecaissement uniqueidentifier = (SELECT TOP 1 PKey FROM dbo.StrEntity WHERE Code = N'StlSettlementOut' ORDER BY PKey ASC);
DECLARE @moduleVente uniqueidentifier = (SELECT TOP 1 PKey FROM dbo.AdmModule WHERE Code = N'COM' ORDER BY PKey ASC);
DECLARE @devise uniqueidentifier = (SELECT CodeValue FROM dbo.AdmParamsGuid WHERE Code = N'DefaultCurrency');
IF @devise IS NULL
    SET @devise = (SELECT TOP 1 PKey FROM dbo.StlCurrency WHERE Code = N'MAD' ORDER BY PKey ASC);
DECLARE @reservation uniqueidentifier = (SELECT TOP 1 PKey FROM dbo.StrStatus
                                         WHERE StrConfigEntityId = @entiteDossier AND Code = N'RESERVATION' ORDER BY PKey ASC);
DECLARE @reservationLibelle nvarchar(255) = (SELECT Description FROM dbo.StrStatus WHERE PKey = @reservation);

IF @entiteDossier IS NULL OR @entiteEncaissement IS NULL OR @entiteDecaissement IS NULL
   OR @moduleVente IS NULL OR @devise IS NULL OR @reservation IS NULL
    THROW 50000, 'Referentiel incomplet (StrEntity, AdmModule, devise ou statut RESERVATION) : rien n''a ete modifie', 1;

-- Cibles, figees avant toute modification.
IF OBJECT_ID('tempdb..#dossiers') IS NOT NULL DROP TABLE #dossiers;
CREATE TABLE #dossiers (PKey uniqueidentifier PRIMARY KEY, SansStatut bit NOT NULL, SansMontants bit NOT NULL);
INSERT INTO #dossiers (PKey, SansStatut, SansMontants)
SELECT f.PKey,
       CASE WHEN f.StrStatusId IS NULL THEN 1 ELSE 0 END,
       CASE WHEN f.Price IS NULL AND f.StkItemId IS NOT NULL THEN 1 ELSE 0 END
FROM dbo.ComFolder f
WHERE f.StrEntityId IS NULL;

IF OBJECT_ID('tempdb..#echeances') IS NOT NULL DROP TABLE #echeances;
CREATE TABLE #echeances (PKey uniqueidentifier PRIMARY KEY);
INSERT INTO #echeances (PKey)
SELECT d.PKey
FROM dbo.ComDocument d
WHERE d.AdmModuleId IS NULL
  AND d.ComSettlementCategoryId IS NOT NULL
  AND (d.ComFolderId IN (SELECT PKey FROM #dossiers) OR d.UserCreate IS NULL OR d.UserCreate = N'Tsiory Admin');

IF OBJECT_ID('tempdb..#reglements') IS NOT NULL DROP TABLE #reglements;
CREATE TABLE #reglements (PKey uniqueidentifier PRIMARY KEY);
INSERT INTO #reglements (PKey)
SELECT s.PKey FROM dbo.StlSettlement s WHERE s.AdmModuleId IS NULL;

PRINT 'Avant : dossiers CRM a reprendre';
SELECT f.Code, st.Code AS Statut, f.Price, a.Code AS Article, d.SansStatut, d.SansMontants
FROM dbo.ComFolder f
JOIN #dossiers d ON d.PKey = f.PKey
LEFT JOIN dbo.StrStatus st ON st.PKey = f.StrStatusId
LEFT JOIN dbo.StkItem a ON a.PKey = f.StkItemId
ORDER BY f.Code ASC;

PRINT 'Avant : dossiers sans montants (recalcules au prix de reference de l''article)';
SELECT f.Code, a.Code AS Article, a.ComSalePrice, a.ComSalePriceFloor
FROM dbo.ComFolder f
JOIN #dossiers d ON d.PKey = f.PKey AND d.SansMontants = 1
JOIN dbo.StkItem a ON a.PKey = f.StkItemId
ORDER BY f.Code ASC;

PRINT 'Signale, non modifie : dossier sans article (montants intacts, echeances sans societe)';
SELECT f.Code FROM dbo.ComFolder f JOIN #dossiers d ON d.PKey = f.PKey WHERE f.StkItemId IS NULL ORDER BY f.Code ASC;

PRINT 'Signale, non modifie : echeances sans categorie (decision humaine)';
SELECT f.Code AS Dossier, d.PKey, d.Amount, d.Description
FROM dbo.ComDocument d
LEFT JOIN dbo.ComFolder f ON f.PKey = d.ComFolderId
WHERE d.AdmModuleId IS NULL AND d.ComSettlementCategoryId IS NULL
ORDER BY f.Code ASC;

PRINT 'Signale, non modifie : sous-articles deja occupes par un autre dossier';
SELECT f.Code AS Dossier, s.Code AS SousArticle, o.Code AS DossierOccupant
FROM dbo.StkItem s
JOIN dbo.ComFolder f ON f.StkItemId = s.ParentId
JOIN #dossiers d ON d.PKey = f.PKey
JOIN dbo.ComFolder o ON o.PKey = s.ComFolderId
WHERE s.ComFolderId <> f.PKey
ORDER BY f.Code ASC;

PRINT 'Avant : visibles dans les vues OMRAN';
SELECT (SELECT COUNT(*) FROM dbo.ComSaleFolderView v JOIN #dossiers d ON d.PKey = v.PKey) AS DossiersDansComSaleFolderView,
       (SELECT COUNT(*) FROM dbo.StlSettlementView v JOIN #reglements r ON r.PKey = v.PKey) AS ReglementsDansStlSettlementView,
       (SELECT COUNT(*) FROM dbo.ComImpayedView v JOIN #echeances e ON e.PKey = v.ComDocumentId) AS EcheancesDansComImpayedView;

BEGIN TRANSACTION;

-- 3. Sous-articles libres du lot principal rattaches au dossier, puis une ligne ComFolderItem par sous-article.
UPDATE s
SET s.ComFolderId = f.PKey
FROM dbo.StkItem s
JOIN dbo.ComFolder f ON f.StkItemId = s.ParentId
JOIN #dossiers d ON d.PKey = f.PKey
WHERE s.ComFolderId IS NULL AND s.PKey <> f.StkItemId;
PRINT 'StkItem rattaches : ' + CAST(@@ROWCOUNT AS varchar(10));

INSERT INTO dbo.ComFolderItem (PKey, ComFolderId, StkItemId, StkItemTypeId, StkItemCategoryId, ParentId, Code,
        Description, Address, Notes, Contenance, AreaFloor, Area, VATRatio, Discount, PriceBeforeDiscountHT, PriceHT,
        PriceBeforeDiscount, Price, AlwaysAffected, IsUnderItem, IsUnderItemOrigin, IsTitled,
        UserCreate, UserCreateDateTime, UserUpdate, UserUpdateDateTime)
SELECT NEWID(), f.PKey, s.PKey, s.StkItemTypeId, s.StkItemCategoryId, s.ParentId, ISNULL(s.Code, N''),
       ISNULL(s.Description, N''), ISNULL(s.Address, N''), ISNULL(s.Notes, N''), ISNULL(s.Contenance, N''),
       ISNULL(s.AreaFloor, 0), ISNULL(s.Area, 0), ISNULL(s.Vat, 0), 0, p.HT, p.HT, p.TTC, p.TTC,
       ISNULL(s.AlwaysAffected, 0), 1, ISNULL(s.IsUnderItemOrigin, 0), ISNULL(s.IsTitled, 0),
       @auteur, GETDATE(), @auteur, GETDATE()
FROM dbo.StkItem s
JOIN dbo.ComFolder f ON f.PKey = s.ComFolderId AND s.PKey <> f.StkItemId
JOIN #dossiers d ON d.PKey = f.PKey
CROSS APPLY (
    SELECT CASE WHEN s.ComSalePrice > 0 THEN s.ComSalePrice
                WHEN s.ComSalePriceFloor > 0 THEN s.ComSalePriceFloor ELSE 0 END AS TTC,
           CASE WHEN s.ComSalePrice > 0
                     THEN ISNULL(s.ComSalePriceHT, ROUND(s.ComSalePrice / (1 + ISNULL(s.Vat, 0)), 6))
                WHEN s.ComSalePriceFloor > 0
                     THEN ISNULL(s.ComSalePriceFloorHT, ROUND(s.ComSalePriceFloor / (1 + ISNULL(s.Vat, 0)), 6))
                ELSE 0 END AS HT
) p
WHERE NOT EXISTS (SELECT 1 FROM dbo.ComFolderItem i WHERE i.ComFolderId = f.PKey AND i.StkItemId = s.PKey);
PRINT 'ComFolderItem crees : ' + CAST(@@ROWCOUNT AS varchar(10));

-- 2. Dossiers sans statut : RESERVATION, avec ses lignes d'historique.
UPDATE f
SET f.StrStatusId = @reservation
FROM dbo.ComFolder f
JOIN #dossiers d ON d.PKey = f.PKey AND d.SansStatut = 1
WHERE f.StrStatusId IS NULL;
PRINT 'Statut RESERVATION pose : ' + CAST(@@ROWCOUNT AS varchar(10));

INSERT INTO dbo.StrFolderStatusHistory (PKey, DocDate, ComFolderId, StrFolderStatusId, CfgTierId, StkItemId, AdmUserId, Description)
SELECT NEWID(), GETDATE(), f.PKey, @reservation, f.CfgCustomerId, f.StkItemId, NULL, N''
FROM dbo.ComFolder f
JOIN #dossiers d ON d.PKey = f.PKey AND d.SansStatut = 1
WHERE NOT EXISTS (SELECT 1 FROM dbo.StrFolderStatusHistory h WHERE h.ComFolderId = f.PKey);
PRINT 'StrFolderStatusHistory crees : ' + CAST(@@ROWCOUNT AS varchar(10));

INSERT INTO dbo.StrHistory (PKey, DocDate, UserDate, ObjectId, StrConfigEntityId, AdmUserDescription, StrStatusId,
        StrStatusDescription, Description)
SELECT NEWID(), GETDATE(), GETDATE(), f.PKey, @entiteDossier, @auteur, @reservation, @reservationLibelle,
       N'Cr' + @eAigu + N'ation (reprise)'
FROM dbo.ComFolder f
JOIN #dossiers d ON d.PKey = f.PKey AND d.SansStatut = 1
WHERE NOT EXISTS (SELECT 1 FROM dbo.StrHistory h WHERE h.ObjectId = f.PKey AND h.StrStatusId IS NOT NULL);
PRINT 'StrHistory crees : ' + CAST(@@ROWCOUNT AS varchar(10));

-- 4. Historique ecrit par le CRM (sans auteur ni article) : client et article du dossier.
UPDATE h
SET h.CfgTierId = ISNULL(h.CfgTierId, f.CfgCustomerId),
    h.StkItemId = f.StkItemId,
    h.Description = ISNULL(h.Description, N'')
FROM dbo.StrFolderStatusHistory h
JOIN dbo.ComFolder f ON f.PKey = h.ComFolderId
WHERE h.AdmUserId IS NULL AND h.StkItemId IS NULL;
PRINT 'StrFolderStatusHistory completees : ' + CAST(@@ROWCOUNT AS varchar(10));

-- 5. Echeances : societe du projet (article -> tranche -> projet), module, TVA de la categorie, HT/TTC.
UPDATE d
SET d.CfgCompanyId = ISNULL(d.CfgCompanyId, pr.CfgCompanyId),
    d.AdmModuleId = @moduleVente,
    d.VATRatio = ISNULL(c.VatRatio, 0.2),
    d.AmountTTC = d.Amount,
    d.AmountHT = ROUND(CAST(d.Amount AS decimal(38, 12)) / (1 + ISNULL(c.VatRatio, 0.2)), 6),
    d.DiscountRatio = ISNULL(d.DiscountRatio, 0),
    d.ExcludeFromPrice = ISNULL(d.ExcludeFromPrice, ISNULL(c.ExcludeFromPrice, 0)),
    d.DocDate = CAST(CAST(d.DocDate AS date) AS datetime),
    d.Code = ISNULL(d.Code, N''),
    d.Notes = ISNULL(d.Notes, N''),
    d.QuittanceCode = ISNULL(d.QuittanceCode, N''),
    d.UserCreate = ISNULL(d.UserCreate, @auteur),
    d.UserCreateDateTime = ISNULL(d.UserCreateDateTime, d.DocDate),
    d.UserUpdate = ISNULL(d.UserUpdate, ISNULL(d.UserCreate, @auteur)),
    d.UserUpdateDateTime = ISNULL(d.UserUpdateDateTime, ISNULL(d.UserCreateDateTime, d.DocDate))
FROM dbo.ComDocument d
JOIN #echeances e ON e.PKey = d.PKey
JOIN dbo.ComSettlementCategory c ON c.PKey = d.ComSettlementCategoryId
LEFT JOIN dbo.ComFolder f ON f.PKey = d.ComFolderId
LEFT JOIN dbo.StkItem a ON a.PKey = f.StkItemId
LEFT JOIN dbo.CfgTranche t ON t.PKey = a.StkHierarchyId
LEFT JOIN dbo.CfgProject pr ON pr.PKey = t.CfgProjectId;
PRINT 'ComDocument repris : ' + CAST(@@ROWCOUNT AS varchar(10));

-- 1. Dossiers : entite, constantes d'insertion OMRAN, libelle, date a minuit, surfaces, auteurs.
UPDATE f
SET f.StrEntityId = @entiteDossier,
    f.PartPercent = ISNULL(f.PartPercent, 1),
    f.Description = f.Code + N' du ' + CONVERT(nvarchar(10), CAST(f.DocDate AS date), 103),
    f.DocDate = CAST(CAST(f.DocDate AS date) AS datetime),
    f.Area = CASE WHEN a.PKey IS NULL THEN f.Area ELSE ISNULL(a.Area, 0) END,
    f.GlobalArea = CASE WHEN a.PKey IS NULL THEN f.GlobalArea ELSE ISNULL(a.Area, 0) + ISNULL(sous.Surface, 0) END,
    f.UnitCostHT = CASE WHEN a.PKey IS NULL THEN f.UnitCostHT ELSE ISNULL(a.ComSaleUnitCostHT, 0) END,
    f.UnitCost = CASE WHEN a.PKey IS NULL THEN f.UnitCost
                      ELSE ROUND(ISNULL(a.ComSaleUnitCostHT, 0) * (1 + ISNULL(a.Vat, 0)), 6) END,
    f.DeliveryRatio = 0.001, f.DeliveryMaxNumber = 180, f.ProbabilityOfFinalisation = 1,
    f.ModificationsAmount = 0, f.ModificationsAmountHT = 0, f.DeliveryNumber = 0, f.DeliveryAmount = 0,
    f.RomInterest = 0, f.CommissionRate = ISNULL(f.CommissionRate, 0),
    f.IsAccounted = 0, f.IsPenaltyDisabled = 0, f.PriceWithModification = 0, f.IsConfirmedAgreementDate = 0,
    f.IsWithGiftChoiceLater = 0, f.IsOldCustomer = ISNULL(f.IsOldCustomer, 0),
    f.Notes = ISNULL(f.Notes, N''), f.CancelDescription = ISNULL(f.CancelDescription, N''),
    f.PenalityDescription = ISNULL(f.PenalityDescription, N''), f.CreditAgency = ISNULL(f.CreditAgency, N''),
    f.FinalTitleNumber = ISNULL(f.FinalTitleNumber, N''),
    f.UserCreate = ISNULL(f.UserCreate, @auteur),
    f.UserCreateDateTime = ISNULL(f.UserCreateDateTime, f.DocDate),
    f.UserUpdate = ISNULL(f.UserUpdate, ISNULL(f.UserCreate, @auteur)),
    f.UserUpdateDateTime = ISNULL(f.UserUpdateDateTime, ISNULL(f.UserCreateDateTime, f.DocDate))
FROM dbo.ComFolder f
JOIN #dossiers d ON d.PKey = f.PKey
LEFT JOIN dbo.StkItem a ON a.PKey = f.StkItemId
OUTER APPLY (SELECT SUM(ISNULL(s.Area, 0)) AS Surface
             FROM dbo.StkItem s WHERE s.ComFolderId = f.PKey AND s.PKey <> f.StkItemId) sous;
PRINT 'ComFolder repris : ' + CAST(@@ROWCOUNT AS varchar(10));

-- 1 bis. Dossiers sans montants : meme calcul que DossierService.appliquerMontants sans remise,
-- au prix de reference cumule de l'article principal et de ses annexes (remise nulle).
IF OBJECT_ID('tempdb..#prix') IS NOT NULL DROP TABLE #prix;
SELECT f.PKey AS DossierId,
       ISNULL(a.Vat, 0) AS Tva,
       r.TTC AS ItemTTC, r.HT AS ItemHT,
       ISNULL(ann.TTC, 0) AS AnnexesTTC, ISNULL(ann.HT, 0) AS AnnexesHT,
       CASE WHEN t.SyndicAmount IS NOT NULL AND t.SyndicPeriod > 0
            THEN ROUND(t.SyndicAmount * t.SyndicPeriod, 2) ELSE 0 END AS Syndic
INTO #prix
FROM dbo.ComFolder f
JOIN #dossiers d ON d.PKey = f.PKey AND d.SansMontants = 1
JOIN dbo.StkItem a ON a.PKey = f.StkItemId
LEFT JOIN dbo.CfgTranche t ON t.PKey = a.StkHierarchyId
CROSS APPLY (
    SELECT CASE WHEN a.ComSalePrice > 0 THEN a.ComSalePrice
                WHEN a.ComSalePriceFloor > 0 THEN a.ComSalePriceFloor ELSE 0 END AS TTC,
           CASE WHEN a.ComSalePrice > 0
                     THEN ISNULL(a.ComSalePriceHT, ROUND(a.ComSalePrice / (1 + ISNULL(a.Vat, 0)), 6))
                WHEN a.ComSalePriceFloor > 0
                     THEN ISNULL(a.ComSalePriceFloorHT, ROUND(a.ComSalePriceFloor / (1 + ISNULL(a.Vat, 0)), 6))
                ELSE 0 END AS HT
) r
OUTER APPLY (
    SELECT SUM(CASE WHEN x.ComSalePrice > 0 THEN x.ComSalePrice
                    WHEN x.ComSalePriceFloor > 0 THEN x.ComSalePriceFloor ELSE 0 END) AS TTC,
           SUM(CASE WHEN x.ComSalePrice > 0
                         THEN ISNULL(x.ComSalePriceHT, ROUND(x.ComSalePrice / (1 + ISNULL(a.Vat, 0)), 6))
                    WHEN x.ComSalePriceFloor > 0
                         THEN ISNULL(x.ComSalePriceFloorHT, ROUND(x.ComSalePriceFloor / (1 + ISNULL(a.Vat, 0)), 6))
                    ELSE 0 END) AS HT
    FROM dbo.StkItem x WHERE x.ComFolderId = f.PKey AND x.PKey <> f.StkItemId
) ann;

UPDATE f
SET f.VATRatio = p.Tva,
    f.ItemPrice = p.ItemTTC, f.ItemPriceHT = p.ItemHT,
    f.UnderItemsPrice = p.AnnexesTTC, f.UnderItemsPriceHT = p.AnnexesHT,
    f.PriceBeforeDiscount = m.AvantTTC, f.PriceBeforeDiscountHT = m.AvantHT,
    f.PriceAfterDiscount = m.AvantTTC, f.PriceAfterDiscountHT = m.ApresHT,
    f.DiscountAmount = m.AvantHT - m.ApresHT,
    f.Discount = CASE WHEN m.AvantHT <> 0 THEN ROUND((m.AvantHT - m.ApresHT) / m.AvantHT, 6) ELSE 0 END,
    f.ItemPriceAfter = p.ItemTTC,
    f.ItemPriceAfterHT = ROUND(p.ItemHT - (m.AvantHT - m.ApresHT) * m.Part, 6),
    f.UnderItemsPriceAfter = p.AnnexesTTC,
    f.UnderItemsPriceAfterHT = ROUND(p.AnnexesHT - (m.AvantHT - m.ApresHT) * (1 - m.Part), 6),
    f.Price = m.AvantTTC, f.PriceHT = m.ApresHT,
    f.TotalToPay = m.AvantTTC + p.Syndic
FROM dbo.ComFolder f
JOIN #prix p ON p.DossierId = f.PKey
CROSS APPLY (
    SELECT p.ItemTTC + p.AnnexesTTC AS AvantTTC,
           p.ItemHT + p.AnnexesHT AS AvantHT,
           CASE WHEN p.Tva = 0 THEN p.ItemTTC + p.AnnexesTTC
                ELSE ROUND((p.ItemTTC + p.AnnexesTTC) / (1 + p.Tva), 6) END AS ApresHT,
           CASE WHEN p.ItemHT + p.AnnexesHT <> 0 THEN ROUND(p.ItemHT / (p.ItemHT + p.AnnexesHT), 10) ELSE 1 END AS Part
) m
WHERE f.Price IS NULL;
PRINT 'ComFolder sans montants recalcules : ' + CAST(@@ROWCOUNT AS varchar(10));

-- 1 ter. Prix contractuel, montant du contrat (dossiers avec article) et detail du schema (tous).
UPDATE f
SET f.AgreementPrice = ISNULL(ag.Contractuel, 0),
    f.AgreementAmount = ROUND(ISNULL(f.Price, 0) - ISNULL(ag.Avance, 0), 2)
FROM dbo.ComFolder f
JOIN #dossiers d ON d.PKey = f.PKey
OUTER APPLY (
    SELECT SUM(CASE WHEN c.IsCredit = 1 AND ISNULL(x.ExcludeFromPrice, 0) = 0 THEN x.Amount ELSE 0 END) AS Contractuel,
           SUM(CASE WHEN c.IsSaleAdvance = 1 AND c.IsCredit = 1 THEN x.Amount
                    WHEN c.IsSaleAdvance = 1 THEN -x.Amount ELSE 0 END) AS Avance
    FROM dbo.ComDocument x
    JOIN dbo.ComSettlementCategory c ON c.PKey = x.ComSettlementCategoryId
    WHERE x.ComFolderId = f.PKey
) ag
WHERE f.StkItemId IS NOT NULL;
PRINT 'Prix contractuel / montant contrat : ' + CAST(@@ROWCOUNT AS varchar(10));

UPDATE f
SET f.ComDocumentDetail = CAST(N'Sch' + @eGrave + N'ma de financement :' + @crlf + ISNULL(det.Lignes, N'') AS nvarchar(max))
FROM dbo.ComFolder f
JOIN #dossiers d ON d.PKey = f.PKey
OUTER APPLY (
    SELECT STRING_AGG(CAST(
               N'Date :' + CONVERT(nvarchar(10), x.DocDate, 103)
               + N'   Montant :' + RIGHT(REPLICATE(N' ', 20) + REPLACE(FORMAT(x.Amount, 'N2', 'en-US'), N',', N' ') + N' DH', 20)
               + N'   R' + @eAigu + N'gl' + @eAigu + N' :' + RIGHT(REPLICATE(N' ', 20) + REPLACE(FORMAT(ISNULL(rg.Regle, 0), 'N2', 'en-US'), N',', N' ') + N' DH', 20)
               + N'   Reste :' + RIGHT(REPLICATE(N' ', 20) + REPLACE(FORMAT(x.Amount - ISNULL(rg.Regle, 0), 'N2', 'en-US'), N',', N' ') + N' DH', 20)
               + N'   Exclure du prix contractuel:' + CASE WHEN x.ExcludeFromPrice = 1 THEN N'Oui' ELSE N'Non' END
               + N'   Cat' + @eAigu + N'gorie : ' + ISNULL(c.Description, N'') + @crlf AS nvarchar(max)), N'')
           WITHIN GROUP (ORDER BY x.DocDate ASC, x.PKey ASC) AS Lignes
    FROM dbo.ComDocument x
    LEFT JOIN dbo.ComSettlementCategory c ON c.PKey = x.ComSettlementCategoryId
    OUTER APPLY (SELECT SUM(l.Amount) AS Regle
                 FROM dbo.StlSettlementLine l
                 JOIN dbo.StlSettlement s ON s.PKey = l.StlSettlementId
                 WHERE l.ComDocumentId = x.PKey AND (s.Status IS NULL OR s.Status <> N'CANCELED')) rg
    WHERE x.ComFolderId = f.PKey
) det;
PRINT 'ComDocumentDetail : ' + CAST(@@ROWCOUNT AS varchar(10));

-- 6. Reglements : TierId = client du dossier, payeur saisi dans TierPayerId ; references, zeros, libelle OMRAN.
UPDATE s
SET s.AdmModuleId = @moduleVente,
    s.StrEntityId = CASE WHEN ISNULL(s.IsCredit, 1) = 1 THEN @entiteEncaissement ELSE @entiteDecaissement END,
    s.StlCurrencyId = ISNULL(s.StlCurrencyId, @devise),
    s.StlCurrencyAffectationId = ISNULL(s.StlCurrencyAffectationId, @devise),
    s.ConvertValue = ISNULL(s.ConvertValue, 1), s.ConvertUnit = ISNULL(s.ConvertUnit, 1),
    s.AmountBase = ISNULL(s.AmountBase, s.Amount), s.AmountBaseConverted = ISNULL(s.AmountBaseConverted, s.Amount),
    s.AmountRS = ISNULL(s.AmountRS, 0), s.RsRatio = ISNULL(s.RsRatio, 0), s.AmountStamp = ISNULL(s.AmountStamp, 0),
    s.RSAmountSettled = ISNULL(s.RSAmountSettled, 0), s.IsAccounted = ISNULL(s.IsAccounted, 0),
    s.IsDcoumentDeliveredToCenter = ISNULL(s.IsDcoumentDeliveredToCenter, 0),
    s.Category = ISNULL(s.Category, N'None'),
    s.ItemNumber = ISNULL(s.ItemNumber, @nonDefinie), s.ItemDescription = ISNULL(s.ItemDescription, @nonDefinie),
    s.Description = ISNULL(ty.Description, N'') + ISNULL(N' ' + bq.Code, N'')
                    + CASE WHEN ISNULL(s.Code, N'') <> N'' THEN N' N' + NCHAR(176) + N' ' + s.Code ELSE N'' END
                    + N' du ' + CONVERT(nvarchar(10), CAST(s.DocDate AS date), 103),
    s.CfgCompanyId = ISNULL(s.CfgCompanyId, dos.CfgCompanyId),
    s.TierPayerId = CASE WHEN s.TierId IS NULL THEN dos.ClientId
                         WHEN dos.ClientId IS NOT NULL AND s.TierId <> dos.ClientId THEN s.TierId
                         ELSE ISNULL(s.TierPayerId, s.TierId) END,
    s.TierId = ISNULL(dos.ClientId, s.TierId),
    s.IsCredit = ISNULL(s.IsCredit, 1),
    s.Status = ISNULL(s.Status, N'IN_WORK'),
    s.EcheanceDate = ISNULL(s.EcheanceDate, CAST(CAST(s.DocDate AS date) AS datetime)),
    s.DocDate = CAST(CAST(s.DocDate AS date) AS datetime),
    s.Notes = ISNULL(s.Notes, N''),
    s.UserCreate = ISNULL(s.UserCreate, @auteur),
    s.UserCreateDateTime = ISNULL(s.UserCreateDateTime, s.DocDate),
    s.UserUpdate = ISNULL(s.UserUpdate, ISNULL(s.UserCreate, @auteur)),
    s.UserUpdateDateTime = ISNULL(s.UserUpdateDateTime, ISNULL(s.UserCreateDateTime, s.DocDate))
FROM dbo.StlSettlement s
JOIN #reglements r ON r.PKey = s.PKey
LEFT JOIN dbo.StlSettlementType ty ON ty.PKey = s.StlSettlementTypeId
LEFT JOIN dbo.CfgTier bq ON bq.PKey = s.CfgBankId
OUTER APPLY (
    SELECT TOP 1 f.CfgCustomerId AS ClientId, pr.CfgCompanyId
    FROM dbo.StlSettlementLine l
    JOIN dbo.ComDocument x ON x.PKey = l.ComDocumentId
    JOIN dbo.ComFolder f ON f.PKey = x.ComFolderId
    LEFT JOIN dbo.StkItem a ON a.PKey = f.StkItemId
    LEFT JOIN dbo.CfgTranche t ON t.PKey = a.StkHierarchyId
    LEFT JOIN dbo.CfgProject pr ON pr.PKey = t.CfgProjectId
    WHERE l.StlSettlementId = s.PKey
    ORDER BY l.PKey ASC
) dos;
PRINT 'StlSettlement repris : ' + CAST(@@ROWCOUNT AS varchar(10));

-- 7. Controles : aucun dossier CRM ne reste sans entite ni sans statut.
IF EXISTS (SELECT 1 FROM dbo.ComFolder f JOIN #dossiers d ON d.PKey = f.PKey
           WHERE f.StrEntityId IS NULL OR f.StrStatusId IS NULL)
BEGIN
    -- THROW et non RAISERROR : seul THROW respecte XACT_ABORT et annule la transaction.
    ROLLBACK TRANSACTION;
    THROW 50001, 'Un dossier CRM reste sans StrEntityId ou sans StrStatusId : rien n''a ete modifie', 1;
END

COMMIT TRANSACTION;

PRINT 'Apres : visibles dans les vues OMRAN (attendu 30 dossiers, 11 reglements ; echeances a reste > 0 d''un dossier actif)';
SELECT (SELECT COUNT(*) FROM dbo.ComSaleFolderView v JOIN #dossiers d ON d.PKey = v.PKey) AS DossiersDansComSaleFolderView,
       (SELECT COUNT(*) FROM dbo.StlSettlementView v JOIN #reglements r ON r.PKey = v.PKey) AS ReglementsDansStlSettlementView,
       (SELECT COUNT(*) FROM dbo.ComImpayedView v JOIN #echeances e ON e.PKey = v.ComDocumentId) AS EcheancesDansComImpayedView;

SELECT f.Code, st.Code AS Statut, f.Description, f.Price, f.AgreementPrice, f.AgreementAmount, f.GlobalArea
FROM dbo.ComFolder f
JOIN #dossiers d ON d.PKey = f.PKey
LEFT JOIN dbo.StrStatus st ON st.PKey = f.StrStatusId
ORDER BY f.Code ASC;

DROP TABLE #prix;
DROP TABLE #reglements;
DROP TABLE #echeances;
DROP TABLE #dossiers;

# ChantierController

Module Chantier, lot 1 (lecture seule) : projets de travaux (`PrjProject`), lots (`PrjMarket`), bordereaux définitifs, décomptes (`PrjWorkingOut`) avec leur facture (`BuyDocument`) et leurs règlements. Visibilité : un rôle exempté (Administrateur, Direction Générale) voit tout ; sinon seuls les projets habilités (`AdmUserHabilitation.PrjProjectId`) — **aucune habilitation = listes vides et 404 sur toute fiche**, contrairement au périmètre commercial. Hors périmètre = même 404 qu'un id inconnu. Montants en `BigDecimal` non formatés ; taux en pourcentage 0–100.

## referentiels

Listes de choix des filtres. `typesLot` : tous les `PrjMarketType`, triés par code numérique (la table n'a pas d'ordre : `ordre` = null). `statutsLot` : statuts de toutes les entités de lot (`StrEntity.Code` commençant par `PrjMarket`), avec leur type (`StrStatusType.Code`). `statutsFacture` : `BuyDocumentStatus` triés par `Sorting`. `categories` : seulement les `StlCategory` portées par au moins un lot visible de l'appelant.

## projets

Tous les projets du périmètre, sans pagination, triés par code. Les totaux d'un projet sont la somme, lot par lot, des mêmes grandeurs que `LotListItemDto` (même calcul). `estCentreDeCouts` = aucun lot et code « AAAA-… » : en production, 18 des 25 projets sont des centres de coûts annuels (factures dépenses), à présenter à part.

## projet

Fiche d'un projet : les champs de la liste plus les textes (null quand vides), le responsable et les dates du projet — toutes vides en production, exposées quand même.

## lots

Lots d'un projet, paginés, tri par défaut `code` décroissant. Clés de tri admises : `code`, `description`, `dateDebut`, `dateFin`, `statutOrdre`, `typeCode`, `fournisseurNom` ; une autre clé renvoie 400.

**Montant marché** = Σ des lignes du bordereau définitif (`Quantity × UnitPriceHT`, TVA = × `VatRatio`, TTC = `Quantity × UnitPriceTTC`) ; **si le lot n'a aucune ligne**, repli sur le forfait saisi (`EstimatedPrice*Manual`) — `montantsDepuisBordereau` dit quel chemin a servi. `avancementPct` = cumul TTC des décomptes / marché TTC × 100 (travaux supplémentaires exclus), null si marché nul, non plafonné. Facturé / réglé / reste : factures liées par `PrjWorkingOut.BuyDocumentId`, réglé = lignes de règlement **non annulées**.

## lot

Fiche d'un lot : champs de la liste, projet, nature (`StrEntity`), estimation (Σ bordereau estimatif, même repli sur le forfait), forfait saisi brut, retenue de garantie et avance théoriques (marché TTC × taux), taux et options, dates contractuelles (`*Manual`), prévisionnelles et d'appel d'offres/travaux (null si non saisies).

## bordereau

Lignes du bordereau définitif (`PrjMarketLine` hors stock), par ordre de ligne, avec leurs totaux recalculés ; `totaux` utilise exactement le calcul du montant marché du lot. Lignes vides = lot au montant forfaitaire.

## decomptesDuLot

Décomptes du lot par numéro (`Sorting`) croissant. Montants **stockés** tels qu'OMRAN les a figés, jamais recalculés. `montantRegle` / `resteARegler` se calculent sur le net à payer **de la facture** (`BuyDocument.AmountToPay`, régularisation comprise), pas sur celui du décompte ; 0 si le décompte n'est pas facturé.

## decomptes

Tous les décomptes visibles, paginés, tri par défaut date puis code décroissants. Clés de tri admises : `dateDocument`, `code`, `numero`, `montantHT`, `montantTTC`, `netAPayer`, `lotCode`, `projetCode`, `fournisseurNom`. Filtres combinables ; `du` / `au` portent sur la date du décompte, bornes incluses.

## decompte

Fiche d'un décompte : tous les montants stockés dans l'ordre de l'écran OMRAN ; `cumulTTC` (Σ TTC des décomptes du lot de numéro ≤) est le seul montant calculé. Les règlements de la facture sont tous listés, un règlement annulé avec `annule = true` et exclu de `montantRegle`. `lignes` est vide pour un décompte saisi uniquement en travaux supplémentaires (cas de tous les décomptes depuis mai 2026) ; `avancementLignePct` peut dépasser 100.

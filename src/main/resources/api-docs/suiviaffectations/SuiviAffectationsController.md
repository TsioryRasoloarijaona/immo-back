# SuiviAffectationsController

Suivi des affectations (écran OMRAN « Suivi des affectations ») : une ligne par affectation d'une pièce de règlement (`StlSettlement`) à une échéance de vente (`ComDocument`), c'est-à-dire une `StlSettlementLine` dont `ComDocumentId` est renseigné ; une pièce à plusieurs lignes donne plusieurs lignes. Les lignes d'une tranche `IsForSale = 0` sont exclues ; une échéance sans catégorie, un dossier sans statut ou un dossier sans article restent listés. Liste, groupes, totaux et export partagent exactement les mêmes filtres et la même visibilité : entités actives (même pour l'Administrateur) et périmètre commercial, jugés ligne par ligne sur le projet de la tranche de l'article du dossier (ligne sans tranche résolue : visible). Une tranche hors périmètre donne une liste vide, pas un 403.

## lister

`montantAffecte` = montant de la ligne, retranché (négatif) quand la pièce est un débit (`IsCredit = 0`), positif sinon (`IsCredit` NULL compris) : même règle de signe que le réglé des dossiers. `dateMouvement` = date d'opération de la pièce (`SettlementDate`), sinon sa date d'échéance ; `du` / `au` portent sur elle, jours entiers inclus, et une ligne sans date est exclue dès qu'une borne est donnée. `search` cherche, espaces ôtés des deux côtés et sans tenir compte de la casse, dans l'entité, le projet, la tranche, l'article, le tiers, la catégorie, le dossier et son statut, le libellé, la référence, le reçu, l'état et le type de la pièce, la banque, le compte et les notes. Tri par défaut `dateMouvement` décroissant (lignes sans date en dernier), départagé par `id` ; tri sur les noms du catalogue `GET /api/referentiels/colonnes/suivi-affectations` dont la colonne a un chemin, plus `id` (sinon 400). Colonnes servies sans `?colonnes=` : celles du catalogue à `parDefaut = true`.

## totauxColonnes

Effectif du filtre (pièces annulées comprises, comme `totalElements`) et un résumé par jeton d'`agregats` (`montantAffecte`, `montantEcheance`, `colonne:FONCTION`). Les sommes de `montantAffecte` excluent les lignes des pièces `CANCELED` ; `montantPiece` n'est pas sommable (une pièce à plusieurs lignes compterait plusieurs fois).

## grouper

Mêmes filtres que la liste. L'effectif d'un groupe compte les pièces annulées, sa somme de `montantAffecte` les exclut (somme d'un groupe sans ligne comptée : 0).

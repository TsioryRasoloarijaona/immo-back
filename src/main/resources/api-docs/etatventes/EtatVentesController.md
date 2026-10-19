# EtatVentesController

État des ventes (écran OMRAN « Suivi des dossiers de vente ») : une ligne par article racine en vente — `StkItem.ParentId` vide, vocation `VENTE`, tranche `IsForSale = 1` ; les parkings autonomes (`IsUnderItem = 1` sans parent) comptent, les sous-articles rattachés non. Synthèse, liste, groupes, totaux et export partagent exactement les mêmes filtres et la même visibilité (entités actives et périmètre commercial) : la somme des lignes de la liste filtrée égale la cellule de la synthèse.

## synthese

Compte les articles par tranche × catégorie × statut. Statut `LIBRE` : article sans dossier, ou dont le dossier est annulé ou désisté (`StrStatus.BoolField002` / `BoolField003`). `SANS_STATUT` : dossier sans statut, colonne présente seulement si elle compte au moins un article. Sinon le `StrStatus.Code` du dossier. `parStatut` omet les statuts à zéro ; `totaux` donne les mêmes comptes par catégorie et au total.

## lister

Filtres partagés avec la synthèse, plus `statut`, `typeId`, `nature`, `search` et la période `champDate` + `du` / `au` (jours inclus, sur une date du dossier : un article libre n'en a aucune). Article libre : `prixVenteTTC` = prix catalogue (`ComSalePrice`), aucune donnée de dossier (même un dossier annulé). Tri sur les noms du catalogue `GET /api/referentiels/colonnes/etat-des-ventes` ; `statutLibelle`, `clientNom`, `commercial`, `dossierId`, `montantRegle` et `resteARegler` ne sont pas triables (400). `montantRegle` = prix de vente réglé du dossier, même calcul que la liste des dossiers ; demandé, il coûte deux requêtes par page.

## totauxColonnes

Effectif du filtre et un résumé par jeton d'`agregats` (`surface`, `prixVenteTTC`, `colonne:FONCTION`). `montantRegle` et `resteARegler` (nom nu) sont aussi acceptés : sommes, sur tous les dossiers du filtre, du calcul de la liste des dossiers.

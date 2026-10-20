# EtatVentesController

État des ventes (écran OMRAN « Suivi des dossiers de vente ») : une ligne par article racine en vente — `StkItem.ParentId` vide, vocation `VENTE`, tranche `IsForSale = 1` ; les parkings autonomes (`IsUnderItem = 1` sans parent) comptent, les sous-articles rattachés non. Synthèse, liste, groupes, totaux et export partagent exactement les mêmes filtres et la même visibilité (entités actives et périmètre commercial) : la somme des lignes de la liste filtrée égale la cellule de la synthèse.

## synthese

Compte les articles par tranche × catégorie × statut. Statut `LIBRE` : article sans dossier, ou dont le dossier est annulé ou désisté (`StrStatus.BoolField002` / `BoolField003`). `SANS_STATUT` : dossier sans statut, colonne présente seulement si elle compte au moins un article. Sinon le `StrStatus.Code` du dossier. `parStatut` omet les statuts à zéro ; `totaux` donne les mêmes comptes par catégorie et au total.

## syntheseVendusLibres

Compte les articles racines par tranche × famille en trois états, calculés une fois dans la base de l'état des ventes : `LIBRE` (aucun dossier actif ; un dossier annulé ou désisté libère le lot), `RESERVE` (dossier en `RESERVATION` ou sans statut ; `sansStatut` les isole, déjà comptés dans `reserves`), `VENDU` (tout autre statut hors sortie). Famille par code de catégorie : `APP` → `APPARTEMENT`, `PAR` → `PARKING`, `COM` → `MAGASIN`, tout autre code → `AUTRE`. Les sous-articles rattachés (parking d'un appartement) ne sont comptés nulle part. Une ligne par tranche × famille non vide ; pourcentages arrondis à une décimale, base = total de la ligne, 0 si total nul. Chaque cellule égale le `totalElements` de `GET /api/etat-des-ventes?trancheId=&famille=&etat=`. Même visibilité que la liste : une tranche hors périmètre demandée explicitement ne rend aucune ligne (pas de 403).

## exporterVendusLibres

Mêmes filtres et mêmes lignes que `GET /api/etat-des-ventes/synthese-vendus-libres`. Colonnes : Tranche (code), Famille (libellé), Libres, Réservés, Vendus, Total, % Libres, % Réservés, % Vendus (valeurs du JSON, ex. 33.3). Après les familles de chaque tranche, une ligne « Total tranche » ; en fin, « Total général ».

## lister

Filtres partagés avec la synthèse, plus `statut`, `famille`, `etat`, `typeId`, `nature`, `search` et la période `champDate` + `du` / `au` (jours inclus, sur une date du dossier : un article libre n'en a aucune). Article libre : `prixVenteTTC` = prix catalogue (`ComSalePrice`), aucune donnée de dossier (même un dossier annulé). Tri sur les noms du catalogue `GET /api/referentiels/colonnes/etat-des-ventes` ; `statutLibelle`, `clientNom`, `commercial`, `dossierId`, `montantRegle` et `resteARegler` ne sont pas triables (400). `montantRegle` = prix de vente réglé du dossier, même calcul que la liste des dossiers ; demandé, il coûte deux requêtes par page.

## totauxColonnes

Effectif du filtre et un résumé par jeton d'`agregats` (`surface`, `prixVenteTTC`, `colonne:FONCTION`). `montantRegle` et `resteARegler` (nom nu) sont aussi acceptés : sommes, sur tous les dossiers du filtre, du calcul de la liste des dossiers.

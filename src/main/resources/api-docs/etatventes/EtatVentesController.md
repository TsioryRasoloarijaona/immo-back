# EtatVentesController

État des ventes (écran OMRAN « Suivi des dossiers de vente ») : une ligne par article racine en vente — `StkItem.ParentId` vide, vocation `VENTE`, tranche `IsForSale = 1` ; les parkings autonomes (`IsUnderItem = 1` sans parent) comptent s'ils portent un dossier non annulé ni désisté (libres : absents de toutes les sorties), les sous-articles rattachés non. `entiteId` (société du projet) ne fait que restreindre le périmètre du compte. Synthèse, liste, groupes, totaux et export partagent exactement les mêmes filtres et la même visibilité (entités actives et périmètre commercial) : la somme des lignes de la liste filtrée égale la cellule de la synthèse.

## synthese

Compte les articles par tranche × catégorie × statut. Statut `LIBRE` : article sans dossier, ou dont le dossier est annulé ou désisté (`StrStatus.BoolField002` / `BoolField003`). `SANS_STATUT` : dossier sans statut, colonne présente seulement si elle compte au moins un article. Sinon le `StrStatus.Code` du dossier. `parStatut` omet les statuts à zéro ; `totaux` donne les mêmes comptes par catégorie et au total.

## lister

Filtres partagés avec la synthèse, plus `statut`, `typeId`, `nature`, `search` et la période `champDate` + `du` / `au` (jours inclus, sur une date du dossier : un article libre n'en a aucune). Article libre : prix du catalogue `StkItem` (formules de la branche libre de la vue OMRAN `ComSaleMonitoringView`), aucune donnée de dossier (même un dossier annulé). Tri sur les noms du catalogue `GET /api/referentiels/colonnes/etat-des-ventes` ; les colonnes sans chemin (`statutLibelle`, `clientNom`, `commercial`, `dossierId` et les montants du dossier : `montantRegle`, `avance`, `netAPayer`, `tropPercu`, taux, prix au m²…) ne sont ni triables ni groupables (400).

Montants du dossier (frais syndic compris), `alerte` et `sousArticles` : même calcul que `GET /api/dossiers` (une seule formule) ; null sur une ligne libre. En demander un coûte deux requêtes par page, un prix au m² une de plus ; aucune si aucun n'est demandé. Un dossier qui retient deux articles racines apparaît sur deux lignes avec les mêmes montants, comme OMRAN.

## totauxColonnes

Effectif du filtre et un résumé par jeton d'`agregats` (`surface`, `prixVenteTTC`, `prixHt`, `colonne:FONCTION`). Les montants du dossier sommables (`montantRegle`, `resteARegler`, `avance`, `netAPayer`, `tropPercu`… : `fonctions = ["SUM"]` au catalogue) sont acceptés en nom nu : somme des lignes de la liste filtrée, donc un dossier à deux articles racines compte deux fois. Taux et prix au m² : 400.

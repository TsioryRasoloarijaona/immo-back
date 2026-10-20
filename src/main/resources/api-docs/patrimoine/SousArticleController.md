# SousArticleController

Écran OMRAN « SOUS ARTICLES » : sous-articles d'origine (`IsUnderItem = 1 AND IsUnderItemOrigin = 1`), libres ou
rattachés à un article. Le détail d'un sous-article est `GET /api/articles/{id}`. Un sous-article **rattaché** est en
lecture seule sur cet écran : la fiche de son article parent le modifie par `PATCH /api/articles/{id}`, le détache par
`POST /api/articles/{id}/sous-articles/{sousArticleId}/desaffecter`. Changer de parent = désaffecter puis rattacher
(`PATCH /api/sous-articles/{id}` avec `parentId`).

## findAll

Mêmes filtres et même catalogue de colonnes que `GET /api/articles`, limités aux sous-articles d'origine des entités
actives. `codeParent` : sous-chaîne du code de l'article parent, espaces retirés de la saisie et du code (un
sous-article libre n'y répond jamais). `etat` : dossier de la ligne elle-même (`LIBRE`, `VENDU`, `LOUE`), comme
OMRAN ; un sous-article sans dossier dont le parent est vendu reste `LIBRE` — la colonne `parentOccupe` le signale.

## creer

Création **libre** : `trancheId`, `code` (unique dans la tranche, articles compris → 409), `description`,
`categorieId` et `typeId` paramétrés « pour les sous-articles », `immeubleId`, `etageId` obligatoires (400).
Défauts : vocation VENTE (PROJET refusée), nature Bâtiment, `estTitre` faux, surfaces et prix comme `POST
/api/articles`. `parentId` facultatif : article principal de la **même tranche** et de la **même vocation** (400),
non occupé (409) ; son prix de vente est recalculé. `toujoursAffecte` vrai sans `parentId` → 400 « Vous ne pouvez
pas toujours affecter un sous article sans article parent ».

## modifier

Sous-article **libre** seulement : un rattaché → 409 (lecture seule, voir l'en-tête) ; un article principal → 404.
Mêmes champs que `PATCH /api/articles/{id}`, `vocationId` compris. `parentId` rattache le sous-article (mêmes règles
qu'à la création, prix du parent recalculé) ; `toujoursAffecte` vrai exige `parentId` (400) ; `parentId` dans
`champsAVider` → 400 (désaffecter par la route de la fiche parent).

## supprimer

Refus 409 : sous-article affecté par défaut à son article (« Vous ne pouvez pas supprimer un sous article affecté
par défaut à l'article en cours ! » — le désaffecter d'abord), dossier de vente, de location ou de syndic, ou encore
référencé (prospection, dossier, offre, historique… ; une autre clé étrangère → 409 nommant la contrainte). Sinon
suppression avec vues, contenances, tracés, pièces jointes et notes, audit `Deleted`, prix du parent recalculé.

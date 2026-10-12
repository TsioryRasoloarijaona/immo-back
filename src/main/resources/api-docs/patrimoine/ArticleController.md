# ArticleController

Articles et sous-articles (StkItem) — lots vendables d'une tranche, avec leur disponibilité et leurs éventuels sous-lots (cave/parking annexés).

## findAll

Liste paginée de tous les articles racine et sous-articles, toutes tranches confondues — pour
parcourir/rechercher le patrimoine sans devoir d'abord choisir une Tranche (contrairement à `GET
/api/tranches/{id}/articles`, point d'entrée à privilégier quand la tranche est déjà connue).
`search` filtre en sous-chaîne sur `code` OU `description` (insensible à la casse). `trancheId`
restreint à une ou plusieurs tranches (OR) sans changer d'écran. `statut` filtre sur la
disponibilité mais, à la différence des autres endpoints Article, **n'a pas de valeur par défaut
ici** : absent = tous les statuts (DISPONIBLE + OCCUPE), pour une vraie vue catalogue. Restreint aux
entités juridiques actives (ADMOD/GHRIZA), comme les autres listings Projet/Tranche/Article. Filtres
de parité avec la grille legacy "Articles" (instructions/from_front.md #4) : `entiteId` (CfgCompany,
voir `GET /api/referentiels/entites`), `natureId` (StkKind, voir `GET /api/referentiels/natures`),
`vocationId` (StkVocation, voir `GET /api/referentiels/vocations`), `etageId` (StkFloor, voir `GET
/api/referentiels/etages`), et les plages `prixDe`/`prixA` (sur `comSalePrice`) et
`surfaceDe`/`surfaceA` (sur `area`), bornes inclusives et indépendamment optionnelles.

## findById

Fonctionne indifféremment pour un article racine ou un sous-article (même table StkItem) — la
"recherche inverse" (partir d'un sous-article pour retrouver sa tranche et son article parent) passe
par ce même endpoint : `trancheId`/`trancheCode`/`trancheDescription` et
`lotParentId`/`lotParentCode`/`lotParentDescription` sont toujours renseignés quand applicable, sans
appel supplémentaire. `sousLots` liste tous les sous-articles de cet article, quel que soit leur
statut. `immeubleId`/`immeubleCode`/`immeubleLibelle` (StkBuilding),
`etageId`/`etageCode`/`etageLibelle` (StkFloor) et `natureId`/`natureCode`/`natureLibelle` (StkKind)
sont chacun des référentiels distincts de Catégorie/Type — tous null si l'article n'est rattaché à
aucune valeur (ex. un terrain n'a ni immeuble ni étage). `salePriceFloor`/`comSalePriceFloor` (prix
plancher) sont distincts de `salePrice`/`comSalePrice` (prix commercial) — ce dernier peut valoir 0
sur un article dont le prix commercial n'a jamais été saisi alors qu'un prix plancher existe déjà
(confirmé sur données réelles) ; au front de décider s'il affiche le plancher en repli dans ce cas.
`tauxTVA` (StkItem.Vat, ratio brut) et
`presentationTypeId`/`presentationTypeCode`/`presentationTypeLibelle` (StkPresentationType, "Type
Présentation" de la grille legacy — codes courts F/C/D/H..., scopés par tranche) répondent à
instructions/from_front.md #4. `superficieSousArticles` est la somme des superficies des
sous-articles rattachés (0 si aucun), distincte de `area` (superficie de l'article lui-même).

## findSousArticles

Liste paginée des sous-articles (StkItem.ParentId) d'un article donné — ex. les places de parking ou
caves annexées à un lot. `statut` filtre sur la disponibilité (défaut: DISPONIBLE) ;
`categorieId`/`typeId` filtrent comme sur `GET /api/tranches/{id}/articles`. Chaque sous-article
renvoyé porte son propre `description` (son nom), pas seulement son `code`.

## create

Réservé au rôle `Administrateur`. Crée un **article principal** (lot bâti). Obligatoires : `trancheId`, `code`
(unique **dans la tranche**, sous-articles compris, casse ignorée — le même code dans une autre tranche est permis),
`description`, `categorieId` et `typeId` (nature Bâtiment, non paramétrés « Pour les sous articles »), `immeubleId`,
`etageId`. `vocationId` vaut VENTE par défaut ; PROJET est refusée. Surfaces : provisoire 1 par défaut, définitive =
provisoire par défaut, toutes deux > 0. Le serveur pose la nature Bâtiment, le type de document OMRAN « StkItem »
(sans lui l'article est invisible dans OMRAN), laisse les colonnes d'affectation vides, met à 0 les nombres et à vide
les textes non saisis, et **calcule les prix stockés** : prix du m² TTC = HT × (1 + TVA) arrondi à 6 décimales, prix
article = prix du m² × surface (provisoire sur `surfaceProvisoire`, définitif sur `surfaceDefinitive`), prix de vente =
prix article + sous-articles rattachés.

### reponse 400

Champ obligatoire absent ; tranche, vocation, catégorie, type, immeuble ou étage introuvable ; catégorie ou type hors
nature Bâtiment ou paramétré pour les sous-articles ; vocation PROJET ; surface ≤ 0 (« La valeur de la supérficie
provisoire doit être strictement supérieure à 0 ! ») ; prix négatif ; TVA ou remise hors de [0 ; 1] ; texte trop long.
Le ProblemDetail porte `champ`.

## update

Réservé au rôle `Administrateur`. Vaut pour un article comme pour un sous-article. PATCH : absent ou `null` = inchangé,
`""` = vider un texte facultatif. `trancheId` ne se modifie pas (400). Un sous-article garde la vocation de son article
(400 si elle change) et ses listes sont celles « Pour les sous articles ». Les prix stockés ne sont recalculés que si
une surface, un prix au m² ou la TVA change ; le prix de vente de l'article parent suit alors.

### reponse 400

Mêmes causes qu'à la création, plus un changement de `trancheId`, ou de vocation sur un sous-article.

### reponse 409

Code déjà pris dans la tranche ; ou **article occupé** (dossier de vente ou de location, ou parent qui en a un) dont la
demande change un champ protégé — code, vocation, catégorie, type, surfaces, prix au m², TVA, remises : « L'article X
est affecté au dossier Y : ses prix, surfaces et classification ne se modifient plus ». Description, notes, adresse,
titre foncier, titré, surfaces intérieure/murs/parties communes, immeuble et étage restent modifiables. `champ` nomme
le premier champ protégé changé.

## createSousArticle

Réservé au rôle `Administrateur`. Crée un lot annexe (parking, jardin, terrasse) **rattaché** à l'article `id` : même
tranche, même vocation et même nature que lui ; immeuble de l'article par défaut ; catégorie et type paramétrés « Pour
les sous articles » ; non titré par défaut. Le prix de vente de l'article est recalculé.

### reponse 400

L'article `id` est lui-même un sous-article ; sinon mêmes causes qu'à la création d'un article.

## affecter

Réservé au rôle `Administrateur`. Le sous-article doit être **libre** dans la même tranche (voir
`GET /api/tranches/{id}/sous-articles-libres`). Corps facultatif `{ "toujoursAffecte": true }`.

### reponse 409

« Ce sous article n'est pas libre » : déjà rattaché, toujours affecté, affecté à un dossier, pas un sous-article, ou dans
une autre tranche ; ou article cible affecté à un dossier.

## desaffecter

Réservé au rôle `Administrateur`. Le sous-article redevient libre (`toujoursAffecte` remis à faux) ; le prix de vente de
l'article est recalculé.

### reponse 409

Le sous-article n'est pas rattaché à cet article ; il a un dossier (« Vous ne pouvez pas désaffecter un sous article
ayant un dossier de vente ! », idem location) ; ou l'article est affecté à un dossier.

## getPiecesJointes

Retourne toutes les pièces jointes rattachées à l'article `id` (`AdmAttachedFile.CategoryId =
StkItem.PKey`, même convention polymorphe sans FK déclarée que pour les pièces jointes de Dossier).
Ce sont essentiellement des plans (types `PLAN`/`PLAN_ARCHITECTE`/`PLAN_ETAGE`), mais aucun filtrage
sur `type`/`Category` n'est appliqué ici — tout ce qui est rattaché à l'article ressort. Triées par
date de dépôt décroissante. Les lignes "cochées mais jamais déposées" (checklist sans fichier réel)
sont exclues. Liste vide si aucune pièce jointe.

## getFichier

**Renvoie une redirection `302` vers une URL présignée**, pas le contenu binaire : le fichier est
stocké sur S3/MinIO et transite directement entre le client et le stockage, sans passer par le
backend.

L'URL signée est valable quelques minutes et vaut autorisation d'accès ; elle n'est délivrée
qu'après vérification que la pièce appartient bien à cet article.

**La disposition est `inline`** : le plan s'affiche dans un onglet ou une visionneuse sans être
téléchargé, ce qui est l'usage courant ici. Le nom proposé reprend la description de la pièce.

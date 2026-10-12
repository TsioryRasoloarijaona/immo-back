# TrancheController

## findAll

Toutes les tranches des entités actives, tous projets confondus, ou filtrées sur un seul projet via `projetId`.
Évite le contournement N+1 (GET /api/projets puis GET /api/projets/{id}/tranches pour chacun) pour peupler une
checklist de filtre côté front — voir instructions/filtres-avances.md point 2. `search` filtre côté serveur, casse
ignorée (code, libellé, ville, code et libellé du programme, entité). `colonnes` choisit les colonnes servies parmi
le catalogue `GET /api/tranches/colonnes` (colonnes de la liste TRANCHES d'OMRAN) ; absent, les colonnes par défaut :
code, description, ville, date d'octroi des T.F, programme, standing, entité, « Visible dans le module Vente ». Les
libellés lus à travers le programme, l'entité ou une référence ne sont calculés que s'ils sont demandés. Courrier
fait à, représentant légal, signataire et responsables sont ceux du programme, comme `CfgTrancheView`.

## create

Réservé au rôle `Administrateur`. Obligatoires : `projetId` (programme existant), `code`, `description`. Le code
est saisi, débarrassé de ses espaces de bord et **unique sur toute la table**, pas par programme (règle d'OMRAN),
casse ignorée. Valeurs initiales d'OMRAN pour tout champ absent : délais 60/30 (engagements), 48 (rétractation),
2 (intervention), 4 (visites), 60/30 (minutes notaire / tout soldé), écarts-types 5/3, pénalité d'annulation sur le
prix de l'article, tout autre nombre à 0, cases à décocher (dont les visibilités Projet, Achat, SAV, gestion
immobilière et location-vente), textes vides. Exception : `estEnVente` vaut **true** par défaut (le module Vente
n'affiche que ces tranches). `concretisationPercent` est toujours recopié de `advancePercent`. Références
facultatives : standing, niveau (« Nbr. étage »), rubrique, plan comptable, projet de facturation (listes
`/api/referentiels-patrimoine/…`), responsable SAV (un personnel), tiers autrui (un fournisseur). Le créateur reçoit
l'habilitation OMRAN sur la tranche (`AdmUserHabilitation`).

### reponse 400

Champ obligatoire absent ou vide ; programme, pays ou autre référence introuvable ; responsable SAV qui n'est pas un
personnel, tiers autrui qui n'est pas un fournisseur ; taux hors de [0 ; 1] (dont « Le crédit max ne doit pas
dépasser 100% ! ») ; montant ou délai négatif ; date de fin (travaux, commercialisation) antérieure au début ; texte
trop long ; `champsAVider` invalide. Le ProblemDetail porte `champ` = nom du champ en faute.

## update

Réservé au rôle `Administrateur`. PATCH : un champ absent ou `null` reste inchangé ; `""` vide un texte facultatif
(refusé pour `code` et `description`). Pour vider une date (dont les 17 dates d'identification foncière), une
référence, un texte ou un nombre, lister son nom dans `champsAVider` : date et référence → `null`, texte → `""`,
nombre → 0 (jamais NULL, comme OMRAN). Nom inconnu, champ obligatoire (`projetId`, `code`, `description`) ou champ à
la fois renseigné et listé → 400 avec `champ` = `champsAVider`. `projetId` ne se modifie pas : une valeur différente
du programme actuel donne 400 « Le programme d'une tranche ne se modifie pas ». `concretisationPercent` suit
`advancePercent`.

### reponse 400

Mêmes causes qu'à la création, plus un `projetId` différent du programme actuel.

## findSousArticlesLibres

Lots annexes de la tranche qu'on peut rattacher à un article (`POST /api/articles/{id}/sous-articles/{sousArticleId}/affecter`) :
`IsUnderItem = 1`, non « toujours affecté », sans article parent, sans dossier de vente ni de location — le filtre
d'OMRAN plus les deux garde-fous d'occupation. Triés par code par défaut.

## findArticles

Liste paginée, mélangeant articles racine et sous-articles de la tranche (chacun porte
`sousArticle`/`lotParentId` pour les distinguer). Filtrable sur la disponibilité (`statut`, défaut
DISPONIBLE), la catégorie (`categorieId`, StkItemCategory — ex. "APPARTEMENT") et le type (`typeId`,
StkItemType — ex. "1 CHAMBRES + SALON"), à cocher indépendamment comme dans le formulaire de
qualification Tranche → Catégorie → Type → Article. Les listes de choix viennent de `GET
/api/referentiels/categories-bien` / `GET /api/referentiels/types-bien`. Filtres de parité avec la
grille legacy "Articles" (instructions/from_front.md #4) : `entiteId` (CfgCompany, voir `GET
/api/referentiels/entites`), `natureId` (StkKind, voir `GET /api/referentiels/natures`),
`vocationId` (StkVocation, voir `GET /api/referentiels/vocations`), `etageId` (StkFloor, voir `GET
/api/referentiels/etages`), et les plages `prixDe`/`prixA` (sur `comSalePrice`) et
`surfaceDe`/`surfaceA` (sur `area`), bornes inclusives et indépendamment optionnelles.

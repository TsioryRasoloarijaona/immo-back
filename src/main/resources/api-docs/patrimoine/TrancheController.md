# TrancheController

## findAll

Toutes les tranches, tous projets confondus, ou filtrées sur un seul projet via `projetId`. Évite le
contournement N+1 (GET /api/projets puis GET /api/projets/{id}/tranches pour chacun) pour peupler
une checklist de filtre côté front — voir instructions/filtres-avances.md point 2.

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

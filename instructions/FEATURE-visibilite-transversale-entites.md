# FEATURE — Visibilité transversale par entité juridique active

**Statut** : à implémenter
**Branche** : `feat/entites-actives-et-fiche-reservation` (suite du commit `3785da9`)
**Dépôt** : `immo-crm` uniquement

---

## 1. Le besoin

L'application de bureau légataire (*Omran Estate*) n'expose que **deux entités juridiques** :
**ADMOD** et **GHRIZA**. Tout ce que l'utilisateur y voit et y manipule provient de ces deux
sociétés — dossiers, prospections, clients, encaissements, devis, interactions, agenda, tableau de
bord.

Le backend ne reproduit ce comportement que **partiellement** : depuis `3785da9`, seuls le
patrimoine (projets / tranches / articles) et les référentiels filtrent sur `CfgCompany.IsActive`.
`GET /api/dossiers`, `/api/clients`, `/api/encaissements`, `/api/devis`, `/api/prospections`,
`/api/hotline/recherche`, `/api/calendrier/evenements` et `/api/dashboard` **ne filtrent pas**.

Cet entre-deux est visible pour l'utilisateur : un dossier MBR ou ART est listé, mais sa tranche
n'existe pas dans le référentiel et sa fiche de réservation renvoie `409` (bug #10 de
`SESSION_MEMORY.md`). Cette feature ferme l'écart.

---

## 2. État réel mesuré (`yesRemsProd`, 2026-09-07)

`CfgCompany.IsActive` est déjà correct en base : **ADMOD** (1 projet, 2 tranches) et **GHRIZA**
(1 projet, 3 tranches) sont les seules actives ; les 7 autres sociétés (AJFR, ANIS MOUAFIK,
BENACEUR, DIOURI ARCHI, MAYLINE, YASMINE MEHDI, YESTEST) sont inactives. Les 5 tranches actives
correspondent exactement à la grille de l'application de bureau : `APF-HIDDEN`, `APF` (ADMOD),
`ARB-ALC`, `ars hidden`, `ARS` (GHRIZA).

Volumétrie de ce que le filtre laisse passer / masque :

| Domaine | Entité active | Entité inactive | Aucune entité résoluble | Total |
|---|---:|---:|---:|---:|
| `ComFolder` (dossiers) | 116 | 242 | 0 | 358 |
| `ComProspection` (prospections + pré-prospections) | 11 595 | 6 036 | **192** | 17 823 |
| `StlSettlement` (règlements de vente) | 332 | 312 | 0 | 644 |
| `StlQuote` (devis) | 71 | 227 | 0 | 298 |
| `ComActionMessage` (interactions) | 29 024 | 17 450 | **1 644** | 48 118 |
| `StkItem` (articles) — *déjà filtré* | 500 | 563 | 5 | 1 068 |

Deux mesures qui justifient des choix d'implémentation :

- **`StlSettlement.CfgCompanyId` est une FK directe, fiable.** Sur les 644 règlements de vente :
  644 concordent avec la société déduite par la chaîne dossier → article → tranche → projet,
  **0 divergent**, et **aucun** règlement ne touche deux sociétés. Le prédicat des encaissements
  s'écrit donc directement sur la racine, sans sous-requête `EXISTS`.
- **`GET /api/encaissements` est déjà restreint aux règlements de vente** (au moins une ligne
  portant un `ComDocumentId`). Les 2 599 règlements d'achat (`BuyDocumentId`, 2 559 lignes sur
  3 222) ne sont pas concernés par cette feature.

---

## 3. Règles

### R1 — Le filtre est global et permanent

Contrairement au périmètre commercial (opt-in, par compte), la visibilité par entité s'applique
**à tout appelant, toujours**, y compris aux rôles de `RolesExemptesPerimetre` (dont
`Administrateur`). Une entité inactive n'est pas « une donnée que ce compte n'a pas le droit de
voir » : c'est une donnée qui **n'est plus en exploitation**, au même titre que dans le légataire.

### R2 — Entité non résoluble ⇒ ligne visible

Le prédicat est toujours `société.isActive = true OR société.id IS NULL`, jamais une jointure
interne. Une prospection sans tranche (192 lignes) ou une interaction rattachée ni à une
prospection ni à un dossier (1 644 lignes) n'appartient à aucune entité — la masquer perdrait le
lead pour protéger une donnée que personne ne revendique.

C'est **la même règle §6.4 que le périmètre commercial**, ce qui permet aux deux prédicats de se
composer sans se contredire.

> Noter la différence avec `StkItemSpecifications.entitesActivesOnly`, qui utilise `cb.isTrue(...)`
> seul et masque donc les 5 articles non résolus. L'écart est assumé : un article sans tranche est
> une anomalie de catalogue, une prospection sans tranche est un lead légitime.

### R3 — `isActive = true`, pas `!= false`

Une société dont le drapeau n'a jamais été renseigné reste invisible — convention déjà posée par
`CfgCompanyRepository.findActivesOrderByDescriptionAsc()`. Ne pas la relâcher.

### R4 — Le filtre s'applique aussi à l'accès par id

Sans cela il se contourne en énumérant les ids : `GET /api/dossiers` masquerait un dossier MBR que
`GET /api/dossiers/{id}` servirait quand même, échéancier et documents compris. Les trois
`verifierAccesXxx()` de `PerimetreCommercialService` — appelés par `PerimetreInterceptor` sur
**toute** route `/api/dossiers/{id}/…` — doivent donc s'exécuter **même lorsque l'appelant n'est
pas restreint par un périmètre**, ce qui n'est pas le cas aujourd'hui.

Une ressource masquée est indiscernable d'une ressource inexistante : **404, jamais 403**.

### R5 — Les prospects ne sont pas filtrés

`CfgTier` n'a aucun lien vers une entité juridique : un prospect est une personne, pas un bien.
`GET /api/prospects` reste non filtré ; ce sont ses prospections et ses dossiers qui le sont
(`GET /api/prospects/{id}/prospections|dossiers|encaissements`).

`GET /api/clients` est différent : un client **est défini** comme « un tier référencé par au moins
un `ComFolder` ». Le filtre s'applique donc à la sous-requête `ComFolder`, et un client dont tous
les dossiers sont sur une entité inactive disparaît de la liste — par construction, pas par
décision supplémentaire.

---

## 4. Surface à modifier

### 4.1 Nouveau — `services/perimetre/VisibiliteEntites.java`

Fabrique de prédicats, sans état (aucune dépendance à l'appelant), donc **statique** et non un
bean. Une méthode par racine, chacune posant elle-même ses `LEFT JOIN`.

> Piège à éviter : `path.get("cfgCompany").get("isActive")` produit une **jointure interne
> implicite** en Criteria API et masquerait les lignes sans société, en violation de R2. Toujours
> `join("cfgCompany", JoinType.LEFT)` explicite.

### 4.2 Composition dans les 5 `filtreXxx()` de `PerimetreCommercialService`

Les 9 sites d'appel existants héritent du filtre **sans être modifiés** :

| Méthode | Sites d'appel |
|---|---|
| `filtreDossiers()` | `DossierService:246`, `ProspectSubResourcesService:117` |
| `filtreProspections()` | `ProspectionService:167`, `:186`, `ProspectSubResourcesService:103` |
| `filtreEncaissements()` | `EncaissementService:145`, `ProspectSubResourcesService:136` |
| `filtreDevis()` | `DevisService:162` |
| `filtreInteractions()` | `HotlineService:123` |

Les deux prédicats partagent la **même chaîne de jointures** : la construire une fois par lambda,
ne pas composer par `.and()` de deux `Specification` (cela dupliquerait les `LEFT JOIN` en SQL).

Chaînes de résolution, inchangées par rapport au périmètre :

- dossier → `stkItem` → `stkHierarchy` (`CfgTranche`) → `cfgProject` → `cfgCompany`
- prospection → `stkHierarchy` → `cfgProject` → `cfgCompany`
- devis → `cfgTranche` → `cfgProject` → `cfgCompany`
- interaction → `coalesce(prospection → …, dossier → …)`, prospection d'abord
- encaissement → `cfgCompany` **en direct** (justifié en §2)

### 4.3 Sites nécessitant leur propre accroche

| Fichier | Ce qu'il faut faire |
|---|---|
| `ClientService.findAll` | ajouter le prédicat d'entité dans la sous-requête `ComFolder` |
| `CalendrierService.evenements` | la méthode dérivée ne porte aucun prédicat — passer par une `@Query`/`Specification` portant le filtre |
| `DashboardService` | `findDashboardProjection[DansPerimetre]` et les 4 compteurs de prospection |
| `PerimetreCommercialService.verifierAcces{Dossier,Prospection,Encaissement}` | R4 — s'exécuter aussi hors périmètre (`existsVisible(id)`) |

### 4.4 Déjà fait (commit `3785da9`) — ne pas retoucher

`ProjetService`, `TrancheService`, `ArticleService`, `ReferentielService`,
`CfgProjectRepository`, `CfgTrancheRepository`, `CfgCompanyRepository`.

---

## 5. Documentation OpenAPI

Chaque `@Operation.description` des endpoints touchés doit mentionner la restriction, en français :
la liste ne renvoie que les données des entités juridiques en exploitation
(`CfgCompany.IsActive`), les lignes sans entité résoluble restant visibles. `/api-docs` est la
seule source de vérité du frontend sur cette règle.

---

## 6. Tests attendus

1. Un dossier sur entité inactive est **absent** de `GET /api/dossiers` et renvoie **404** en accès
   direct par id (R1 + R4).
2. Une route fille (`/api/dossiers/{id}/echeancier`) sur ce même dossier renvoie **404** (R4).
3. Une prospection **sans tranche** reste présente dans `GET /api/prospections` (R2) — c'est le
   test qui verrouille les 192 lignes.
4. Un client dont l'unique dossier est sur une entité inactive est absent de `GET /api/clients` ;
   un client ayant au moins un dossier actif est présent (R5).
5. Un compte **`Administrateur`** subit le filtre comme les autres (R1) — verrouille le fait que
   la visibilité n'est pas du périmètre déguisé.
6. Périmètre commercial **et** entité active se composent : un compte restreint à un projet d'une
   entité inactive voit une liste vide, pas la liste du périmètre.

---

## 7. Impact front

Rupture de contrat pour `crm-prototype`, à relayer :

- **Volumes en forte baisse** sur `/api/dossiers` (358 → 116), `/api/encaissements` (644 → 332),
  `/api/devis` (298 → 71), `/api/prospections` (17 823 → 11 787), `/api/hotline/recherche`.
  Tout écran affichant un total figé ou un compteur en dur doit être revérifié.
- **`GET /api/dossiers/{id}` peut désormais renvoyer 404** sur un id qui répondait `200` la veille
  (dossiers MBR / ART). Idem pour toutes les sous-routes. Un lien profond mémorisé côté front
  cassera — c'est attendu.
- **Le `409` de `GET /api/dossiers/{id}/documents/fiche-reservation` sur ART/MBR disparaît**,
  remplacé par un `404` en amont : ces dossiers ne sont plus adressables. Le bug #10 est clos par
  cette feature, sans avoir à dessiner les gabarits MBR et ART.
- Le tableau de bord change d'assiette : ses agrégats ne portent plus que sur ADMOD + GHRIZA.

Rien à faire côté backend au-delà de ce signalement : le frontend est un autre dépôt et une autre
PR.

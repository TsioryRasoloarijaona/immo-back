# FEATURE — Filtre par tranche sur le tableau de bord (administrateur)

Branche : `feat/dashboard-filtre-tranche` dans **immo-crm** et **crm-prototype**
(coupée depuis `main` le 06/10/2026).

## Objectif

Sur le tableau de bord (`/`, `crm-prototype/src/pages/Dashboard.tsx`), un
**administrateur** choisit une tranche (`CfgTranche`, alias `StkHierarchy`) et
**tout le tableau de bord se recalcule pour cette tranche** : KPI, répartition
par étape, couloirs (prospections, à traiter, pré-prospections, dossiers
récents, clients), relances du jour / en retard, synthèse annuelle.

Un **commercial** ne voit pas le filtre, et le backend **refuse** le paramètre
s'il le reçoit d'un appelant non administrateur (double contrôle, décision
utilisateur du 06/10/2026).

## Décisions utilisateur (06/10/2026)

1. **Une tranche à la fois, ou toutes.** Liste déroulante : « Toutes les
   tranches » (défaut, comportement actuel inchangé) ou une seule tranche.
2. **Bloc Clients** : avec une tranche choisie, seuls les clients ayant **au
   moins un dossier dans cette tranche**.
3. **Sécurité** : le front masque le filtre aux non-administrateurs **et** le
   backend répond **403** si un non-administrateur envoie `trancheId` à l'un des
   paramètres **ajoutés par cette feature**.

## Backend (immo-crm)

### Paramètre commun

`trancheId` — `UUID`, optionnel, **une seule valeur**. Absent = comportement
actuel, octet pour octet.

Ajouté à :

| Endpoint | Ce qui est restreint à la tranche |
|---|---|
| `GET /api/dashboard` | tous les agrégats du `DashboardDto` (`valeurPipeline`, `panierMoyen`, `dossiersParEtape`, `relancesEnRetard`, `prospectionsOuvertes`, `prospectionsTotal`, …) |
| `GET /api/dashboard/relances` | relances dont la prospection porte cette tranche (les deux `echeance`, les deux `portee`) |
| `GET /api/dashboard/annuel` | les trois blocs (`echeancier`, `ventesConcretisees`, `encaissements`) et `dossiersSansDateAvance` |
| `GET /api/clients` | clients ayant au moins un dossier (`ComFolder`) dont la tranche est `trancheId` |

`GET /api/dossiers` et `GET /api/prospections` / `GET /api/pre-prospections`
acceptent **déjà** `trancheId` (liste) : **ne pas y toucher**, ni à leur
autorisation (les pages de liste des commerciaux s'en servent). Le front les
appelle avec `trancheId=<id>`.

`GET /api/dashboard/nouveaux-pre-prospects` : n'est plus appelé par le tableau
de bord (cf. commentaire Dashboard.tsx l.47) → hors périmètre.

### Règles (chacune testée)

- **R1 — Autorisation.** Si `trancheId` est présent et que l'appelant n'a pas le
  rôle `Administrateur` → **403**, sur chacun des 4 endpoints du tableau
  ci-dessus. Le contrôle lit les autorités de l'appelant authentifié (même
  source que `@PreAuthorize("hasRole('Administrateur')")` de
  `CleApiController`), **pas** un paramètre de requête. Sans `trancheId`, un
  commercial garde exactement son accès actuel (200, périmètre inchangé).
- **R2 — Une seule source de vérité pour « appartient à la tranche ».** Le
  rattachement d'un dossier à une tranche se fait comme dans le filtre
  `trancheId` existant de `GET /api/dossiers` (« au moins un article dans la
  tranche ») ; celui d'une prospection comme dans le filtre existant de
  `GET /api/prospections` (`StkHierarchyId`). **Réutiliser ces prédicats**, ne pas
  en écrire un second : les chiffres du tableau de bord filtré doivent être
  égaux aux totaux des listes filtrées sur la même tranche.
- **R3 — Cohérence liste / tableau de bord.** Pour une tranche T :
  `prospectionsOuvertes(T)` = `totalElements` de `GET /api/prospections` avec les
  mêmes filtres que le compteur actuel + `trancheId=T` ; somme de
  `dossiersParEtape(T)` = `totalElements` de `GET /api/dossiers?trancheId=T`.
- **R4 — Le filtre ne fait que restreindre.** Le périmètre commercial et la
  restriction aux entités juridiques actives continuent de s'appliquer ; la
  tranche s'y ajoute en ET.
- **R5 — Tranche inconnue ou sans données** → 200 avec agrégats à zéro / listes
  vides (pas 404). `trancheId` non parsable → 400.
- **R6 — Argent.** `valeurPipeline`, `panierMoyen` et les montants de la
  synthèse annuelle filtrés sont calculés par **le même code** que les montants
  non filtrés (un prédicat de plus, pas un calcul parallèle). Test : pour une
  tranche réelle, `valeurPipeline(T)` ≤ `valeurPipeline()`, et
  `valeurPipeline(T)` = somme des montants des dossiers actifs renvoyés par
  `GET /api/dossiers?trancheId=T`. (Pas d'égalité « somme sur toutes les
  tranches = total » : un dossier dont les articles couvrent deux tranches
  compte dans les deux — même sémantique que le filtre liste.)

### OpenAPI

`@Parameter` en français sur chaque nouveau `trancheId`, mentionnant :
« réservé aux administrateurs — 403 sinon » ; ajouter la réponse `403` dans
`@ApiResponses`. Puis `npm run docs:update` et **`git diff docs/api-docsV4.json`**
avant d'écrire le front.

## Frontend (crm-prototype)

- **Visibilité** : `isAdmin = user?.roles?.includes('Administrateur')` (même
  règle que `Layout.tsx`). Non admin → aucun filtre rendu, **aucun**
  `trancheId` envoyé.
- **Contrôle** : une liste déroulante dans l'en-tête de la page Dashboard,
  libellé « Tranche », option par défaut « Toutes les tranches ». Options via
  `getTranchesReferentiel()` (référentiel non paginé déjà dans `api.ts`),
  libellé = code + libellé de la tranche.
- **Effet** : la valeur choisie (`trancheId | undefined`) est passée à
  **tous** les appels du tableau de bord : `getDashboard`, les deux
  `listProspections`, `listPreProspections`, `listDossiers`, `listClients`, les
  deux `listRelancesDashboard`, la synthèse annuelle (`SyntheseAnnuelle.tsx`),
  et aux dialogues « Voir tout » (`ListeProspectionsDialog`,
  `ListeRelancesDialog`) pour que la liste complète corresponde au couloir. Les
  `useApi` prennent `trancheId` en dépendance.
- **Mémoire** : le choix est conservé dans l'URL (`?tranche=<id>`) pour survivre
  au rechargement ; un `?tranche=` présent pour un non-admin est ignoré.
- **États** : pendant le rechargement après changement de tranche, garder la
  mise en page (pas d'écran blanc) ; erreur → `ErrorState` existant ; tranche
  sans données → les zéros et couloirs vides habituels. Quand une tranche est
  active, un rappel discret près du titre (« Tranche : <code> ») avec un
  bouton pour revenir à « Toutes les tranches ».
- `types.ts` / `api.ts` : ajouter `trancheId?: UUID` aux paramètres de
  `getDashboard`, `listRelancesDashboard`, `getSyntheseAnnuelle` (nom réel dans
  api.ts), `listClients` — noms du fil exactement.

## Hors périmètre

- Les pages de liste (Dossiers, Prospections, Clients…) : aucun changement
  visible.
- Le filtre par projet, ou multi-tranches.
- `GET /api/dashboard/nouveaux-pre-prospects`.
- Les autorisations des paramètres `trancheId` déjà existants.

## Vérification

Backend (`./mvnw test`, suite complète verte, compte à rapporter) — un test
d'intégration par point :

1. R1 : commercial + `trancheId` → 403 sur les 4 endpoints ; commercial sans
   `trancheId` → 200 ; admin + `trancheId` → 200.
2. R3 : pour une tranche ayant des données dans `yesRemsTest`, égalité
   compteurs tableau de bord / `totalElements` des listes filtrées.
3. R5 : UUID aléatoire → 200 et zéros ; `trancheId=abc` → 400.
4. R6 : `valeurPipeline(T)` ≤ total et = somme des dossiers actifs de la liste filtrée.
5. `GET /api/clients?trancheId=T` (admin) ne renvoie que des clients ayant un
   dossier dans T.
6. Non-régression : `GET /api/dashboard` sans paramètre renvoie le même JSON
   qu'avant (tests existants verts).

Frontend : `npm run build` exit 0. Captures d'écran : (a) admin, « Toutes les
tranches » ; (b) admin, une tranche choisie, chiffres changés ; (c) commercial,
pas de filtre.

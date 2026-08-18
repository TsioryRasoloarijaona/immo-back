# Implémentation des endpoints — AyadERP (immo-crm)

## Règles de travail (à respecter sur toute la tâche, sans exception)

1. **Ne jamais modifier `pom.xml` sans demander d'abord.** Si l'implémentation
   d'un endpoint révèle un besoin de nouvelle dépendance (ex. `springdoc-openapi`,
   une lib de mapping), **arrête-toi et demande** avant d'ajouter quoi que ce
   soit au `pom.xml`. Ne suppose jamais qu'une dépendance est "sans doute déjà
   là" — vérifie le fichier avant d'écrire du code qui en dépend.
2. **Un endpoint (ou un petit groupe cohérent) à la fois.** Pour chaque tâche
   de la liste ci-dessous : implémente (DTO + repository si besoin + service +
   controller) → écris le test → **exécute le test** (`./mvnw test -Dtest=...`)
   → seulement si vert, passe à la tâche suivante. Ne pas enchaîner
   l'implémentation de plusieurs endpoints avant d'avoir fait tourner les tests
   du précédent.
3. **Si un test échoue et que la cause n'est pas évidente en 1-2 tentatives**,
   ne pas s'acharner en boucle — signale le blocage avec le message d'erreur
   complet plutôt que de continuer sur les tâches suivantes avec une base
   cassée.
4. **Respecter la structure déjà posée** :
   - `entities/{cfg,com,stk,stl}` et `repositories/{cfg,com,stk,stl}` — miroir
     des préfixes legacy, un repository par entité
   - `services/`, `controller/`, `dto/` — organisés par domaine métier
     (Prospection, Dossier, Prospect, Patrimoine), jamais par préfixe
   - `domain/` — logique métier pure (enums, tables de transition), aucune
     annotation JPA ici
5. **`TransitionsValidees` (statut Dossier) doit être validée côté service**,
   pas seulement documentée. Toute tentative de transition hors de la table
   retourne `409 Conflict` via `ProblemDetail` (RFC 7807), avec la liste des
   transitions autorisées dans le corps de la réponse.
6. **Tester au minimum** : le happy path de chaque endpoint, un cas d'erreur
   (404 sur id inexistant), et pour `PATCH .../statut` spécifiquement le rejet
   d'une transition invalide (409). Pas besoin d'une couverture exhaustive de
   tous les cas limites à ce stade.

---

## Inventaire complet des endpoints à générer

### Prospection (`ComProspection`)

| Méthode | Route | Description |
|---|---|---|
| GET | `/api/prospections` | Liste, filtres `etape`/`origine`/`commercial`/`search`/`ouvertes` |
| GET | `/api/pre-prospections` | Liste où `Prospect IS NULL AND Commercial IS NULL` |
| GET | `/api/prospections/{id}` | Détail complet |
| POST | `/api/pre-prospections/{id}/creer-prospect` | Crée un `CfgTier` lié |
| POST | `/api/prospections/{id}/transformer` | Pré-prospection → Prospection |
| PATCH | `/api/prospections/{id}/etape` | Change `Nature` (validation d'appartenance à l'enum, pas de table de transition stricte ici) |
| PATCH | `/api/prospections/{id}/archiver` | Archive avec raison |
| GET | `/api/prospections/{id}/interactions` | Journal (`ComActionMessage`) |
| POST | `/api/prospections/{id}/interactions` | Ajoute une interaction |

### Prospect / Client (`CfgTier`)

| Méthode | Route | Description |
|---|---|---|
| GET | `/api/prospects` | Liste, filtres `search`/`type` |
| GET | `/api/prospects/{id}` | Détail complet |
| POST | `/api/prospects` | Création |
| PATCH | `/api/prospects/{id}` | Édition |
| GET | `/api/prospects/{id}/prospections` | Prospections liées |
| GET | `/api/prospects/{id}/dossiers` | Dossiers liés |
| GET | `/api/commerciaux` | Liste des personnes assignables — **source à confirmer** (`CfgTier` filtré par `CfgTierKind`, ou table `Adm*`) avant d'implémenter |

### Dossier de vente (`ComFolder`)

| Méthode | Route | Description |
|---|---|---|
| GET | `/api/dossiers` | Liste, filtres `statut`/`commercial`/`projet`/`search` |
| GET | `/api/dossiers/{id}` | Détail complet, inclut `etapesAtteignables` calculé |
| POST | `/api/dossiers` | Création depuis une prospection |
| PATCH | `/api/dossiers/{id}/statut` | **Validation TransitionsValidees obligatoire, 409 si invalide** |
| GET | `/api/dossiers/{id}/echeancier` | Lignes de `ComDocument` |
| POST | `/api/dossiers/{id}/echeancier` | Ajoute une ligne |
| PATCH | `/api/dossiers/{id}/echeancier/{echeanceId}` | Édite une ligne |
| POST | `/api/dossiers/{id}/echeancier/{echeanceId}/reglements` | Enregistre un `StlSettlement` |
| GET | `/api/dossiers/{id}/montants` | Récap par catégorie — **calculé serveur**, jamais stocké |

### Patrimoine (`CfgProject` / `CfgTranche` / `StkItem`)

| Méthode | Route | Description |
|---|---|---|
| GET | `/api/projets` | Liste |
| GET | `/api/projets/{id}` | Détail |
| GET | `/api/projets/{id}/tranches` | Tranches du projet |
| GET | `/api/tranches/{id}` | Détail (inclut champs de règles métier : pénalités, délais) |
| GET | `/api/tranches/{id}/articles` | Lots, filtre `statut` (défaut `DISPONIBLE`) |
| GET | `/api/articles/{id}` | Détail (inclut sous-lots via `lotParentId`) |

### Dashboard

| Méthode | Route | Description |
|---|---|---|
| GET | `/api/dashboard` | Valeur pipeline, panier moyen, dossiers par étape, relances en retard |

**Total : 27 endpoints.**

Les DTOs (`records`) pour chacun sont déjà spécifiés en détail dans
`API-endpoints-vente-prospection-patrimoine.md` — s'y référer plutôt que de
réinventer les champs. Adapter uniquement les noms d'entités JPA sources
(`ComFolder` et non `Dossier`, `CfgTier` et non `Contact`, etc. — voir le
tableau de correspondance dans le récapitulatif de session).

---

## Ordre d'implémentation recommandé

Grouper par dépendances, pas par ordre alphabétique — chaque phase doit être
verte (tests passants) avant la suivante :

**Phase 0 — Fondations (pas d'endpoint, mais bloquant pour tout le reste)**
1. `domain/StatutDossier` (enum, 13 valeurs + les 2 sorties)
2. `domain/TransitionsValidees` (table de transitions, au minimum la
   transition confirmée depuis `ENVOI_AU_NOTAIRE` ; le reste temporairement
   "tout sauf soi-même" avec un commentaire `// TODO: transition non confirmée`)
3. `domain/EtapeProspection` (enum, 7 valeurs)
4. Vérifier/générer les repositories manquants pour les 29 entités

**Phase 1 — Patrimoine (le plus simple, aucune règle de transition, bon
échauffement)**
5. `GET /api/projets`, `/api/projets/{id}`, `/api/projets/{id}/tranches`
6. `GET /api/tranches/{id}`
7. `GET /api/tranches/{id}/articles`, `/api/articles/{id}`

**Phase 2 — Prospect**
8. `GET /api/prospects`, `/api/prospects/{id}`
9. `POST /api/prospects`, `PATCH /api/prospects/{id}`
10. `GET /api/commerciaux` — **s'arrêter et demander confirmation de la
    source de données avant d'écrire le code** (point ouvert non résolu)

**Phase 3 — Prospection**
11. `GET /api/prospections`, `/api/pre-prospections`, `/api/prospections/{id}`
12. `POST /api/pre-prospections/{id}/creer-prospect`
13. `POST /api/prospections/{id}/transformer`
14. `PATCH /api/prospections/{id}/etape`, `.../archiver`
15. Interactions (`GET`/`POST /api/prospections/{id}/interactions`)

**Phase 4 — Dossier (le plus dense, dépend de tout ce qui précède)**
16. `GET /api/dossiers`, `/api/dossiers/{id}`
17. `POST /api/dossiers`
18. `PATCH /api/dossiers/{id}/statut` — **tester explicitement le rejet 409**
19. Échéancier : `GET`/`POST`/`PATCH` sur `/echeancier`
20. `POST /api/dossiers/{id}/echeancier/{echeanceId}/reglements`
21. `GET /api/dossiers/{id}/montants` — vérifier que le calcul groupe bien
    par catégorie et ne compte pas `FRAIS` dans le total soumis à la règle
    prix TTC (cf. `FEATURE-plan-financement.md`)

**Phase 5 — Dashboard**
22. `GET /api/dashboard`

---

## Definition of done, par endpoint

- [ ] DTO(s) créé(s) dans `dto/`, noms alignés sur `API-endpoints-vente-prospection-patrimoine.md`
- [ ] Service dans le bon package domaine métier
- [ ] Controller avec `ProblemDetail` sur les cas d'erreur (404 minimum)
- [ ] Test écrit et **exécuté avec succès** avant de passer à l'endpoint suivant
- [ ] Aucune modification de `pom.xml` sans validation explicite demandée et obtenue

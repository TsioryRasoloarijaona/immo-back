# Session Memory
_Last updated: 2026-09-25T12:20+01:00_
_Project: immo-crm (AyadERP) — C:\Users\tsiory\Documents\ayad-internship\prototype\immo-crm_

---

## 🎯 Current Objective

Deux chantiers en parallèle : (1) **parité fonctionnelle avec OMRAN sur la prospection**, pilotée
par la trace d'audit (dernier lot : **clés d'API pour n8n**, branche poussée, PR à merger ;
doublons de prospect + unicité prospect × projet mergé en PR #83) ; (2) **mise en production du second VPS (`vps_ayad`, client)** à côté du
VPS de dev (`vps_tsiory`) — pipeline en matrice mergé, vérification CORS sur `vps_ayad` encore à
faire. Les deux monitorings **restent séparés** (décision utilisateur du 24/09).

---

## ✅ What Worked

### Livré ce 25/09 (après-midi) — **`feat/cle-api-n8n`** (poussée, **PR à merger**)
- Demande : authentification pour n8n (insère les pré-prospections) et pour un serveur MCP.
  Décision : **clé d'API** (`X-Api-Key`) pour n8n dans ce lot ; le MCP (JWT utilisateur en
  `Authorization: Bearer` + `POST /api/auth/jeton`) est décrit en §6 de la spec, **pas fait**.
- Mesures : 12 856 lignes n8n (`Code 50/90`) en SQL direct, **12 060 sans `UserCreate`** ; tout
  leur contenu est couvert par `POST /api/pre-prospections`.
- Code : table `AppApiClient` (`011_AppApiClient.sql`, ajoutée à `ci/schema/010_tables.sql` +
  `020_foreign_keys.sql`, **appliquée à `yesRemsTest`**) ; `ApiKeyAuthenticationFilter` avant le
  JWT (401 `CLE_INVALIDE`, 403 `HORS_PORTEE`) ; `PorteeCleApi` ; `CleApiService` (SHA-256,
  `ak_` + 43 car.) ; `/api/cles-api` (Administrateur) ; schéma OpenAPI `cleApi`.
- Règle découverte : un `AdmUser` **sans habilitation ni rôle exempté n'a aucune restriction de
  périmètre** (`perimetreDe` = `Optional.empty()`) → la portée de la clé est la seule limite.
- Tests : `CleApiAuthentificationTest` 10, `CleApiAdminTest` 8, suite **1023 verts**. Spec
  `instructions/FEATURE-cle-api-n8n.md` (§8 : mise en service côté n8n).

### Mergé le 25/09 — **`feat/doublons-prospect-projet`** (PR #83)
- Demande : avant de créer un prospect depuis une pré-prospection, retrouver les fiches au même
  nom / téléphone / email et laisser choisir « c'est lui » ou « nouveau contact » ; et un prospect
  n'a qu'une prospection par projet (une prospection porte plusieurs dossiers).
- Mesures `yesRemsProd` (14 040 PRO/PRORNT/CLI/CLIRNT) : 244 groupes d'homonymes, 491 de
  téléphones, 170 d'emails, mais **6 seulement nom + tel identiques** → on affiche, on ne refuse
  pas. Valeurs bidon : `000000000`/`111111111` (17 fiches chacun), `x@hotmail.fr` (154), `-`,
  `nd`, `.`. Prospections : 217 couples prospect × projet répétés sur 13 884, **216 après
  clôture, 1 seul à deux ouvertes** (MOUNIR/APF, saisi deux fois dans OMRAN). Dossiers : 369
  prospections × 1, 5 × 2 à 6.
- OMRAN (trace d'audit) contrôle les prospects par `REPLACE(FirstName,' ','')`/`LastName` dans
  les deux ordres **ET** `EMail`, types PRO + CLI ; **aucun** contrôle prospect × projet.
- Code : `GET /api/prospects/doublons` (critères `NOM`/`TELEPHONE`/`EMAIL` par fiche, 50 max) ;
  `POST /api/prospects` et `creer-prospect` → **409** `doublons` sauf `ignorerDoublons: true` ;
  `POST /api/pre-prospections/{id}/lier-prospect` ; règle 2 en 409 `prospectionExistante` sur
  `POST /api/prospections`, `transformer`, `tranche-principale` (si le projet change),
  `lier-prospect`. Fermée ne bloque pas ; pré-prospection ne compte pas ; `IsClosed NULL` = ouverte.
- Tests : `ProspectDoublonsTest` 14, `ProspectionUniciteProjetTest` 16, suite **1005 verts**.
  Spec `instructions/FEATURE-doublons-prospect-projet.md`.

### Mergé le 24/09 — `feat/desarchiver-prospection` (PR #82)
- `PATCH /api/prospections/{id}/desarchiver` ; « archivée » = `IsClosed` seul ; `archiver`
  désarme les relances comme OMRAN. Voir `FEATURE-desarchiver-prospection.md`.

### Mergé les 23–24/09 (rappel court)
- PR #72 création pré-prospection (+ import CSV Meta) ; PR #73 suivi des actions ; PR #74 retrait
  Qodana ; PR #75 `deploy.yml` en matrice `vps_tsiory`/`vps_ayad` ; PR #76 CORS
  `arpent.ayaddeveloppement.com` + tag `sha-…` ; PR #78/#79 `nettoyer_orphelins`.

### Méthode
- **Trace d'audit OMRAN** (`sys.fn_get_audit_file('C:\backup\*.sqlaudit')` depuis `master`) pour
  les `SELECT COUNT` de contrôle et la séquence d'écritures ; **`AdmWebHistorics.details`** pour
  le diff colonne par colonne. Mesurer avant de concevoir.
- Éditions accentuées : Write/Edit ou script Python `.py` dans le scratchpad (`io.open` utf-8) ;
  **les heredocs bash longs cassent** (« unexpected EOF ») → fichier `.py` puis `python fichier`.
  Vérifier `grep 'Ã'` = 0. Commit via `git commit -F <scratchpad>` ; `git add` par chemins.
- `grep -c` renvoie 1 quand le compte est 0 → ne pas le chaîner avec `&&`.
- Tests 409 dans la transaction de test : la mutation d'entité avant le `throw` reste visible dans
  le contexte de persistance (pas de rollback intermédiaire) — asserter sur ce qui est écrit
  **après** le contrôle, ou compter les lignes.
- **MockMvc partage le contexte de sécurité entre les requêtes d'un même test** : une requête
  acceptée par un filtre qui fait `setAuthentication` remplace le `@WithMockUser` pour les
  requêtes suivantes → mettre ces appels en dernier, ou dans un test séparé.
- `addFilterBefore(x, JwtAuthenticationFilter.class)` doit venir **après** l'enregistrement du
  filtre JWT (« does not have a registered order » sinon).

---

## ❌ What Failed

- **Heredoc bash long (Python inline)** → `unexpected EOF` → script `.py` en fichier.
- **`git add -A`** → fichiers IDE sur `main` → chemins explicites.
- `indicatifMobile: "212"` dans un test sans référentiel pays → 400 ; ne mettre le mobile que
  là où le test l'exige.
- **Image `mssql/server:2022` : `mssql` = uid/gid 10001** → `chown -R nahoma:10001`, `chmod 2770`.
- `docker compose restart` ne relit pas le `.env` → `up -d`.
- **Horloges** : Windows +00:00 vs JVM `Africa/Casablanca` +01:00 → fixtures datées d'hier.
- Lecture de `<prototype>/.claude/scripts/*.mjs` bloquée ; les **exécuter** via `node <chemin absolu>` marche.

---

## 🐛 Bugs & Errors Log

| # | Erreur / Bug | Statut | Correctif |
|---|---|---|---|
| 29 | Pipeline n8n à l'arrêt depuis le 08/09 | **Open** (externe) | import CSV possible |
| 30 | Heure de RDV non captée (front `type="date"`) | **Open** (front) | backend prêt |
| 33 | 13 actions sans auteur (18→22/09) | **Fixed** pour les nouvelles ; les 13 restent | script de rattrapage proposé |
| 42 | `vps_ayad` conflit `<id>_immo-back` pendant le `up` | **Fix mergé** (PR #78), **non revérifié** | second essai après échec |
| 43 | CORS bloqué depuis `arpent.ayaddeveloppement.com` | **Cause trouvée**, **à vérifier** sur `vps_ayad` | `docker inspect immo-back` = `sha-…` courant |
| 44 | `archiver` ne désarmait pas les relances | **Fixed** (PR #82) | `desarmerRelancesDeLaProspection` |
| 45 | Doublon de prospection ouverte prospect × projet (MOUNIR/APF, 2 saisies OMRAN) | **Fixed** côté CRM (branche `feat/doublons-prospect-projet`) | règle 2, 409 |
| 46 | `26/05941` existe **deux fois** dans `yesRemsProd` (2e ligne sans `UserCreate`, 20/09) | **Open** (hors périmètre) | compteur `AdmSequence` en retard ; à vérifier sur `yesRemsDev` |
| 47 | 12 060 pré-prospections n8n sans auteur (SQL direct) | **Fix livré** (branche `feat/cle-api-n8n`) | n8n passe par l'API avec sa clé → `UserCreate = n8n` |

---

## 🔑 Key Points to Remember

- **Clé d'API** : `X-Api-Key: ak_…`, compte de service `AdmUser` au nom de la clé, portée
  fermée (`PorteeCleApi`) ; `/api/cles-api` n'est dans aucune portée. Créer la clé n8n après
  merge : `POST /api/cles-api {"nom":"n8n","portees":["PRE_PROSPECTIONS_ECRIRE","REFERENTIELS_LIRE"]}`.
- **`011_AppApiClient.sql` à appliquer par l'utilisateur** sur `yesRemsProd` et `yesRemsDev`
  avant tout déploiement de cette branche (sinon le filtre échoue sur table absente).
- Cas MCP = jeton **utilisateur** en `Authorization: Bearer` (à faire), pas une clé.

- **Doublons de prospect** : afficher, ne pas refuser — sauf 409 tant que `ignorerDoublons` n'est
  pas `true`. Le front doit gérer ce 409 sur `POST /api/prospects` et `creer-prospect`.
- **Un prospect, un projet, une prospection ouverte** ; les dossiers se multiplient sur la
  prospection. 54 pré-prospections réelles tomberont en 409 à la transformation si l'autre est
  ouverte — voulu.
- **Archivée = `IsClosed` seul** ; `IsClosed NULL` n'est pas archivée (donc compte comme ouverte).
- **Pré-prospection = `StrEntityId = B92DF3D8-…`** ; prospection vente = `E7C73F70-1111-40D9-AA78-A2825C42B601`.
- Format des mobiles en base : `6 65 68 09 64` (sans zéro, espacé, indicatif à part) →
  comparer sur 9 derniers chiffres.
- `/api/prospects` n'est **pas** une collection scopée par `PerimetreInterceptor` (seuls dossiers,
  prospections, pre-prospections, encaissements le sont).
- **Deux VPS, deux Environments GitHub** (`vps_tsiory`, `vps_ayad`). Les `VPS_*` au niveau dépôt
  existent encore → à supprimer une fois `vps_ayad` vert.
- Mode de travail : backend seulement ; git via `node <prototype>/.claude/scripts/git-flow.mjs`
  (**chemin absolu**) ; **demander avant toute branche** ; commit + push dans le même tour dès que
  la suite est verte ; **attendre le merge GitHub** avant `sync`. `sync back` ne supprime pas
  toujours les branches mergées → `git branch -d` (refuse si non mergée).

---

## 📁 Important Files & Paths

- `instructions/FEATURE-cle-api-n8n.md` — spec des clés d'API (§5 comportement, §6 MCP hors lot, §8 mise en service).
- `security/ApiKeyAuthenticationFilter`, `security/CleApiService`, `domain/PorteeCleApi`,
  `services/auth/CleApiAdminService`, `controller/auth/CleApiController`, `entities/app/AppApiClient`.
- `src/main/resources/db/migration/011_AppApiClient.sql` ; `ci/schema/010_tables.sql` + `020_foreign_keys.sql`.
- `instructions/FEATURE-doublons-prospect-projet.md` — spec du lot (règle OMRAN, mesures, contrat,
  impact front).
- `services/prospect/ProspectService.java` — `rechercherDoublons`, `estProspectOuClient`,
  normalisations (`cleNom`, `critereTelephone`, `critereEmail`, `neufDerniers`) ; `createEntity` (409).
- `services/prospection/ProspectionService.java` — `lierProspect`, `verifierUniciteProspectProjet`,
  `projetDe` ; appels dans `creerProspection` / `transformer` / `modifierTranchePrincipale`.
- `repositories/cfg/CfgTierRepository.trouverDoublons`, `repositories/com/ComProspectionRepository.trouverOuvertesMemeProspectMemeProjet`.
- `exceptions/DoublonsProspectException`, `ProspectionExistanteException` + `web/GlobalExceptionHandler` (propriétés `doublons`, `prospectionExistante`).
- `instructions/FEATURE-desarchiver-prospection.md` — lot précédent.
- `.github/workflows/deploy.yml` — build (tag `sha-…`), matrice 2 VPS.
- `graphify-out/` (gitignoré) — hook post-commit lancé ; `instructions/**` à passer par `/graphify . --update`.

---

## 🔧 Environment & Config

- SQL Server local `localhost,49687` (compte `tsix`, identifiants dans `CLAUDE.md`) ; bases
  `yesRems` (OMRAN, auditée), `yesRemsProd` (mesures), `yesRemsTest` (tests). Audit :
  `sys.fn_get_audit_file('C:\backup\*.sqlaudit')` depuis `master`, heures en UTC.
- `Invoke-Sqlcmd` : un seul jeu de résultats par appel si les colonnes diffèrent (`Format-Table`
  perd les suivants) → un `Invoke-Sqlcmd` par requête.
- Tests : `./mvnw.cmd test` — 1005 verts au 25/09.
- `vps_tsiory` = `api.tsiory.tech`, user `prod`, `/home/prod/app` ; `vps_ayad` =
  `vps.ayaddeveloppement.com`, user `nahoma`, `~/arpent`.
- Scratchpad de session pour scripts Python, messages de commit (`git commit -F`).

---

## ⏭️ Next Step

> **Attendre le merge GitHub de `feat/cle-api-n8n`**. Une fois confirmé : `sync back`, puis
> **demander** avant toute nouvelle branche. À la mise en service : appliquer
> `011_AppApiClient.sql` sur le VPS, créer la clé `n8n` par `POST /api/cles-api`, configurer le
> nœud HTTP n8n (Header Auth `X-Api-Key`, `POST /api/pre-prospections`), retirer son accès SQL.
> Impact front à relayer (lot précédent, mergé) : 409 `doublons` sur `POST /api/prospects` et
> `creer-prospect` (+ `ignorerDoublons`, `lier-prospect`), 409 `prospectionExistante`.

### Backlog
- [ ] **MCP** : `Authorization: Bearer <jwt>` dans `JwtAuthenticationFilter` + `POST /api/auth/jeton`
      (spec §6 de `FEATURE-cle-api-n8n.md`) — branche à demander.
- [ ] Écran front d'administration des clés (`/api/cles-api`) — additif.
- [ ] Vérifier `vps_ayad` (bug #43/#42) : `docker inspect immo-back --format '{{.Config.Image}}'`
      = tag courant ; preflight `OPTIONS /api/auth/connexion` avec
      `Origin: https://arpent.ayaddeveloppement.com` → 200.
- [ ] Bug #46 : vérifier le compteur `AdmSequence` « Prospection » sur `yesRemsDev` vs `MAX(Code)`.
- [ ] Supprimer les 4 `VPS_*` au niveau dépôt GitHub une fois `vps_ayad` vert sur un run.
- [ ] Rollback par bouton : `workflow_dispatch` (`tag`, `cible`) — branche `ci/rollback-par-tag`, à demander.
- [ ] `vps_ayad` : `.env` complet (`JWT_SECRET` différent, `APP_JWT_COOKIE_SECURE=true`).
- [ ] Script SQL (appliqué par l'utilisateur) pour rattacher les 13 actions anonymes du 18→22/09.
- [ ] Appliquer `010_AppTraceOmran.sql` sur `yesRemsDev` ; audit sur le 115 avant `AUDIT_TRACE_PATH`.
- [ ] Moniteur n8n ; adresses projets ; Grafana par VPS ; `graphify label` optionnel.

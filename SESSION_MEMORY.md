# Session Memory
_Last updated: 2026-09-24T12:15+01:00_
_Project: immo-crm (AyadERP) — C:\Users\tsiory\Documents\ayad-internship\prototype\immo-crm_

---

## 🎯 Current Objective

Deux chantiers en parallèle : (1) **parité fonctionnelle avec OMRAN sur la prospection**, pilotée
par la trace d'audit (dernier lot : archivage / désarchivage, branche poussée, PR à merger) ;
(2) **mise en production du second VPS (`vps_ayad`, client)** à côté du VPS de dev
(`vps_tsiory`) — pipeline en matrice mergé, vérification CORS sur `vps_ayad` encore à faire.
Les deux monitorings **restent séparés** (décision utilisateur du 24/09).

---

## ✅ What Worked

### Livré ce 24/09 — **`feat/desarchiver-prospection`** (6af2a7d, poussée, **PR à merger**)
- Méthode : l'utilisateur a archivé puis désarchivé la prospection `26/05947` dans OMRAN ; la
  trace d'audit + `AdmWebHistorics.details` ont donné le diff exact de chaque action :
  - archivage = `IsClosed` 0→1, `ClosingDate`, `ComProspectionClosingReasonId` **+**
    `UPDATE ComActionMessage SET WithoutRevival=1` (même prédicat que la transformation en vente)
    **+** `UPDATE AdmAlertData SET Active=0` ;
  - désarchivage = `[["IsClosed","True","False"]]` **et rien d'autre** — date et raison conservées,
    relances non réarmées.
- Preuve sur `yesRemsProd` : 88 prospections vente `IsClosed=0` portent encore date **et** raison
  de clôture → **« archivée » = `IsClosed`/`fermee` seul, jamais `ClosingDate != null`**.
- Code : `PATCH /api/prospections/{id}/desarchiver` (sans corps, 200/404/409 si pas archivée) ;
  `archiver` appelle désormais `desarmerRelancesDeLaProspection` et pose `UserUpdate` ;
  `@Schema` sur `fermee`/`dateFermeture`/`raisonFermeture*` (liste + fiche). `AdmAlertData` non
  reproduit (table hors dépôt, sans effet côté app).
- Tests : `ProspectionEtapeArchiverTest` 12/12, suite complète **974 verts**. Spec
  `instructions/FEATURE-desarchiver-prospection.md`. `feature-index add desarchiver-prospection`
  fait. PR : https://github.com/TsioryRasoloarijaona/immo-back/compare/main...feat%2Fdesarchiver-prospection

### Mergé les 23–24/09 (rappel court)
- PR #72 création pré-prospection (+ import CSV Meta) ; PR #73 suivi des actions (parité 48 213) ;
  PR #74 retrait Qodana ; PR #75 `deploy.yml` en matrice `vps_tsiory`/`vps_ayad` (secrets par
  Environment GitHub) ; PR #76 CORS `arpent.ayaddeveloppement.com` + tag immuable `sha-…` ;
  PR #78/#79 `nettoyer_orphelins` avant/après `up` (run `sha-9f109b8` vert sur `vps_tsiory`).
- Second VPS préparé à la main (`vps.ayaddeveloppement.com`, user `nahoma`, `~/arpent`) : compose
  identique à `/home/prod/app`, volume SQL recréé, `.bak` restaurés, `docker login ghcr.io`.

### Méthode
- **Trace d'audit OMRAN** (`sys.fn_get_audit_file('C:\backup\*.sqlaudit')` depuis `master`) pour
  la séquence d'écritures, **`AdmWebHistorics.details`** (`objectId` = PKey) pour le **diff
  colonne par colonne** que l'ORM masque (il réécrit les 53 colonnes). Combiner les deux.
- Mesurer avant de concevoir (population, parité vue ↔ requête) ; `@WithMockUser` = auteur
  **anonyme** (`UtilisateurAffiche.courant().nom()` null) — ne pas asserter `UserUpdate` en test.
- Éditions accentuées : outil Edit/Write (UTF-8 propre, vérifié `grep 'Ã'` = 0) ; commit message
  via fichier scratchpad + `git commit -F` ; `git add` **par chemins explicites**.
- graphify : le hook post-commit ne couvre pas `instructions/**` → `/graphify . --update` à la
  main ; le manifeste était périmé (432 fichiers « changés », cache sémantique vide) → 7
  sous-agents Sonnet, ~790 k tokens. Graphe : 5 915 nœuds, 289 communautés, santé OK ;
  `graphify cluster-only .` auto-nomme les communautés (évite l'étiquetage manuel de 289) ;
  HTML en vue agrégée (> 5 000 nœuds).

---

## ❌ What Failed

- **Heredoc bash + accents / `\\n`** → passer par un fichier `.py` ou l'outil Write.
- **`git add -A`** → fichiers IDE sur `main` → chemins explicites.
- **`ComActionMessageView` comme source** → 6,6 s la page ; absente de `yesRemsTest`.
- **Sous-requête dans un `GROUP BY`** → SQL Server erreur 144 → `@JoinFormula` ou table dérivée.
- Dates `'2026-09-16'` en T-SQL → `'20260916'`.
- **Image `mssql/server:2022` : `mssql` = uid/gid 10001**, pas dans le groupe root → `chown -R
  nahoma:10001`, `chmod 2770`, `660` sur les `.bak`.
- `docker compose restart` ne relit pas le `.env` → `up -d` ; `SA_PASSWORD` seulement à la 1re init.
- `!` dans un mot de passe en bash → `set +H`.
- **Horloges** : Windows « Morocco Standard Time » = +00:00, JVM `Africa/Casablanca` = +01:00 →
  tests `now()` vs `GETDATE()` en jours cassent entre 23 h et minuit → fixtures datées d'hier.
- `docker compose up` à la main pendant un run → conflit `<id>_immo-back`.
- `meta.outputs.version` (docker/metadata-action) = tag de plus haute priorité (`latest`), pas le SHA.
- Centralisation des monitorings : conçue, **écartée par l'utilisateur**.
- `ssh vps_ayad` en BatchMode → `Permission denied (publickey)` ; `ssh vps-db` (tsiory) OK.
- Lecture de `<prototype>/.claude/scripts/*.mjs` depuis cette session → bloquée
  (`blockReadsOutsideWorkingDirectories`) ; les **exécuter** via `node <chemin absolu>` marche.
- AST graphify en parallèle sous Windows via stdin → `BrokenProcessPool`, repli séquentiel
  automatique (sans conséquence).

---

## 🐛 Bugs & Errors Log

| # | Erreur / Bug | Statut | Correctif |
|---|---|---|---|
| 29 | Pipeline n8n à l'arrêt depuis le 08/09 | **Open** (externe) | import CSV possible |
| 30 | Heure de RDV non captée (front `type="date"`) | **Open** (front) | backend prêt |
| 33 | 13 actions sans auteur (18→22/09) | **Fixed** pour les nouvelles ; les 13 restent | script de rattrapage proposé, non fait |
| 36–37 | SQL Server 2nd VPS (`Access is denied` master.mdf / `.bak`) | **Fixed** (à la main) | volume recréé, droits 10001 |
| 38 | `vps_tsiory` conflit `<id>_immo-back` | **Fixed** (PR #76 + #78) | nettoyage des orphelins |
| 39 | `IMMO_BACK_TAG=latest` à chaque déploiement | **Fixed** (PR #76) | tag `sha-…` |
| 40 | `sqlserver` 2nd VPS `unhealthy` après `ALTER LOGIN sa` | **Fixed** (à la main) | `.env` aligné + `up -d` |
| 41 | `ProspectionFiltresAvancesTest` échoue 23 h–minuit | **Fixed** (PR #76) | fixture `now().minusDays(1)` |
| 42 | `vps_ayad` conflit `<id>_immo-back` pendant le `up` | **Fix mergé** (PR #78), **non revérifié sur `vps_ayad`** | second essai après échec |
| 43 | CORS bloqué depuis `arpent.ayaddeveloppement.com` | **Cause trouvée** (correctif jamais démarré sur `vps_ayad`), **à vérifier** | `docker inspect immo-back` = `sha-…` courant ; preflight `OPTIONS /api/auth/connexion` → 200 |
| 44 | `archiver` ne désarmait pas les relances (écart avec OMRAN) | **Fixed** (branche `feat/desarchiver-prospection`) | appel `desarmerRelancesDeLaProspection` |

---

## 🔑 Key Points to Remember

- **Archivée = `IsClosed` seul.** Le désarchivage OMRAN ne touche ni `ClosingDate` ni la raison ;
  le front doit brancher le badge sur `fermee`, pas sur `dateFermeture`.
- **Pré-prospection = `StrEntityId = B92DF3D8-…`** ; prospection vente = `E7C73F70-…` ; catégorie
  d'action Prospection = `69F8E7D6-1111-1111-1111-1AA8B1F02008`.
- **Auteur d'une action** : `UserName` ; « Système » = à personne ; `admin` porté par 2 comptes.
- `/api/suivi-actions` et `/api/kpi` hors `PerimetreInterceptor` : périmètre dans le service ; toute
  route sans id sous une collection scopée → `PerimetreInterceptorInvarianteTest.ROUTES_SANS_ID_AUTORISEES`.
- **Deux VPS, deux Environments GitHub** (`vps_tsiory`, `vps_ayad`), 4 secrets chacun. Les `VPS_*`
  au niveau **dépôt** existent encore → repli silencieux ; à supprimer une fois `vps_ayad` vert.
- `vps_ayad` : le run `sha-eb6bed4` a écrit `IMMO_BACK_TAG` ailleurs que `~/arpent/.env` (mystère) ;
  si le conflit revient : `sudo grep -rl '^IMMO_BACK_TAG=' /home /root /opt`.
- Rollback aujourd'hui = Actions → ancien run → « Re-run all jobs ». Amélioration proposée, non
  commencée : `workflow_dispatch` (`tag`, `cible`), build sauté si tag, `pull` avant `.env`.
- Mode de travail : backend seulement ; git via `node <prototype>/.claude/scripts/git-flow.mjs`
  (**chemin absolu**) ; **demander avant toute branche** ; commit + push dans le même tour dès que
  la suite est verte ; **attendre le merge GitHub** avant `sync`.

---

## 📁 Important Files & Paths

- `instructions/FEATURE-desarchiver-prospection.md` — spec du lot archivage/désarchivage (trace
  OMRAN, tableau IsClosed × ClosingDate × raison, impact front).
- `services/prospection/ProspectionService.java` — `archiver()` / `desarchiver()` /
  `marquerModification()` ; `controller/prospection/ProspectionController.java` — routes +
  springdoc ; `dto/prospection/ProspectionDetailDto.java`, `ProspectionListItemDto.java` — `@Schema`
  clôture ; `test/.../ProspectionEtapeArchiverTest.java` — 12 cas.
- `repositories/com/ComActionMessageRepository.desarmerRelancesDeLaProspection` — le prédicat OMRAN
  (`AlertValidityDate IS NOT NULL AND ComDocumentId IS NULL`), partagé vente/archivage.
- `.github/workflows/deploy.yml` — build (tag `sha-…`), matrice 2 VPS, `nettoyer_orphelins`, sonde `:9091`.
- `src/main/java/com/tsix/immocrm/web/CorsConfig.java` + `CorsConfigTest` — origines de prod.
- `monitoring/{README.md,grafana-*.json}` — seuls fichiers monitoring du dépôt ; le reste vit sur les VPS.
- `graphify-out/` (gitignoré) — `graph.json`, `GRAPH_REPORT.md`, `graph.html` (agrégé), `manifest.json`
  à jour au 24/09 ; sauvegarde des labels dans `graphify-out/2026-09-24/`.

---

## 🔧 Environment & Config

- SQL Server local `localhost,49687` (compte `tsix`, identifiants dans `CLAUDE.md`) ; bases
  `yesRems` (celle d'OMRAN, auditée), `yesRemsProd` (mesures), `yesRemsTest` (tests). Audit :
  `sys.fn_get_audit_file('C:\backup\*.sqlaudit')` depuis `master`, heures en UTC.
- Windows « Morocco Standard Time » affiché +00:00 ; JVM +01:00 (bug #41).
- Tests : `./mvnw.cmd test` — 974 verts au 24/09 ~11 h.
- `vps_tsiory` = `api.tsiory.tech`, user `prod`, `/home/prod/app` ; `vps_ayad` =
  `vps.ayaddeveloppement.com`, user `nahoma`, `~/arpent`. Tunnel SQL : `ssh -N -L 1434:127.0.0.1:1433 vps_ayad`.
- graphify : interpréteur `C:\Python314\python.exe` (`graphify-out/.graphify_python`) ; pas de
  `tree_sitter_sql` (16 `.sql` ignorés) ; `GRAPHIFY_VIZ_NODE_LIMIT` ne lève pas la vue agrégée.
- Scratchpad de session pour scripts Python, messages de commit (`git commit -F`) et simulations.

---

## ⏭️ Next Step

> **Attendre le merge GitHub de `feat/desarchiver-prospection`** (PR ci-dessus). Une fois
> confirmé : `node <prototype>/.claude/scripts/git-flow.mjs sync back` depuis la racine du dépôt,
> puis **demander** avant toute nouvelle branche. Impact front à relayer (additif) : bouton
> « Désarchiver » sur fiche `fermee = true`, badge « Archivée » sur `fermee`.

### Backlog
- [ ] Vérifier `vps_ayad` (bug #43/#42) : `docker inspect immo-back --format '{{.Config.Image}}'`
      = tag courant ; preflight `OPTIONS /api/auth/connexion` avec
      `Origin: https://arpent.ayaddeveloppement.com` → 200 + `Access-Control-Allow-Origin`.
      Si `curl` passe mais pas le navigateur → Caddy de `api.ayaddeveloppement.com`.
- [ ] Supprimer les 4 `VPS_*` au niveau dépôt GitHub une fois `vps_ayad` vert sur un run.
- [ ] Rollback par bouton : `workflow_dispatch` (`tag`, `cible`) — branche `ci/rollback-par-tag`, à demander.
- [ ] `vps_ayad` : `.env` complet (`JWT_SECRET` différent, `APP_JWT_COOKIE_SECURE=true`,
      `MAIL_ENABLED`/`WP_ENABLED=false` si les deux serveurs partagent une base).
- [ ] Script SQL (appliqué par l'utilisateur) pour rattacher les 13 actions anonymes du 18→22/09.
- [ ] Synchroniser les comptes (`AdmUser`/`AppUserCredential`) vers la source avant le passage
      deux-bases en prod ; 1er déploiement deux-bases : ne PAS pré-remplir `AppSyncEtat` (bug #27).
- [ ] Appliquer `010_AppTraceOmran.sql` sur `yesRemsDev` ; audit sur le 115 avant `AUDIT_TRACE_PATH`.
- [ ] Moniteur n8n (jauge + alerte 24 h) ; credential SQL n8n avant fermeture de la redirection 115.
- [ ] Adresses projets (APO, Villa Lazrak, adresses « siège »).
- [ ] Importer `monitoring/grafana-vue-generale.json` dans le Grafana de chaque VPS (séparés).
- [ ] Optionnel : `graphify label` pour renommer les 289 communautés par LLM (noms actuels = hub).

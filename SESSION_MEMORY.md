# Session Memory
_Last updated: 2026-09-24T02:45+01:00_
_Project: immo-crm (AyadERP) — C:\Users\tsiory\Documents\ayad-internship\prototype\immo-crm_

---

## 🎯 Current Objective

**Mettre en production le second VPS (`vps_ayad`, client) à côté du VPS de dev (`vps_tsiory`)** :
pipeline de déploiement en matrice sur les deux serveurs, CORS pour le nouveau frontend,
fiabilisation du `docker compose up` distant. Les deux monitorings **restent séparés** pour le
moment (décision utilisateur du 24/09, centralisation étudiée puis abandonnée).

---

## ✅ What Worked

### Livré et MERGÉ (sessions 23–24/09)
1. **PR #72 `feat/creation-pre-prospection`** — `POST /api/pre-prospections` + import CSV Meta ;
   pré-prospection = `StrEntity.Code = 'ComPreProspection'` partout (847 lignes réelles migrées).
2. **PR #73 `feat/suivi-actions`** — `GET /api/suivi-actions[/groupes]` (parité 48 213 actions,
   0 écart avec la vue), accès admin/soi, KPI sur toutes les actions, correctif auteur
   (`UtilisateurAffiche.courant()`).
3. **PR #74** — retrait des fichiers Qodana embarqués par `git add -A`.
4. **PR #75 `ci/deploiement-second-vps`** (29acecd) — `deploy.yml` : job `deploy` en **matrice**
   `cible: [vps_tsiory, vps_ayad]`, `environment: ${{ matrix.cible }}`, `fail-fast: false`. Les 4
   secrets `VPS_HOST/USER/SSH_KEY/PATH` vivent dans l'Environment GitHub du nom de la cible ;
   `TOKEN` reste au dépôt.
5. **PR #76 `fix/cors-ayaddeveloppement`** (7a70370) —
   - `CorsConfig` : origine exacte `https://arpent.ayaddeveloppement.com` (pas de joker, convention
     « chaque origine de prod listée »). 4 tests ajoutés (`CorsConfigTest`, 8 verts).
   - `deploy.yml` : tag immuable calculé `sha-${GITHUB_SHA::7}` — `meta.outputs.version` renvoyait
     `latest` (priorité raw 200 > sha 100), **tous les déploiements passés tournaient sur `latest`**.
   - `ProspectionFiltresAvancesTest` : fixture datée d'hier (bug #41).
6. **PR #78 `ci/nettoyage-conteneur-immo-back`** (a69e16a + f839036 « test the pipeline » par
   l'utilisateur) — fonction `nettoyer_orphelins` (noms via `docker ps -a --format` + grep
   `^[0-9a-f]{12}_immo-back$`, `|| true` obligatoire sous `pipefail`), appelée avant `up` et une
   seconde fois après un `up` en échec, puis un unique nouvel essai. Vérifié hors ligne : `bash -n`
   + simulation avec un faux `docker` (3 cas). **Run `sha-9f109b8` : `vps_tsiory` vert** et à jour.

### Second VPS préparé à la main (`vps.ayaddeveloppement.com`, user `nahoma`, `~/arpent`)
- Compose identique à `vps_tsiory` (`/home/prod/app`) : SQL Server 2022 Express, MinIO, Loki,
  Alloy, Grafana, Prometheus, cAdvisor, node-exporter. Frontend `arpent.ayaddeveloppement.com`,
  API `api.ayaddeveloppement.com`.
- Volume `arpent_mssql_data` recréé propre (un conteneur `compose run` orphelin le tenait avec des
  fichiers aux mauvais droits) ; `.bak` restaurés depuis `~/arpent/myssql-backups` (monté en
  `/var/backups`) ; `docker login ghcr.io` fait pour `nahoma` ; clé SSH du runner générée sur le VPS.

### Méthode
- **Trace d'audit** OMRAN (`sys.fn_get_audit_file`) pour reconstituer les écrans avant de spécifier.
- Mesurer avant de concevoir (population, parité vue ↔ requête).
- Éditions multi-lignes accentuées : **script Python dans le scratchpad** (`python -X utf8`), jamais
  heredoc bash ni `Get-Content | Set-Content` (double encodage cp1252→utf8, réparé une fois).
- Un test qui échoue « hors périmètre » : `git stash` du diff + relance de la classe = preuve qu'il
  préexiste sur `main`, avant de chercher la cause.
- Un script de workflow se vérifie hors ligne : extraire le heredoc `<<'REMOTE'`, `bash -n`, puis
  simuler avec un faux binaire `docker` sous `set -euo pipefail`.

---

## ❌ What Failed

- **Heredoc bash + accents / `\\n`** → passer par un fichier `.py`.
- **`git add -A`** → fichiers IDE sur `main` → ajouter les chemins explicitement.
- **`ComActionMessageView` comme source** → 6,6 s la page ; absente de `yesRemsTest`.
- **Sous-requête dans un `GROUP BY`** → SQL Server erreur 144 → `@JoinFormula` ou table dérivée.
- Dates `'2026-09-16'` en T-SQL → format `'20260916'`.
- **Image `mssql/server:2022` : `mssql` = `uid=10001 gid=10001`**, PAS dans le groupe root → dossier
  ou fichier en `nahoma:0` / `700` invisible (« Access is denied » sur `master.mdf` ou un `.bak`).
  Réglage retenu : `chown -R nahoma:10001`, `chmod 2770` (setgid → les `scp` héritent du groupe),
  `660` sur les `.bak`.
- `docker compose restart` ne relit pas le `.env` → `up -d` ; `SA_PASSWORD` n'est appliqué qu'à la
  **première** initialisation du volume ; `ALTER LOGIN sa` sans reporter le `.env` → healthcheck
  `unhealthy`.
- `!` dans un mot de passe en ligne de commande bash → expansion d'historique (`set +H`).
- **Horloges qui divergent d'1 h sur le poste** : Windows « Morocco Standard Time » = **+00:00** (et
  `GETDATE()` suit), JVM `Africa/Casablanca` = **+01:00**. Tout test qui écrit `LocalDateTime.now()`
  puis compare à `GETDATE()` en jours échoue **entre 23 h et minuit heure Windows**. Parade :
  fixtures datées d'hier quand une vue calcule un `DATEDIFF`.
- **`docker compose up` lancé à la main pendant un run du pipeline** → conflit de nom
  `<id>_immo-back` (compose renomme l'ancien conteneur avant de créer le nouveau). Ne jamais
  déployer à la main pendant qu'un run tourne.
- `meta.outputs.version` de `docker/metadata-action` = tag de plus haute priorité, pas le SHA.
- Centralisation des deux monitorings (Grafana unique sur `vps_ayad`, `vps_tsiory` poussant par
  Alloy `remote_write` + Loki push derrière Caddy) : conçue, **écartée par l'utilisateur** —
  « ça reste séparé pour le moment ». Les dashboards du dépôt n'ont aucune variable `serveur`, il
  faudrait la leur ajouter avant toute centralisation.
- `ssh vps_ayad` en `BatchMode` depuis ce poste → `Permission denied (publickey)` ; `ssh vps-db`
  (tsiory, user `prod`) fonctionne en lecture.

---

## 🐛 Bugs & Errors Log

| # | Erreur / Bug | Statut | Correctif |
|---|---|---|---|
| 29 | Pipeline n8n à l'arrêt depuis le 08/09 | **Open** (externe) | import CSV possible |
| 30 | Heure de RDV non captée (front `type="date"`) | **Open** (front) | backend prêt |
| 33 | 13 actions sans auteur (18→22/09) | **Fixed** pour les nouvelles ; les 13 restent | script de rattrapage proposé, non fait |
| 35 | Fichiers Qodana committés par erreur | **Fixed** (PR #74) | suppression |
| 36 | SQL Server 2nd VPS en boucle (`Access is denied` sur `master.mdf`) | **Fixed** (à la main) | conteneur orphelin supprimé, volume recréé |
| 37 | `RESTORE` : `Cannot open backup device` (`.bak` en `700`) | **Fixed** (à la main) | `nahoma:10001`, `2770`/`660` |
| 38 | `vps_tsiory` : `Conflict … "/770947ff125a_immo-back" already in use` | **Fixed** (PR #76 + #78, run 9f109b8 vert) | nettoyage des orphelins |
| 39 | `IMMO_BACK_TAG=latest` écrit à chaque déploiement | **Fixed** (PR #76) | tag `sha-…` calculé |
| 40 | `sqlserver` 2nd VPS `unhealthy` après `ALTER LOGIN sa` | **Fixed** (à la main) | `.env` aligné + `up -d` |
| 41 | `ProspectionFiltresAvancesTest` : 2 échecs entre 23 h et minuit (écart JVM/SQL Server) | **Fixed** (PR #76), préexistant sur `main` | fixture `now().minusDays(1)` |
| 42 | `vps_ayad`, run `sha-eb6bed4` : conflit `eb3b00a85e4d_immo-back` apparu PENDANT le `up` | **Fix mergé** (PR #78) — **non revérifié sur `vps_ayad`** | second essai après échec |
| 43 | CORS bloqué depuis `arpent.ayaddeveloppement.com` | **Cause trouvée** : le correctif (#76) n'a jamais démarré sur `vps_ayad` (run #42 en échec) ; `~/arpent/.env` n'avait **aucune** ligne `IMMO_BACK_TAG` — l'utilisateur l'a ajoutée à la main | à vérifier : `docker inspect immo-back` = `sha-9f109b8` et preflight `OPTIONS` en 200 |

---

## 🔑 Key Points to Remember

- **Pré-prospection = `StrEntityId = B92DF3D8-…`** ; prospection vente = `E7C73F70-…` ; catégorie
  d'action Prospection = `69F8E7D6-1111-1111-1111-1AA8B1F02008`.
- **Auteur d'une action** : `UserName` ; « Système » = à personne ; `admin` porté par 2 comptes.
- `/api/suivi-actions` et `/api/kpi` hors `PerimetreInterceptor` : périmètre dans le service ; toute
  route sans id sous une collection scopée → `PerimetreInterceptorInvarianteTest.ROUTES_SANS_ID_AUTORISEES`.
- **Deux VPS, deux Environments GitHub** (`vps_tsiory`, `vps_ayad`), chacun ses 4 secrets. Les
  `VPS_*` au niveau **dépôt** existent encore → un secret manquant dans un Environment y retombe
  **en silence** ; à supprimer une fois les deux cibles vertes.
- Sur `vps_ayad`, mystère non résolu : le run `sha-eb6bed4` a écrit `IMMO_BACK_TAG` quelque part,
  mais pas dans `~/arpent/.env`. L'utilisateur affirme que `VPS_PATH` vise bien `~/arpent`. Si le
  conflit revient : `sudo grep -rl '^IMMO_BACK_TAG=' /home /root /opt` pour trouver l'autre projet.
- Rollback aujourd'hui = Actions → ancien run → « Re-run all jobs » (rebuild puis redéploie les
  deux). Amélioration proposée, **non commencée** : `workflow_dispatch` avec entrées `tag` +
  `cible`, build sauté si tag fourni, et **`pull` avant l'écriture du `.env`** (aujourd'hui un tag
  inexistant pollue le `.env`).
- Mode de travail : backend seulement ; git via `node <prototype>/.claude/scripts/git-flow.mjs`
  (**chemin absolu**, le cwd peut avoir glissé dans `monitoring/`) ; **demander avant toute branche**.

---

## 📁 Important Files & Paths

- `.github/workflows/deploy.yml` — build une fois (tag `sha-…` calculé), matrice 2 VPS, script
  distant : `.env` → `pull` → `nettoyer_orphelins` → `up` (+ 1 second essai) → sonde `:9091`.
- `src/main/java/com/tsix/immocrm/web/CorsConfig.java` + `CorsConfigTest` — origines de prod.
- `src/test/.../ProspectionFiltresAvancesTest.java` — fixture datée d'hier (commentaire = bug #41).
- `monitoring/{README.md,grafana-vue-generale.json,grafana-sync-pipeline.json}` — seuls fichiers
  monitoring du dépôt ; `prometheus.yml`, `config.alloy`, `loki-config.yml`, provisioning Grafana
  vivent **uniquement sur les VPS** (`/home/prod/app/monitoring` et `~/arpent/monitoring`).
- `instructions/FEATURE-creation-pre-prospection.md`, `FEATURE-suivi-actions.md` — specs du 23/09.

---

## 🔧 Environment & Config

- SQL Server local `localhost,49687` (compte `tsix`, identifiants dans `CLAUDE.md`) ; bases
  `yesRems`, `yesRemsProd` (mesures), `yesRemsTest` (tests). Windows en « Morocco Standard Time »
  affiché **+00:00** ; JVM en +01:00 (voir bug #41).
- Tests : `./mvnw.cmd test` — suite complète verte hors la fenêtre 23 h–minuit avant le fix #41
  (dernier passage complet : 1 seule classe rouge, corrigée et revérifiée dans la fenêtre).
- `vps_tsiory` = `api.tsiory.tech`, user `prod`, `/home/prod/app`, Caddy système, n8n dans
  `/home/prod/n8n`. `vps_ayad` = `vps.ayaddeveloppement.com`, user `nahoma`, `~/arpent`.
- Tunnel SQL : `ssh -N -L 1434:127.0.0.1:1433 vps_ayad` (1433 est pris par le tunnel `vps-db`).
- Scratchpad de session pour scripts Python, messages de commit (`git commit -F`) et simulations.

---

## ⏭️ Next Step

> Le dépôt est encore sur la branche locale `ci/nettoyage-conteneur-immo-back` (mergée, PR #78),
> `main` local en retard de 4 commits — l'utilisateur a interrompu le `sync`. Reprendre par
> `node <prototype>/.claude/scripts/git-flow.mjs sync back` **depuis la racine du dépôt**, puis
> vérifier `vps_ayad` : `docker inspect immo-back --format '{{.Config.Image}}'` doit donner
> `sha-9f109b8`, et le preflight `OPTIONS /api/auth/connexion` avec
> `Origin: https://arpent.ayaddeveloppement.com` doit répondre 200 avec
> `Access-Control-Allow-Origin` (bug #43). Si le `curl` local passe mais pas le navigateur → config
> Caddy de `api.ayaddeveloppement.com`.

### Backlog
- [ ] Supprimer les 4 `VPS_*` au niveau dépôt GitHub une fois `vps_ayad` vert sur un run.
- [ ] Rollback par bouton : `workflow_dispatch` (`tag`, `cible`), build sauté si tag, `pull` avant
      `.env` — branche `ci/rollback-par-tag`, à demander.
- [ ] `vps_ayad` : `.env` complet (`JWT_SECRET` différent, `APP_JWT_COOKIE_SECURE=true`,
      `MAIL_ENABLED`/`WP_ENABLED=false` si les deux serveurs partagent une base).
- [ ] Script SQL (appliqué par l'utilisateur) pour rattacher les 13 actions anonymes du 18→22/09.
- [ ] Synchroniser les comptes (`AdmUser`/`AppUserCredential`) vers la source avant le passage
      deux-bases en prod ; 1er déploiement deux-bases : ne PAS pré-remplir `AppSyncEtat` (bug #27).
- [ ] Appliquer `010_AppTraceOmran.sql` sur `yesRemsDev` ; audit sur le 115 avant `AUDIT_TRACE_PATH`.
- [ ] Moniteur n8n (jauge + alerte 24 h) ; credential SQL n8n avant fermeture de la redirection 115.
- [ ] Adresses projets (APO, Villa Lazrak, adresses « siège »).
- [ ] Importer `monitoring/grafana-vue-generale.json` dans le Grafana de chaque VPS (séparés).

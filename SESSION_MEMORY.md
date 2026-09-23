# Session Memory
_Last updated: 2026-09-24T02:15+01:00_
_Project: immo-crm (AyadERP) — C:\Users\tsiory\Documents\ayad-internship\prototype\immo-crm_

---

## 🎯 Current Objective

**Aligner le CRM sur les écrans OMRAN à partir de la trace d'audit** : session du 23/09 —
création + import CSV de pré-prospections, discriminant `StrEntity`, écran « Suivi des actions »
(colonnes, sélecteur, groupement, accès admin/soi), KPI sur toutes les actions, correctif
d'auteur ; déploiement vers un second VPS en attente.

---

## ✅ What Worked

### Livré et MERGÉ
1. **PR #72 `feat/creation-pre-prospection`** (012c40d + 38834af) :
   - `POST /api/pre-prospections` : rejoue OMRAN (trace `26/05948`) — `AdmSequence` « Prospection »
     (`AA/NNNNN`), `StrEntityId = ComPreProspection` (B92DF3D8…), contact en `Temp*`, seul `nom`
     requis, vocation VENTE par défaut. Doublons (nom + tél 9 chiffres + email) **signalés, jamais
     refusés** (décision utilisateur). Réponse `{ preProspection, doublons }`.
   - **Décision #44 appliquée** : pré-prospection = `StrEntity.Code = 'ComPreProspection'`
     (`DiscriminantPreProspection`), partout (listes, groupes, dashboard, relances). **847** lignes
     réelles passent de `/prospections` à `/pre-prospections`. `transformer` bascule le type en
     place (trace `26/05947`).
   - Import CSV (export Meta Leads Center) : `POST /api/pre-prospections/import/apercu` (lit tout,
     n'écrit rien, propose la tranche du 1er mot du formulaire) puis `/import` (+ `trancheId`
     obligatoire, **1 fichier = 1 tranche**). Statuts NOUVELLE / DOUBLON / DEJA_IMPORTEE (même
     contact + même date à la minute → réimport sans effet) / ERREUR. Défauts = motif n8n
     (`1 A TRAITER`, FACEBOOK, WEB). Lecteur RFC 4180 maison (`LecteurCsvLeads`, `,`/`;`, BOM, cp1252).
   - Fuite de périmètre corrigée : un doublon hors périmètre n'est jamais nommé
     (`ProspectionService#visibles`).
2. **PR #73 `feat/suivi-actions`** (d6c5e8c, 86d29c1, 5d82018) :
   - `GET /api/suivi-actions[/groupes]` + catalogue `GET /api/referentiels/colonnes/suivi-actions`
     (`ColonneSuiviAction`) : écran OMRAN (trace : `ComActionMessageView WHERE CfgTrancheId IN …
     AND DocDate >= début AND DocDate < fin [AND UserName = …]`). Construit sur `ComActionMessage`
     (vue = 6,6 s/page) ; tranche résolue en `@JoinFormula` (`trancheResolue`) ; rang / alerte
     active / état / personnel calculés pour la page. **Parité : 5 623 groupes, 48 213 actions,
     0 écart** avec la vue. Appels par jour : `groupBy=dateAction&granularite=JOUR`.
   - Accès : `ROLE_Administrateur` voit tout ; autres = **leurs actions** (`UserName` = mon nom OU
     `AdmUserId` = moi hors « Système »).
   - Correctif auteur : `UtilisateurAffiche.courant()` (compte en base d'écriture → description lue
     sur la copie via `jdbcLecture` → username du token) ; `AdmUserId` seulement si le compte existe.
   - KPI (`/api/kpi/commerciaux[/moi]`) sur **toutes les actions** : `actionsParType`,
     `interactionsTotal` hors « Système », `tauxActionVersRdv`, attribution comme le suivi, compte
     sans employé = sa ligne (`utilisateurId`), `/moi` 200 au lieu de 404. Projection ligne à ligne
     (`projeterActionsPourKpi`) agrégée en Java.

3. **PR #74** (`48836a9`) : retrait de `qodana.yaml` et du workflow Qodana embarqués par erreur.

4. **PR #75 `ci/deploiement-second-vps`** (29acecd) :
   - `deploy.yml` : job `deploy` en **matrice** `cible: [vps_tsiory, vps_ayad]`, `environment:
  ${{ matrix.cible }}`, `fail-fast: false`. Build une fois, déploiement parallèle, mêmes 4 secrets
  (`VPS_HOST/USER/SSH_KEY/PATH`) lus dans l'Environment du nom de la cible. `TOKEN` reste au dépôt.
   - **1er run** : `vps_ayad` vert (sonde 200 en 37 s) ; `vps_tsiory` rouge — conteneur orphelin
     `770947ff125a_immo-back` d'un recreate interrompu (à supprimer à la main, cf. bug #38).
   - **Second VPS préparé à la main** (`vps.ayaddeveloppement.com`, user `nahoma`, compose dans
  `~/arpent`) : SQL Server 2022 Express en conteneur, MinIO, stack Grafana/Loki/Prometheus.
     Volume `arpent_mssql_data` recréé propre (un conteneur `compose run` orphelin le tenait avec
     des fichiers aux mauvais droits) ; `.bak` restaurés depuis `~/arpent/myssql-backups` monté en
     `/var/backups` ; `docker login ghcr.io` fait pour `nahoma`.

5. **PR #76 `fix/cors-ayaddeveloppement`** (7a70370) — mergée, run `sha-eb6bed4` : `vps_tsiory`
   vert (tag SHA + nettoyage OK), `vps_ayad` rouge (voir bug #42).
   - `CorsConfig` : origine exacte `https://arpent.ayaddeveloppement.com` (nom donné par
  l'utilisateur ; pas de joker, convention « chaque origine de prod listée »). Même domaine
  enregistrable que l'API du 2nd VPS → SameSite=Strict tient. 4 tests ajoutés (`CorsConfigTest`,
  8 verts : origine acceptée, autre sous-domaine refusé, http refusé, domaine sosie refusé).
   - `deploy.yml` : tag immuable calculé (`sha-${GITHUB_SHA::7}`) au lieu de `meta.outputs.version`
     qui renvoyait `latest` (priorité raw 200 > sha 100 — **tous les déploiements passés tournaient
     sur `latest`**) ; `docker rm -f` des `*_immo-back` orphelins avant `up`.
   - `ProspectionFiltresAvancesTest` rendu indépendant de l'heure (bug #41).

### En cours — branche `ci/nettoyage-conteneur-immo-back` (24/09)
- `deploy.yml` : fonction `nettoyer_orphelins` (noms via `docker ps -a --format` + grep
  `^[0-9a-f]{12}_immo-back$`, plus `--filter name=`), appelée avant `up` **et** une seconde fois
  après un `up` en échec, suivi d'un unique nouvel essai. Motif : sur `vps_ayad` l'orphelin est
  apparu PENDANT le `up` (aucun listé au nettoyage préalable) — signature d'un `docker compose up`
  manuel concurrent du pipeline.

### Méthode
- **Trace d'audit** : `sys.fn_get_audit_file('C:\backup\*.sqlaudit', …)` depuis `master`, filtrer
  `object_name`, reconstituer par session ; l'utilisateur rejoue l'écran à la demande.
- Mesurer avant de concevoir (population, parité vue ↔ requête) — a évité de mapper la vue lente.
- Éditions multi-lignes accentuées : **script Python dans le scratchpad** (`python -X utf8 fichier.py`),
  jamais heredoc bash (accents cassés, `\` de fin de ligne avalés).

---

## ❌ What Failed

- **Heredoc bash + accents / `\\n`** → assertions en échec ou `\n` littéraux devenus vrais sauts de
  ligne dans des chaînes Java → passer par un fichier `.py`.
- **`git add -A`** → a committé des fichiers IDE (Qodana) jusque sur `main` → ajouter les chemins
  explicitement (mémoire `jamais-git-add-A-sans-relire`).
- **`ComActionMessageView` comme source** → 6,6 s la page, 9,3 s le comptage, absente de
  `yesRemsTest`.
- **Sous-requête dans un `GROUP BY`** → SQL Server erreur 144 → la mettre dans la condition de
  jointure (`@JoinFormula`) ou une table dérivée.
- Dates `'2026-09-16'` en T-SQL → hors limites selon la langue serveur → format `'20260916'`.
- **Image `mssql/server:2022` : l'utilisateur `mssql` est `uid=10001 gid=10001`**, PAS dans le
  groupe root → un dossier/fichier en `nahoma:0` ou `700` lui est invisible (« Access is denied »
  sur `master.mdf` ou sur un `.bak`). Réglage retenu : `chown -R nahoma:10001`, `chmod 2770` sur le
  dossier (setgid → les `scp` héritent du groupe), `660` sur les `.bak`.
- `docker compose restart` ne relit pas le `.env` → `up -d` ; et `SA_PASSWORD` n'est appliqué qu'à
  la **première** initialisation du volume.
- `!` dans un mot de passe passé en ligne de commande bash → expansion d'historique (`set +H`).
- **Horloges qui divergent d'1 h sur le poste** : Windows « Morocco Standard Time » = **+00:00**
  (et `GETDATE()` de SQL Server suit Windows), la JVM résout la même zone en `Africa/Casablanca`
  = **+01:00**. Tout test qui écrit `LocalDateTime.now()` puis compare à `GETDATE()` en jours
  (`DATEDIFF`) échoue **entre 23 h et minuit heure Windows**. Vu sur `ProspectionFiltresAvancesTest`
  (`inactiviteMin=0` → vue `InactivityTimeout = -1`). Parade : fixtures datées d'hier, jamais
  « maintenant » quand une vue calcule un écart en jours.

---

## 🐛 Bugs & Errors Log

| # | Erreur / Bug | Statut | Correctif |
|---|---|---|---|
| 29 | Pipeline n8n à l'arrêt depuis le 08/09 | **Open** (externe) | leads saisis à la main / import CSV désormais possible |
| 30 | Heure de RDV non captée (front `type="date"`) | **Open** (front) | backend prêt |
| 31 | 847 pré-prospections OMRAN classées en prospections (heuristique) | **Fixed** (#72) | discriminant `StrEntity` |
| 32 | Doublons hors périmètre nommés à la création | **Fixed** (#72) | `ProspectionService#visibles` |
| 33 | 13 actions sans auteur (18→22/09), invisibles dans le suivi OMRAN | **Fixed** pour les nouvelles (#73) ; les 13 existantes restent | `UtilisateurAffiche.courant()` ; script de rattrapage proposé, non fait |
| 34 | KPI gonflé par les actions « Système » (212/228 Hamza) | **Fixed** (#73) | exclusion `UserName = 'Système'` |
| 35 | Fichiers Qodana committés par erreur | **Fixed** (PR #74) | suppression |
| 36 | SQL Server du 2nd VPS en boucle (`Access is denied` sur `master.mdf`) | **Fixed** (à la main) | conteneur orphelin supprimé, volume recréé |
| 37 | `RESTORE` : `Cannot open backup device` (`.bak` en `700 nahoma:nahoma`) | **Fixed** (à la main) | `nahoma:10001`, `2770`/`660` |
| 38 | `vps_tsiory` : `Conflict. The container name "/770947ff125a_immo-back" is already in use` | **Open** côté serveur (`docker rm -f 42eebfb0a7f3` à faire) ; prévention dans `deploy.yml` (branche fix) | nettoyage des orphelins avant `up` |
| 39 | `IMMO_BACK_TAG=latest` écrit à chaque déploiement (rollback par tag impossible) | **Fix sur branche** `fix/cors-ayaddeveloppement` | étape `Calculer le tag immuable` |
| 40 | `sqlserver` du 2nd VPS `unhealthy` après `ALTER LOGIN sa` sans reporter le `.env` | **Fixed** (à la main) | `.env` aligné + `up -d` |
| 41 | `ProspectionFiltresAvancesTest` : 2 échecs (`expected:<1> but was:<0>`) entre 23 h et minuit — écart d'1 h JVM (+01:00) / SQL Server (+00:00) | **Fixed** (PR #76) — pré-existant sur `main`, vérifié par `git stash` | fixture `DocDate = now().minusDays(1)` |
| 42 | `vps_ayad`, run `sha-eb6bed4` : `Conflict … "/eb3b00a85e4d_immo-back" already in use` alors que le nettoyage préalable n'a rien trouvé | **Fix sur branche** `ci/nettoyage-conteneur-immo-back` | nettoyage grep + second essai après échec |

---

## 🔑 Key Points to Remember

- **Pré-prospection = `StrEntityId = B92DF3D8-…` (`ComPreProspection`)** ; prospection vente =
  `E7C73F70-…`. Catégorie d'action Prospection = `69F8E7D6-1111-1111-1111-1AA8B1F02008`.
- **Auteur d'une action** : `UserName` (99,97 %) ; « Système » = actions générées pendant une session
  (~1 330 liées à un compte, notes surtout) — à personne. `admin` est porté par 2 comptes → jamais
  résolu par nom.
- **Comptes de connexion du banc** (`t.admin`, `Khadija`) n'existent que dans la copie
  `yesRemsProd`, pas dans la source `yesRems` → même piège en prod au passage deux-bases.
- `/api/suivi-actions` et `/api/kpi` ne sont pas gardés par `PerimetreInterceptor` : périmètre
  appliqué dans le service (`filtreInteractions`). Toute route sans id sous une collection scopée
  doit figurer dans `PerimetreInterceptorInvarianteTest.ROUTES_SANS_ID_AUTORISEES`.
- Mode de travail : backend seulement ; git via `node ../.claude/scripts/git-flow.mjs` ; **demander
  avant toute branche** ; l'utilisateur a accepté de mettre plusieurs lots sur une même branche.

---

## 📁 Important Files & Paths

- `instructions/FEATURE-creation-pre-prospection.md` (§1–5, Impact front) et
  `instructions/FEATURE-suivi-actions.md` (§1–7, Impact front) — specs du 23/09.
- `services/prospection/{DiscriminantPreProspection,ImportPreProspectionService,LecteurCsvLeads}.java`
- `services/suivi/SuiviActionService.java`, `controller/suivi/SuiviActionController.java`,
  `domain/ColonneSuiviAction.java`, `dto/suivi/SuiviActionDto.java`
- `entities/com/ComActionMessage.java` — `trancheResolue` (`@JoinFormula`).
- `security/UtilisateurAffiche.java` — résolution d'auteur (utilisée par Prospection/DossierService).
- `services/kpi/PerformanceCommercialService.java`, `dto/kpi/{PerformanceCommercialDto,ActionParTypeDto}.java`
- `.github/workflows/deploy.yml` — CD actuel (build GHCR → SSH → `docker compose up -d immo-back`
  → sonde `127.0.0.1:9091/actuator/health`).

---

## 🔧 Environment & Config

- SQL Server local `localhost,49687` (compte `tsix`, identifiants dans `CLAUDE.md`) ; bases
  `yesRems` (source du banc), `yesRemsProd` (copie, mesures), `yesRemsTest` (tests).
- Tests : `./mvnw.cmd test` — **965 verts** au dernier passage (~4 min).
- Scratchpad de session pour scripts Python et messages de commit (`git commit -F`).

---

## ⏭️ Next Step

> PR `ci/nettoyage-conteneur-immo-back` poussée : l'utilisateur la merge, puis `git-flow sync
> back`. Le merge redéploie les deux VPS ; vérifier `vps_ayad` (bug #42). Ne PAS lancer de
> `docker compose up` à la main pendant qu'un run tourne. Ensuite : `IMMO_BACK_TAG=sha-…` dans
> les deux `.env`, puis supprimer les `VPS_*` au niveau dépôt GitHub.

### Backlog
- [ ] `vps_ayad` : `docker login ghcr.io` (PAT `read:packages`), `.env` complet (`JWT_SECRET`
      différent, `APP_JWT_COOKIE_SECURE=true`, `MAIL_ENABLED`/`WP_ENABLED` à `false` si les deux
      serveurs partagent une base), clé publique du runner dans `authorized_keys`.
- [ ] Script SQL (appliqué par l'utilisateur) pour rattacher les 13 actions anonymes du 18→22/09.
- [ ] Synchroniser les comptes (`AdmUser`/`AppUserCredential`) vers la source avant le passage
      deux-bases en prod.
- [ ] Appliquer `010_AppTraceOmran.sql` sur `yesRemsDev` ; audit sur le 115 avant `AUDIT_TRACE_PATH`.
- [ ] 1er déploiement deux-bases : ne PAS pré-remplir `AppSyncEtat` (bug #27).
- [ ] Moniteur n8n (jauge + alerte 24 h) ; credential SQL n8n avant fermeture de la redirection 115.
- [ ] Adresses projets (APO, Villa Lazrak, adresses « siège »).
- [ ] Importer `monitoring/grafana-vue-generale.json` dans le Grafana du VPS.

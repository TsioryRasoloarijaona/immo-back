# Session Memory
_Last updated: 2026-09-25T17:30+01:00_
_Project: immo-crm (AyadERP) — C:\Users\tsiory\Documents\ayad-internship\prototype\immo-crm_

---

## 🎯 Current Objective

Mettre n8n (et plus tard un serveur MCP) en accès API authentifié, et fiabiliser le montage
**deux-bases** en production sur les deux VPS (`vps_tsiory` = dev, `vpd-ayad` = client) : source
de vérité unique `YesRems` sur `192.168.11.115` (= `197.230.172.136`), copies `yesRemsDev` sur
chaque VPS. Chantier prospection (parité OMRAN) en pause.

---

## ✅ What Worked

### Livré et mergé ce 25/09
- **PR #83 `feat/doublons-prospect-projet`** : `GET /api/prospects/doublons`, 409 `doublons` sur
  `POST /api/prospects` / `creer-prospect` sauf `ignorerDoublons`, `POST /api/pre-prospections/{id}/lier-prospect`,
  règle « une prospection ouverte par prospect × projet » (409 `prospectionExistante`). Spec
  `FEATURE-doublons-prospect-projet.md`.
- **PR #84 `feat/cle-api-n8n`** : clés d'API `X-Api-Key` (table `AppApiClient`, script 011),
  `ApiKeyAuthenticationFilter` avant le JWT (401 `CLE_INVALIDE`, 403 `HORS_PORTEE`), portées
  `PorteeCleApi` (PRE_PROSPECTIONS_ECRIRE / _LIRE, REFERENTIELS_LIRE), `/api/cles-api`
  (Administrateur), schéma OpenAPI `cleApi`. Spec `FEATURE-cle-api-n8n.md` (§6 = MCP, pas fait).
- **PR #85** (même branche) : matrice deploy → `vpd-ayad` (l'Environment qui porte vraiment les
  secrets du VPS client) ; version embarquée (`build-args` IMAGE_TAG/GIT_SHA → `APP_BUILD_*` →
  `config/InfosBuild` : jauge `immo_back_build_info{tag,commit}` + `/actuator/info`) ; panneau
  Grafana « Version déployée » dans `monitoring/grafana-vue-generale.json`.
- **Non mergé** : commit `8751092` sur `feat/cle-api-n8n` = `CleApiAdminCookieTest` (création de
  clé par vrai cookie → 201 ; Commercial → 403). Branche poussée, PR à ouvrir/merger.

### Infrastructure et bases (hors dépôt, faits ce 25/09)
- **Pipeline** : les Environments `vps_tsiory`/`vps_ayad` n'avaient **aucun secret** → les deux
  jobs déployaient le VPS de dev. Corrigé : matrice `vpd-ayad`, secrets posés dans `vps_tsiory`,
  clé SSH `actions_deploy_vps_tsiory` autorisée sur le VPS de dev, secrets `VPS_*` de dépôt
  supprimés. Les deux VPS servent `sha-6dff8af` (vérifié `/actuator/info` et `/api-docs`).
- **Source 115 équipée** (plan approuvé) : tables `App*` créées (001, 002, 003, 006, 011),
  `t.admin` + rôle insérés, 3 credentials + 3 138 `AppFileObject` recopiés (bcp natif, checksum
  -1167713681 égal aux copies), **CT activé** sur 4 tables App* + `AdmUser`/`AdmUserRole`/
  `AdmUserHabilitation` → 27 tables suivies, 27 filigranes dans `AppSyncEtat` des deux copies.
- **Clé n8n créée** sur le VPS client (`AppApiClient` = 1 ligne sur source + 2 copies).
- Ligne `ComActionMessage` de Yassine (21/09) ignorée par le CT : « touchée » sur la source
  (`UPDATE … SET UserUpdateDateTime = UserUpdateDateTime`) → fusionnée dans les 2 copies en < 1 min.
  `ComActionMessage` = 48 307 partout.

### Méthode
- Un **401 nu (corps vide) sur une écriture** = exception serveur masquée par le dispatch `/error`
  (tables absentes sur la source, FK…) → lire `docker compose logs immo-back` avant le cookie.
  Un 401 avec `ProblemDetail` = vrai refus d'identité.
- Routage deux-bases : `readOnly` → copie ; écriture ou hors transaction → source. Login et
  validation de clé lisent la **copie** ; création/révocation de clé écrit la **source**.
- Comparer les copies par `CHECKSUM_AGG(CHECKSUM(…))` **avec les bonnes colonnes** (AppFileObject
  n'a pas de PKey : `AdmAttachedFileId`). Comparer aussi les **hashs** de mot de passe.
- bcp 15 : pas d'option `-u` ; `queryout` avec liste de colonnes explicite, `in -N -k`.
- `CHANGETABLE(...)` exige une **constante** de version et un **alias** ; un `-b` avec erreur de
  syntaxe annule tout le lot (l'UPDATE compris).
- MockMvc partage le contexte de sécurité entre requêtes d'un test ; `addFilterBefore(x, Jwt…)`
  après l'enregistrement du filtre JWT ; heredocs bash longs → script `.py` en scratchpad.

---

## ❌ What Failed

- **Rattrapage des mots de passe** : les credentials ont été recopiés depuis la copie **dev** ; le
  premier cycle a écrasé ceux de la copie **client**. Si les mots de passe différaient, ceux du
  client sont désormais ceux du dev. Non vérifié avant (comparaison de hashs ratée, non refaite).
- Premier cycle après CT : `AdmUserRole`/`AdmUserHabilitation` fusionnés avant `AdmUser` → FK,
  rejoués au cycle suivant (ordre `ORDRE_CLES_ETRANGERES` à compléter).
- Postman : un onglet avec Authorization configurée ajoutait un en-tête parasite → 401.
- cAdvisor sur les VPS n'attache ni `name` ni `image` : inutilisable pour la version.
- Import cURL n8n : refuse le multi-ligne ; une seule ligne ou saisie manuelle.

---

## 🐛 Bugs & Errors Log

| # | Erreur / Bug | Statut | Correctif |
|---|---|---|---|
| 29 | Pipeline n8n à l'arrêt depuis le 08/09 | **En cours** : flux HTTP + clé d'API en construction | |
| 30 | Heure de RDV non captée (front) | Open (front) | |
| 43 | CORS `arpent.ayaddeveloppement.com` | **Fixed** (le VPS client est enfin déployé) | |
| 46 | `26/05941` en double dans `yesRemsProd` | Open (hors périmètre) | compteur `AdmSequence` |
| 47 | 12 060 pré-prospections n8n sans auteur | Fix livré (clé d'API) | |
| 48 | Job `vps_ayad` déployait le VPS de dev (Environment vide, repli sur secrets de dépôt) | **Fixed** | matrice `vpd-ayad`, secrets par Environment, `VPS_*` dépôt supprimés |
| 49 | Source 115 sans tables `App*` → toute écriture App* en 401 nu | **Fixed** (25/09) | tables + données + CT |
| 50 | Écritures App* échouées masquées en **401 nu** (dispatch ERROR) | **Open** (code) | `dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()` — branche à demander |
| 51 | Mots de passe client possiblement remplacés par ceux du dev | **Open** (à confirmer par l'utilisateur) | reset via `POST /api/utilisateurs/{id}/reinitialiser-mot-de-passe` |
| 52 | `nahoma` en double dans les copies (`nahoma` API 03/08 + `Nahoma ` OMRAN 18/09) → `findByCode` renvoie 2 lignes → 500/401 | **Open** | désactiver/renommer l'un des deux, dans OMRAN |
| 53 | 91 `AdmUserHabilitation` + 1 `AdmUserRole` présents seulement dans les copies (h.ennagim 57, Mkamara 17, Khadija 17) | **Open** | pousser vers la source (même méthode bcp) |
| 54 | Ordre de fusion : `AdmUserRole`/`Habilitation` avant `AdmUser` | Open (transitoire) | ajouter `AdmUser` en tête de `ORDRE_CLES_ETRANGERES` |
| 55 | Mot de passe `sasa` et clé SSH privée transités dans le chat | **Open** | rotation `sasa` (`.env` des 2 VPS + IDE) ; clé dev déjà remplacée |

---

## 🔑 Key Points to Remember

- **Les deux VPS partagent désormais comptes et mots de passe** (source unique). Un reset vaut
  pour les deux au cycle suivant.
- **Toute nouvelle table `App*` écrite par JPA** doit être créée sur la source (script) **et**
  suivie par CT, sinon écriture en 401 nu. Ne jamais suivre `AppSyncEtat`/`AppTraceOmran`.
- Ligne présente sur la source mais absente des copies : la « toucher » sur la source suffit.
- `VPS_PATH` de `vps_tsiory` = `/home/prod/app` (le `.env` complet) ; celui de `vpd-ayad` doit
  être `/home/nahoma/arpent`. `/home/prod/app/backend/` est un reliquat à supprimer.
- Version déployée : `docker inspect immo-back --format '{{.Config.Image}}'`, label OCI
  `org.opencontainers.image.revision`, `curl 127.0.0.1:9091/actuator/info`, GitHub → Deployments.
- Clé d'API : `X-Api-Key`, portée fermée ; `/api/cles-api` jamais dans une portée. Une clé par
  serveur (base par serveur). Rôle requis pour l'admin des clés : `Administrateur` (t.admin l'a).
- Pré-prospection n8n : seul `nom` obligatoire ; ids de référentiel fixes par campagne, lus une
  fois (`/api/referentiels/origines-prospection`, `etapes-prospection`, `types-contact-prospection`,
  `/api/referentiels/tranches`).
- Mode de travail : backend seulement ; git via `node <prototype>/.claude/scripts/git-flow.mjs`
  (chemin absolu) ; demander avant toute branche ; commit + push dès que vert ; attendre le merge.

---

## 📁 Important Files & Paths

- `instructions/FEATURE-cle-api-n8n.md`, `FEATURE-doublons-prospect-projet.md`,
  `FEATURE-source-de-verite-115.md` (§ check-list 115 — **faite le 25/09**, à annoter).
- `security/ApiKeyAuthenticationFilter`, `security/CleApiService`, `domain/PorteeCleApi`,
  `services/auth/CleApiAdminService`, `controller/auth/CleApiController`, `entities/app/AppApiClient`.
- `config/InfosBuild.java` ; `Dockerfile` (ARG/ENV version) ; `.github/workflows/deploy.yml`
  (matrice `vps_tsiory`, `vpd-ayad`, build-args).
- `config/SynchronisationDescendante`, `RemonteeLecture`, `CollecteurLignesEcrites` — mécanique
  deux-bases (BASELINE sans DELETE, DELTA par CT, clé `PKey` uuid).
- `src/main/resources/db/migration/011_AppApiClient.sql` ; `ci/schema/010_tables.sql`, `020_foreign_keys.sql`.
- `monitoring/grafana-vue-generale.json` (panneau Version), `monitoring/README.md`.
- Test à merger : `src/test/java/com/tsix/immocrm/controller/auth/CleApiAdminCookieTest.java`.
- Plan exécuté : `C:\Users\tsiory\.claude\plans\composed-hugging-biscuit.md`.

---

## 🔧 Environment & Config

- Source 115 : `192.168.11.115` (LAN) = `197.230.172.136:1433`, base `YesRems`, compte `sasa`
  (mot de passe dans `.env` des VPS, `SOURCE_DB_PWD`) — CT ON, rétention 7 j, 27 tables.
- Copies : `yesRemsDev` sur chaque VPS ; tunnels SSH `vps-db` (→ `127.0.0.1:1433`, user SQL `dev`)
  et `vps-ayad-db` (→ `127.0.0.1:1434`, user SQL `prod`, même mot de passe que le dev).
- SSH : `vps-db` (prod@api.tsiory.tech) fonctionne en BatchMode ; `vps_ayad` (nahoma) refuse la
  clé en non-interactif → commandes à faire taper à l'utilisateur avec `!`.
- Local : SQL Server `localhost,49687`, `yesRemsProd` (banc, **pas** une source de données pour
  les VPS), `yesRemsTest` (tests, table `AppApiClient` créée). Tests : 1025 verts.
- Outils : `sqlcmd`, `bcp` 15.0 (pas de `-u`), `gh`, `python` (sans `yaml`).
- GitHub : Environments `vps_tsiory` (4 secrets), `vpd-ayad` (4 secrets), `vps_ayad` (vide, à
  supprimer) ; plus aucun `VPS_*` au niveau dépôt.

---

## ⏭️ Next Step

> **Faire confirmer par l'utilisateur la connexion `t.admin` sur le VPS client avec le mot de
> passe du dev** (bug 51). Si oui : réinitialiser les mots de passe voulus via
> `POST /api/utilisateurs/{id}/reinitialiser-mot-de-passe` (vaut pour les deux VPS). Puis pousser
> les 92 lignes d'habilitations/rôle vers la source (bug 53) et traiter le doublon `nahoma` (bug 52).

### Backlog
- [ ] Ouvrir/merger la PR du commit `8751092` (`CleApiAdminCookieTest`), puis `sync back`.
- [ ] Branche à demander : 401 nu → 500 propre (`DispatcherType.ERROR` permitAll) + `AdmUser`
      en tête de `ORDRE_CLES_ETRANGERES` + annoter `FEATURE-source-de-verite-115.md`.
- [ ] Rotation du mot de passe `sasa` ; supprimer la clé `github_deploy` privée du VPS client ;
      `PasswordAuthentication no` sur le VPS client ; supprimer l'Environment `vps_ayad` et
      `/home/prod/app/backend/`.
- [ ] n8n : finir le nœud HTTP (Header Auth `X-Api-Key`, body via Edit Fields), retirer
      l'identifiant SQL, créer la clé du VPS dev si besoin.
- [ ] MCP : `Authorization: Bearer <jwt>` + `POST /api/auth/jeton` (spec §6).
- [ ] Réimporter `grafana-vue-generale.json` dans les Grafana des deux VPS.
- [ ] Bug 46 (`26/05941` en double, `AdmSequence`) ; bug 30 (front).

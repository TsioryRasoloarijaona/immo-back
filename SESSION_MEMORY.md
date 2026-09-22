# Session Memory
_Last updated: 2026-09-21T21:15+01:00_
_Project: immo-crm (AyadERP) — C:\Users\tsiory\Documents\ayad-internship\prototype\immo-crm_

---

## 🎯 Current Objective

**Aligner l'app sur les règles de l'écran légataire OMRAN et fiabiliser/observer la synchro
descendante deux-bases** (celle de `source-de-verite-115`, désormais **mergée dans `main`**).
Cette session a livré 5 branches (prospection : tranche + champs obligatoires + contacts ;
synchro : moniteur Micrometer, fix StlSettlementLine, ouverture aux commerciaux).

---

## ✅ What Worked

### Méthode (répétée tout le tour)
- **La trace d'audit OMRAN `C:\backup\*.sqlaudit`** (via `sys.fn_get_audit_file`) est LA source de
  vérité sur ce qu'OMRAN écrit : `application_name = 'OMRAN estate'` isole ses requêtes. On lit
  l'INSERT/UPDATE (colonnes) + la ligne réelle créée pour mapper libellé → colonne.
- **Mesurer avant de décider** (taux de population) justifie chaque règle. Ex. tranche obligatoire :
  14 700/14 725 prospections qualifiées ont une tranche (99,83 %).
- **Vérifier la synchro via `AppSyncEtat` et le contenu des tables, JAMAIS le log** (bufferisé).

### Features livrées (toutes tests verts, 921 au dernier passage)
1. **Tranche obligatoire à `POST /api/prospections/{id}/transformer`** (`TransformerProspectionRequest`
   gagne `trancheId`/`articleId` ; invariant : une tranche à la sortie, sinon 400).
2. **5 champs obligatoires à `POST /api/prospections`** (alignés OMRAN, prospection `26/05946`) :
   `notes`, `origineId` (Source de contact→`ComProspectionOriginId`), `typeContactId`
   (Type→`ComActionMessageTypeId`), `etapeId` (Nature→`ComProspectionKindId`), `trancheId`
   (Tranche→`StkHierarchyId`). **Piège : « Type » = `ComActionMessageType`, PAS la vocation.**
3. **Contacts** : `CfgTierContact` (lien vers un autre `CfgTier` + `CfgRelationType`). OMRAN crée
   la personne à la volée → `ajouterContact` gagne `nouveauContact` (via `ProspectService.createEntity`).
   `ContactProspectDto` enrichi (mobile, email). Nouveaux `GET/POST /api/prospections/{id}/contacts`.
4. **Moniteur de pipeline (Micrometer)** : `MoniteurSynchronisation` expose `sync_*` sur
   `/actuator/prometheus:9091` (déjà câblé, 0 dépendance). Signal central `sync_watermark_lag{table}`.
5. **Fix StlSettlementLine** (voir bug #24) et **ouverture de la synchro manuelle aux commerciaux**.

### Change Tracking étendu + prouvé
- CT activé sur `ComProspectionProject` (autres tranches) + `City`/`Profile`/`SelectedProfile`/
  `ProfileCategory`. La synchro lit `sys.change_tracking_tables` **dynamiquement**
  (`SynchronisationDescendante.tablesSuivies()`) → **aucun code à changer**, activer le CT suffit.
- Propagation prouvée sur banc local : lignes d'autres-tranches écrites par OMRAN → arrivées dans
  la copie (INSERT/UPDATE/DELETE), convergence via `AppSyncEtat`.

---

## ❌ What Failed

- **Here-string PowerShell `@'...'@`** pour `git commit -m` : se découpe mal (blank lines →
  pathspec error). **Solution : `git commit -F <fichier>`** (message écrit dans le scratchpad).
- **Test contact avec `mobile`** → 400 : `numeroSupport.valider` exige un mobile au bon format
  (« 600000001 » refusé). Retiré le mobile du test (email suffit à prouver l'enrichissement).
- **Croire que « OMRAN n'atterrit pas »** = bug de code : non, c'est la synchro pas déployée /
  table hors CT. Toujours vérifier le périmètre CT et le mode deux-bases d'abord.

---

## 🐛 Bugs & Errors Log

| # | Erreur / Bug | Statut | Correctif |
|---|---|---|---|
| 24 | `StlSettlementLine` MERGE viole FK `BuyDocument` → table en échec **en boucle**, filigrane figé | **Fixed** (branche `fix/synchro-lignes-hors-perimetre`) | `RemonteeLecture.COLONNES_HORS_PERIMETRE` : exclut les lignes où `BuyDocumentId`/`RntDocumentId`/`StlDocumentId` non-null (achat/location, hors périmètre). Garde 665 lignes vente / 3227. |
| 25 | Autres tranches OMRAN non synchronisées | **Fixed** | `ComProspectionProject` n'avait pas de CT → activé sur la source. Aucun code. |
| — | Déploiement `deploy.yml` YAML error L83-84 | **Réglé par l'utilisateur** (hors périmètre de moi) |

---

## 🔑 Key Points to Remember

### Git / déploiement
- **Repo `immo-crm` = remote `TsioryRasoloarijaona/immo-back`.** Cette session a commité en git
  direct (pas git-flow) : `git checkout main && git pull` pour sync, `git checkout -b <branche>`
  depuis main, `git commit -F <fichier>`, `git push -u origin <branche>`.
- **`deploy.yml`** (GitHub Actions) : push sur `main` → build image GHCR → deploy VPS via SSH,
  écrit `IMMO_BACK_TAG` dans le `.env` du VPS, `docker compose up -d immo-back`. Healthcheck sur
  `127.0.0.1:9091/actuator/health` (actuator sur **9091**, pas 8080).
- **PRs en attente de merge** (ordre de priorité) : `fix/synchro-lignes-hors-perimetre` (débloque
  la prod !), `chore/monitoring-dashboard`, `feat/sync-manuel-commerciaux`.

### Monitoring
- Prometheus scrape déjà l'actuator (job `immo-back`). Grafana **déjà en place sur le VPS** (logs +
  métriques). Dashboard prêt : `monitoring/grafana-sync-pipeline.json` (branche monitoring).
- **Les `sync_*` n'existent que si le backend tourne en DEUX-BASES** (`SOURCE_DB_HOST` défini).
  Vérif : `curl -s http://127.0.0.1:9091/actuator/prometheus | grep '^sync_'`. Vérifié :
  l'instance qui tournait n'avait PAS les `sync_` → soit ancien build, soit mono-base.
- Timer sans histogramme → durée **moyenne** (`rate(_sum)/rate(_count)`), pas p95.

### Rôles / sécurité
- Rôles = `ROLE_<AdmRole.Code>` (`JwtAuthenticationFilter`), `@PreAuthorize("hasRole('...')")`.
  Codes commerciaux réels : `Commercial`, `Commercial Chef`, `Commercial Aide`, `Assistante
  Commerciale`, `Direction fonciere`, `Niveua remise 2`. Sync manuelle ouverte aux 3 premiers + admin.

### Synchro — invariants
- `SynchronisationDescendante` : deux régimes par table (BASELINE si filigrane absent/périmé, DELTA
  via `CHANGETABLE`). Filigrane par table dans `AppSyncEtat` (côté copie). Échec d'une table = son
  filigrane fige et elle rejoue ; la **convergence FK** (enfant avant parent) est normale sur 1-2 cycles.
- **Le noyau ~23 tables n'est PAS FK-clos** → certaines lignes réfèrent des modules hors périmètre
  (achat/location). Pour une table qui boucle : ajouter à `COLONNES_HORS_PERIMETRE`.

---

## 📁 Important Files & Paths

- `instructions/FEATURE-transformation-tranche-obligatoire.md` — tranche oblig. + 5 champs oblig. + CT autres tranches.
- `instructions/FEATURE-contacts-prospection.md` — contacts (création OMRAN + endpoints prospection).
- `instructions/FEATURE-moniteur-pipeline-synchro.md` — métriques + PromQL/alertes.
- `instructions/FIX-synchro-lignes-hors-perimetre.md` — le fix StlSettlementLine.
- `src/main/java/.../config/MoniteurSynchronisation.java` — mètres Micrometer.
- `src/main/java/.../config/RemonteeLecture.java` — `COLONNES_HORS_PERIMETRE`/`predicatPerimetre` (sur la branche fix uniquement).
- `src/main/java/.../controller/synchronisation/SynchronisationController.java` — `hasAnyRole(...)`.
- `monitoring/grafana-sync-pipeline.json` + `monitoring/README.md` — dashboard (branche monitoring).

---

## 🔧 Environment & Config

- Banc local deux-bases : `DB_*`=yesRemsProd (copie), `SOURCE_DB_*`=yesRems (source/rôle 115), tous
  `localhost,49687` (tsix / securePassword123). CT actif sur `yesRems` (28 tables au dernier relevé).
- Interroger la trace : `sys.fn_get_audit_file('C:\backup\*.sqlaudit', default, default)`.
- Tests : `./mvnw.cmd test` (~4 min, **921 verts**). RemonteeLecture/Synchro/Moniteur = beans
  conditionnels deux-bases → **non instanciés en test mono-base** ; testés en unitaire pur
  (`SimpleMeterRegistry`, `predicatPerimetre` statique).
- ⚠️ Identifiants VPS/115/tsix en clair au fil des sessions — dette connue.

---

## ⏭️ Next Step

> **Merger `fix/synchro-lignes-hors-perimetre` en priorité** (il débloque la synchro en prod :
> `StlSettlementLine` cesse d'échouer en boucle), puis vérifier sur le dashboard que
> `sync_tables_failed` retombe à 0 et `sync_watermark_lag{table="StlSettlementLine"}` repasse à 0.

### Backlog
- [ ] Merger `chore/monitoring-dashboard` et `feat/sync-manuel-commerciaux`.
- [ ] **Confirmer que le backend prod tourne en deux-bases** (`SOURCE_DB_HOST`=115) — sinon les
      `sync_*` n'apparaissent pas dans Grafana et OMRAN n'atterrit pas.
- [ ] Décider si d'autres rôles commerciaux (`Assistante Commerciale`, `Direction fonciere`,
      `Niveua remise 2`) doivent aussi déclencher la synchro manuelle.
- [ ] Sous-tables restantes hors CT (`ComProspectionClosingReason`, référentiels) — à ajouter si
      leurs détails doivent remonter d'OMRAN.
- [ ] `ComActionMessage` : 1 ligne irréductible (FK vers module externe) — ne boucle pas, laissée.
- [ ] Décision métier : répercuter ou non la suppression d'une pièce jointe côté légataire.

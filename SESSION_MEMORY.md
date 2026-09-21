# Session Memory
_Last updated: 2026-09-21T16:40+01:00_
_Project: immo-crm (AyadERP) — C:\Users\tsiory\Documents\ayad-internship\prototype\immo-crm_

---

## 🎯 Current Objective

**Source de vérité unique + synchro bidirectionnelle (branche `feat/source-de-verite-115`, PR ouverte, non mergée).**
Une seule base est écrite (OMRAN/115) ; le backend lit sa copie locale. Implémenté et vérifié :
routage, rapatriement immédiat (INSERT/UPDATE/DELETE), conformité légataire, pièces jointes,
**synchro descendante par Change Tracking (cycle 2 min + endpoint manuel)**. Cette session a aussi
**mis à niveau `yesRemsDev` (VPS) depuis 115** et **prouvé que la synchro descendante propage
bien les UPDATE d'OMRAN** (pas seulement les INSERT).

---

## ✅ What Worked

### Session du 21/09 (suite)

- **Endpoint manuel `POST /api/synchronisation`** (rôle Administrateur), synchrone, `ReentrantLock`
  anti-recouvrement partagé avec le cycle planifié (409 si déjà en cours / mono-base). Vérifié.
- **Test de la synchro descendante sur banc local** (`yesRems`=115, `yesRemsProd`=VPS) : INSERT,
  UPDATE **et** DELETE d'un prospect + prospection faits par SQL direct dans `yesRems` sont tous
  arrivés dans `yesRemsProd` (fusions v60/61/63, suppression v65). **La preuve = `AppSyncEtat`
  (filigranes), pas le log** — le log Maven est bufferisé et retarde de plusieurs minutes.
- **Mise à niveau `yesRemsDev` depuis 115** : CT activé sur 115 (base + 20 tables), baselines par
  l'outil pour les grosses tables (`CfgTier` 14553, `ComProspection` 17976…), puis **insertion
  ciblée** pour le reste (StrHistory +262, ComActionMessage +68, AdmAttachedFile +2) — plus rapide
  et gère proprement les FK hors périmètre. Suppression de 38 lignes VPS-only (test + orphelins).
- **Recherche OMRAN Estate** : éditeur **Yasmine Engineering Systems** (Tunis), .NET desktop, **SQL
  Server exclusif** (uniqueidentifier, FILESTREAM, Change Tracking, triggers). **Incompatible MySQL**
  sans re-plateformage par l'éditeur ; migration MySQL possible seulement après retrait d'OMRAN.
- **Connexion au VPS via tunnel** : `Encrypt=True` obligatoire (le SQL du VPS impose TLS) — sinon
  la poignée de main échoue en silence. Tunnel = `ssh vps-db -N` → `localhost:1433` = `yesRemsDev`.

### Acquis antérieurs (toujours valables)

- Mesurer avant de conclure ; connexion persistante pour benchmarker (pas sqlcmd par requête).
- Le pilote mssql livre `uniqueidentifier` en `String`, pas `UUID`.

---

## ❌ What Failed

### Session du 21/09 (suite)

- **Baseline de masse sur lien lent/FK non closes** → `ComActionMessage` 48k tirées ~30 min pour
  +88, et `StlSettlementLine` échoue en boucle (FK vers `BuyDocument`/`RntDocument` hors périmètre).
  **Leçon : le noyau ~23 tables n'est PAS FK-clos** ; pour un backfill ponctuel, l'**insertion
  ciblée des PKey manquants** (en sautant les FK hors périmètre) bat la baseline de l'outil.
- **Croire le log de sync** → il est bufferisé par Maven (retard de minutes). Vérifier `AppSyncEtat`
  et l'état des tables, jamais le log seul.
- **Anti-jointure PowerShell qui échoue en silence** (colonne inexistante `Description` sur
  `ComProspection`) → affiche « 0 » à tort. Toujours revérifier un « 0 » suspect.
- **Écrire directement sur 115 via IP publique** pour tester → l'utilisateur veut du **local**
  (`yesRems`/`yesRemsProd`), la prod n'étant pas déployée. Un prospect de test avait été inséré sur
  115 et propagé au vrai VPS — nettoyé des deux côtés.

---

## 🐛 Bugs & Errors Log

| # | Erreur / Bug | Statut | Correctif |
|---|---|---|---|
| 19 | `DELETE` non rapatriés (fantômes) | **Fixed** | `POST_DELETE` + absence=suppression |
| 20 | Upload pièce jointe 401 (colonne absente) | **Fixed** | migration 006 sur la source |
| 21 | Synchro descendante : UUID livré en String | **Fixed** | conversion `UUID.fromString` |
| 22 | `yesRemsDev` : baseline `StlSettlementLine`/`ComActionMessage` échoue sur FK hors périmètre | **Contourné** | insertion ciblée ; 13 lignes irréductibles laissées (modules achat/location) |
| 23 | Tunnel VPS : poignée de main échoue | **Fixed** | `Encrypt=True` dans la chaîne de connexion |
| — | **« OMRAN n'atterrit pas sur la nouvelle app »** | **Non-bug** | le code marche (INSERT/UPDATE/DELETE testés OK) ; cause = **la synchro n'est pas déployée en prod** (backend pas en mode deux-bases) |

---

## 🔑 Key Points to Remember

### Synchro descendante — état réel

- **Le code marche pour INSERT/UPDATE/DELETE** (prouvé sur banc local). Le listener Hibernate
  écoute `POST_INSERT/UPDATE/DELETE` mais ne sert QUE app→115 ; ce qui capte OMRAN c'est le
  **Change Tracking** (`CHANGETABLE` renvoie tous les types, MERGE réapplique l'UPDATE).
- **Pourquoi OMRAN « n'atterrit pas » en réel** : (1) la synchro ne tourne QUE si le backend est
  lancé en **deux-bases** (`SOURCE_DB_HOST`=115) — **pas encore déployé en prod** ; (2) CT activé
  sur 115 seulement aujourd'hui ; (3) **seules 20 tables sont suivies** — les sous-tables de
  prospection (`ComProspectionCity`, `…Profil`, `…TypeBien`) **ne remontent pas**. À ajouter au CT
  si ces détails doivent remonter.
- ⚠️ **Vérifier l'état via `AppSyncEtat` et les comptes de tables, JAMAIS via le log** (bufferisé).

### Parc réel (mesuré)

- `yesRems` (localhost,49687) = **rôle 115** en local ; `yesRemsProd` = **rôle VPS** en local. CT
  actif sur `yesRems` (23 tables). `AppSyncEtat` sur `yesRemsProd`.
- **115 réel** = `197.230.172.136:1433` (public), compte `sasa`/`Azerty123+-`, db_owner. **CT
  maintenant actif** (base + 20 tables, réversible `SET CHANGE_TRACKING = OFF`).
- **VPS réel** = tunnel `ssh vps-db -N` → `localhost:1433`, base `yesRemsDev`, `dev`/`pro*0803`,
  db_owner, **`Encrypt=True` requis**. SQL Server dans Docker (`ports: 127.0.0.1:1433:1433`).
- Migration `009_AppSyncEtat` appliquée sur `yesRemsDev` ; 20 filigranes posés.

### OMRAN Estate

- Éditeur **Yasmine Engineering Systems** (Tunis, 2005), .NET desktop, **SQL Server exclusif**.
  **Pas migrable vers MySQL** sauf par l'éditeur. Tant qu'OMRAN vit → SQL Server obligatoire.

### Backend (rappels)

- Mode deux-bases : `SOURCE_DB_HOST` vide = mono-base (comportement d'avant, tests verts). CT sur
  la source requis pour la synchro descendante. Cycle `SOURCE_DB_SYNC_MINUTES:2`.
- Insertion ciblée > baseline pour un backfill ponctuel sur lien lent / FK non closes.

---

## 📁 Important Files & Paths

- `instructions/FEATURE-source-de-verite-115.md` — **document de référence de la branche** (toutes
  sections : routage, pièces jointes, synchro descendante, cadence 2 min + endpoint manuel).
- `src/main/java/.../config/SynchronisationDescendante.java` — cycle + `executer()` + `ResultatSync`.
- `src/main/java/.../controller/synchronisation/SynchronisationController.java` — endpoint manuel.
- `src/main/java/.../config/{RemonteeLecture,CollecteurLignesEcrites,EnregistrementCollecteur,DataSourceConfig}.java`
- `src/main/resources/db/migration/009_AppSyncEtat.sql` — filigranes (copies uniquement, jamais 115).
- `prototype/ct-activer-yesRems.sql` · `prototype/sync-ct-descendant.sql` — scripts CT / conception.
- Scratchpad `vps_deletions_audit.txt` — audit des PKey supprimés sur `yesRemsDev` (hors repo).

---

## 🔧 Environment & Config

- Banc local deux-bases : `DB_*`=yesRemsProd, `SOURCE_DB_*`=yesRems, tous `localhost,49687` (tsix).
  Lancer : `./mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=808X
  --management.server.port=909X"` (varier les ports, 9091/9092 souvent occupés).
- Tests : `./mvnw.cmd test` (~4 min, 901 verts au dernier passage).
- Git : `node .claude/scripts/git-flow.mjs` depuis `prototype/`. Branche `feat/source-de-verite-115`
  **poussée, PR ouverte** (3 commits de la session : `366a2bc`, `680a1a4`, `c00447b` + docs).
- ⚠️ Identifiants VPS/115 en clair au fil des sessions — dette connue.

---

## ⏭️ Next Step

> **Décider avec l'utilisateur du déploiement prod de la synchro descendante** : pour qu'OMRAN
> atterrisse réellement sur la nouvelle app, il faut lancer le backend de prod en **mode deux-bases**
> (`SOURCE_DB_HOST`=115). Le code, le CT sur 115 et les filigranes du VPS sont déjà prêts.

### Backlog

- [ ] **Ajouter les sous-tables de prospection au Change Tracking** (`ComProspectionCity`, `…Profil`,
      `…TypeBien`…) + à la liste synchronisée, si ces détails doivent remonter d'OMRAN.
- [ ] Les **13 lignes irréductibles** sur `yesRemsDev` (12 `StlSettlementLine` + 1 `ComActionMessage`,
      FK vers modules achat/location) : les laisser, ou étendre le périmètre à ces modules ?
- [ ] Décider si le **CT reste actif sur 115** en attendant le déploiement (laissé actif ; réversible).
- [ ] **Merger la PR `feat/source-de-verite-115`**, puis `sync back`.
- [ ] Décision métier : répercuter ou non la suppression d'une pièce jointe côté légataire.
- [ ] Backlog stable : filtre par colonne (`from_front.md` n°11), 13 champs encaissement sans
      description springdoc, `ModifierProspectionRequest` (tempDescription/tempTel/tempEmail),
      pagination non déterministe, encaissements non affectés, verrous pessimistes.

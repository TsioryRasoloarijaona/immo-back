# Session Memory
_Last updated: 2026-09-21T12:30+01:00_
_Project: immo-crm (AyadERP) — C:\Users\tsiory\Documents\ayad-internship\prototype\immo-crm_

---

## 🎯 Current Objective

**La source de vérité unique (branche `feat/source-de-verite-115`, PR ouverte, non mergée).**
Une seule base est écrite (celle d'OMRAN, à terme 115) ; le backend lit sa copie locale.
Le routage, le rapatriement, la conformité légataire (`StrEntityId`/`IsClosed`), **les pièces
jointes**, **la propagation des suppressions** et **la synchronisation descendante par Change
Tracking (cycle 5 min, `SynchronisationDescendante`)** sont implémentés et vérifiés de bout en
bout sur la paire locale. L'utilisateur applique lui-même migrations et variables côté prod/115.

Chantier infra parallèle (inchangé depuis le 16/09) : VPN NetBird sur le VPS pour remplacer la
redirection de port qui expose `192.168.11.115:1433` — voir `../infra/NETBIRD-VPS.md`.

---

## ✅ What Worked

### Session du 21/09 (pièces jointes + campagne de test deux-bases)

- **Le flux pièces jointes retenu** : S3 → source (métadonnées par JPA routé + **binaire par
  `UPDATE` natif via `EntityManager`, même transaction donc même connexion routée** —
  `Propagation.MANDATORY`) → copie par rapatriement (sans colonne binaire, `AppFileObject`
  entier). Le `PKey` est frappé côté application (`UUID.randomUUID()`) : **aucun risque de
  collision, pas d'aller-retour d'identifiants** — pas de compteur `AdmSequence` sur ce flux.
- **Campagne `/postman:test` en direct** (Postman MCP non connecté) : backend en deux-bases
  local (`yesRems` source / `yesRemsProd` copie), compte `Khadija`, 14 endpoints d'écriture,
  vérification SQL des deux côtés après chaque appel. Tout conforme, compteurs de la copie
  jamais bougés. Détail complet : FEATURE, section « Vérifié de bout en bout (21/09) ».
- **Faire tourner ma propre instance sur :8081** (`--server.port=8081 --management.server.port=9092`)
  pour avoir les logs — c'est ce qui a résolu les 401 nus.
- **Lire `.env` avec l'outil Read**, pas `cat` : le fichier sans saut de ligne final s'était
  concaténé avec la sortie suivante → clé S3 fausse → 403 signature MinIO.

### Sessions précédentes — toujours valable

- Mesurer avant de recommander ; compter avant de conclure ; vérifier qu'une colonne existe
  avant de la mapper ; fixtures datées 2077 ; retouches de source en Python depuis un fichier.
- Trace d'audit : filtrer `application_name = 'OMRAN estate'` ; `event_time` en UTC.

---

## ❌ What Failed

### Session du 21/09

- **`401` nu sur les uploads multipart** → deux causes successives, aucune liée à l'auth :
  (1) colonne `DeletedBy` absente de `yesRems.AppFileObject` (migration 006 non appliquée sur la
  source du banc) ; (2) clé S3 tronquée/faussée par ma lecture de `.env`. **Leçon : une exception
  dans la chaîne part dans le dispatch d'erreur et ressort en 401 sans corps** — ne pas chercher
  côté JWT.
- **Les `DELETE` ne voyageaient pas** (collecteur limité à `POST_INSERT`/`POST_UPDATE`) → fantômes
  dans la copie, contact supprimé toujours affiché. Corrigé (voir Key Points).
- **`PATCH /prospects` avec un champ inexistant (`notes`)** → 200 no-op silencieux (Jackson ignore
  les champs inconnus). Vérifier le DTO avant de conclure à un bug de routage.
- **`sqlcmd` par ligne dans une boucle shell** → une connexion par itération, timeout : faire un
  seul `IN (...)`.
- **`VAR=val` quoté en préfixe bash** (`'S3_SECRET_KEY=...'`) → « command not found » : `export`.

---

## 🐛 Bugs & Errors Log

| # | Erreur / Bug | Statut | Correctif |
|---|---|---|---|
| 1–15 | (lots G–R + créations, voir historique) | **Fixed** | PR #43–#55 |
| 16 | `192.168.11.115:1433` exposé à internet | **Ouvert** | VPN NetBird, `../infra/NETBIRD-VPS.md` |
| 17 | Écriture disque 100 ms sur 115 | **Ouvert — assumé** | garder 115 tel quel |
| 18 | Prospection CRM invisible dans OMRAN (`StrEntityId`/`IsClosed` NULL) | **Fixed** | commits `f83ba18`/`a54208d` |
| 19 | `DELETE` non rapatriés → fantômes dans la copie | **Fixed** | `POST_DELETE` + absence = suppression (commit `366a2bc`) |
| 20 | Upload pièce jointe en 401 (colonne `DeletedBy` absente de la source) | **Fixed (banc)** | 006 appliquée sur `yesRems` ; sur 115 = check-list utilisateur |

---

## 🔑 Key Points to Remember

### L'architecture deux-bases (branche courante)

- Routage sur le drapeau `readOnly` (`DataSourceConfig`), rapatriement par `MERGE` après commit
  (`CollecteurLignesEcrites` → `RemonteeLecture`). **`SOURCE_DB_HOST` vide = mono-base**, aucun
  bean n'existe, comportement d'avant — c'est ce qui garde les 895 tests verts.
- **Depuis le 21/09** : clé d'identité par table (`AppFileObject` → `AdmAttachedFileId`),
  colonnes exclues dès le `SELECT` (`AdmAttachedFile.AttachedFile`), **absence sur la source =
  suppression dans la copie** (après les fusions, ordre inverse — la copie porte 1 219 FK), et
  `DepotBinaireSource` (+2 impl.) pour le binaire des pièces jointes.
- **Synchro descendante** (`SynchronisationDescendante`, cycle `SOURCE_DB_SYNC_MINUTES:2`) :
  Change Tracking, filigrane par table dans `AppSyncEtat` (copie, migration `009` — pré-remplir
  à la version courante sur une copie fraîchement restaurée), régimes BASELINE (jamais de
  suppression) / DELTA (la relecture fait foi, pas le code I/U/D). Un cycle vide ne logge rien.
  ⚠️ Le pilote mssql livre `uniqueidentifier` en `String`, pas en `UUID`.
  Vérifié : INSERT/UPDATE/DELETE simulés par sqlcmd dans la source arrivent au cycle suivant.
- **Déclenchement manuel** : `POST /api/synchronisation` (rôle Administrateur), synchrone, rend
  le bilan. `ReentrantLock.tryLock` partagé avec le cycle planifié → 409 si un cycle tourne ;
  409 aussi en mono-base (contrôleur présent via `ObjectProvider`, service absent). Vérifié en
  deux-bases (200+fusion, 2 appels → 200/409) et en mono-base (409/403, tests auto).
- ⚠️ **Coût fixe par cycle = ~23 tables × 3 requêtes × RTT.** Sur 115 (queue de latence) un
  cycle vide = quelques secondes. Ne PAS descendre bien en dessous de 1–2 min sans agréger les
  filigranes en une requête. Décision honnête livrée à l'utilisateur : garder les deux bases,
  la base unique 115 ferait payer la latence + sa queue à **toutes** les lectures (mesuré :
  liste 50 prospects p50 20 ms copie vs 77 ms / p90 300 ms sur 115).
- **Toute ligne référencée par une création doit exister sur la source** : la validation lit
  dans la transaction d'écriture. Face à 115 (sur-ensemble) c'est automatique, SAUF pour ce que
  le CRM crée seul → **rattrapage obligatoire avant activation : `AppUserCredential`,
  `AppFileObject`, employés créés par l'API**.
- **Check-list 115 (l'utilisateur s'en charge)** : appliquer `001`, `002`, `003`, `006` —
  **jamais la 004** (elle supprime la colonne que le légataire lit) — puis recopier les lignes
  `App*` existantes ; variables `SOURCE_DB_*`.
- Décision métier laissée ouverte : une pièce supprimée par le CRM reste visible dans OMRAN
  (la ligne source et son binaire ne sont pas touchés).
- Hors périmètre restant : synchro descendante périodique (Change Tracking, 23 tables déjà
  activées) ; reprise de l'historique des binaires 115 ↔ S3.

### Le banc de test deux-bases local

- Source `yesRems`, copie `yesRemsProd`, les deux sur `localhost,49687` (tsix). Compte HTTP :
  `Khadija` (copie seulement !) — `t.admin` aussi. Sur la source : `tsiory`, `t.nahoma`, etc.
- Lancer une instance : exporter `DB_*`→copie, `SOURCE_DB_*`→source, `JWT_SECRET`, `S3_*`
  (valeurs dans `.env` — le lire avec Read, il n'a pas de saut de ligne final),
  `--server.port=8081 --management.server.port=9092` si l'instance de l'utilisateur tourne.
- **Données de test à purger** : script complet dans le FEATURE, section « Données de test à
  retirer (21/09) » — à passer sur les deux bases. Compteurs source avancés (14442/5942/409),
  toléré.

### Parc, doctrine, conventions (inchangé)

- 115 = production vivante, lecture seule stricte (compte `sasa`, mdp non consigné) ; FILESTREAM
  ne compte pas dans la limite Express ; lenteur = disque. `AdmSequence` : `SeqValue` est le
  compteur (`SeqValueNumber` = largeur de format).
- Doctrine `Optional`, `IsSystem` (deux conventions justes), commits ASCII `feat(scope):`,
  ne jamais réécrire un fichier source via PowerShell.

---

## 📁 Important Files & Paths

- `instructions/FEATURE-source-de-verite-115.md` — **le document de référence de la branche** :
  conception, pièges, vérifications 18/09 et 21/09, check-list 115, script de nettoyage.
- `src/main/java/.../config/{DataSourceConfig,CollecteurLignesEcrites,RemonteeLecture,RemonteeLectureListener,EnregistrementCollecteur,SourceDeVeriteProperties}.java`
- `src/main/java/.../services/piecejointe/{DepotBinaireSource,DepotBinaireAbsent,DepotBinaireVersSource,PieceJointeSupport}.java`
- `src/main/resources/db/migration/00{1,2,3,6}*.sql` — à appliquer sur 115 (jamais la 004) ;
  `008_ComProspection_visibilite_legataire.sql` — à appliquer à la main.
- `../infra/NETBIRD-VPS.md` — runbook VPN (non versionné, `prototype/` n'est pas un repo).
- `instructions/from_front.md` n° 11 — le filtre par colonne, backlog front prioritaire.

---

## 🔧 Environment & Config

- Mesure : `sqlcmd -S localhost,49687 -U tsix -P <voir CLAUDE.md> -d <base> -C`. `-W`/`-y`
  exclusifs, chemins via `cygpath -w`, préférer un seul `IN (...)` aux boucles.
- Tests : `./mvnw.cmd test` (~4 min, 895). SQL Server requis.
- Git : `node .claude/scripts/git-flow.mjs` depuis `prototype/`. Branche courante
  `feat/source-de-verite-115`, **poussée, PR ouverte, en attente de merge**.
- ⚠️ L'instance backend de l'utilisateur (:8080) date d'avant les correctifs du 21/09 —
  redémarrer après pull pour retester.

---

## ⏭️ Next Step

> **Attendre la merge de la PR `feat/source-de-verite-115`** (l'utilisateur la fait sur GitHub),
> puis `node .claude/scripts/git-flow.mjs sync back`. Ensuite, passer le script de nettoyage
> des données de test (FEATURE, section 21/09) sur `yesRems` et `yesRemsProd`.

### Backlog

**Source de vérité / infra (l'utilisateur pilote)**
- [ ] Merge PR + `sync back` ; nettoyage des données de test (script dans le FEATURE).
- [ ] Côté 115 (utilisateur) : migrations 001/002/003/006, rattrapage `App*`, variables prod.
- [ ] VPN NetBird : DNS `vpn.tsiory.tech` → étapes de `../infra/NETBIRD-VPS.md`.
- [x] ~~Synchro descendante périodique~~ — faite et vérifiée le 21/09 (cycle 5 min).
- [ ] Sur le banc : `AppSyncEtat` existe désormais dans `yesRemsProd` (22 filigranes posés,
      baseline passée sur `ComProspectionType`) — c'est l'état nominal de la copie, à garder.
- [ ] Reprise de l'historique des binaires (115 ↔ S3) — les deux sens.
- [ ] Décision métier : répercuter ou non la suppression d'une pièce côté légataire.

**Backend (backlog stable)**
- [ ] Lot S — filtre par colonne (`from_front.md` n° 11), insensible casse ET accents.
- [ ] 13 champs du DTO encaissement sans description springdoc (règle 10).
- [ ] `ModifierProspectionRequest` : `tempDescription`, `tempTel`, `tempEmail`, `commercialId`.
- [ ] `CreerReglementRequest.dateReglement` publiée par le spec mais refusée (400).
- [ ] Ordre de pagination non déterministe ; encaissements non affectés (16 pièces, 6,4 MDH) ;
      verrous pessimistes ; seconde couche de permissions ; onglets fiche dossier ;
      contrôles de création OMRAN (décision produit) ; branches mergées à supprimer.

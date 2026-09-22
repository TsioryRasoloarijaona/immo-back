# Session Memory
_Last updated: 2026-09-22T10:45+01:00_
_Project: immo-crm (AyadERP) — C:\Users\tsiory\Documents\ayad-internship\prototype\immo-crm_

---

## 🎯 Current Objective

**Préserver et exploiter la trace d'audit OMRAN, et fiabiliser/observer la synchro descendante
deux-bases.** Cette session : feature `feat/archivage-trace-omran` — l'audit SQL Server tourne
sur 20 fichiers × 100 Mo et supprime les plus anciens ; un cycle planifié archive la part OMRAN
avant la rotation. Les 3 PRs de la session précédente restent à merger.

---

## ✅ What Worked

### Feature livrée : archivage de la trace OMRAN (930 tests verts)
- `config/ArchivageTraceOmran` (moule de `SynchronisationDescendante`) : lit
  `sys.fn_get_audit_file` **sur la source**, filtre `application_name = 'OMRAN estate'` côté
  serveur, écrit `AppTraceOmran` **sur la copie** via `jdbcLecture` (migration
  `010_AppTraceOmran.sql`, appliquée sur yesRemsProd le 22/09).
- Activation : deux-bases **ET** `AUDIT_TRACE_PATH` non vide (défaut vide = rien n'existe,
  la suite de tests boote inchangée). Cadence `AUDIT_TRACE_MINUTES` (60).
- **Vérifié sur banc** : 1re passe T-SQL = 42 852 lignes = compte brut de l'audit à bornes
  identiques ; 2e passe purge-et-rejeu : comptes inchangés (idempotence prouvée).

### Insertion directe de 7 pré-prospections FERME BRETONNE sur le vrai 115 (22/09)
- **Le pipeline n8n est à l'arrêt depuis le 08/09** (dernier lead injecté) — d'où l'insertion
  manuelle des soumissions du 21–22/09, demandée explicitement par l'utilisateur.
- Motif n8n copié (2 725 leads mesurés) : `Code='50'`, `TempDescription/TempEMail/TempTel`
  (tél. 9 chiffres sans le 0), `IsClosed=0`, `StrEntityId=ComPreProspection`
  (`b92df3d8-...`), `DocDate=UserCreateDateTime`, `UserCreate NULL`, origine FACEBOOK,
  vocation VENTE, type WEB, nature « 1 A TRAITER », tranche APF
  (`c273dec7-08d1-4256-9e3c-efa1485c4ee8`, AYAD PRIVATE FERME BRETONE).
- Script idempotent (PKey figées + NOT EXISTS) :
  `scratchpad/insert-preprospections-ferme-bretonne.sql`. 7 insérées, 7 visibles dans
  `ComSalePreProspectionView` (la vue de l'écran OMRAN).
- 6 des 7 personnes existaient déjà (soumissions antérieures) — une ligne PAR soumission est
  le comportement légataire (amakssa 4×), donc inséré quand même.

### Fix auteur des interactions (branche `fix/auteur-interactions`, 932 tests verts)
- Symptôme : une action saisie dans le CRM s'affichait « Système » ; OMRAN, lui, retrace.
- **L'écriture était déjà bonne** (AdmUserId/UserName/UserCreate posés depuis le jeton).
  Le bug était **à la lecture** : `employeNom` dérivé du seul `CfgEmployeeId` (77 % des
  48 278 interactions réelles, jamais nos comptes) alors qu'`UserName` est à 99,996 %.
- Correctif : `ProspectionService.nomAuteur` (employé → UserName → UserCreate), partagé avec
  `HotlineService` (leurs mappeurs avaient déjà divergé une fois). `@Schema` mis à jour :
  le front ne doit plus afficher « Système » sur la seule absence d'`employeId`.
- PR : https://github.com/TsioryRasoloarijaona/immo-back/pull/new/fix/auteur-interactions

### Confirmation de visite : localisation au lieu des plans (même branche `fix/auteur-interactions`)
- Demande utilisateur : plus de plan joint ; envoyer l'adresse + « localisation exacte si
  disponible ». **Aucun GPS en base** (0/1 068 StkItem ; rien sur CfgProject/CfgTranche) →
  lien Google Maps construit sur l'adresse R4 (tranche → projet, fausses adresses rejetées).
- Supprimé tout le mécanisme plans (filtre PLAN*, plafond, lecture S3) ; `lienCarte` dans le
  mail HTML + corps texte + WhatsApp. `nombrePiecesJointes` = toujours 0 (champ conservé).
- RDV : l'heure n'est captée nulle part — front `type="date"` (à changer côté front) ; OMRAN
  non plus (2 454 RDV réels, AlertValidityDate 100 % à minuit, l'heure en texte libre).
  `AdmEvent` est VIDE sur le 115 : OMRAN ne structure pas ses RDV.

### Méthode (toujours payante)
- **Mesurer avant de concevoir** a invalidé deux designs successifs (voir What Failed) :
  toujours compter les doublons dans les données réelles avant de choisir une clé.
- `git commit -F <fichier scratchpad>` pour les messages multilignes (le here-string PS casse).

---

## ❌ What Failed

- **Clé naturelle `(file_name, audit_file_offset, sequence_number)` pour dédupliquer l'audit**
  → 1 044 groupes en doublon : un bloc tampon partage son offset entre plusieurs
  enregistrements, et une requête sur une vue produit **un enregistrement par objet référencé**.
- **Clé étendue (+ object_name, action_id)** → 18 848 lignes du fichier brut **strictement
  identiques sur toutes les colonnes** : il n'existe AUCUNE clé naturelle dans
  `sys.fn_get_audit_file`. Dédupliquer perdrait de vrais enregistrements.
- **Filigrane strict (`>`) + lots coupés aux frontières de tick** → cassé par `batchUpdate`
  qui commite instruction par instruction : un échec à mi-lot laisse un tick à moitié archivé.
  **Design final : purge-et-rejeu de la borne en `>=`, lecture ordonnée, borne haute −5 min.**

---

## 🐛 Bugs & Errors Log

| # | Erreur / Bug | Statut | Correctif |
|---|---|---|---|
| — | Aucun bug nouveau cette session (26 → voir sessions précédentes : #24 StlSettlementLine et #25 CT autres-tranches, fixés, PRs en attente) | — | — |

---

## 🔑 Key Points to Remember

### Audit OMRAN
- `Audit_Global` → `C:\backup\`, **rotation 20 fichiers × 100 Mo** ; la spec
  (`Audit_DML_DDL_Spec` sur `yesRems`) capte TOUT le DML — le bruit (synchro JDBC : 24 k
  lignes, Workbench : 10 k) consomme la fenêtre, d'où l'urgence d'archiver.
- `action_id` arrive complété d'espaces (`'SL  '`) — normalisé au trim dans l'archive.
- L'audit horodate en **UTC** (colonne `EventTimeUtc`).
- **En prod (source = 115) : vérifier qu'un audit y est configuré avant de poser
  `AUDIT_TRACE_PATH`** — le chemin est résolu par l'instance SQL du 115, pas par le conteneur.
  `sys.fn_get_audit_file` exige `CONTROL SERVER`.

### Git / PRs en attente de merge (ordre de priorité)
1. `fix/synchro-lignes-hors-perimetre` (**débloque la synchro en prod**)
2. `chore/monitoring-dashboard`
3. `feat/sync-manuel-commerciaux`
4. `feat/archivage-trace-omran` (cette session) —
   https://github.com/TsioryRasoloarijaona/immo-back/pull/new/feat/archivage-trace-omran

### Synchro — invariants (inchangés, voir sessions précédentes)
- Filigrane par table dans `AppSyncEtat` ; vérifier la synchro via `AppSyncEtat` + contenu
  des tables, jamais le log. `sync_*` sur `:9091/actuator/prometheus` seulement en deux-bases.
- Rôles = `ROLE_<AdmRole.Code>` ; sync manuelle ouverte à `Commercial`, `Commercial Chef`,
  `Commercial Aide` + admin.

---

## 📁 Important Files & Paths

- `instructions/FEATURE-archivage-trace-omran.md` — la spec de cette session (mesures + design).
- `src/main/java/.../config/ArchivageTraceOmran.java` — le cycle d'archivage.
- `src/main/resources/db/migration/010_AppTraceOmran.sql` — la table (copies uniquement,
  appliquée sur yesRemsProd ; **à appliquer sur yesRemsDev au déploiement**).
- `src/test/java/.../config/ArchivageTraceOmranTest.java` — 7 tests unitaires purs.
- `instructions/FEATURE-moniteur-pipeline-synchro.md`, `FIX-synchro-lignes-hors-perimetre.md`
  — sessions précédentes, PRs en attente.

---

## 🔧 Environment & Config

- Banc local deux-bases : `DB_*`=yesRemsProd (copie), `SOURCE_DB_*`=yesRems (source), tous
  `localhost,49687` (tsix). Audit : `sys.fn_get_audit_file('C:\backup\*.sqlaudit', DEFAULT, DEFAULT)`.
- Nouvelles variables : `AUDIT_TRACE_PATH` (vide = inactif), `AUDIT_TRACE_MINUTES` (60).
- Tests : `./mvnw.cmd test` (~4 min, **930 verts**). ArchivageTraceOmran = bean conditionnel
  deux-bases+chemin → non instancié en test ; testé en unitaire pur (requête, paramètres, trim).
- `AppTraceOmran` sur yesRemsProd contient déjà les 42 852 lignes du banc (11/09 → 22/09).

---

## ⏭️ Next Step

> **Merger les 4 PRs sur GitHub** (ordre : fix hors-périmètre → monitoring → sync commerciaux →
> archivage trace), puis `git checkout main && git pull` et supprimer les branches mergées.

### Backlog
- [ ] Appliquer `010_AppTraceOmran.sql` sur **yesRemsDev** (VPS) au déploiement.
- [ ] Vérifier/poser un audit SQL Server **sur le 115** avant d'activer `AUDIT_TRACE_PATH`
      en prod (compte `sasa`, mot de passe détenu par l'utilisateur).
- [ ] Confirmer que le backend prod tourne en deux-bases (`SOURCE_DB_HOST`=115).
- [ ] Décider si d'autres rôles commerciaux doivent déclencher la synchro manuelle.
- [ ] Sous-tables restantes hors CT (`ComProspectionClosingReason`, référentiels).
- [ ] Décision métier : répercuter ou non la suppression d'une pièce jointe côté légataire.

# Session Memory
_Last updated: 2026-09-25T12:30+01:00_
_Project: immo-crm (AyadERP) — C:\Users\tsiory\Documents\ayad-internship\prototype\immo-crm_

---

## 🎯 Current Objective

Deux chantiers en parallèle : (1) **parité fonctionnelle avec OMRAN sur la prospection**, pilotée
par la trace d'audit (dernier lot : doublons de prospect + unicité prospect × projet, branche
poussée, PR à merger) ; (2) **mise en production du second VPS (`vps_ayad`, client)** à côté du
VPS de dev (`vps_tsiory`) — pipeline en matrice mergé, vérification CORS sur `vps_ayad` encore à
faire. Les deux monitorings **restent séparés** (décision utilisateur du 24/09).

---

## ✅ What Worked

### Livré ce 25/09 — **`feat/doublons-prospect-projet`** (7857495, poussée, **PR à merger**)
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
  Spec `instructions/FEATURE-doublons-prospect-projet.md`. `feature-index add` fait.
  PR : https://github.com/TsioryRasoloarijaona/immo-back/compare/main...feat%2Fdoublons-prospect-projet

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

---

## 🔑 Key Points to Remember

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

> **Attendre le merge GitHub de `feat/doublons-prospect-projet`** (PR ci-dessus). Une fois
> confirmé : `node <prototype>/.claude/scripts/git-flow.mjs sync back`, puis **demander** avant
> toute nouvelle branche. Impact front à relayer : 409 `doublons` sur `POST /api/prospects` et
> `creer-prospect` (+ `ignorerDoublons`, `lier-prospect`), 409 `prospectionExistante` sur la
> création / transformation / changement de tranche.

### Backlog
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

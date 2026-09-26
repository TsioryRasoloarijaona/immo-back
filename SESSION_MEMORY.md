# Session Memory
_Last updated: 2026-09-26T15:20+01:00_
_Project: immo-crm (AyadERP) — C:\Users\tsiory\Documents\ayad-internship\prototype\immo-crm_

---

## 🎯 Current Objective

Aligner le cycle de vie des **alertes de relance** sur le légataire (`AdmAlertData`) — code livré
sur `feat/alerte-admalertdata`, PR à merger — puis remettre les données de la source 115 d'équerre
(change tracking sur `AdmAlertData`, script 012, une synchronisation). En arrière-plan : accès API
n8n/MCP, fiabilisation du montage deux-bases sur les deux VPS.

---

## ✅ What Worked

### Livré ce 26/09 (branche `feat/alerte-admalertdata`, commit `51a5d83`, poussée, **PR à merger**)
- **Mécanisme OMRAN reproduit** dans `services/prospection/AlerteRelanceService` :
  `poserAlerte(interaction)` (appelé par `POST /api/prospections/{id}/interactions` **seulement si
  `dateAlerte`**) = `WithoutRevival=1` sur les autres relances de la prospection (`AlertValidityDate
  IS NOT NULL AND ComDocumentId IS NULL AND IsUnpaid=0 AND PKey<>nouvelle`) + `AdmAlertData.Active=0`
  sur les alertes des messages sans statut de dossier + `INSERT AdmAlertData` (Active=1).
  `desarmerToutes(prospectionId)` = ordres 1 et 2, utilisé par `DossierService.create`
  (transformation) et `archiver`.
- Entité `entities/adm/AdmAlertData` (clé `Pkey`, k minuscule ; constantes `Alerte`, `Prospection`,
  `RightKey = 69f8e7d6-1111-1111-1111-1aa8b1f02008`), `AdmAlertDataRepository.alertesActivesDeLaProspection`
  (native, `StrFolderStatusId IS NULL AND ComDocumentId IS NULL AND Active=1`).
- Désarmement **entité par entité** (pas d'`UPDATE` de masse) pour passer par
  `CollecteurLignesEcrites` ; `AdmAlertData` ajoutée à `ORDRE_CLES_ETRANGERES` après `ComActionMessage`.
- `dernieresAlertes` (liste `GET /api/prospections`) filtre `ISNULL(WithoutRevival,0)=0`.
- Test `ProspectionInteractionAlerteTest` (7 cas) ; suite complète **1034 verts**.
- Script `db/migration/012_AdmAlertData_rattrapage_api.sql` (idempotent, essai à blanc OK sur le
  bac à sable local `yesRems` : 7 prospections, 3 WR, 0 Active→0, 10 créées — chiffres locaux).
- Spec `instructions/FEATURE-alerte-admalertdata.md`.

### Méthode qui a payé
- **Trace d'audit locale `C:\backup\*.sqlaudit`** (pas `C:\backups`) lue via
  `sys.fn_get_audit_file('C:\backup\*.sqlaudit', DEFAULT, DEFAULT)` sur `localhost,49687`, filtre
  `action_id IN ('UP','IN','DL')` — donne les ordres exacts d'OMRAN. Le rejouer en direct dans
  l'OMRAN local (prospection `26/05941`, 14:33 / 14:35 / 14:37) a **précisé** la règle.
- Cross-tab sur la source 115 pour valider une hypothèse avant de coder (Active × action
  postérieure avec/sans alerte).
- Essai à blanc d'un script : remplacer `COMMIT` par `ROLLBACK` via `sed` (garder les quotes du
  `PRINT` !) ; les `DROP TABLE #tmp` finaux doivent être conditionnels.

---

## ❌ What Failed

- Lecture SSH du VPS dev (`ssh vps-db …`) refusée par le classifieur auto-mode (« Production
  Reads ») → impossible de compter `AdmAlertData` sur la copie ; à faire par l'utilisateur.
- `PRINT '…' + CAST((SELECT COUNT(*) …))` : sous-requête interdite dans `PRINT` → variable.
- `LEFT(colonne text, n)` échoue : `CAST(... AS nvarchar(max))` d'abord (`AdmWebHistorics.Details`).
- Littéral `'2026-09-26 14:30'` ambigu sous locale FR → `'20260926 14:30:00'`.

---

## 🐛 Bugs & Errors Log

| # | Erreur / Bug | Statut | Correctif |
|---|---|---|---|
| 29 | Pipeline n8n à l'arrêt depuis le 08/09 | En cours | flux HTTP + clé d'API |
| 30 | Heure de RDV non captée (front) | Open (front) | |
| 46 | `26/05941` en double dans `yesRemsProd` (et `50` en double sur 115) | Open (hors périmètre) | compteur `AdmSequence` |
| 50 | Écritures App* échouées masquées en **401 nu** (dispatch ERROR) | Open (code) | `dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()` — branche à demander |
| 51 | Mots de passe client possiblement remplacés par ceux du dev | Open (à confirmer) | reset via `POST /api/utilisateurs/{id}/reinitialiser-mot-de-passe` |
| 52 | `nahoma` en double dans les copies → `findByCode` 2 lignes | Open | désactiver/renommer dans OMRAN |
| 53 | 91 `AdmUserHabilitation` + 1 `AdmUserRole` seulement dans les copies | Open | pousser vers la source (bcp) |
| 54 | Ordre de fusion `AdmUserRole`/`Habilitation` avant `AdmUser` | Open | `AdmUser` en tête de `ORDRE_CLES_ETRANGERES` |
| 55 | Mot de passe `sasa` et clé SSH transités dans le chat | Open | rotation `sasa` |
| 56 | Alertes API invisibles dans OMRAN / jamais éteintes (pas d'`AdmAlertData`, `dernieresAlertes` sans filtre) | **Fix livré** (PR à merger) | `AlerteRelanceService` + script 012 |
| 57 | `AdmAlertData` **pas sous change tracking** sur la source → copies VPS figées à la copie initiale pour toutes les alertes OMRAN | **Open** (utilisateur) | `ALTER TABLE dbo.AdmAlertData ENABLE CHANGE_TRACKING WITH (TRACK_COLUMNS_UPDATED = OFF)` sur 115 |

---

## 🔑 Key Points to Remember

- **Règle des alertes OMRAN** : c'est la **prochaine interaction porteuse d'une date d'alerte**
  qui éteint la précédente. Une note sans alerte ne touche à rien ; la date de l'alerte ne joue
  pas ; rien n'éteint par le temps (3 666 alertes échues encore actives). `Active=0` ⇔
  `WithoutRevival=1` (33 416 / 33 530). L'écran lit `AdmAlertData` via
  `ComSaleProspectionView.LastActiveAlertDate` (`Active=1 AND AlertDate <= GETDATE()+DefaultAlertDelai(0)`).
- `AdmAlertData` : 1 ligne par message (`RelatedKey`), `Code = Pkey` minuscules, `Label` nul,
  `Description='Alerte'`, `Groupping='Prospection'`, aucun trigger/procédure, pas de FK.
- Variante OMRAN `Sorting <= 1` vue une fois (11/09) — non reproduite.
- Toute nouvelle table écrite par JPA doit exister sur la source **et** être suivie par CT
  (sinon 401 nu / copies figées). `ORDRE_CLES_ETRANGERES` à compléter à chaque nouvelle entité.
- Un `@Modifying` JPQL échappe au collecteur Hibernate → préférer les entités quand la copie
  doit voir l'écriture immédiatement.
- Mode de travail : backend seulement ; git via `node <prototype>/.claude/scripts/git-flow.mjs`
  (`start`/`commit`/`push`/`sync back`) ; demander avant toute branche ; commit + push dès que
  vert ; attendre le merge.

---

## 📁 Important Files & Paths

- `instructions/FEATURE-alerte-admalertdata.md` — spec, mesures, ordre des opérations données.
- `services/prospection/AlerteRelanceService.java`, `entities/adm/AdmAlertData.java`,
  `repositories/adm/AdmAlertDataRepository.java`, `ComActionMessageRepository.dernieresAlertes`.
- `src/main/resources/db/migration/012_AdmAlertData_rattrapage_api.sql` — à appliquer sur 115
  **après** le CT.
- `src/test/java/.../prospection/ProspectionInteractionAlerteTest.java`.
- `config/CollecteurLignesEcrites` (`ORDRE_CLES_ETRANGERES`), `config/SynchronisationDescendante`
  (`tablesSuivies()` lit `sys.change_tracking_tables` de la source).
- Trace d'audit OMRAN local : `C:\backup\Audit_Global_*.sqlaudit`.
- Autres specs vivantes : `FEATURE-cle-api-n8n.md` (§6 MCP à faire), `FEATURE-source-de-verite-115.md`.

---

## 🔧 Environment & Config

- Source 115 : `192.168.11.115` = `197.230.172.136:1433`, base `YesRems`, compte `sasa`
  (mot de passe connu de l'utilisateur, non consigné) — CT ON sur 27 tables, **pas `AdmAlertData`**.
  `AdmAlertData` = 39 599 lignes (4 876 actives) le 26/09.
- Local : `localhost,49687` (`tsix`) — `yesRems` = bac à sable de l'OMRAN local (audité),
  `yesRemsProd` = banc lecture seule, `yesRemsTest` = tests (a `AdmAlertData`, vide).
- Copies VPS : `yesRemsDev` ; tunnels `vps-db` (dev) / `vps-ayad-db` (client) — lecture SSH
  bloquée pour l'agent, commandes à faire taper par l'utilisateur avec `!`.
- Tests : 1034 verts (`./mvnw.cmd test`, ~10 min).

---

## ⏭️ Next Step

> **Faire merger la PR `feat/alerte-admalertdata`**
> (https://github.com/TsioryRasoloarijaona/immo-back/compare/main...feat%2Falerte-admalertdata?expand=1),
> puis `sync back`. Ensuite, par l'utilisateur sur la source 115, dans l'ordre :
> 1) `ALTER TABLE dbo.AdmAlertData ENABLE CHANGE_TRACKING WITH (TRACK_COLUMNS_UPDATED = OFF);`
> 2) `sqlcmd … -d YesRems -b -i src/main/resources/db/migration/012_AdmAlertData_rattrapage_api.sql`
> (attendu : 9 prospections, 8 WR, 4 Active→0, 12 créées) ; 3) laisser passer **un** cycle de
> synchronisation et vérifier `SELECT COUNT(*) FROM AdmAlertData` sur une copie (= 39 611).

### Backlog
- [ ] Branche à demander : 401 nu → 500 propre (`DispatcherType.ERROR` permitAll) + `AdmUser`
      en tête de `ORDRE_CLES_ETRANGERES` + annoter `FEATURE-source-de-verite-115.md`.
- [ ] Bug 51 (mots de passe), 53 (habilitations vers la source), 52 (`nahoma` en double).
- [ ] Rotation `sasa` ; nettoyage clé SSH VPS client ; supprimer Environment `vps_ayad`.
- [ ] n8n : nœud HTTP avec `X-Api-Key` ; MCP : `POST /api/auth/jeton` (spec §6).
- [ ] Réimporter `grafana-vue-generale.json` dans les deux Grafana.
- [ ] Bug 46 (`AdmSequence`), bug 30 (front).

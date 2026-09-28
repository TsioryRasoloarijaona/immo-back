# Session Memory
_Last updated: 2026-09-28T01:10+01:00_
_Project: immo-crm (AyadERP) — C:\Users\tsiory\Documents\ayad-internship\prototype\immo-crm_

---

## 🎯 Current Objective

Parité OMRAN sur la **prospection**. Livré et en production : le cycle de vie des alertes de
relance, les relances en retard du tableau de bord limitées aux prospections du mois, le filtre
d'archivage à trois états. Reste : les écarts de l'audit face à l'aide en ligne du légataire, le
fuseau horaire du serveur, la dérive des copies VPS par rapport à la source.

---

## ✅ What Worked

### Livré, mergé et déployé (26 → 28/09/2026)
- **PR #89 `feat/alerte-admalertdata`** — `AlerteRelanceService` : une interaction avec `dateAlerte`
  désarme les relances précédentes (`WithoutRevival=1`, `AdmAlertData.Active=0`) et pose sa ligne
  `AdmAlertData` ; transformation et archivage éteignent tout ; `dernieresAlertes` filtre
  `WithoutRevival`. Avec : référence de l'aide en ligne (`REFERENCE-aide-omran.md`, 249/334 pages),
  audit prospection, scripts de données 012 et 013.
- **PR #90 / #91 `feat/dashboard-relances-du-mois`** :
  - `GET /api/dashboard/relances?echeance=RETARD` = relances échues des prospections
    **qualifiées, ouvertes, créées dans le mois civil en cours** (remplace la fenêtre glissante de
    30 jours). `AUJOURDHUI` inchangé.
  - `ouvertes` **ternaire** sur les 4 endpoints (listes et groupes, prospections et
    pré-prospections) : absent = tout, `true` = ouvertes, `false` = archivées.
- Suite de tests : **1 045 verts**.

### Données corrigées sur la source 115
- Change tracking activé sur `AdmAlertData` (28 tables suivies).
- Script `012` (12 alertes API sans ligne `AdmAlertData`) et `013` (583 alertes actives sur
  prospections fermées) **appliqués par l'utilisateur** : 4 891 → 4 308 alertes actives.
- 4 relances périmées dans la copie de production « touchées » sur la source avec l'accord de
  l'utilisateur (`UPDATE … SET UserUpdateDateTime = UserUpdateDateTime`) → propagées en < 2 min.

### Vérifié en production (`api.ayaddeveloppement.com`, compte de la commerciale)
- `RETARD MOI` = 43 = les 43 alertes en retard de sa liste du mois, mêmes prospections.
- `ouvertes=true` 67 / `false` 17 / absent 84 (67 + 17) ; groupes cohérents avec la liste ;
  pré-prospections 63 / 1 / 64.

### Méthode qui a payé
- Trace d'audit locale `C:\backup\*.sqlaudit` (`sys.fn_get_audit_file`, `action_id IN
  ('UP','IN','DL')`) + rejeu en direct dans l'OMRAN local pour préciser une règle.
- **Croiser un endpoint avec la source** : appeler l'API, charger les ids dans une table temporaire
  sur la source, classer chaque ligne. C'est ce qui a montré les 4 lignes périmées de la copie.
- **Chiffrer chaque lecture possible d'une demande avant de coder** (« en retard ce mois » :
  45 / 68 / 81) puis faire trancher l'utilisateur.
- Essai à blanc d'un script : `sed` de `COMMIT` en `ROLLBACK` sur le bac à sable local `yesRems`.
- Attendre une suite de tests lancée en tâche de fond : `until grep -q "^exit=" log; do sleep 5; done`.

---

## ❌ What Failed

- **Téléchargement parallèle du site d'aide** (`xargs -P 8`) → le serveur `help.omranestate.com`
  est tombé après 249 pages, toujours hors service le 28/09. `fetch.sh` est séquentiel.
- Processus lancé avec `( … &)` dans un appel Bash → meurt avec l'appel ; utiliser
  `run_in_background: true`.
- `sleep N` en tête de commande → bloqué par l'outil ; boucle `until` ou Monitor.
- Lecture SSH des VPS → refusée par le classifieur ; les copies ne sont accessibles que par
  l'API ou par l'utilisateur.
- `/actuator/info` en production → 401 sans session ; vérifier la version par `/api-docs`
  (chercher une phrase de la nouvelle documentation) et par `gh run list`.
- SQL : `PRINT` refuse une sous-requête ; une CTE ne vaut que pour l'instruction suivante ;
  `GROUP BY` refuse une sous-requête ; `LEFT()` refuse le type `text` ; littéral de date ambigu
  sous locale FR (`'20260926 14:30:00'`).
- Écriture sur `main` → bloquée par le hook ; toute écriture, même `SESSION_MEMORY.md`, demande
  une branche et donc l'accord de l'utilisateur.

---

## 🐛 Bugs & Errors Log

| # | Erreur / Bug | Statut | Correctif |
|---|---|---|---|
| 29 | Pipeline n8n à l'arrêt depuis le 08/09 | En cours | flux HTTP + clé d'API |
| 30 | Heure de RDV non captée (front) | Open (front) | |
| 46 | Codes de prospection en double (`26/05941`, `50`) | Open | compteur `AdmSequence` |
| 50 | Écritures App* échouées masquées en 401 nu | Open (code) | `DispatcherType.ERROR` permitAll |
| 51 | Mots de passe client possiblement remplacés par ceux du dev | Open (à confirmer) | |
| 52 | `nahoma` en double dans les copies | Open | |
| 53 | 91 habilitations + 1 rôle seulement dans les copies | Open | pousser vers la source |
| 54 | `AdmUser` absent de la tête de `ORDRE_CLES_ETRANGERES` | Open | |
| 55 | Mots de passe transités dans le chat (compte SQL de la source, compte applicatif d'une commerciale) | Open | rotation |
| 56 | Alertes API invisibles dans OMRAN / jamais éteintes | **Fixed** (PR #89) | `AlerteRelanceService` |
| 57 | `AdmAlertData` hors change tracking | **Fixed** (26/09) | CT activé |
| 58 | 583 alertes actives sur prospections fermées | **Fixed** (27/09) | script 013 |
| 59 | Copies VPS en retard sur la source pour les modifications antérieures au CT (étendue inconnue) | **Open** | comparer l'empreinte des copies à la source, toucher les lignes divergentes |
| 60 | `/api/commerciaux` filtre par kind au lieu de `CfgTier.IsVisibleInList` | Open (audit 1.5) | |
| 61 | Interaction : règles GEN0005 non appliquées (résultat, message, date d'alerte) | Open (audit 2.2, 2.3) | |
| 62 | Alerte acceptée sur une prospection fermée | Open | refuser dans `addInteraction` (le tableau de bord, lui, est protégé) |
| 63 | Site d'aide OMRAN hors service, 85 pages manquantes | Open (externe) | relancer `fetch.sh` |
| 64 | Tableau de bord 81 vs liste 46 | **Fixed** (PR #90) | `RETARD` = prospections du mois |
| 65 | `ouvertes=false` renvoyait toute la liste | **Fixed** (PR #91) | prédicat ternaire |
| 66 | **Le serveur calcule « aujourd'hui » en UTC** : entre 00:00 et 01:00 au Maroc le tableau de bord a un jour de retard, y compris au changement de mois | **Open** | `LocalDate.now(ZoneId.of("Africa/Casablanca"))` via une horloge injectée |

---

## 🔑 Key Points to Remember

- **Règle des alertes** (aide GEN0005 + trace + test en direct) : c'est la prochaine interaction
  **porteuse d'une date d'alerte** qui éteint la précédente. Une note sans alerte n'éteint rien ;
  la date ne joue pas ; rien n'expire. Archivage et passage en dossier éteignent tout ; impayés
  épargnés. L'écran OMRAN lit `AdmAlertData.Active`.
- **OMRAN laisse poser une alerte sur une prospection archivée** et ne l'éteint jamais.
- **Tableau de bord et liste comptent la même population** depuis le 28/09 : prospections
  qualifiées, ouvertes, dont `DocDate` est dans le mois civil. Conséquence assumée : au 1er du
  mois, les retards des prospections du mois précédent sortent du tableau de bord.
- **La liste OMRAN et la liste du front filtrent par défaut sur le mois en cours** — d'où « 68
  prospections » affichées pour une commerciale qui en a 127 ouvertes.
- **`REFERENCE-aide-omran.md` est le premier endroit où chercher une règle d'écran** ; la base et
  la trace d'audit tranchent en cas d'écart.
- **Les copies ne reçoivent que ce qui change après l'activation du CT sur la table.**
- Écritures sur la source : seulement sur accord explicite de l'utilisateur, en transaction, avec
  contrôle du nombre de lignes. Les scripts versionnés restent appliqués par lui.
- `IsClosed` à `NULL` = ouverte (937 lignes réelles), jamais « archivée ».
- Mode de travail : backend seulement ; git via `node <prototype>/.claude/scripts/git-flow.mjs` ;
  **demander avant toute branche** ; commit + push dès que vert ; attendre le merge puis `sync`.

---

## 📁 Important Files & Paths

- `instructions/REFERENCE-aide-omran.md` + `instructions/aide-omran/` — aide en ligne extraite et outils.
- `instructions/AUDIT-prospection-aide-omran.md` — les 7 écarts à corriger, classés.
- `instructions/FEATURE-alerte-admalertdata.md` — alertes, étapes de données 1 à 4.
- `instructions/FEATURE-dashboard-relances.md` — §3 bis (décision du 28/09) et §7 (impact front).
- `instructions/from_front.md` — demande 15 (`ouvertes`) classée en « Traitées ».
- `services/prospection/AlerteRelanceService.java`, `entities/adm/AdmAlertData.java`.
- `services/dashboard/RelanceDashboardService.java` (`prospectionDuMois`), `domain/EcheanceRelance.java`.
- `services/prospection/ProspectionService.java` (`buildSpec`, filtre `ouvertes`),
  `DiscriminantPreProspection.java` (prédicat pour une jointure).
- Tests : `ProspectionInteractionAlerteTest`, `DashboardRelancesTest` (19), `ProspectionFiltreOuvertesTest` (6).
- `db/migration/012_…` et `013_…` — déjà appliqués sur la source.
- `services/prospect/CommercialService.java` (`COMMERCIAL_KIND_CODES`, à remplacer).

---

## 🔧 Environment & Config

- Source 115 : `197.230.172.136:1433`, base `YesRems` — CT sur 28 tables. `AdmAlertData` :
  39 611 lignes, 4 308 actives. Horloge en heure du Maroc.
- Production : `https://api.ayaddeveloppement.com`, connexion `POST /api/auth/connexion`, cookie
  de session. Serveur en **UTC**. Déploiement automatique au merge (« Build et déploiement »),
  quelques minutes. Synchronisation descendante < 2 min.
- Identifiants : fournis par l'utilisateur en séance, non consignés ici.
- Local : `localhost,49687` — `yesRems` (bac à sable de l'OMRAN local, audité), `yesRemsProd`
  (banc lecture seule), `yesRemsTest` (tests).
- Tests : 1 045 verts, environ 10 minutes.

---

## ⏭️ Next Step

> **Demander une branche pour le lot « fuseau et règles de saisie prospection »**, en commençant
> par le bug 66 : calculer « aujourd'hui » dans le fuseau `Africa/Casablanca` pour le tableau de
> bord (`RelanceDashboardService`, nouveaux pré-prospects), avec une horloge injectable pour tester
> le passage de minuit et le changement de mois.

### Backlog
- [ ] Bug 60 : commerciaux par `IsVisibleInList`.
- [ ] Bugs 61 et 62 : règles GEN0005 de l'interaction, refus d'une alerte sur prospection fermée,
      refus d'une `dateAlerte` antérieure à l'interaction.
- [ ] Audit : actions commerciales actives, alertes futures dans la liste native, articles
      « prospectables », doc du `POST /api/prospections`, colonnes de liste manquantes.
- [ ] Décider si `AUJOURDHUI` doit suivre la même population que `RETARD`.
- [ ] Bug 59 : empreinte des copies vs source, puis toucher les lignes divergentes.
- [ ] Compléter la référence quand le site d'aide revient, puis `/graphify . --update`.
- [ ] 3 alertes de prospect (tiers 26/14437, 26/14438, 26/14440) encore actives.
- [ ] Branche : 401 nu → 500 propre + `AdmUser` en tête de `ORDRE_CLES_ETRANGERES`.
- [ ] Bugs 51, 52, 53 ; rotation des mots de passe du bug 55.
- [ ] n8n (nœud HTTP `X-Api-Key`) ; MCP (`POST /api/auth/jeton`, spec §6).

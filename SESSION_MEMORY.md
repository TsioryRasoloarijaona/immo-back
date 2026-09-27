# Session Memory
_Last updated: 2026-09-27T23:45+01:00_
_Project: immo-crm (AyadERP) — C:\Users\tsiory\Documents\ayad-internship\prototype\immo-crm_

---

## 🎯 Current Objective

Parité OMRAN sur la **prospection** : le cycle de vie des alertes de relance est livré
(`feat/alerte-admalertdata`, **PR à merger**), les données de la source 115 sont remises d'équerre,
et un audit liste les sept écarts restants face à l'aide en ligne du légataire. En arrière-plan :
dérive des copies VPS par rapport à la source, accès API n8n/MCP.

---

## ✅ What Worked

### Branche `feat/alerte-admalertdata` (poussée, PR non mergée) — 6 commits
- `51a5d83` **Mécanisme OMRAN des alertes** : `AlerteRelanceService.poserAlerte` (seulement si
  `dateAlerte`) = `WithoutRevival=1` sur les autres relances + `AdmAlertData.Active=0` + `INSERT
  AdmAlertData` ; `desarmerToutes` pour transformation et archivage ; entité `AdmAlertData` ;
  `dernieresAlertes` filtre `WithoutRevival` ; 7 tests ; suite **1034 verts**.
- `a2dfefa` **Référence de l'aide en ligne** `instructions/REFERENCE-aide-omran.md` (249 pages sur
  334, mot pour mot) + outils `instructions/aide-omran/` + section dans `CLAUDE.md`.
- `bee8481` **Audit** `instructions/AUDIT-prospection-aide-omran.md` : règles prospection du
  backend vs site, 7 écarts classés.
- `b3f440a` **Script 013** (prospections fermées) + note dans la spec.
- Scripts de données **appliqués par l'utilisateur sur la source 115** :
  - CT activé sur `AdmAlertData` (version de départ 338) ;
  - `012` (26/09) : 9 prospections, 8 WR, 4 Active→0, 12 lignes créées — vérifié ;
  - `013` (27/09) : 582 WR, 583 alertes éteintes, **4 891 → 4 308 actives**, 0 alerte active sur
    prospection fermée — vérifié.

### Vérifications faites le 27/09
- **Khadija ELKORRI** (fiche `26/14435`, compte `Khadija`) : 221 alertes, toutes cohérentes ; 149
  prospections affectées dont **127 ouvertes** (OMRAN en affiche 68 = celles de septembre, confirmé
  correct par l'utilisateur) ; 93 alertes actives échues.
- **`GET /api/dashboard/relances`** appelé sur `api.ayaddeveloppement.com` avec son compte : logique
  conforme (82 retards sur 30 jours = les 82 alertes actives d'OMRAN). La production en servait 4
  de trop : lignes désarmées sur la source avant l'activation du CT, jamais propagées à la copie.
  **Touchées sur la source** (`UPDATE … SET UserUpdateDateTime = UserUpdateDateTime`, 4 lignes, avec
  l'accord de l'utilisateur) → API retombée à 81 (MOI) / 82 (EQUIPE) en moins de 2 minutes.

### Méthode qui a payé
- Trace d'audit locale `C:\backup\*.sqlaudit` via `sys.fn_get_audit_file` (`action_id IN
  ('UP','IN','DL')`) + rejeu en direct dans l'OMRAN local pour préciser une règle.
- Croiser un endpoint avec la source : appeler l'API, charger les ids dans une table temporaire
  sur la source, classer chaque ligne (`identique` / `désarmée sur la source` / `hors fenêtre`).
- Essai à blanc d'un script : `sed` de `COMMIT` en `ROLLBACK` sur le bac à sable local `yesRems`.
- Site HelpNDoc : liste des pages dans `js/hndsd.min.js` (`aTl`), hiérarchie dans `_toc.json`.

---

## ❌ What Failed

- **Téléchargement parallèle du site d'aide** (`xargs -P 8`) : le serveur `help.omranestate.com`
  est tombé (connexion refusée sur 443 et 80, aussi depuis un autre réseau, absent des archives
  web) après 249 pages. Toujours hors service le 27/09. `fetch.sh` est désormais séquentiel.
- Processus lancé avec `( … &)` dans un appel Bash : meurt avec l'appel. Utiliser
  `run_in_background: true`.
- `sleep` en avant-plan bloqué par l'outil : passer par Monitor ou une tâche de fond.
- Lecture SSH des VPS refusée par le classifieur (« Production Reads ») : les copies ne sont
  pas interrogeables par l'agent, seulement via l'API ou par l'utilisateur.
- SQL : `PRINT` n'accepte pas de sous-requête ; une CTE ne vaut que pour l'instruction suivante ;
  `GROUP BY` refuse une sous-requête (passer par une CTE) ; `LEFT()` refuse le type `text`.

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
| 55 | Mots de passe transités dans le chat (`sasa`, compte Khadija) | Open | rotation |
| 56 | Alertes API invisibles dans OMRAN / jamais éteintes | **Fix livré**, PR à merger | `AlerteRelanceService` |
| 57 | `AdmAlertData` hors change tracking | **Fixed** (26/09) | CT activé par l'utilisateur |
| 58 | 583 alertes actives sur prospections fermées (résidu OMRAN) | **Fixed** (27/09) | script 013 |
| 59 | **Copies VPS en retard sur la source** pour les modifications antérieures à l'activation du CT (4 relances vues via l'API, corrigées ; étendue réelle inconnue) | **Open** | comparer l'empreinte des copies à celle de la source, puis toucher les lignes divergentes |
| 60 | `/api/commerciaux` filtre par kind au lieu de `CfgTier.IsVisibleInList` (21 tiers en trop, 1 manquant) | Open (audit 1.5) | |
| 61 | Interaction : résultat/message/date d'alerte obligatoires non conformes à GEN0005 | Open (audit 2.2, 2.3) | |
| 62 | Rien n'empêche de poser une alerte sur une prospection fermée (460 cas OMRAN) | Open | refuser dans `addInteraction` + `IsClosed = 0` dans le prédicat des relances |
| 63 | Site d'aide OMRAN hors service ; 85 pages manquantes dans la référence | Open (externe) | relancer `fetch.sh` quand il revient |

---

## 🔑 Key Points to Remember

- **Règle des alertes** (site GEN0005 + trace + test en direct) : c'est la prochaine interaction
  **porteuse d'une date d'alerte** qui éteint la précédente. Une note sans alerte n'éteint rien ;
  la date ne joue pas ; rien n'expire. Archivage et passage en dossier éteignent tout. Impayés
  épargnés. L'écran lit `AdmAlertData.Active`, pas `AlertValidityDate`.
- **OMRAN laisse poser une alerte sur une prospection archivée** et ne l'éteint jamais — source
  des 460 alertes nettoyées. Notre API doit le refuser.
- **`REFERENCE-aide-omran.md` est le premier endroit où chercher une règle d'écran** ; la base et
  la trace d'audit tranchent en cas d'écart. Ne jamais regénérer en parallèle.
- **Les copies ne reçoivent que ce qui change après l'activation du CT sur la table.** Une ligne
  modifiée avant reste périmée jusqu'à ce qu'on la « touche » sur la source.
- L'endpoint des relances ne lit que `WithoutRevival` : juste tant que les données le sont.
- La fenêtre `RETARD` de 30 jours et l'unicité par projet sont des choix produit, pas des règles
  d'OMRAN (voir l'audit, section « plus strict »).
- Khadija a deux fiches tiers : `26/14435` (COM, visible, la bonne) et `26/14436` (doublon vide).
- Écritures sur la source : **seulement sur accord explicite de l'utilisateur**, en transaction,
  avec contrôle du nombre de lignes. Les scripts versionnés restent appliqués par lui.
- Mode de travail : backend seulement ; git via `node <prototype>/.claude/scripts/git-flow.mjs` ;
  demander avant toute branche ; commit + push dès que vert ; attendre le merge.

---

## 📁 Important Files & Paths

- `instructions/REFERENCE-aide-omran.md` — l'aide en ligne extraite (249/334 pages).
- `instructions/aide-omran/` — `fetch.sh`, `convert.py`, `assemble.py`, `README.md`.
- `instructions/AUDIT-prospection-aide-omran.md` — écarts prospection, classés.
- `instructions/FEATURE-alerte-admalertdata.md` — spec, mesures, étapes de données 1 à 4.
- `services/prospection/AlerteRelanceService.java`, `entities/adm/AdmAlertData.java`,
  `repositories/adm/AdmAlertDataRepository.java`.
- `db/migration/012_AdmAlertData_rattrapage_api.sql`, `013_AdmAlertData_prospections_fermees.sql`
  — tous deux **déjà appliqués** sur la source.
- `services/dashboard/RelanceDashboardService.java`, `domain/EcheanceRelance.java`.
- `services/prospect/CommercialService.java` (`COMMERCIAL_KIND_CODES`, à remplacer).

---

## 🔧 Environment & Config

- Source 115 : `197.230.172.136:1433`, base `YesRems`, compte `sasa` — CT sur 28 tables
  (dont `AdmAlertData`), version 371 le 27/09 à 23:37. `AdmAlertData` : 39 611 lignes, 4 308 actives.
- Empreinte des relances sur la source le 27/09 (avant la touche des 4 lignes, qui ne la change
  pas) : 37 807 alertes, 34 576 désarmées, `CHECKSUM_AGG` = -1432723714.
- Production : `https://api.ayaddeveloppement.com`, connexion `POST /api/auth/connexion`
  (`username`, `password`), cookie de session. Synchronisation descendante < 2 min constatée.
- Local : `localhost,49687` — `yesRems` (bac à sable de l'OMRAN local, audité), `yesRemsProd`
  (banc lecture seule), `yesRemsTest` (tests).
- Tests : 1034 verts, environ 10 minutes.

---

## ⏭️ Next Step

> **Faire merger la PR `feat/alerte-admalertdata`**
> (https://github.com/TsioryRasoloarijaona/immo-back/compare/main...feat%2Falerte-admalertdata?expand=1),
> puis `sync back`. Ensuite demander une branche pour le lot « règles de saisie prospection » :
> commerciaux par `IsVisibleInList`, règles GEN0005 de l'interaction, refus d'une alerte sur
> prospection fermée, `IsClosed = 0` dans le prédicat des relances, actions commerciales actives.

### Backlog
- [ ] Bug 59 : faire lancer par l'utilisateur la requête d'empreinte sur les deux copies, comparer
      à la source, puis lister et toucher les lignes divergentes.
- [ ] Compléter la référence quand le site d'aide revient (85 pages), puis `/graphify . --update`.
- [ ] Reste de l'audit : alertes futures dans la liste native, articles « prospectables », doc du
      `POST /api/prospections`, colonnes de liste manquantes.
- [ ] 3 alertes de prospect (tiers 26/14437, 26/14438, 26/14440) encore actives : à éteindre si
      l'utilisateur le souhaite (SQL fourni le 27/09).
- [ ] Branche : 401 nu → 500 propre + `AdmUser` en tête de `ORDRE_CLES_ETRANGERES`.
- [ ] Bugs 51, 52, 53 ; rotation `sasa` et mot de passe de Khadija ; nettoyage SSH.
- [ ] n8n (nœud HTTP `X-Api-Key`) ; MCP (`POST /api/auth/jeton`, spec §6).

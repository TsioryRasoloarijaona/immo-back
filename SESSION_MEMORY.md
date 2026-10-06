# Session Memory
_Last updated: 2026-10-06_
_Project: immo-crm (AyadERP) — C:\Users\tsiory\Documents\ayad-internship\prototype\immo-crm_

---

## 🎯 Current Objective

**06/10 — `feat/dashboard-filtre-tranche`** (PR à fusionner) : paramètre `trancheId` réservé à l'administrateur (403 sinon, `services/tranche/FiltreTrancheAdministrateur`) sur `GET /api/dashboard`, `/relances`, `/annuel` et `GET /api/clients`. Appartenance à une tranche centralisée dans `services/tranche/AppartenanceTranche` (dossiers, prospections, clients). Spec : `instructions/FEATURE-dashboard-filtre-tranche.md`.

Parité OMRAN sur la **prospection**. Livré et en production : le cycle de vie des alertes de
relance, les relances en retard du tableau de bord limitées aux prospections du mois, le filtre
d'archivage à trois états. 18 leads Meta de la campagne Ferme Bretonne ont été importés en
pré-prospections par l'API. Reste : le CSV au format d'import demandé par l'utilisateur, les
écarts de l'audit, le fuseau horaire du serveur, la dérive des copies VPS.

---

## ✅ What Worked

### Livré, mergé et déployé (26 → 28/09/2026)
- **PR #89** — `AlerteRelanceService` (alertes posées et éteintes comme OMRAN), référence de
  l'aide en ligne, audit prospection, scripts de données 012 et 013.
- **PR #90 / #91** — `GET /api/dashboard/relances?echeance=RETARD` = relances échues des
  prospections qualifiées, ouvertes, créées dans le mois civil ; `ouvertes` ternaire sur les
  4 endpoints de liste et de groupes.
- **PR #92** — mémoire de session.
- Suite de tests : **1 045 verts**. Vérifié en production : `RETARD MOI` = 43 = la liste ;
  `ouvertes` true 67 / false 17 / absent 84.

### Import de leads Meta en production (28/09, à la demande de l'utilisateur)
- Fichier collé : 32 leads de l'export **Meta Lead Ads** (`id, created_time, …, platform,
  réponses du formulaire, nom_complet, full_name, email, phone`), campagne « LEADS | AYAD PRIVÉ |
  FERME BRETONNE », formulaire `FORM | FERME BRETONNE | PROSPECT | V1`.
- **14 étaient déjà en base** (même contact, même date à la minute, du 21/09 08:18 au 23/09
  08:32) → non réinsérés. **18 créés** : codes `26/06009` à `26/06026`.
- Voie utilisée : `POST /api/pre-prospections/import/apercu` (n'écrit rien) puis
  `POST /api/pre-prospections/import?trancheId=<APF>` en multipart, avec le compte d'une
  commerciale. Résultat : pré-prospections ouvertes, tranche APF, source FACEBOOK, nature
  `1 A TRAITER`, type WEB, vocation VENTE.
- Notes ensuite **reformatées au format n8n** par `PATCH /api/prospections/{id}` (`notes`),
  une information par ligne : `Source`, `Projet`, `Acquisition`, `Type de bien`, `Contact par
  téléphone`, `Ville`, `Nom saisi`, `Canal`, `Annonce`, `Lead Meta`, `Importé via l'API le …`.
  18 sur 18 vérifiées par relecture et sur la source.

### Données corrigées sur la source 115
- CT activé sur `AdmAlertData` (28 tables suivies) ; scripts 012 et 013 appliqués par
  l'utilisateur (4 891 → 4 308 alertes actives) ; 4 relances périmées « touchées ».

### TRIBECA (01/10) — ce qui rend une tranche visible dans l'application
- TRIBECA = tranche `ART` (AYAD RESIDENCE TRIBECA), projet `ART`, société **MAYLINE** ;
  `IsForSale = 1`, 80 articles, 26 dossiers, 3 794 prospections (64 en septembre).
- La visibilité dans l'application = `CfgCompany.IsActive` de la société, lu sur la **copie**
  (référentiels Projet/Tranche sans filtre de périmètre). `CfgCompany` n'est pas sous CT :
  chaque copie garde sa valeur. Sur la source, les 9 sociétés sont à 1 (script 005 jamais joué
  là, et c'est voulu : OMRAN pourrait lire le drapeau).
- Production : la liste des tranches ne renvoie que ADMOD + GHRIZA → MAYLINE à 0 sur la copie.
  Activer = `UPDATE dbo.CfgCompany SET IsActive = 1 WHERE Code = 'MAYLINE'` sur chaque copie.
- 21 utilisateurs sont habilités sur ART (dont Faycal SAOUD, Macire Kamara) ; Khadija ne l'est pas.

### Méthode qui a payé
- **Aperçu avant écriture** : tout import passe d'abord par `/import/apercu`.
- **Chercher l'existant avant d'insérer** : jointure des leads sur `TempTel` (9 derniers
  chiffres) et `TempEMail`, puis comparaison de `DocDate` à la minute.
- **Calquer le format sur les lignes déjà en base** de la même campagne plutôt que l'inventer.
- Croiser un endpoint avec la source (ids en table temporaire) ; chiffrer chaque lecture
  possible d'une demande avant de coder ; trace d'audit locale + rejeu en direct dans OMRAN.
- Console Windows : `PYTHONIOENCODING=utf-8` pour afficher accents et arabe ; `sqlcmd -f 65001`.

---

## ❌ What Failed

- Téléchargement parallèle du site d'aide → serveur `help.omranestate.com` tombé, 85 pages
  manquantes, toujours hors service.
- `( … &)` dans un appel Bash → le processus meurt avec l'appel ; `run_in_background: true`.
- `sleep N` en tête de commande → bloqué ; boucle `until` ou Monitor.
- Lecture SSH des VPS → refusée ; copies accessibles par l'API ou par l'utilisateur seulement.
- `/actuator/info` en production → 401 ; vérifier la version par `/api-docs` et `gh run list`.
- `sqlcmd -W` avec `-y 0` → options exclusives ; pour lire un champ long, `-y 0` seul, vers un
  fichier.
- `print()` Python d'un texte accentué sur la console cp1252 → `UnicodeEncodeError`.
- Écriture sur `main` → bloquée par le hook ; toute écriture demande une branche, donc l'accord
  de l'utilisateur, **à chaque fois**.
- **Dernière demande interrompue par l'utilisateur** (appel d'outil refusé) : la lecture du
  lecteur CSV pour produire le fichier au format d'import. À reprendre sur son signal.

---

## 🐛 Bugs & Errors Log

| # | Erreur / Bug | Statut | Correctif |
|---|---|---|---|
| 29 | Pipeline n8n à l'arrêt (dernière ligne `n8n_api` le 17/09) | En cours | flux HTTP + clé d'API |
| 30 | Heure de RDV non captée (front) | Open (front) | |
| 46 | Codes de prospection en double (`26/05941`, et `50` sur des milliers de lignes anciennes) | Open | compteur `AdmSequence` |
| 50 | Écritures App* échouées masquées en 401 nu | Open (code) | `DispatcherType.ERROR` permitAll |
| 51 | Mots de passe client possiblement remplacés par ceux du dev | Open (à confirmer) | |
| 52 | `nahoma` en double dans les copies | Open | |
| 53 | 91 habilitations + 1 rôle seulement dans les copies | Open | pousser vers la source |
| 54 | `AdmUser` absent de la tête de `ORDRE_CLES_ETRANGERES` | Open | |
| 55 | Mots de passe transités dans le chat (compte SQL de la source, compte applicatif d'une commerciale) | Open | rotation |
| 56 | Alertes API invisibles dans OMRAN / jamais éteintes | **Fixed** (PR #89) | |
| 57 | `AdmAlertData` hors change tracking | **Fixed** | |
| 58 | 583 alertes actives sur prospections fermées | **Fixed** | script 013 |
| 59 | Copies VPS en retard sur la source (étendue inconnue) ; l'aperçu d'import (copie) et l'import (source) ont compté 5 puis 3 doublons | **Open** | empreinte des copies vs source, toucher les lignes divergentes |
| 60 | `/api/commerciaux` filtre par kind au lieu de `CfgTier.IsVisibleInList` | Open (audit 1.5) | |
| 61 | Interaction : règles GEN0005 non appliquées | Open (audit 2.2, 2.3) | |
| 62 | Alerte acceptée sur une prospection fermée | Open | refuser dans `addInteraction` |
| 63 | Site d'aide OMRAN hors service, 85 pages manquantes | Open (externe) | relancer `fetch.sh` |
| 64 | Tableau de bord 81 vs liste 46 | **Fixed** (PR #90) | |
| 65 | `ouvertes=false` renvoyait toute la liste | **Fixed** (PR #91) | |
| 66 | Serveur en UTC : « aujourd'hui » décalé entre 00:00 et 01:00 au Maroc | **Open** | horloge en `Africa/Casablanca` |
| 67 | L'API ne sait pas modifier nom, téléphone, email d'une pré-prospection (`ModifierProspectionRequest`) | Open | demande front n° 12 |
| 68 | L'import CSV n'accepte que l'export **Leads Center** ; l'export **Meta Lead Ads** (`created_time`, `full_name`, `phone`, réponses du formulaire) doit être transformé à la main | Open | reconnaître aussi ces en-têtes dans `LecteurCsvLeads` |
| 69 | 3 téléphones importés avec un zéro en trop après l'indicatif (`+2120…`) | Open (donnée d'origine) | normaliser à l'import, ou corriger quand le bug 67 sera levé |
| 70 | TRIBECA (société MAYLINE) invisible dans l'application : `IsActive = 0` sur les copies | **Open** (utilisateur) | `UPDATE CfgCompany SET IsActive = 1 WHERE Code = 'MAYLINE'` sur chaque copie, jamais sur la source |
| 71 | **Modification non commitée de `005_CfgCompany_IsActive.sql`** dans l'arbre de travail (01/10) : la réactivation ne liste plus que `MAYLINE` → joué tel quel, le script **désactive ADMOD et GHRIZA** et fait disparaître APF et ARS. Les commentaires du script parlent encore d'« exactement ADMOD et GHRIZA » | **Open — DANGER** | mettre `('ADMOD', 'GHRIZA', 'MAYLINE')` et corriger les commentaires avant tout usage ; non commité par la session |

---

## 🔑 Key Points to Remember

- **Règle des alertes** : c'est la prochaine interaction **porteuse d'une date d'alerte** qui
  éteint la précédente ; archivage et passage en dossier éteignent tout ; rien n'expire.
- **Tableau de bord et liste comptent la même population** : prospections qualifiées, ouvertes,
  `DocDate` dans le mois civil. La liste OMRAN et celle du front filtrent sur le mois en cours.
- **Format d'import de la plateforme** (`LecteurCsvLeads`) : en-tête `Créé, Nom, Adresse e-mail,
  Source, Formulaire, Canal, Étape, Propriétaire, Étiquettes, Téléphone, Numéro de téléphone
  secondaire, Numéro WhatsApp` ; colonnes reconnues par leur nom, seule `Nom` indispensable ;
  séparateur `,` ou `;` ; UTF-8 (BOM accepté) ou Windows-1252 ; date `09/23/2026 1:45am` ou ISO
  `2026-09-23 01:45[:ss]` ; 5 000 lignes au plus. `Formulaire`, `Source`, `Canal`, `Étape`,
  `Étiquettes` vont dans les notes.
- **Sort d'une ligne importée** : `NOUVELLE` créée ; `DOUBLON` (même contact, autre date) créée
  et signalée ; `DEJA_IMPORTEE` (même contact et même minute) ignorée.
- **Conventions des leads de cette campagne** : nom = `full_name` du profil ; date = heure de
  l'export telle quelle (fuseau du compte publicitaire, 8 h de moins que le Maroc) ; source
  FACEBOOK même pour Instagram ; ni support ni action à l'import brut. Les leads n8n portent en
  plus le support `AYAD DEVELOPPEMENT (ADS)` et l'action `APF2025`, que la commerciale pose à la
  qualification.
- **L'aperçu lit la copie, l'import écrit et lit la source** (routage deux-bases).
- Écritures en production : seulement sur demande explicite de l'utilisateur. Les identifiants
  sont fournis en séance et ne sont jamais consignés.
- `REFERENCE-aide-omran.md` est le premier endroit où chercher une règle d'écran.
- Mode de travail : backend seulement ; git via `node <prototype>/.claude/scripts/git-flow.mjs` ;
  **demander avant toute branche** ; commit + push dès que vert ; attendre le merge puis `sync`.

---

## 📁 Important Files & Paths

- `services/prospection/LecteurCsvLeads.java`, `ImportPreProspectionService.java` — format et
  règles de l'import de leads.
- `controller/prospection/PreProspectionController.java` — `/import/apercu`, `/import`.
- `dto/prospection/ModifierProspectionRequest.java` — champs modifiables (pas le nom ni le téléphone).
- `instructions/REFERENCE-aide-omran.md` + `instructions/aide-omran/` — aide en ligne et outils.
- `instructions/AUDIT-prospection-aide-omran.md` — les 7 écarts à corriger.
- `instructions/FEATURE-alerte-admalertdata.md`, `FEATURE-dashboard-relances.md` (§3 bis, §7),
  `from_front.md` (demande 15 traitée, demande 12 en attente).
- `services/prospection/AlerteRelanceService.java`, `services/dashboard/RelanceDashboardService.java`,
  `domain/EcheanceRelance.java`, `services/prospection/ProspectionService.java` (`buildSpec`).
- `db/migration/012_…`, `013_…` — déjà appliqués sur la source.

---

## 🔧 Environment & Config

- Source 115 : `197.230.172.136:1433`, base `YesRems` — CT sur 28 tables. Horloge en heure du Maroc.
- Production : `https://api.ayaddeveloppement.com`, connexion `POST /api/auth/connexion`, cookie
  de session. Serveur en UTC. Déploiement automatique au merge. Synchronisation < 2 min.
- Référentiels utiles : tranche `APF` = `c273dec7-08d1-4256-9e3c-efa1485c4ee8` ; action
  `APF2025` = `cc648025-d542-43b1-89ae-08ce82d1db95` ; support `AYAD DEVELOPPEMENT (ADS)` =
  `68f8d807-03b9-4c68-9294-ad2ff3ecda97` ; origine `FACEBOOK` = `eda5c563-6193-4aa3-aa2d-c44bddf16c1c`.
- Local : `localhost,49687` — `yesRems` (bac à sable audité), `yesRemsProd` (banc), `yesRemsTest`.
- Tests : 1 045 verts, environ 10 minutes.
- Dossier de travail de session : aucune donnée de lead, aucun cookie conservé.

---

## ⏭️ Next Step

> **Bug 71 d'abord** : le script 005 modifié dans l'arbre de travail (`WHERE Code IN ('MAYLINE')`)
> ne doit pas être joué ni commité tel quel — il désactiverait ADMOD et GHRIZA. Remettre
> `('ADMOD', 'GHRIZA', 'MAYLINE')`, aligner les commentaires, puis faire jouer sur chaque copie
> (jamais sur la source) : `UPDATE dbo.CfgCompany SET IsActive = 1 WHERE Code = 'MAYLINE'` pour
> activer TRIBECA (bug 70). Ensuite reprendre le CSV des 32 leads Meta au format d'import
> (demande interrompue ; demander où le déposer et s'il faut les 32 lignes ou les 18 nouvelles).

### Backlog
- [ ] Support et action commerciale des 18 pré-prospections : à poser si l'utilisateur tranche
      entre `AYAD DEVELOPPEMENT (ADS)` et `AYAD PRIVATE (ADS)`.
- [ ] Bug 68 : faire accepter l'export Meta Lead Ads par l'import, réponses du formulaire en
      notes ligne par ligne.
- [ ] Bug 66 : fuseau `Africa/Casablanca` pour le tableau de bord.
- [ ] Bugs 60, 61, 62 : commerciaux par `IsVisibleInList`, règles GEN0005, refus d'une alerte sur
      prospection fermée.
- [ ] Audit : actions commerciales actives, alertes futures dans la liste native, articles
      « prospectables », doc du `POST /api/prospections`, colonnes de liste manquantes.
- [ ] Bug 59 : empreinte des copies vs source.
- [ ] Compléter la référence quand le site d'aide revient, puis `/graphify . --update`.
- [ ] Branche : 401 nu → 500 propre + `AdmUser` en tête de `ORDRE_CLES_ETRANGERES`.
- [ ] Bugs 51, 52, 53 ; rotation des mots de passe du bug 55.
- [ ] n8n (nœud HTTP `X-Api-Key`) ; MCP (`POST /api/auth/jeton`, spec §6).

# Session Memory
_Last updated: 2026-09-16T10:30+01:00_
_Project: immo-crm (AyadERP) — C:\Users\tsiory\Documents\ayad-internship\prototype\immo-crm_

---

## 🎯 Current Objective

Deux chantiers parallèles, l'infrastructure ayant pris le pas le 16/09 :

**A — INFRASTRUCTURE (actif).** Remplacer la **redirection de port du routeur**, qui expose
aujourd'hui `192.168.11.115:1433` à tout internet, par un **VPN NetBird auto-hébergé sur le
VPS**. Motif déclencheur : l'ancien admin est parti en laissant ce montage, et 6 utilisateurs
distants en dépendent.

**B — BACKEND (en attente).** Aligner les endpoints sur ce que fait réellement OMRAN Estate,
via sa trace d'audit SQL Server. Lots G→R mergés, **881 tests verts**.

| Lot | Sujet | État |
|---|---|---|
| G–I | Groupement des listes, colonnes du dossier | mergés (PR #43/#44/#45) |
| J | Conformité légataire : écritures manquantes | mergé (PR #46) |
| K | Encaissements : `IsCredit=1` + filtre par tranche | mergé (PR #47) |
| L–M | Encaissements : colonnes, groupement, totaux, menu Σ | mergés (PR #48) |
| N | Fiche encaissement : affectations + un 500 corrigé | mergés (PR #49/#51) |
| O | Conformité Java 21 | mergé (PR #50) |
| P | Tableau de bord : relances + leads du jour | mergé (PR #53) |
| Q | Tableau de bord d'OMRAN porté | mergé (PR #54) |
| R | Créations prospect/prospection : code + interaction | mergé (PR #55) |
| S | **Filtre par colonne** — demande front consignée, **non commencé** | branche `docs/memoire-et-demande-front` **poussée, PR en attente de merge** |

⚠️ **Deux PR documentaires attendent la merge de l'utilisateur sur GitHub** :
`immo-crm` → `docs/memoire-et-demande-front` · `crm-prototype` → `docs/demande-filtre-colonne`.

---

## ✅ What Worked

### Session du 16/09 (infrastructure)

- **Mesurer avant de recommander, systématiquement.** Chaque affirmation de cette session a
  été vérifiée par requête : édition SQL, tailles, latences disque, ports d'écoute, divergence
  entre bases. Trois recommandations ont changé après mesure.
- **Aller chercher la doc officielle plutôt que d'écrire de mémoire.** `WebFetch` sur
  `docs.netbird.io` a corrigé trois erreurs que j'aurais livrées : le proxy embarqué de NetBird
  est **Traefik** (pas Caddy), l'installeur propose une option **« External Caddy »** toute
  faite, et **la plage de ports TURN n'existe plus** (WebSocket/QUIC via 443, seul 3478/udp
  reste requis).
- **`sys.dm_io_virtual_file_stats` pour diagnostiquer une lenteur** plutôt que supposer. C'est
  elle qui a désigné le disque de 115, chiffres à l'appui.
- **`sys.dm_tcp_listener_states`** pour savoir si SQL Server acceptera une interface future :
  il écoute sur `0.0.0.0:1433`, donc le VPN ne demandera **aucune reconfiguration SQL**.
- **Anti-jointure sur `PKey` plutôt que filigrane d'horloge** pour la synchro (conception
  retenue, non implémentée) : exacte, insensible à la dérive d'horloge et à l'ordre de
  validation, **idempotente et auto-réparante**.

### Sessions précédentes (backend) — toujours valable

- **Lire la trace d'audit en filtrant sur `application_name = 'OMRAN estate'`.** Le compte SQL
  est `tsix` pour tout le monde et **nos propres `sqlcmd` y apparaissent aussi**.
- **Caler le décalage horaire par une requête-sonde** : `event_time` est en **UTC**, local = +1.
- **Vérifier qu'une table existe dans `yesRemsTest` AVANT de la mapper.**
- **Compter avant de conclure, même sur une jointure qui « ne change rien ».**
- **Dater les fixtures de test d'une année dédiée (2077)** pour asserter des valeurs exactes.
- **Écrire les retouches de source en Python depuis un fichier**, avec `assert old in s`.

---

## ❌ What Failed

### Session du 16/09

- **Avoir annoncé un mur imminent sur la limite des 10 Go d'Express** → **faux**. Les données
  **FILESTREAM ne comptent pas** dans cette limite : 115 est à 1 718 Mo sur 10 Go (17 %), avec
  8 248 Mo de FILESTREAM à côté. Corrigé après mesure — mais la recommandation d'achat en
  dépendait.
- **Avoir mal lu la correspondance des bases**, deux tours de suite. L'utilisateur a dû répéter
  quatre fois. Leçon : quand une clarification est répétée à l'identique, **relire la précédente
  réponse plutôt que d'en produire une nouvelle**.
- **Avoir présumé un flux de synchronisation depuis la trace d'audit** → elle ne porte **aucune
  valeur** (318/318 `INSERT` en `@P1…@Pn`) et trace la base **locale**, pas 115.
- **`sqlcmd`** : `-W` et `-y` exclusifs (reconfirmé), colonnes inventées (`is_change_tracking_on`
  n'existe pas dans `sys.databases` ; `AdmSequence` n'a ni `LastValue`, ni `Prefix`, ni `Suffix`).

### Sessions précédentes — toujours valable

- **Les heredocs Bash portant un document long** (accents, guillemets français, backticks)
  cassent en `unexpected EOF`. → outil `Write`, ou script Python dans un fichier.
- **`sqlcmd` ne comprend pas les chemins MSYS** → `cygpath -w` pour `-i` et `-o`.
- **`COUNT(colonne)` pour juger une colonne utile** → compte les non-nuls.
- **`SUM(expression contenant une sous-requête)`** → SQL Server, erreur 130.
- **`COALESCE(SUM(x), 0)`** → arrondit l'argent à l'unité. Substituer en Java, en `BigDecimal`.
- **Pousser la mémoire de session APRÈS la merge de sa PR** → commit orphelin.

---

## 🐛 Bugs & Errors Log

| # | Erreur / Bug | Statut | Correctif |
|---|---|---|---|
| 1–13 | (lots G–N : dérive `yesRemsTest`, arrondi monétaire, `@Formula` en test, `GROUP BY` + sous-requête, `TransientPropertyValue`, 500 sur la fiche encaissement, mémoire orpheline…) | **Fixed** | PR #43–#52 |
| 14 | Prospect créé sans `Code` par l'API | **Fixed** | `GenerateurCodeSequence` (PR #55) |
| 15 | Prospection créée sans aucune interaction | **Fixed** | `ecrireInteractionDeCreation` (PR #55) |
| 16 | **`192.168.11.115:1433` exposé à internet** par redirection totale du routeur | **Ouvert — chantier en cours** | VPN NetBird, voir `../infra/NETBIRD-VPS.md` |
| 17 | **Écriture disque à 100 ms sur 115** (lecture 5,5 ms) — disque mécanique | **Ouvert — non traité** | décision prise de **garder 115 tel quel** |

**Aucun bug applicatif ouvert.** Le reste est bloqué sur décision produit.

---

## 🔑 Key Points to Remember

### ⚠️ Le parc réel, mesuré le 16/09

| Machine | Ce qu'elle est |
|---|---|
| **`192.168.11.115`** (`DESKTOP-7ESOL53\SQLEXPRESS`) | **la production vivante du client**, lecture seule stricte. Compte `sasa` — mot de passe fourni en session, **non consigné ici** |
| **VPS** | Contabo, Ubuntu, **Caddy en service système** (pas en conteneur), backend `immo-crm` + MinIO (`s3.tsiory.tech`), base `yesRemsDev` |
| **`localhost,49687`** | `yesRems` (OMRAN local + tracé par l'audit), `yesRemsProd` (mesure), `yesRemsCiCheck`, `yesRemsTest` |

### 115 — la fiche technique complète

```
Édition      : SQL Server 2019 Express, RTM (15.0.2000.5) — jamais mis à jour
Matériel     : 2 cœurs, 8 Go de RAM  (jamais éteint)
Données      : ROWS 1 718 Mo (1 352 Mo utilisés) · FILESTREAM 8 248 Mo · LOG 121 Mo
Disque       : lecture 5,5 ms / écriture 100,1 ms  → signature d'un disque mécanique
Écoute       : 0.0.0.0:1433 et [::]:1433, port fixe
```

**Trois conséquences non déductibles du code :**

1. **Les données FILESTREAM ne comptent PAS dans la limite des 10 Go d'Express.** 115 est à
   17 % du plafond, pas au bord. Ne pas refaire l'erreur inverse.
2. **La lenteur vient du disque, pas de l'édition ni du volume.** Donc **acheter SQL Server
   Standard ne réglerait rien**, et un VPS à 24 Go de RAM non plus — Express plafonne son cache
   à ~1,4 Go quelle que soit la machine. Le jeu de données utilisé (1 352 Mo) est **à 96 % de
   ce plafond**.
3. **SQL Server écoute sur toutes les interfaces** → il acceptera l'interface VPN
   automatiquement, **sans aucune reconfiguration**.

### OMRAN : comment il se connecte (déterminant pour le VPN)

Configuration en **IP + utilisateur + mot de passe + nom de base**. Donc :

- connexion **par IP**, pas par nom d'instance → **SQL Browser (UDP 1434) jamais sollicité**,
  seul TCP 1433 est à ouvrir ;
- **authentification SQL**, pas Windows → **aucune dépendance à un contrôleur de domaine**,
  ce qui aurait été le point dur d'un accès Kerberos à travers un VPN ;
- migrer un poste = **remplacer un champ**.

Reste à vérifier sur le premier poste : ce champ accepte-t-il un **nom d'hôte** ou strictement
une **IP** ? Si IP seulement, la réservation d'adresse VPN fixe pour 115 devient obligatoire.

### La décision d'architecture du 16/09, et pourquoi

Le parcours a tourné avant de se poser — le noter pour ne pas le refaire :

1. *Synchroniser deux bases* → étudié en détail, **conception arrêtée mais non construite** ;
2. *Base unique, backend joignant 115* → écarté : **l'utilisateur ne veut pas que le backend
   dépende de 115** (lenteur, fichiers en binaire) ;
3. *Déménager OMRAN sur le VPS* → écarté : latence Maroc→Europe sur un client lourd bavard,
   et responsabilité de la production du client ;
4. **Retenu : garder 115 tel quel, et ne faire que le VPN.**

**Le VPN est donc le seul chantier d'infrastructure actif.** La synchro est *parquée*, pas
abandonnée.

### Conception de synchro arrêtée (si elle repart un jour)

- **`PKey` est un `uniqueidentifier` partout** → deux bases qui insèrent ne peuvent pas
  produire la même identité. Insertions seules + UUID = **aucun conflit possible**.
- **Détection** : filigrane `UserCreateDateTime` pour *restreindre* (posé à ~100 % : 17 938/17 939,
  14 515/14 516, 48 163/48 163, 358/358, 3 243/3 243 — **y compris sur les lignes `n8n`, qui
  n'ont pourtant pas de `UserCreate`**), puis **anti-jointure `PKey` pour décider**.
- **`AdmWebHistorics` est inutilisable seul** : 564 653 lignes, mais **0 des 12 prospections
  créées en août** y ont une ligne `Added`. Il ne voit pas les injections automatisées.
- ⚠️ **`AdmSequence` est LE danger irréversible.** Compteurs `Tier` 14 503, `Prospection` 5 949,
  `Dossier de vente` 379 ; **aucun index unique sur `Code`** (les 8 index trouvés sont des
  artefacts `_dta_index_*`, tous `is_unique = 0`) ; **6 doublons existent déjà sur `CfgTier`,
  49 sur `ComProspection`**. À partitionner par préfixe **avant** toute insertion croisée.
- **Fermeture FK des 5 tables métier = 118 tables** (sur 1 219 FK). Le référentiel `Cfg*`/`Adm*`/
  `Str*` s'aligne une fois, seul le transactionnel circule.
- **`ComFolder` porte 2 déclencheurs actifs** (`trgAfterComFolderInsert`/`Update`) — à lire
  avant de l'inclure.
- Divergence locale mesurée (`yesRems` → `yesRemsProd`) : ComProspection 8/267, CfgTier 30/107,
  ComActionMessage 33/208, ComFolder 31/2. Schéma **identique**, 0 colonne divergente.
- **Les pièces jointes sont le point dur** : binaire sur 115, S3 sur le VPS.
  `migration/migrate_blobs_to_s3.py` deviendrait un composant **permanent**, pas ponctuel.

### La trace d'audit : ce qu'elle est et n'est pas

`sys.fn_get_audit_file('C:\backup\*.sqlaudit', DEFAULT, DEFAULT)` depuis `master`.

**Elle sert à savoir quelles colonnes un écran d'OMRAN cible** — rien d'autre. Elle **ne peut
pas** servir de flux de synchronisation : **318 `INSERT` sur 318 sont en `@P1…@Pn`**, sans
aucune valeur, et elle trace le `yesRems` **local**, pas 115.

- Filtrer sur `application_name = 'OMRAN estate'` — sinon on lit ses propres `sqlcmd`.
- `event_time` en **UTC**, local = +1. Tronque à 4 000 caractères.
- Spec `Audit_DML_DDL_Spec`, tous principaux, SELECT/INSERT/UPDATE/DELETE.
- ⚠️ **Si un agent écrit un jour dans `yesRems`, lui donner son propre `Application Name`**
  pour qu'il se retire de ces lectures.

### La doctrine `Optional` du dépôt (posée par `d20362c`)

> « Un `Optional` exprime l'absence dans un **type de retour**, pas dans une **expression
> intermédiaire**. » **Test : un appelant déballe-t-il vers `null` ?** Si oui, laisser le `null`.

### Ce qui ne doit PAS être refactoré

Entités JPA · `requireNonNull` sur les constructeurs injectés · `CompletableFuture`/threads
virtuels · le `ThreadLocal` de `web/ColonnesSelection`.

### `IsSystem` : deux conventions opposées, toutes deux justes

Interaction **saisie** → `IsSystem = 0`. Interaction **de création de prospection** →
`IsSystem = 1`, `WithoutRevival = 1`, exclue du dernier échange par `ComSaleProspectionView`.
**Ne pas « harmoniser ».**

### Autres règles backend acquises

- **Numérotation** : `services/sequence/GenerateurCodeSequence` sert `Tier`, `Prospection` et
  `Dossier de vente`, et incrémente **la même ligne `AdmSequence` que le logiciel de bureau**.
- **`BoolField002`/`BoolField003` == `StatutDossier.isSortie()`** — rien à mapper.
- **Groupabilité** : pas de chemin to-many, pas de `nvarchar(max)`, valeur calculée portée par
  la base (`@Formula` ou `@Subselect` joignable).
- **Piège des colonnes « pleines »** : mesurer **distinct et non-zéro**, jamais `COUNT(colonne)`.
- **`@Formula`/`@Subselect` ne sont peuplés qu'au chargement** → `flush` + `clear` en test.
- **Commits en ASCII pur, sans accents.** Préfixes `feat(scope):` / `fix(scope):` / `docs(scope):`.

---

## 📁 Important Files & Paths

- **`../infra/NETBIRD-VPS.md`** — **le livrable du 16/09**. Runbook NetBird complet adapté au
  VPS (Caddy service système, option `[4] External Caddy`), chaque commande et chaque ligne de
  Caddyfile expliquée. ⚠️ `prototype/` **n'est pas un dépôt git** : ce fichier n'est pas versionné.
- `instructions/from_front.md` — canal officiel front→backend. **Entrée n° 11 = le filtre par
  colonne**, priorité annoncée au-dessus du reste.
- `instructions/FIX-creation-prospect-prospection.md` — §5, les trois contrôles non implémentés.
- `instructions/FEATURE-dashboard-omran.md` · `…-dashboard-relances.md` · `…-affectations-encaissement.md`
- `migration/migrate_blobs_to_s3.py` + `README.md` — **le moule maison** pour tout outil de
  données hors build : Python hors Maven, `.env`, `--verify`, confirmation explicite, log.
  ⚠️ `tsix` n'a **pas** `CREATE TABLE` sur `yesRems`.
- `src/main/java/.../services/sequence/GenerateurCodeSequence.java`
- `src/main/java/.../domain/{Chemin,FonctionAgregat,Colonnes,Colonne}.java` — `Colonne` est `sealed`.
- `src/main/resources/db/migration/007_ComActionMessage_drapeaux_api.sql` — **à appliquer à la main**.
- `../FEATURES.md` — index dérivé. ⚠️ son détecteur « endpoints sans appelant » **invente 16 faux
  positifs**.

---

## 🔧 Environment & Config

- **Mesurer** : `sqlcmd -S localhost,49687 -U tsix -P <voir CLAUDE.md> -d yesRemsProd -C`.
  Chemins `-i`/`-o` via `cygpath -w`. `-W`/`-y` exclusifs, `-h`/`-y 0` exclusifs.
- **115** : `sqlcmd -S 192.168.11.115 -U sasa -P <demander à l'utilisateur> -d YesRems -C`.
  **Lecture seule stricte.**
- **Audit** : `sys.fn_get_audit_file('C:\backup\*.sqlaudit', DEFAULT, DEFAULT)` depuis `master`.
- **Tests** : `./mvnw.cmd test` nu, ~3 min 30 pour 881 tests. SQL Server doit tourner.
- **Git** : tout via `node .claude/scripts/git-flow.mjs {status,start,commit,push,sync}` depuis
  `prototype/`. `guard-branch.mjs` **interdit toute écriture sur `main`**. Avant `push` :
  `feature-index.mjs add <slug>` puis `generate`.
- ⚠️ Ne jamais réécrire un fichier source via PowerShell (encodage). Heredocs Bash longs → cassent.
- ⚠️ **Identifiants de service en clair au fil des sessions et dans `CLAUDE.md`** — dette connue,
  non traitée. Le mot de passe de 115 n'est volontairement pas écrit ici.

### État des branches au 16/09

| Repo | Branche | État |
|---|---|---|
| `immo-crm` | `docs/memoire-et-demande-front` | 1 commit, **poussée, PR en attente de merge** |
| `crm-prototype` | `docs/demande-filtre-colonne` | 2 commits, **poussée, PR en attente de merge** |

Branches mergées supprimables : `feat/java-21-conformite`, `feat/affectations-encaissement`,
`docs/memoire-session-lot-O`, `feat/dashboard-relances`, `feat/dashboard-omran`,
`fix/creation-prospect-prospection`.

---

## ⏭️ Next Step

> **Créer l'enregistrement DNS `vpn.tsiory.tech` → IP publique du VPS**, et vérifier sa
> propagation (`dig +short vpn.tsiory.tech`) avant toute autre chose.
>
> C'est l'étape 1 de `../infra/NETBIRD-VPS.md` : tout le reste en dépend, et la propagation
> prend le temps qu'elle prend. Lancer le script d'installation avant que le DNS réponde fait
> échouer la validation ACME de Caddy, avec des erreurs de certificat qu'on prend à tort pour
> un problème de configuration.

### Backlog

**Infrastructure**
- [ ] Suivre `../infra/NETBIRD-VPS.md` : DNS → 3478/udp → script (option `[4]`) → Caddyfile →
      compte admin → tableau de bord → 115 → les 6 postes → fermer la redirection **en dernier**.
- [ ] **Sur le premier poste migré** : le champ « IP » d'OMRAN accepte-t-il un nom d'hôte ?
- [ ] **Mesurer la latence** poste utilisateur → VPS.
- [ ] Versionner `infra/NETBIRD-VPS.md` quelque part (demander le repo et une branche).
- [ ] **Vérifier ce que les conteneurs du VPS exposent déjà** — Docker court-circuite `ufw`.

**Backend**
- [ ] **Merger les deux PR documentaires**, puis `sync` des deux repos.
- [ ] **Lot S — le filtre par colonne** (`instructions/from_front.md` n° 11). `ColonneDto.filtrable`
      + `operateurs`, `?filtreColonne=col:OPERATEUR:valeur` répétable, 400 nommant la colonne,
      **`CONTIENT`/`EGAL` insensibles à la casse ET aux accents** (la base écrit `CHÉQUE`,
      « cheque » rend 0 ligne sur 180 correspondantes). Absorbe probablement le vieux `?statut=`.
- [ ] `ModifierProspectionRequest` — ajouter `tempDescription`, `tempTel`, `tempEmail`, `commercialId`.
- [ ] **Les 13 nouveaux champs du DTO encaissement n'ont aucune description springdoc** — viole
      la règle 10 du `CLAUDE.md`.
- [ ] Écart serveur sur `titreFoncier` (liste 33/116, fiche 5/8) — à confirmer.
- [ ] `CreerReglementRequest.dateReglement` : le spec la publie, le serveur la refuse (400).
- [ ] Catalogue prospects : description mensongère sur la bande de groupement.
- [ ] **Les trois contrôles de création d'OMRAN** (SOUNDEX, unicité, prospection ouverte unique)
      — **décision produit** : bloquant ou consultatif ?
- [ ] Alertes de jalon du dossier — **décision produit**.
- [ ] Encaissements non affectés — 16 pièces, 6 435 400 DH, invisibles.
- [ ] Verrous pessimistes `AdmWebLockEntities` + `AdmWebSessions`.
- [ ] Seconde couche de permissions (`StrStatusIntervener` × `StrIntervener`) — le plus structurant.
- [ ] **Ordre de pagination non déterministe** — défaut latent réel, absorbé côté front seulement.
- [ ] Les 8 onglets absents de la fiche dossier.
- [ ] Supprimer les six branches mergées.

**Parqué**
- [ ] **La synchro 115 ↔ VPS** — conception arrêtée (voir §Key Points), rien de construit.
      Ne repart que si la décision « garder 115 tel quel » est révisée.

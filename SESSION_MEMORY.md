# Session Memory
_Last updated: 2026-09-15T16:20+01:00_
_Project: immo-crm (AyadERP) — C:\Users\tsiory\Documents\ayad-internship\prototype\immo-crm_

---

## 🎯 Current Objective

**Aligner nos endpoints sur ce que fait réellement le logiciel de bureau (OMRAN Estate)**, en
partant de sa **trace d'audit SQL Server** plutôt que de captures d'écran ou de lectures de vue.

| Lot | Sujet | État |
|---|---|---|
| G–I | Groupement des listes, colonnes du dossier | mergés (PR #43/#44/#45) |
| J | Conformité légataire : écritures manquantes | mergé (PR #46) |
| K | Encaissements : `IsCredit=1` + filtre par tranche | mergé (PR #47) |
| L–M | Encaissements : colonnes, groupement, totaux, menu Σ | mergés (PR #48) |
| N | Fiche encaissement : affectations + un 500 corrigé | mergés (PR #49/#51) |
| O | Conformité Java 21 (demande ponctuelle, hors trame OMRAN) | mergé (PR #50) |
| P | Tableau de bord : relances du jour + leads du jour | mergé (PR #53) |
| Q | Tableau de bord d'OMRAN porté (`/api/dashboard/annuel`) | mergé (PR #54) |
| R | Créations prospect/prospection : code + interaction manquants | mergé (PR #55) |

**881 tests verts.** `main` = `c14c897`, propre, aucune PR en attente.

---

## ✅ What Worked

- **Lire la trace d'audit en filtrant sur `application_name`.** C'est LE discriminant : le compte
  SQL est `tsix` pour tout le monde, **nos propres `sqlcmd` y apparaissent aussi** (vus comme
  `SQLCMD`, IP du poste, contre `OMRAN estate` en 127.0.0.1). Sans ce filtre on lit ses propres
  requêtes en croyant lire celles d'Omran — c'est arrivé.
- **Caler le décalage horaire par une requête-sonde** plutôt que par déduction : lancer une requête
  horodatée sur `yesRems`, la relire dans l'audit, comparer. Résultat : `event_time` est en **UTC**,
  heure locale = UTC + 1.
- **Mesurer la conformité avant de refactorer** (lot O). Demandé comme « huge refactor » ; l'audit a
  montré 115 DTO sur 115 déjà en `record`, 0 `@Data`, 0 switch ancien style, 0 thread brut. Le
  livrable honnête était 6 changements, pas une passe massive.
- **Lire le message du commit de refactor antérieur** (`d20362c`) : il porte les *principes* du
  dépôt, introuvables ailleurs, et sa section « Hors perimetre volontaire » vaut contrat.
- **Vérifier qu'une table existe dans `yesRemsTest` AVANT de la mapper.** A évité de mapper
  `AdmParamsGuid` (absente) : une table absente ne se règle pas par une fixture, contrairement à une
  ligne non semée. Le seuil a été résolu autrement, sans toucher au schéma CI.
- **Compter avant de conclure, même sur une jointure qui « ne change rien ».** La jointure `StkItem`
  du bloc C d'OMRAN rend les mêmes 82 lignes avec ou sans — mais elle **double** les 3 dossiers à
  deux articles principaux et **exclut** les 37 sans article. Retirée.
- **Tester les hypothèses séduisantes.** « Une interaction plus récente périme la relance
  précédente » : faux, mesuré (3 701 des 3 765 relances armées sont portées par la dernière
  interaction). Deux minutes de SQL contre une conception fausse.
- **Dater les fixtures de test d'une année dédiée (2077)** plutôt que mesurer des deltas : la base de
  test est partagée entre classes, une année à nous permet d'asserter des valeurs **exactes**. Un
  test qui dit « le total vaut 300 000 » attrape une double-comptabilisation ; « le total a
  augmenté » ne l'attrape pas.
- **Écrire les retouches de source en Python depuis un fichier**, avec `assert old in s` avant chaque
  `replace`. L'assertion transforme un motif introuvable en échec immédiat.

---

## ❌ What Failed

- **Les heredocs Bash portant un document long** (accents, guillemets français, backticks) cassent
  en `unexpected EOF`. Confirmé 3 fois cette session. → outil d'écriture dédié, ou script Python
  dans un fichier.
- **`sqlcmd` ne comprend pas les chemins MSYS** (`/c/Users/...`) → `cygpath -w` pour `-i` et `-o`.
- **`sqlcmd` : `-W` et `-y` sont exclusifs**, et **`-h` et `-y 0` aussi** (nouveau).
- **Un script Python qui écrit le fichier en fin de course** ne persiste rien s'il échoue en
  chemin — ce qui est heureux, mais il faut le savoir avant de diagnostiquer un état à moitié appliqué.
- **Convertir en `Optional` un parseur dont l'unique appelant déballe en `.orElse(null)`** →
  recrée le motif que `d20362c` avait supprimé sur 49 sites. Reverti (`MarqueurTriProspection`).
- **`COUNT(colonne)` pour juger une colonne utile** → compte les non-nuls.
- **`SUM(expression contenant une sous-requête)`** → SQL Server, erreur 130.
- **`cb.coalesce(cb.sum(x), ZERO)` / `COALESCE(SUM(x), 0)`** → arrondit l'argent à l'unité.
  Substituer en Java, en `BigDecimal`.
- **Pousser la mémoire de session APRÈS la merge de sa PR** → commit orphelin (arrivé à `55997f6`).

---

## 🐛 Bugs & Errors Log

| # | Erreur / Bug | Statut | Correctif |
|---|---|---|---|
| 1–11 | (lots G–K : dérive `yesRemsTest`, arrondi monétaire, routes `/groupes`, `@Formula` en test, `GROUP BY` + sous-requête, `clearAutomatically`, `TransientPropertyValue`, `IsSystem` non posé, prospection non fermée, `StrHistory`, décaissement) | **Fixed** | voir PR #43–#47 |
| 12 | 500 sur `GET /api/encaissements/{id}` — `ObjectNotFoundException` | **Fixed** | Sous-select en `LEFT JOIN` depuis `StlSettlement` |
| 13 | Mémoire de session orpheline sur une branche post-merge | **Fixed** | PR #51/#52 |
| 14 | **Prospect créé sans `Code`** par l'API (12 anomalies sur 13 913 réels) | **Fixed** | `GenerateurCodeSequence` sur `AdmSequence.Code='Tier'` (PR #55) |
| 15 | **Prospection créée sans aucune interaction** — dernier échange, délai de prise en charge et KPI faussés dès la naissance | **Fixed** | `ecrireInteractionDeCreation` (PR #55) |

**Aucun bug ouvert.** Ce qui reste est **bloqué sur décision produit**.

---

## 🔑 Key Points to Remember

### La trace d'audit : la lire sans se tromper de client

`sys.fn_get_audit_file('C:\backup\*.sqlaudit', DEFAULT, DEFAULT)` depuis `master`, compte `tsix`.
Scopée sur `yesRems`. **Vivante** : l'utilisateur peut rejouer un écran à la demande, et l'a fait
plusieurs fois cette session.

- **Filtrer sur `application_name = 'OMRAN estate'`** — sinon on lit ses propres `sqlcmd`.
- **`event_time` est en UTC**, local = UTC + 1 (vérifié par sonde, pas déduit).
- Tronque à 4 000 caractères, masque les valeurs (`@P1…@Pn`).
- **Un `SELECT vue.*` ne dit rien de l'affichage** ; le groupement et les totaux d'Omran sont
  calculés **côté client** (grille DevExpress).
- **Un `INSERT` ne dit rien des données obligatoires** : l'ORM écrit toutes les colonnes de
  l'entité, renseignées ou non. L'obligation vit dans les `SELECT` qui le précèdent.

### La doctrine `Optional` du dépôt (posée par `d20362c`)

> « Un `Optional` exprime l'absence dans un **type de retour**, pas dans une **expression
> intermédiaire**. »

**Test avant toute conversion : un appelant déballe-t-il vers `null` ?** Si oui, laisser le `null`.
`utils/Champs.de` est l'idiome maison pour *naviguer* un graphe nullable (~220 sites) — ce n'est pas
la même chose qu'un parseur.

### Ce qui ne doit PAS être refactoré

Entités JPA (pas de `record` possible, `@Getter @Setter` imposés) · `requireNonNull` sur les
constructeurs injectés (Spring échoue déjà au démarrage) · `CompletableFuture`/threads virtuels
(aucun éventail d'E/S ; le mail est déjà `@Async` sur un pool géré) · le `ThreadLocal` de
`web/ColonnesSelection` (délibéré, Jackson 3 vs `MappingJacksonValue` typé Jackson 2, nettoyé en
`finally`).

### `IsSystem` : deux conventions opposées, toutes deux justes

`ComSaleProspectionView` calcule le dernier échange avec `AND IsSystem = 0`.

- **Interaction saisie** (`creerInteraction`) → `IsSystem = 0`, `WithoutRevival = 0`. C'est un
  échange, il compte.
- **Interaction de création de prospection** (`ecrireInteractionDeCreation`) → `IsSystem = 1`,
  `WithoutRevival = 1`, pas d'échéance, `AlertMessage` = les notes. Créer une prospection n'est pas
  un échange : OMRAN l'exclut volontairement, mesuré sur ses 2 634 interactions de création.

Ne pas « harmoniser » les deux.

### Numérotation : un seul générateur, et on incrémente la ligne d'OMRAN

`services/sequence/GenerateurCodeSequence` sert les trois : `Tier` (`26/14431`), `Prospection`
(`26/05935`), `Dossier de vente` (`APF 2026/00380`, préfixe projet à la charge de l'appelant).
**C'est la même ligne `AdmSequence` que le logiciel de bureau incrémente** — un compteur parallèle
produirait des collisions, les deux applications écrivant dans `yesRems`.

### Le schéma légataire n'impose presque rien

`CfgTier`, `ComProspection`, `ComActionMessage` : **la seule colonne `NOT NULL` est `PKey`**. Toute
obligation est applicative. Les contrôles d'OMRAN à la création (non implémentés, voir backlog) :
SOUNDEX sur prénom+nom, unicité (nom+prénom+mobile) et (+email), une seule prospection ouverte par
couple prospect/tranche.

### `BoolField002`/`BoolField003` == `StatutDossier.isSortie()`

Vérifié sur les 13 statuts de l'entité de config du dossier (`E7C73F70-…`) : `BoolField002=1`
seulement sur ANNULATION, `BoolField003=1` seulement sur DESISTEMENT. **Rien à mapper.** Les
`CLÔTURE` qui portent le drapeau relèvent d'autres modules — d'où l'importance de filtrer sur
`StrConfigEntityId` avant de conclure.

### Le tableau de bord d'OMRAN (lot Q)

Trois blocs, sept requêtes. **Les tables de prévision sont vides des deux côtés** (`CfgPrevision`,
`ComPrevision`, `ComPrevisionVersion` = 0) — non mappées. La courbe restante somme
`ComDocument.Amount`, c'est-à-dire **l'échéancier, pas l'encaissé** : des mois futurs y figurent.
`ventesConcretisees` s'indexe sur `AdvanceDate`, que **232 des 387 dossiers n'ont pas** → compteur
`dossiersSansDateAvance`.

La requête légataire écrit `(… OR IsSaleExpense=1) AND IsSaleExpense=0` — deux clauses qui
s'annulent. **Écrire l'intention (« frais exclus »), pas le bug.**

### Les règles de groupabilité / agrégeabilité

1. Un chemin traversant une association **to-many** n'est jamais groupable (brise « somme des
   effectifs = `totalElements` »).
2. Un **`nvarchar(max)`** n'est pas groupable (`COUNT(DISTINCT)` le refuse).
3. Une valeur calculée doit être **portée par la base** — `@Formula` si arithmétique sur la même
   ligne, **pré-agrégation `@Subselect` joignable** si un terme est une somme (erreur 130).
4. Les fonctions autorisées se déduisent du `Chemin` (`FonctionAgregat.pour`).

### Le piège des colonnes « pleines »

`Group1Label/2/3` à 0/643 · `Category`/`ItemNumber`/`ItemDescription` à 643/643 mais **une seule
valeur distincte** · `AmountRS`/`AmountStamp` non nulles mais toujours à zéro. → **Mesurer distinct
et non-zéro, jamais `COUNT(colonne)`.**

### `@Formula` et `@Subselect` ne sont peuplés qu'au chargement

Un test transactionnel reçoit l'instance gérée, champs nuls. Remède : `flush` + `clear` avant
l'appel HTTP. Sans effet en production.

### ⚠️ Quatre bases locales, et `yesRemsTest` dérive

| Base | Rôle |
|---|---|
| `yesRems` | celle qu'Omran utilise — c'est elle que l'audit trace |
| `yesRemsProd` | restauration légataire, celle qui sert à mesurer (prescrite par `CLAUDE.md`) |
| `yesRemsCiCheck` | bâtie depuis `ci/schema/*.sql` |
| `yesRemsTest` | **le défaut** de `src/test/resources/application.properties` |

**Absentes de `yesRemsTest`** : `StlSettlementView`, `StlSettlementAmountAffectedView`,
`StlSettlementHistory`, `StlDeduction`, **`AdmParamsGuid`**. Et **0 statut de dossier semé** — les
tests fabriquent leurs `StrStatus`.

`ci/schema/010_tables.sql` est **généré**, sa graine étant « les tables mappées par une entité JPA ».
Mapper une table neuve oblige donc à toucher le schéma CI **et** la base de test locale.

### Conventions de commit

Sujets et corps **en ASCII pur, sans accents** — vérifié sur tout l'historique. Préfixes français
`feat(scope):` / `fix(scope):` / `docs(scope):` / `refactor(scope):`. Corps détaillé, souvent avec
une section **« Hors perimetre volontaire »** qui vaut contrat. Trailers `Co-Authored-By:` et
`Claude-Session:`.

---

## 📁 Important Files & Paths

- `instructions/FIX-creation-prospect-prospection.md` — lot R. §2 « seule `PKey` est NOT NULL »,
  §5 les trois contrôles non implémentés et pourquoi.
- `instructions/FEATURE-dashboard-omran.md` — lot Q. §2 prévisions vides, §5 pourquoi
  `AdmParamsGuid` n'est pas mappée.
- `instructions/FEATURE-dashboard-relances.md` — lot P. §3 le mur des retards, §4 les 108 non
  affectées.
- `instructions/FEATURE-affectations-encaissement.md` · `…-colonnes-groupement-encaissements.md` ·
  `…-encaissements-conformite-legataire.md` — lots L à N.
- `src/main/java/.../services/sequence/GenerateurCodeSequence.java` — numérotation partagée.
- `src/main/java/.../domain/{Chemin,FonctionAgregat,Colonnes,Colonne}.java` — contrat de groupement.
  `Colonne` est **`sealed`** depuis le lot O.
- `src/main/java/.../services/groupement/GroupementService.java` — moteur des quatre listes.
- `src/main/java/.../utils/{Champs,Requis,TierNomFormatter}.java` — idiomes maison.
- `src/main/resources/db/migration/007_ComActionMessage_drapeaux_api.sql` — **à appliquer à la main**.
- `../FEATURES.md` — index dérivé (33 routes, 169 endpoints). *Où* vit une feature ; la mémoire dit
  *pourquoi*. ⚠️ Son détecteur « endpoints sans appelant » **invente 16 faux positifs** (défaut de
  correspondance sur les chemins à paramètres) — signalé par le front, outil à corriger.

---

## 🔧 Environment & Config

- **Audit** : `sys.fn_get_audit_file('C:\backup\*.sqlaudit', DEFAULT, DEFAULT)` depuis `master`.
  Filtrer `application_name = 'OMRAN estate'`. `event_time` en UTC (local = +1).
- **Mesurer** : `sqlcmd -S localhost,49687 -d yesRemsProd -C` (identifiants dans `CLAUDE.md`).
  Chemins `-i`/`-o` via `cygpath -w`. `-W`/`-y` exclusifs, `-h`/`-y 0` exclusifs. Rediriger vers un
  fichier plutôt que piper dans `head`.
- **Tests** : `./mvnw.cmd test` fonctionne nu, ~3 min 30 pour 881 tests. SQL Server doit tourner.
- **Git** : tout via `node .claude/scripts/git-flow.mjs {status,start,commit,push,sync}` depuis
  `prototype/`. `guard-branch.mjs` **interdit toute écriture sur `main`** — demander une branche à
  l'utilisateur avant d'écrire quoi que ce soit, **y compris ce fichier**. Avant `push` :
  `feature-index.mjs add <slug>` puis `generate`.
- ⚠️ Ne jamais réécrire un fichier source via PowerShell (encodage). Heredocs Bash longs → cassent.
- ⚠️ Identifiants de service en clair au fil des sessions — dette connue.

### État des branches

**`main` = `c14c897`, propre, en phase avec `origin`. Aucune PR en attente.**
Le front (`crm-prototype`) n'est jamais touché — **tous ces lots sont backend seul**.

Branches mergées supprimables : `feat/java-21-conformite`, `feat/affectations-encaissement`,
`docs/memoire-session-lot-O`, `feat/dashboard-relances`, `feat/dashboard-omran`,
`fix/creation-prospect-prospection`. Anciennes non mergées : `docs/memoire-de-session`,
`feat/remise-dossier`, `fix/ci-pollution-doubles-envoi`.

---

## ⏭️ Next Step

> **Traiter la demande front du 15/09 : le filtre par colonne.** Le document complet a été fourni
> en fin de session et **n'est pas encore consigné dans `instructions/from_front.md`** — le faire
> en premier, c'est le canal officiel front→backend.
>
> Le front l'annonce « au-dessus du reste » et la raison est solide : c'est la seule demande dont
> l'absence produit un **résultat faux présenté comme complet**, sur les dix listes.

### Ce que la demande contient (résumé fidèle)

1. **`ColonneDto.filtrable` + `operateurs`** — n'existent pas aujourd'hui, vérifié sur les quatre
   catalogues. Sans eux le front ne peut qu'afficher une case sur toutes les colonnes et découvrir
   au 400 celles que le serveur refuse.
2. **`?filtreColonne=col:OPERATEUR:valeur`**, répétable, combiné en ET — même grammaire que
   `?agregats=prix:AVG` que le front sait déjà composer. Une colonne non filtrable, un opérateur
   refusé ou une valeur non parsable → **400 nommant la colonne**, jamais un 200 silencieux.
3. **Opérateurs par type** : TEXTE `CONTIENT|EGAL|COMMENCE_PAR` ; NOMBRE/MONTANT
   `EGAL|SUP|INF|ENTRE` ; DATE `EGAL|AVANT|APRES|ENTRE` (**valeurs ISO-8601**, la mise en forme
   reste au front) ; BOOLEEN `EGAL`.
4. **`CONTIENT`/`EGAL` sur TEXTE insensibles à la casse ET aux accents.** C'est le cœur : la colonne
   Type des encaissements contient `CHÉQUE` (159, accent aigu fautif du légataire), `CHEQUE SYNDIC`
   (14), `CHEQUE NOTAIRE` (6)… Taper « cheque » rend **0 ligne** aujourd'hui, « chèque » aussi,
   alors que **180 sur 332** correspondent. Précédent existant : `GET /api/hotline/recherche`
   documente déjà « insensible à la casse **et aux accents** ».

### Ce que ça débloque

Le lot de 500 lignes sur `/dossiers` disparaît · groupement et filtre cessent de s'exclure · le
filtre devient paginable (utilisable sur 9 654 prospections).

### Backlog

- [ ] **Filtre `?statut=` sur `/api/dossiers` par libellé** — demandé depuis le 10/09, remonté en
      priorité le 11/09. Mesure inchangée : `statut=CONCRETISATION` → 68, `statut=Concrétisation` →
      **0 en HTTP 200**. Aucune normalisation naïve ne rattrape (« Remise des clés » se code
      `REMISE DES CLEFS`, « Annulé » → `ANNULATION`). **Vraisemblablement résolu par le filtre par
      colonne ci-dessus** — vérifier avant de traiter séparément.
- [ ] **`ModifierProspectionRequest`** — ajouter `tempDescription`, `tempTel`, `tempEmail`,
      `commercialId` (servis par le GET, non modifiables ; ce sont les 4 champs éditables de
      l'écran légataire). Rien sur les 8 champs mesurés morts.
- [ ] **Les 13 nouveaux champs du DTO encaissement n'ont aucune description springdoc** — viole la
      règle 10 du `CLAUDE.md`. Taux mesurés par le front, pas lus : `bordereauCode` 0/332,
      `dateRemiseAuCentre` 1/332, `recuCode` 12/332.
- [ ] **Écart serveur sur `titreFoncier`** : la colonne de liste le sert sur 33/116 dossiers, la
      fiche ne le porte que 5 fois sur 8 de ces mêmes dossiers. Même donnée, deux chemins, taux
      différents — à confirmer.
- [ ] **`CreerReglementRequest.dateReglement`** : le spec la publie, le serveur la refuse (400).
      Contrat et comportement se contredisent — trancher côté backend.
- [ ] **Catalogue prospects** : sa description annonce que les drapeaux « pilotent la bande de
      groupement », or il a 0 groupable et il n'existe pas de `/api/prospects/groupes`. Formulation
      à corriger, sans conséquence fonctionnelle.
- [ ] **Les trois contrôles de création d'OMRAN** (SOUNDEX, unicité, prospection ouverte unique) —
      **décision produit** : bloquant (409, rupture de contrat à annoncer au front) ou consultatif ?
      Voir `FIX-creation-prospect-prospection.md` §5.
- [ ] **Alertes de jalon du dossier** — 4 `ComActionMessage` + 4 `AdmAlertData` à la vente. Bloqué
      sur décision produit : quels jalons, quelle échéance, quel libellé.
- [ ] **Encaissements non affectés** — 16 pièces, 6 435 400 DH, invisibles car sans ligne. L'écran
      légataire les affiche ; un opérateur qui compare les totaux voit 6,4 MDH d'écart.
- [ ] **Verrous pessimistes** `AdmWebLockEntities` + `AdmWebSessions` — séquence connue par la trace.
- [ ] **Seconde couche de permissions** — filtrage par statut visible (`StrStatusIntervener` ×
      `StrIntervener`). Le plus structurant des restes.
- [ ] **Ordre de pagination non déterministe** — signalé par le front : les ex æquo sur `docDate`
      sont départagés par le plan SQL du moment, ce qui peut cacher ou dupliquer une ligne entre
      deux pages. Absorbé côté front sur le groupement (`sort=docDate,desc&sort=id,asc`), **pas
      corrigé côté serveur**. Défaut latent réel.
- [ ] **Les 8 onglets absents de la fiche dossier** (Sous-articles, Modifications, Cadeaux,
      Dépenses, Factures proforma, Factures, Avoirs, Réclamations) — aucune demande immédiate, à
      prioriser avec le métier ; la facturation porte vraisemblablement le plus de valeur.
- [ ] Supprimer les six branches mergées.

# Session Memory
_Last updated: 2026-09-14T13:05+01:00_
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
| L | Encaissements : sélecteur de colonnes + groupement | mergé (PR #48) |
| M | Totaux des colonnes calculées + menu Σ + total général | mergé (PR #48) |
| **N** | **Fiche encaissement : les affectations + un 500 corrigé** | **poussé, PR en attente** |

**851 tests verts** (833 en début de session, +18).

---

## ✅ What Worked

- **Demander à la trace ce qu'elle sait, et accepter sa réponse quand elle est négative.**
  Question : « où est la requête de groupement de la grille encaissements ? » Réponse : **il n'y en
  a pas**. Dernière instruction SQL d'Omran à 10:35:31, capture d'écran à 11:36 — une heure sans
  aller-retour base. Sur 14 `GROUP BY` dans 4 528 lignes de trace, 12 sont des requêtes d'enquête,
  1 est un trigger, 1 est un tableau de bord. **Le légataire charge tout et calcule côté client**
  (grille DevExpress). Il n'y avait pas de mécanisme à copier.
- **Mesurer *distinct* et *non-zéro*, jamais `COUNT(colonne)`.** A évité deux erreurs symétriques
  en une passe : croire `Group1Label/2/3` utiles (0/643) et croire `Category`/`ItemNumber`/
  `ItemDescription` pleines (643/643 mais **une seule valeur distincte** — `None`, `Non définie`).
- **Vérifier le type SQL avant de déclarer une colonne groupable.** `Notes` est `nvarchar(max)` :
  le `COUNT(DISTINCT)` du comptage de groupes l'aurait refusé en 500. Trouvé avant d'écrire.
- **Tester la faisabilité SQL de la conception avant de l'écrire.** La route évidente pour
  `montantBrutRestant` (une `@Formula` à sous-requête corrélée, comme `delaiConcretisation`)
  **ne marche pas** : SQL Server refuse `SUM()` sur une expression contenant une sous-requête
  (**erreur 130**). Une requête d'essai a coûté deux minutes ; la découvrir à l'implémentation
  aurait coûté le lot.
- **Ouvrir une capacité sans rompre le contrat.** Le menu Σ (SUM/AVG/MIN/MAX/COUNT) a été livré
  avec un nom nu valant toujours SUM sous la même clé. Preuve : les **34 tests de groupement des
  dossiers et prospections passent sans une seule modification**.
- **Vérifier le SQL réellement émis**, pas seulement le test vert : `sum(Amount)` et `sum(Affecte)`
  séparés sur un `LEFT JOIN` vers le sous-select, exactement la forme mesurée à 7 ms.

---

## ❌ What Failed

- **`SUM(expression contenant une sous-requête)`** → SQL Server, **erreur 130**. Passer par une
  pré-agrégation **joignable**, et agréger les deux termes séparément.
- **`COUNT(colonne)` pour juger une colonne utile** → compte les non-nuls. Cinq occurrences
  cumulées dans ce dépôt.
- **`cb.coalesce(cb.sum(x), BigDecimal.ZERO)`** → arrondit la somme d'argent à l'unité en silence.
  Même famille : **`cb.avg()` rend un `double`**. Toute soustraction, substitution de `NULL` et
  division se font en Java, en `BigDecimal`.
- **`@ManyToOne` vers un `@Subselect` dont la clé étrangère est le `PKey` de l'entité elle-même**
  → toujours renseignée, donc Hibernate fabrique toujours un proxy ; si le sous-select n'a pas de
  ligne, le déréférencer lève `ObjectNotFoundException`. Voir bug n°12.
- **Piper `sqlcmd` dans `head`** → tronque silencieusement et fait croire qu'une section n'a rien
  rendu. Rediriger vers un fichier, puis lire.
- **`sqlcmd -W` et `-y` sont mutuellement exclusifs.**
- **Heredoc Bash contenant du Python à triple quote** → casse (`unexpected EOF`). Confirmé deux
  fois cette session. Écrire le script dans un fichier, puis `python fichier.py`.

---

## 🐛 Bugs & Errors Log

| # | Erreur / Bug | Statut | Correctif |
|---|---|---|---|
| 1 | 54 tests en erreur, `Nom d'objet/de colonne non valide` | **Fixed** | Dérive de `yesRemsTest`, base réalignée |
| 2 | Somme d'argent arrondie à l'unité (190,75 → 191,0) | **Fixed** | `COALESCE` SQL retiré, substitution en Java |
| 3 | Routes `/groupes` non déclarées à l'invariant de périmètre | **Fixed** | Ajoutées à `ROUTES_SANS_ID_AUTORISEES` avec justification |
| 4 | `@Formula` rendant `null` dans les tests | **Fixed** | `flush` + `clear` avant l'appel HTTP |
| 5 | `GROUP BY` avec sous-requête → 500 | **Fixed** | Colonne déclarée `agregeable` seulement |
| 6 | `clearAutomatically` détachant l'entité créée | **Fixed** | `flushAutomatically` |
| 7 | `TransientPropertyValueException` | **Fixed** | Réutiliser la valeur de retour de `save()` |
| 8 | Interactions exclues du « dernier échange » (`IsSystem`) | **Fixed** | Script `007_…sql`, **à appliquer à la main** |
| 9 | Prospection restée active après la vente | **Fixed** | `IsClosed`/`ClosingDate` |
| 10 | Dossiers absents de `StrHistory` | **Fixed** | Table écrite à la création |
| 11 | Décaissement de −470 000 DH faussant les totaux | **Fixed** | `IsCredit = 1`, étendu au groupement |
| 12 | **500 sur `GET /api/encaissements/{id}`** — `ObjectNotFoundException` sur `StlSettlementAffectation` | **Fixed** | Introduit par le lot M, **mergé**. Le sous-select ne portait que les règlements *ayant* des lignes ; **130 sur 3 243 (4 %)** n'en ont aucune. Sous-select repris depuis `StlSettlement` en `LEFT JOIN`. Non-régression : `reglementNonAffecte_rendUnTableauVide` |

---

## 🔑 Key Points to Remember

### La trace d'audit : ce qu'elle prouve, et ce qu'elle ne peut pas prouver

`sys.fn_get_audit_file('C:\backup\*.sqlaudit', DEFAULT, DEFAULT)` depuis `master`, compte `tsix`.
Scopée sur la base `yesRems`. **Vivante** : l'utilisateur peut rejouer un écran à la demande.

- Tronque à 4 000 caractères, ne montre pas les valeurs (`@P1…@Pn`), décalage d'une heure (UTC).
- **Un `SELECT vue.*` ne dit rien de l'affichage.** L'écran encaissements prend les 107 colonnes de
  `StlSettlementView` sans en nommer aucune, et **le légataire ne stocke pas la disposition de sa
  grille en base** (aucune table `*Layout*`/`*Grid*`/`*Column*` ; `AdmFormsLog` ne journalise que
  l'ouverture d'écran). Le relais est alors la **mesure**.
- **Le groupement et les totaux sont calculés côté client**, pas en SQL. Ne pas chercher à copier.

### Les règles qui décident de ce qui est groupable / agrégeable

1. Un chemin qui traverse une association **to-many** n'est jamais groupable : il compte plusieurs
   fois les lignes multi-rattachées et brise l'invariant « somme des effectifs = `totalElements` ».
   C'est ce qui exclut `dossierId`/`dossierCode` des encaissements (654 couples pour 643
   règlements, 8 multi-dossiers).
2. Un **`nvarchar(max)`** n'est pas groupable : `COUNT(DISTINCT)` le refuse.
3. Une valeur calculée **doit être portée par la base** pour être agrégée — `@Formula` si c'est de
   l'arithmétique sur la même ligne, **pré-agrégation `@Subselect` joignable** si un terme est
   lui-même une somme (erreur 130).
4. Les **fonctions** autorisées se déduisent du `Chemin` (`FonctionAgregat.pour`) et sont publiées
   par colonne dans le catalogue. Une **différence** n'offre ni MIN ni MAX : le moteur agrège ses
   deux termes séparément, exact pour un total et une moyenne, faux pour un extremum.

### Le piège des colonnes « pleines »

Trois familles rencontrées, toutes trompeuses au `COUNT` :
`Group1Label/2/3` à **0/643** · `Category`/`ItemNumber`/`ItemDescription` à 643/643 mais **une
seule valeur** · `AmountRS`/`AmountStamp`/`MapLatitude` non nulles mais **toujours à zéro**.
Et l'inverse : `StlSettlementHistory` couvre **100 %** des règlements mais n'est informative que
sur **65 sur 643** — les 578 autres ont une ligne unique qui recopie l'état courant (vérifié,
`Reference = Code` sur 578/578).

### `@Formula` et `@Subselect` ne sont peuplés qu'au **chargement**

Un test transactionnel qui construit ses fixtures puis interroge reçoit l'instance déjà gérée,
dont ces champs sont nuls. Remède : `relireDepuisLaBase()` = `flush` + `clear` avant l'appel HTTP.
**Sans effet en production**, où chaque requête ouvre sa propre transaction. Corollaire observé :
relue depuis la base, une colonne `decimal(26,6)` sort en `1000.000000`, pas `1000` — une
assertion écrite sur l'instance non relue portait sur l'échelle du littéral Java du test.

### Les catalogues `Colonne*` pilotent DEUX features

`ColonneDossier` / `ColonneProspection` / `ColonneProspect` / `ColonneEncaissement` alimentent le
sélecteur de colonnes **et** le groupement, pour quatre listes. Le cœur (`GroupementService`,
`Chemin`, `Colonnes`, `GroupeDto`, `ColonneDto`) est **partagé** : toute évolution les touche
toutes les quatre, et la garde de non-régression est que `DossierGroupementTest` et
`ProspectionGroupementTest` restent verts **sans modification**.

### Deux couches de permissions chez Omran, une seule chez nous

En plus du périmètre par société, Omran filtre **par statut visible** (`StrStatusIntervener` ×
`StrIntervener`). **Non implémenté.** Il pose aussi un **verrou pessimiste** par enregistrement
(`AdmWebLockEntities` + `AdmWebSessions`, 13 733 lignes) — la trace en donne maintenant la séquence
exacte : vérification à l'ouverture, `INSERT`, `UPDATE State=2` à la fermeture.

### ⚠️ Quatre bases locales, et `yesRemsTest` dérive

| Base | Rôle |
|---|---|
| `yesRems` | celle qu'Omran utilise — c'est elle que l'audit trace |
| `yesRemsProd` | restauration légataire, **celle qui sert à mesurer** |
| `yesRemsCiCheck` | bâtie depuis `ci/schema/*.sql` |
| `yesRemsTest` | **le défaut** de `src/test/resources/application.properties` |

**N'existent pas dans `yesRemsTest`** : `StlSettlementView`, `StlSettlementAmountAffectedView`,
`StlSettlementHistory`, `StlDeduction`. C'est pourquoi les pré-agrégations sont écrites en
**`@Subselect` défini en Java** plutôt qu'en mappant une vue légataire.

---

## 📁 Important Files & Paths

- **`instructions/FEATURE-affectations-encaissement.md`** — lot N. §1 la séquence du détail, §3 le
  piège de l'historique, §5 le 500 corrigé.
- `instructions/FEATURE-colonnes-groupement-encaissements.md` — lots L et M. §7 les totaux, le
  menu Σ et le total général ; §5.3 porte un bandeau disant que M renverse L.
- `instructions/FEATURE-encaissements-conformite-legataire.md` — lot K. §4 les encaissements non
  affectés, décision produit ouverte.
- `src/main/java/.../domain/{Chemin,FonctionAgregat,AgregatDemande,Colonnes}.java` — le contrat de
  groupement, partagé par les quatre listes.
- `src/main/java/.../entities/stl/StlSettlementAffectation.java` — `@Subselect`, et son entête
  explique les deux pièges (erreur 130, proxy non résolu).
- `src/main/java/.../services/groupement/GroupementService.java` — moteur des quatre listes.
- `src/main/resources/db/migration/007_ComActionMessage_drapeaux_api.sql` — **à appliquer à la
  main**.
- Mémoire auto (hors dépôt) : `trace-audit-omran-sqlaudit.md`,
  `fiche-encaissement-ecart-legataire.md`.

---

## 🔧 Environment & Config

- **Audit** : `sys.fn_get_audit_file('C:\backup\*.sqlaudit', DEFAULT, DEFAULT)` depuis `master`.
- **Mesurer** : `sqlcmd -S localhost,49687 -U tsix -P securePassword123 -d yesRemsProd -C`.
  Rediriger vers un fichier plutôt que de piper dans `head`. `-W` et `-y` exclusifs.
- **Tests** : `./mvnw.cmd test` fonctionne nu. Une colonne « absente » est presque toujours une
  dérive de base, pas un bug de mapping — comparer `INFORMATION_SCHEMA.COLUMNS`.
- **Toujours mesurer trois fois** : le cache est froid aux deux premières passes.
- ⚠️ Ne jamais réécrire un fichier source via PowerShell (encodage). Les heredocs Bash contenant du
  Python à triple quote cassent — écrire le script dans un fichier.
- ⚠️ Identifiants de service en clair au fil des sessions — dette connue, non traitée.

### État des branches

**Branche en cours : `feat/affectations-encaissement`, 1 commit, poussée, PR en attente.**
Le front (`crm-prototype`) n'est pas touché — **tous ces lots sont backend seul**.

| Repo | Branche | État |
|---|---|---|
| back | **`feat/affectations-encaissement`** | lot N, 851 tests verts — **en attente de merge** |
| back | `docs/memoire-de-session`, `feat/remise-dossier`, `fix/ci-pollution-doubles-envoi` | non mergées, anciennes |

Les neuf branches mergées ont été supprimées le 14/09 (`git branch -d`, jamais `-D`).

---

## ⏭️ Next Step

> **Attendre la merge de la PR `feat/affectations-encaissement`**, puis `git checkout main &&
> git pull --ff-only` et supprimer la branche.
>
> **Impact front à relayer** (aucune clé ne disparaît, mais deux changements de comportement) :
> `GET /api/encaissements/{id}` n'est plus filtré par `?colonnes=` et renvoie donc **plus** de clés
> qu'avant, plus le tableau `affectations` ; et `GET /api/referentiels/colonnes/*` publie désormais
> `fonctions` par colonne, sur quoi le menu Σ doit se piloter.

### Backlog

- [ ] **Alertes de jalon du dossier** — 4 `ComActionMessage` + 4 `AdmAlertData` à la vente.
      **Bloqué sur une décision produit** : quels jalons, à quelle échéance, quel libellé. La trace
      donne la forme (« l'état RESERVATION du dossier… » à +7 j), pas la règle.
- [ ] **Verrous pessimistes** `AdmWebLockEntities` + `AdmWebSessions` — séquence désormais connue
      par la trace. 13 733 lignes en base.
- [ ] **Seconde couche de permissions** — filtrage par statut visible
      (`StrStatusIntervener` × `StrIntervener`). Le plus structurant des restes.
- [ ] **Encaissements non affectés** — 16 pièces, 6 435 400 DH, invisibles car sans ligne.
      Décision produit ouverte.
- [ ] **Historique d'un règlement** (`StlSettlementHistory`) — trame des remplacements de chèque.
      Informatif sur 65 règlements sur 643 seulement, **et la table manque dans `yesRemsTest`** :
      l'ajouter au schéma CI *avant* de mapper.
- [ ] **Diff sémantique de `ComSaleProspectionView`** — 148 colonnes contre nos 56, en résolvant
      chaque colonne vers son origine réelle, jamais par comptage brut.
- [ ] **Journalisation d'usage** `AdmWebHistorics` / `AdmFormsLog`.
- [ ] **Onglets « Dates » / « Autres montants » du dossier** — attendent leurs captures.
- [ ] **Fiche tiers** — `ProspectDetailDto` 52 champs contre 97 dans `ComSaleProspectView`.

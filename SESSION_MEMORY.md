# Session Memory
_Last updated: 2026-09-15T11:32+01:00_
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
| N | Fiche encaissement : les affectations + un 500 corrigé | mergé (PR #49 et #51) |
| **O** | **Conformité Java 21 (hors trame OMRAN — demande ponctuelle)** | **mergé (PR #50)** |

**851 tests verts**, inchangés par le lot O (refactor à comportement constant).
`main` = `a4c2e3d`.

---

## ✅ What Worked

- **Mesurer la conformité avant de refactorer.** Le lot O était demandé comme un « huge
  refactor » ; l'audit a montré que le dépôt satisfaisait déjà l'essentiel de la règle —
  **115 DTO sur 115 en `record`**, 0 `@Data`, 0 `switch` ancien style (36 sites en `case ->`),
  0 thread brut, 0 `synchronized`, 0 `Optional.get()` non gardé, 1 seul `instanceof` et il est
  déjà un pattern match. Le livrable honnête était **6 changements sur ~8 fichiers**, pas une
  passe massive. Rapporter le périmètre réel avec les chiffres a évité de produire du bruit.
- **Lire le message du commit de refactor antérieur (`d20362c`, dans `main` depuis le 02/09).**
  Il porte les *principes que ce dépôt s'est donné*, introuvables ailleurs. Deux ont directement
  changé mes décisions en cours de route (voir Key Points). Un `git log --format='%B'` sur un
  commit structurant vaut mieux qu'une relecture du code.
- **Vérifier qu'une dépendance est déjà résolue avant d'envisager `pom.xml`.**
  `./mvnw.cmd dependency:tree -Dincludes='org.jspecify:jspecify'` → **jspecify 1.0.0 en transitif
  de Spring Framework 7**. `@Nullable` était donc utilisable sans toucher au `pom.xml` (règle 1
  du `CLAUDE.md` : ne jamais le modifier sans demander).
- **Relever tous les appelants avant de toucher une API publique**, puis juger *chaque site*
  plutôt que d'appliquer la règle en aveugle. C'est ce qui a distingué les conversions utiles des
  conversions-churn.
- **Éditer les sources Java en UTF-8 via `python - <<PY` en heredoc Bash**, avec `assert old in s`
  avant chaque `replace`. Les accents survivent (vérifié à l'`od -c`), et l'assertion transforme un
  motif introuvable en échec immédiat au lieu d'une édition silencieusement partielle.
- **Relancer la suite complète sur l'arbre exact qui part au commit** — trois passes ici (après le
  refactor, après le nettoyage des imports, après le revert). 3 min 30 la passe.

---

## ❌ What Failed

- **Convertir en `Optional` un parseur dont l'unique appelant déballe en `.orElse(null)`.**
  Fait sur `MarqueurTriProspection.depuis`, **puis reverti** : cela recréait exactement le motif
  que `d20362c` avait supprimé sur 49 sites. Test décisif : *un appelant déballe-t-il vers `null` ?*
  Si oui, ne pas convertir.
- **`COUNT(colonne)` pour juger une colonne utile** → compte les non-nuls. Cinq occurrences
  cumulées dans ce dépôt.
- **`SUM(expression contenant une sous-requête)`** → SQL Server, **erreur 130**. Passer par une
  pré-agrégation **joignable** et agréger les deux termes séparément.
- **`cb.coalesce(cb.sum(x), BigDecimal.ZERO)`** → arrondit la somme d'argent à l'unité en silence.
  Même famille : **`cb.avg()` rend un `double`**. Soustraction, substitution de `NULL` et division
  se font en Java, en `BigDecimal`.
- **`@ManyToOne` vers un `@Subselect` dont la FK est le `PKey` de l'entité elle-même** → proxy
  toujours fabriqué ; si le sous-select n'a pas de ligne, le déréférencer lève
  `ObjectNotFoundException` (bug n°12).
- **Piper `sqlcmd` dans `head`** → tronque silencieusement. Rediriger vers un fichier, puis lire.
- **`sqlcmd -W` et `-y` sont mutuellement exclusifs.**
- **Les heredocs Bash portant un document long avec accents, guillemets français et backticks**
  cassent (`unexpected EOF`). Confirmé à nouveau cette session en écrivant ce fichier. Utiliser
  l'outil d'écriture dédié, ou passer par un script Python dans un fichier.
- **Pousser la mémoire de session sur une branche *après* la merge de sa PR** → le commit reste
  orphelin. C'est arrivé à `55997f6` (mémoire du 13:05), resté invisible une session entière
  pendant que `main` portait la version périmée de 08:10. Récupéré par la PR #51.

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
| 12 | 500 sur `GET /api/encaissements/{id}` — `ObjectNotFoundException` | **Fixed** | Sous-select repris depuis `StlSettlement` en `LEFT JOIN` ; 130 règlements sur 3 243 (4 %) n'ont aucune ligne |
| 13 | Mémoire de session orpheline sur une branche post-merge | **Fixed** | PR #51 ; règle : commiter la mémoire **sur la branche, avant** la merge |

**Aucun bug ouvert.** Ce qui reste est **bloqué sur décision produit**, pas sur de la technique.

---

## 🔑 Key Points to Remember

### La doctrine `Optional` de ce dépôt (posée par `d20362c`, à respecter)

> « Un `Optional` exprime l'absence dans un **type de retour**, pas dans une **expression
> intermédiaire**. »

`d20362c` a supprimé 49 sites de `orNull(...).orElse(null)` dans `ArticleService` — « 49 `Optional`
alloués pour être déballés au token suivant ». **Test avant toute conversion : un appelant
déballe-t-il vers `null` ?** Si oui, laisser le `null`. Corollaires appliqués au lot O :

- **Convertir** quand le `null` pilote une branche de contrôle ou quand un appelant a un *vrai*
  défaut (`orElse(IN_WORK)`), un test (`filter().isPresent()`), un jet (`orElseThrow()`) ou une
  transformation (`map().orElseGet(List::of)`).
- **Ne pas convertir** les helpers privés qui alimentent directement un composant de `record`
  nullable — `mediane`, `ratio`, `parseIntOrNull`, les mappeurs `toPaysDto`/`toVilleDto`.
  Le `null` y est *documenté comme sémantiquement distinct de zéro*.
- `utils/Champs.de(source, lecteur)` est l'idiome maison pour **naviguer** un graphe nullable
  (~220 sites). Ce n'est pas la même chose qu'un parseur : ne pas le remplacer par `Optional`.

### Ce qui ne doit PAS être refactoré, et pourquoi

- **Les entités JPA** ne peuvent pas être des `record` (constructeur sans argument, champs
  mutables) et `CLAUDE.md` impose `@Getter @Setter` — pas `@Data`.
- **`requireNonNull` sur les constructeurs injectés par Spring** : Spring échoue déjà au démarrage.
- **`CompletableFuture` / threads virtuels** : aucun éventail d'E/S à paralléliser. L'envoi de mail
  est déjà `@Async("mailExecutor")` sur un `ThreadPoolTaskExecutor` géré, arrêt gracieux configuré.
- **`web/ColonnesSelection`** : le `ThreadLocal` est délibéré et documenté (Jackson 3 de Spring
  Boot 4.1 ne rencontre pas le `MappingJacksonValue` typé Jackson 2), nettoyé dans un `finally`.
- `PerimetreCommercialService` / `PerimetreInterceptor` étaient déclarés **hors périmètre** par
  `d20362c` (« autorisation, fail-closed par construction »). Le lot O a touché `parseUuid` de
  l'interceptor **en connaissance de cause** — c'est le site où l'absence doit être visible du
  compilateur, un oubli y valant contournement de périmètre (faille rejouée deux fois, `%2D` puis
  `%20`). Couvert par 87 tests de `controller/perimetre`, dont une classe adversariale.

### La trace d'audit : ce qu'elle prouve, et ce qu'elle ne peut pas prouver

`sys.fn_get_audit_file('C:\backup\*.sqlaudit', DEFAULT, DEFAULT)` depuis `master`, compte `tsix`.
Scopée sur la base `yesRems`. **Vivante** : l'utilisateur peut rejouer un écran à la demande.

- Tronque à 4 000 caractères, ne montre pas les valeurs (`@P1…@Pn`), décalage d'une heure (UTC).
- **Un `SELECT vue.*` ne dit rien de l'affichage.** L'écran encaissements prend les 107 colonnes de
  `StlSettlementView` sans en nommer aucune, et **le légataire ne stocke pas la disposition de sa
  grille en base** (aucune table `*Layout*`/`*Grid*`/`*Column*`). Le relais est alors la **mesure**.
- **Le groupement et les totaux sont calculés côté client** (grille DevExpress), pas en SQL.
  Prouvé négativement : une heure sans aller-retour base entre la dernière instruction et la
  capture d'écran. Ne pas chercher à copier un mécanisme qui n'existe pas.

### Les règles qui décident de ce qui est groupable / agrégeable

1. Un chemin traversant une association **to-many** n'est jamais groupable : il brise l'invariant
   « somme des effectifs = `totalElements` ». Exclut `dossierId`/`dossierCode` des encaissements
   (654 couples pour 643 règlements).
2. Un **`nvarchar(max)`** n'est pas groupable : `COUNT(DISTINCT)` le refuse.
3. Une valeur calculée doit être **portée par la base** pour être agrégée — `@Formula` si c'est de
   l'arithmétique sur la même ligne, **pré-agrégation `@Subselect` joignable** si un terme est
   lui-même une somme (erreur 130).
4. Les fonctions autorisées se déduisent du `Chemin` (`FonctionAgregat.pour`). Une **différence**
   n'offre ni MIN ni MAX : le moteur agrège ses deux termes séparément.

### Le piège des colonnes « pleines »

`Group1Label/2/3` à **0/643** · `Category`/`ItemNumber`/`ItemDescription` à 643/643 mais **une
seule valeur distincte** · `AmountRS`/`AmountStamp`/`MapLatitude` non nulles mais **toujours à
zéro**. Et l'inverse : `StlSettlementHistory` couvre 100 % des règlements mais n'est informative
que sur **65 sur 643**. → **Mesurer distinct et non-zéro, jamais `COUNT(colonne)`.**

### Les catalogues `Colonne*` pilotent DEUX features

`ColonneDossier` / `ColonneProspection` / `ColonneProspect` / `ColonneEncaissement` alimentent le
sélecteur de colonnes **et** le groupement, pour quatre listes. Le cœur (`GroupementService`,
`Chemin`, `Colonnes`, `GroupeDto`, `ColonneDto`) est **partagé**. Garde de non-régression :
`DossierGroupementTest` et `ProspectionGroupementTest` restent verts **sans modification**.
Depuis le lot O, `Colonne` est **`sealed`** — ajouter un 5ᵉ catalogue exige de l'inscrire au
`permits`, ce qui est voulu.

### `@Formula` et `@Subselect` ne sont peuplés qu'au **chargement**

Un test transactionnel qui construit ses fixtures puis interroge reçoit l'instance déjà gérée,
champs nuls. Remède : `relireDepuisLaBase()` = `flush` + `clear` avant l'appel HTTP. **Sans effet
en production.** Corollaire : relue depuis la base, une `decimal(26,6)` sort en `1000.000000`.

### Deux couches de permissions chez Omran, une seule chez nous

En plus du périmètre par société, Omran filtre **par statut visible** (`StrStatusIntervener` ×
`StrIntervener`) — **non implémenté**. Il pose aussi un **verrou pessimiste** par enregistrement
(`AdmWebLockEntities` + `AdmWebSessions`, 13 733 lignes) ; la trace en donne la séquence exacte.

### ⚠️ Quatre bases locales, et `yesRemsTest` dérive

| Base | Rôle |
|---|---|
| `yesRems` | celle qu'Omran utilise — c'est elle que l'audit trace |
| `yesRemsProd` | restauration légataire, **celle qui sert à mesurer** |
| `yesRemsCiCheck` | bâtie depuis `ci/schema/*.sql` |
| `yesRemsTest` | **le défaut** de `src/test/resources/application.properties` |

**N'existent pas dans `yesRemsTest`** : `StlSettlementView`, `StlSettlementAmountAffectedView`,
`StlSettlementHistory`, `StlDeduction`. D'où des pré-agrégations en **`@Subselect` défini en Java**
plutôt qu'un mapping de vue légataire.

### Conventions de commit du dépôt

- Sujets et corps **en ASCII pur, sans accents** (`repete`, `identique a la baseline`) — vérifié
  sur tout l'historique. Préfixes : `feat(scope):`, `fix(scope):`, `docs(scope):`,
  `refactor(scope):`, en français.
- Corps détaillé, avec souvent une section **« Hors perimetre volontaire »** qui vaut contrat pour
  les sessions suivantes. **La lire avant de toucher un fichier qu'elle nomme.**
- Trailers `Co-Authored-By:` et `Claude-Session:` présents sur les commits récents.

---

## 📁 Important Files & Paths

- `instructions/FEATURE-affectations-encaissement.md` — lot N. §1 séquence du détail, §3 piège de
  l'historique, §5 le 500 corrigé.
- `instructions/FEATURE-colonnes-groupement-encaissements.md` — lots L et M. §7 totaux, menu Σ,
  total général ; §5.3 porte un bandeau disant que M renverse L.
- `instructions/FEATURE-encaissements-conformite-legataire.md` — lot K. §4 décision produit ouverte.
- `src/main/java/.../domain/Colonne.java` — **`sealed`** depuis le lot O.
- `src/main/java/.../domain/{Chemin,FonctionAgregat,AgregatDemande,Colonnes}.java` — contrat de
  groupement, partagé par les quatre listes.
- `src/main/java/.../domain/StatutReglement.java` — `lireCode()` rend un `Optional` (lot O ;
  `depuisCodeOuNull` n'existe plus).
- `src/main/java/.../entities/stl/StlSettlementAffectation.java` — `@Subselect`, son entête
  explique les deux pièges (erreur 130, proxy non résolu).
- `src/main/java/.../services/groupement/GroupementService.java` — moteur des quatre listes.
- `src/main/java/.../utils/{Champs,Requis}.java` — idiomes maison de `d20362c`.
- `src/main/resources/db/migration/007_ComActionMessage_drapeaux_api.sql` — **à appliquer à la main**.
- `../FEATURES.md` — index dérivé (33 routes, 166 endpoints, 17 non appelés). *Où* vit une feature ;
  la mémoire de session dit *pourquoi*.
- Mémoire auto (hors dépôt) : `trace-audit-omran-sqlaudit.md`,
  `fiche-encaissement-ecart-legataire.md`, `backend-only-never-touch-frontend.md`.

---

## 🔧 Environment & Config

- **Audit** : `sys.fn_get_audit_file('C:\backup\*.sqlaudit', DEFAULT, DEFAULT)` depuis `master`.
- **Mesurer** : `sqlcmd -S localhost,49687 -d yesRemsProd -C` (identifiants dans `CLAUDE.md`).
  Rediriger vers un fichier plutôt que de piper dans `head`. `-W` et `-y` exclusifs.
- **Tests** : `./mvnw.cmd test` fonctionne nu, ~3 min 30 pour 851 tests. SQL Server doit tourner —
  vérifier avant de dispatcher, sinon la panne ressemble à un bug de code.
- Une colonne « absente » est presque toujours une **dérive de base**, pas un bug de mapping —
  comparer `INFORMATION_SCHEMA.COLUMNS`.
- **Mesurer une requête trois fois** : le cache est froid aux deux premières passes.
- **Git** : tout passe par `node .claude/scripts/git-flow.mjs {status,start,commit,push,sync}`
  depuis `prototype/`. `.claude/hooks/guard-branch.mjs` **interdit toute écriture quand le repo est
  sur `main`** — donc *demander une branche à l'utilisateur* avant d'écrire quoi que ce soit, y
  compris ce fichier. Avant `push` : `feature-index.mjs add <slug>` puis `generate`.
- ⚠️ **Ne jamais réécrire un fichier source via PowerShell** (encodage cassé plusieurs fois).
  Bash + heredoc Python fonctionne pour des retouches ciblées (UTF-8 vérifié) ; pour un document
  long, utiliser l'outil d'écriture dédié. Effet de bord bénin : les écritures Python passent la
  copie de travail en LF alors que `core.autocrlf=true` ; le blob commité reste correct, le diff ne
  montre que les vraies lignes.
- ⚠️ Identifiants de service en clair au fil des sessions — dette connue, non traitée.

### État des branches

**`main` = `a4c2e3d`, propre, en phase avec `origin`. Aucune PR en attente.**
Le front (`crm-prototype`) n'est pas touché — **tous ces lots sont backend seul**.

| Branche | État |
|---|---|
| `feat/java-21-conformite`, `feat/affectations-encaissement` | **mergées (PR #50, #51) — supprimables** (`git branch -d`, jamais `-D`) |
| `docs/memoire-de-session`, `feat/remise-dossier`, `fix/ci-pollution-doubles-envoi` | non mergées, anciennes |

---

## ⏭️ Next Step

> **Demander à l'utilisateur sa politique d'alertes de jalon.** C'est ce qui bloque l'étape 4 du
> lot J et c'est une décision **produit, pas technique** : quels jalons déclenchent une alerte, à
> quelle échéance, avec quel libellé. La trace donne la *forme* (« l'état RESERVATION du dossier… »
> à +7 j, « FRAIS SYNDIC (16 800 DH) arrivé à échéance »), jamais la *règle*.

### Backlog

- [ ] **Alertes de jalon du dossier** — 4 `ComActionMessage` + 4 `AdmAlertData` à la vente.
      Bloqué sur la décision produit ci-dessus.
- [ ] **Verrous pessimistes** `AdmWebLockEntities` + `AdmWebSessions` — séquence connue par la
      trace, 13 733 lignes en base.
- [ ] **Seconde couche de permissions** — filtrage par statut visible
      (`StrStatusIntervener` × `StrIntervener`). Le plus structurant des restes.
- [ ] **Encaissements non affectés** — 16 pièces, 6 435 400 DH, invisibles car sans ligne.
      Décision produit ouverte.
- [ ] **Historique d'un règlement** (`StlSettlementHistory`) — informatif sur 65 règlements sur
      643 seulement, **et la table manque dans `yesRemsTest`** : l'ajouter au schéma CI *avant*
      de mapper.
- [ ] **Diff sémantique de `ComSaleProspectionView`** — 148 colonnes contre nos 56, en résolvant
      chaque colonne vers son origine réelle, jamais par comptage brut.
- [ ] **Journalisation d'usage** `AdmWebHistorics` / `AdmFormsLog`.
- [ ] **Onglets « Dates » / « Autres montants » du dossier** — attendent leurs captures.
- [ ] **Fiche tiers** — `ProspectDetailDto` 52 champs contre 97 dans `ComSaleProspectView`.
- [ ] Supprimer les deux branches mergées (`feat/java-21-conformite`,
      `feat/affectations-encaissement`).
- [ ] *(optionnel, si le sujet Java 21 revient)* annotations `@NullMarked`/`@Nullable` jspecify
      systématiques par `package-info` — écarté ce lot faute de vérificateur au build ; l'ajouter
      (NullAway) exigerait une modification du `pom.xml`, donc l'accord de l'utilisateur.

# Session Memory
_Last updated: 2026-09-11T17:40+01:00_
_Project: immo-crm (AyadERP) — C:\Users\tsiory\Documents\ayad-internship\prototype\immo-crm_

---

## 🎯 Current Objective

**Aligner nos endpoints sur ce que le logiciel de bureau (OMRAN Estate) fait réellement**, en
partant de sa **trace d'audit SQL Server** plutôt que de captures d'écran ou de lectures de vue.

Quatre lots livrés dans cette session, dans cet ordre :

| Lot | Sujet | État |
|---|---|---|
| G | Groupement et agrégats sur les listes | **mergé** (PR #43) |
| H | Groupement sur *toutes* les colonnes + groupe vide écarté | **mergé** (PR #44) |
| I | Colonnes du dossier vues dans la trace (article, responsable, délais) | **mergé** (PR #45) |
| **J** | **Conformité au légataire : écritures manquantes** | **branche poussée, PR en attente** |

**813 tests verts** (763 en début de session, +50).

---

## ✅ What Worked

- **Partir de la trace d'audit SQL Server plutôt que du code ou des captures.** C'est la méthode
  la plus productive de la session :
  ```sql
  SELECT event_time, action_id, succeeded, server_principal_name,
         database_name, schema_name, object_name, statement
  FROM sys.fn_get_audit_file('C:\backup\*.sqlaudit', DEFAULT, DEFAULT)
  ORDER BY event_time DESC;
  ```
  Elle donne la **séquence exacte des écritures** d'Omran (auth, liste, ouverture de dossier,
  transformation en vente, ajout d'interaction).
- **Lire `AdmWebHistorics` / `ComActionMessage` en base** pour qualifier une action : l'audit ne
  montre que `@P1…@Pn`, la table montre les valeurs.
- **Mesurer le coût avant de choisir une conception.** Le groupement par `derniereVisiteDate` :
  5 496 ms via la vue légataire, ~1 s via une pré-agrégation large, **6 ms** via une pré-agrégation
  étroite (`ComProspectionVisites`). Deux ordres de grandeur pour un `WHERE` de plus.
- **`@Formula` pour les colonnes calculées** (délais de cycle) : elles deviennent des propriétés
  ordinaires, donc groupables et sommables sans toucher au moteur de groupement. Coût mesuré
  négligeable (1 ms sur une page de 20).
- **`@ManyToOne` paresseux vers une entité `@Subselect`**, jamais `@OneToOne` : un `@OneToOne`
  optionnel ne se proxifie pas et produit un N+1 sur tout le domaine.
- **Vérifier une régression supposée en remisant ses changements** (`git stash` + rejouer le test)
  avant d'accuser son propre code. A servi deux fois, et a innocenté le code les deux fois.

---

## ❌ What Failed

- **`COUNT(colonne)` pour mesurer si une colonne est utile** → compte les non-nuls, pas les valeurs
  utiles. **Trois fois** dans cette session : `PenalityDescription` (non nulle, vide partout),
  `FinalTitleNumber` (idem), `MapLatitude` (non nulle, **à zéro** sur 14 033 lignes — annoncée à
  tort comme « le plus gros manque »). **Compter les distinctes ou les non-zéro.**
- **Compter les colonnes d'une vue pour chiffrer un écart** → surestime massivement.
  `H1/H2/H3` du tri légataire semblaient absentes ; ce sont la tranche, le projet et la société,
  **déjà servis** sous d'autres noms. Résoudre chaque colonne vers son origine avant de conclure.
- **`cb.coalesce(cb.sum(x), BigDecimal.ZERO)`** → tire le type du résultat vers l'échelle du
  littéral : `100,50 + 90,25` revenait à **191,0**. Somme d'argent arrondie à l'unité, en silence.
  Substituer le `NULL` en Java, pas en SQL.
- **`@Modifying(clearAutomatically = true)`** → vide le contexte de persistance et détache
  l'entité que l'appelant vient de créer. Trois tests de création sont tombés dessus.
  `flushAutomatically` suffit.
- **Une grosse pré-agrégation couvrant six colonnes d'un coup** → ~1 s. L'étroite, 6 ms.
- **`GROUP BY` sur une expression contenant une sous-requête** → SQL Server refuse. D'où
  `delaiConcretisation` sommable mais non groupable.

---

## 🐛 Bugs & Errors Log

| # | Erreur / Bug | Statut | Correctif |
|---|---|---|---|
| 1 | 54 tests en erreur, `Nom d'objet/de colonne non valide` | **Fixed** | Dérive de `yesRemsTest` (163 colonnes, `StrEntity`, `ComSaleProspectionView` manquants). Base réalignée depuis `yesRemsProd` et `ci/schema/050_views.sql` |
| 2 | Somme d'argent arrondie à l'unité (190,75 → 191,0) | **Fixed** | `COALESCE` SQL retiré, substitution du `NULL` en Java. Test `sommeDesMontants_conserveLesCentimes` |
| 3 | `PerimetreInterceptorInvarianteTest` — 3 routes `/groupes` non déclarées | **Fixed** | Ajoutées à `ROUTES_SANS_ID_AUTORISEES` avec justification (scopées par la Specification, pas par l'intercepteur) |
| 4 | `@Formula` rendant `null` dans les tests | **Fixed** | Une formule n'est peuplée qu'au **chargement** : `flush` + `clear` avant l'appel HTTP (`relireDepuisLaBase()`) |
| 5 | `GROUP BY` avec sous-requête → `SQLGrammarException` en 500 | **Fixed** | `delaiConcretisation` déclaré `agregeable` seulement → 400 lisible |
| 6 | `clearAutomatically` détachant le dossier créé | **Fixed** | `flushAutomatically` |
| 7 | `TransientPropertyValueException` dans les tests | **Fixed** | `save()` renvoie l'instance **gérée** (identifiant assigné à la main → `merge`) ; réutiliser sa valeur de retour |
| 8 | **Nos interactions exclues du « dernier échange »** | **Fixed** | `IsSystem` n'était pas posé ; la vue filtre `AND IsSystem = 0` et `NULL` ne satisfait pas `= 0`. 14 lignes en base, 8 prospections. Script `007_…sql` |
| 9 | Prospection restée active après la vente | **Fixed** | `IsClosed`/`ClosingDate` + désarmement des relances dans `DossierService.create` |
| 10 | Dossiers absents de `StrHistory` | **Fixed** | Table lue pour l'onglet Historique mais jamais écrite |

---

## 🔑 Key Points to Remember

### ⚠️ Il y a QUATRE bases locales, et elles ne disent pas la même chose

| Base | Tables | Rôle |
|---|---:|---|
| `yesRems` | — | **celle qu'Omran utilise** — c'est elle que l'audit trace |
| `yesRemsProd` | 462 | restauration légataire, **celle que `CLAUDE.md` prescrit pour mesurer** |
| `yesRemsCiCheck` | 167 | bâtie depuis `ci/schema/*.sql` |
| `yesRemsTest` | 86+ | **le défaut** de `src/test/resources/application.properties` |

`yesRems` ≠ `yesRemsProd` : 387 dossiers contre 358, 17 678 prospections contre 17 939. Même
schéma, deux restaurations différentes, **ni l'une ni l'autre n'est un sous-ensemble**. Vérifier
`database_name` dans l'audit avant de rapporter une mesure à une trace.

### L'audit SQL et ses limites

- Il est **vivant** : une requête lancée maintenant y apparaît en quelques secondes.
- Il **tronque les requêtes à 4 000 caractères**.
- Il **ne montre pas les valeurs** : `INSERT ComFolder(112 colonnes) VALUES (@P1…@P112)`. La liste
  de colonnes est celle de l'ORM d'Omran, **pas** les champs réellement renseignés. Ce qu'il prouve,
  c'est la **séquence des effets de bord**.
- **Décalage d'une heure** : audit en UTC, application en heure locale.
- Les requêtes sur `sys.all_objects` / `clmns.` sont l'introspection de l'IDE, **pas Omran**.

### La séquence de transformation prospection → vente (relevée le 11/09)

1. `UPDATE ComProspection` — clôture ✅ *(livré lot J)*
2. `INSERT AdmAttachedFile` ❌
3. `UPDATE AdmSequence SeqValue+1` ✅
4. `INSERT ComFolder` ✅
5. `INSERT ComDocument` ×3 (schéma de financement) ✅
6. `INSERT ComActionMessage` ×4 + `AdmAlertData` ×4 — **alertes de jalon** ❌ *(reste)*
7. `INSERT StrHistory` ✅ *(lot J)*
8. `UPDATE AdmAlertData Active=0` + `ComActionMessage WithoutRevival=1` ✅ *(lot J)*
9. `UPDATE StkItem comFolderId` ✅
10. `INSERT StrFolderStatusHistory` ✅ *(enrichi lot J)*
11. `UPDATE CfgTier` — **re-normalisation, pas une transition**

### `UPDATE CfgTier` : le statut client est DÉRIVÉ, jamais stocké

La requête force le type à `PROSPECT` pour **tout tiers sans dossier actif**. Après une vente, le
tiers sort du `WHERE` et n'est donc plus ramené à prospect. **Ne jamais écrire un « type client »
à la création** — ce serait inventer un état que le légataire ne stocke pas.

### Deux couches de permissions chez Omran, une seule chez nous

En plus du périmètre par société (`AdmUserHabilitation`, que nous avons), Omran filtre les dossiers
**par statut visible** via `StrStatusIntervener` × `StrIntervener`, par rôle *ou* par utilisateur
nommé. `PerimetreCommercialService` ne connaît que le projet. **Non implémenté.**

### Le catalogue `Colonne*` pilote DEUX features

`ColonneDossier` / `ColonneProspection` alimentent **le sélecteur de colonnes ET le groupement**.
Toute entrée ajoutée doit avoir son champ dans le DTO correspondant, sinon le sélecteur annonce une
colonne absente de la réponse. Une colonne avec un `Chemin` devient groupable **sans une ligne dans
le moteur**.

### Couverture de groupement atteinte

`ColonneDossier` **40/40** puis 56/56 après le lot I · `ColonneProspection` **44/56** — les 12
restantes sont exclues **sur mesure de coût**, pas sur opinion (5 colonnes agrégeant 46 667 lignes
à ~1 s, 2 colonnes d'alerte en `ROW_NUMBER` à 0,7–4,7 s, 3 colonnes de dossier dont l'association
inverse n'est pas proxifiable, `typeProspection` à 4 lignes sur 17 939).

---

## 📁 Important Files & Paths

- **`instructions/FEATURE-conformite-legataire.md`** — le lot en cours (J). §1 le défaut `IsSystem`
  prouvé, §2 la clôture de la prospection, §4 **ce qui ne sera pas ajouté et pourquoi**, §5 le reste.
- `instructions/FEATURE-groupement-listes.md` — lot G. ⚠️ **§2 et §4.1 sont périmés**, un bandeau le
  dit en tête.
- `instructions/FEATURE-groupement-toutes-colonnes.md` — lot H, renverse deux décisions du lot G.
- `instructions/FEATURE-colonnes-dossier-ecran-legataire.md` — lot I. §1 explique l'erreur
  `H1/H2/H3`.
- `src/main/resources/db/migration/007_ComActionMessage_drapeaux_api.sql` — **à appliquer à la
  main**, rattrape les 14 interactions écrites sans drapeaux.
- `src/main/java/.../domain/Chemin.java` — le chemin JPA des colonnes de catalogue, pièce centrale
  du groupement.
- `src/main/java/.../entities/com/ComProspectionVisites.java` — `@Subselect` étroit, 6 ms contre
  5 496 ms par la vue.
- `src/main/java/.../services/groupement/GroupementService.java` — moteur générique des trois listes.
- `target/audit-full.json`, `target/audit-transfo.json` — traces exportées (gitignored).

---

## 🔧 Environment & Config

- **Lire l'audit d'Omran** : `sys.fn_get_audit_file('C:\backup\*.sqlaudit', DEFAULT, DEFAULT)`
  depuis `master`, compte `tsix`. L'audit est **scopé sur la base `yesRems`**.
- **Tests** : `./mvnw.cmd test` fonctionne nu depuis le réalignement de `yesRemsTest`
  (11/09). Avant de suspecter le code sur une colonne absente, **comparer les bases** :
  ```sql
  SELECT COUNT(*) FROM yesRemsProd.INFORMATION_SCHEMA.COLUMNS p
  JOIN yesRemsTest.INFORMATION_SCHEMA.TABLES tt ON tt.TABLE_NAME = p.TABLE_NAME
  WHERE NOT EXISTS (SELECT 1 FROM yesRemsTest.INFORMATION_SCHEMA.COLUMNS t
                    WHERE t.TABLE_NAME = p.TABLE_NAME AND t.COLUMN_NAME = p.COLUMN_NAME);
  ```
- **Mesurer une requête** : ouvrir **une seule** `SqlConnection` .NET et boucler — le cache est
  froid aux deux premières passes (vu 1 238 → 627 → **6 ms** sur la même requête). **Toujours
  mesurer trois fois** avant de conclure sur une performance.
- ⚠️ **Ne jamais réécrire un fichier source via PowerShell** (encodage cassé plusieurs fois).
  Les heredocs Bash contenant du Python à triple quote cassent aussi — préférer l'outil d'édition.
- ⚠️ Identifiants de service échangés en clair au fil des sessions — dette connue, non traitée ici.

### État des branches

**Branche en cours : `feat/conformite-legataire`, 2 commits, poussée, PR en attente de merge.**
Le front (`crm-prototype`) est sur `main`, intact — **tous ces lots sont backend seul**.

| Repo | Branche | État |
|---|---|---|
| back | **`feat/conformite-legataire`** | lot J, 813 tests verts — **en attente de merge** |
| back | `feat/groupement-listes`, `feat/groupement-toutes-colonnes`, `feat/colonnes-dossier-ecran-legataire` | **mergées (PR #43/#44/#45) — supprimables** |
| back | `feat/fiche-dossier-alignement-ecran`, `feat/contexte-dossier` | mergées — supprimables |
| back | `fix/ci-pollution-doubles-envoi` | PR #38 mergée mais la locale a divergé (1 commit hors `main`) |
| back | `docs/memoire-de-session` (2 commits), `feat/remise-dossier` (1 commit, aucun remote) | non mergées |
| front | `feat/groupement-listes` | **construit par une autre session** — consomme le lot G |
| front | `refractor/best-practice` | **11 commits, LOCAL SEUL** — remote supprimé sans merge |

---

## ⏭️ Next Step

> **Attendre la merge de la PR `feat/conformite-legataire`**, puis `sync`. Ne rien démarrer qui en
> dépende avant.
>
> **Puis demander à l'utilisateur sa politique d'alertes de jalon** — c'est ce qui bloque l'étape 4
> du lot J, et c'est une décision **produit, pas technique** : quels jalons déclenchent une alerte,
> à quelle échéance, avec quel libellé. La trace donne la forme
> (« l'état RESERVATION du dossier… » à +7 j, « FRAIS SYNDIC (16 800 DH) arrivé à échéance »), pas
> la règle.
>
> **Impact front à relayer** (aucune rupture de contrat, mais un changement de comportement voulu) :
> après `POST /api/dossiers`, la prospection transformée **disparaît de `GET /api/prospections`**.
> Un écran qui la cherchait après la vente ne la trouvera plus.

### Backlog

- [ ] **Alertes de jalon du dossier** — 4 `ComActionMessage` + 4 `AdmAlertData` à la vente.
      Bloqué sur la décision produit ci-dessus.
- [ ] **Diff sémantique de `ComSaleProspectionView`** — 148 colonnes contre nos 56. À faire en
      résolvant chaque colonne vers son origine réelle, **jamais par comptage brut** (leçon
      `H1/H2/H3`).
- [ ] **Seconde couche de permissions** — filtrage par statut visible
      (`StrStatusIntervener` × `StrIntervener`). Le plus structurant des restes.
- [ ] **Verrous pessimistes** `AdmWebLockEntities` + `AdmWebSessions` : deux utilisateurs ne peuvent
      pas ouvrir le même dossier en écriture chez Omran.
- [ ] **Journalisation d'usage** `AdmWebHistorics` / `AdmFormsLog`.
- [ ] **Onglets « Dates » / « Autres montants » du dossier** — attendent leurs captures
      (`FEATURE-fiche-dossier-alignement-ecran.md` §6).
- [ ] **Décision produit : filtrer les doublons de pré-prospection ?** 798 sur 3 818 (20,9 %) sont
      du déchet marqué.
- [ ] **Fiche tiers** — `ProspectDetailDto` 52 champs contre 97 dans `ComSaleProspectView`.
- [ ] Supprimer les branches mergées (`git branch -d`, jamais `-D`).

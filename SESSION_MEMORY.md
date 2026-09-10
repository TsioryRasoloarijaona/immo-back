# Session Memory
_Last updated: 2026-09-10T00:00+01:00_
_Project: immo-crm (AyadERP) — C:\Users\tsiory\Documents\ayad-internship\prototype\immo-crm_

---

## 🎯 Current Objective

**Exploiter les 216 vues du légataire** pour combler l'écart entre ce que nos `GET` renvoient et
ce que le logiciel de bureau affiche. Le manque était réel et chiffré : 24 champs dans
`DossierDetailDto` contre **217 colonnes** dans `ComSaleFolderView`, 45 contre 148 pour la
prospection.

| # | Lot | État |
|---|---|---|
| 1 | Fiche prospection + interactions (vues `@Immutable`) | **mergé** (PR #31, `01ce74c`) |
| 2 | Sélection de colonnes `?colonnes=` sur la liste | poussé, **non mergé** |
| 3 | Inventaire des 148 colonnes | poussé, **non mergé** |
| A | Retrait des champs morts (budget/surface) | poussé, **non mergé** |
| B | 18 colonnes manquantes ajoutées à la liste | poussé, **non mergé** |
| C | 15 filtres avancés | poussé, **non mergé** |

Branche en cours : **`feat/selection-colonnes-listes`**, 6 commits, PR ouverte.
**673 tests verts** (648 en début de session, +25).

---

## ✅ What Worked

- **Mesurer une baseline avant de conclure.** Mon premier banc donnait 1 535 ms sur la vue et
  j'allais la déclarer inutilisable ; `SELECT 1` coûtait déjà **667 ms** d'ouverture de connexion.
  À chaud, la même requête tombe à **18 ms**. Toujours mesurer un témoin.
- **`AdmFormsLog` (311 923 lignes) comme boussole de priorité.** Journal d'ouverture d'écran du
  desktop, avec utilisateur et horodatage. Il a réordonné tout le refactor : la **fiche**
  prospection est ouverte 7 372 fois sur 12 mois contre **270** pour sa liste (facteur 27), et les
  interactions sont le 2ᵉ écran du logiciel — que je n'avais mis dans aucun lot.
- **Mapper la vue en entité `@Immutable` en ne déclarant QUE les colonnes utiles.** Hibernate
  n'émet alors que celles-là, ce qui déclenche l'élagage des sous-requêtes corrélées par SQL
  Server. C'est ce qui rend `ComSaleProspectionView` utilisable (26 ms sur une ligne).
- **Enrichir la page déjà chargée par `WHERE PKey IN (20 ids)`** plutôt que servir la liste depuis
  la vue : **2–7 ms** contre 515 ms. La liste garde ses `Specification`, son périmètre commercial
  et sa pagination profonde sur table indexée.
- **Compter avant de conclure.** 14 118 + 3 817 + 4 = 17 939 exactement → la vue ne perd aucune
  ligne, elle sépare prospection et pré-prospection. Un total qui tombe juste prouve qu'aucune
  ligne ne se cache ailleurs.
- **Lire la définition de la vue (`sys.sql_modules`) plutôt que sonder** : c'est elle qui a donné
  le vrai discriminant (`StrEntityId`), la règle de tranche des interactions, et la preuve que
  `ComProspectionOriginType` est un alias.
- **Distinguer `NULL` de la chaîne vide dans toute mesure de population.** Sans ça, cinq colonnes
  de la vue prospection passent pour remplies à 100 % alors qu'elles sont vides.

## ❌ What Failed

- **Réécrire un fichier Java via PowerShell** (`Set-Content`, `[IO.File]::WriteAllText`) → accents
  passés en latin-1 + BOM ajouté, compilation cassée (`illegal character: '\ufeff'`). Arrivé
  **deux fois**. Restauré via `git checkout --` puis refait avec l'outil Edit. **Ne jamais
  réécrire un fichier source par script.**
- **`MappingJacksonValue` pour le filtrage Jackson par requête** → ne compile pas. Spring Boot 4.1
  sérialise avec **Jackson 3** (`tools.jackson`), or le seul `MappingJacksonValue` de Spring 7 est
  typé sur le `FilterProvider` de Jackson 2.
- **`ComActionMessageView`** → écrite, puis **supprimée**. Aucune sélectivité : `SELECT *` sur
  **une seule ligne** par sa clé primaire prend **30 070 ms** (un `COUNT` corrélé calcule
  `ActionRank`). Tout ce qu'on en voulait vient de deux jointures sur les entités.
- **Le SQL dynamique pour `?colonnes=`** → abandonné après mesure : demander 4 colonnes ou les 20
  mappées coûte pareil (7 ms), la vue étant interrogée par clé. Assembler du SQL depuis une entrée
  client pour un gain nul ne se justifie pas.
- **`jsonPath(...).exists()` sur une valeur `null`** → échoue. A fait rougir deux tests dont le
  code était correct. Poser une valeur dans la fixture.
- **Chiffre publié faux : `cin` à 99 %** → vaut **2,7 %**. J'avais compté les non-`NULL` d'une
  colonne remplie de chaînes vides — le piège que le même lot documentait par ailleurs.
- **Vérification exhaustive de la règle de tranche sur `ComActionMessageView`** → dépasse 2 min,
  abandonnée. La preuve retenue est la définition de la vue, pas le sondage.

---

## 🐛 Bugs & Errors Log

| # | Error / Bug | Status | Fix applied |
|---|-------------|--------|-------------|
| 43 | **`ComProspection.StrEntityId` jamais posé par `creerProspection`** → toute prospection créée par notre API est **invisible dans le logiciel de bureau** (les vues font un `INNER JOIN StrEntity`) | **Open — décision métier** | Champ mappé, comportement figé par un test. Corriger = poser `E7C73F70-1111-40D9-AA78-A2825C42B601`, mais les fiches apparaîtraient chez les commerciaux |
| 44 | **830 pré-prospections classées « prospection »** — notre heuristique (`CfgProspectId IS NULL AND CfgCommercialId IS NULL`) diverge du legacy (`StrEntity.Code = 'ComPreProspection'`) | **Open — décision métier** | Écart unidirectionnel, mesuré. Corriger change le contenu de deux listes |
| 45 | `prix`/`surface` exposés alors que morts (`Price` = 4 lignes/14 114, vaut **0** jamais `NULL`) | **Partiellement fixed** | Retirés de la sélection **par défaut** de la liste ; conservés dans la fiche (retrait = rupture front) avec `@Schema` « ne pas afficher » |
| 46 | `cin` documenté à 99 %, vaut 2,7 % | **Fixed** | Corrigé dans `@Schema`, FEATURE et guide front |
| 47 | `FEATURES.md` porte **deux entrées** pour `feat/vues-legacy-prospection` | **Open (mineur)** | L'index a tourné après chaque commit ; la 2ᵉ ne liste que `bb5660c` |
| 41 | `creerProspection` ne pose ni `UserCreate` ni `UserCreateDateTime` | **Open** | **Confirmé par la mesure : `UserCreate` peuplé à 34,3 % seulement** |
| 42 | 277 tiers `CLI` liés à une prospection, invisibles dans `GET /api/prospects` | **Open (question produit)** | Non tranché |
| 39 | `sync-vps-yesRemsDev.sql` ne transporte aucune table `Adm*` | **Open (par conception)** | Correctifs de référentiel rejoués à la main |
| 32 | VS Code retient les ports 9090/9091 | **Open** | `MANAGEMENT_SERVER_PORT=9092` |
| 11 | Règlement `CANCELED` non filtré dans `sumAmountByComDocumentId` | **Open** | Latent |
| 12 | `POST /echeancier` sans `exclureDuPrix` | **Open** | Non corrigé |
| 13 | `S3_ENDPOINT` de la run config pointe la **production** | **Open** | Aucune écriture faite |

---

## 🔑 Key Points to Remember

### Les mesures qui commandent l'architecture

Sur `yesRemsProd`, connexion chaude, 14 118 lignes dans `ComSaleProspectionView` :

| Forme | Temps |
|---|---:|
| Détail, 1 ligne (20 colonnes mappées) | **26 ms** |
| Enrichir une page : `IN (20 ids)` | **2–7 ms** |
| Enrichir 100 ids | 34 ms |
| Liste servie **depuis** la vue, page 1 | 515 ms |
| Liste depuis la vue, `OFFSET 5000`, 8 col | 123 ms · **toutes colonnes : 5 942 ms** |
| Filtre par sous-requête sur la vue | **231 ms** page 1, **302 ms** `OFFSET 5000` |
| Référence : liste sur table seule | 100 / 134 ms |

**La règle qui en découle** : liste = `ComProspection` (table indexée), enrichissement = vue par
lot d'identifiants, filtre « vue » = sous-requête. **Jamais** `findAll()` nu sur une entité de vue.

### Jackson 3 — le piège de Spring Boot 4.1

`tools.jackson.core:jackson-databind:3.1.4` est le sérialiseur ; Jackson 2 n'est là que via `jjwt`.

- `MappingJacksonValue` (Spring 7) est **typé Jackson 2** → inutilisable.
- Montage retenu : `@JsonFilter` sur le DTO + `SelectionColonnesPropertyFilter` (Jackson 3) +
  `ColonnesSelection` (`ThreadLocal`) + `ColonnesSelectionFilter` (servlet, nettoyage en
  `finally`) + `ColonnesJacksonConfig` (`JsonMapperBuilderCustomizer`).
- **Le filtre doit être enregistré globalement** : Jackson lève dès qu'il croise un `@JsonFilter`
  inconnu, et `ProspectionListItemDto` est embarqué par `HotlineFicheDto` et servi par
  `GET /api/pre-prospections` → 500 sur ces deux endpoints sinon.
- **La sélection par défaut doit être réellement appliquée**, sinon les 14 champs ajoutés
  apparaissent à `null` partout et « identique à l'existant » devient faux.

### Ce que la donnée réelle dit (et qu'il ne faut pas redécouvrir)

- **Bloc qualification mort** : `Price` = 4 lignes sur 14 114 et vaut **0** (jamais `NULL`, donc
  non masquable côté front) ; `Area`, `AreaTo`, `PriceTo`, `AdvanceAmount`, les 3 financements =
  **0 ligne**.
- **Colonnes vides malgré un nom prometteur** : `Categories`, `Types`, `Cities` (0 %), `ComBudget`,
  `ComUtilization`, `ComPaymentMethod`, `CfgProposer`, `CfgIntermediate`, `MapLatitude/Longitude`
  (0 %), `Delai` (0,8 %), `TempField3..10`.
- **`ComProspectionOriginType` EST `ComActionMessageType`** — alias dans la vue, identique sur les
  14 118 lignes. Nous le servons déjà sous `typeContact`. Ne pas l'ajouter.
- **Le mieux peuplé et le plus utile** : `InactivityTimeout` **100 %** (le tri de relance du
  desktop), `ActionsMessageNumber` 97 %, `LastActionDate` 97 %, `LastCommunication*` 90 %,
  `CfgProject`/`CfgCompany` 99,8 %.
- **`StatusImg` est de type `image`** (binaire) : ne jamais sérialiser.
- Conversion prospection → dossier : **2,5 %** (358 dossiers / 14 118). Taux réel, pas un manque.

### Pièges de test propres à ce chantier

- **`saveAndFlush` obligatoire** avant d'interroger une vue : Hibernate ne connaît pas les tables
  qu'elle lit, son auto-flush ne se déclenche pas.
- **`StrEntityId` obligatoire sur la fixture**, sinon la ligne n'est dans aucune vue et le test
  passe pour la mauvaise raison.
- La liste **exclut** les prospections sans prospect ni commercial → rattacher un `CfgTier`.
- Cibler sa propre ligne avec `?search=<code>` : la base de test est partagée entre classes.

### La topologie des bases (inchangée)

| Base | Rôle |
|---|---|
| `192.168.11.115` / `YesRems` | source de vérité, app de bureau — lecture seule |
| `yesRemsProd` (local, `49687`) | copie de travail, **c'est elle qu'interroge le backend local** |
| `yesRemsCiCheck` (local) | **base des tests** — 165 tables, et désormais 1 vue |
| `yesRemsDev` (VPS) | environnement déployé |

---

## 📁 Important Files & Paths

- **`instructions/INVENTAIRE-colonnes-prospection.md`** — les 148 colonnes avec leur population
  mesurée, les manques, les filtres possibles. **Le document de référence pour la suite.**
- `instructions/FEATURE-vues-legacy-prospection.md` — lot 1 : §4 pourquoi `ComActionMessageView`
  est écartée, §6 le schéma de test, §8 le défaut `StrEntityId`.
- `instructions/FEATURE-selection-colonnes-listes.md` — lot 2 : les mesures, le contrat
  `?colonnes=`, §4 pourquoi l'implémentation Jackson est inhabituelle.
- **`instructions/GUIDE-FRONT-fiche-prospection.md`** — destiné à `crm-prototype` : ordre
  d'affichage, pièges (`activite` nul sur pré-prospection, `null` ≠ zéro), contrat du sélecteur,
  colonnes à ne pas demander.
- `domain/ColonneProspection.java` — catalogue **et** liste blanche. Une seule source, donc le
  sélecteur front ne peut pas diverger du serveur.
- `entities/com/ComSaleProspectionView.java` — 20 des 148 colonnes. **Ne pas en mapper d'autres
  sans remesurer** : c'est le nombre de colonnes mappées qui décide de l'élagage SQL.
- `web/Colonnes*.java` + `SelectionColonnesPropertyFilter.java` — le montage Jackson 3.
- `services/prospection/ContexteInteraction.java` — la règle de tranche des interactions, copiée
  de la définition de la vue.
- `dto/prospection/FiltresAvancesProspection.java` — les 15 filtres du lot C, liés par
  `@ModelAttribute`.
- **`ci/schema/050_views.sql`** — la vue, `AdmAlertData`, `AdmParamsInteger`, et les semis
  (`DefaultAlertDelai`, les deux `StrEntity`). À appliquer **après** `040` ;
  `.github/workflows/ci.yml` étendu en conséquence.

---

## 🔧 Environment & Config

- **Tests — la commande qui marche** :
  ```powershell
  $env:SPRING_DATASOURCE_URL="jdbc:sqlserver://localhost:49687;databaseName=yesRemsCiCheck;encrypt=true;trustServerCertificate=true"
  $env:SPRING_DATASOURCE_USERNAME="tsix"; $env:SPRING_DATASOURCE_PASSWORD="securePassword123"
  .\mvnw.cmd test "-Dspring.jpa.show-sql=false"
  ```
- **Interroger `yesRemsProd`** : `Invoke-Sqlcmd -ServerInstance "localhost,49687" -Database
  yesRemsProd -Username tsix -Password securePassword123 -TrustServerCertificate`. Pour un banc de
  mesure, **ouvrir une seule `SqlConnection` .NET** et boucler — sinon on mesure l'ouverture de
  connexion (667 ms).
- **⚠️ Ne jamais réécrire un fichier source via PowerShell** (encodage cassé deux fois).
- **⚠️ Dette de secrets** : jeton UltraMsg, PAT GHCR, clé MinIO, mot de passe `sasa` du `.115`,
  compte `dev` du VPS — tous passés en clair en conversation. À révoquer.
- **⚠️ Ne pas lancer `mvnw clean`** pendant qu'une instance tourne depuis IntelliJ (bug #24).

### État des branches

| Repo | Branche | État |
|---|---|---|
| back | **`feat/selection-colonnes-listes`** | 6 commits, poussée, **PR en attente de merge** |
| back | `docs/memoire-de-session` | 2 commits, existe sur `origin` |
| back | `feat/remise-dossier` | 1 commit, **aucun remote** |
| front | `refractor/best-practice` | **11 commits, LOCAL SEUL** — remote supprimé sans merge |

Nettoyage fait cette session : `feat/vues-legacy-prospection` et `feat/offre-apres-visite`
supprimées en local et sur `origin`. `git branch -d` refuse toute branche non mergée — s'en servir
comme garde-fou plutôt que `-D`. Le `stash` de `SESSION_MEMORY.md` qui traînait depuis la session
précédente a été récupéré et consommé ; il ne reste que `stash@{0}` (wip 172 fichiers, ancien).

---

## ⏭️ Next Step

> **Merger la PR `feat/selection-colonnes-listes` sur GitHub**, puis
> `node ..\.claude\scripts\git-flow.mjs sync back`.
>
> Attention : `sync` **ne supprime pas** la branche mergée — le faire à la main
> (`git branch -d` + `git push origin --delete`), comme cette session l'a fait pour les deux
> précédentes.

### Backlog

- [ ] **Fiche tiers** — 5 447 ouvertures/an (3ᵉ écran), `ProspectDetailDto` 52 champs contre 97
      dans `ComSaleProspectView`. Le meilleur rapport valeur/effort restant.
- [ ] **Pré-prospection** — 5 420 ouvertures/an (4ᵉ écran), `ComSalePreProspectionView`
      (70 colonnes) jamais exploitée.
- [ ] **Décision métier : les 830** (bug #44) — aligner sur `StrEntity.Code` ?
- [ ] **Décision métier : `StrEntityId` à la création** (bug #43) — rendre nos prospections
      visibles dans le desktop ?
- [ ] Déduplication de l'entrée `FEATURES.md` (bug #47).
- [ ] **Trancher `refractor/best-practice`** (front, 11 commits orphelins, local seul).
- [ ] Relayer au dépôt frontend : `GUIDE-FRONT-fiche-prospection.md`, et le fait que les champs de
      `ProspectionListItemDto` deviennent **optionnels** dans `types.ts`.
- [ ] `from_front.md` #10 — filtre `?statut=` des dossiers qui renvoie du vide (contourné côté
      front par un chargement de 500 dossiers).
- [ ] Dossier : `ComSaleFolderView` (217 colonnes) — la ventilation financière par poste
      (Montant/Réglé/Reste × 12) et les 15 dates de chronologie réellement peuplées sur 40.
- [ ] `UserCreate` sur `creerProspection` (bug #41) — préalable à l'exposition de l'auteur.
- [ ] Les 277 tiers `CLI` (bug #42).
- [ ] Règlement annulé (#11), `exclureDuPrix` (#12), garde-fou `S3_ENDPOINT` (#13).
- [ ] Révoquer tous les secrets listés dans Environment.
- [ ] Déclarer le check `Tests` comme requis sur `main`.

# Session Memory
_Last updated: 2026-09-10T18:00+01:00_
_Project: immo-crm (AyadERP) — C:\Users\tsiory\Documents\ayad-internship\prototype\immo-crm_

---

## 🎯 Current Objective

**Exploiter les 216 vues du légataire** pour combler l'écart entre ce que nos `GET` renvoient et
ce que le logiciel de bureau affiche.

| # | Lot | État |
|---|---|---|
| 1 | Fiche prospection + interactions (vues `@Immutable`) | **mergé** (PR #31) |
| 2 | Sélection de colonnes `?colonnes=` sur la liste | **mergé** |
| 3 | Inventaire des 148 colonnes | **mergé** |
| A/B/C | Champs morts retirés · 18 colonnes ajoutées à la liste · 15 filtres | **mergé** |
| **D** | **Enrichissement de la fiche** (`GET /api/prospections/{id}`) | **branche en cours** |

Branche : **`feat/enrichissement-fiche-prospection`**, coupée d'un `main` fraîchement pull.
**718 tests verts** (673 en fin de session précédente, +45 dont 8 écrits ici).

---

## ✅ What Worked

- **Lire la définition de la vue avant de décider quoi mapper.** `sys.sql_modules` sur
  `ComSaleProspectionView` (508 lignes) montre trois couches : `ComProspection` (56 colonnes),
  **~30 `LEFT JOIN` de référentiel**, et **~15 sous-requêtes corrélées** sur `ComActionMessage`.
  Seule la troisième est irremplaçable — et le lot 1 l'avait déjà prise. **Tout ce qui restait à
  gagner sur la fiche était de la jointure**, donc obtenable sans toucher à l'entité de vue.
  C'est cette lecture qui a décidé de l'architecture, pas une intuition.
- **Comparer avant d'ajouter** : quatre « manques » de l'inventaire se sont révélés des alias
  (voir §Key Points). Un `SUM(CASE WHEN a <> b)` sur 14 118 lignes coûte une requête et supprime
  quatre champs du périmètre.
- **Confronter la fiche à la liste, pas seulement à la vue.** Le vrai constat n'était pas
  « la vue a 148 colonnes » mais « le lot B a enrichi la liste sans enrichir la fiche ». D'où la
  règle appliquée : **réutiliser les noms de champs de la liste** (`mobileProspect`,
  `indicatifMobile`, `emailFiche`, `societeLibelle`, `creeLe`, `modifieLe`, `typeProspect`)
  plutôt que d'inventer une seconde convention pour la même donnée.
- **`Champs.de(...)` pour les chaînes d'associations.** En passant de 2 à 3 déréférencements par
  association (id + libellé + code), les ternaires auraient triplé les passages par le proxy LAZY.
  Une variable locale par association + `Champs.de` = un seul passage.
- **Mesurer la population avec la règle `NULL` **et** chaîne vide**, systématiquement.

## ❌ What Failed

- **`StkItem.stkVocation` est `@NotNull`** — fixture d'article rejetée à la persistance
  (`ConstraintViolation ... propertyPath=stkVocation`). Le message Hibernate ne nomme pas le champ
  dans la sortie Maven : il faut aller le lire dans `target/surefire-reports/*.txt`.
- **Le `cd` du terminal persiste entre appels d'outils** — un `cd .../entities` fait échouer
  `.\mvnw.cmd` trois appels plus tard avec « n'est pas reconnu ». Repartir d'un `Set-Location`
  absolu avant Maven.
- **Deux affirmations fausses corrigées en cours de route** (les deux venaient de lire la vue
  sans lire notre propre code) :
  - « la société n'est jamais renvoyée » → **elle l'est, par la liste**, depuis le lot B ;
  - « `GET /api/pre-prospections/{id}` sert le même DTO » → **cette route n'existe pas**,
    `/api/prospections/{id}` sert les deux.
- **Agréger 37 colonnes de la vue coûte 109 s** sur 14 118 lignes. Acceptable pour une mesure
  ponctuelle, mais confirme qu'on ne sert jamais une liste depuis la vue.

---

## 🐛 Bugs & Errors Log

| # | Error / Bug | Status | Fix applied |
|---|-------------|--------|-------------|
| 48 | `proposeurId`/`proposeurNom` exposés alors que **morts : 1 ligne sur 14 118** | **Documenté** | `@Schema` « ne pas afficher » + §5 de la FEATURE. Retrait = rupture front, non fait |
| 46 | `cin` documenté à 99 %, vaut 2,7 % | **Fixed (reliquat traité)** | La correction de la session précédente avait manqué le javadoc de `ComSaleProspectionView` — corrigé ici |
| 43 | **`ComProspection.StrEntityId` jamais posé par `creerProspection`** → prospection **invisible dans le logiciel de bureau** | **Open — décision métier** | Comportement figé par un test |
| 44 | **830 pré-prospections classées « prospection »** — heuristique divergente de `StrEntity.Code` | **Open — décision métier** | Écart mesuré, unidirectionnel |
| 41 | `creerProspection` ne pose ni `UserCreate` ni `UserCreateDateTime` | **Open** | Re-confirmé : `UserCreate` à 34,3 %. `creePar` est exposé mais son `@Schema` avertit |
| 45 | `prix`/`surface` morts | **Partiellement fixed** | Hors sélection par défaut de la liste ; conservés en fiche |
| 47 | `FEATURES.md` porte deux entrées pour `feat/vues-legacy-prospection` | **Open (mineur)** | |
| 42 | 277 tiers `CLI` liés à une prospection, invisibles dans `GET /api/prospects` | **Open (question produit)** | |
| 39 | `sync-vps-yesRemsDev.sql` ne transporte aucune table `Adm*` | **Open (par conception)** | |
| 32 | VS Code retient les ports 9090/9091 | **Open** | `MANAGEMENT_SERVER_PORT=9092` |
| 11 | Règlement `CANCELED` non filtré dans `sumAmountByComDocumentId` | **Open** | Latent |
| 12 | `POST /echeancier` sans `exclureDuPrix` | **Open** | |
| 13 | `S3_ENDPOINT` de la run config pointe la **production** | **Open** | Aucune écriture faite |

---

## 🔑 Key Points to Remember

### La structure de `ComSaleProspectionView` (à ne pas redécouvrir)

```
ComProspection (56 col.)
  + ~30 LEFT JOIN de référentiel      → reproductibles en JPA, pas besoin de la vue
  + ~15 sous-requêtes corrélées        → l'apport unique, déjà mappé au lot 1
    sur ComActionMessage                 (inactivité, nb interactions, communications, visites, alerte)
  WHERE StrEntityId <> 'B92DF3D8-…'    → exclut les pré-prospections
```

- `CfgCompanyId` vient de **`CfgTranche → CfgProject → CfgCompany`** : la société est dérivée de
  la tranche, jamais saisie sur la prospection.
- `CfgCustomer*` et `StatusIs*` sont **conditionnés à la vocation** (`IIF(StkVocationId IN (…),
  ComFolder…, RntFolder…)`) — vente contre location.
- **Règle tenue :** ne jamais mapper une colonne de plus sur `ComSaleProspectionView`. C'est le
  nombre de colonnes mappées qui décide de l'élagage des sous-requêtes par SQL Server, donc des
  26 ms de la fiche.

### Quatre alias, mesurés à 0 écart sur 14 118 lignes — ne pas les exposer

| Colonne | Vaut exactement |
|---|---|
| `Commercial` | `CfgCommercialDescription` |
| `ComProspectionOriginType*` | `ComActionMessageType*` (déjà servi sous `typeContact`) |
| `LastActiveAlertOld` | `LastActiveAlertDate` — **tranche la question laissée ouverte par `INVENTAIRE` §2** |
| `CfgTierFirstName` + `' '` + `CfgTierLastName` | `CfgTierDescription` (7 lignes d'écart) |

Et `CfgTierMobile` est un **format d'affichage** — `(212) 6 61 83 60 90` — dont `CfgTierMobile1`
et `CfgTierMobile1Indicatif` sont les composants. **Un numéro, trois représentations**, pas trois
numéros. L'indicatif est stocké **sans `+`**.

### Population mesurée, colonnes ajoutées à la fiche

Société 99,8 % · code prospect 100 % · type de tiers 100 % · mobile 99,8 % · indicatif 100 % ·
code commercial 99,3 % · `UserCreateDateTime`/`UserUpdate*` 100 % · codes de référentiel 100 % ·
email de fiche 59,1 % · motif de clôture 60,5 % · support d'origine 50,2 % · action commerciale
40,2 % · `UserCreate` 34,3 % · pays 27,3 % · catégorie/type d'article 5,7 % (= **100 % des 804
lignes portant un article**).

Écartés : `CfgTierCity` 5,4 % (exigerait de mapper `CfgTier.AdmCityId`) · `StatusIsCancellation`
0,2 % / `StatusIsClosing` 0,1 % · `CfgCustomer*`/`ComFolderDescription` 2,5 % (obligeraient à
mapper la vue ou une requête de plus) · `MapRadius` 16,7 % (rayon sans centre) · `TempField1/2`
9,6 / 7,6 % (contenu non qualifié) · `StatusImg` (type `image`, ne jamais sérialiser).

`ComProspection.AlertValidityDate` et `AlertMessage` existent sur la table, sont absentes de la
vue, et sont à **0 %**.

### Piège de nommage livré (hérité de la liste)

`typeProspect` = type du **tiers** (`PRO`, `PRORNT`). `typeProspectionCode` = type du **document**
(`ComProspection`, `ComPreProspection`). Un caractère d'écart, deux notions. Les deux coexistaient
déjà sur la liste ; la fiche conserve la convention plutôt que d'en inventer une autre.

### Pièges de test propres à ce chantier

- **`saveAndFlush` obligatoire** avant d'interroger une vue.
- **`StrEntityId` obligatoire sur la fixture**, sinon la ligne n'est dans aucune vue.
- **Pas de `jsonPath(...).exists()` sur une valeur potentiellement nulle** — poser une valeur et
  comparer avec `.value(...)`.
- `StkItem` exige `stkVocation`, `stkKind`, `stkItemType`, `stkItemCategory`.
- La liste **exclut** les prospections sans prospect ni commercial.

### La topologie des bases

| Base | Rôle |
|---|---|
| `192.168.11.115` / `YesRems` | source de vérité, app de bureau — lecture seule |
| `yesRemsProd` (local, `49687`) | copie de travail, **c'est elle qu'on interroge pour mesurer** |
| `yesRemsCiCheck` (local) | **base des tests** — porte `ComSaleProspectionView` et `StrEntity` |
| `yesRemsTest` (local) | ancienne base de test — **n'a ni la vue ni `StrEntity`**, la suite y échoue |
| `yesRemsDev` (VPS) | environnement déployé |

⚠️ `src/test/resources/application.properties` pointe encore `yesRemsTest` : **il faut surcharger
par variables d'environnement** (voir Environment), sinon les tests de vue échouent.

---

## 📁 Important Files & Paths

- **`instructions/FEATURE-enrichissement-fiche-prospection.md`** — le lot en cours : §1 les trois
  couches de la vue et pourquoi on n'y touche pas, §2 les mesures + le tableau
  « déjà sur la liste ? », §3 les quatre alias, §4 les écartés et pourquoi, §5 les deux défauts.
- **`instructions/INVENTAIRE-colonnes-prospection.md`** — les 148 colonnes et leur population.
- **`instructions/GUIDE-FRONT-fiche-prospection.md`** — handoff `crm-prototype` : nouveau
  « Rang 5 », la note « le repli ne concerne que `activite` », les quatre alias à ne pas demander.
- `instructions/FEATURE-vues-legacy-prospection.md` — lot 1.
- `entities/com/ComSaleProspectionView.java` — 20 des 148 colonnes. **Ne pas en mapper d'autres.**
- `services/prospection/ProspectionService.java` — `toDetail` (mapping enrichi, variables locales
  par association) et `toListItem`.
- `dto/prospection/ProspectionDetailDto.java` — chaque champ porte sa population dans son `@Schema`.
- `src/test/.../ProspectionFicheEnrichieTest.java` — 8 tests, dont
  `preProspection_porteLesChampsDEntite_bienQueLeBlocActiviteSoitNul` qui **garde la décision de
  conception** : il rougira si on rebranche ces champs sur la vue.
- `utils/Champs.java` — lecture à travers 1 ou 2 associations optionnelles.

---

## 🔧 Environment & Config

- **Tests — la commande qui marche** :
  ```powershell
  Set-Location C:\Users\tsiory\Documents\ayad-internship\prototype\immo-crm
  $env:SPRING_DATASOURCE_URL="jdbc:sqlserver://localhost:49687;databaseName=yesRemsCiCheck;encrypt=true;trustServerCertificate=true"
  $env:SPRING_DATASOURCE_USERNAME="tsix"; $env:SPRING_DATASOURCE_PASSWORD="securePassword123"
  .\mvnw.cmd test "-Dspring.jpa.show-sql=false"
  ```
- **Interroger `yesRemsProd`** : `Invoke-Sqlcmd -ServerInstance "localhost,49687" -Database
  yesRemsProd -Username tsix -Password securePassword123 -TrustServerCertificate`. Pour un banc de
  mesure, ouvrir **une seule** `SqlConnection` .NET et boucler (l'ouverture coûte 667 ms).
- **⚠️ Le répertoire courant persiste entre appels d'outils** — `Set-Location` absolu avant Maven.
- **⚠️ Ne jamais réécrire un fichier source via PowerShell** (encodage cassé deux fois par le
  passé : accents en latin-1 + BOM → `illegal character: '\ufeff'`). Utiliser l'outil d'édition.
- **⚠️ Ne pas lancer `mvnw clean`** pendant qu'une instance tourne depuis IntelliJ (bug #24).
- **⚠️ Dette de secrets** : plusieurs identifiants de service et d'infrastructure ont été échangés
  en clair au fil des sessions (messagerie sortante, registre d'images, stockage objet, comptes
  serveurs). **À révoquer et à faire tourner** — ne pas les recopier dans ce fichier.

### État des branches

| Repo | Branche | État |
|---|---|---|
| back | **`feat/enrichissement-fiche-prospection`** | lot D, 718 tests verts |
| back | `docs/memoire-de-session` | 2 commits, existe sur `origin` |
| back | `feat/remise-dossier` | 1 commit, **aucun remote** |
| front | `refractor/best-practice` | **11 commits, LOCAL SEUL** — remote supprimé sans merge |

Les branches des lots 1 à C ont été mergées sur GitHub (PR jusqu'à #38). `sync` **ne supprime pas**
la branche mergée — le faire à la main (`git branch -d` + `git push origin --delete`) ; `-d` refuse
toute branche non mergée, s'en servir comme garde-fou plutôt que `-D`.

---

## ⏭️ Next Step

> **Faire merger la PR `feat/enrichissement-fiche-prospection` sur GitHub**, puis
> `node ..\.claude\scripts\git-flow.mjs sync back` et supprimer la branche à la main.

### Backlog

- [ ] **Relayer au dépôt frontend** `GUIDE-FRONT-fiche-prospection.md` (« Rang 5 ») : 25 champs
      **additifs** sur `ProspectionDetailDto`, tous optionnels dans `types.ts`, **aux mêmes noms
      que la liste** — le type de la fiche peut réutiliser les champs déjà déclarés.
- [ ] **Fiche tiers** — 5 447 ouvertures/an (3ᵉ écran), `ProspectDetailDto` 52 champs contre 97
      dans `ComSaleProspectView`. Le meilleur rapport valeur/effort restant. Appliquer la même
      méthode : lire la définition de la vue, séparer jointures et sous-requêtes corrélées.
- [ ] **Pré-prospection** — 5 420 ouvertures/an (4ᵉ écran), `ComSalePreProspectionView`
      (70 colonnes) jamais exploitée.
- [ ] **Décision métier : les 830** (bug #44) — aligner sur `StrEntity.Code` ?
- [ ] **Décision métier : `StrEntityId` à la création** (bug #43).
- [ ] `UserCreate` sur `creerProspection` (bug #41) — préalable à un affichage honnête de `creePar`.
- [ ] Dossier : `ComSaleFolderView` (217 colonnes) — ventilation financière et chronologie.
- [ ] Déduplication de l'entrée `FEATURES.md` (bug #47).
- [ ] **Trancher `refractor/best-practice`** (front, 11 commits orphelins, local seul).
- [ ] `from_front.md` #10 — filtre `?statut=` des dossiers qui renvoie du vide.
- [ ] Les 277 tiers `CLI` (bug #42) · règlement annulé (#11) · `exclureDuPrix` (#12) ·
      garde-fou `S3_ENDPOINT` (#13).
- [ ] Révoquer les secrets échangés en clair.
- [ ] Déclarer le check `Tests` comme requis sur `main`.

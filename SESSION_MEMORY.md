# Session Memory
_Last updated: 2026-09-11T09:30+01:00_
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
| D | Enrichissement de la fiche (`GET /api/prospections/{id}`) | **mergé** (PR #39) |
| E | Marqueurs de tri + pointeur de doublon | **mergé** (PR #41) |
| **F** | **Fiche dossier alignée sur l'écran légataire** | **branche en cours** |

Branche : **`feat/fiche-dossier-alignement-ecran`**, coupée d'un `main` fraîchement pull.
**763 tests verts** (673 en début de session, +90 dont 53 écrits ici).

**Le chantier a changé de méthode au lot F, et c'est l'acquis principal.** Jusque-là on partait de
la vue ; désormais on part de **captures d'écran du logiciel de bureau**, confrontées case par case
à la base. C'est ce qui a révélé `PriceWithModification` (un booléen, pas un prix) et les ratios
stockés en fraction — deux erreurs qu'aucune lecture de schéma n'aurait évitées.

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
- **Mesurer la taille d'une vue avant de projeter du travail dessus.** La longueur de sa
  définition et son nombre de sous-requêtes disent en une requête s'il y a quelque chose à en
  tirer. `ComSalePreProspectionView` : 4 229 caractères, **0 sous-requête** — verdict immédiat,
  là où j'allais lancer un lot complet.
- **Tester une hypothèse de sémantique au lieu de la supposer.** « `TempField10` pointe un
  tiers » se vérifie par un `EXISTS` : **849/849**. Sans ça, on expose une chaîne opaque.
- **Demander une capture d'écran plutôt que deviner l'usage.** Deux captures de l'écran « Dossier
  de vente » ont réorienté tout le lot F : l'onglet **Montants** est celui qui s'ouvre par défaut,
  pas les Dates que je m'apprêtais à livrer en premier. Et la confrontation case↔colonne a attrapé
  deux pièges invisibles au schéma (voir §Key Points).
- **Vérifier une formule sur toute la population avant de la coder.** `Total = Prix + Frais`
  semblait évident sur le dossier de la capture ; il ne tenait qu'à **91,6 %**. Les 30 écarts ont
  révélé une vraie règle métier (dossiers sortis) qu'on aurait sinon livrée fausse.
- **Renoncer quand la vérification ne passe pas.** `ratioAvance` tenait 328/328 sur sa formule
  finale mais 350/358 sur un maillon intermédiaire : non livré, et documenté comme tel.
- **Vérifier l'unicité avant d'écrire un `findByCode`.** `CfgTier.Code` n'est pas unique — un
  dérivé `Optional` aurait levé `IncorrectResultSizeDataAccessException` en production, sur un
  cas absent des données du jour.

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
- **Un champ servi « brut » sortait avec ses espaces** (`"  TRAITÉ  "`). Le test a eu raison ;
  j'ai normalisé côté serveur et corrigé le mot « brut » dans le spec, devenu inexact.
- **Mesurer un coût sur une seule ligne, deux fois de suite** → 14 ms, chiffre faux : c'était le
  plan mis en cache. Sur **12 lignes distinctes**, la même requête coûte **360 ms**. Toujours
  échantillonner des lignes différentes.
- **Croire que l'élagage par sélection de colonnes est une propriété générale** → faux.
  Il marchait sur `ComSaleProspectionView`, il **ne marche pas** sur `ComSaleFolderView`.
- **`-Dtest=A+B` n'est pas la syntaxe surefire** — c'est `-Dtest=A,B`. Le `+` échoue sans
  message utile (`BUILD FAILURE` nu).
- **`SUM(CASE WHEN (SELECT …))` est refusé par SQL Server** (« fonction d'agrégation sur une
  expression comportant une sous-requête ») : passer par une table dérivée.

---

## 🐛 Bugs & Errors Log

| # | Error / Bug | Status | Fix applied |
|---|-------------|--------|-------------|
| 52 | **Le legacy double-compte une contre-passation** — `ART 2026/00365` porte une paire −470 000/+470 000 ; la vue rend 1 270 000 au lieu de 330 000 | **Open (défaut legacy)** | **Notre calcul est le juste.** Ne pas aligner l'API sur la vue. Seul dossier concerné sur 358 |
| 51 | `POST /echeancier` ne posait pas `UserCreateDateTime` → `creeLe` nul sur nos créations | **Fixed** | Posé dans `addEcheance`. `UserCreate` (auteur) reste non posé — même famille que #41 |
| 50 | **798 pré-prospections (20,9 %) marquées doublon/spam sont servies comme du travail à faire** — 795 encore à « 1 A TRAITER » | **Rendu visible** | `marqueurTri` exposé (lot E). **Filtrer reste une décision produit ouverte** |
| 49 | `@Schema` d'audit annonçaient « 100 % » sans qualifier la population, alors que le DTO sert prospections **et** pré-prospections (28 % pour `modifieLe`) | **Fixed** | Taux qualifiés par population dans les `@Schema` et le guide front |
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

### Le dossier : la vue est riche mais **inutilisable**, pour une raison différente

| Vue | Colonnes | Sous-requêtes | Coût / ligne |
|---|---:|---:|---:|
| **`ComSaleFolderView`** | **217** | **77** | **~360 ms, sans élagage** |
| `ComSaleProspectionView` | 148 | 22 | 26 ms (20 col. mappées) |
| `ComSaleProspectView` (fiche tiers) | 97 | **3** | non mesuré |
| `ComSalePreProspectionView` | 70 | 0 | — |

**L'élagage par sélection de colonnes ne marche pas sur la vue dossier** : 15 colonnes coûtent
autant que 217. Et le prix est absurde — 358 dossiers, 921 échéances, 3 243 règlements.
**89 des 217 colonnes sont directement sur `ComFolder`.** Tout se lit sur les entités.

À noter pour le backlog : `ComSaleProspectView` (fiche tiers) n'a que **3 sous-requêtes** — donc
elle non plus n'apportera presque rien au-delà des jointures.

### Deux pièges du dossier, invisibles au schéma

- **`PriceWithModification` est un BOOLÉEN**, pas le « Prix contractuel » de l'écran. Ce prix-là,
  c'est `Price`. Un mapping fait au nom se trompe silencieusement.
- **Les ratios sont des fractions** : `PartPercent`, `ProbabilityOfFinalisation`, `Advance`,
  `Discount` valent `1.00` pour 100 %. `probabiliteFinalisation` était **déjà exposé** ainsi sans
  que rien ne le dise ; son `@Schema` le précise maintenant.

### Les formules de l'onglet Montants, vérifiées sur 358 dossiers

| Règle | Vérifiée |
|---|---|
| `netAPayer = prix + frais exclus du prix` (actifs) | **328/328** |
| `netAPayer = encaissé`, reste **nul** (dossiers **sortis**) | **30/30** |
| `reste = montant − réglé` | **358/358** |
| `IsCancellation` ⟺ `StatutDossier.isSortie()` (`ANNULATION`/`DESISTEMENT`) | **30/30** |
| `ratioAvance` — maillon `AdvanceAmount` = total catégorie `AVANCE` | **350/358 → non livré** |

Population utile de l'en-tête : `UnitCost`/`PriceHT`/`PriceBeforeDiscountHT` **98,3 %**,
`TitleNumber` 69,6 %, `VATRatio` 42,7 %, présentation 23,2 %, `StatusValidityDate` 11,5 %,
`PartPercent` 100 % mais **95 % valent 1,00**. **`UnderItemsPrice` : 2,2 % → écarté.**
Échéancier : `QuittanceCode` (« Décompte ») **16,5 %, jamais NULL mais vide** ;
`UserCreateDateTime` 100 %. **Pas de facturation vente en base** — la colonne « Facture » de
l'écran n'a aucune source.

Chronologie du dossier : 32 des 34 dates sont sur `ComFolder`, mais **12 sont à 0 %** et 9
seulement dépassent 10 %. `ReservationDate` vaut **0,6 %** malgré son nom.

### `ComSalePreProspectionView` : rien à en tirer, définitivement

| | Vue vente | Vue pré-prospection |
|---|---:|---:|
| Colonnes | 148 | **70** |
| Définition | 21 133 car. | **4 229 car.** |
| Sous-requêtes corrélées | ~15 | **0** |

Vue **plate** : `ComProspection` + 13 `LEFT JOIN` déjà mappés. Aucune autre vue n'existe pour la
pré-prospection (vérifié par nom **et** par définition). Sur ses 70 colonnes, les seules non
servies après le lot D sont mesurées **à 0 %** : proposeur, intermédiaire, mode de paiement,
budget. **Ne pas reprendre ce sujet.**

### Les champs libres, ce qu'ils contiennent vraiment

- **`TempField1` porte deux conventions.** Pré-prospections : marqueur de tri (`Doublon` 770,
  `spam` 30, `Traité` 9). Prospections : **typologie recherchée** — `salon_+_2_chambres` 518,
  `salon_+_1_chambre` 213 — plus villes et budgets. 1 239 lignes de texte libre non reconnu.
  D'où le nommage `noteQualification` (brut) + `marqueurTri` (dérivé, `null` si non reconnu).
- **`TempField10` est un pointeur de doublon** : **849/849** résolvent vers un `CfgTier.Code`.
- **`TempField3`–`TempField9` : 0 %.** `TempField2` : 29 lignes (tranches de budget).
- **Conséquence produit :** 798 pré-prospections (20,9 %) marquées doublon/spam, dont **795
  encore à l'étape « 1 A TRAITER »**. Le lot E expose, **ne filtre pas** — filtrer reste une
  décision produit ouverte.
- **`INVENTAIRE` §1 est à nuancer** : le bloc qualification est mort *en tant que colonnes*
  (`Price`, `Area` = 0), mais l'information existe en texte libre. Non exploitée.

### `CfgTier.Code` n'est pas unique

14 516 tiers pour **14 504 codes distincts** (`000009` porté par 4 tiers). Aucun des 849
pointeurs réels n'est ambigu, mais tout `findByCode` doit renvoyer une **`List`** — un
`Optional` lèverait `IncorrectResultSizeDataAccessException`.

### Les taux d'audit dépendent de la population, et le DTO est partagé

| | Prospections | Pré-prospections |
|---|---:|---:|
| `creePar` | 34,3 % | 27,1 % |
| `creeLe` | 100 % | 100 % |
| `modifiePar` / `modifieLe` | 100 % | **28,1 %** |

Et sur une pré-prospection : `fermee` **0 %** (aucune n'est jamais clôturée), `article*` 0,2 %,
`proposeur*` 0 %. La tranche, elle, est à 95,5 % — la société reste utile.

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

- **`instructions/FEATURE-fiche-dossier-alignement-ecran.md`** — le lot en cours : §1 pourquoi la
  vue dossier est écartée (mesures), §2 les deux pièges, §5 **les formules de l'onglet Montants et
  leur vérification**, plus les deux anomalies de données signalées.
- `src/test/.../MontantsSyntheseTest.java` — les chiffres du dossier réel de la capture
  (`ARS 2025/00357`) servent de scénario : s'ils changent, l'API a cessé de dire ce que l'écran dit.
- `instructions/FEATURE-marqueurs-tri-pre-prospection.md` — lot E : §1 pourquoi la
  vue pré-prospection ne sert à rien, §2 ce que les champs libres contiennent, §3 le contrat et
  pourquoi brut **et** dérivé, §4 le fait opérationnel, §5 les corrections de doc.
- `domain/MarqueurTriProspection.java` — correspondance **exacte** sur valeur normalisée. Le
  risque n'est pas de rater un marqueur mais d'en inventer un : 1 239 lignes de texte libre.
- `src/test/.../MarqueurTriProspectionTest.java` — 23 cas, dont les **négatifs** qui sont
  l'essentiel du test.
- `instructions/FEATURE-enrichissement-fiche-prospection.md` — lot D : §1 les trois
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
| back | **`feat/fiche-dossier-alignement-ecran`** | lot F, 763 tests verts |
| back | `feat/contexte-dossier` | **prouvée mergée, 0 commit hors `main` — supprimable** |
| back | `fix/ci-pollution-doubles-envoi` | PR #38 mergée **mais la branche locale a divergé depuis** (1 commit hors `main`) |
| back | `docs/memoire-de-session` | 2 commits, sur `origin`, non mergée |
| back | `feat/remise-dossier` | 1 commit, **aucun remote** |
| front | `refractor/best-practice` | **11 commits, LOCAL SEUL** — remote supprimé sans merge |

Lots 1→D mergés sur GitHub (PR jusqu'à #39). `sync` **ne supprime pas** la branche mergée — le
faire à la main (`git branch -d` + `git push origin --delete`, fait pour le lot D). `-d` refuse
toute branche non mergée : s'en servir comme garde-fou plutôt que `-D`. C'est ce refus qui a
révélé la divergence de `fix/ci-pollution-doubles-envoi`.

---

## ⏭️ Next Step

> **Faire merger la PR `feat/fiche-dossier-alignement-ecran` sur GitHub**, puis
> `node ..\.claude\scripts\git-flow.mjs sync back` et supprimer la branche à la main.

### Backlog

- [ ] **Demander les captures des onglets restants du dossier** — « Dates », « Dates autres »,
      « Autres montants », « Annulation / Résiliation », « Acquéreurs ». C'est devenu la façon la
      plus rapide de cadrer un périmètre : deux captures ont suffi pour tout le lot F.
- [ ] **Comprendre les 8 dossiers qui bloquent `ratioAvance`** (`AdvanceAmount` ≠ total catégorie
      `AVANCE` ; la catégorie `PRÉ-RÉSERVATION` est le suspect). Préalable à livrer le champ.
- [ ] **Relayer au dépôt frontend** `GUIDE-FRONT-fiche-prospection.md`, rangs **5 et 6** :
      30 champs **additifs** sur `ProspectionDetailDto`, tous optionnels dans `types.ts`, et pour
      le rang 5 **aux mêmes noms que la liste** — le type de la fiche peut réutiliser les champs
      déjà déclarés.
- [ ] **Décision produit : filtrer les doublons ?** Le lot E expose `marqueurTri` sans filtrer.
      798 pré-prospections (20,9 %) sont du déchet marqué, 795 encore à « 1 A TRAITER ». Un
      `?exclureDoublons=` optionnel, ou une exclusion par défaut (= rupture de comportement).
- [ ] **Fiche tiers** — 5 447 ouvertures/an (3ᵉ écran), `ProspectDetailDto` 52 champs contre 97
      dans `ComSaleProspectView`. Le meilleur rapport valeur/effort restant. Appliquer la même
      méthode, en commençant par **mesurer la définition de la vue** (longueur, nb de
      sous-requêtes) avant de projeter quoi que ce soit.
- [ ] ~~Pré-prospection / `ComSalePreProspectionView`~~ — **clos par le lot E** : vue plate,
      0 sous-requête, rien à en tirer. Ne pas rouvrir.
- [ ] **Décision métier : les 830** (bug #44) — aligner sur `StrEntity.Code` ?
- [ ] **Décision métier : `StrEntityId` à la création** (bug #43).
- [ ] `UserCreate` sur `creerProspection` (bug #41) — préalable à un affichage honnête de `creePar`.
- [x] ~~Dossier : `ComSaleFolderView` (217 colonnes)~~ — **traité au lot F**, sans la vue (trop
      chère). Restent les onglets non vus et la chronologie (12 dates à 0 %, à ne pas livrer en
      bloc).
- [ ] Déduplication de l'entrée `FEATURES.md` (bug #47).
- [ ] **Trancher `refractor/best-practice`** (front, 11 commits orphelins, local seul).
- [ ] `from_front.md` #10 — filtre `?statut=` des dossiers qui renvoie du vide.
- [ ] Les 277 tiers `CLI` (bug #42) · règlement annulé (#11) · `exclureDuPrix` (#12) ·
      garde-fou `S3_ENDPOINT` (#13).
- [ ] Révoquer les secrets échangés en clair.
- [ ] Déclarer le check `Tests` comme requis sur `main`.

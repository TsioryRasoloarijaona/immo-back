# Session Memory
_Last updated: 2026-09-10T15:30+01:00_
_Project: immo-crm (AyadERP) — C:\Users\tsiory\Documents\ayad-internship\prototype\immo-crm_

---

## 🎯 Current Objective

**Exploiter les vues du légataire** pour combler l'écart entre ce que nos `GET` renvoient et ce que
le logiciel de bureau affiche. Trois périmètres inventoriés cette session (prospect, dossier), plus
la généralisation du sélecteur de colonnes et la remise au vert de la CI.

| PR | Contenu | État |
|---|---|---|
| #33 | Récupération du commit de mémoire orphelin | **mergée** |
| #34 | `StrEntity` — type de document sur la prospection | **mergée** |
| #35 | Audit + tranches d'intérêt sur la fiche prospect | **mergée** |
| #36 | Contexte patrimonial / audit / origine sur le dossier | **mergée** |
| #37 | Sélecteur de colonnes sur les listes prospect **et** dossier | **mergée** |
| — | `fix/ci-pollution-doubles-envoi` | **poussée, CI verte, en attente de merge** |

**710 tests verts** (648 en début de refactor, 673 session précédente).

---

## ✅ What Worked

- **Mesurer avec la règle adaptée au type.** Non-`NULL` pour les dates, **non-zéro** pour les
  nombres, **non-vide** pour le texte. Sans ça, `Delai` paraît à 100 % alors qu'il vaut 0,8 %.
- **Matérialiser une vue large avant de la mesurer.** 217 scans de `ComSaleFolderView` (sous-requêtes
  corrélées) dépassent 5 min ; `SELECT * INTO #v` puis comptage dynamique sur `#v` → quelques
  secondes. Génération du SQL depuis `sys.columns` pour ne pas retaper 217 noms.
- **Vérifier qu'une colonne est un doublon plutôt que le supposer** — un `SUM(CASE WHEN a=b)` sur la
  table entière tranche en une requête.
- **Isoler une variable à la fois.** Deux tests corrigés d'un coup = cause inconnue. Retirer
  `setIsActive` et réobserver a prouvé lequel comptait.
- **Reproduire un échec de CI localement avant de le corriger** :
  `-Dsurefire.runOrder=reversealphabetical` a rejoué exactement les deux mêmes échecs.
- **Lire la définition de la vue (`sys.sql_modules`)** plutôt que sonder : c'est elle qui a donné la
  règle d'agrégation des tranches et révélé que `Mobile1` est un alias.
- **Vérifier les parents du commit de merge après chaque PR** — c'est ce qui a rattrapé un commit
  perdu.

## ❌ What Failed

- **`gh pr list` sans `--repo`** → renvoie une liste vide sans erreur. Toujours passer `--repo`.
- **Heredoc bash + `\n` dans une chaîne Java** → écrit de vrais retours à la ligne, casse les
  littéraux (« unclosed string literal »). Passer par `chr(10)`/`chr(92)` explicites.
- **Supposer que la chaîne agrégée `CfgTranches` était ambiguë** → vérifié : **aucune** des 16
  descriptions de tranche ne contient de virgule. L'argument retenu pour la liste structurée est
  plus modeste (rien ne garantit le séparateur).
- **Annoncer `Mobile1` comme un second numéro** → c'est `CfgTier.Mobile` brut. Les 13 632 lignes
  « différentes » l'étaient parce que je comparais du **formaté** à du **brut**.
- **`git-flow start` avec des fichiers non commités** → refusé, et `commit` refuse sur `main` : les
  deux garde-fous se croisent. Passer par `git stash` → `start` → `stash pop`.
- **Croire un `main` vert** → sur `main`, seul le workflow de **déploiement** tourne. La suite de
  tests ne s'exécute que sur les branches.

---

## 🐛 Bugs & Errors Log

| # | Error / Bug | Status | Fix applied |
|---|---|---|---|
| 48 | **CI rouge sur chaque branche depuis la PR #32** — `$.emailEnvoye` false. Doubles d'envoi = singletons Spring, `ConfirmationVisiteTest` les éteignait sans les restaurer après son dernier test | **Fixed** | `@AfterEach` dans la classe qui mute. Reproduit puis vérifié dans les deux ordres |
| 49 | **La suite de tests ne tourne sur aucune PR ni sur `main`** — un merge part sans que les tests aient tourné | **Open** | Non traité (choix utilisateur). Ajouter le déclencheur `pull_request` au workflow |
| 50 | `PR #32` a mergé un head antérieur, laissant `9ddbd0c` orphelin | **Fixed** | Rouvert une PR depuis la même branche (#33). **Toujours vérifier `git log -1 --format=%P` du merge** |
| 51 | Le hook `guard-branch` n'a **pas** bloqué une édition alors que le dépôt était sur `main` | **Open** | Constaté, non investigué. Le commit a bien fini sur une branche |
| 52 | `profession` (fiche prospect) vient de `CfgTier.Quality` = **0 ligne / 13 664** — toujours nul. `professionId` est le seul référentiel servi **sans** son libellé | **Open (mineur)** | Documenté ; `AdmProfessionId` n'est qu'à 0,2 %, correction non rentable |
| 53 | `notes` (fiche dossier) vient de `ComFolder.Notes` = **0 ligne / 358** | **Open (mineur)** | Documenté, pas retiré (romprait le contrat front) |
| 54 | **`Agreement*` vs bloc principal** : miroir sur la plupart des lignes, divergent sur 20 % (`AgreementPrice`≠`Price` sur 72/358, `Total`≠`Price` sur 136/358) | **Open — décision métier** | Écarté du catalogue tant que « lequel fait foi » n'est pas tranché |
| 43 | `creerProspection` ne pose pas `StrEntityId` → prospections **invisibles du logiciel de bureau** | **Open — décision métier** | Les 4 lignes identifiées : 1 API, 2 `n8n_api`, 1 script de test |
| 44 | **830 pré-prospections** dans la liste de vente (+ 4 `RntProspection`, location) | **Open — décision métier** | Rendu **visible et filtrable** via `?typeProspection=` sans rien reclasser |
| 41 | `creerProspection` ne pose ni `UserCreate` ni `UserCreateDateTime` | **Open** | `UserCreate` peuplé à 34,3 % seulement |
| 42 | 277 tiers `CLI` liés à une prospection, invisibles de `GET /api/prospects` | **Open** | Non tranché |
| 45 | `prix`/`surface` morts mais exposés | **Partiellement fixed** | Hors sélection par défaut, `@Schema` explicite |
| 47 | `FEATURES.md` porte deux entrées pour `feat/vues-legacy-prospection` | **Open (mineur)** | — |
| 39 | `sync-vps-yesRemsDev.sql` ne transporte aucune table `Adm*` | **Open (par conception)** | — |
| 11 / 12 / 13 | Règlement `CANCELED` non filtré · `POST /echeancier` sans `exclureDuPrix` · `S3_ENDPOINT` pointe la prod | **Open** | Latents |

---

## 🔑 Key Points to Remember

### Les pièges de mesure — ils ont faussé plusieurs chiffres

| Piège | Exemple |
|---|---|
| Les colonnes **numériques** stockent `0`, pas `NULL` | `Delai` : 14 118 non-`NULL`, **111 utiles (0,8 %)**. Idem `MapRadius` 16,7 %, `Age` 0,4 %, `StatusIsCancellation` 0,2 % |
| Les colonnes **texte** stockent `''`, pas `NULL` | `cin` annoncé 99 %, vaut **2,7 %** |
| **Peuplé ≠ informatif** | `StlCurrencyCode` = `MAD` (1 valeur distincte), `SocialForm` = « Personne physique » sur 13 661/13 664 |
| Les **alias de vue** ressemblent à des manques | `Commercial` = `CfgCommercialDescription`, `TierEmployee` = `Commercial`, les 8 `Tier*` = leur homologue, `CfgTierFirstName`+`LastName` = décomposition de `CfgTierDescription` (14 113/14 118) |

**`Mobile1` n'existe dans AUCUNE table** — seulement dans les 8 vues prospect/client. C'est
`CfgTier.Mobile` brut, tandis que ce que la vue appelle `Mobile` est la version **formatée**
(`(212) 6 98 29 80 95`). Ne jamais mapper une colonne de vue sans vérifier `INFORMATION_SCHEMA`.

### Les trois inventaires

| Vue | Colonnes | Servies | Vrais manques restants |
|---|---:|---:|---|
| `ComSaleProspectionView` | 148 | 84 | `StrEntity*` (traité), `LastActiveAlertOld`, `AdmCountryId/Code`, `TempField1/2` |
| `ComSaleProspectView` | 97 | 58 | 6, dont audit et tranches — **traités** |
| `ComSaleFolderView` | **217** | 26 → ~45 | ventilation financière (5 postes vivants/12), chronologie (38 dates, 12 mortes) |

`CfgTierView` (134 col.) est **1:1 avec la table** `CfgTier` ; `ComSaleProspectView` en est le
sous-ensemble « prospect de vente ». `ComSaleCustomerView` (101 col., 332 lignes) jamais regardée.

### Chaînes de résolution à ne pas redécouvrir

- **Dossier → projet passe par la TRANCHE, jamais par l'article** : `StkItem.CfgProjectId` est
  `NULL` sur les **358** articles de dossier. Chaîne : `ComFolder.stkItem` → `stkHierarchy` →
  `cfgProject` → `cfgCompany`, vérifiée 358/358.
- **Tranches d'intérêt d'un prospect** = union **DISTINCT** de la tranche principale de chaque
  prospection et de ses `ComProspectionProject`. 13 246 prospects en ont 1, 254 en ont 2, 8 en ont 3.
- **`ice` vient de `CfgTier.ICE`**, pas de `FiscalCode` (0,4 %, non servie).
- `ComFolder` n'a **pas** de colonne `AdvanceAmount` : le « montant d'avance » de la fiche est
  `AgreementAmount` (99,7 %).

### Le sélecteur de colonnes — architecture

Trois catalogues (`ColonneProspection` 56, `ColonneProspect` 46, `ColonneDossier` 39) partagent
`domain/Colonne` + `domain/Colonnes` (résolution générique) + `domain/ColonneInconnueException`.

**Le `ThreadLocal` porte la demande BRUTE, pas la sélection résolue.** C'est la correction
structurelle du lot : le montage d'origine y déposait les défauts de `ColonneProspection`, ce qui
aurait fait revenir les listes prospect et dossier **avec la seule clé `id`**. Il y a désormais
**une instance de `SelectionColonnesPropertyFilter` par catalogue**, portant ses propres défauts.

Trois états distincts : `null` (hors requête → tout sérialiser), `Demande.noms() == null`
(sans `?colonnes=` → défaut du catalogue), noms renseignés (sélection explicite).

Rappels du lot 2 toujours valables : le filtre Jackson **doit être enregistré globalement**
(sinon 500 sur `GET /api/pre-prospections` et la hotline), et la **sélection par défaut doit être
réellement appliquée**.

### Pièges de test

- **`CfgCompany.isActive` est obligatoire** sur toute fixture de dossier rattaché à un projet. Le
  périmètre commercial s'applique à la **fiche** autant qu'à la liste → **404**, et l'échec
  ressemble à un bug de mapping. 8 tests sur 9 échouaient pour cette seule raison.
- **`CfgTierType.Code = PRO`** obligatoire pour qu'un tiers apparaisse dans `GET /api/prospects`.
- **Les doubles d'envoi sont des singletons Spring** : leur état survit au rollback. Qui les mute
  doit les restaurer en `@AfterEach`, pas seulement en `@BeforeEach`.
- `jsonPath(...).exists()` **échoue sur `null`** → utiliser `doesNotExist()`.
- Ne jamais supposer une ligne préexistante : la base de test est partagée entre classes.

### La CI — ce qu'il faut savoir avant de la croire

- Sur **`main`** : uniquement « Construire et pousser l'image » + « Déployer sur le VPS ».
- Sur une **branche** : « Suite complete sur SQL Server ephemere ».
- **Aucun déclencheur `pull_request`** → un merge part sans que les tests aient tourné. C'est ce qui
  a laissé la CI rouge pendant des semaines sans rien bloquer (bug #49).
- Récupérer un log : `gh run view <id> --repo TsioryRasoloarijaona/immo-back --log-failed`.

### La topologie des bases (inchangée)

| Base | Rôle |
|---|---|
| `192.168.11.115` / `YesRems` | source de vérité, app de bureau — **lecture seule** |
| `yesRemsProd` (local, `49687`) | copie de travail, celle qu'interroge le backend local |
| `yesRemsCiCheck` (local) | base des tests locaux |
| CI | SQL Server **éphémère**, bâti depuis `ci/schema/*.sql` |
| `yesRemsDev` (VPS) | environnement déployé |

---

## 📁 Important Files & Paths

- **`instructions/FEATURE-contexte-dossier.md`** — inventaire des **217 colonnes**, la méthode de
  mesure, le piège `Agreement*`, les 35 mortes. **Le document de référence pour le dossier.**
- **`instructions/FEATURE-selecteur-colonnes-prospect-dossier.md`** — pourquoi la plomberie a dû
  être refondue avant d'ajouter quoi que ce soit.
- `instructions/FEATURE-audit-tranches-prospect.md` — inventaire des 97 colonnes du prospect.
- `instructions/FEATURE-strentity-prospection.md` — les 5 entités, le tableau croisé du bug #44.
- `instructions/INVENTAIRE-colonnes-prospection.md` — les 148 colonnes (session précédente).
- `domain/Colonne.java`, `Colonnes.java`, `ColonneInconnueException.java` — le socle partagé.
- `domain/ColonneProspect.java`, `ColonneDossier.java` — les deux nouveaux catalogues.
- `web/ColonnesSelection.java`, `SelectionColonnesPropertyFilter.java`, `ColonnesJacksonConfig.java`
  — le montage Jackson 3, **une instance de filtre par catalogue**.
- `entities/str/StrEntity.java` — 3 colonnes mappées sur 42, `@Immutable`.
- `services/dossier/DossierService.java` — `tranche()` / `projet()` / `societe()` portent la règle
  « le projet vient de la tranche ».
- `controller/prospection/ProspectionTypeStrEntityTest.java` — fige l'écart des 830 **sans** le
  corriger.
- `controller/dossier/DossierContexteTest.java` — `projet_vientDeLaTrancheEtNonDeLArticle`.
- `controller/referentiel/SelecteurColonnesProspectDossierTest.java` —
  `chaqueListeAppliqueSonPropreDefaut`.
- `controller/prospection/ConfirmationVisiteTest.java` — le `@AfterEach` du correctif CI.

---

## 🔧 Environment & Config

- **Port SQL Server FIGÉ à 49687.** Il était **dynamique** et avait dérivé à 49702, cassant
  silencieusement `workspace.xml`, les tests et toutes les commandes documentées. Corrigé dans le
  registre (`…\MSSQL15.SQLEXPRESS\…\Tcp\IPAll` : `TcpPort=49687`, `TcpDynamicPorts` vidé) **et
  réservé** (`netsh int ipv4 add excludedportrange … 49687`, car il est dans la plage éphémère
  49152–65535). Si la connexion est refusée : **vérifier le registre**, ne pas chercher un nouveau
  port.
- **Tests — la commande qui marche** :
  ```powershell
  $env:SPRING_DATASOURCE_URL="jdbc:sqlserver://localhost:49687;databaseName=yesRemsCiCheck;encrypt=true;trustServerCertificate=true"
  $env:SPRING_DATASOURCE_USERNAME="tsix"; $env:SPRING_DATASOURCE_PASSWORD="securePassword123"
  .\mvnw.cmd test "-Dspring.jpa.show-sql=false"
  ```
  Ajouter `-Dsurefire.runOrder=reversealphabetical` pour débusquer une pollution entre classes.
- **`gh` CLI v2.100.0 installé** mais **absent du `PATH`** de la session : appeler
  `C:\Program Files\GitHub CLI\gh.exe`. Authentifié. **Toujours passer `--repo`.**
- **⚠️ Ne jamais réécrire un fichier source via PowerShell** (encodage cassé, historique).
- **⚠️ Dette de secrets** : jeton UltraMsg, PAT GHCR, clé MinIO, mot de passe du `.115`, compte VPS —
  passés en clair en conversation. À révoquer.

### État des branches

| Repo | Branche | État |
|---|---|---|
| back | **`fix/ci-pollution-doubles-envoi`** | 1 commit, poussée, **CI verte**, PR en attente |
| back | `main` | `a216fe4` (merge #37), à jour avec `origin` |
| back | `feat/contexte-dossier` | mergée (#36 + #37) — **supprimable, local et distant** |
| back | `docs/memoire-de-session` | 2 commits, **non mergée, aucune PR jamais ouverte**, périmée |
| back | `feat/remise-dossier` | 1 commit, **local seul, aucun remote** |
| front | `refractor/best-practice` | 11 commits, **local seul** |

`sync` **supprime** la branche locale quand elle est prouvablement mergée (corrige la note de la
session précédente) mais **jamais la branche distante** — la supprimer à la main.
`stash@{0}` (wip 172 fichiers, ancien) traîne toujours.

---

## ⏭️ Next Step

> **Merger la PR `fix/ci-pollution-doubles-envoi`** —
> https://github.com/TsioryRasoloarijaona/immo-back/compare/main...fix%2Fci-pollution-doubles-envoi
> puis `node ..\.claude\scripts\git-flow.mjs sync back`, et **vérifier les parents du commit de
> merge** (`git log -1 --format=%P main`) : la PR #32 avait laissé un commit orphelin.

### Backlog

- [ ] **Bug #49 — ajouter le déclencheur `pull_request` au workflow de tests.** Sans lui, aucun
      merge n'est protégé, et le backlog « déclarer le check `Tests` requis sur `main` » n'a pas de
      sens.
- [ ] Supprimer `feat/contexte-dossier` (local + distant), trancher `docs/memoire-de-session`
      (périmée, 36 commits de retard) et `feat/remise-dossier` (local seul).
- [ ] **Décisions métier** : le discriminant des 830 (#44), `StrEntityId` à la création (#43),
      `Agreement*` vs bloc principal (#54).
- [ ] **Dossier, lot 2** : ventilation financière (5 postes vivants sur 12) et chronologie (12 dates
      au-dessus de 10 %) — bloqué par la décision #54.
- [ ] **Pré-prospection** — `ComSalePreProspectionView` (70 colonnes) jamais exploitée, 4ᵉ écran du
      desktop (5 420 ouvertures/an).
- [ ] **Client** — `ComSaleCustomerView` (101 colonnes, 332 lignes) jamais regardée.
- [ ] Relayer au dépôt frontend : trois catalogues de colonnes sur le même contrat (un seul
      composant de sélecteur suffit), et les champs devenus **optionnels** dans `types.ts` pour
      `ProspectListItemDto` et `DossierListItemDto`.
- [ ] `from_front.md` #10 — filtre `?statut=` des dossiers qui renvoie du vide.
- [ ] `UserCreate` sur `creerProspection` (#41), les 277 tiers `CLI` (#42).
- [ ] Règlement annulé (#11), `exclureDuPrix` (#12), garde-fou `S3_ENDPOINT` (#13), doublon
      `FEATURES.md` (#47).
- [ ] Élucider pourquoi le hook `guard-branch` n'a pas bloqué une édition sur `main` (#51).
- [ ] Révoquer tous les secrets listés dans Environment.

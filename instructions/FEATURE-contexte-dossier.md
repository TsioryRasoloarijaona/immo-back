# FEATURE — contexte patrimonial, audit et origine sur le dossier de vente

**Branche** : `feat/contexte-dossier` (backend seul)
**Périmètre** : strictement **additif** — aucune valeur existante ne change.
**Origine** : inventaire de `ComSaleFolderView` (217 colonnes) mené le 10/09/2026, §1.

---

## 1. L'inventaire de `ComSaleFolderView`

`ComSaleFolderView` est **1:1 avec `ComFolder`** : 358 lignes de part et d'autre, aucun filtre.
Nos `GET` en servent **26 colonnes sur 217**.

### Méthode

217 scans d'une vue à sous-requêtes corrélées dépassent les 5 minutes. La mesure a été faite en
**matérialisant la vue dans une table temporaire** (un seul parcours), puis en comptant avec la
règle adaptée à chaque type : non-`NULL` pour les dates, **non-zéro** pour les nombres,
**non-vide** pour le texte. Les deux derniers points ne sont pas du zèle — c'est le piège qui a
déjà faussé deux mesures sur la prospection (`Delai` annoncé à 100 % vaut 0,8 %).

| Bande | Colonnes |
|---|---:|
| ≥ 95 % | 83 |
| 50–95 % | 15 |
| 0–50 % | 84 |
| **0 % — mortes** | **35** |

### Les 35 mortes

`Modifications*` (×4) · `Penality*` (×4) · `Cancel{Amount,Settled,Rest}` · `Rents*` (×3) ·
`TropPercuSettled` · `AgreementCreditSettled` · `CfgSaleManager2{Id,Description}` ·
`CreditAgency` · `Contact` · **`Notes`** · **`Usage`** · `StkItemContenance` ·
`CreditPossessionDate` · `DeliveryDateRealCustomer` · `InsuranceDate` · `InterventionDate` ·
`PrecisionActDateCustomer` · `PrecisionActRegistryDate` · `ReceptionDate` ·
`ReservationDateCustomer` · `ReservationRegistryDate` · `Revival1` · `Revival2` ·
`Revival2ValidityDate`

> **Défaut relevé, non corrigé ici.** `DossierDetailDto.notes` est servi depuis `ComFolder.Notes`,
> mesurée à **0 ligne sur 358**. Le champ est donc toujours nul. Même classe de défaut que
> `profession` sur la fiche prospect (alimenté par `CfgTier.Quality`, 0 ligne). À traiter en
> documentation, pas en retrait — retirer romprait le contrat front.

### Le piège `Agreement*` — à ne surtout pas prendre pour un alias

Le bloc `Agreement*` **miroite** le bloc financier principal sur la plupart des lignes et **diverge
sur une minorité** :

| Paire | Identiques |
|---|---:|
| `AgreementCreditAmount` = `CreditAmount` | **358 / 358** |
| `AgreementSettled` = `AdvanceSettled` | **358 / 358** |
| `AgreementSelfFinancingAmount` = `SelfFinancingAmount` | 357 / 358 |
| `AgreementAdvance` = `Advance` | 353 / 358 |
| **`AgreementPrice` = `Price`** | **286 / 358** |
| **`Total` = `Price`** | **222 / 358** |

`AgreementPrice` s'écarte de `Price` sur **72 dossiers (20 %)**, `Total` sur 136 (38 %). C'est le
cas le plus dangereux : assez proche pour qu'on l'expose par erreur comme un synonyme, assez
différent pour que ce soit faux une fois sur cinq. **Lequel fait foi est une question métier.**
Hors périmètre de ce lot, et à ne pas trancher en écrivant du code.

### La ventilation financière : 5 postes vivants sur 12

| Poste | Montant | Réglé | Reste |
|---|---:|---:|---:|
| `Price` | 100 % | 88,8 % | 43,9 % |
| `SelfFinancing` | 92,5 % | 53,9 % | 39,1 % |
| `Advance` | 41,6 % | 34,6 % | 11,2 % |
| `Expense` | 35,8 % | 5,3 % | 30,4 % |
| `ExpenseSyndic` | 35,5 % | 5,3 % | 30,2 % |
| `Credit` | 7,3 % | 6,7 % | 0,8 % |
| `Modifications` / `Penality` / `Cancel` / `Rents` | **0** | **0** | **0** |

`GET /api/dossiers/{id}/montants` existe déjà mais dérive ses montants de l'**échéancier**
(`ComDocument`) groupés par catégorie : il ne sert aucune de ces colonnes. Les deux vues du
financement coexisteraient plutôt qu'elles ne se remplaceraient. **Hors périmètre.**

### La chronologie : 38 dates, 12 mortes, 12 au-dessus de 10 %

`DocDate` 100 % · `ComProspectionDocDate` 99,2 % · `StkItemOccupancyPermitsDate` 91,3 % ·
`ContractPrintDate` 42,7 % · `AdvanceDate` 42,2 % · `DiscountChangeDate` 26,3 % ·
`AgreementDate` 20,4 % · `BankRecordDate` 19,8 % · `AgreementSavingDate` 17,9 % ·
`DeliveryDateReal` 17,6 % · `AgreementRegistryDate` 13,7 % · `StatusValidityDate` 11,5 %

**`ReservationDate` est à 0,6 %** : la « date de réservation » du métier passe en réalité par
`DocDate`, ce que `ChampDateDossier` supposait déjà. **Hors périmètre.**

---

## 2. Ce que ce lot sert : ~31 colonnes, toutes ≥ 99 %

### 2.1 Contexte patrimonial du dossier

Le dossier sert son article mais **ne dit pas où il se trouve**. Quatre référentiels, tous à
**100 %**, sur `DossierDetailDto` :

| Champs | Source |
|---|---|
| `projetId` / `projetCode` / `projetLibelle` | `ComFolder.stkItem.stkHierarchy.cfgProject` |
| `trancheId` / `trancheCode` / `trancheLibelle` | `ComFolder.stkItem.stkHierarchy` |
| `societeId` / `societeCode` / `societeLibelle` | `…cfgProject.cfgCompany` |
| `vocationId` / `vocationCode` / `vocationLibelle` | `ComFolder.stkVocation` |

> **Le projet ne vient PAS de l'article.** Mesuré : `StkItem.CfgProjectId` est **`NULL` sur les
> 358** articles de dossier. Il passe par la tranche — `StkItem.StkHierarchyId` → `CfgTranche`
> → `CfgProject` → `CfgCompany`, chaîne vérifiée à **358/358** à chaque maillon. Mapper
> `StkItem.cfgProject` directement aurait rendu `null` partout, sans erreur visible.

### 2.2 Contexte patrimonial de chaque article

Un dossier peut porter **plusieurs articles** (`StkItem.ComFolderId`, ex. « MB 18 1 04 » et son
annexe « MB 18 1 04 TER »). Ces quatre référentiels sont donc portés par `ArticleSummaryDto`, pas
par la racine — tous à **100 %** :

`categorieId`/`categorieCode`/`categorieLibelle` · `typeId`/`typeCode`/`typeLibelle` ·
`etageId`/`etageCode`/`etageLibelle` · `immeubleId`/`immeubleCode`/`immeubleLibelle`

Sources : `StkItem.stkItemCategory`, `.stkItemType`, `.stkFloor`, `.stkBuilding` — les quatre déjà
mappées.

### 2.3 Piste d'audit — 4 champs, ~100 %

`creePar` / `creeLe` / `modifiePar` / `modifieLe` depuis `ComFolder.userCreate…`, **déjà mappées**.
Mêmes noms que sur `ProspectDetailDto` et `ProspectionListItemDto` : ne pas inventer de variante.

### 2.4 Contexte prospection — 99,2 %

`etapeProspection` (`ComProspectionKind.description`), `origineProspection`
(`ComProspectionOrigin.description`), `dateProspection` (`ComProspection.docDate`). Le dossier ne
renvoyait que `prospectionId` et `prospectionCode`.

`null` sur les **3 dossiers sur 358** sans prospection — cas réel, pas une anomalie.

---

## 3. Implémentation

- Aucune entité à créer ni modifier : les huit associations nécessaires sont déjà mappées.
- `DossierDetailDto` : 12 champs de contexte + 4 d'audit + 3 de prospection, **en fin de record**
  pour ne déplacer aucun composant existant.
- `ArticleSummaryDto` : 12 champs, en fin de record.
- `DossierService.toDetail` / `toArticleSummary` : déréférencement défensif à chaque maillon —
  toute la chaîne est en `LEFT JOIN` côté vue, un `null` intermédiaire ne doit jamais lever.
- **`DossierListItemDto` n'est pas touchée** : elle n'a pas de sélection de colonnes, un ajout y
  serait payé sur chaque ligne de chaque page. Même raison que pour la liste des prospects.

---

## 4. Tests

- fiche → projet, tranche, société, vocation servis ;
- **le projet vient bien de la tranche** — un article sans `CfgProjectId` (le cas des 358) doit
  quand même rendre le projet ;
- fiche → audit servi ;
- fiche → étape, origine et date de prospection servies ;
- dossier sans prospection → les trois à `null`, réponse 200 ;
- article → catégorie, type, étage, immeuble servis ;
- article sans étage ni immeuble → pas d'exception, champs à `null` ;
- la liste `GET /api/dossiers` reste inchangée.

---

## 5. Impact front

**Aucune rupture.** 19 champs de plus sur `GET /api/dossiers/{id}` et 12 sur chaque article
embarqué ; aucun retrait, aucun changement de type. `GET /api/dossiers` est inchangée.

Dans `crm-prototype/src/types.ts`, `DossierDetailDto` et `ArticleSummaryDto` gagnent des champs
**optionnels** — tous peuvent valoir `null` sur un dossier dont la chaîne patrimoniale est
incomplète, même si aucun cas n'existe aujourd'hui en base.

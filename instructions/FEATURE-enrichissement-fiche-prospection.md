# FEATURE — enrichissement de la fiche prospection (`GET /api/prospections/{id}`)

> Suite directe de `INVENTAIRE-colonnes-prospection.md`. L'inventaire recensait les 148 colonnes
> de `ComSaleProspectionView` sans distinguer ce qui manquait à la **liste** de ce qui manquait à
> la **fiche**. Ce document ne traite que la fiche, et il tranche quatre candidats que l'inventaire
> laissait ouverts.

## 1. Le constat qui commande tout le reste

La définition de la vue (`sys.sql_modules`, 508 lignes) se décompose en trois couches :

| Couche | Contenu | Reproductible hors vue ? |
|---|---|---|
| `ComProspection` | les 56 colonnes de la table | déjà à nous |
| **~30 `LEFT JOIN`** de référentiel | `CfgTier`, `CfgTierType`, `CfgCompany`, `AdmCountry`, `StkItemType`… | **oui, trivialement** |
| **~15 sous-requêtes corrélées** sur `ComActionMessage` | inactivité, nb interactions, dernière communication, visites, alerte | **non — apport unique de la vue** |

Le lot 1 (`FEATURE-vues-legacy-prospection.md`) a mappé exactement les 20 colonnes de la troisième
couche. **Tout ce qui reste à gagner sur la fiche relève de la deuxième** : ce sont des jointures
sur des entités déjà mappées.

**Conséquence de conception : cette feature ne touche pas `ComSaleProspectionView`.** Mapper une
colonne de plus sur l'entité de vue dégraderait l'élagage des sous-requêtes corrélées par SQL
Server — c'est ce qui fait tenir la fiche à 26 ms, et le javadoc de l'entité l'interdit
explicitement sans nouvelle mesure. Les champs ajoutés ici passent tous par les associations JPA
de `ComProspection`.

Corollaire utile : ces champs sont donc **également renseignés sur une pré-prospection**, alors
que le bloc `activite` y vaut `null` en entier.

## 2. Mesures

Base `yesRemsProd`, **14 118 lignes** de `ComSaleProspectionView`. Règle de population : pour les
chaînes, `NULL` **et** chaîne vide comptent comme non renseigné ; pour les numériques, `NULL`
**et** zéro.

### Champs ajoutés

| Champ DTO | Source | Peuplé | Déjà sur la liste ? |
|---|---|---:|---|
| `societeId` / `societeCode` / `societeLibelle` | `stkHierarchy.cfgProject.cfgCompany` | **99,8 %** | oui |
| `prospectCode` | `cfgProspect.code` | **100 %** | oui |
| `typeProspectId` / `typeProspect` / `typeProspectLibelle` | `cfgProspect.cfgTierType` | **100 %** | code seul |
| `mobileProspect` | `cfgProspect.mobile` | 99,8 % | oui |
| `indicatifMobile` | `cfgProspect.mobileIndicatif` | **100 %** | oui |
| `emailFiche` | `cfgProspect.eMail` | 59,1 % | oui |
| `commercialCode` | `cfgCommercial.code` | 99,3 % | oui |
| `vocationCode`, `etapeCode`, `origineCode`, `typeContactCode` | référentiels déjà joints | **100 %** | non |
| `origineSupportCode` | `comProspectionOriginSupport.code` | 50,2 % | non |
| `actionCommercialeCode` | `comAction.code` | 40,2 % | non |
| `raisonFermetureCode` | `comProspectionClosingReason.code` | 60,5 % | non |
| `articleCategorie` / `articleType` | `stkItem.stkItemCategory` / `.stkItemType` | 5,7 % = **100 % des 804 lignes ayant un article** | non |
| `paysId` / `paysCode` / `pays` | `admCountry` (déjà mappé) | 27,3 % | libellé seul |
| `creePar` | `userCreate` | 34,3 % — voir §5 | oui |
| `creeLe` | `userCreateDateTime` | **100 %** | oui |
| `modifiePar` / `modifieLe` | `userUpdate` / `userUpdateDateTime` | **100 %** | oui |

### La fiche était en retard sur la liste

La dernière colonne du tableau est le vrai constat. Le lot B a enrichi
`ProspectionListItemDto` de 18 colonnes — société, code prospect, mobile, indicatif, email de
fiche, code commercial, pays, audit — **sans que la fiche reçoive le même traitement**. Une liste
plus riche que le détail qu'elle ouvre est une incohérence d'API : le front affichait la société
en colonne puis la perdait en cliquant dessus.

Cette feature aligne donc la fiche sur la liste, **en réutilisant exactement les mêmes noms de
champs** (`mobileProspect`, `indicatifMobile`, `emailFiche`, `societeLibelle`, `creeLe`,
`modifieLe`, `typeProspect`…) plutôt qu'en inventant une seconde convention pour les mêmes
données. Elle ajoute par-dessus ce que la liste n'a pas : les **codes de référentiel** et la
**catégorie / le type de l'article**.

⚠️ **Piège de nommage, hérité de la liste et conservé pour la cohérence** : `typeProspect`
qualifie le **tiers** (`PRO`, `PRORNT`) tandis que `typeProspectionCode` qualifie le **document**
(`ComProspection`, `ComPreProspection`). Deux champs, deux notions, un caractère d'écart. Les
`@Schema` des deux le disent explicitement.

La société reste le champ le plus structurant : elle n'est pas saisie sur la prospection — la vue
la dérive de `CfgTranche → CfgProject → CfgCompany`, et l'implémentation suit ce chemin à
l'identique.

### Le mobile est un numéro, pas trois

`CfgTierMobile` (99,8 %), `CfgTierMobile1` (99,8 %) et `CfgTierMobile1Indicatif` (100 %) diffèrent
sur 14 087 lignes sur 14 118, ce qui donnait à croire à trois numéros distincts. La définition de
la vue dit autre chose :

```sql
LTRIM(RTRIM(ISNULL(CASE WHEN (CfgTier.Mobile='' or CfgTier.Mobile=null) THEN ''
  ELSE (ISNULL(CASE WHEN (CfgTier.MobileIndicatif='') THEN ''
    ELSE ('(' + CfgTier.MobileIndicatif + ')') End,'') + ' ' + ISNULL(CfgTier.Mobile,'')) End,''))) AS CfgTierMobile
, CfgTier.Mobile          AS CfgTierMobile1
, CfgTier.MobileIndicatif AS CfgTierMobile1Indicatif
```

`CfgTierMobile` est un **format d'affichage** — `(212) 6 61 83 60 90` — dont les deux autres sont
les composants. L'API expose les composants (`prospectMobile`, `prospectMobileIndicatif`) et
laisse le formatage au front, qui a déjà ses utilitaires. Noter que l'indicatif est stocké
**sans `+`** (`212`).

## 3. Quatre candidats éliminés par la mesure

Ceux-là figuraient comme manques dans l'inventaire §2. Ils n'en sont pas, et **il ne faut pas les
ajouter** :

| Colonne(s) | Verdict | Preuve |
|---|---|---|
| `Commercial` | alias de `CfgCommercialDescription` | **0 écart / 14 118** |
| `ComProspectionOriginType*` | alias de `ComActionMessageType*`, déjà servi sous `typeContact` | **0 écart / 14 118** |
| `LastActiveAlertOld` | doublon strict de `LastActiveAlertDate` | **0 écart** sur les trois tests (présence croisée dans les deux sens, et valeur) |
| `CfgTierFirstName` / `CfgTierLastName` | `Description` = `Prénom + ' ' + Nom` | diffèrent sur **7 lignes / 14 118** |

Le cas `LastActiveAlertOld` clôt une question que l'inventaire §2 laissait explicitement ouverte
(« doublon apparent […] à ne pas exposer sans avoir tranché lequel fait foi »). Les deux colonnes
sont rigoureusement interchangeables ; `LastActiveAlertDate`, déjà exposée sous
`activite.alerteActiveDate`, reste la seule servie.

## 4. Écartés pour d'autres raisons

- **`CfgTierCity` (5,4 %)** — exigerait de mapper `CfgTier.AdmCityId`, association absente de
  l'entité. Coût de mapping pour 5 % de couverture : reporté, pas refusé.
- **`StatusIsCancellation` / `StatusIsClosing`** — 22 et 12 lignes (0,2 % / 0,1 %). Calculées par
  la vue depuis `StrStatus.BoolField002/003`, et l'information est déjà portée par
  `activite.statutDossier`.
- **`CfgCustomerCode` / `CfgCustomerDescription` / `ComFolderDescription`** (2,5 %, soit 100 % des
  359 converties) — vraies, mais elles obligeraient soit à mapper trois colonnes de plus sur
  l'entité de vue (interdit, §1), soit à charger `ComFolder` en une requête supplémentaire sur
  chaque fiche pour un cas qui concerne 2,5 % d'entre elles. `activite.clientId` et
  `activite.dossierCode` suffisent au front pour établir le lien.
- **`MapRadius` (16,7 %)** — un rayon sans centre : `MapLatitude`/`MapLongitude` sont à 0 %.
- **`TempField1` / `TempField2`** (9,6 % / 7,6 %) — contenu non qualifié, à ne pas exposer sans
  savoir ce qu'ils portent.
- **`StatusImg`** — type `image` (binaire). À ne jamais sérialiser, quelle que soit sa population.

## 5. Deux défauts que cette feature documente sans les corriger

**`proposeurId` / `proposeurNom` sont morts.** `CfgProposerId` est renseigné sur **1 ligne sur
14 118**. Même catégorie que `prix` et `surface` (`INVENTAIRE` §1) : le champ reste exposé pour ne
pas rompre le contrat, mais son `@Schema` porte désormais la mesure et la consigne de ne pas
l'afficher.

**`creePar` est à 34,3 %.** Cohérent avec le bug #41 : `creerProspection` ne pose ni `UserCreate`
ni `UserCreateDateTime`. Le champ est exposé — `dateCreation`, elle, est à 100 % — mais un front
qui affiche l'auteur verra deux fiches sur trois sans auteur. La correction du défaut de création
est un travail distinct.

Accessoirement : `ComProspection.AlertValidityDate` et `AlertMessage` existent sur la table, sont
absentes de la vue, et sont mesurées à **0 %**. Rien à en tirer.

## 6. Impact front

**Additif — aucune rupture.** Aucun champ n'est retiré, aucun ne change de type, aucun ne devient
requis. Les nouveaux champs de `ProspectionDetailDto` sont tous **optionnels** dans `types.ts` :
chacun peut valoir `null` quand la donnée n'est pas là, et les taux du §2 disent à quelle
fréquence s'y attendre.

Bonne nouvelle pour l'intégration : les champs communs avec la liste **portent les mêmes noms**,
donc le type de la fiche peut réutiliser les champs déjà déclarés pour
`ProspectionListItemDto` au lieu d'en dupliquer une variante.

Quatre pièges à relayer :

1. **`indicatifMobile` n'a pas de `+`** — c'est `212`, pas `+212`. Le front compose l'affichage
   (`indicatifMobile` + `mobileProspect`).
2. **`societeLibelle` est dérivée de la tranche**, pas saisie : une prospection sans tranche
   (0,2 % des cas) n'a pas de société, et ce n'est pas une anomalie de données.
3. **`typeProspect` ≠ `typeProspectionCode`** — type du tiers contre type du document. Voir §2.
4. **`proposeurId` / `proposeurNom` et le bloc budgétaire ne doivent pas être affichés** —
   mesurés morts (§5 et `INVENTAIRE` §1).

Contrairement au bloc `activite`, ces champs restent renseignés sur une **pré-prospection** — et
c'est le même endpoint qui la sert : `/api/pre-prospections` n'expose pas de route de détail
propre, `GET /api/prospections/{id}` sert les deux. Une fiche sans `activite` n'est donc pas une
fiche vide.

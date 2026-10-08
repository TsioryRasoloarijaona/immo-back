# Inventaire — toutes les colonnes de la prospection dans le légataire

> **Étape 1 : savoir quelles colonnes existent.** Ce document n'engage aucun endpoint. Il
> recense ce que le logiciel de bureau peut afficher sur une prospection, mesure ce qui est
> réellement rempli, et confronte le tout à ce que nos `GET` renvoient aujourd'hui. Les décisions
> d'API (réutiliser ou créer une méthode de rendu) viennent après, sur cette base.

## Méthode

Source : `ComSaleProspectionView`, **148 colonnes**, la vue que le logiciel de bureau lit pour
son écran Prospection (7 372 ouvertures sur 12 mois, l'écran le plus utilisé du logiciel).

Population mesurée sur les **14 118 lignes réelles** de `yesRemsProd`, en une passe, avec une
règle par type — pour les chaînes, `NULL` **et** chaîne vide comptent comme non renseigné ; pour
les numériques, `NULL` **et** zéro. Sans cette distinction, cinq colonnes de cette vue passent
pour remplies à 100 % alors qu'elles sont vides (piège déjà rencontré au lot 1).

---

## 1. Le défaut à traiter en premier : le bloc « qualification » est mort

| Colonne | Renseignée |
|---|---:|
| `Price` (budget) | **4 lignes sur 14 114** |
| `Area` (surface) | **0** |
| `AreaTo`, `PriceTo` | **0** |
| `AdvanceAmount` (avance) | **0** |
| `FundingOwnBase`, `FundingCredit`, `FundingOther` | **0** |

`Price` n'est jamais `NULL` : il vaut **0** sur 14 110 lignes. Ce n'est donc pas une donnée
absente que le front peut masquer, c'est un zéro qu'il affiche.

**Conséquence directe.** `ProspectionListItemDto.prix` et `.surface` sont exposés comme colonnes
de liste (`PRIX` porte même le libellé « Budget »), et `ProspectionDetailDto` expose les huit
champs. En pratique la liste affiche « 0,00 DH » de budget et « 0 » de surface pour la totalité
du portefeuille. Ce n'est pas un manque d'information : c'est de l'information fausse.

À trancher : les retirer de la sélection par défaut, ou les retirer tout court. Retirer un champ
d'un DTO est une rupture pour le front — d'où la mention ici plutôt qu'une décision unilatérale.

---

## 2. Ce que la vue expose et que nous ne renvoyons pas

Classé par population. Ce sont les candidats réels à l'enrichissement des `GET`.

### Fortement peuplé (≥ 99 %) — les manques les plus nets

| Colonne(s) | Peuplé | Ce que c'est |
|---|---:|---|
| `CfgCompanyId` / `Code` / `Description` | 99,8 % | **La société / entité juridique** (ADMOD, GHRIZA). Elle filtre déjà nos résultats côté serveur mais n'est jamais renvoyée : le front ne peut pas l'afficher ni la proposer en filtre. |
| `ComProspectionOriginTypeId` / `Code` / `Description` | 100 % | **Le type d'origine**, au-dessus de l'origine. Nous n'exposons que l'origine et son support. |
| `CfgTierTypeId` / `Code` / `Description` | 100 % | Type du tiers (`PRO`, `PRORNT`…). |
| `StrEntityId` / `Code` / `Description` | 100 % | Le discriminant prospection / pré-prospection. Voir §5. |
| `UserCreateDateTime`, `UserUpdate`, `UserUpdateDateTime` | 100 % | **Piste d'audit.** `UserCreate` n'est qu'à 34,3 % (voir §5). |
| `CfgTierMobile`, `CfgTierMobile1`, `CfgTierMobile1Indicatif` | 99,8–100 % | Les numéros bruts. Nous ne renvoyons qu'un `telephone` déjà fusionné — pratique pour appeler, insuffisant pour éditer ou dédoublonner. |
| `CfgTierCode` | 100 % | Code du prospect. |
| `CfgCommercialCode` | 99,3 % | Nous n'exposons que le nom. |
| `IsOldCustomer` | 100 % | Déjà exposé en détail (`ancienClient`), **absent de la liste**. |

### Moyennement peuplé (25–99 %) — utile, à afficher conditionnellement

| Colonne(s) | Peuplé | Ce que c'est |
|---|---:|---|
| `ClosingDate` | 62,9 % | Date de clôture. En détail, **absente de la liste**. |
| `ComProspectionClosingReason*` | 60,5 % | Motif de clôture. Idem. |
| `CfgTierEMail` | 59,1 % | Email brut de la fiche. |
| `ComProspectionOriginSupport*` | 50,2 % | Support de l'origine. En détail, absent de la liste. |
| `ComActionId` / `Code` / `Description` | 40,2 % | Action commerciale (campagne). En détail, absente de la liste. |
| `UserCreate` | 34,3 % | Auteur de la création. |
| `AdmCountryId` / `Code` / `Description` | 27,3 % | Pays du prospect. **Jamais exposé.** |
| `LastActiveAlertOld` | 25,2 % | Doublon apparent de `LastActiveAlertDate`, à ne pas exposer sans avoir tranché lequel fait foi. |
| `MapRadius` | 16,7 % | Rayon de recherche — **mais `MapLatitude`/`MapLongitude` sont à 0 %**, donc un rayon sans centre. Inexploitable. |
| `ClosingReasonText` | 10,3 % | Motif libre. |
| `TempField1` / `TempField2` | 9,6 % / 7,6 % | Champs libres de la saisie initiale. Contenu à qualifier avant de l'exposer. |

### Faiblement peuplé (< 10 %) — non pas vide, mais rare

| Colonne(s) | Peuplé | Note |
|---|---:|---|
| `StkItemId`, `Article`, `ArticleDescription`, `ItemCategory`, `ItemType` | 5,7 % | Déjà exposé en détail. `ItemCategory`/`ItemType` ne le sont pas. |
| `CfgTierCity` | 5,4 % | |
| `CfgTierIDNumber` (CIN) | **2,7 %** | ⚠️ Exposé au lot 1 comme « 99 % ». **Le chiffre était faux** — voir §4. |
| `ComFolder*`, `CfgCustomer*`, `Status*` | 2,5 % | Conversion en dossier. Taux réel de conversion, pas un manque. |
| `CfgTierPhone` | 1,7 % | Le fixe est quasi mort ; le mobile est à 99,8 %. |
| `AdvanceDate` | 1,1 % | |
| `Delai` | 0,8 % | |
| `ItemPresentationType` | 0,9 % | |
| `ThereIsInstructionNotRead`, `CfgCommercialForInstructionId` | 0,3 % | |

### Mortes — ne pas exposer

`Area`, `AreaTo`, `Price` (4 lignes), `PriceTo`, `AdvanceAmount`, `FundingOwnBase`,
`FundingCredit`, `FundingOther` (§1) · `CfgTierKindCode` / `Description` · `ComProspectionKindAlertDelay` ·
`CfgProposer*` · `CfgIntermediate*` · `ComPaymentMethod*` · `ComUtilization*` · `ComBudget*` ·
`MapLatitude` / `MapLongitude` · `Project` · `Categories` / `Types` / `Cities` (0,1 %) ·
`TempField3` à `TempField10`.

`StatusImg` est de type `image` (binaire) : à ne jamais sérialiser en JSON, quel que soit son
taux de population.

---

## 3. Les filtres possibles

Filtres actuels de `GET /api/prospections` : `etape`, `origine`, `commercial`, `trancheId`,
`dateDebut`/`dateFin` (sur `DocDate`), `search`, `ouvertes`.

Candidats, classés par ce que la donnée permet réellement :

**Filtres à forte couverture — exploitables immédiatement**

| Filtre | Colonne | Peuplé |
|---|---|---:|
| Projet | `CfgProjectId` | 99,8 % |
| Société / entité juridique | `CfgCompanyId` | 99,8 % |
| Type de contact | `ComActionMessageTypeId` | 100 % |
| Type d'origine | `ComProspectionOriginTypeId` | 100 % |
| Vocation | `StkVocationId` | 100 % |
| Ancien client | `IsOldCustomer` | 100 % |
| **Inactivité** (`>= n` jours) | `InactivityTimeout` | 100 % |
| Dernière communication (plage) | `LastCommunicationDate` | 90,4 % |
| Nombre d'interactions (`>= n`) | `ActionsMessageNumber` | 97 % |

`InactivityTimeout` est le plus intéressant des trois derniers : c'est le tri de relance du
logiciel de bureau, et il est renseigné partout.

**Filtres à couverture moyenne — utiles mais partiels**

Motif de clôture (60,5 %) · support d'origine (50,2 %) · action commerciale (40,2 %) ·
créateur (34,3 %) · pays (27,3 %) · alerte active (25,2 %) · nombre de visites (20,8 %).

**Filtres à couverture faible — n'ont de sens qu'en booléen**

Converti en dossier (2,5 %) : « a un dossier / n'en a pas » est exploitable ; filtrer *sur* un
dossier précis ne l'est pas.

**Impossibles** : budget, surface, avance, financement (§1) ; géolocalisation.

⚠️ **Le tri est une autre affaire que le filtre.** Trier sur une colonne d'activité renvoie
aujourd'hui 400 : le tri s'applique sur `ComProspection`, qui ne porte pas ces colonnes. Un
filtre sur `InactivityTimeout` pose la même question — filtrer dessus exige que le prédicat
atteigne la vue, ce que l'architecture actuelle (liste sur table, enrichissement par lot) ne
permet pas. **C'est le principal arbitrage de l'étape 2**, et il est mesuré :
servir la liste depuis la vue coûte 513 ms à l'`OFFSET 5000` contre 2–5 ms aujourd'hui.

---

**Coût mesuré des filtres sur colonnes calculées** (déplacé du Javadoc de
`FiltresAvancesProspection` le 2026-10-07) : les cinq filtres portés par
`ComSaleProspectionView` passent par une sous-requête — 231 ms page 1 et 302 ms à
`OFFSET 5000`, contre 100/134 ms sans. Le surcoût n'est payé que si l'un d'eux est
actif et reste plat en profondeur ; servir la liste depuis la vue coûtait 515 ms
dès la première page.

## 4. Correction d'un chiffre publié au lot 1

`FEATURE-vues-legacy-prospection.md` et `ActiviteProspectionDto` annoncent **`cin` peuplé à
99 %**. La mesure exhaustive donne **2,7 %** (`CfgTierIDNumber`, 386 lignes sur 14 118).

L'erreur vient d'une mesure faite avec `IS NOT NULL` sur une colonne qui porte des chaînes
vides — exactement le piège que le lot 1 documentait par ailleurs. Le champ reste exposé, il est
simplement beaucoup plus rare qu'annoncé : à traiter comme conditionnel, pas comme une colonne
d'identité fiable.

## 5. Deux points déjà ouverts que cet inventaire confirme

**`UserCreate` à 34,3 %.** Les deux tiers des prospections n'ont pas d'auteur — cohérent avec le
bug #41 (`creerProspection` ne pose ni `UserCreate` ni `UserCreateDateTime`). Exposer l'auteur
n'a d'intérêt qu'une fois ce défaut corrigé.

**`StrEntity` à 100 % dans la vue** — par construction, la vue fait un `INNER JOIN` dessus. Les
lignes sans entité n'y sont pas, et sont donc invisibles du logiciel de bureau
(`FEATURE-vues-legacy-prospection.md` §8).

---

## 6. Ce que l'étape 2 doit trancher

1. **Les champs morts déjà exposés** (`prix`, `surface`, avance, financement) — retirer de la
   sélection par défaut, ou du DTO.
2. **Les manques à ≥ 99 %** — société, type d'origine, type de tiers, audit, numéros bruts :
   les ajouter au catalogue de colonnes, ou à la fiche.
3. **Filtrer sur une colonne de vue** (`InactivityTimeout` en tête) : cela impose de faire
   porter le prédicat par la vue. C'est l'arbitrage structurant, avec un coût mesuré.
4. **Réutiliser ou créer une méthode de rendu** : la question ne se pose vraiment que si (3) fait
   basculer la source de la liste. Tant que la liste reste servie par `ComProspection` et
   enrichie par lot, `toListItem` suffit.

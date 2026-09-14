# FEATURE — Encaissements : sélecteur de colonnes et groupement

Aligne `GET /api/encaissements` sur les deux capacités que les listes dossiers, prospections et
pré-prospections ont déjà (lots G et H) : **choisir ses colonnes** et **replier la liste en
groupes**. La liste des encaissements était la seule des quatre à n'avoir ni l'une ni l'autre.

Source : la trace d'audit SQL Server d'OMRAN Estate (`C:\backup\*.sqlaudit`), relevée le
14/09/2026, plus des mesures sur `yesRemsProd`.

---

## 1. Ce que la trace dit — et ce qu'elle ne peut pas dire

L'écran legacy `Yes.Com.Front.Credit.SettlementList` émet exactement :

```sql
SELECT StlSettlementView.* FROM StlSettlementView WHERE (1 = 1) AND (...)
```

**`SELECT *` sur une vue de 107 colonnes.** La trace ne nomme donc aucune colonne d'affichage :
elle prouve que le légataire charge *toute* la vue et laisse sa grille décider. Et la disposition
de cette grille **n'est pas en base** — aucune table `*Layout*`, `*Grid*`, `*Column*` ni
`*Setting*` ne la porte, et `AdmFormsLog` (10 colonnes) ne journalise que l'ouverture d'un écran,
pas sa mise en page. Elle vit côté client, dans le poste Windows.

Conséquence pour la conception : **« quelles colonnes l'écran cible » n'a pas de réponse dans la
trace.** Le seul critère exploitable est donc la mesure — ce que les colonnes de la vue
*contiennent réellement* sur la population que l'endpoint sert.

Rappel de méthode (les trois erreurs du lot G) : `COUNT(colonne)` compte les non-nuls, pas les
valeurs utiles. Toutes les mesures ci-dessous comptent les **valeurs distinctes** et, pour les
numériques, les **non-zéro**.

---

## 2. La population mesurée

Exactement celle que sert `GET /api/encaissements` : Vente (au moins une ligne rattachée à un
`ComDocument`) **et** `IsCredit = 1`.

```sql
SELECT s.* FROM StlSettlement s
WHERE s.IsCredit = 1
  AND EXISTS (SELECT 1 FROM StlSettlementLine l
              WHERE l.StlSettlementId = s.PKey AND l.ComDocumentId IS NOT NULL);
-- 643 règlements (yesRemsProd)
```

---

## 3. Colonnes ajoutées au catalogue, et leur mesure

Toutes sont `parDefaut = false` : **la sélection par défaut reproduit exactement les 27 champs
servis avant ce lot**, règle intouchable depuis le lot des prospects.

| Colonne exposée | Source | Renseignées / 643 | Distinctes |
|---|---|---:|---:|
| `description` | `StlSettlement.Description` | **643** | 530 |
| `montantBase` | `AmountBase` | **643** (643 non-zéro) | 200 |
| `tierPayeurId` / `tierPayeurNom` | `TierPayerId` | **643** | 313 |
| `remisAuCentre` | `IsDcoumentDeliveredToCenter` | **643** | 2 |
| `creePar` / `creeLe` | `UserCreate` / `UserCreateDateTime` | **643** | 17 / 629 |
| `modifiePar` / `modifieLe` | `UserUpdate` / `UserUpdateDateTime` | **643** | 21 / 625 |
| `recuCode` | `ReceiptCode` | 19 | 18 |
| `dateRemiseAuCentre` | `DeliveredToCenterDate` | 4 | 3 |
| `bordereauCode` / `dateBordereau` | `SlipCode` / `SlipDate` | 3 / 4 | 4 / 3 |

`description` est le manque le plus net : **530 libellés distincts sur 643 lignes**, servis par le
légataire jusque dans sa grille de détail des affectations, et absents de notre DTO.

Les quatre colonnes d'audit sont traitées comme dans `ColonneProspect` (groupe `AUDIT`, hors
défaut). `creePar` — 17 valeurs sur 643 — est en prime un bon axe de groupement (« qui a saisi »).

Les six dernières lignes du tableau sont **marginalement peuplées et c'est dit au catalogue**
plutôt que caché : ce sont des scalaires, leur coût est nul, mais elles ne doivent pas être
proposées comme des filtres utiles.

---

## 4. Colonnes écartées, avec la mesure qui le justifie

### 4.1 Les colonnes de groupement du légataire sont vides

`Group1Label`, `Group2Label`, `Group3Label` : **0 / 643**. Ce sont littéralement les colonnes que
le légataire réserve à ses libellés de groupe, et elles ne portent rien sur cette population.

C'est le piège `H1/H2/H3` du lot I à l'envers : là on avait conclu trop vite à une absence, ici on
conclurait trop vite à une présence. Dans les deux cas, la règle est la même — **résoudre la
colonne vers son contenu réel avant de décider**.

### 4.2 Trois colonnes pleines à 100 % qui ne contiennent qu'un placeholder

| Colonne | Renseignées | Distinctes | Valeur unique |
|---|---:|---:|---|
| `Category` | 643 | **1** | `None` |
| `ItemNumber` | 643 | **1** | `Non définie` |
| `ItemDescription` | 643 | **1** | `Non définie` |

Un `COUNT(colonne)` les aurait déclarées parfaitement peuplées. Elles portent zéro information.
**Ne pas les mapper.** (Même famille que `PenalityDescription` et `FinalTitleNumber`.)

### 4.3 Colonnes à zéro

`TierType`, `CategoryId`, `StlCategoryId`, `CfgTierOtherId`, `CfgLawyerId`, `StlSlipId`,
`RSSettlementDate`, `RSSheetCode`, `PropositionSheetCode` — toutes à **0 / 643**.

`AmountRS`, `RsRatio`, `AmountStamp` : 643 lignes non nulles mais **0 non-zéro**. C'est la leçon
`MapLatitude`. Elles restent utilisées par le calcul de `montantNet` (qui vaut donc toujours
`montantBrut` aujourd'hui, ce que le `@Schema` dit déjà) mais ne deviennent pas des colonnes.

`ConvertValue`, `ConvertUnit`, `AmountBaseConverted` : une seule devise
(`StlCurrencyId` distinct = 1), conversion sans objet.

### 4.4 Colonnes non mappables sans toucher à l'entité

`AdmModuleId` (COM 640 / STL 3), `StrEntityId`, `StlCurrencyId` n'ont pas d'association sur
`StlSettlement`. Les ajouter est un autre lot, avec sa propre justification — pas un effet de bord
de celui-ci.

### 4.5 Ce que nous servons déjà et qui est presque vide

À documenter côté front plutôt qu'à corriger — le contrat est publié :

| Champ servi | Renseignées / 643 |
|---|---:|
| `depotId` / `depotCode` | **0** |
| `compteId` / `compteCode` | 12 (1,9 %) |
| `dateReglement` | 10 (1,6 %) |
| `notes` | 3 (0,5 %) |
| `reference` | 491 (76 %) |

---

## 5. Groupement

Le moteur `GroupementService` est déjà générique (`Class<E>` + `Specification<E>`) : ce lot
n'ajoute **aucune ligne au moteur**, seulement un catalogue porteur de `Chemin`.

### 5.1 Axes offerts, par cardinalité mesurée

| Axe | Groupes | Lecture |
|---|---:|---|
| `etat` | 2 | IN_WORK 633 · POINTED 10 |
| `entiteCode` / `entiteLibelle` | 5 | société |
| `typeCode` / `typeLibelle` | 10 | mode de règlement |
| `banqueNom` | 17 | |
| `creePar` / `modifiePar` | 17 / 21 | qui a saisi |
| `comptabilise`, `remisAuCentre` | 2 | booléens |
| `tierNom`, `tierPayeurNom` | 309 / 313 | presque plat, mais offert |
| dates (`dateDocument`, `dateEcheance`, …) | via `Granularite` | |
| montants | groupables **et** sommables | |

`tierNom` et `tierPayeurNom` rendent presque autant de groupes que de lignes. Ils sont offerts
quand même : **c'est au client de juger de l'utilité d'un groupement, pas au catalogue** — la
décision actée au lot H, sur le cas de `prix` côté dossier. Filtré sur une tranche, l'axe redevient
lisible.

### 5.2 Ce qui n'est PAS groupable, et pourquoi c'est structurel

**`dossierId` / `dossierCode` ne sont pas des axes de groupement.** Un règlement n'a pas de
dossier : il a des *lignes*, chacune rattachée à une échéance, donc à un dossier. Mesuré :

- 643 règlements ;
- **654** couples (règlement, dossier) ;
- **8 règlements portant sur plusieurs dossiers**.

Grouper par ce chemin traverserait une association *to-many* et compterait ces 8 règlements
plusieurs fois : la somme des effectifs des groupes rendrait 654 là où la liste annonce 643. Or
l'invariant « somme des effectifs = `totalElements` » est testé
(`DossierGroupementTest#sommeDesEffectifs_egaleLeTotalDeLaListe`) et c'est lui qui rend les
totaux dignes de confiance. Même raisonnement pour une tranche ou un projet, qui se résolvent par
le même chemin.

Les deux colonnes restent **sélectionnables** (elles le sont déjà) ; elles ne sont simplement pas
`groupable`. Le front le lit sur `GET /api/referentiels/colonnes/encaissements`, il n'a aucune
liste à coder en dur.

`montantBrutRestant` et `montantNet` ne sont ni groupables ni sommables : **calculés en Java**,
ligne par ligne, ils n'existent pas en base. Les rendre groupables demanderait une `@Formula`,
comme les délais de cycle du lot I — mesure à faire, autre lot.

---

## 6. Contrat

### Ajouts

- `GET /api/encaissements?colonnes=…` — liste blanche, 400 sur nom inconnu avec la liste des noms
  valides. `id` toujours servi.
- `GET /api/encaissements?groupeColonne=&groupeValeur=&granularite=` — dépliage d'un groupe.
- `GET /api/encaissements/groupes?groupBy=&agregats=&granularite=&tri=&inclureSansValeur=` —
  mêmes filtres que la liste, périmètre commercial compris.
- `GET /api/referentiels/colonnes/encaissements` — catalogue, avec `groupable` / `agregeable`.

### Impact front

**Aucune rupture.** Sans `?colonnes=`, la réponse est octet pour octet celle d'avant : les 27
champs historiques, dans le même ordre. Les nouveaux champs n'apparaissent que s'ils sont demandés.

Deux points à relayer :

1. `EncaissementDto` porte désormais `@JsonFilter` — un client qui désérialise en type strict doit
   tolérer des champs absents, ce qui est déjà le cas des trois autres listes.
2. Le catalogue publie `groupable = false` sur `dossierId` / `dossierCode` / `montantBrutRestant` /
   `montantNet`. Un sélecteur qui proposerait ces axes recevrait un 400 — il doit se piloter sur le
   booléen, pas sur une liste en dur.

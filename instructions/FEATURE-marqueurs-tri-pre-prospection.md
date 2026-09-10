# FEATURE — marqueurs de tri et pointeur de doublon sur la fiche

> Fait suite à `FEATURE-enrichissement-fiche-prospection.md`. Le point de départ était
> « exploiter `ComSalePreProspectionView` comme on a exploité `ComSaleProspectionView` ». **La
> mesure a montré qu'il n'y a rien à y exploiter** — §1. Ce lot livre ce que l'investigation a
> trouvé à la place.

## 1. Pourquoi la vue pré-prospection ne sert à rien

| | `ComSaleProspectionView` | `ComSalePreProspectionView` |
|---|---:|---:|
| Colonnes | 148 | **70** |
| Définition (`sys.sql_modules`) | 21 133 car. | **4 229 car.** |
| Sous-requêtes corrélées | ~15 | **0** |

C'est une vue **plate** : `ComProspection` + 13 `LEFT JOIN` sur des référentiels que l'entité
mappe déjà tous. Elle ne calcule rien, donc elle n'apporte rien que les associations JPA ne
produisent. Il n'existe aucune autre vue pour la pré-prospection — vérifié par nom **et** par
définition (recherche des vues filtrant sur `StrEntityId = 'B92DF3D8-…'`).

L'enrichissement précédent a donc déjà fermé l'écart. Sur les 70 colonnes, ne restaient non
servies que des colonnes **mesurées à 0 % sur les 3 817 pré-prospections** — proposeur,
intermédiaire, mode de paiement, et tout le bloc budgétaire — plus les `TempField*`, qui font
l'objet de ce lot.

**Conséquence : aucune entité de vue n'est créée ni modifiée ici.** Tout vient de
`ComProspection.TempField1` / `TempField10`, déjà mappés.

## 2. Ce que les champs libres contiennent réellement

### `TempField1` — deux conventions dans une seule colonne

| | Pré-prospections | Prospections |
|---|---:|---:|
| `Doublon` / `doublan` | **770** | 151 |
| `spam` | 30 | 2 |
| `Traité` / `traite` | 9 | 1 |
| **texte libre non reconnu** | 39 | **1 200** |

Côté pré-prospection, c'est un **marqueur de tri** posé à la main. Côté prospection, la même
colonne sert à noter la **typologie recherchée** — `salon_+_2_chambres` (518),
`salon_+_1_chambre` (213), `salon_+_3_chambres` (159) — avec aussi des villes et des budgets
(`1_100_000_dhs`).

C'est la raison du nommage retenu (§3) : **le champ brut ne peut pas s'appeler `marqueurTri`**,
il mentirait sur 1 200 prospections.

> Effet de bord à noter pour plus tard : `INVENTAIRE-colonnes-prospection.md` §1 déclare le bloc
> qualification mort parce que `Price` et `Area` valent 0. **L'information existe** — typologie et
> budget — mais en texte libre dans `TempField1`/`TempField2`, jamais dans les colonnes prévues
> pour elle. Ce lot ne l'exploite pas ; il l'expose brute et le documente.

### `TempField10` — un pointeur de doublon, propre

Hypothèse testée sur la totalité des lignes renseignées : **849 valeurs sur 849 résolvent vers un
`CfgTier.Code` existant. Zéro échec** — 690/690 côté pré-prospection, 159/159 côté prospection.
Et 685 des 690 co-occurrent avec `TempField1 = 'Doublon'`.

C'est un mécanisme de déduplication que l'équipe utilise à la main et que rien ne documentait.

### `TempField3` à `TempField9` : 0 %

Mesurés vides sur les deux populations. Non exposés.

## 3. Le contrat

Cinq champs ajoutés à `ProspectionDetailDto` (`GET /api/prospections/{id}`, qui sert aussi les
pré-prospections).

| Champ | Source | Renseigné |
|---|---|---|
| `noteQualification` | `TempField1`, tel quel | 848 pré + 1 354 pro |
| `marqueurTri` | **dérivé** de `TempField1` | `DOUBLON` 921 · `SPAM` 32 · `TRAITE` 10 |
| `doublonDeCode` | `TempField10`, tel quel | 849 |
| `doublonDeId` / `doublonDeNom` | `TempField10` **résolu** en `CfgTier` | 849 aujourd'hui |

**« Tel quel » signifie : ni casse ni orthographe corrigées, mais espaces de bordure retirés et
vide rendu en `null`.** La colonne contient réellement des valeurs comme `"  TRAITÉ  "` ; l'espace
ne porte aucune information et se voit à l'affichage, donc le laisser passer obligerait chaque
client à le retirer. C'est la seule transformation appliquée — le reste du contenu est intouché,
puisque c'est précisément ce qu'on ne sait pas interpréter.

**Pourquoi le brut *et* le dérivé.** `marqueurTri` est une énumération
(`MarqueurTriProspection`) obtenue par correspondance **exacte** sur une valeur normalisée
(trim, minuscules, accents retirés). Elle vaut `null` dès que le contenu n'est pas un marqueur
reconnu — ce qui est le cas de 1 239 lignes, dont les 1 200 typologies. Le front obtient donc un
champ typé sans que l'API prétende que la colonne ne contient que des marqueurs, et
`noteQualification` garantit qu'aucune information n'est perdue au passage.

Correspondance **exacte**, délibérément : une ligne vaut `doublon tribeca/bel air`. La traiter
comme `DOUBLON` par préfixe reviendrait à décider à la place de l'utilisateur ; elle ressort donc
avec `marqueurTri = null` et son texte intact dans `noteQualification`.

**Pourquoi le code brut *et* la résolution.** `CfgTier.Code` **n'est pas unique** : 14 516 tiers
pour 14 504 codes distincts (`000009` est porté par 4 tiers). Aucun des 849 pointeurs réels n'est
ambigu aujourd'hui, mais résoudre un code ambigu reviendrait à désigner un tiers au hasard. La
résolution rend donc `null` sur 0 ou plusieurs correspondances, et `doublonDeCode` conserve le
pointeur brut dans tous les cas. **Le repository renvoie une `List`, jamais un `Optional`** — un
`Optional` lèverait `IncorrectResultSizeDataAccessException` le jour où un pointeur tombe sur un
code dupliqué.

**Coût.** Une requête supplémentaire par fiche, **uniquement quand `TempField10` est renseigné** —
849 lignes sur ~18 000, soit moins de 5 % des appels. Requête par colonne indexée sur une table de
14 516 lignes.

## 4. Le fait opérationnel que ce lot rend visible

**798 pré-prospections (20,9 % du vivier) sont marquées `Doublon` ou `spam`, et 795 d'entre elles
sont encore à l'étape `1 A TRAITER`.**

Le marqueur est posé dans un champ libre ; l'étape, elle, ne bouge pas. Jusqu'ici l'API servait
l'étape et pas le marqueur — le front présentait donc environ 800 déchets comme du travail à
faire.

Ce lot **expose, il ne filtre pas** : aucune liste ne change de contenu, aucun défaut n'est
modifié. Filtrer est une décision produit distincte, et elle est réversible une fois le champ
visible — l'inverse n'est pas vrai.

## 5. Corrections de documentation

**Les taux d'audit dépendent de la population, et le même DTO sert les deux.** Les `@Schema` du
lot précédent annonçaient « 100 % » sans qualifier. Vérifié par mesure directe sur la table :

| | Prospections (14 118) | Pré-prospections (3 817) |
|---|---:|---:|
| `creePar` (`UserCreate`) | 34,3 % | **27,1 %** |
| `creeLe` (`UserCreateDateTime`) | 100 % | 100 % |
| `modifiePar` / `modifieLe` | 100 % | **28,1 %** |

**Trois champs sont structurellement vides sur une pré-prospection** — à documenter pour que le
front ne les attende pas :

- `fermee` : **0 %**. Aucune pré-prospection n'est jamais clôturée ; tout le bloc de clôture
  (`dateFermeture`, `raisonFermeture*`) est sans objet.
- `articleId` / `articleCode` / `articleCategorie` / `articleType` : `StkItemId` à **0,2 %**.
- `proposeur*` : **0 %** (déjà signalé mort côté vente, à 1 ligne sur 14 118).

À l'inverse, `societeLibelle` reste utile : `StkHierarchyId` est à **95,5 %** sur les
pré-prospections.

## 6. Hors périmètre

- **La liste.** `ProspectionListItemDto` et le catalogue `ColonneProspection` ne bougent pas. Les
  y ajouter est un lot à part, qui suppose de trancher d'abord la question du filtrage (§4).
- **Le filtrage des doublons** (§4).
- **L'exploitation de la typologie en texte libre** (§2) — demanderait un travail de parsing sur
  1 200 valeurs non normalisées, et une décision sur ce qu'on en fait.

## 7. Impact front

**Additif, aucune rupture.** Cinq champs optionnels de plus, aucun champ retiré ou modifié,
aucune liste dont le contenu change.

Trois points à relayer :

1. **`marqueurTri` est `null` sur la majorité des fiches qui ont pourtant un
   `noteQualification`** — 1 239 lignes portent du texte libre non reconnu. `null` veut dire
   « pas un marqueur connu », pas « pas de note ».
2. **`doublonDeCode` peut être renseigné sans `doublonDeId`** si le code devient ambigu. Afficher
   le code dans ce cas, sans lien cliquable.
3. **`marqueurTri = DOUBLON` ou `SPAM` n'empêche rien** : la fiche reste à `1 A TRAITER` dans 795
   cas sur 798. C'est au front de décider du traitement visuel.

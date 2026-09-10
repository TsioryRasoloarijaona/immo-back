# FEATURE — exposer le type de prospection (`StrEntity`)

**Branche** : `feat/strentity-prospection` (backend seul — le front n'est pas touché)
**Périmètre** : strictement **additif**. Aucune ligne ne change de liste.
**Origine** : `INVENTAIRE-colonnes-prospection.md` §2 et §5, complété par l'investigation SQL du
10/09/2026 reproduite ci-dessous.

---

## 1. Le problème

`StrEntity` est le **discriminant de type de document du légataire**. `ComProspection.StrEntityId`
le porte, les vues du logiciel de bureau font un `INNER JOIN` dessus, et il vaut 100 % — c'est la
seule colonne à 100 % de la vue que nos `GET` ne renvoient pas.

Notre API ne l'expose nulle part. Elle distingue prospection et pré-prospection par une
**heuristique** (`CfgProspectId IS NULL AND CfgCommercialId IS NULL`) qui n'est pas celle du
légataire. Conséquence : personne ne peut constater l'écart depuis l'application — il faut ouvrir
SQL Server.

### Les cinq entités, mesurées sur `yesRemsProd`

| `PKey` | `Code` | `Description` | Lignes |
|---|---|---|---:|
| `E7C73F70-1111-40D9-AA78-A2825C42B601` | `ComProspection` | Prospection vente | 14 114 |
| `B92DF3D8-158C-4E1E-827A-70FEB172A264` | `ComPreProspection` | Pré-Prospection | 3 817 |
| `D1ADD014-EA98-4E03-8EC9-2776C2AC119D` | `RntProspection` | Prospection location | 4 |
| `B73EA23C-8378-4E7A-AB52-3C2646AB3383` | `PrmProspection` | Prospection gestion immobilière | 0 |
| `5AD6C44B-E7D3-4E68-8E00-9DAFEB1051C7` | `RomProspection` | Prospection location-vente | 0 |
| — | *(`StrEntityId` NULL)* | — | 4 |

**14 114 + 3 817 + 4 + 4 = 17 939**, soit exactement le nombre de lignes de `ComProspection`.
Le total tombe juste : aucune ligne ne se cache ailleurs, la table `StrEntity` compte 162 entrées
au total dont ces cinq concernent la prospection.

### Ce que l'heuristique actuelle produit

| Vérité légataire | Notre heuristique classe en | N |
|---|---|---:|
| `ComPreProspection` | PRÉ ✔ | 2 987 |
| `ComPreProspection` | **PROSPECTION ✘** | **830** |
| `ComProspection` | PROSPECTION ✔ | 14 114 |
| `RntProspection` | **PROSPECTION ✘** | **4** |
| *(NULL)* | PROSPECTION | 4 |

L'écart est **strictement unidirectionnel** : aucune vraie prospection de vente n'est classée en
pré-prospection. L'erreur ne va que dans un sens — 830 pré-prospections polluent la liste de vente,
plus 4 prospections de **location** qui n'y ont rien à faire par nature.

---

## 2. Ce que cette feature fait — et ne fait pas

**Elle expose le type. Elle ne reclasse rien.**

Après ce lot, les 830 et les 4 sont toujours dans la liste des prospections de vente, exactement
comme aujourd'hui. Ce qui change, c'est qu'un client de l'API peut désormais **les compter et les
afficher**, donc que la décision métier qui suit pourra se prendre sur un écran réel plutôt que sur
une requête SQL.

**Hors périmètre, délibérément** — trois décisions métier, chacune avec des conséquences visibles
par de vrais commerciaux :

| # | Changement écarté | Pourquoi il est écarté ici |
|---|---|---|
| 44 | Remplacer l'heuristique par `StrEntity.Code` | Déplace 830 lignes d'une liste à l'autre sous les yeux des utilisateurs |
| — | Exclure `RntProspection` de la liste de vente | Même nature de décision, volume négligeable aujourd'hui |
| 43 | Poser `StrEntityId` à la création | Rend nos prospections visibles dans le logiciel de bureau, sans retour arrière simple sur les lignes déjà créées |

---

## 3. Contrat d'API

### 3.1 Champs ajoutés

Trois champs, sur **`ProspectionDetailDto`** (`GET /api/prospections/{id}`) et
**`ProspectionListItemDto`** (`GET /api/prospections`, `GET /api/pre-prospections`) :

| Champ | Type | Contenu | Exemple |
|---|---|---|---|
| `typeProspectionId` | `UUID` | `ComProspection.StrEntityId` brut | `E7C73F70-1111-…` |
| `typeProspectionCode` | `String` | `StrEntity.Code` | `ComProspection` |
| `typeProspection` | `String` | `StrEntity.Description` | `Prospection vente` |

**Les trois valent `null` quand `StrEntityId` est `NULL`** — 4 lignes en base réelle. Ce `null` est
porteur de sens et doit être documenté comme tel : il signifie « ligne invisible du logiciel de
bureau », pas « type inconnu ».

> **Exception assumée à la convention.** Partout ailleurs (`origine`, `etape`, `vocation`,
> `typeContact`, `actionCommerciale`, `raisonFermeture`), `toDetail` sert **Id + Description** et
> jamais le `Code`. Ici le `Code` est servi **en plus**, parce que c'est lui qui porte le sens
> métier (`ComProspection` vs `ComPreProspection`) tandis que la `Description` n'est qu'un libellé
> d'affichage traduit. Un front qui teste le type doit tester `typeProspectionCode`, jamais
> `typeProspection`.

### 3.2 Catalogue de colonnes

Trois entrées dans `domain/ColonneProspection` :

```
TYPE_PROSPECTION_ID   ("typeProspectionId",   "Id type",          Groupe.IDENTITE, TEXTE, false, Source.ENTITE)
TYPE_PROSPECTION_CODE ("typeProspectionCode", "Code type",        Groupe.IDENTITE, TEXTE, false, Source.ENTITE)
TYPE_PROSPECTION      ("typeProspection",     "Type de prospection", Groupe.IDENTITE, TEXTE, false, Source.ENTITE)
```

`parDefaut = false` sur les trois — **non négociable** : la sélection par défaut doit rester
identique au champ près, sinon le lot cesse d'être additif.

`Source.ENTITE` et non `VUE` : la valeur vient de `ComProspection.StrEntityId` résolu contre
`StrEntity`, pas de `ComSaleProspectionView`. Elle ne doit donc **pas** déclencher la requête de
vue. En revanche le service doit sauter la résolution `StrEntity` quand aucune des trois colonnes
n'est demandée — même discipline que `exigeLaVue`.

### 3.3 Filtre de liste

Un filtre dans `dto/prospection/FiltresAvancesProspection` :

```
?typeProspection=ComPreProspection
```

Sur `StrEntity.Code`, valeurs multiples acceptées. C'est ce filtre qui rend les 830 comptables
depuis le front — donc c'est la partie de ce lot qui a le plus de valeur, pas les champs.

---

## 4. Implémentation

### 4.1 Nouvelle entité `StrEntity`

`entities/str/StrEntity.java` + `repositories/str/StrEntityRepository.java`.

- **Ne mapper que `PKey`, `Code`, `Description`.** La table en compte 42, dont une vingtaine de
  `BoolField0xxLabel` et deux colonnes `text` de configuration d'écran du légataire. Même
  discipline que `ComSaleProspectionView` : on ne mappe que ce qu'on sert.
- `@Immutable` — table de référentiel, jamais écrite par nous.
- `@Nationalized` + `@Size(max = 255)` sur les deux colonnes texte, conformément aux conventions
  d'entité du `CLAUDE.md`.

### 4.2 Ne PAS transformer `ComProspection.strEntityId` en `@ManyToOne`

Il reste un `UUID` brut. Trois raisons :

1. C'est la convention établie pour les références à `StrEntity` dans ce dépôt —
   `AdmEvent.StrEntityId`, `StrHistory.StrConfigEntityId`, `StrStatus.StrConfigEntityId` la suivent
   toutes, et le javadoc actuel de `ComProspection.strEntityId` la justifie déjà.
2. `ComProspection` est une entité centrale : modifier son mapping a un rayon de souffle sans
   commune mesure avec le gain.
3. La résolution par lot est de toute façon nécessaire pour la liste.

**Résolution côté service** : sur la page courante, collecter les `strEntityId` distincts →
`findAllById` → `Map<UUID, StrEntity>` → enrichissement. C'est exactement le motif déjà en place
pour `ComSaleProspectionViewRepository.findByIdIn`. Sur une page de 20 lignes, le nombre de types
distincts est de 1 ou 2 : le coût est celui d'une requête sur une table de 162 lignes.

---

## 5. Tests

Couverture minimale, conforme au `CLAUDE.md` (nominal + 404) :

- fiche d'une prospection de vente → `typeProspectionCode = "ComProspection"` ;
- fiche d'une prospection dont `StrEntityId` est `NULL` → **les trois champs à `null`**, et pas
  d'exception ;
- liste sans `?colonnes=` → les trois champs **absents** de la réponse (preuve du caractère
  additif) ;
- liste avec `?colonnes=typeProspectionCode` → champ présent, et **aucune requête sur la vue** ;
- `?typeProspection=ComPreProspection` → ne renvoie que des pré-prospections.

### Pièges de la base de test

- `yesRemsCiCheck` ne contient que **3 lignes** de `StrEntity` : `ComFolder`, `ComPreProspection`,
  `ComProspection` (semées par `ci/schema/050_views.sql`).
- **Les `PKey` sont identiques entre `yesRemsProd` et `yesRemsCiCheck`** — vérifié le 10/09/2026 :
  `E7C73F70-1111-40D9-AA78-A2825C42B601` et `B92DF3D8-158C-4E1E-827A-70FEB172A264` de part et
  d'autre. Les fixtures peuvent donc porter les UUID réels sans divergence.
- **`RntProspection` est absente de la base de test.** Tout test sur la prospection de location
  exige de l'ajouter d'abord à `ci/schema/050_views.sql`.
- Rappel des lots précédents : `saveAndFlush` avant d'interroger une vue, et `StrEntityId`
  obligatoire sur toute fixture censée y apparaître.

---

## 6. Impact front

**Aucune rupture.** Trois champs optionnels de plus sur deux DTO, absents de la réponse tant que
`?colonnes=` ne les demande pas, et un paramètre de requête facultatif de plus.

Dans `crm-prototype/src/types.ts`, `ProspectionListItemDto` gagne trois champs **optionnels** — ce
qui est déjà le régime de tous les champs de ce DTO depuis le lot 2.

Le point à relayer, quand le front s'en saisira : **tester `typeProspectionCode`, jamais
`typeProspection`** (voir §3.1), et traiter le `null` comme « invisible du logiciel de bureau »
plutôt que comme une donnée manquante.

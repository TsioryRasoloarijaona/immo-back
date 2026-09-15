# FEATURE — Le tableau de bord d'OMRAN, porté sur notre API

Reprendre l'écran d'accueil du logiciel de bureau. Contrairement aux lots précédents, la source
n'est ni une capture d'écran ni une lecture de vue : **la trace d'audit a livré les sept requêtes
SQL exactes** qu'OMRAN exécute à l'ouverture de cet écran, le 15/09/2026 entre 12:28:48 et
12:29:06 (session 73, `application_name = 'OMRAN estate'`). C'est une implémentation de référence,
pas une interprétation.

Mesures sur `yesRems` (base vivante qu'OMRAN alimente) et recoupées sur `yesRemsProd`.
**Backend seul** — voir §7.

> **Lire la trace sans se tromper de client.** Le compte SQL est `tsix` pour tout le monde, y
> compris nos propres `sqlcmd`. Le discriminant est `application_name` (`OMRAN estate` contre
> `SQLCMD`) et accessoirement `client_ip` (127.0.0.1 contre le poste). Et `event_time` est en
> **UTC** : heure locale = UTC + 1, vérifié empiriquement par une requête-sonde horodatée, pas
> déduit.

---

## 1. Ce que l'écran charge réellement

Sept requêtes, trois blocs :

| Heure locale | Bloc | Requête |
|---|---|---|
| 12:28:48 | — | `CfgTrancheView` filtrée par habilitation et `IsForSale = 1` (sélecteur de tranche) |
| 12:28:49 | A | `CfgPrevision` de l'année |
| 12:28:49 | A | `ComPrevisionVersion` — **dernière version** par tranche, agrégée par mois |
| 12:28:55 | A | `SUM(ComDocument.Amount)` groupé par `MONTH(DocDate)` |
| 12:28:55 | A | `ComPrevision` de l'année |
| 12:29:05 | B | `ComFolder` + `PriceSettled` en sous-requête — **ventes concrétisées** |
| 12:29:06 | C | `StlSettlementLine` → `StlSettlement` — **encaissements** |

Les trois blocs portent **tous** le filtre de périmètre du légataire :
`StkHierarchyId IN (SELECT CfgTrancheId FROM AdmUserHabilitation WHERE AdmUserId = …)`.
C'est exactement le mécanisme de `PerimetreCommercialService` — confirmation directe que notre
implémentation du périmètre reproduit bien la sienne.

---

## 2. Bloc A — la courbe mensuelle

### Les prévisions n'existent pas

| Table | `yesRemsProd` | `yesRems` |
|---|---:|---:|
| `CfgPrevision` | **0** | **0** |
| `ComPrevision` | **0** | **0** |
| `ComPrevisionVersion` | **0** | **0** |

Trois des quatre requêtes du bloc A interrogent des tables **vides des deux côtés**. Le graphe
« prévisions vs réalisé » d'OMRAN n'affiche donc qu'une seule courbe chez eux aussi.

> **Décision produit (utilisateur, 15/09/2026) — une seule courbe.**
> `CfgPrevision`, `ComPrevision` et `ComPrevisionVersion` ne sont **pas mappées**. Trois entités et
> une logique de versionnement (`MAX(VersionDate)` par tranche) économisées, pour zéro donnée
> perdue. À rouvrir le jour où quelqu'un saisit des prévisions — la requête légataire est
> conservée ci-dessous pour ce jour-là.

<details>
<summary>La requête de versionnement, conservée pour mémoire</summary>

```sql
SELECT NEWID() AS PKey, ComPrevisionVersion.Month, 0 AS Year,
       SUM(ComPrevisionVersion.Quantity) AS Quantity, SUM(ComPrevisionVersion.Amount) AS Amount
FROM ComPrevisionVersion
INNER JOIN (SELECT MAX(VersionDate) AS MaxVersionDate, StkHierarchyId AS ProjectId
            FROM ComPrevisionVersion
            WHERE Year = @P1 AND StkHierarchyId IN (…habilitation…)
            GROUP BY StkHierarchyId) TT
  ON ComPrevisionVersion.StkHierarchyId = TT.ProjectId
 AND ComPrevisionVersion.VersionDate = TT.MaxVersionDate
WHERE ComPrevisionVersion.Year = @P2
GROUP BY ComPrevisionVersion.Month
```

Le point à retenir : la prévision est **versionnée**, et seule la dernière version de chaque
tranche compte. Une implémentation naïve sommerait toutes les versions.
</details>

### La courbe restante somme l'échéancier, pas l'encaissé

C'est le point le plus trompeur de cet écran. La requête somme **`ComDocument.Amount`**, or
`ComDocument` est l'**échéancier** — des montants *dus*, pas *encaissés*. La preuve tient dans les
chiffres 2026 mesurés :

| Mois | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Montant (kDH) | 150 | 295 | 465 | 460 | 1 140 | 1 117 | 1 338 | **5 767** | — | 380 | 50 |

**Octobre et novembre 2026 sont déjà renseignés alors qu'on est en septembre**, et septembre est
vide. Une courbe d'encaissements ne peut pas avoir cette forme : c'est du prévisionnel contractuel.

> **Décision produit (utilisateur, 15/09/2026) — fidèle à OMRAN, mais nommée pour ce qu'elle est.**
> Même calcul que le légataire, donc chiffres comparables poste à poste pendant la transition,
> mais le champ s'appelle `echeancier`, jamais `realise`. Le mois à venir n'est pas une anomalie
> à masquer.

### Le filtre, et la contradiction du légataire à ne pas recopier

```sql
AND ((IsSaleAdvance=1) OR (IsSaleCredit=1) OR (IsSaleAutoFinancment=1) OR (IsSaleExpense=1))
AND (IsSaleExpense=0)
```

Les deux clauses se contredisent : la première admet les frais, la seconde les rejette. **L'effet
net est « frais exclus »**, et c'est cette intention qu'il faut écrire — pas le bug. Reproduire la
contradiction à l'identique donnerait le bon résultat pour la mauvaise raison, et personne ne
saurait la corriger plus tard.

---

## 3. Bloc B — ventes concrétisées

Assiette : `ComFolder.AdvanceDate` dans l'année, statut ≥ CONCRETISATION, hors sorties.

**2026 : 4 dossiers, 4 081 900 DH.**

### Le trou d'assiette

**232 dossiers portent `AdvanceDate` à NULL** — sur 387 au total, soit 60 %. Ils sont
structurellement absents de ce bloc, quelle que soit l'année. Ce n'est pas un bug du légataire :
`AdvanceDate` est la date de l'avance, et un dossier qui n'en a pas reçu n'a rien à y faire.

> **Décision produit (utilisateur, 15/09/2026) — fidèle, avec un compteur d'exclus.**
> Même clé qu'OMRAN, donc chiffre comparable ; et la réponse porte `dossiersSansDateAvance`, pour
> que le trou cesse d'être invisible. Pas de repli sur une autre date : il ferait diverger le
> chiffre de l'écran d'OMRAN sans qu'on puisse expliquer l'écart en une phrase.

### `PriceSettled`, la sous-requête corrélée

Pour chaque dossier, la somme des règlements d'avance, de crédit et d'autofinancement, **signée**
par `ComSettlementCategory.IsCredit` (`-1` quand faux). Les frais n'y entrent pas — seules les
trois catégories citées. À servir tel quel : c'est le « déjà payé » du dossier.

---

## 4. Bloc C — encaissements de l'année

**2026 : 82 lignes, 7 623 785 DH.**

Filtres, tous nécessaires et tous déjà connus du dépôt :

| Filtre | Raison |
|---|---|
| `StlSettlement.IsCredit = 1` | écarte les décaissements — c'est le correctif du lot K, un décaissement de −470 000 DH faussait les totaux |
| `StlSettlementType.IsNotContabilized = 0` | écarte les types non comptabilisés |
| `StkItem.IsUnderItem = 0` | écarte les sous-lots, qui doubleraient les lignes |
| `ComSettlementCategory.IsSaleExpense = 0` | écarte les frais |
| `YEAR(ISNULL(SettlementDate, EcheanceDate)) = @P1` | **date d'encaissement si pointé, d'échéance sinon** |

Le `ISNULL(SettlementDate, EcheanceDate)` mérite d'être relevé : il reprend l'invariant du lot K —
`SettlementDate` n'est renseignée que sur les règlements pointés (14 lignes réelles sur 3 244), le
reste tombe sur l'échéance.

---

## 5. Ce qu'il faut ajouter, et ce qu'il ne faut surtout pas ajouter

### Déjà mappé — rien à faire

`ComDocument` (`DocDate`, `Amount`, `ComSettlementCategoryId`), `ComSettlementCategory`,
`StlSettlementType`, `StlSettlement`, `StlSettlementLine`, `ComFolder` (`AdvanceDate`, `Price`),
`StkItem`, `CfgTranche`, `CfgTier`, `StrStatus.Sorting`.

### `StrStatus.BoolField002` / `BoolField003` — **ne pas mapper**

Le légataire filtre `BoolField002 = 0 AND BoolField003 = 0`. Vérifié sur les **13** statuts de
l'entité de configuration du dossier (`E7C73F70-…`) :

| Statut | Sorting | `BoolField002` | `BoolField003` | Dossiers |
|---|---:|---:|---:|---:|
| RESERVATION | 1 | 0 | 0 | 53 |
| CONCRETISATION | 2 | 0 | 0 | 72 |
| … (8 statuts intermédiaires) | 3–9 | 0 | 0 | 221 |
| **ANNULATION** | 30 | **1** | 0 | 22 |
| **DESISTEMENT** | 31 | 0 | **1** | 8 |
| LIVRAISON PROVISOIRE | 71 | 0 | 0 | 0 |

`BoolField002 = 1` n'est posé **que** sur ANNULATION, `BoolField003 = 1` **que** sur DESISTEMENT.
Le filtre légataire vaut donc **exactement** `StatutDossier.estSortie()`, qui existe déjà. Les
`CLÔTURE` qui portent `BoolField003 = 1` appartiennent à d'autres modules et ne sont jamais des
statuts de dossier — d'où l'importance d'avoir filtré sur `StrConfigEntityId` avant de conclure.

### `AdmParamsGuid` — **finalement pas mappée**, et la raison compte

Le légataire lit le seuil dans `AdmParamsGuid.DefaultComFolderStatusConcretisation`, qui pointe sur
le statut `CONCRETISATION` (`Sorting = 2`). Le seuil est donc « tout sauf RESERVATION ».

La mapper paraissait le choix fidèle, jusqu'à la vérification d'usage — le réflexe que ce dépôt
s'impose avant tout mapping :

| | `AdmParamsGuid` | `StrStatus` |
|---|---|---|
| `yesRemsProd` | présente | présente |
| **`yesRemsTest`** | **absente** | présente, mais **0 statut de dossier** |

La différence n'est pas de degré. Un `StrStatus` non semé se règle en fabriquant la fixture, ce que
tous les tests de dossier font déjà. Une **table absente** ne se règle pas : toute requête lève
`Invalid object name`, et il faudrait à la fois étendre `ci/schema/010_tables.sql` (fichier
*généré*) et créer la table dans la base de test locale de l'utilisateur.

**Retenu** : résoudre le seuil par `StrStatus` où `Code = 'CONCRETISATION'` dans l'entité de
configuration du dossier, et comparer les `Sorting`. Aucune table nouvelle, aucune mutation
d'environnement, et le comportement est **identique aujourd'hui** puisque le paramètre pointe sur
ce statut — mesuré, pas supposé.

**Ce qu'on perd, explicitement** : si l'exploitation déplaçait le paramètre vers un autre statut,
notre chiffre cesserait de suivre le leur sans rien signaler. Le risque est accepté ici parce que
le seuil porte le même nom que notre constante d'enum et qu'un déplacement serait de toute façon
un changement métier à répercuter. Le jour où il faut la fidélité totale, mapper `AdmParamsGuid`
suppose d'abord de l'ajouter au schéma CI **et** à `yesRemsTest`.

---

## 6. Contrat

### `GET /api/dashboard/annuel`

| Paramètre | Défaut | Rôle |
|---|---|---|
| `annee` | année courante | L'année observée. Les trois blocs la partagent, comme chez OMRAN. |

Un objet unique, pas une collection — même règle que `GET /api/dashboard` et
`GET /api/dossiers/{id}/montants` (cf. `PageDto`, qui n'enveloppe que les listes).

`SyntheseAnnuelleDto` :

| Champ | Type | Contenu |
|---|---|---|
| `annee` | int | rappel de l'année servie |
| `echeancier` | `List<PointMensuelDto>` | **12 points, toujours** — un mois sans échéance vaut `0`, pas une absence de point (§2) |
| `ventesConcretisees` | `VentesConcretiseesDto` | §3 |
| `encaissements` | `EncaissementsAnnuelsDto` | §4 |

`PointMensuelDto` : `mois` (1–12), `montant` (BigDecimal), `lignes` (long).

`VentesConcretiseesDto` : `nombreDossiers`, `prixTotal`, `montantRegle` (somme des `PriceSettled`),
`dossiersSansDateAvance` (§3).

`EncaissementsAnnuelsDto` : `nombreLignes`, `montantTotal`.

### Périmètre

Les trois blocs passent par `PerimetreCommercialService`, comme le reste — et comme le légataire,
qui pose le même filtre sur ses sept requêtes (§1).

### Tests attendus

Minimum du dépôt (`CLAUDE.md` règle 5), plus :

- les **12 mois sont toujours présents**, même vides — c'est ce qui permet au front de tracer une
  courbe sans trous ;
- un dossier en **ANNULATION** et un en **DESISTEMENT** sont exclus des trois blocs, un dossier en
  **RESERVATION** aussi (sous le seuil de concrétisation), un en FORMALISE est compté ;
- une échéance de **catégorie frais** (`IsSaleExpense = 1`) n'entre pas dans la courbe — c'est
  l'intention derrière la contradiction du légataire (§2) ;
- un dossier **sans `AdvanceDate`** est compté dans `dossiersSansDateAvance` et absent de
  `nombreDossiers` ;
- un règlement **non pointé** est rattaché à l'année de son échéance, un règlement **pointé** à
  celle de sa date de règlement (§4) ;
- un **décaissement** (`IsCredit = 0`) n'entre pas dans `encaissements`.

---

## 7. Impact front

**Aucune rupture : un endpoint neuf, aucun existant modifié.** Backend seul ; la consommation côté
`crm-prototype` n'entre pas dans ce lot.

Deux points à relayer tels quels, parce qu'ils sont contre-intuitifs à l'affichage :

> **`echeancier` n'est pas un réalisé.** Des mois futurs y sont renseignés (octobre et novembre
> 2026 l'étaient en septembre). Une courbe qui s'arrête au mois courant masquerait de la donnée
> vraie ; une courbe qui va jusqu'à décembre doit se lire comme un engagement contractuel, pas
> comme un encaissement.

> **`ventesConcretisees.nombreDossiers` ne couvre pas tous les dossiers de l'année.** 60 % n'ont
> pas de date d'avance. Le champ `dossiersSansDateAvance` est là pour que l'écart soit affichable,
> pas pour être ignoré.

# FIX — Synchro : lignes référençant un module hors périmètre (StlSettlementLine)

Branche : `fix/synchro-lignes-hors-perimetre` (depuis `main`).

## Symptôme (prod, `yesRemsDev`)

```
Synchronisation de StlSettlementLine en echec — filigrane inchange, la table sera rejouee au prochain cycle.
Caused by: ...The MERGE statement conflicted with the FOREIGN KEY constraint
"StlSettlementLine_BuyDocument_FK1" ... table "dbo.BuyDocument", column 'PKey'.
```

`StlSettlementLine` échoue **à chaque cycle** : son filigrane ne bouge jamais, et **aucune** de
ses lignes ne se réplique (une seule ligne fautive fait échouer toute la passe de table).

## Cause

`StlSettlementLine` a cinq clés étrangères ; trois pointent vers des tables **hors du périmètre
répliqué** (non suivies par le Change Tracking, jamais présentes dans la copie) :

| Colonne | Table référencée | Module |
|---|---|---|
| `BuyDocumentId` | `BuyDocument` | achat |
| `RntDocumentId` | `RntDocument` | location |
| `StlDocumentId` | `StlDocument` | (hors périmètre) |
| `ComDocumentId` | `ComDocument` | **vente — suivie** |
| `StlSettlementId` | `StlSettlement` | **suivie** |

Une ligne d'encaissement porte le document réglé par **exactement une** clé. Le CRM est
mono-métier (vente) : les lignes achat/location référencent des documents qui ne seront jamais
répliqués → leur `MERGE` viole la FK. Le noyau ~23 tables suivies n'est pas clos par clés
étrangères ; c'est le cas « irréductible » déjà connu (SESSION_MEMORY).

Mesuré sur 3 227 lignes réelles :

| population | nombre |
|---|---:|
| vente (`ComDocumentId` renseigné, 3 FK hors périmètre nulles) | **665** |
| achat (`BuyDocumentId`) | 2 559 |
| location (`RntDocumentId`) | 1 |
| StlDocument | 2 |

Les 665 lignes de vente ont `ComDocumentId` **et** `StlSettlementId` renseignés (tous deux dans
le périmètre) → leur `MERGE` est satisfiable.

## Correctif

On **écarte de la réplication les lignes qui référencent un module hors périmètre**, au lieu de
faire échouer toute la table. `RemonteeLecture.COLONNES_HORS_PERIMETRE` :
`StlSettlementLine → [BuyDocumentId, RntDocumentId, StlDocumentId]`. Chaque colonne listée doit
être `NULL` pour qu'une ligne soit répliquée (`predicatPerimetre`). Le filtre est appliqué dans
les **deux** régimes :

- **BASELINE** (`fusionnerTout`) : `SELECT … WHERE [BuyDocumentId] IS NULL AND … ` → seules les
  665 lignes de vente sont fusionnées.
- **DELTA** (`fusionner`) : une ligne hors périmètre renvoie zéro tuple au `SELECT` filtré →
  traitée comme « absente » → `DELETE` sans objet dans la copie. Le filigrane avance, la table
  n'est plus bloquée.

Mécanisme identique à `COLONNES_ABSENTES_DE_LA_COPIE` (qui retire une *colonne*) mais au niveau
*ligne*, sur critère de contenu.

## Effet

- `StlSettlementLine` se synchronise de nouveau (665 lignes de vente), le filigrane avance, et
  les lignes d'encaissement de vente écrites par OMRAN remontent.
- Les 2 562 lignes achat/location ne sont pas répliquées — elles sont hors du périmètre du CRM.

## Portée / limites

- `ComActionMessage` (autre cas noté en mémoire) **ne boucle pas** de la même façon : ses FK hors
  périmètre sont surtout des tables de référence (Type, Catégorie, AdmUser) présentes dans la
  copie. Non traité ici, pas de symptôme récurrent.
- Si une autre table venait à boucler sur une FK hors périmètre, l'ajouter à
  `COLONNES_HORS_PERIMETRE` (avec la ou les colonnes qui désignent le module externe).

## Fichiers

- `config/RemonteeLecture.java` — `COLONNES_HORS_PERIMETRE`, `predicatPerimetre`, `projection`,
  `selection`, `selectionTout` (filtre appliqué en baseline et en delta).
- `test/.../config/RemonteePerimetreTest.java` — prédicat (logique pure).

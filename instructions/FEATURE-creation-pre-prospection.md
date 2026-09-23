# FEATURE — Création d'une pré-prospection + discriminant `StrEntity`

_Branche : `feat/creation-pre-prospection` — 23/09/2026_

Deux changements liés, décidés ensemble par l'utilisateur :

1. **`POST /api/pre-prospections`** — créer un lead brut, comme l'écran « Pré-prospection »
   d'OMRAN ;
2. **décision #44 de `FEATURE-strentity-prospection.md`** — la pré-prospection se définit
   désormais par `StrEntity.Code = 'ComPreProspection'`, plus par l'heuristique
   `CfgProspectId IS NULL AND CfgCommercialId IS NULL`.

Le 2 est la condition du 1 : une pré-prospection créée correctement (avec son `StrEntityId`)
doit apparaître dans notre liste de pré-prospections exactement comme dans OMRAN.

---

## 1. Ce que fait OMRAN — trace d'audit du 21/09/2026

Création de `26/05948` (18:34:09 UTC, session 87, Macire Kamara). Écritures, dans l'ordre :

| # | Instruction | Commentaire |
|---|---|---|
| 1 | `SELECT COUNT(*) FROM ComProspection WHERE PKey <> @P1 AND tempDescription = @P2 AND tempEMail = @P3 AND tempTel = @P4` | contrôle de doublon (nom + email + tél) |
| 2 | `UPDATE AdmSequence SET SeqValue = SeqValue + 1 WHERE PKey = '51c4f768-…'` | compteur **`Prospection`** — le même que les prospections |
| 3 | `INSERT INTO ComProspection(strEntityId, code, docDate, tempDescription, tempEMail, tempTel, tempField1…10, notes, cfgProspectId, stkHierarchyId, stkItemId, comProspectionKindId, comActionMessageTypeId, comActionId, comProspectionOriginId, comProspectionOriginSupportId, cfgCommercialId, area…, stkVocationId, userCreate, userCreateDateTime, …)` | `StrEntityId = B92DF3D8…` (`ComPreProspection`) ; **`IsClosed` absent de la liste** |
| 4 | `INSERT AdmWebHistorics`, lecture de `StrNotificationView` | journal et notifications propres à OMRAN — non repris |

Le contact n'est **pas** un `CfgTier` : il vit dans `TempDescription` / `TempTel` / `TempEMail`.

### Transformation : en place, par bascule de `StrEntityId`

`26/05947` (session 67) : `INSERT` en pré-prospection à 18:06:01 → `INSERT CfgTier` (le prospect)
à 18:06:49 → `UPDATE ComProspection` par l'ORM pré-prospection (pose `cfgProspectId`) à
18:07:42 → `UPDATE` par l'ORM prospection à 18:07:43, qui **bascule `strEntityId` sur
`ComProspection`** et pose `isClosed`. Même ligne, même `PKey`, même code.

⟹ avec un discriminant `StrEntity`, notre `POST /api/prospections/{id}/transformer` doit faire
la même bascule, sans quoi une pré-prospection transformée resterait dans la liste des
pré-prospections.

---

## 2. Mesures — `yesRemsProd`, 3 823 pré-prospections (`StrEntityId = ComPreProspection`)

| Colonne | Renseignée | Champ de la requête |
|---|---:|---|
| `TempDescription` | 3 818 (99,9 %) | `nom` — **obligatoire** |
| `TempTel` | 3 648 (95 %) | `telephone` |
| `TempEMail` | 2 267 (59 %) | `email` |
| `Notes` | 3 621 (95 %) | `notes` |
| `StkHierarchyId` (tranche) | 3 649 (95 %) | `trancheId` |
| `ComProspectionKindId` (Nature) | 3 727 | `etapeId` |
| `ComProspectionOriginId` | 3 722 | `origineId` |
| `ComActionMessageTypeId` (Type) | 3 720 | `typeContactId` |
| `ComActionId` | 2 304 | `actionCommercialeId` |
| `ComProspectionOriginSupportId` | 1 348 | `origineSupportId` |
| `StkVocationId` | 3 801 — **toutes VENTE** | `vocationId`, défaut VENTE |
| `StkItemId` | 13 | `articleId` |
| `TempField1` | 851 — valeurs `doublon` / `spam` | non exposé : marqueur de tri, pas une donnée |
| `IsClosed` | 0 : 2 884 · NULL : 937 · 1 : 2 | posé à `false` (voir §3) |

Seul `nom` est obligatoire : OMRAN a enregistré `26/05943` avec le nom, les notes et la vocation
seulement. Le code suit la séquence `AA/NNNNN` pour 100 % des saisies humaines (Issam 159/159) ;
les `Code = '50'` / `'90'` sont des insertions n8n, pas une règle.

### Écart de l'ancienne heuristique

| Vérité légataire | Ancienne heuristique | N |
|---|---|---:|
| `ComPreProspection` sans prospect ni commercial | PRÉ ✔ | 2 976 |
| `ComPreProspection` avec prospect **ou** commercial | **PROSPECTION ✘** | 847 |

Ces **847** lignes (mesure du 23/09 ; 830 à l'époque de `FEATURE-strentity-prospection`)
**quittent `GET /api/prospections` et rejoignent `GET /api/pre-prospections`**. Aucune ne fait
le chemin inverse : les lignes sans prospect ni commercial sont toutes de type
`ComPreProspection` (0 exception).

---

## 3. Contrat — `POST /api/pre-prospections`

Corps `CreerPreProspectionRequest` :

| Champ | Type | Colonne | Règle |
|---|---|---|---|
| `nom` | string ≤ 255 | `TempDescription` | **requis**, non blanc — 400 sinon |
| `telephone` | string ≤ 255 | `TempTel` | optionnel, stocké tel quel |
| `email` | string ≤ 255 | `TempEMail` | optionnel |
| `notes` | string ≤ 1000 | `Notes` | optionnel |
| `trancheId` | UUID | `StkHierarchyId` | optionnel ; 400 si inconnu ; **403** hors périmètre commercial |
| `articleId` | UUID | `StkItemId` | optionnel ; exige `trancheId` et doit lui appartenir (400) |
| `etapeId` | UUID | `ComProspectionKindId` | optionnel, 400 si inconnu |
| `origineId` | UUID | `ComProspectionOriginId` | optionnel, 400 si inconnu |
| `origineSupportId` | UUID | `ComProspectionOriginSupportId` | optionnel, 400 si inconnu |
| `typeContactId` | UUID | `ComActionMessageTypeId` | optionnel, 400 si inconnu |
| `actionCommercialeId` | UUID | `ComActionId` | optionnel, 400 si inconnu |
| `vocationId` | UUID | `StkVocationId` | optionnel ; absent ⇒ vocation de code `VENTE` si elle existe |

Posé par le serveur : `PKey`, `Code` (compteur `AdmSequence` « Prospection », `AA/NNNNN`),
`DocDate` = maintenant, `StrEntityId` = `ComPreProspection`, `IsClosed = false`,
`IsOldCustomer = false`, `UserCreate` = nom de l'utilisateur connecté, `UserCreateDateTime`.
Jamais de prospect ni de commercial : c'est l'objet des étapes `creer-prospect` / `transformer`.

`IsClosed = false` plutôt que `NULL` comme OMRAN : les deux coexistent en base (2 884 / 937), le
filtre `ouvertes` de nos listes accepte les deux, et `false` évite qu'une ligne transformée plus
tard tombe hors de la vue de travail OMRAN (`IsClosed = 0`).

### Doublons — créer quand même, signaler

Décision utilisateur : **pas de refus**. OMRAN crée une ligne par soumission (6 des 7 leads Ferme
Bretonne étaient des re-soumissions). La réponse **signale** les pré-prospections existantes au
même contact, pour que l'écran puisse avertir.

Règle (plus robuste que l'égalité stricte d'OMRAN, qui ne se déclenche jamais dès qu'un des trois
champs est `NULL`) : même **nom** (sans casse ni espaces de bord) **et** même **téléphone**
normalisé (9 derniers chiffres, normalisation de la hotline) **et** même **email** (sans casse) —
un champ absent des deux côtés compte comme égal. Recherche sur toutes les `ComProspection`,
50 résultats au plus.

Réponse **201** `PreProspectionCreeeDto` :

```json
{ "preProspection": { …ProspectionDetailDto… },
  "doublons": [ { "id": "…", "code": "26/05940", "dateCreation": "2026-09-20T10:12:00" } ] }
```

`doublons` vide = aucun doublon. Codes : 201, 400 (nom manquant, référentiel inconnu, article hors
tranche), 403 (tranche hors périmètre).

---

## 4. Discriminant `StrEntity` — partout où l'heuristique était utilisée

| Lieu | Avant | Après |
|---|---|---|
| `GET /api/pre-prospections` (+ `/groupes`) | `CfgProspectId IS NULL AND CfgCommercialId IS NULL` | `StrEntity.Code = 'ComPreProspection'` |
| `GET /api/prospections` (+ `/groupes`) | complément | `StrEntityId IS NULL OR StrEntity.Code <> 'ComPreProspection'` |
| Dashboard : compteurs de prospections | complément | idem |
| `GET /api/dashboard/nouveaux-pre-prospects` | heuristique | `StrEntity` |
| Relances : champ `preProspection` | heuristique | `StrEntity` |

Les 4 lignes sans `StrEntityId` et les 4 `RntProspection` restent côté prospections, comme avant
(hors périmètre ici).

`POST /api/prospections/{id}/transformer` : bascule `StrEntityId` de `ComPreProspection` vers
`ComProspection` (et `IsClosed` NULL → `false`), comme OMRAN.

`POST /api/pre-prospections/{id}/creer-prospect` : inchangé (400 si un prospect est déjà lié) —
une pré-prospection avec prospect existe en base (261), le lien ne suffit pas à la transformer.

---

## Impact front

- **`POST /api/pre-prospections`** : nouveau. Réponse enveloppée `{ preProspection, doublons }`,
  pas un `ProspectionDetailDto` nu.
- **`GET /api/pre-prospections`** renvoie 847 lignes de plus (pré-prospections OMRAN ayant déjà un
  prospect ou un commercial) : `prospectNom` / `commercialNom` n'y sont **plus** toujours nuls, et le
  filtre `commercial` peut désormais renvoyer des lignes.
- **`GET /api/prospections`** renvoie ces 847 lignes de moins. `typeProspectionCode` n'y vaut plus
  jamais `ComPreProspection`.
- Compteurs du dashboard (prospections, prospections ouvertes) : baisse du même ordre.
- Après `transformer`, la ligne passe de la liste des pré-prospections à celle des prospections
  (avant : elle y passait dès qu'un prospect était lié).

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

**Périmètre commercial.** Seuls les doublons **visibles de l'appelant** sont renvoyés — même
prédicat que les listes (`filtreProspections` : projets affectés, entités actives). Nommer un
doublon hors périmètre révélerait qu'un contact existe sur un projet non affecté, ce que le reste
de l'API masque en 404. Test : `PreProspectionImportTest#doublonHorsPerimetre_nEstJamaisNomme`.

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

## 5. Import CSV des leads — `POST /api/pre-prospections/import` (+ `/apercu`)

Demande utilisateur du 23/09, même branche. Source : l'export CSV des leads (Meta / Leads Center).

```
Créé,Nom,Adresse e-mail,Source,Formulaire,Canal,Étape,Propriétaire,Étiquettes,Téléphone,Numéro de téléphone secondaire,Numéro WhatsApp
09/23/2026 1:45am,Imane Bennis,Imanebennis68@gmail.com,Payé,APF FORM 02*2026 - GISK,E-mail,À évaluer,Unassigned,,+212663434434,,
```

### Règle centrale : un fichier = une tranche

L'export ne porte aucune tranche exploitable. Le parcours est donc en deux temps :

1. **`POST /api/pre-prospections/import/apercu`** (multipart `fichier`) — extrait **toutes** les
   informations, ligne par ligne, et le sort de chacune. **N'écrit rien.** Propose
   `trancheSuggereeId` : la tranche dont le code est le premier mot du formulaire
   (`APF FORM 02*2026 - GISK` → `APF`), si tous les formulaires du fichier désignent la même.
2. L'écran **demande la tranche** (liste déroulante, pré-sélectionnée sur la suggestion).
3. **`POST /api/pre-prospections/import`** (multipart `fichier` + `trancheId` **obligatoire**) — le
   même fichier ; **toutes** les lignes sont rattachées à cette tranche. Un fichier dont les
   formulaires désignent plusieurs tranches revient avec un `avertissements` : le découper.

Deux appels sans état plutôt qu'un import « en attente » côté serveur : rien à nettoyer si
l'utilisateur abandonne, et le rapport de l'aperçu est exactement celui de l'import.

### Colonnes

| Colonne | Pré-prospection |
|---|---|
| Créé (`M/d/yyyy h:mma`, **mois en premier**, heure sur 12 ; ISO accepté) | `DocDate` = date de soumission du lead |
| Nom | `TempDescription` — **indispensable** (colonne absente : 400 ; vide : ligne en ERREUR) |
| Adresse e-mail | `TempEMail` |
| Téléphone → à défaut WhatsApp → à défaut secondaire | `TempTel`, tel qu'écrit |
| Formulaire, Source, Canal, Étape, Étiquettes, Propriétaire (sauf `Unassigned`), autres numéros | `Notes` |

Tolérances : colonnes reconnues par leur nom (casse/accents ignorés, ordre libre, colonnes
inconnues ignorées) ; séparateur `,` ou `;` ; UTF-8 avec/sans BOM, sinon Windows-1252 ; 5 000
lignes au plus. Pas de bibliothèque CSV (pas de modification du `pom.xml`) : lecteur RFC 4180
maison, `LecteurCsvLeads`.

### Valeurs posées

Chaque ligne passe par le même chemin que `POST /api/pre-prospections` (`ecrirePreProspection`) :
code `AA/NNNNN`, `ComPreProspection`, `IsClosed = false`, auteur, vocation VENTE. Référentiels
par défaut = **le motif des 2 700+ leads injectés par n8n depuis les mêmes formulaires** (mesuré
sur `yesRemsProd`) : Nature `1 A TRAITER`, source `FACEBOOK`, type `WEB` — chacun remplaçable
(`etapeId`, `origineId`, `typeContactId`, `origineSupportId`, `actionCommercialeId`). Un défaut
absent du référentiel reste vide.

### Doublons — le réimport ne crée rien deux fois

| Statut | Condition | Effet |
|---|---|---|
| `NOUVELLE` | aucun contact identique | créée |
| `DOUBLON` | même contact (règle §3), **autre date** — en base ou plus haut dans le fichier | créée et signalée (une ligne par soumission, comme OMRAN) |
| `DEJA_IMPORTEE` | même contact **et même date à la minute** | **ignorée** : c'est le même lead réimporté |
| `ERREUR` | nom vide, date vide/illisible, champ > 255 | ignorée, les autres passent |

Le couple *contact + date de soumission* identifie un lead de l'export : réimporter le fichier, ou
un export qui chevauche le précédent, est donc sans effet.

Périmètre : `DEJA_IMPORTEE` se juge sur **toute** la table (sinon un commercial restreint
recréerait un lead déjà importé sur un autre projet), mais n'y nomme que les lignes visibles —
hors périmètre, le message est « Lead déjà importé », sans code ni id. `DOUBLON`, lui, ne se
déclenche que sur des lignes visibles : hors périmètre, la ligne est `NOUVELLE`. Les deux routes
sont inscrites dans `PerimetreInterceptorInvarianteTest.ROUTES_SANS_ID_AUTORISEES` avec cette
justification. Une date `23/09/2026 03:00` (jour en
premier) est **refusée** plutôt que devinée : `09/10` serait ambigu.

Tout ou rien pour les erreurs de requête (tranche inconnue → 400, hors périmètre → 403 : aucune
ligne écrite).

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
- **Import CSV** (nouveau) : écran en deux temps — envoi du fichier à `/import/apercu`, tableau
  des lignes (statut par ligne, compteurs), **liste déroulante de tranche** pré-sélectionnée sur
  `trancheSuggereeId`, confirmation qui renvoie le même fichier à `/import` avec `trancheId`.
  Multipart, champ `fichier`. Les deux réponses ont la forme `RapportImportPreProspectionsDto`.

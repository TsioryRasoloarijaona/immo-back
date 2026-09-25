# FEATURE — Doublons de prospect et unicité prospect × projet

Branche : `feat/doublons-prospect-projet` (backend seul). Demande utilisateur du 25/09/2026.

Deux règles, toutes deux mesurées sur `yesRemsProd` avant d'être écrites :

1. **Identité du prospect** — avant de créer une fiche `CfgTier` (depuis une pré-prospection ou
   directement), retrouver les fiches existantes au même nom, téléphone ou email, et laisser
   l'utilisateur choisir : « c'est l'une d'elles » (on la rattache) ou « c'est un nouveau
   contact » (on crée quand même).
2. **Un prospect, un projet, une prospection ouverte** — un prospect ne peut pas avoir deux
   prospections ouvertes sur le même projet. Une prospection porte ensuite autant de dossiers
   de vente que nécessaire (5 prospections réelles en ont 2 à 6).

---

## 1. Ce que font OMRAN et les données

### Contrôle d'identité d'OMRAN (trace d'audit, 22 occurrences du 19 au 24/09)

```sql
select Count(*) from CfgTier WHERE (PKey <> @P1)
  AND (CfgTierTypeId IN ('07830036-…' /* CLI */, '3233d153-…' /* PRO */))
  AND ((REPLACE(FirstName,' ','') = @P2 AND REPLACE(LastName,' ','') = @P3)
    OR (REPLACE(FirstName,' ','') = @P4 AND REPLACE(LastName,' ','') = @P5))
  AND ((EMail = @P6) OR (EMail = @P7))
```

Prénom + nom sans espaces, **dans les deux ordres**, **et** l'email. Le téléphone n'est pas
regardé, et l'égalité stricte ne se déclenche jamais si l'email est vide (5 903 fiches sur
14 040 n'en ont pas). La règle du CRM est plus large : **une seule** correspondance suffit à
faire apparaître une fiche, et chaque fiche dit sur quels critères elle correspond.

### Population (`CfgTier` de type PRO / PRORNT / CLI / CLIRNT, 14 040 fiches)

| Colonne | Renseignée |
|---|---:|
| `Description` (nom affiché) | 14 040 |
| `Mobile` | 13 991 |
| `Phone` | 247 |
| `EMail` | 8 137 |

| Critère | Groupes | Fiches |
|---|---:|---:|
| Même nom (`Description`, sans casse ni espaces de bord) | 244 | 584 |
| Même téléphone (9 derniers chiffres de `Mobile`/`Phone`) | 491 | 1 071 |
| Même email (sans casse) | 170 | 644 |
| Même nom **et** même téléphone | 6 | 12 |
| Même nom, téléphones différents | 237 | 570 |

Le même nom avec un contact différent est le cas courant : on **affiche**, on ne refuse pas.

**Valeurs bidon à ignorer** (sinon elles font apparaître 17 fiches à chaque saisie) :
téléphones `000000000` (17 fiches), `111111111` (17), `666666666` (5) ; emails `x@hotmail.fr`
(154), `-` (26), `nd` (21), `x@hotmail.com` (20), `.` (17), `x@gmail.com` (14). Règle : un
téléphone de moins de 9 chiffres ou dont les 9 chiffres sont identiques n'est pas un critère ;
un email sans `@` non plus. `x@…` reste un critère (il a un `@`) — 154 fiches, mais c'est un
vrai email de complaisance saisi par les commerciaux, pas une valeur technique.

Format réel des mobiles : `6 14 19 71 95` (sans le zéro, espacé, indicatif dans
`MobileIndicatif`). Normalisation = celle de la hotline, `PhoneNumberUtils.normalize` (9
derniers chiffres).

### Prospect × projet (`ComProspection` qualifiées, hors `ComPreProspection`)

| Mesure | N |
|---|---:|
| Prospections qualifiées | 14 138 (toutes avec prospect) |
| … avec prospect **et** projet | 14 113 |
| Couples prospect × projet distincts | 13 884 |
| Couples répétés | 217 (209 × 2, 5 × 3, 2 × 4, 1 × 5) |
| Couples répétés dont la précédente est **fermée** | 216 |
| Couples répétés avec **2 prospections ouvertes** en même temps | **1** |
| Couples répétés sur des tranches différentes du même projet | 38 |
| Prospects avec plusieurs projets | 250 |
| Dossiers par prospection | 369 × 1, 5 × 2 à 6 ; 3 dossiers sans prospection |

Les 216 sont des re-prospections après clôture, typiquement un lead n8n (`Code = '50'`,
fermé, `1 A TRAITER`) suivi d'une vraie prospection. Le seul couple à deux prospections
ouvertes est Yassine MOUNIR sur APF : `26/05893` (22/07, Othman DIOURI) puis `26/05941` (26/08,
Khadija ELKORRI), toutes deux `3 QUALIFIÉ`, saisies dans OMRAN par deux commerciaux. OMRAN ne
fait **aucun** contrôle ici (aucun `SELECT COUNT` sur `ComProspection` par `CfgProspectId` dans
la trace). C'est exactement le cas que la règle 2 empêche.

54 pré-prospections portent déjà un prospect qui a une prospection sur le même projet (22 en
2025, 32 en 2026). Elles ne sont pas modifiées ; leur transformation tombera en 409 si l'autre
est ouverte — c'est voulu, la bonne action est d'ouvrir l'existante.

Note hors périmètre : `26/05941` existe **deux fois** dans `yesRemsProd` (la seconde sans
`UserCreate`, 20/09 21:35). Le compteur `AdmSequence` de cette base est en retard sur ses
lignes ; à surveiller sur `yesRemsDev`.

---

## 2. Règle 1 — identité du prospect

### `GET /api/prospects/doublons`

Paramètres, tous optionnels mais **au moins un** renseigné (400 sinon) : `nom`, `prenom`,
`raisonSociale`, `telephone`, `mobile`, `email`.

Réponse 200 : liste (50 au plus) de `ProspectDoublonDto`, les fiches correspondant sur le plus
de critères d'abord, puis les plus récentes :

```json
[{ "id": "…", "code": "26/14428", "nom": "Badr ARH", "prenom": "Badr", "nomFamille": "ARH",
   "raisonSociale": null, "telephone": null, "mobile": "6 65 68 09 64",
   "email": "hei.tsiory@gmail.com", "ville": null, "type": "PRO",
   "dateCreation": "2026-08-24T10:12:00", "criteres": ["NOM", "TELEPHONE"] }]
```

Critères (`CritereDoublon`) :

| Critère | Règle | Côté fiche |
|---|---|---|
| `NOM` | `prenom + nom` (ou `raisonSociale` seule) en minuscules, **sans aucun espace** | égal à `FirstName+LastName`, à `LastName+FirstName`, à `Description` ou à `SocialReason`, normalisés pareil |
| `TELEPHONE` | 9 derniers chiffres de `telephone` **ou** de `mobile` ; ignoré si < 9 chiffres ou 9 chiffres identiques | `Phone`, `Phone1`, `Phone2`, `Mobile` normalisés pareil |
| `EMAIL` | minuscules, sans espaces de bord ; ignoré sans `@` | `EMail` normalisé pareil |

Seules les fiches de type PRO / PRORNT / CLI / CLIRNT sont candidates (comme OMRAN et comme
`GET /api/prospects`). Pas de périmètre commercial : les prospects n'y sont pas soumis.

### `POST /api/prospects` et `POST /api/pre-prospections/{id}/creer-prospect`

`CreerProspectRequest` gagne `ignorerDoublons` (Boolean, défaut false). Avant toute écriture, la
recherche ci-dessus est faite avec les champs du corps. S'il y a des candidats et que
`ignorerDoublons` n'est pas `true` → **409** `ProblemDetail` :

```json
{ "status": 409, "detail": "2 fiches prospect ou client portent le même nom, téléphone ou email",
  "doublons": [ …ProspectDoublonDto… ] }
```

Le front affiche la liste : « est-ce l'une de ces personnes ? ». Oui → `lier-prospect` (ci-
dessous) ou, pour `POST /api/prospects`, simplement ouvrir la fiche. Non → renvoyer le même corps
avec `ignorerDoublons: true` → 201 comme avant.

### `POST /api/pre-prospections/{id}/lier-prospect`

Corps `{ "prospectId": "…" }`. Pose un `CfgTier` **existant** comme prospect de la
pré-prospection, sans rien créer — le pendant de `creer-prospect`, qui n'existait pas.

- 200 `ProspectionDetailDto` ; historique « Prospect existant rattaché : X ».
- 400 : `prospectId` absent ou inconnu ; pré-prospection ayant déjà un prospect (comme
  `creer-prospect`) ; tiers d'un type autre que prospect/client.
- 404 : pré-prospection inconnue ou hors périmètre.
- **409** : la pré-prospection porte une tranche et le prospect a déjà une prospection ouverte sur
  ce projet (règle 2) — corps `prospectionExistante`.

---

## 3. Règle 2 — une prospection ouverte par prospect × projet

**Définition.** Pour un prospect P et un projet J, il existe au plus une `ComProspection`
qualifiée (pas `ComPreProspection`) **ouverte** (`IsClosed` = 0 ou NULL — une ligne NULL n'est
pas archivée, voir `FEATURE-desarchiver-prospection.md`) dont la tranche appartient à J. Une
prospection fermée ne bloque pas (les 216 re-prospections réelles). Le projet est celui de la
tranche : deux tranches d'un même projet comptent comme le même projet (38 des 217 couples).

**Où elle s'applique** — tout point où le couple (prospect, projet) se forme ou change :

| Endpoint | Couple contrôlé | Exclusion |
|---|---|---|
| `POST /api/prospections` | `prospectId` × projet de `trancheId` | — |
| `POST /api/prospections/{id}/transformer` | prospect lié × projet de la tranche effective (`trancheId` fourni, sinon celle déjà posée) | la prospection elle-même |
| `PATCH /api/prospections/{id}/tranche-principale` | prospect lié × projet de la nouvelle tranche, **seulement si le projet change** | la prospection elle-même |
| `POST /api/pre-prospections/{id}/lier-prospect` | `prospectId` × projet de la tranche déjà portée (si elle en porte une) | la pré-prospection elle-même |

Pas de contrôle sur `creer-prospect` (la fiche est neuve, elle n'a aucune prospection), ni sur
`PATCH .../desarchiver` (rouvrir une prospection alors qu'une autre est ouverte est un cas
d'archivage, pas de création — et la donnée ne le montre pas).

**Refus : 409** `ProblemDetail` :

```json
{ "status": 409,
  "detail": "Le prospect Yassine MOUNIR a déjà une prospection ouverte sur le projet APF (26/05893) : ouvrez-la plutôt que d'en créer une seconde.",
  "prospectionExistante": { "id": "…", "code": "26/05893", "dateDocument": "2026-07-22T14:39:33",
     "etapeLibelle": "3 QUALIFIÉ", "commercialNom": "Othman DIOURI",
     "trancheCode": "APF", "projetCode": "APF" } }
```

Aucune fuite de périmètre : l'appelant écrit sur ce projet (403 sinon, contrôle déjà en place),
la prospection nommée est donc sur un projet qu'il voit.

---

## 4. Fichiers

- `dto/prospect/ProspectDoublonDto.java`, `CritereDoublon.java` ; `CreerProspectRequest.ignorerDoublons`.
- `dto/prospection/LierProspectRequest.java`, `ProspectionExistanteDto.java`.
- `exceptions/DoublonsProspectException.java`, `ProspectionExistanteException.java` (409, propriétés
  `doublons` / `prospectionExistante` posées par `GlobalExceptionHandler`).
- `repositories/cfg/CfgTierRepository.trouverDoublons(...)` ;
  `repositories/com/ComProspectionRepository.trouverOuvertesMemeProspectMemeProjet(...)`.
- `services/prospect/ProspectService.rechercherDoublons(...)`, `createEntity` (409) ;
  `services/prospection/ProspectionService.lierProspect(...)`, `verifierUniciteProspectProjet(...)`.
- Contrôleurs : `ProspectController` (`GET /doublons`), `PreProspectionController` (`lier-prospect`),
  springdoc sur les quatre endpoints touchés par la règle 2.
- Tests : `ProspectDoublonsTest`, `ProspectionUniciteProjetTest`.

---

## 5. Impact front

Additif sauf un point : **`POST /api/prospects` et `creer-prospect` peuvent désormais renvoyer
409** là où ils renvoyaient 201. Un front qui ne gère pas le 409 verra la création « échouer »
sur tout homonyme.

- Formulaire prospect (depuis une pré-prospection ou direct) : à la soumission, sur 409, afficher
  `doublons` avec leurs `criteres` ; bouton « C'est cette personne » → `lier-prospect`
  (pré-prospection) ou ouverture de la fiche ; bouton « Nouveau contact » → renvoyer avec
  `ignorerDoublons: true`. Optionnel : appeler `GET /api/prospects/doublons` à la volée pendant la
  saisie pour prévenir avant la soumission.
- Création / transformation / changement de tranche : sur 409 avec `prospectionExistante`,
  proposer d'ouvrir `GET /api/prospections/{id}` — c'est là qu'on ajoute un dossier.

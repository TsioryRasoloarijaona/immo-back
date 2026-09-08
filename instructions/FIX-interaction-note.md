# FIX — La note d'une interaction n'est ni lue ni écrite au bon endroit

> Statut : spécifié le 08/09/2026. Porte sur `immo-crm` uniquement.
> **Contient un changement de contrat pour le frontend — voir §6.**

## 1. Le symptôme

`GET /api/prospections/{id}/interactions` renvoie bien un champ `description`, mais il ne
contient jamais ce que le commercial a écrit. Le texte de l'interaction n'est exposé nulle part.

## 2. Ce que la base dit (`yesRemsProd`, 08/09/2026, 46 546 interactions de prospection)

| Colonne | Remplie | Longueur min–max | Moyenne |
|---|---:|---:|---:|
| `ComActionMessage.Description` | **46 546 / 46 546** (100 %) | **22 – 41** | 31 |
| `ComActionMessage.AlertMessage` | 44 981 (96,6 %) | **1 – 944** | 43 |

Une colonne remplie à 100 % dont la longueur tient dans une fourchette de 19 caractères n'est
pas de la saisie humaine. Le contenu le confirme :

```
Description                       │ AlertMessage
07/09/2026 17:13 Khadija ELKORRI  │ la cliente est interesse par app 2 chambres et salon…
07/09/2026 12:20 Khadija ELKORRI  │ pour son fils qui travaille a cfc  rdv mercredi 10h
04/09/2026 11:46 Khadija ELKORRI  │ brochures envoyes   a rappeller
```

**`Description` est un libellé généré** : date, heure, auteur. Sur les 46 546 lignes,
**46 546 suivent le motif `JJ/MM/AAAA HH:MM <auteur>` — zéro exception.** Et son contenu est
déjà exposé séparément par le DTO (`date`, `employeNom`) : c'est une redondance, pas une
information.

**Le texte réel est dans `AlertMessage`**, mappé sur l'entité mais jamais exposé.

### `AlertMessage` malgré son nom n'appartient pas aux relances

| | avec relance | sans relance |
|---|---:|---:|
| **avec note** | 38 479 | **6 502** |
| **sans note** | 135 | 1 430 |

6 502 interactions portent un texte sans aucune `AlertValidityDate`, contre 135 relances sans
texte. C'est donc bien la note de l'interaction, pas le message d'une alerte.

### Composition exacte du libellé

Vérifiée ligne à ligne : `Description` = `DocDate` au format `dd/MM/yyyy HH:mm`, un espace,
puis `AdmUser.Description`. Les colonnes `UserName` et `UserCreate` portent le même nom
d'auteur. `CfgEmployeeId` est vide 4 fois sur 5 — **l'auteur d'une interaction est `AdmUserId`**,
comme déjà établi pour les KPI commerciaux.

## 3. Le second défaut, trouvé en cherchant le premier

`POST /api/prospections/{id}/interactions` écrit `request.description()` dans
`message.setDescription(...)` — **c'est-à-dire dans la colonne du libellé généré** — et ne
renseigne jamais `AlertMessage`. Il ne renseigne pas non plus l'auteur (`AdmUser`,
`CfgEmployee`, `UserName`, `UserCreate` restent nuls).

Conséquences si l'endpoint est utilisé tel quel :

1. la note serait invisible dans l'application de bureau, qui lit `AlertMessage` ;
2. elle serait invisible dans le champ `note` ajouté par ce correctif ;
3. le libellé afficherait du texte libre là où toute l'application affiche « date + auteur » ;
4. l'interaction n'aurait aucun auteur, donc `employeNom` nul et un KPI faussé.

**Aucune ligne n'est concernée** : le motif est respecté à 100 %, donc aucune interaction n'a
encore été créée par cette API. Le défaut est **latent**, il n'y a aucune donnée à réparer —
mais il se déclenche au premier usage réel.

## 4. Correctif — lecture

`InteractionDto` gagne un champ **`note`**, alimenté par `ComActionMessage.AlertMessage`.

`description` **est conservé** : le retirer casserait tout consommateur existant pour un gain
nul. Son `@Schema` doit en revanche dire ce qu'il est réellement — un libellé généré, redondant
avec `date` et `employeNom` — et orienter vers `note`.

## 5. Correctif — écriture

`addInteraction` aligne ce que nous écrivons sur ce qu'écrit l'application de bureau :

| Colonne | Valeur |
|---|---|
| `AlertMessage` | le texte fourni par l'appelant |
| `Description` | **généré** : `dd/MM/yyyy HH:mm ` + nom de l'auteur |
| `AdmUser` | le compte authentifié (l'auteur) |
| `CfgEmployee` | sa fiche employé, quand le jeton la porte |
| `UserName`, `UserCreate` | le nom de l'auteur |
| `UserCreateDateTime` | l'instant de création |

**R1 — le libellé est toujours généré, jamais fourni.** L'appelant ne peut pas l'imposer :
c'est ce qui garantit que nos interactions restent indiscernables de celles du légataire.

**R2 — l'auteur vient du jeton, jamais du corps de la requête.** Une interaction dont
l'appelant choisirait l'auteur fausserait les KPI de performance commerciale.

**R3 — sans utilisateur authentifié**, le libellé se réduit à la date et l'auteur reste nul,
plutôt que d'échouer : certains contextes (génération de documents, tests) n'ont pas de
principal, et une interaction sans auteur reste préférable à une exception.

**R4 — `AdmEvent.Subject`** (créé pour un rendez-vous) continue de porter le texte de
l'interaction : c'est un sujet d'agenda, la note y a du sens.

## 6. Impact front (`crm-prototype` — autre dépôt, autre PR)

> Ce dépôt ne touche pas le frontend. Cette section et les annotations springdoc sont le canal.

**Champ ajouté, additif** — `GET /api/prospections/{id}/interactions` et les interactions
embarquées par `GET /api/hotline/recherche` :

| Champ | Type | Sens |
|---|---|---|
| `note` | `string` | Le texte saisi par le commercial. `null` si absent (3,4 % des lignes). **C'est ce qu'il faut afficher dans le journal.** |

**`description` ne doit plus être affiché comme du contenu.** Il vaut `07/09/2026 17:13 Khadija
ELKORRI` sur 100 % des lignes réelles — soit exactement `date` + `employeNom`, déjà présents.
L'afficher revient à répéter trois fois la même information.

**Aucun changement de requête pour la création** : `CreerInteractionRequest.description` reste
le champ d'entrée et reste obligatoire ; seule sa destination en base change. Le nom du champ
est conservé pour ne rien casser, mais il alimente désormais `note` — l'asymétrie est assumée
et documentée plutôt que corrigée par un renommage qui obligerait le front à bouger.

## 7. Tests

| Cas | Attendu |
|---|---|
| Lecture d'une interaction portant une note | `note` renvoyée telle quelle |
| Lecture d'une interaction sans note | `note` à `null`, pas de plantage |
| Création puis relecture | la note écrite ressort dans `note` |
| Format du libellé généré | `dd/MM/yyyy HH:mm ` + nom, et **pas** le texte de l'appelant |
| Auteur | `AdmUser` renseigné depuis le jeton, `employeNom` non nul |
| Création sans principal authentifié (R3) | réussit, auteur nul, libellé réduit à la date |

# PreProspectionController

Leads bruts non encore qualifiés : ComProspection de type légataire `ComPreProspection` (`StrEntity`), le discriminant d'OMRAN lui-même — une pré-prospection peut donc déjà porter un prospect ou un commercial tant qu'elle n'a pas été transformée. Même forme que /api/prospections.

Collection soumise au périmètre commercial : sur **toutes** les routes portant un identifiant, une ressource hors du périmètre de l'appelant renvoie 404 (jamais 403 — un 403 confirmerait son existence), et un identifiant qui n'est pas un UUID valide renvoie 404 lui aussi, plutôt que le 400 habituel : il est refusé avant d'atteindre le contrôleur. Voir `perimetreRestreint` sur GET /api/mon-compte.

**Entités juridiques en exploitation.** Ne sont renvoyées que les pré-prospections rattachées à une entité juridique active (`CfgCompany.IsActive`) — aujourd'hui ADMOD et GHRIZA. Restriction **globale et permanente**, distincte du périmètre commercial : elle s'applique à tout appelant, rôles exemptés compris. Une pré-prospection **sans tranche** n'est rattachée à aucune entité et reste donc visible — le lead n'est pas perdu.

## findAll

Mêmes filtres que GET /api/prospections (étape, origine, commercial, tranche(s), plage de dates,
recherche libre, ouvertes) appliqués aux leads non encore qualifiés. Voir
instructions/filtres-avances.md point 5.

**Périmètre commercial** : si l'appelant est restreint, cette liste ne contient que les
pré-prospections de ses projets affectés, scopées par le projet de leur tranche visée. Les
pré-prospections sans tranche restent visibles de tous. Le filtre `commercial` s'applique : une
pré-prospection saisie dans OMRAN peut déjà porter un commercial (~780 en base réelle).

### param ouvertes

Filtre d'archivage, **à trois états**. Absent : toutes les pré-prospections. `true` : seulement les
ouvertes (`fermee` à `false` ou `null`). `false` : seulement les archivées (`fermee` à `true`).
Avant le 28/09/2026, `false` était ignoré et renvoyait toute la liste.

### param groupeValeur

Valeur du groupe à déplier — la `cle` renvoyée telle quelle par `/groupes`. Omise avec
`groupeColonne` fourni : le groupe sans valeur.

## grouper

Même mécanique que `GET /api/prospections/groupes`, appliquée au périmètre des pré-prospections
(type légataire `ComPreProspection`).

**Les effectifs portent sur tout le périmètre filtré, pas sur une page.** Pour ouvrir un groupe :
`GET /api/pre-prospections?groupeColonne=<groupBy>&groupeValeur=<cle>`.

`prospectNom` et `commercialNom` sont nuls sur la plupart des lignes (leads non encore qualifiés) :
grouper dessus donne surtout le groupe sans valeur.

### param agregats

Agrégats à calculer, répétable ou séparés par des virgules.

Deux formes. Un **nom nu** (`prix`) vaut une **somme** et revient sous ce nom nu — c'est le
comportement historique, inchangé. Un nom **qualifié** (`prix:AVG`) applique la fonction nommée et
revient sous le jeton complet `prix:AVG`. Les deux formes peuvent coexister dans le même appel.

Fonctions : `SUM`, `AVG`, `MIN`, `MAX`, `COUNT` — mais **pas sur toutes les colonnes**. Sommer une
date n'a pas de sens, et une colonne calculée par différence n'a pas d'extremum exprimable. La liste
autorisée par colonne est publiée par le catalogue (`fonctions`) : le menu Σ doit s'y piloter, pas
sur une liste en dur. Une fonction refusée rend 400 avec les fonctions acceptées par cette
colonne-là.

`AVG` est calculée à partir de la somme et de l'effectif renseigné, en décimal exact et arrondie au
centime — jamais en virgule flottante.

Absent : seul l'effectif de chaque groupe est renvoyé.

### param inclureSansValeur

Inclure le groupe des lignes dont la colonne groupée est vide. **Faux par défaut** : l'écran veut
les valeurs qui existent, et sur une colonne peu renseignée le groupe vide écraserait tous les
autres. Passer `true` pour le récupérer — la somme des effectifs retombe alors sur le
`totalElements` de la liste non groupée.

### param ouvertes

Filtre d'archivage à trois états, **identique à celui de la liste** : absent = tout, `true` =
ouvertes, `false` = archivées.

## creer

Équivalent de l'écran « Pré-prospection » d'OMRAN, dont la trace d'audit fixe la séquence : code
tiré du compteur `AdmSequence` « Prospection » (format `AA/NNNNN`, **le même compteur que les
prospections**), type de document `ComPreProspection`, contact saisi en champs libres
(`TempDescription`, `TempTel`, `TempEMail`) — **aucune fiche prospect n'est créée**.

Seul `nom` est obligatoire. Posés par le serveur : code, date du document (maintenant), `IsClosed =
false`, auteur (utilisateur connecté), vocation `VENTE` si aucune n'est fournie. Jamais de prospect
ni de commercial : c'est l'objet de `POST /api/pre-prospections/{id}/creer-prospect` puis `POST
/api/prospections/{id}/transformer`.

**Doublons signalés, jamais refusés.** La réponse liste les prospections existantes au même contact
(même nom, même téléphone sur ses 9 derniers chiffres, même email — un champ absent des deux côtés
compte comme égal). OMRAN enregistre lui aussi une ligne par soumission ; à l'écran d'avertir. Seuls
les doublons **visibles de l'appelant** (périmètre commercial) sont listés : un contact hors
périmètre n'est jamais révélé.

La ligne créée apparaît aussitôt dans `GET /api/pre-prospections` et dans le logiciel de bureau.

### reponse 400

`nom` absent ou blanc ; champ trop long (nom/telephone/email 255, notes 1 000) ; référentiel inconnu
(tranche, article, étape, origine, support, type, action, vocation) ; `articleId` sans `trancheId`
ou hors de la tranche.

## apercuImport

Première étape de l'import : envoie le fichier, reçoit **tout ce qui en a été extrait**, ligne par
ligne, avec le sort que l'import réservera à chacune. **Rien n'est écrit.**

**Un fichier = une tranche.** L'export ne dit pas à quelle tranche les leads appartiennent : après
l'aperçu, l'écran **demande la tranche** (liste déroulante), et cette tranche unique vaudra pour
toutes les lignes du fichier. Pour la pré-sélectionner, l'aperçu propose `trancheSuggereeId` : la
tranche dont le code est le premier mot du formulaire (`APF FORM 02*2026 - GISK` → `APF`), quand
tous les formulaires du fichier désignent la même. Un fichier qui mêle plusieurs tranches revient
avec un `avertissements` : le découper.

Étape 2 : `POST /api/pre-prospections/import` avec **le même fichier** et la tranche choisie.

**Format attendu** — l'export CSV des leads (Meta / Leads Center), en-tête : `Créé, Nom, Adresse
e-mail, Source, Formulaire, Canal, Étape, Propriétaire, Étiquettes, Téléphone, Numéro de téléphone
secondaire, Numéro WhatsApp`.
- Colonnes reconnues **par leur nom** (casse et accents ignorés), dans n'importe quel ordre ; les
colonnes inconnues sont ignorées. Seule `Nom` est indispensable (400 sinon).
- Séparateur `,` ou `;` (un fichier réenregistré par Excel en français), déduit de l'en-tête.
Encodage UTF-8 (avec ou sans BOM), à défaut Windows-1252.
- 5 000 lignes au plus (400 au-delà) ; lignes vides ignorées.

**Ce que devient chaque colonne**
| Colonne | Pré-prospection |
|---|---|
| Créé (`09/23/2026 1:45am`, mois en premier) | date du document `DocDate` — la date de soumission
du lead |
| Nom | `TempDescription` |
| Adresse e-mail | `TempEMail` |
| Téléphone (à défaut WhatsApp, à défaut secondaire) | `TempTel`, tel qu'écrit |
| Formulaire, Source, Canal, Étape, Étiquettes, Propriétaire (sauf « Unassigned »), autres numéros |
recopiés dans `Notes` |

**Sort de chaque ligne** (`statut`) :
- `NOUVELLE` — créée.
- `DOUBLON` — même contact (nom, téléphone sur ses 9 derniers chiffres, email ; casse ignorée)
qu'une prospection existante ou qu'une ligne plus haut, mais **autre date** : c'est une nouvelle
soumission, **créée quand même** et signalée.
- `DEJA_IMPORTEE` — même contact **et même date** (à la minute) : le même lead réimporté,
**ignoré**. Réimporter un fichier, ou un export qui chevauche le précédent, ne crée donc rien deux
fois.

**Périmètre commercial** : seuls les doublons visibles de l'appelant sont nommés (`doublonIds`,
codes dans `message`). Un lead déjà importé sur un projet hors périmètre est bien ignoré, avec le
message « Lead déjà importé » et rien d'autre ; un simple homonyme hors périmètre ne fait pas un
`DOUBLON`.
- `ERREUR` — nom vide, date vide ou illisible, champ trop long : **ignorée**, les autres lignes
passent.

## importer

Crée une pré-prospection par ligne `NOUVELLE` ou `DOUBLON` du fichier, **toutes rattachées à
`trancheId`** — la tranche choisie par l'utilisateur après l'aperçu (`POST
/api/pre-prospections/import/apercu`). **Une seule tranche par fichier** : un fichier qui en mêle
plusieurs doit être découpé avant l'import.

Chaque ligne créée est une pré-prospection complète, exactement comme `POST /api/pre-prospections` :
code `AA/NNNNN` du compteur « Prospection », type `ComPreProspection`, `IsClosed = false`, auteur =
utilisateur connecté, vocation `VENTE`. Référentiels posés par défaut — le motif des leads déjà
injectés depuis ces formulaires — chacun remplaçable par son paramètre : Nature `1 A TRAITER`
(`etapeId`), source `FACEBOOK` (`origineId`), type `WEB` (`typeContactId`). Un défaut absent du
référentiel est laissé vide.

**Tout ou rien** pour les erreurs de requête (tranche inconnue ou hors périmètre : aucune ligne
écrite). Les lignes en `ERREUR` n'empêchent pas les autres. Le rapport renvoyé a la forme de
l'aperçu, avec `importe = true` et l'identifiant et le code de chaque pré-prospection créée.

**Format attendu** — l'export CSV des leads (Meta / Leads Center), en-tête : `Créé, Nom, Adresse
e-mail, Source, Formulaire, Canal, Étape, Propriétaire, Étiquettes, Téléphone, Numéro de téléphone
secondaire, Numéro WhatsApp`.
- Colonnes reconnues **par leur nom** (casse et accents ignorés), dans n'importe quel ordre ; les
colonnes inconnues sont ignorées. Seule `Nom` est indispensable (400 sinon).
- Séparateur `,` ou `;` (un fichier réenregistré par Excel en français), déduit de l'en-tête.
Encodage UTF-8 (avec ou sans BOM), à défaut Windows-1252.
- 5 000 lignes au plus (400 au-delà) ; lignes vides ignorées.

**Ce que devient chaque colonne**
| Colonne | Pré-prospection |
|---|---|
| Créé (`09/23/2026 1:45am`, mois en premier) | date du document `DocDate` — la date de soumission
du lead |
| Nom | `TempDescription` |
| Adresse e-mail | `TempEMail` |
| Téléphone (à défaut WhatsApp, à défaut secondaire) | `TempTel`, tel qu'écrit |
| Formulaire, Source, Canal, Étape, Étiquettes, Propriétaire (sauf « Unassigned »), autres numéros |
recopiés dans `Notes` |

**Sort de chaque ligne** (`statut`) :
- `NOUVELLE` — créée.
- `DOUBLON` — même contact (nom, téléphone sur ses 9 derniers chiffres, email ; casse ignorée)
qu'une prospection existante ou qu'une ligne plus haut, mais **autre date** : c'est une nouvelle
soumission, **créée quand même** et signalée.
- `DEJA_IMPORTEE` — même contact **et même date** (à la minute) : le même lead réimporté,
**ignoré**. Réimporter un fichier, ou un export qui chevauche le précédent, ne crée donc rien deux
fois.

**Périmètre commercial** : seuls les doublons visibles de l'appelant sont nommés (`doublonIds`,
codes dans `message`). Un lead déjà importé sur un projet hors périmètre est bien ignoré, avec le
message « Lead déjà importé » et rien d'autre ; un simple homonyme hors périmètre ne fait pas un
`DOUBLON`.
- `ERREUR` — nom vide, date vide ou illisible, champ trop long : **ignorée**, les autres lignes
passent.

### param trancheId

Tranche (CfgTranche.id) choisie dans la liste déroulante. **Obligatoire** : toutes les lignes du
fichier y sont rattachées.

### reponse 400

`trancheId` absent ou inconnu ; fichier absent, vide, sans colonne `Nom` ou de plus de 5 000 lignes
; référentiel fourni inconnu.

## creerProspect

Première étape de qualification d'un lead brut : crée un `CfgTier` à partir des informations
fournies et le pose comme prospect de la pré-prospection. Valide uniquement sur une pré-prospection
(aucun prospect déjà lié) — 400 sinon. L'étape suivante est `POST
/api/prospections/{id}/transformer`, qui assigne le commercial et achève la transformation en
prospection.

**Contrôle de doublon d'identité.** Avant toute écriture, les fiches prospect/client existantes au
même nom, téléphone ou email sont recherchées (mêmes règles que `GET /api/prospects/doublons`). S'il
y en a et que `ignorerDoublons` n'est pas `true`, la création est refusée en **409** avec la liste
`doublons` : l'écran demande « est-ce l'une de ces personnes ? ». Oui → `POST
/api/pre-prospections/{id}/lier-prospect` avec son id. Non → renvoyer le même corps avec
`ignorerDoublons: true`. Voir `FEATURE-doublons-prospect-projet.md` §2.

**Périmètre commercial** : une pré-prospection hors du périmètre de l'appelant renvoie 404, comme
partout ailleurs.

### reponse 400

Cette prospection a déjà un prospect lié ; ou `nom`/`raisonSociale` absents, numéro sans indicatif,
référence inconnue (mêmes règles que `POST /api/prospects`).

### reponse 404

Aucune pré-prospection avec cet id, ou pré-prospection hors du périmètre commercial de l'appelant —
les deux cas sont volontairement indiscernables. Également renvoyé (plutôt qu'un 400) lorsque
l'identifiant fourni n'est pas un UUID valide.

### reponse 409

Des fiches prospect/client existantes portent le même nom, téléphone ou email et `ignorerDoublons`
n'est pas `true`. Corps : `doublons`, liste de `ProspectDoublonDto` avec leurs `criteres`.

## lierProspect

Le pendant de `creer-prospect` pour la réponse « c'est l'une de ces personnes » à la liste de
doublons : pose un `CfgTier` **existant** comme prospect de la pré-prospection, sans rien créer.
Valide uniquement sur une pré-prospection sans prospect lié (400 sinon) ; seuls les tiers de type
prospect ou client (`PRO`, `PRORNT`, `CLI`, `CLIRNT`) sont acceptés — le même sous-ensemble que `GET
/api/prospects/doublons`. L'étape suivante reste `POST /api/prospections/{id}/transformer`.

**Un prospect, un projet, une prospection.** Si la pré-prospection porte déjà une tranche et que ce
prospect a une prospection **ouverte** sur le projet de cette tranche, le rattachement est refusé en
**409** : la bonne action est d'ouvrir `prospectionExistante` et d'y ajouter un dossier. Voir
`FEATURE-doublons-prospect-projet.md` §3.

**Périmètre commercial** : une pré-prospection hors du périmètre de l'appelant renvoie 404, comme
partout ailleurs.

### reponse 200

Prospect rattaché ; la pré-prospection sous la forme de `GET /api/prospections/{id}`. Historique : «
Prospect existant rattaché : … ».

### reponse 400

`prospectId` manquant ou introuvable ; cette prospection a déjà un prospect lié ; le tiers n'est pas
une fiche prospect ou client.

### reponse 409

Le prospect a déjà une prospection **ouverte** sur le projet de la tranche portée par la
pré-prospection. Corps : `prospectionExistante`.

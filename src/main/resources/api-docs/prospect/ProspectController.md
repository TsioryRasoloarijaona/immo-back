# ProspectController

## findAll

### param colonnes

Colonnes a serialiser, repetable ou separees par des virgules. Absent : les 10 colonnes historiques.
Catalogue et liste blanche : `GET /api/referentiels/colonnes/prospects`. Un nom hors catalogue rend
400 avec la liste des noms valides. `id` est toujours servi.

## exporter

Classeur Excel (`.xlsx`) de la liste courante : mêmes filtres que `GET /api/prospects` (`search`, `type`), sans pagination. Une feuille « DONNEES » ; en-têtes = libellés du catalogue de colonnes, dans l'ordre de `colonnes` ; dates, nombres et booléens en cellules typées. Auteur du classeur = utilisateur connecté, titre « Prospects », fichier `prospects_<aaaaMMjj_HHmmss>.xlsx`. Plafond : 20 000 lignes. Les filtres de colonne de la grille (appliqués côté navigateur à la page chargée) ne sont pas transmis.

### param format

Seule valeur acceptée : `xlsx`.

### param colonnes

Colonnes exportées, dans l'ordre voulu (celui de la grille). Absent : les colonnes par défaut de la liste.

### reponse 400

`format` autre que `xlsx`, ou colonne inconnue.

## doublons

Le contrôle d'identité à faire **avant** de créer un prospect — depuis une pré-prospection ou
directement — pour éviter les doublons. Renvoie les fiches de type prospect ou client (`PRO`,
`PRORNT`, `CLI`, `CLIRNT`) qui correspondent à l'identité saisie sur **au moins un** critère,
chacune avec ses `criteres` :

- `NOM` : `prenom` + `nom` (ou `raisonSociale` seule) en minuscules et sans aucun espace, comparé à
prénom+nom, nom+prénom, `Description` et `SocialReason` de la fiche normalisés pareil — la règle
d'OMRAN, qui rend « El Mehdi LAHLOU » et « El MEHDI LAHLOU » égaux ;
- `TELEPHONE` : 9 derniers chiffres de `telephone` ou de `mobile`, contre les quatre numéros de la
fiche, quel que soit le format stocké ;
- `EMAIL` : sans casse ni espaces de bord.

Les valeurs bidon ne sont pas des critères : téléphone de moins de 9 chiffres ou à 9 chiffres
identiques (`000000000`), email sans `@` (`-`, `nd`). **Un homonyme au contact différent est le cas
courant** (237 groupes sur 244 dans les données réelles) : la liste est là pour que l'utilisateur
tranche, pas pour refuser. Tri : le plus de critères d'abord, puis les plus récentes ; 50 au plus.
**Vide = aucune fiche ne correspond.** Pas de périmètre commercial : les prospects n'y sont pas
soumis.

Le même contrôle est appliqué par `POST /api/prospects` et `POST
/api/pre-prospections/{id}/creer-prospect` (409 sauf `ignorerDoublons: true`). Voir
`FEATURE-doublons-prospect-projet.md` §2.

## create

`nom` ou `raisonSociale` est requis ; tout le reste est optionnel.

**Un numéro de téléphone ne s'enregistre jamais seul.** Fournir `mobile` impose `indicatifMobile`,
fournir `telephone` impose `indicatifTelephone` — sinon **400**. L'indicatif est l'indicatif pays
sans le `+` (`"212"`, le `+` étant toléré et retiré), et il doit exister dans le référentiel : voir
GET /api/referentiels/pays, champ `indicatifTelephone`. C'est ce contrôle qui rend l'envoi WhatsApp
fiable en aval — un numéro sans indicatif est inenvoyable, un indicatif fautif expédie le message à
un inconnu.

**Le numéro est normalisé avant écriture**, au format que porte déjà la base : 9 chiffres deviennent
`6 98 29 80 95` (espacé, zéro initial retiré) — la forme de 98,8 % des fiches existantes ; toute
autre longueur est stockée en chiffres nus, comme le sont déjà les mobiles nord-américains à 10
chiffres. La valeur relue peut donc différer de celle envoyée.

**Contrôle de doublon d'identité.** Avant toute écriture, les fiches prospect/client au même nom,
téléphone ou email sont recherchées (règles de `GET /api/prospects/doublons`) ; s'il y en a et que
`ignorerDoublons` n'est pas `true`, **409** avec la liste `doublons`.

### reponse 400

`nom`/`raisonSociale` absents ; numéro fourni sans son indicatif, ou indicatif seul sans numéro ;
indicatif absent du référentiel ; numéro hors de 6–14 chiffres ; référence (type, pays,
profession...) inconnue.

### reponse 409

Des fiches prospect/client existantes portent le même nom, téléphone ou email (mêmes règles que `GET
/api/prospects/doublons`) et `ignorerDoublons` n'est pas `true`. Corps : `doublons`, liste de
`ProspectDoublonDto` avec leurs `criteres` — l'écran demande « est-ce l'une de ces personnes ? » et
renvoie avec `ignorerDoublons: true` pour un nouveau contact. Voir
`FEATURE-doublons-prospect-projet.md` §2.

## update

Mise à jour partielle : un champ absent ou `null` est laissé inchangé.

**Les mêmes règles de téléphone qu'à la création s'appliquent** (voir POST /api/prospects), avec une
conséquence propre à la modification : l'indicatif **n'est jamais repris implicitement** de la fiche
existante. Modifier `mobile` sans envoyer `indicatifMobile` renvoie 400 même si la fiche en portait
déjà un — un numéro qui change peut changer de pays, et hériter de l'ancien indicatif fabriquerait
un numéro qui n'existe pas.

En revanche, une modification qui **ne touche pas** au téléphone ne demande aucun indicatif : les
fiches historiques dont l'indicatif manque restent modifiables sur leurs autres champs.

## getProspections

Mêmes `ProspectionListItemDto` que GET /api/prospections.

**Périmètre commercial** : si l'appelant est restreint, seules les prospections de ses projets
affectés sont renvoyées. Le prospect, lui, n'est pas filtré — sa fiche reste accessible et la liste
peut simplement revenir vide ou partielle. `totalElements` reflète le nombre filtré.

## getDossiers

Mêmes `DossierListItemDto` que GET /api/dossiers.

**Périmètre commercial** : si l'appelant est restreint, seuls les dossiers de ses projets affectés
sont renvoyés ; le prospect lui-même n'est pas filtré. `totalElements` reflète le nombre filtré.

## getReglements

Triés par date de pièce décroissante par défaut.

**Périmètre commercial** : si l'appelant est restreint, seuls les règlements rattachés à un dossier
de ses projets affectés sont renvoyés (plus ceux dont aucune ligne ne résout de projet) ; le
prospect lui-même n'est pas filtré. `totalElements` reflète le nombre filtré.

## getHistorique

Onglet « Historique » du tiers (OMRAN : « l'historique de dossier de vente d'un client en
affichant : L'utilisateur, La date, L'état, Le N° Dossier, L'article, Les détails »). Une ligne par
changement de statut d'un dossier dont le tiers est le client (`StrFolderStatusHistory`), la plus
récente en premier ; le tri demandé par le client est ignoré. `utilisateur` est null sur les lignes
legacy sans auteur. Un tiers sans dossier rend une page vide.

**Périmètre commercial** : les lignes d'un dossier hors des projets affectés de l'appelant (ou d'une
entité hors exploitation) ne sont pas renvoyées ; le tiers lui-même n'est pas filtré.

## getContacts

Contacts (conjoint, notaire, apporteur…) liés au prospect — lignes `CfgTierContact` jointes au
`CfgTier` de chaque personne (nom, mobile, téléphone, e-mail) et à son type de relation.

## ajouterContact

La personne-contact est soit un `CfgTier` existant (`contactProspectId`), soit **créée à la volée**
(`nouveauContact`) puis liée — comme l'écran légataire, qui insère un nouveau `CfgTier` puis le lien
`CfgTierContact`. Fournir l'un **ou** l'autre ; `relationTypeId` est obligatoire.

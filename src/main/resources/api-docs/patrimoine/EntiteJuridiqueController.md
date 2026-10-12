# EntiteJuridiqueController

Écran « Entités juridiques » d'OMRAN (`CfgCompany`) : liste, fiche en quatre onglets, création, modification — dont
le drapeau **Active**, qui décide de tout ce que le CRM affiche (programmes, tranches, articles, prospections, dossiers
d'une entité inactive disparaissent des listes ; programme, prospection et dossier répondent 404 par id). Toutes les
écritures exigent le rôle
`Administrateur`. Lisible par tout utilisateur connecté, **jamais par une clé API** (hors `PorteeCleApi`). Les
sélecteurs d'entité des autres formulaires restent sur `GET /api/referentiels/entites` (actives seulement).

## lister

Ne filtre **pas** sur `IsActive` (sauf `actif=true|false`) : c'est l'écran qui gère le drapeau. Filtre comme
`CfgCompanyView.GetList` d'OMRAN : seules les entités où l'appelant a une habilitation société
(`AdmUserHabilitation.CfgCompanyId`), sauf rôle exempté (`Administrateur`, `Direction Générale`, lus dans
`AdmUserRole`). Un appelant sans compte `AdmUser` ne voit rien. `search` retire les espaces de la saisie et des
colonnes comparées (code, libellé, matricule fiscal, registre du commerce), casse ignorée.

## fiche

Même visibilité que la liste : une entité hors habilitation répond 404, active ou non.

## creer

Obligatoires : `code` (débarrassé de ses espaces de bord, unique sur toute la table, casse ignorée), `description`,
`deviseId` (devise existante ; présélectionner celle marquée `parDefaut` dans `GET …/devises`). `actif` vaut `true`
par défaut ; les textes non saisis sont enregistrés `""`, les nombres 0, jamais `null`, comme OMRAN. Le représentant
légal, le signataire et les responsables sont des personnels (`GET /api/referentiels-patrimoine/personnels`).

Dans la même transaction (`CfgCompany.AfterInsert`) : l'habilitation OMRAN du créateur sur l'entité (sans elle,
personne ne la voit dans OMRAN ni ici) et les 4 compteurs `AdmSequence` de la société — `INVOICE` « Facture »,
`REFUND` « Avoir », `RSSheetCode` « Certificat de retenue à la source », `Receipt` « Reçu », année courante,
basés sur l'année système, 2 chiffres d'année, 3 de numéro, valeur 0, préfixe, séparateur et suffixe vides.

`repartition` facultative. Le contrôle `Verif` d'OMRAN s'applique **toujours** : le nombre de lignes doit égaler
`nombreActionnaires`, la somme des parts doit égaler `nombreParts` (0 et 0 sans répartition).

### reponse 400

`code`, `description` ou `deviseId` manquant ; devise, groupe, pays ou personnel inconnu (ou tiers qui n'est pas un
personnel) ; texte trop long ; `capital`, `nombreActionnaires`, `nombreParts` négatifs ; `partMaison` hors [0 ; 1] ;
`repartition` (champ `repartition`) : actionnaire absent, inconnu ou en double (« L'actionnaire existe déjà dans la
liste !!! »), parts absentes ou négatives ; `Verif` : « Le nombre de répartitions doit être égal au nombre
d'actionnaires ! » (champ `nombreActionnaires`) ou « Le nombre de parts (actions) doit être égal au total des nombre
de parts / actions dans la répartition ! » (champ `nombreParts`).

## modifier

Champ absent ou `null` = inchangé ; `champsAVider` remet un texte à `""`, une date ou une référence à `null`, un
nombre à 0 (`code`, `description`, `deviseId`, `actif` ne se vident pas). `actif` est modifiable : l'effet sur la
visibilité est immédiat. `repartition` présente remplace toutes les lignes ; absente, `Verif` se fait sur les lignes
enregistrées — changer `nombreParts` seul est donc refusé si la répartition ne suit pas. `groupeId` pose le groupe
**sans** recalculer la répartition (OMRAN demande confirmation avant de la recharger : voir `depuis-groupe`).

## supprimer

Refusée (409, le détail liste les références) tant qu'une clé étrangère `NO_ACTION` de la production pointe sur
l'entité : programmes, articles, tiers, échéances, règlements, comptabilité, RH, achats, SAV… (57 colonnes). Sinon,
dans la même transaction : ses séquences, habilitations et lignes de répartition, puis l'entité ; les autres
dépendances `CASCADE` de production (`ActDefault*`, `Prv*Prevision`, `StlCategoryPrevision`,
`StlProjectVentilation`…) suivent par la base. Le groupe d'actionnaires de l'entité n'est jamais supprimé.

### reponse 409

L'entité est référencée : le détail donne le nombre de lignes par nature (« 2 programme(s), 14 règlement(s) »).

## modifierRepartition

Corps : la liste complète `[{actionnaireId, nombreParts}]`, qui remplace toutes les lignes. Contrôlée contre
`nombreActionnaires` et `nombreParts` **enregistrés** sur la fiche (changer aussi ces nombres : `PATCH` avec
`repartition`).

### reponse 400

Actionnaire absent, inconnu ou en double ; parts absentes ou négatives ; `Verif` (mêmes messages et champs qu'à la
création).

## modifierRepartitionDepuisGroupe

`CfgCompany.SetRepartitions` d'OMRAN, à appeler après la confirmation « Est ce que vous voulez charger les
répartitions !! ». Pose le groupe sur la fiche, vide la répartition puis la recalcule sur le `nombreParts`
**enregistré** : pour chaque ligne du groupe sauf la dernière (ordre : code de l'actionnaire),
`(int)(pourcentage × 100) × nombreParts / 100` en troncatures entières ; la dernière reçoit le reste, si bien que
la somme vaut toujours `nombreParts`. `nombreActionnaires` est aligné sur le nombre de lignes du groupe (OMRAN le
laisse à ressaisir, et refuserait l'enregistrement sinon). Calcul : `domain/RepartitionActionnaires`.

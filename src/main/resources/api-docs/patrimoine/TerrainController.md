# TerrainController

Écran OMRAN « TERRAINS » : `StkItem` de nature Terrain (`TER`), entité de statut `LandProject`. Écriture réservée à
l'Administrateur ; c'est le seul chemin d'écriture d'un terrain (`PATCH /api/articles/{id}` répond 400). Visibilité :
société directe (`CfgCompanyId`) active, comme le reste du Patrimoine ; un terrain d'une société inactive répond 404 en
lecture comme en écriture. Audit, notes, actions et pièces jointes passent par les routes de l'article (porteur
`article`, même `StkItem`), contenances par `GET/PUT /api/articles/{id}/contenances`.

## lister

Tous les terrains de nature TER des sociétés actives, lotissements compris. `entiteId` filtre la société du terrain,
`statutId` son statut, `vocationId` sa vocation ; `search` cherche sans casse ni espaces dans le code, la description,
le titre foncier, l'adresse, les notes et les libellés des références.

## lire

`coutTotal` = Σ des montants de ses frais (validés ou non) et `coutUnitaireFinal` = `coutTotal` / surface définitive
(0 si la surface est nulle) sont calculés à chaque lecture, jamais stockés. `sousTerrainsAutorises` reflète le statut
du terrain (case OMRAN « Créer un sous-terrain ») : aucun statut de production ne l'a aujourd'hui.

## creer

Obligatoires (400) : `code`, `description`, `entiteId`, `paysId`, `villeId`, `vocationId`, `categorieId`, `typeId`,
`surfaceProvisoire` ; société, pays, ville ou référence inconnus → 400, catégorie ou type d'une autre nature que
Terrain → 400, `natureId` autre que Terrain → 400. `code` unique dans la société (409). Statut = premier statut
(`Sorting`) de l'entité `LandProject` lu en base ; aucun → 500 sans écriture. Prix : HT ou TTC au m² par surface,
l'autre et les totaux recalculés comme pour un article ; `prixAchatM2` et `prixAchat` se déduisent l'un de l'autre par
la surface définitive. `estTermine` vaut vrai par défaut.

## modifier

Mêmes contrôles qu'à la création. Terrain occupé (dossier de vente ou de location) : surfaces, prix au m² HT/TTC,
TVA et titre foncier → 409, ligne inchangée. `estTermine` ne se modifie pas sur un sous-terrain ou un terrain issu
d'une prospection foncière (400). `champsAVider` : `niveauId`, `vocationTerrainId`, `orientationId`,
`categorieChargeId`, `fournisseurId`, les dates et les textes facultatifs.

## supprimer

Refus 409 : un frais validé (« Suppression impossible : frais <type> du <date> (<montant>) validé. ») ou porté par
une facture, un terrain occupé, ou encore référencé (prospection, dossier, offre… ; une autre clé étrangère → 409
nommant la contrainte) ; un sous-terrain ne se supprime que si le statut de son parent autorise les sous-terrains.
Sinon le terrain part avec ses sous-terrains, ses frais non validés, contenances, pièces jointes et notes.

## creerSousTerrain

Autorisé si le statut du parent a la case « Créer un sous-terrain » (`BoolField001`), sinon 409. Corps : `code`,
`description`, surfaces et prix ; société, statut, vocation, catégorie, nature, pays, ville, niveau, orientation,
type, vocation foncière, catégorie de charge, adresse et lien Google sont copiés du parent, `estTermine` vrai.

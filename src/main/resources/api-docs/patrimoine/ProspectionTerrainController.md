# ProspectionTerrainController

Écran OMRAN « PROSPECTIONS TERRAIN » : `LndLand`, terrain repéré avant achat, workflow `Str*` de l'entité `LndLand`.
Lecture pour tout compte connecté, écriture réservée à l'Administrateur ; aucune visibilité par société (la table n'en a
pas) ; aucune portée de clé d'API. Notes et audit : porteur `prospection-terrain` (`/api/notes`, `/api/audit?objetId=`) ;
pièces jointes : `/api/prospections-terrain/{id}/pieces-jointes`. Surfaces et prix calculés par le serveur seul
(`SurfacesTerrain`, variante marocaine si `AdmParamsString.CountryFormule` vaut `MA` ou est absent).

## lister

`search` cherche (sans casse) dans le code, la description, la ville, la catégorie, le type, la vocation et le niveau ;
`archivee`, `transformee`, `amenage` sont tri-états (absent = tous). `nbEtudesFaisabilite` compte les études de
faisabilité qui citent la prospection.

## lire

`peutTransformer` = Administrateur, aucun terrain issu, statut courant avec la case « Transformer en terrain »
(`BoolField001`) et superficie > 0 ; sinon `motifNonTransformable` donne le refus. `terrainId` / `terrainCode` : terrain
(`StkItem.LndLandId`) créé depuis la prospection.

## creer

Défauts OMRAN : date du jour, superficie 1, Aménagé, pays `AdmParamsGuid.DefaultCountry`, textes `""`, nombres 0,
booléens faux ; statut = premier statut de type « Nouveau » de l'entité `LndLand`, validité = date + durée du statut si
elle est positive ; une ligne d'historique « Création ». Obligatoires (400) : `code`, `description`, `villeId`,
`categorieId` (nature PROS TER), `typeId` (nature TER), pays (corps ou défaut). 400 aussi : superficie ≤ 0 (« La valeur
de la supérficie doit être strictement supérieure à 0 ! »), ville d'un autre pays, référence inconnue (le message nomme
le champ), lien Google ne commençant pas par `https://www.google`, texte trop long, nombre négatif ou hors bornes.

## modifier

Mêmes contrôles sur la valeur résultante (corps sinon base), dont le total des zones ≤ superficie nette (« la
superficies des zones ne doit pas dépasser la superficie nette du terrain ! »). Paire coefficient / montant : un montant
envoyé sans son coefficient en dérive le coefficient, sinon le montant suit le coefficient ; `coefEquipements`,
`superficieNette`, `superficieHabitat` sont ignorés. Statut d'échec ou d'annulation : la prospection est réarchivée, sauf
`estArchivee` explicite dans le corps. `champsAVider` : références facultatives, dates, textes facultatifs. 409 si le
statut courant est en lecture seule (« Cette prospection est en lecture seule dans le statut [X]. »).

## supprimer

409 : statut sans suppression (« Vous ne pouvez pas supprimer cette entité dans le statut : [X] ! »), terrain issu de la
prospection (nommé), études de faisabilité liées (nombre), autre clé étrangère (nommée). Sinon part avec ses zones, son
historique de statuts, ses pièces jointes et ses notes.

## modifierZones

Grille complète : `id` connu = mise à jour, sans `id` = création, absente = suppression, `id` d'une autre prospection →
400 ; `identification` obligatoire. Calcul de zone sans voies : emprise = COS × superficie, plancher couvert =
superficie × CUF ; plancher donné seul → CUF = plancher / (superficie × COS), comme OMRAN (aller-retour non idempotent si
COS ≠ 1). Total des superficies > superficie nette → 400.

## apercu

Recalcule toutes les grandeurs de `SurfacesTerrain` depuis la saisie, sans accès base hormis `CountryFormule`.
`champModifie` désigne le champ qui vient de changer : ce côté de sa paire gagne ; sans lui, un montant envoyé sans son
coefficient en dérive le coefficient, sinon le coefficient gagne. Division par zéro → 0.

## transitions

Toutes les transitions sortantes du statut courant (`StrWorkFlow.StatusFromId`), sans filtre de rôle.

## executerTransition

Transition qui n'est pas une sortie du statut courant → 409 avec `transitionsAutorisees` (codes des statuts d'arrivée
permis). Transition à second utilisateur → 409 ; transition liée à des rôles (intervenants) → 403 hors de ces rôles,
l'Administrateur passe ; commentaire exigé et vide → 400 « Veuillez saisir un commentaire obligatoirement ! ». Écrit le
statut, une ligne d'historique (date utilisateur = `dateUtilisateur` si la transition l'accepte, sinon maintenant),
archive vers un statut d'échec ou d'annulation (jamais désarchivée automatiquement) et pose la validité = date + durée si
la durée du statut d'arrivée est positive.

## historique

Lignes `StrHistory` de la prospection : « Création » puis une ligne par transition.

## transformerEnTerrain

Crée le terrain comme la fiche Terrain (`POST /api/terrains`, mêmes contrôles : société active, code unique dans la
société, catégorie de nature Terrain, type de la prospection de nature TER, statut initial `LandProject`), puis copie les
données foncières de la prospection ; prix au m² = prix proposé / superficie, prix d'achat = prix proposé. La
prospection est archivée et marquée transformée, dans la même transaction. 409 : déjà transformée (`terrainId` dans le
ProblemDetail), statut sans la case « Transformer en terrain ».

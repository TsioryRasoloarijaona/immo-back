# ConfigWorkflowFaisabiliteController

Configuration des statuts (`StkFeasibilityStudyStatus`) et des transitions (`StkFeasibilityStudyWorkFlow`) des études de
faisabilité (aide OMRAN « STATUTS », « TRANSITIONS »). Lecture Administrateur + Direction Générale, écriture
Administrateur ; aucune clé d'API (403 `HORS_PORTEE`). Rôles : `GET /api/referentiels/roles` ; types de document :
`GET /api/referentiels/types-piece-jointe`. Un changement de rôle ou de champ obligatoire change qui peut faire passer
une étude d'un statut à l'autre.

## creerStatut

`code`, `description` et `typeEtatId` obligatoires (400 nommant le champ) ; `code` unique (409). Défauts : `sorting` 1,
`delai` 0, `couleur` et `notes` vides. `champsObligatoires` : noms du catalogue `/champs-obligatoires` (nom inconnu →
400), stockés au format OMRAN avec virgule initiale (« ,Code,Area »), vide si aucun. Plusieurs statuts « Nouveau » sont
permis (le premier par ordre est le statut initial d'une étude). Documents stockés sans effet.

## supprimerStatut

409 si une étude est à ce statut, si une transition part de lui ou y arrive, ou si l'historique d'une étude le cite.
Ses documents partent avec lui.

## creerTransition

`description`, `statutDepartId`, `statutArriveeId` obligatoires. Défauts : `sorting` 1, `noteObligatoire`,
`avecNotification`, `envoyerEmail` faux (stockés sans effet). `champsObligatoires` : comme pour un statut (255 caractères
au plus une fois stockés).

### reponse 400

« Le statut de départ et le statut d'arrivé doivent être différents! » (message OMRAN exact), champ obligatoire absent,
statut, rôle ou type de document inconnu (detail nomme le champ), nom de champ inconnu.

## modifierTransition

Un champ absent ou null reste inchangé ; `roles`, `documents` ou `champsObligatoires` reçus remplacent l'existant
(rôles en double dédoublonnés). Une transition à 0 rôle est permise : personne ne peut alors l'emprunter. Seuls les
rôles lecteurs de l'étude (Administrateur, Direction Générale) comptent : un autre rôle listé est stocké sans effet.

### reponse 400

Mêmes refus que la création.

## supprimerTransition

Retire d'abord les rôles (comme OMRAN), puis les documents, puis la transition.

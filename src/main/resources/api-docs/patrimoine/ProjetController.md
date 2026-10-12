# ProjetController

## findAll

Restreinte aux programmes dont l'entité juridique est active (`CfgCompany.IsActive = 1`). Chaque ligne porte les
23 colonnes de la liste OMRAN (aide, page PROGRAMMES) : `adresseComplete` assemble ville, adresses, code postal et
pays comme la vue `CfgProjectView` ; `representantLegal` est celui de l'entité juridique. `search` filtre côté
serveur, casse ignorée, sur le code, le libellé, le code et le libellé de l'entité, et la ville.

## defautsEntite

Reprend `CfgProject.GetDefaultValuesFromCompany` d'OMRAN : à l'écran, au choix de l'entité, « Voulez-vous charger les
données par défaut de l'entité sélectionnée ? ». Copie **le code et le libellé** de l'entité (comme OMRAN : le
deuxième programme d'une même entité devra changer de code, sinon 409 à l'enregistrement), adresses, ville, code
postal, pays, téléphones, email, site, « Courrier fait à », signataire, responsables technique et commercial et
description détaillée (tronquée à 500 caractères). Ne crée rien. Fonctionne aussi pour une entité inactive, que le
POST refusera ensuite.

## create

Réservé au rôle `Administrateur`. Obligatoires : `entiteId` (entité existante **et active**), `code`, `description`.
Le code est saisi (aucune numérotation automatique, comme OMRAN), débarrassé de ses espaces de bord et **unique sur
toute la table**, casse ignorée. Les textes non saisis sont enregistrés vides (`""`), jamais `null`, pour rester
lisibles par OMRAN. `signataireId`, `responsableTechniqueId`, `responsableCommercialId` doivent désigner un
personnel (tiers de type EMP, `GET /api/referentiels-patrimoine/personnels`). Le créateur reçoit l'habilitation
OMRAN sur le programme (ligne `AdmUserHabilitation`), sans laquelle OMRAN ne l'affiche à personne. Trace :
`creePar`/`creeLe` et `modifiePar`/`modifieLe` posés au même instant.

### reponse 400

Champ obligatoire absent ou vide ; entité, pays ou personnel introuvable ; tiers qui n'est pas un personnel ; entité
inactive ; texte trop long ; date de fin (travaux ou commercialisation) antérieure à la date de début ;
`champsAVider` invalide. Le corps ProblemDetail porte `champ` = nom du champ en faute.

## update

Réservé au rôle `Administrateur`. PATCH : un champ absent ou `null` reste inchangé ; une chaîne vide `""` vide un
texte facultatif mais est refusée (400) pour `code` et `description`. Pour vider une date, le pays, le signataire ou
un responsable, lister son nom dans `champsAVider` (date et référence → `null`, texte → `""`) ; un nom inconnu, un
champ obligatoire (`entiteId`, `code`, `description`) ou un champ à la fois renseigné et listé donnent 400 avec
`champ` = `champsAVider`. Les dates sont contrôlées sur les valeurs résultantes (fin ≥ début). Pose
`modifiePar`/`modifieLe`.

## delete

Réservé au rôle `Administrateur`. Refusé (409, message listant ce qui bloque) tant qu'une tranche, un article ou une
autre donnée référence le programme ou ses zones (tarifs, variations de stock, dépenses et réclamations SAV,
journées travaillées) — les clés étrangères de la production. Sinon supprime dans la même transaction, comme OMRAN :
les habilitations `AdmUserHabilitation` du programme, ses zones, ses pièces jointes et messages (`CategoryId`), puis
le programme.

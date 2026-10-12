# ProjetController

## findAll

Restreinte aux programmes dont l'entité juridique est active (`CfgCompany.IsActive = 1`). `entiteCode` donne
l'entité de chaque ligne.

## create

Réservé au rôle `Administrateur`. Obligatoires : `entiteId` (entité existante **et active**), `code`, `description`.
Le code est saisi (aucune numérotation automatique, comme OMRAN), débarrassé de ses espaces de bord et **unique sur
toute la table**, casse ignorée. Les textes non saisis sont enregistrés vides (`""`), jamais `null`, pour rester
lisibles par OMRAN. Le créateur reçoit l'habilitation OMRAN sur le programme (ligne `AdmUserHabilitation`), sans
laquelle OMRAN ne l'affiche à personne. Trace : `creePar`/`creeLe` et `modifiePar`/`modifieLe` posés au même instant.

### reponse 400

Champ obligatoire absent ou vide ; entité ou pays introuvable ; entité inactive ; texte trop long ; date de fin
(travaux ou commercialisation) antérieure à la date de début. Le corps ProblemDetail porte `champ` = nom du champ en
faute.

## update

Réservé au rôle `Administrateur`. PATCH : un champ absent ou `null` reste inchangé ; une chaîne vide `""` vide un
texte facultatif mais est refusée (400) pour `code` et `description`. Les dates sont contrôlées sur les valeurs
résultantes (fin ≥ début). Pose `modifiePar`/`modifieLe`.

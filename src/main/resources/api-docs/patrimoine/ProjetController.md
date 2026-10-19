# ProjetController

## creer

Crée un programme (`CfgProject`). Obligatoires : `entiteId` (entité active), `code`, `description`. Le code est
comparé après `trim`, sans casse, à tous les programmes (409 avec le code et le nom du programme existant). Les
textes non saisis sont écrits `""` (OMRAN ne supporte pas NULL). Fin ≥ début pour les travaux et la
commercialisation (400). Le créateur reçoit l'habilitation OMRAN sur le programme (`AdmUserHabilitation`), sans
quoi le programme serait invisible dans OMRAN. Trace : `creePar`/`modifiePar` = l'utilisateur connecté.

## modifier

Modifie un programme. Champ absent ou `null` = inchangé, `""` = vidé (sauf code et description, obligatoires).
Une date ou le pays ne se vident pas. La règle fin ≥ début porte sur la valeur résultante (corps, sinon base).

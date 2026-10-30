# RolesController

Écran « Rôles » d'OMRAN, réservé à l'Administrateur. Le code d'un rôle porte les autorisations : il ne change jamais après création. Le rôle Administrateur est verrouillé. Les restrictions sont stockées ; seule `rapports` est lue ailleurs. Voir `instructions/FEATURE-admin-roles.md`.

## remplacerDroits

Écriture par différence : les droits de l'arbre non cochés sont retirés, les nouveaux sont insérés avec le libellé de l'arbre, les lignes conservées gardent leur PKey. Les droits absents de l'arbre (hérités d'OMRAN) ne sont jamais touchés, et un tel code dans `codes` répond 400 sans rien écrire. Sans changement effectif, rien n'est écrit ni tracé. L'arbre `AdmRightEntity` n'est jamais écrit.

## supprimer

Refusée en 409 si le rôle est lié à un utilisateur (`AdmUserRole` ou rôle web), un intervenant, un rapport, un workflow ou un droit web : en base, ces liens partiraient en cascade sans prévenir.

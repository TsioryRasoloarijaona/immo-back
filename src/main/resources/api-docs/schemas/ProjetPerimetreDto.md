# ProjetPerimetreDto

Un projet du périmètre commercial d'un utilisateur (CfgProject). Forme volontairement minimale —
pour la fiche complète d'un projet, voir GET /api/projets.

## id

Identifiant du projet (CfgProject.PKey) — la valeur à renvoyer dans `projetIds` de PUT
/api/utilisateurs/{admUserId}/projets.

## code

Code court du projet (CfgProject.Code), ex. "ARS", "MBR" — c'est ce code que le bandeau "Périmètre
limité à : ARS, MBR" affiche côté front.

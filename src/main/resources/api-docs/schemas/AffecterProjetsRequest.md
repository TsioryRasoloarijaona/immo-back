# AffecterProjetsRequest

Corps de PUT /api/utilisateurs/{admUserId}/projets — remplacement intégral du périmètre projet d'un
utilisateur (le front envoie l'état final de ses cases à cocher, pas un delta).

## projetIds

Identifiants des projets (CfgProject.PKey) désormais affectés à ce compte. **Obligatoire** : une
liste explicitement vide `[]` retire toute restriction (le compte voit tout — un périmètre vide
signifie "voit tout", jamais "ne voit rien"), mais un champ absent ou `null` est refusé en 400, pour
qu'un corps tronqué ne puisse pas lever une restriction en silence. Les doublons sont ignorés. 400
également si un identifiant ne correspond à aucun projet.

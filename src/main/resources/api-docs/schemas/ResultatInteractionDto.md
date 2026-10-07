# ResultatInteractionDto

Un résultat possible pour une interaction (appel, message, visite...), avec les indications
nécessaires pour adapter le formulaire côté front sans avoir à coder en dur les libellés métier.

## necessiteDateRdv

true si ce résultat rend le champ "Date du rendez-vous" obligatoire à la création de l'interaction
(résultats "Rendez vous pris", "RDV reporté" et "Le prospect veut faire une visite du projet") —
POST /api/prospections/{id}/interactions renvoie 400 si dateRdv est absente dans ce cas, et crée une
entrée exploitable par GET /api/calendrier/evenements.

## alerteRecommandee

true si la donnée légataire indique qu'une relance (dateAlerte) est recommandée pour ce résultat.
Purement indicatif — jamais imposé côté serveur.

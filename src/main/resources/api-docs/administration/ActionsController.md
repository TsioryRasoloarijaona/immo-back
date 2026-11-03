# ActionsController

Journal, en lecture seule, de ce qui a été créé ou modifié dans les principaux objets (actions commerciales, prospections, dossiers, règlements, stock…) : l'écran « Actions » du groupe Sécurité d'OMRAN Estate, lu sur la vue `AdmActionsView`. Réservé au rôle Administrateur (403 avant tout 400). Aucune écriture, aucune route annexe. Voir `instructions/FEATURE-admin-actions.md`.

## lister

Lignes triées par date de création décroissante, les lignes sans date en dernier ; à date égale, par `PKey` puis `action`, ordre stable entre les pages. Sans filtre : toute la vue, aucune période préréglée. Deux lignes de même `PKey` et d'`action` différente restent deux lignes, comptées toutes deux dans `totalElements`. `recherche` cherche une sous-chaîne dans `code`, `action`, `description`, `creePar` et `modifiePar`, espaces ignorés des deux côtés, `%`, `_` et `[` pris à la lettre ; vide ou blanche = pas de filtre. Les périodes de création et de modification se cumulent, leurs bornes sont des jours inclus ; une ligne sans date est exclue dès qu'une borne de sa période est posée. Les valeurs vides sont rendues telles quelles. `id` sert de clé de ligne, il n'ouvre aucun objet.

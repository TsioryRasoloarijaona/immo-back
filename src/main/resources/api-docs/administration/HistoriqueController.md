# HistoriqueController

Journal de toutes les actions (`AdmWebHistorics`), en lecture seule : l'écran « Historique » du groupe Sécurité d'OMRAN Estate. Réservé au rôle Administrateur (403 avant tout 400). Voir `instructions/FEATURE-admin-historique.md`.

## lister

Lignes de la période, date décroissante, à date égale par `PKey`. Sans date : les 30 derniers jours. `dateFin` est incluse jusqu'à 23:59:59. `texte` cherche dans `ObjectType` et `ObjectLabel` seulement, `%`, `_` et `[` pris à la lettre ; `utilisateur` est une égalité exacte. `totalElements` reflète les filtres ; une page vide n'est pas une erreur.

## utilisateurs

Valeurs distinctes de `UserDescription` sur la période, triées, sans null ni vide. Mêmes règles de période que la liste : sans date, les 30 derniers jours.

## exporter

Mêmes filtres et même tri que la liste, sans pagination : au plus 20 000 lignes. Date au format `dd/MM/yyyy HH:mm` ; `Details` et `Description` ne sont pas exportés.

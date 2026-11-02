# SessionsController

Sessions OMRAN (`AdmWebSessions`) : l'écran « Sessions » du groupe Sécurité d'OMRAN Estate. Réservé au rôle Administrateur (403 avant tout 400). L'identifiant de session (`WebSession`) n'est jamais exposé. Voir `instructions/FEATURE-admin-sessions.md`.

## lister

Lignes triées par date d'ouverture décroissante, à date égale par `PKey` (ordre SQL Server). Sans date : toute la table. `dateFin` est incluse jusqu'au dernier instant du jour (`StartDate < dateFin + 1 jour`). `utilisateur` est une égalité exacte sur `UserDesc`. `ip` cherche une sous-chaîne, `%`, `_` et `[` pris à la lettre. `ouverte=true` ne rend que `State = 2`, `ouverte=false` tout le reste, verrouillées comprises. `totalElements` reflète les filtres ; une page vide n'est pas une erreur.

## utilisateurs

Valeurs distinctes de `UserDesc`, triées. Tableau JSON de chaînes, pas une page.

## fermer

Pose `State = 4` et `EndDate = maintenant` sur une session ouverte ou verrouillée, sans toucher aux autres colonnes ; une date de fin déjà posée sur une session ouverte est remplacée. Le poste OMRAN n'est pas déconnecté, aucun compte Arpent n'est touché, aucun verrou de fiche n'est libéré et aucune trace n'est écrite dans le journal. Une session dans tout autre état répond 409 et rien n'est écrit, écart volontaire avec OMRAN qui réécrirait la date.

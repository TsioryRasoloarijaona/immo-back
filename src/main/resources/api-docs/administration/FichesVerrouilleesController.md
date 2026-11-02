# FichesVerrouilleesController

Verrous posés par OMRAN sur les fiches en cours d'édition (`AdmWebLockEntities`) : l'écran « Fiches verrouillées » du groupe Sécurité d'OMRAN Estate. Réservé au rôle Administrateur (403 avant tout 400). Arpent ne pose ni ne lit aucun verrou. Voir `instructions/FEATURE-admin-fiches-verrouillees.md`.

## lister

Lignes triées par date de verrouillage décroissante, à date égale par `PKey` (ordre SQL Server). Sans date : toute la table. `dateFin` est incluse jusqu'au dernier instant du jour (`LockDate < dateFin + 1 jour`). `recherche` cherche une sous-chaîne dans `Code`, `Description` et `UserDesc`, `%`, `_` et `[` pris à la lettre. Une ligne dont la session n'existe plus reste listée, `utilisateur` à null. `totalElements` reflète les filtres ; une page vide n'est pas une erreur.

## deverrouiller

Pose `State = 2` sur un verrou actif, sans toucher aux autres colonnes ni à la session, qu'elle soit ouverte ou non. Le poste OMRAN n'est pas prévenu : si la fiche y est encore ouverte, un autre utilisateur pourra la modifier en même temps et le dernier enregistrement gagnera. Aucune trace n'est écrite dans le journal. Une ligne dans tout autre état répond 409 et rien n'est écrit, écart volontaire avec OMRAN qui ne ferait rien en silence.

## deverrouillerTout

Pose `State = 2` sur toutes les lignes `State = 1`, sans filtre ni corps, comme OMRAN. Mêmes conséquences côté poste OMRAN que `deverrouiller`, aucune trace dans le journal. `nombre` compte les lignes touchées ; aucun verrou actif répond 200 avec 0.

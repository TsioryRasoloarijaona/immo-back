# AuditController

## lister

Les lignes sont rendues de la plus récente à la plus ancienne.

### param objetId

PKey de l'entité (même inactive), du programme, de la tranche, de l'article, de la prospection, de l'étude ou du rendez-vous. Le journal d'un rendez-vous est réservé à l'Administrateur.

### reponse 404

Aucun objet visible : entité, programme, tranche, article, prospection, étude (Administrateur / Direction Générale) ou rendez-vous (Administrateur seul). Un rendez-vous supprimé ne ressort plus par cette route : sa trace d'audit reste en base, mais la route répond 404.

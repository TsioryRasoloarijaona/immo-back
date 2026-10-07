# SynchronisationController

Rapatriement, à la demande, de ce que le logiciel de bureau écrit sur la base de référence vers la copie de lecture du CRM. En temps normal un cycle automatique s'en charge toutes les deux minutes ; cet endpoint permet de le déclencher immédiatement. Ouvert à l'Administrateur et aux commerciaux (Commercial, Commercial Chef, Commercial Aide) — l'opération est idempotente, sans risque de doublon, et un seul cycle tourne à la fois.

## declencher

Rapatrie sur-le-champ, dans la copie de lecture, ce que le logiciel de bureau a écrit sur la base de
référence depuis le dernier cycle — au lieu d'attendre le cycle automatique (toutes les deux
minutes). Renvoie le bilan du cycle : lignes fusionnées, supprimées, tables traitées.

**Synchrone** : la réponse n'arrive qu'une fois le cycle terminé (quelques secondes en régime normal
; plus long si une table doit repartir d'une reconstruction complète). **Un seul cycle à la fois** :
si un cycle automatique ou manuel tourne déjà, l'appel renvoie 409 sans rien relancer. L'opération
est idempotente et sans risque de doublon. Ouvert à l'Administrateur et aux commerciaux (Commercial,
Commercial Chef, Commercial Aide).

### reponse 409

Soit un cycle est déjà en cours, soit l'application tourne en mono-base (aucune base de référence
distincte) : rien n'est déclenché. Le message précise le cas.

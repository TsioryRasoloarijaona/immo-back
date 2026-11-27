# DemandesRemiseController

Demandes de remise des agences (`AppDemandeRemise`, table propre à Arpent). Une agence ne modifie pas le prix d'un dossier : elle demande un taux de remise, en pourcentage du total HT avant remise ; le siège valideur l'accepte (la remise est alors appliquée au dossier par le même calcul que `PATCH /api/dossiers/{id}`) ou la refuse avec un motif. Une seule demande `EN_ATTENTE` par dossier. Est valideur un compte sans rôle Agence qui porte le second niveau de plafond de remise ou le droit « sans plafond » (`validationRemise` du profil). Voir `instructions/FEATURE-agences-vente.md` (A3).

## demander

Réservé aux comptes Agence, sur un dossier de leur agence (sinon 404). Ordre des contrôles : corps (400), dossier sorti (409), taux demandé ≤ taux actuel du dossier (400), demande déjà en attente (409). Rien n'est écrit sur le dossier.

## accepter

Corps facultatif : sans `taux`, le taux demandé est accordé. La remise est appliquée au dossier avec le niveau de plafond le plus haut ; si l'application échoue (dossier sorti, remise dépassant le total HT), la demande reste `EN_ATTENTE`.

## annuler

Réservé à l'agence du dossier ; sans corps.

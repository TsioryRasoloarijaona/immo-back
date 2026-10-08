# VentesConcretiseesDto

Les ventes concrétisées de l'année, sur l'assiette exacte du logiciel de bureau : dossiers dont la
date d'avance tombe dans l'année et dont le statut a atteint CONCRETISATION, hors sorties.

## montantRegle

Ce qui a déjà été réglé sur ces dossiers — avances, crédits et autofinancements, signés par le sens
de la catégorie. Les frais n'y entrent pas.

## dossiersSansDateAvance

**Dossiers écartés faute de date d'avance**, toutes années confondues. 232 sur 387 en base réelle —
60 %. Ce compteur existe pour que ce trou d'assiette soit affichable : `nombreDossiers` ne couvre
pas tous les dossiers, et l'ignorer ferait croire à un effondrement des ventes.

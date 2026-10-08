# SynchronisationResultatDto

## version

Version Change Tracking de la source atteinte par ce cycle. Sert de repère de diagnostic ; null si
le Change Tracking n'est pas actif sur la source (le cycle n'a alors rien pu faire).

## fusions

Nombre de lignes insérées ou mises à jour dans la copie depuis la source (créations et modifications
faites hors CRM, typiquement par le logiciel de bureau).

## tablesEnEchec

Nombre de tables en échec sur ce cycle : leur filigrane est resté inchangé et elles seront rejouées
au cycle suivant. 0 en fonctionnement nominal.

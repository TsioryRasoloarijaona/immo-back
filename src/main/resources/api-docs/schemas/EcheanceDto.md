# EcheanceDto

## decompte

Numéro de décompte rattaché à l'échéance (`ComDocument.QuittanceCode`) — la colonne « Décompte » de
l'écran légataire. **16,5 %** des 921 échéances réelles en portent un.

`null` quand il n'y en a pas. En base la colonne n'est **jamais** `NULL` — elle porte une chaîne
vide, normalisée ici. Le logiciel de bureau affiche « N.A. » dans ce cas ; c'est un choix
d'affichage, à refaire côté client.

## creeLe

Date de création de la ligne d'échéancier (`ComDocument.UserCreateDateTime`) — colonne « Créé le »
de l'écran légataire. Renseignée sur **100 %** des échéances.

À ne pas confondre avec `dateEcheance`, qui est la date d'exigibilité : une échéance créée le
10/12/2025 peut être exigible le 10/12/2026.

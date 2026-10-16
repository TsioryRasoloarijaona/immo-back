# TotauxListeDto

## totaux

Une entrée par jeton demandé dans `agregats`, indexée par **le jeton tel qu'envoyé** (`surface` pour
une somme en nom nu, `surface:AVG` pour une fonction qualifiée). Mêmes calculs que la bande de totaux
de `/groupes`, sur toutes les lignes du filtre.

Valeur `null` pour `MIN`, `MAX` ou `AVG` quand aucune ligne n'a la colonne renseignée. Une date
revient en chaîne ISO-8601, comme dans `/groupes`. `COUNT` compte les lignes où la colonne est renseignée. Vide si
`agregats` est absent.

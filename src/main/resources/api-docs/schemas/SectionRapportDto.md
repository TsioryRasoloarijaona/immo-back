# SectionRapportDto

## lignes

Une ligne = un objet dont les clés sont les `nom` des `colonnes`. Nombres et montants en nombre JSON, pourcentages en
0–100, dates `aaaa-MM-jj`, valeur absente = `null`.

## totaux

Une clé par colonne ayant une `fonctionTotal` autre que `AUCUNE` ou une valeur imposée par le rapport (qui l'emporte sur
le calcul). `null` quand la section n'a aucune ligne. À afficher tel quel en pied de tableau.

## sousTotaux

Seulement si `groupePar` est renseigné : une entrée par valeur de la colonne de regroupement, chacune avec les mêmes clés
que `totaux` (sans valeur imposée). `null` sinon.

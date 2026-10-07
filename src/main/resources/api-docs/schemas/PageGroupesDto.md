# PageGroupesDto

## totaux

Les mêmes agrégats que dans chaque groupe, mais calculés sur **tout le périmètre filtré** : la bande
« Total : » de bas de grille, par opposition à celle d'un en-tête de groupe. Indexés par le même
jeton. Vide si `agregats` n'est pas fourni.

Suit `inclureSansValeur` à l'identique : sinon ce total et la somme des groupes désigneraient deux
populations différentes.

## nombreTotal

Nombre total de lignes agrégées — l'effectif dont `totaux` est tiré. Avec `inclureSansValeur=true`,
égale le `totalElements` de la liste non groupée.

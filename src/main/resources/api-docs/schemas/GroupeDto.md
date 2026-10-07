# GroupeDto

Un groupe de la liste repliée — l'équivalent d'un en-tête « Client : Adnane ES-SAIYDY (1 item) »
suivi de sa bande « Total : » dans le logiciel de bureau.

Les groupes sont **repliés** : ils portent l'effectif et les totaux, pas les lignes. Pour obtenir
les lignes d'un groupe, rappeler la liste correspondante avec `?groupeColonne=<la colonne
groupée>&groupeValeur=<la clé ci-dessous>`.

## cle

Valeur technique du groupe, à réémettre telle quelle dans `?groupeValeur=` pour déplier le groupe.
Selon la colonne groupée : un identifiant (groupement par client/commercial/notaire), un code
(statut, tranche, origine), un booléen, ou un entier de période (`202507` pour juillet 2025, `2025`
pour l'année).

**`null` est une valeur de groupe légitime** : ce sont les lignes sans valeur sur la colonne
groupée. Le groupe est renvoyé et non masqué, sans quoi la somme des effectifs cesserait de retomber
sur le total de la liste.

## libelle

Libellé affichable du groupe. Identique à `cle` pour la plupart des colonnes ; dérivé pour les
groupements par tiers (priorité raison sociale > description > prénom + nom, exactement comme la
colonne correspondante de la liste) et pour les périodes (`juillet 2025`). `null` pour le groupe des
lignes sans valeur.

## agregats

Résultats des agrégats demandés via `?agregats=`, **indexés par le jeton envoyé** — `prix` pour un
nom nu, `prix:AVG` pour un nom qualifié. Vide si `agregats` n'est pas fourni.

Un nom nu vaut toujours une somme et revient sous ce nom nu : la réponse d'une intégration existante
est donc inchangée par l'ouverture du menu Σ.

**Le type de la valeur suit la fonction** : nombre décimal pour `SUM` et `AVG`, entier pour `COUNT`,
et pour `MIN`/`MAX` le type de la colonne — donc une date ISO-8601 sur une colonne date. Une somme
dont toutes les lignes du groupe sont nulles vaut `0`, jamais `null` : c'est un total, pas une
valeur absente. Un `MIN`/`MAX` sans aucune valeur vaut `null`, lui — il n'existe pas de minimum de
rien, et `0` serait un mensonge.

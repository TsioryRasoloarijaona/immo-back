# OngletsTrancheController

## chargerDepuis

Bouton OMRAN « Charger à partir d'une autre tranche ». La source écrase les paramètres de la cible, valeurs nulles
comprises : standing, niveau terrain, responsable SAV, coordonnées, règles 1, cadeaux, remises anciens clients, délais,
description longue. `concretisationPercent` de la cible reprend son `advancePercent`.

Les listes Étages, Plancher couvert, Frais de vente et Finalisations sont fusionnées par clé (niveau, catégorie
d'article, catégorie de règlement, type de dossier) : ligne absente de la cible créée, ligne commune reprise de la
source, ligne propre à la cible conservée. Le TTC des frais est recalculé depuis le HT et la TVA ; `plancherCouvert`
de la cible = somme de ses lignes après fusion.

Jamais copiés : code, description, programme, dates, indicateurs `isFor*`, terrains, commissions vendeur, prévisions,
seuils de versement. Une seule transaction : un refus n'écrit rien.

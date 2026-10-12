# OngletsTrancheController

## enregistrerTerrains

Réservé au rôle `Administrateur`. Le corps `{"lignes": [...]}` est la liste **complète** de l'onglet, réconciliée en
une transaction : ligne avec un `id` de cette tranche → modifiée, sans `id` → créée, ligne enregistrée absente →
supprimée ; `[]` vide l'onglet. Un terrain déjà présent plus haut dans la liste est ignoré (doublon, comme OMRAN).
L'ordre (`Sorting`) est le rang de la ligne dans la liste ; `ratio` absent vaut 1. Aucun contrôle de la somme des
ratios. Un terrain doit être un article de nature TER et de vocation PROJET (`GET /api/tranches/terrains-disponibles`),
sauf sur une ligne existante qui garde son terrain. Chaque ligne écrite est tracée au journal d'audit OMRAN comme
enfant de la tranche.

## enregistrerEtages

Réservé au rôle `Administrateur`. Même réconciliation que l'onglet Terrains (liste complète, une transaction, doublon
de niveau ignoré). `niveauTerrainId` désigne un niveau de construction `LndLevel`
(`GET /api/referentiels-patrimoine/niveaux-terrain`), sans lien avec l'étage d'un article (`StkFloor`).

## enregistrerPlancherCouvert

Réservé au rôle `Administrateur`. Même réconciliation que l'onglet Terrains (doublon de catégorie ignoré). La
catégorie doit être une catégorie d'article ou de sous-article de nature Bâtiment
(`GET /api/referentiels/categories-bien?pourSousArticle=false` et `=true`). Dans la même transaction, le plancher
couvert de la tranche (`CoveredFloorArea`, champ `plancherCouvert` de la fiche) devient la somme des superficies.

## enregistrerFrais

Réservé au rôle `Administrateur`. Même réconciliation que l'onglet Terrains (doublon de catégorie ignoré). Le client
envoie le HT (`saisiEnTTC` faux ou absent) ou le TTC (`saisiEnTTC` vrai) ; le serveur calcule l'autre :
TTC = HT × (1 + TVA), HT = TTC / (1 + TVA), à 6 décimales arrondi au plus proche. `ordre` absent = rang de la ligne.
Une nouvelle catégorie doit figurer dans `GET /api/tranches/categories-frais`.

## categoriesFrais

Catégories de règlement « frais de vente » (`IsCredit` et `IsSaleExpense`), moins les trois que le dossier de vente
pose lui-même : paramètres OMRAN `DefaultCustomerTransfertDocumentType`, `DefaultProductTransfertDocumentType`,
`DefaultCancellationDocumentType`.

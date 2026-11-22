# BilansProjetController

Tableau de bord **Bilans** du module Projet (OMRAN `Yes.Prj.Front.Tdb.ProjectReview`), en lecture seule. Voir
`FEATURE-n5-tableaux-de-bord-projet.md` § Bilans.

**Droit** : `Yes.Prj.Front.Tdb.ProjectReview`, sinon 403. **Visibilité** : projets demandés ∩ projets habilités ; un
projet hors périmètre est ignoré (jamais 403) ; si `projets` est absent ou ne contient aucun projet habilité : 400
« Veuillez sélectionner au moins un projet ! ».

## bilans

Arbre des catégories de dépenses `StlCategory` (racines sans parent, tri `Sorting` puis `Description`). Chaque nœud
**cumule sa catégorie et ses descendants** ; une catégorie nulle sur les 8 colonnes avec tous ses descendants est
masquée. Colonnes (codes) : `etudeFaisabilite`, `estimationBase`, `estimationConfidentielle`, `marche`, `marcheAjuste`,
`previsions`, `coutPrevisionnel`, `coutReel`, en HT ou TTC selon `ttc`. Étude de faisabilité = prévisions de l'étude
ventilées sur les projets non communs de la tranche ; les 7 autres = montants des lots répartis par catégorie
(`PrjMarketVentilationCategory`), un lot de projet commun étant réparti sur les projets non communs de ses tranches.
Un lot annulé ou résilié multiplie toutes ses colonnes, sauf Marché, par son ratio d'avancement. Règlements `CANCELED`
hors totaux. Le défaut d'OMRAN sur la colonne Marché des racines n'est pas reproduit.
`ecart` = colonne1 − colonne2, calculé seulement si les deux sont fournis (sinon 0 partout) ; `ecart1` / `ecart2`
renvoient les codes reçus, null pour un paramètre absent.

## exporter

Classeur xlsx de l'arbre du bilan avec les mêmes filtres, mêmes refus (400, 403) et mêmes projets visibles.

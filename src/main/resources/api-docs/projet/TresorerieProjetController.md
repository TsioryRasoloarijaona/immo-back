# TresorerieProjetController

Tableau de bord **Trésorerie** du module Projet (OMRAN `Yes.Prj.Front.Tdb.ExpensesByYear`), en lecture seule. Voir
`FEATURE-n5-tableaux-de-bord-projet.md`.

**Droit** : `Yes.Prj.Front.Tdb.ExpensesByYear`, sinon 403. **Visibilité** : les lignes n'entrent que par les projets
demandés ∩ projets habilités, et par une société habilitée. Un projet hors périmètre est ignoré (jamais 403) ; si
`projets` est absent ou ne contient aucun projet habilité : 400 « Veuillez sélectionner au moins un projet ! ».

**Sources** (prévisions seulement) : échéances de lot `PrjMarketPaymentPrevision` (montant × taux de ventilation de la
tranche, une ligne par tranche ventilée, au débit) et reste à payer des pièces d'achat `BuyDocument` rattachées à un lot
(`CanBePayed`, reste = à payer − Σ lignes de règlement, **règlements `CANCELED` exclus** — écart à OMRAN, qui ne filtre
pas le statut ; avoir `REFUND` au crédit, montant positif). **Aucun réel** : parité OMRAN, dont la lecture du réglé
réel par projet ne ramène rien. Une ligne datée au plus tard à la fin du mois courant est **reportée à aujourd'hui**
(`reporte = true`, `dateOrigine` conservée), y compris celles des années antérieures, comptées dans l'année courante.

## tresorerie

Année civile `annee` (défaut : année du jour), mois m = Σ des lignes de date en (annee, m). Onglet `projet` : racines
« Encaissement » (`estCredit = true`) / « Décaissement » → projet (`PrjProject.Description`) → fournisseur (nom du
fournisseur, sinon description du lot ou fournisseur saisi). Onglet `fournisseur` : mêmes lignes regroupées directement
par fournisseur sous les racines ; le total d'un onglet égale celui de l'autre. Un nœud dont les 12 mois sont nuls est
absent. `regle` = Σ des lignes dont la date retenue (reportée) est avant aujourd'hui. `projets` = choix du sélecteur
(projets habilités de la société). Pas de ligne Solde ni Cumulé (absentes de l'écran Projet d'OMRAN).

## cellule

Lignes sources de la cellule (`noeud` = `cle` lue dans l'arbre, `mois` de 1 à 12), mêmes filtres que l'arbre, triées par
date. Leur somme égale la valeur de la cellule. `objetId` = PKey de la source (échéance de lot ou pièce d'achat).

## exporter

Classeur xlsx de l'arbre (onglet Projet) avec les mêmes filtres, mêmes refus (400, 403) et mêmes lignes visibles.

# TresorerieController

Écrans de trésorerie des arbres **Projet** (`module=PRJ`) et **Achat** (`module=BUY`), en **lecture seule** : Dépenses ›
Décaissements, Dépenses › Affectations, Recettes (factures proforma, factures, avoirs, encaissements, affectations).
Voir `FEATURE-n3-recettes-depenses.md`. Chaque écran exige son droit OMRAN `Yes.Stl.Front.*` (403 sans lui).

**Visibilité** (celle des pièces d'achat) : société habilitée ET (projet habilité OU sans projet). Le projet d'un
règlement se lit **à la ligne** : une affectation est visible si sa pièce l'est ; un règlement est visible si sa société
est habilitée ET (il n'a aucune ligne OU au moins une ligne visible). Une pièce de recette (`StlDocument`) n'a pas de
projet : elle suit la règle « sans projet ». `projetIds` restreint aux projets cochés ; `sansProjet=true` ajoute les
pièces et règlements sans projet (seul : uniquement eux).

**Annulés** : listés par défaut (`inclureAnnules` absent = true), **jamais dans les totaux** ; un statut NULL n'est pas
annulé. Les totaux portent sur toute la liste filtrée, pas sur la page. Tri fixe : date de pièce décroissante puis
référence décroissante.

**Période** : `dateSur` choisit la date filtrée par `du` / `au` (jours inclus) : `PIECE` (DocDate), `ECHEANCE`,
`POINTAGE` (SettlementDate), `POINTAGE_REPORTE` (date de mouvement : `d` = date de pointage, sinon échéance ; si
`d` ≤ aujourd'hui et non pointé → aujourd'hui, sinon `d`).

## decaissements

Une ligne par règlement débit (`IsCredit = 0`) du module. `montant` tel que stocké ; `montantAffecte` et
`resteAAffecter` portent sur **toutes** les lignes du règlement, même celles d'un projet non habilité (un mouvement
bancaire unique). `nonAffecte` : aucune ligne. `enRetard` : échéance ≤ aujourd'hui et statut ≠ `POINTED`.

## decaissement

Même visibilité que la liste (sans filtre de module). `lignes` ne contient que les affectations dont la pièce est
visible ; `lignesMasquees` compte les autres.

## affectations

Une ligne par affectation `StlSettlementLine` visible, rattachée à un règlement du module et du sens demandés.
`totaux.montantReglement` compte chaque règlement une fois ; `debit` / `credit` = Σ `montantAffecte` des règlements
débit / crédit. Droit : `Debit.SettlementAffectationList` (DEBIT) ou `Credit.SettlementAffectationList` (CREDIT).

## recettes

`proforma`, `factures`, `avoirs` : pièces `StlDocument` de nature `INVOICE_PROFORMA`, `INVOICE`, `REFUND` ; réglé = Σ
des lignes de règlement non annulées, reste = net à payer − réglé ; `totaux.montant` = Σ net à payer. `dateSur` :
`PIECE` ou `ECHEANCE` seulement (400 sinon). `encaissements` : règlements crédit (`IsCredit = 1`) du module, colonnes
de la grille OMRAN ; `totaux.montant` = Σ montant hors annulés. `colonnes` décrit les champs de chaque ligne.

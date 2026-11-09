# LotsProjetController

Module Projet (chantier OMRAN), en **lecture seule** : lots (`PrjMarket`, « marché » dans la base), bordereau, décomptes
(`PrjWorkingOut`) et leurs factures. Voir `FEATURE-projet-lots-decomptes.md`.

**Visibilité, comme OMRAN** : l'appelant voit les projets de ses lignes `AdmUserHabilitation.PrjProjectId`, **quel que soit
son rôle** (aucune exemption Administrateur ni Direction Générale). Sans ligne d'habilitation, ou appelant non identifiable :
listes vides. Un lot sans projet, ou dont le projet n'est pas visible, répond **404** comme un identifiant inconnu (jamais 403).

## referentiels

Listes de choix des écrans Lots et Décomptes. `projets` et `categories` ne contiennent que ce que l'appelant voit ;
`typesLot`, `statutsLot` et `statutsFacture` sont des référentiels complets. Le statut d'un lot n'est pas tenu à jour dans
la donnée OMRAN : c'est une étiquette, jamais une règle.

## lots

Liste **globale** des lots des projets visibles. `projetIds` est **intersecté** avec le périmètre : un projet demandé hors
périmètre est ignoré sans erreur, et une intersection vide donne une page vide (200). Tri par défaut : `code` décroissant ;
clés de tri acceptées : `code`, `description`.

Montants (calculés, jamais stockés) : marché = Σ du bordereau définitif (`Quantity × UnitPriceHT`, TVA par `VatRatio`,
`Quantity × UnitPriceTTC`), ou, si le lot n'a aucune ligne, le montant forfaitaire saisi (`EstimatedPrice*Manual`) ;
`montantsDepuisBordereau` dit quel chemin a servi. Cumuls = sommes des colonnes stockées des décomptes du lot.
`avancementPct` = cumul décomptes TTC / marché TTC × 100 (les travaux supplémentaires n'y entrent pas), `null` si le marché
est nul. Facturé = Σ `BuyDocument.AmountToPay` des factures des décomptes (`PrjWorkingOut.BuyDocumentId`) ; réglé = Σ des
lignes de règlement non annulées de ces factures ; reste = facturé − réglé.

## lot

Fiche d'un lot : les champs de la liste, plus le montant forfaitaire saisi, l'estimation (Σ bordereau estimatif, sinon le
forfaitaire), la retenue de garantie et l'avance théoriques (marché TTC × taux), les taux en pourcentage (0–100), les
options et les dates d'appel d'offres et de chantier (`null` si non saisies).

## bordereau

Lignes du bordereau **définitif** (`PrjMarketLine` hors stock), tri par ordre, avec leurs totaux calculés. Liste vide : lot
au montant forfaitaire (« Bordereau non saisi »).

## decomptesDuLot

Tous les décomptes du lot (pas de pagination), tri par numéro (`Sorting`) puis date. Mêmes champs que
`GET /api/projet/decomptes`, dont le contrôle d'écart du net à payer.

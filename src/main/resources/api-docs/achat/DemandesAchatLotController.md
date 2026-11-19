# DemandesAchatLotController

## creer

Ordre des refus : 404 (lot inconnu ou hors périmètre) → 403 (`Yes.Buy.Front.CallOfTender.BuySelectionList.toolNew`, ou aucun statut de départ visible) → 409 (statut du lot) → 400 (procédure ou description absente, procédure sans entité) → 409 (autre demande non annulée). Demande au module Projet : société et projet du lot, procédure choisie par montant si l'estimé TTC du lot n'est pas nul, montants, paiement, délai et dates du lot ; bordereau estimatif du lot copié (groupes et lignes hors stock).

### reponse 409

Statut du lot qui n'ouvre pas l'ajout d'une demande (`BoolField005`) ; lot non multi-fournisseur ayant déjà une demande non annulée (« Pour pouvoir ajouter un nouvel appel d'offre il faut d'abords annuler tous les autres ! »).

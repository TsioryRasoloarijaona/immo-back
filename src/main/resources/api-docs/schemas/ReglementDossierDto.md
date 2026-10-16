# ReglementDossierDto

## montantAffecte

Somme des `StlSettlementLine.Amount` de **toutes** les lignes du règlement, tous dossiers et tous types de
pièce confondus — le même calcul que la fiche encaissement. Vaut 0 pour un règlement `CANCELED` (exclu des
totaux).

## resteAAffecter

`montant − montantAffecte`, calculé, jamais stocké (« Reste à affecter : Montant base - Montant affecté »,
aide OMRAN). Identique au `montantBrutRestant` de `GET /api/encaissements/{id}` pour le même règlement.

## montantAffecteAutresDossiers

Part de `montantAffecte` portée par des lignes hors de ce dossier (autre dossier ou pièce non vente). 0 en
général ; positive pour un règlement légataire réparti sur plusieurs dossiers.

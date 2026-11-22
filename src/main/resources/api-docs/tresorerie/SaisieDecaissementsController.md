# SaisieDecaissementsController

## creer

Crée un règlement débit (`IsCredit = 0`) du module PRJ ou BUY et ses affectations aux pièces d'achat
(`StlSettlementLine.BuyDocumentId`). Exige la lecture de l'écran et `toolNew`. Ordre des refus : 404 (société, fournisseur,
compte ou pièce inconnus ou invisibles) → 403 → 400 → 409. Montant du règlement > 0 ; `Amount = AmountBase`, sans retenue à
la source ni timbre. Une ligne d'historique `StlSettlementHistory` est écrite. Refus 400, messages OMRAN : aucune ligne
sans `CanSaveWithoutAffectation` (R-a), Σ lignes > montant (R-b), ligne à 0 (R-c), devise de pièce différente (R-d), date
de pointage sans compte (R-e), échéance < date pièce (R-f), pointage sans statut POINTED (R-g), POINTED sans pointage
(R-h), référence / notaire / compte obligatoires selon le type (R-k), statut CANCELED, pièce d'un autre fournisseur, pièce
répétée, compte d'une autre société, ligne > reste à payer de la pièce. 409 : référence déjà utilisée pour un type à
référence unique (même type, sens et compte, R-l) ; pointage à une date ≤ dernier rapprochement du compte (R-i, message
« dernier » corrigé de l'OMRAN « dérnier »).

## modifier

Mêmes règles que la création. Le module est ignoré. Les lignes visibles sont remplacées par `lignes` ; les lignes
masquées (pièce d'un projet ou d'une société non habilités) sont gardées et comptent dans le reste à affecter. Changer
`societeId` exige `CanModifyCompany` (403). `IsAccounted` repasse à 0 si le reste à affecter, la date de pointage, le type
ou le compte changent. Une ligne d'historique s'ajoute si l'échéance, le type ou la référence changent. 409 si le
règlement appartient à un virement interne (`StlInternalTransferId`). Le contrôle R-i ne joue que si la date de pointage
change.

## supprimer

Exige la lecture et `toolDelete`. Supprime le règlement et son historique. 409 : règlement pointé (date de pointage
renseignée, R-m), avec affectations (R-n), référencé par une écriture comptable, un bordereau, une feuille de
transmission, une pièce jointe ou une déduction (R-o), ou jambe d'un virement interne.

## referentiels

Types de règlement (drapeaux de contrôle), sociétés et comptes habilités, devise par défaut, statuts proposés
(`IN_WORK`, `POINTED`, `NOTPAYED`) et droits du compte connecté (`droits`), pour afficher ou masquer Nouveau, Supprimer et
le choix de société.

## piecesAAffecter

Pièces d'achat (`BuyDocument`) du module, du fournisseur et de la société, visibles par le compte connecté, dont le
reste à payer est > 0. `regle` exclut les règlements annulés et, avec `reglementId`, les lignes de ce règlement (cas de la
modification).

# DossierReglementsController

Règlements d'un dossier de vente répartis sur plusieurs échéances, comme OMRAN : un règlement (`StlSettlement`) porte une ligne (`StlSettlementLine`) par échéance cochée, et n'appartient au dossier que par ces lignes. Le « reste à affecter » vaut montant − somme des lignes ; il est calculé, jamais stocké (aide OMRAN, page RÈGLEMENTS : « Reste à affecter : Montant base - Montant affecté »). L'ancien `POST .../echeancier/{echeanceId}/reglements` reste disponible : c'est la même écriture avec une seule affectation.

## creer

Mêmes règles d'en-tête que `POST .../echeancier/{echeanceId}/reglements` (montant > 0, `dateEcheance` et `typeId` requis, banque et référence selon le type, `IN_WORK` seulement, `dateReglement` refusée). Puis, chacune en 400 : au moins une affectation ; chaque échéance du dossier, une seule fois, avec un montant > 0 au plus égal à son reste dû ; somme des lignes ≤ `montant` (« Le reste à affecter doit être >= 0 »). Égalité et infériorité sont permises : la différence reste à affecter. La pénalité de retard de chaque échéance affectée est générée avant l'enregistrement, sur le reste d'avant ce règlement. Tout est contrôlé avant la première écriture : une règle violée n'écrit rien.

## lister

Règlements ayant au moins une ligne sur une échéance de ce dossier, saisie la plus récente d'abord (`DocDate` puis `UserCreateDateTime` décroissants). `montantAffecte` et `resteAAffecter` portent sur toutes les lignes du règlement, `affectations` sur celles de ce dossier seulement.

## affecter

Ajoute des lignes à un règlement `IN_WORK` ou `POINTED` (409 s'il est `CANCELED`), sans modifier son montant ni ses lignes existantes. Mêmes règles par ligne qu'à la création, plus : une échéance qui a déjà une ligne dans ce règlement est refusée (400), et la somme des nouvelles lignes ne dépasse pas le reste à affecter actuel (400 « dépasse le reste à affecter »).

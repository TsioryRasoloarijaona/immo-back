# CreerReglementDossierRequest

## montant

Montant total du règlement (`StlSettlement.Amount`). **Requis**, strictement positif. La somme des
affectations doit lui être inférieure ou égale (400 « Le reste à affecter doit être >= 0 » sinon) ; la
différence est le reste à affecter, affectable plus tard par `POST .../reglements/{reglementId}/affectations`.

## affectations

Répartition du règlement sur les échéances du dossier, une `StlSettlementLine` par élément. **Au moins une**
(400 sinon : un règlement n'est relié au dossier que par ses lignes). Chaque échéance appartient au
dossier, n'apparaît qu'une fois, et reçoit un montant strictement positif au plus égal à son reste dû.

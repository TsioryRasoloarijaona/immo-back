# AffecterReglementRequest

## affectations

Lignes ajoutées au règlement, **au moins une**. Mêmes règles qu'à la création (échéance du dossier, une
seule fois, montant > 0 et ≤ reste dû), plus deux : une échéance qui a déjà une ligne dans ce règlement est
refusée, et la somme des nouvelles lignes ne dépasse pas le reste à affecter actuel (400 sinon).

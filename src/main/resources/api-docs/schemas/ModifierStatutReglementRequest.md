# ModifierStatutReglementRequest

## statut

Statut cible. Valeurs acceptées : `POINTED` (encaissement constaté) ou `CANCELED` (règlement
annulé). Depuis `IN_WORK` uniquement — les deux états sont terminaux, donc toute transition partant
de `POINTED` ou `CANCELED` renvoie 409.

## dateReglement

Date d'encaissement effectif (`StlSettlement.SettlementDate`). **Requise** pour `POINTED`, ignorée
pour `CANCELED` (qui laisse la colonne à `null`, conformément aux données réelles).

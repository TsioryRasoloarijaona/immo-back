# ModifierStatutReglementRequest

## statut

Statut cible. Valeurs acceptées : `POINTED` (encaissement constaté) ou `CANCELED` (règlement
annulé). Depuis `IN_WORK` uniquement — les deux états sont terminaux, donc toute transition partant
de `POINTED` ou `CANCELED` renvoie 409.

## dateReglement

Date d'encaissement effectif (`StlSettlement.SettlementDate`). **Requise** pour `POINTED`, ignorée
pour `CANCELED` (qui laisse la colonne à `null`, conformément aux données réelles). Pour `POINTED`, elle doit
être postérieure au dernier rapprochement du compte (`StlAccountPosition`), sinon 409.

## compteId

Compte bancaire du pointage (`StlSettlement.StlAccountId`), à choisir dans
`GET /api/referentiels/comptes?societeId=<ReglementDto.societeId>`. **Requis** pour `POINTED` (400 « Si votre
pièce possède une date de pointage, il est obligatoire de lui affecter un compte ! ») ; il doit exister et
appartenir à la société du règlement (tout compte si le règlement n'a pas de société), sinon 400. Ignoré pour
`CANCELED`.

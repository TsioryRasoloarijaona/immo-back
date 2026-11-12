# LignePositionDto

## type

`INVOICE`, `DOCUMENT_TO_PAY` (crédit), `REFUND` (avoir, débit), `REGLEMENT` (ligne de règlement sur une pièce) ou
`REGLEMENT_NON_RATTACHE` (part d'un règlement non affectée à une pièce : `AmountBase` − Σ lignes, sans projet).

## dateEcheance

Pièce : `MaturityDate`. Règlement : date de pointage (`SettlementDate`), sinon échéance (`EcheanceDate`).

## montantTTC

Pièce : (TTC + stock + fluctuations + prorata) × taux de projet ; null pour un règlement.

## aPayer

Pièce : montant compté dans la position (net à payer × taux de projet, avoirs non financiers réajoutés pour une facture) ;
null pour un règlement. Σ (crédits − débits) des lignes sans `du` = `soldeFin` du fournisseur.

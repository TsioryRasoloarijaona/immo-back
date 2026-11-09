# DecomptesController

## decomptes

Liste **globale** des décomptes des lots visibles (même périmètre que les lots). `projetIds` est intersecté avec le
périmètre (hors périmètre ignoré, intersection vide = page vide). Tri par défaut : `docDate` puis `code`, décroissants ;
clés de tri acceptées : `docDate`, `code`, `sorting`.

Les montants sont ceux **stockés** par OMRAN. `netAPayerRecalcule` applique la formule d'OMRAN aux colonnes stockées :
`AmountTTC − AdvanceAmount − WorkingOutAdvanceAmount − WarrantyAmount − nonRefundableRetainer + extraAmount +
ProrataRefundAmount + StockAmountToPay + FluctuationsToPay` (terme NULL = 0, arrondi 2 décimales sur la somme).
`ecartNetAPayer` = `netAPayer − netAPayerRecalcule` ; `ecartSignificatif` si sa valeur absolue dépasse 0,02. Un écart
s'affiche, il ne bloque rien.

La facture d'un décompte est **seulement** celle de `PrjWorkingOut.BuyDocumentId` (une pièce qui pointe le décompte par
`BuyDocument.PrjWorkingOutId` n'est pas sa facture). Réglé = Σ des lignes de règlement non annulées de la facture ;
reste = `BuyDocument.AmountToPay` − réglé (le net de la facture inclut sa régularisation `AmountRegul`).

## decompte

Fiche d'un décompte : les champs de la liste, le détail des montants stockés dans l'ordre OMRAN, le cumul TTC du lot
jusqu'à ce décompte, la facture, ses règlements (annulés compris, marqués `annule = true`, exclus du réglé), les lignes
(jointes au bordereau, avancement par ligne non plafonné), les travaux supplémentaires et les retenues.

# AgregatsBonCommandeDto

Agrégats d'un bon de commande, **calculés côté serveur** comme la grille « Bons de Commande » d'OMRAN (vue
`BuyDocumentOrderView`), sur ses pièces liées : toutes les pièces `BuyDocument` dont `BuyOrderId` désigne le bon de
commande, qu'elles soient visibles ou non de l'appelant (comme OMRAN). Factures = nature `INVOICE`, bons de livraison =
`DELIVRY`, avances = `ReferenceType` `ORDER_ADVANCE` (toute nature), restitutions de garantie = `ORDER_WARRANTY`.
Le réglé d'une pièce exclut les règlements annulés (`CANCELED`) ; une facture annulée compte. Aucun plafonnement : un
dépassement donne un reste négatif, affiché signé. Montants arrondis à 2 décimales (HALF_UP) sur chaque agrégat final.

Présent seulement sur un bon de commande (`nature` = `ORDER`), `null` pour toute autre nature. Un bon de commande sans
pièce liée a tous ses montants à 0, `resteALivrer*` = ses montants et `resteAPayerTotal` = son à payer.
Dans les totaux de la liste : Σ champ à champ des lignes, ratios et codes `null`. Voir FEATURE-achat-bc-agregats.md.

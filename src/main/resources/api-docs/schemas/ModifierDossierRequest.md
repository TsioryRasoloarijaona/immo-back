# ModifierDossierRequest

## concretisation

Onglet Dates — concrétisation (`ComFolder.AdvanceDate`). Premier renseignement libre pour le groupe DATES ; une fois
posée, la modifier ou la vider est réservé à l'Administrateur (groupe PRIX), sinon 403 (`Sale.cs`, droit
`toolModifyAdvanceDate`). Voir `champsVerrouilles` de `GET /api/dossiers/{id}/edition`.

## prixVenteHT

Saisie par le prix de vente HT après remise : le montant de remise devient total HT avant remise − saisie
(`Sale.cs`, setter `PriceAfterDiscountHT`). Exclusif de `prixVenteTTC`, `remiseMontantHT` et `remisePourcentage`
(400 si plusieurs) ; négatif ou supérieur au total HT avant remise ⇒ 400. Mêmes droits et contrôles que la remise
(groupe PRIX, plafonds de l'article, `forcerRemise`).

## prixVenteTTC

Saisie par le prix de vente TTC après remise : le taux de remise devient 1 − saisie / total TTC avant remise
(`Sale.cs`, setter `PriceAfterDiscount`), le montant HT en découle. Exclusif de `prixVenteHT`, `remiseMontantHT` et
`remisePourcentage` (400 si plusieurs) ; négatif ou supérieur au total TTC avant remise ⇒ 400. Mêmes droits et
contrôles que la remise.

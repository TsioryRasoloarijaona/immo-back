# SchemaFinancementDto

## prixAvantRemise

Prix TTC avant remise : article principal + annexes (ex. parking). Exactement la valeur que `POST
/api/dossiers` enregistrera (`prixAvantRemise` du dossier) avec les mêmes paramètres.

## montantRemise

Montant **TTC** de la remise, calculé sur `prixAvantRemise` (principal + annexes) : `prixAvantRemise`
− `prixApresRemise`. Attention : le dossier créé expose `remiseMontant` en **HT** (convention du
logiciel de bureau) ; ce montant TTC en est le pendant toutes taxes comprises.

## remisePercent

Taux de remise en pourcentage (5 = 5 %), jamais un ratio 0–1. De 0 à 100 quand il est saisi
(`remisePercent` de la requête) ; déduit de l'écart entre le prix de référence de l'article (+ annexes)
et le `prix` négocié (rapporté au HT), il peut être négatif (prix négocié > prix de référence).
Exactement la valeur que le dossier créé exposera dans `remise` (`ComFolder.Discount` en base est le
même taux en ratio 0–1, comme l'écrit le logiciel de bureau).

## prixApresRemise

Prix TTC après remise (`prixAvantRemise` − `montantRemise`), base du schéma de financement. Exactement
la valeur que `POST /api/dossiers` enregistrera (`prixApresRemise` du dossier) avec les mêmes paramètres.

# SchemaFinancementDto

## prixAvantRemise

Prix TTC avant remise : article principal + annexes (ex. parking). Exactement la valeur que `POST
/api/dossiers` enregistrera (`prixAvantRemise` du dossier) avec les mêmes paramètres.

## montantRemise

Montant TTC de la remise, calculé sur `prixAvantRemise` (principal + annexes). Exactement la valeur
que `POST /api/dossiers` enregistrera (`remiseMontant` du dossier) avec les mêmes paramètres.

## remisePercent

Taux de remise en pourcentage (5 = 5 %), jamais un ratio 0–1. De 0 à 100 quand il est saisi
(`remisePercent` de la requête) ; déduit de l'écart entre le prix de référence de l'article (+ annexes)
et le `prix` négocié, il peut être négatif (prix négocié > prix de référence). Exactement la valeur
que `POST /api/dossiers` enregistrera dans `ComFolder.Discount`.

## prixApresRemise

Prix TTC après remise (`prixAvantRemise` − `montantRemise`), base du schéma de financement. Exactement
la valeur que `POST /api/dossiers` enregistrera (`prixApresRemise` du dossier) avec les mêmes paramètres.

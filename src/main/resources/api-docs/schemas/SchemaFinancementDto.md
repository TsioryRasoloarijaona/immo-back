# SchemaFinancementDto

## prixAvantRemise

Prix TTC avant remise : article principal + annexes (ex. parking). Exactement la valeur que `POST
/api/dossiers` enregistrera (`prixAvantRemise` du dossier) avec les mêmes paramètres.

## montantRemise

Montant TTC de la remise, calculé sur `prixAvantRemise` (principal + annexes). Exactement la valeur
que `POST /api/dossiers` enregistrera (`remiseMontant` du dossier) avec les mêmes paramètres.

## remisePercent

Taux de remise en pourcentage, de 0 à 100 (5 = 5 %), jamais un ratio 0–1. Le taux saisi s'il a été
fourni ; sinon le taux déduit de l'écart entre le prix de référence de l'article (+ annexes) et le
`prix` négocié. Exactement la valeur que `POST /api/dossiers` enregistrera dans `ComFolder.Discount`.

## prixApresRemise

Prix TTC après remise (`prixAvantRemise` − `montantRemise`), base du schéma de financement. Exactement
la valeur que `POST /api/dossiers` enregistrera (`prixApresRemise` du dossier) avec les mêmes paramètres.

# SimulerSchemaFinancementRequest

## prix

Total TTC envisagé, article principal + annexes. Avec `remisePercent` : total AVANT remise (le
principal vaut `prix` − prix des annexes ; `prix` inférieur au prix des annexes ⇒ 400). Sans
`remisePercent` : prix négocié, utilisé tel quel. Même sens que dans `POST /api/dossiers`. Obligatoire.

## remisePercent

Remise commerciale en % (0 à 100), optionnelle. Fournie ⇒ le schéma est calculé sur le prix APRÈS
remise (article principal + annexes − remise), exactement comme le fera `POST /api/dossiers` avec
les mêmes paramètres.

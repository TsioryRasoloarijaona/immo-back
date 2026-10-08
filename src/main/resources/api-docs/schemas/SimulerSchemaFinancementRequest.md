# SimulerSchemaFinancementRequest

## prix

Prix de vente TTC envisagé. Avec `remisePercent` : prix AVANT remise. Sans `remisePercent` : prix
négocié, utilisé tel quel. Obligatoire.

## remisePercent

Remise commerciale en % (0 à 100), optionnelle. Fournie ⇒ le schéma est calculé sur le prix APRÈS
remise (article principal + annexes − remise), exactement comme le fera `POST /api/dossiers` avec
les mêmes paramètres.

# MontantsDossierDto

## prixVente

Ligne « Prix de vente » de l'écran légataire.

`montant` est le prix stocké sur le dossier (`ComFolder.Price`), **pas** une re-dérivation de
l'échéancier : les deux coïncident sur 327 des 328 dossiers actifs, et c'est le prix stocké qui fait
foi.

`regle` agrège les règlements des échéances **non exclues du prix**
(`ComDocument.excludeFromPrice`), donc hors frais.

## netAPayer

Ligne « Net à payer » de l'écran légataire — le prix de vente augmenté des frais exclus du prix.

⚠️ **Sur un dossier sorti** (`ANNULATION` ou `DESISTEMENT`), le montant se réduit à ce qui a déjà
été encaissé et le reste tombe à **zéro** : un dossier annulé ne reste rien devoir. Règle vérifiée
sur les 30 dossiers sortis réels, sans exception. Ce n'est pas une donnée manquante mais la règle
métier.

## dossierSorti

`true` si le dossier est **sorti** du parcours — statut `ANNULATION` ou `DESISTEMENT`. Correspond
exactement au `IsCancellation` du logiciel de bureau (30/30 sur les dossiers réels). Explique la
règle particulière de `netAPayer`.

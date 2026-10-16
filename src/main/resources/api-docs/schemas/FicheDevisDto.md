# FicheDevisDto

## dossierLie

Dossier créé depuis cette offre : dossier de vente (`ComFolder.StlQuoteId`) en priorité, sinon dossier
de location (`RntFolder.StlQuoteId`). null si l'offre n'a donné aucun dossier. Une offre liée reste
modifiable (comme OMRAN) mais ne peut plus être supprimée (DELETE → 409).

## schemaFinancementSaisi

true si OMRAN a saisi un schéma de financement sur l'offre (lignes `StlQuoteDocument`) : c'est alors ce
schéma que montre `offre.schemaFinancement`, et toute modification qui change le prix de vente TTC
après remise est refusée (400) tant que la somme AVANCE + CRÉDIT + AUTO-FINANCEMENT du schéma ne
l'égale plus, comme `VerifSchema` d'OMRAN. false : le schéma est calculé depuis la tranche.

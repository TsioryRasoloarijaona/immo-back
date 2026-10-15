# DevisDto

## prospectionCode

Code de la prospection d'origine — pour la colonne "Prospection" et le lien cliquable vers la
prospection dans la liste globale des offres (GET /api/devis).

## schemaFinancement

Plan de financement de l'offre. Si OMRAN a saisi un schéma sur l'offre (lignes StlQuoteDocument),
ce sont ces lignes, telles quelles (catégorie, montant, exclusion du prix de la catégorie). Sinon,
le plan (AUTO-FINANCEMENT / AVANCE / FRAIS SYNDIC) est calculé sur le prix de vente TTC après
remise, même calcul que POST /api/dossiers/simulation-financement : il suit donc toute
modification du prix ou de la remise. Vide si l'offre n'a pas de prix.

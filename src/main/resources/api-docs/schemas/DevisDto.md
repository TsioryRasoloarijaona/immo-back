# DevisDto

## prospectionCode

Code de la prospection d'origine — pour la colonne "Prospection" et le lien cliquable vers la
prospection dans la liste globale des offres (GET /api/devis).

## schemaFinancement

Plan de financement (AUTO-FINANCEMENT / AVANCE / FRAIS SYNDIC) calculé sur le prix de vente TTC
après remise de l'offre, même calcul que POST /api/dossiers/simulation-financement. Vide si l'offre
n'a pas de prix.

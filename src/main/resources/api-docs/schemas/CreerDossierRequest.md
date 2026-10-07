# CreerDossierRequest

## prix

Prix de vente TTC. **Avec `remisePercent`** : prix AVANT remise (remplace le prix commercial de
l'article principal comme base de calcul, même rôle que le `prix` d'une Offre) — le prix réellement
dû est recalculé en appliquant la remise. **Sans `remisePercent`** (mode historique) : prix négocié
APRÈS remise, et la remise est alors déduite de l'écart avec le prix commercial de l'article.
Optionnel : sans lui, aucun montant n'est calculé et aucun schéma de financement n'est généré.

## remisePercent

Remise commerciale en pourcentage (0 à 100), saisie par le commercial. Optionnel. Fournie ⇒ elle est
appliquée telle quelle sur le prix avant remise (article principal + annexes) et se répercute sur
`prixApresRemise`, `totalAPayer` **et** sur le schéma de financement généré (AVANCE /
AUTO-FINANCEMENT calculés sur le prix après remise). Absente ⇒ comportement historique inchangé, la
remise est déduite du prix négocié.

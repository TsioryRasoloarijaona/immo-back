# CreerDossierRequest

## prix

Total TTC de la vente, **article principal + annexes** (ex. parking), dans les deux modes.
**Avec `remisePercent`** : total AVANT remise — le prix du principal en est déduit (`prix` − prix
des annexes, qui gardent leur prix de référence) et le prix réellement dû est recalculé en appliquant
la remise. `prix` inférieur au prix des annexes ⇒ 400. **Sans `remisePercent`** (mode historique) :
prix négocié APRÈS remise, et la remise est déduite de l'écart avec le prix de référence (principal +
annexes). Ce n'est pas le `prix` d'une Offre (prix du principal seul). Optionnel : sans lui, aucun
montant n'est calculé et aucun schéma de financement n'est généré.

## remisePercent

Remise commerciale en pourcentage (0 à 100), saisie par le commercial. Optionnel. Fournie ⇒ elle est
appliquée telle quelle sur le prix avant remise (article principal + annexes) et se répercute sur
`prixApresRemise`, `totalAPayer` **et** sur le schéma de financement généré (AVANCE /
AUTO-FINANCEMENT calculés sur le prix après remise). Absente ⇒ comportement historique inchangé, la
remise est déduite du prix négocié.

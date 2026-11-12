# PositionFournisseurDto

## soldeDebut

Σ crédits − Σ débits des mouvements datés **avant** `du`, chaque somme arrondie à 3 décimales. Vaut 0 quand `du` est
absent (comme OMRAN : sans début de période, aucun solde de début).

## netAPayer

Σ des crédits datés de `du` à `au` inclus (borne absente = ouverte) : factures et documents à régler au net à payer (avoirs
non financiers réajoutés), règlements au crédit. Les avoirs ne sont pas soustraits ici (ils sont au débit).

## regle

Σ des débits de la même fenêtre : règlements au débit (rattachés ou non à une pièce, annulés compris) et avoirs.

## soldeFin

Σ crédits − Σ débits des mouvements datés jusqu'à `au` inclus, ou de **tout l'historique** si `au` est absent. N'est pas
`soldeDebut + netAPayer − regle` quand `du` est absent.

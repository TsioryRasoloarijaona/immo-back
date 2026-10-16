# ReajustementSuperficiesController

Écran OMRAN « Réajuster les superficies », lancé depuis la liste des dossiers pour une tranche. « Le système n'effectue des modifications que lorsqu'on change la superficie » (aide OMRAN) : un dossier est proposé quand la superficie de son article principal, ou d'un de ses sous-articles, diffère du stock (arrondie à 2 décimales). La remise est recalculée comme pour la réinitialisation (montant conservé, taux recalculé) ; un sous-article dont la superficie change reprend aussi son prix (prix TTC du stock / (1 + TVA)). Dossiers annulés ou désistés exclus. Réservé au rôle Administrateur.

## verifier

Un dossier n'apparaît que si le nouveau prix de vente TTC après remise diffère du total avance + crédit +
autofinancement de son schéma de financement (écart arrondi à 2 décimales) : sans écart, le légataire écarte
la ligne. `ligneCible` dit où ira l'écart : la dernière ligne d'autofinancement, sinon la dernière ligne de
crédit, sinon une nouvelle ligne d'autofinancement datée du jour.

Un dossier dont le nouveau prix ne peut pas être calculé est listé avec `motifExclusion` et sans prix, écart
ni ligne cible : prix du m² HT du dossier absent ou nul (dossier saisi au forfait, le prix tomberait à 0),
prix de stock absent pour un sous-article dont la superficie change, ou nouveau prix nul ou négatif. Il ne
peut pas être coché.

## valider

Applique, pour chaque dossier coché, ce que `GET` annonçait : nouvelles superficies et prix, écart ajouté (ou
retranché) à la ligne cible du schéma de financement, avec la note « Document modifié lors du réajustement des
superficies par (utilisateur) le (date et heure) ». Tout ou rien : chaque dossier est vérifié avant la première
écriture. Le dossier est signé (`UserUpdate*`). Les lignes d'échéancier sont écrites directement, avec les
mêmes colonnes qu'une échéance ajoutée à la main.

### reponse 409

Un dossier coché n'a aucun écart de superficie à réajuster (ou plus d'écart dans son schéma), porte un
`motifExclusion` (rien n'est écrit), ou aucune catégorie d'autofinancement n'existe pour sa vocation alors qu'une ligne doit être créée.

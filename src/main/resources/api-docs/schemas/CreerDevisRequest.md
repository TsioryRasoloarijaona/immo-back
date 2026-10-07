# CreerDevisRequest

## articleId

Article principal de l'offre. Si renseigné, sert de base au calcul HT/TTC/TVA/prix au m². Optionnel
(offre libre sans ventilation sinon).

## sousArticleIds

Sous-articles inclus dans l'offre (leur prix commercial est sommé dans "prix sous-articles"). Ignoré
si articleId est absent.

## prix

Prix de vente TTC. Obligatoire si articleId est absent (offre libre); sinon optionnel et remplace le
prix commercial de l'article s'il est fourni.

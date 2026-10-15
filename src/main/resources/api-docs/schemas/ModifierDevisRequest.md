# ModifierDevisRequest

## articleId

Nouvel article principal. Mêmes contrôles qu'à la création : article de la tranche (la nouvelle si
`trancheId` est aussi fourni) et non vendu. L'article déjà porté par l'offre n'est pas recontrôlé
comme « vendu ». Refusé (400) si l'offre a déjà donné un dossier : l'article est occupé par ce
dossier. Changer d'article sans `sousArticleIds` retire les sous-articles. Une offre avec article ne
peut pas redevenir une offre libre.

## sousArticleIds

Liste complète des sous-articles (remplace l'ancienne) ; tous doivent être des enfants de l'article.
Liste vide = aucun sous-article.

## prix

Prix TTC qui remplace le prix commercial de l'article (ou prix de vente TTC d'une offre libre).
Absent : le prix actuel de l'offre est conservé, sauf si l'article change (prix commercial du
nouvel article).

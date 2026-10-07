# EncaissementDetailDto

Fiche complète d'un règlement : ses champs, plus les échéances qu'il solde. Contrairement aux lignes
de la liste, cette réponse n'est pas filtrée par `?colonnes=`.

## affectations

Les échéances que ce règlement solde, **montant affecté croissant** — l'ordre de l'écran légataire,
qui trie ses lignes par `Amount ASC`.

Jamais `null` : un tableau vide signifie un règlement encaissé mais pas encore imputé. Sur les
données réelles, un règlement du périmètre Vente en porte de une à trois.

La somme des `montantAffecte` vaut `montantBrut - montantBrutRestant`.

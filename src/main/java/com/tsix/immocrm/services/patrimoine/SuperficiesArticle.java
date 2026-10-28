package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.entities.stk.StkItem;

import java.math.BigDecimal;

/** Superficie titrée d'un article, seule règle de la fiche et de la liste (colonne TitledArea de StkItemView). */
final class SuperficiesArticle {

    private SuperficiesArticle() {
    }

    /** Surface de l'article s'il est titré, plus {@code sousArticlesTitres} = Σ des surfaces de ses sous-articles titrés. */
    static BigDecimal titree(StkItem a, BigDecimal sousArticlesTitres) {
        BigDecimal propre = Boolean.TRUE.equals(a.getIsTitled()) ? zero(a.getArea()) : BigDecimal.ZERO;
        return propre.add(zero(sousArticlesTitres));
    }

    static BigDecimal zero(BigDecimal valeur) {
        return valeur != null ? valeur : BigDecimal.ZERO;
    }
}

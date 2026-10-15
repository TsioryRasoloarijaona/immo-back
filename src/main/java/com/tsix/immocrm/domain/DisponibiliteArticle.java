package com.tsix.immocrm.domain;

import com.tsix.immocrm.entities.stk.StkItem;

/**
 * Un article est occupé (vendu) tant qu'un dossier le porte ({@code StkItem.ComFolderId}) : l'annulation et le
 * désistement libèrent le lot ({@code LiberationLot}). Règle commune à la création de dossier et d'offre.
 */
public final class DisponibiliteArticle {

    private DisponibiliteArticle() {
    }

    public static boolean estOccupe(StkItem article) {
        return article.getComFolderId() != null;
    }
}

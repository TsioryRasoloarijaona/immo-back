package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.domain.PrixArticle;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stl.StlQuoteItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Prix avant remise des sous-articles d'un dossier : ceux figés dans l'offre d'origine ({@code StlQuoteItem}) quand
 * le sous-article y figure, sinon le prix catalogue du jour. Seule source pour les montants et les {@code ComFolderItem}.
 */
public record PrixSousArticles(Map<UUID, StlQuoteItem> lignesOffre) {

    public static final PrixSousArticles CATALOGUE = new PrixSousArticles(Map.of());

    public static PrixSousArticles deLOffre(List<StlQuoteItem> lignes) {
        return new PrixSousArticles(lignes.stream()
                .filter(ligne -> ligne.getStkItem() != null)
                .collect(Collectors.toUnmodifiableMap(ligne -> ligne.getStkItem().getId(), ligne -> ligne,
                        (premiere, doublon) -> premiere)));
    }

    public Optional<StlQuoteItem> ligne(StkItem sousArticle) {
        // Map immuable : get(null) lève, d'où le test d'id (article pas encore enregistré).
        return sousArticle.getId() == null ? Optional.empty() : Optional.ofNullable(lignesOffre.get(sousArticle.getId()));
    }

    public BigDecimal ttc(StkItem sousArticle) {
        return ligne(sousArticle).map(StlQuoteItem::getPriceBeforeDiscount)
                .orElseGet(() -> PrixArticle.ttcOuZero(sousArticle));
    }

    public BigDecimal ht(StkItem sousArticle, BigDecimal tvaParDefaut) {
        return ligne(sousArticle).map(StlQuoteItem::getPriceBeforeDiscountHT)
                .orElseGet(() -> PrixArticle.htOuZero(sousArticle,
                        sousArticle.getVat() != null ? sousArticle.getVat() : tvaParDefaut));
    }
}

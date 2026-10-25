package com.tsix.immocrm.services.perimetre;

import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.repositories.cfg.TrancheVisibleRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Tranche ou article désigné dans le corps d'une écriture (FEATURE-patrimoine-reliquat-v2.md §B) : inconnu ou d'une
 * entité inactive → le même 400 sur le champ, sans code ni mention de l'entité, à lire avant le contrôle de périmètre.
 */
@Component
public class ObjetsVisibles {

    public static final String TRANCHE_ID = "trancheId";
    public static final String ARTICLE_ID = "articleId";

    private final TrancheVisibleRepository tranches;
    private final StkItemRepository articles;

    public ObjetsVisibles(TrancheVisibleRepository tranches, StkItemRepository articles) {
        this.tranches = tranches;
        this.articles = articles;
    }

    public CfgTranche tranche(UUID id) {
        return tranches.trancheVisible(id)
                .orElseThrow(() -> new InvalidRequestException(TRANCHE_ID + " : tranche introuvable."));
    }

    public StkItem article(UUID id, String champ) {
        return articles.findVisible(id).orElseThrow(() -> new InvalidRequestException(champ + " : article introuvable."));
    }

    public List<StkItem> articles(List<UUID> ids, String champ) {
        return ids.stream().map(id -> article(id, champ)).toList();
    }

    /** Article visible et rangé sous {@code tranche}. */
    public StkItem articleDeLaTranche(UUID id, CfgTranche tranche) {
        StkItem article = article(id, ARTICLE_ID);
        verifierTranche(article, tranche);
        return article;
    }

    public static void verifierTranche(StkItem article, CfgTranche tranche) {
        if (article.getStkHierarchy() == null || !tranche.getId().equals(article.getStkHierarchy().getId())) {
            throw new InvalidRequestException(ARTICLE_ID + " : l'article n'appartient pas à la tranche.");
        }
    }
}

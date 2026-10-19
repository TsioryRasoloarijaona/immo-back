package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.dossier.actions.ActionDossierDto;
import com.tsix.immocrm.dto.dossier.actions.CreerActionDossierRequest;
import com.tsix.immocrm.repositories.com.ActionDossierRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.services.dossier.fiche.DossierActionsService;
import com.tsix.immocrm.services.patrimoine.PorteursPatrimoine.Porteur;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Actions (GEN0005) d'un article : le modèle d'action des dossiers, porté par {@code StkItemId}. */
@Service
public class ActionsArticleService {

    private final PorteursPatrimoine porteurs;
    private final ActionDossierRepository actionRepository;
    private final StkItemRepository itemRepository;
    private final DossierActionsService dossierActions;

    public ActionsArticleService(PorteursPatrimoine porteurs, ActionDossierRepository actionRepository,
                                 StkItemRepository itemRepository, DossierActionsService dossierActions) {
        this.porteurs = porteurs;
        this.actionRepository = actionRepository;
        this.itemRepository = itemRepository;
        this.dossierActions = dossierActions;
    }

    @Transactional(readOnly = true)
    public List<ActionDossierDto> lister(UUID articleId) {
        porteurs.exigerVisible(Porteur.ARTICLE, articleId);
        return actionRepository.actionsDeLArticle(articleId).stream().map(DossierActionsService::versDto).toList();
    }

    @Transactional
    public ActionDossierDto creer(UUID articleId, CreerActionDossierRequest request) {
        porteurs.exigerVisible(Porteur.ARTICLE, articleId);
        return dossierActions.creerSurArticle(itemRepository.getReferenceById(articleId), request);
    }
}

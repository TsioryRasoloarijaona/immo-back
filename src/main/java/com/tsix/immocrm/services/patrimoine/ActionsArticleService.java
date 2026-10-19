package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.dossier.actions.ActionDossierDto;
import com.tsix.immocrm.dto.dossier.actions.CreerActionDossierRequest;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.services.dossier.fiche.DossierActionsService;
import com.tsix.immocrm.services.patrimoine.PorteursPatrimoine.Porteur;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Actions (GEN0005) d'un article : le modèle d'action des dossiers, porté par {@code StkItemId}. */
@Service
public class ActionsArticleService {

    private final PorteursPatrimoine porteurs;
    private final ComActionMessageRepository actionRepository;
    private final StkItemRepository itemRepository;
    private final DossierActionsService dossierActions;
    private final PerimetreCommercialService perimetreService;

    public ActionsArticleService(PorteursPatrimoine porteurs, ComActionMessageRepository actionRepository,
                                 StkItemRepository itemRepository, DossierActionsService dossierActions,
                                 PerimetreCommercialService perimetreService) {
        this.porteurs = porteurs;
        this.actionRepository = actionRepository;
        this.itemRepository = itemRepository;
        this.dossierActions = dossierActions;
        this.perimetreService = perimetreService;
    }

    /** Les actions d'un dossier ou d'une prospection de l'article restent soumises au périmètre commercial. */
    @Transactional(readOnly = true)
    public List<ActionDossierDto> lister(UUID articleId) {
        porteurs.exigerVisible(Porteur.ARTICLE, articleId);
        Specification<ComActionMessage> deLArticle = Specification
                .<ComActionMessage>where((root, query, cb) -> cb.equal(root.get("stkItem").get("id"), articleId))
                .and(perimetreService.filtreInteractions());
        return actionRepository.findAll(deLArticle, Sort.by(Sort.Direction.DESC, "docDate")).stream()
                .map(DossierActionsService::versDto).toList();
    }

    @Transactional
    public ActionDossierDto creer(UUID articleId, CreerActionDossierRequest request) {
        porteurs.exigerVisible(Porteur.ARTICLE, articleId);
        return dossierActions.creerSurArticle(itemRepository.getReferenceById(articleId), request);
    }
}

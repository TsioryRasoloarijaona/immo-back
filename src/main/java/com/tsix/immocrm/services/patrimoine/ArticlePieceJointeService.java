package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.dossier.PieceJointeDto;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.services.piecejointe.PieceJointeSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * {@code /api/articles/{id}/pieces-jointes[/...]} — accès en lecture aux pièces jointes d'un
 * Article ({@code AdmAttachedFile}, {@code CategoryId = StkItem.PKey}). Read-only for now — no
 * upload endpoint, unlike {@code PieceJointeService} (Dossier) which also handles deposits: the
 * real data behind this is legacy plans (PLAN/PLAN_ARCHITECTE/PLAN_ETAGE), not something this
 * app currently produces, so a write path wasn't asked for. Add one by calling
 * {@link PieceJointeSupport#deposer} if/when needed.
 *
 * <p>Toute la mécanique commune vit dans {@link PieceJointeSupport} ; il ne reste ici que ce qui
 * est propre à l'Article (vérification d'existence + libellé d'erreur).
 */
@Service
@Transactional(readOnly = true)
public class ArticlePieceJointeService {

    private final StkItemRepository itemRepository;
    private final PieceJointeSupport support;

    public ArticlePieceJointeService(StkItemRepository itemRepository, PieceJointeSupport support) {
        this.itemRepository = itemRepository;
        this.support = support;
    }

    /** Same "IsCheckedButNotAttached" exclusion as {@code PieceJointeService#findByDossier}. */
    public List<PieceJointeDto> findByArticle(UUID articleId) {
        if (!itemRepository.existsById(articleId)) {
            throw new ResourceNotFoundException("Article introuvable: " + articleId);
        }
        return support.lister(articleId);
    }

    /** URL présignée de téléchargement — 404 si la pièce n'appartient pas à cet article. */
    public URI urlFichier(UUID articleId, UUID pieceId) {
        if (!itemRepository.existsById(articleId)) {
            throw new ResourceNotFoundException("Article introuvable: " + articleId);
        }
        return support.urlTelechargement(articleId, pieceId, "cet article");
    }
}

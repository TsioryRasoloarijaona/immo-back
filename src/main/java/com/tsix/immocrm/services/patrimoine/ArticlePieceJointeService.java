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

/** Pièces jointes d'un article ({@code AdmAttachedFile.CategoryId = StkItem.PKey}), en lecture seule. */
@Service
@Transactional(readOnly = true)
public class ArticlePieceJointeService {

    private final StkItemRepository itemRepository;
    private final PieceJointeSupport support;

    public ArticlePieceJointeService(StkItemRepository itemRepository, PieceJointeSupport support) {
        this.itemRepository = itemRepository;
        this.support = support;
    }

    public List<PieceJointeDto> findByArticle(UUID articleId) {
        if (!itemRepository.existsById(articleId)) {
            throw new ResourceNotFoundException("Article introuvable: " + articleId);
        }
        return support.lister(articleId);
    }

    /** 404 si la pièce n'appartient pas à cet article. */
    public URI urlFichier(UUID articleId, UUID pieceId) {
        if (!itemRepository.existsById(articleId)) {
            throw new ResourceNotFoundException("Article introuvable: " + articleId);
        }
        return support.urlTelechargement(articleId, pieceId, "cet article");
    }
}

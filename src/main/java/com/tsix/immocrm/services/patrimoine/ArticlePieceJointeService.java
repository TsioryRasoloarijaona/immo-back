package com.tsix.immocrm.services.patrimoine;

import com.tsix.immocrm.dto.dossier.PieceJointeDto;
import com.tsix.immocrm.entities.adm.AdmAttachedFile;
import com.tsix.immocrm.entities.adm.AdmAttachedFileSummary;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmAttachedFileRepository;
import com.tsix.immocrm.repositories.adm.AdmAttachedFileSummaryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * {@code /api/articles/{id}/pieces-jointes[/...]} — accès en lecture aux pièces jointes d'un
 * Article ({@code AdmAttachedFile}, {@code CategoryId = StkItem.PKey}). Read-only for now — no
 * upload endpoint, unlike {@code PieceJointeService} (Dossier) which also handles deposits: the
 * real data behind this is legacy plans (PLAN/PLAN_ARCHITECTE/PLAN_ETAGE), not something this
 * app currently produces, so a write path wasn't asked for. Add one mirroring
 * {@code PieceJointeService#create} if/when needed.
 */
@Service
@Transactional(readOnly = true)
public class ArticlePieceJointeService {

    private final StkItemRepository itemRepository;
    private final AdmAttachedFileSummaryRepository summaryRepository;
    private final AdmAttachedFileRepository fileRepository;

    public ArticlePieceJointeService(StkItemRepository itemRepository, AdmAttachedFileSummaryRepository summaryRepository,
                                      AdmAttachedFileRepository fileRepository) {
        this.itemRepository = itemRepository;
        this.summaryRepository = summaryRepository;
        this.fileRepository = fileRepository;
    }

    /** Same "IsCheckedButNotAttached" exclusion as {@code PieceJointeService#findByDossier}. */
    public List<PieceJointeDto> findByArticle(UUID articleId) {
        if (!itemRepository.existsById(articleId)) {
            throw new ResourceNotFoundException("Article introuvable: " + articleId);
        }
        return summaryRepository.findByArticleId(articleId).stream()
                .filter(a -> Boolean.TRUE.equals(a.getHasFile()))
                .map(ArticlePieceJointeService::toDto)
                .toList();
    }

    /** Fetches the binary content — 404 if it doesn't belong to this article or has no file attached. */
    public AdmAttachedFile findFichier(UUID articleId, UUID pieceId) {
        if (!itemRepository.existsById(articleId)) {
            throw new ResourceNotFoundException("Article introuvable: " + articleId);
        }
        AdmAttachedFile fichier = fileRepository.findByIdAndCategoryId(pieceId, articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Pièce jointe introuvable pour cet article: " + pieceId));
        if (fichier.getAttachedFile() == null) {
            throw new ResourceNotFoundException("Aucun fichier attaché pour cette pièce jointe: " + pieceId);
        }
        return fichier;
    }

    private static PieceJointeDto toDto(AdmAttachedFileSummary a) {
        String type = a.getType() != null ? a.getType().getDescription() : null;
        return new PieceJointeDto(a.getId(), a.getDescription(), a.getCategory(), type, a.getExtension(),
                a.getUserCreateDateTime(), a.getUserCreate());
    }
}

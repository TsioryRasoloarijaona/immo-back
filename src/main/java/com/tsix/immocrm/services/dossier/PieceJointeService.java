package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.dto.dossier.PieceJointeDto;
import com.tsix.immocrm.entities.adm.AdmAttachedFile;
import com.tsix.immocrm.entities.adm.AdmAttachedFileSummary;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmAttachedFileRepository;
import com.tsix.immocrm.repositories.adm.AdmAttachedFileSummaryRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * {@code GET /api/dossiers/{id}/pieces-jointes[/...]} — read-only access to a Dossier's
 * pièces jointes ({@code AdmAttachedFile}). See {@link AdmAttachedFileSummary}'s doc-comment
 * for how the linkage to {@code ComFolder} was confirmed (no declared FK in the DB).
 */
@Service
@Transactional(readOnly = true)
public class PieceJointeService {

    private final ComFolderRepository folderRepository;
    private final AdmAttachedFileSummaryRepository summaryRepository;
    private final AdmAttachedFileRepository fileRepository;

    public PieceJointeService(ComFolderRepository folderRepository, AdmAttachedFileSummaryRepository summaryRepository,
                               AdmAttachedFileRepository fileRepository) {
        this.folderRepository = folderRepository;
        this.summaryRepository = summaryRepository;
        this.fileRepository = fileRepository;
    }

    /**
     * Excludes rows with {@code hasFile = false} (real data: "IsCheckedButNotAttached"
     * checklist placeholders — {@code Category} legitimately starts with "Dossier de vente"
     * and {@code CategoryId} matches, but no file was ever actually uploaded).
     */
    public List<PieceJointeDto> findByDossier(UUID dossierId) {
        if (!folderRepository.existsById(dossierId)) {
            throw new ResourceNotFoundException("Dossier introuvable: " + dossierId);
        }
        return summaryRepository.findByDossierId(dossierId).stream()
                .filter(a -> Boolean.TRUE.equals(a.getHasFile()))
                .map(PieceJointeService::toDto)
                .toList();
    }

    /**
     * Fetches the binary content for viewing/downloading — 404 if it doesn't belong to this
     * dossier, or if it's a checklist placeholder with no actual file attached (see
     * {@link #findByDossier}).
     */
    public AdmAttachedFile findFichier(UUID dossierId, UUID pieceId) {
        if (!folderRepository.existsById(dossierId)) {
            throw new ResourceNotFoundException("Dossier introuvable: " + dossierId);
        }
        AdmAttachedFile fichier = fileRepository.findByIdAndCategoryId(pieceId, dossierId)
                .orElseThrow(() -> new ResourceNotFoundException("Pièce jointe introuvable pour ce dossier: " + pieceId));
        if (fichier.getAttachedFile() == null) {
            throw new ResourceNotFoundException("Aucun fichier attaché pour cette pièce jointe: " + pieceId);
        }
        return fichier;
    }

    private static PieceJointeDto toDto(AdmAttachedFileSummary a) {
        return new PieceJointeDto(a.getId(), a.getDescription(), a.getExtension(), a.getUserCreateDateTime());
    }
}

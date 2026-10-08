package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.dto.dossier.PieceJointeDto;
import com.tsix.immocrm.entities.adm.AdmAttachedFileSummary;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.services.piecejointe.PieceJointeSupport;
import com.tsix.immocrm.storage.PorteurPieceJointe;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Pièces jointes d'un dossier, rattachées sans FK (voir {@link AdmAttachedFileSummary}) : seul ce qui
 * lui est propre, la mécanique est dans {@link PieceJointeSupport}.
 */
@Service
@Transactional(readOnly = true)
public class PieceJointeService {

    private static final DateTimeFormatter DATE_LEGACY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ComFolderRepository folderRepository;
    private final PieceJointeSupport support;
    private final Clock clock;

    public PieceJointeService(ComFolderRepository folderRepository, PieceJointeSupport support, Clock clock) {
        this.folderRepository = folderRepository;
        this.support = support;
        this.clock = clock;
    }

    public List<PieceJointeDto> findByDossier(UUID dossierId) {
        if (!folderRepository.existsById(dossierId)) {
            throw new ResourceNotFoundException("Dossier introuvable: " + dossierId);
        }
        return support.lister(dossierId);
    }

    public URI urlFichier(UUID dossierId, UUID pieceId) {
        if (!folderRepository.existsById(dossierId)) {
            throw new ResourceNotFoundException("Dossier introuvable: " + dossierId);
        }
        return support.urlTelechargement(dossierId, pieceId, "ce dossier");
    }

    /** {@code Category} au format legacy, par cohérence seulement : la relecture filtre sur {@code CategoryId}. */
    @Transactional
    public PieceJointeDto create(UUID dossierId, MultipartFile fichier, String description, UUID typeId,
                                  AuthenticatedUser principal) {
        ComFolder folder = folderRepository.findById(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable: " + dossierId));
        String category = "Dossier de vente N° " + folder.getCode() + " du " + LocalDateTime.now(clock).format(DATE_LEGACY);
        return support.deposer(dossierId, PorteurPieceJointe.DOSSIER, category, fichier, description,
                typeId, principal);
    }

    /** Suppression logique, sans règle métier côté dossier (contrairement au règlement pointé). */
    @Transactional
    public void supprimer(UUID dossierId, UUID pieceId, AuthenticatedUser principal) {
        if (!folderRepository.existsById(dossierId)) {
            throw new ResourceNotFoundException("Dossier introuvable: " + dossierId);
        }
        support.supprimer(dossierId, pieceId, "ce dossier", principal);
    }
}

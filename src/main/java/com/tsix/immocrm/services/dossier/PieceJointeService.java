package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.dto.dossier.PieceJointeDto;
import com.tsix.immocrm.entities.adm.AdmAttachedFile;
import com.tsix.immocrm.entities.adm.AdmAttachedFileSummary;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.services.piecejointe.PieceJointeSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * {@code /api/dossiers/{id}/pieces-jointes[/...]} — accès (lecture + dépôt) aux pièces jointes
 * d'un Dossier ({@code AdmAttachedFile}). Voir la doc de {@link AdmAttachedFileSummary} pour le
 * rattachement à {@code ComFolder} (confirmé malgré l'absence de FK déclarée en DB).
 *
 * <p>Ne porte plus que ce qui est <b>propre au Dossier</b> — la vérification d'existence du
 * dossier et la convention legacy du {@code Category}. Toute la mécanique commune (liste, binaire,
 * dépôt, DTO) vit dans {@link PieceJointeSupport}, partagée avec les pièces jointes d'Article et
 * de Règlement.
 */
@Service
@Transactional(readOnly = true)
public class PieceJointeService {

    private static final DateTimeFormatter DATE_LEGACY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ComFolderRepository folderRepository;
    private final PieceJointeSupport support;

    public PieceJointeService(ComFolderRepository folderRepository, PieceJointeSupport support) {
        this.folderRepository = folderRepository;
        this.support = support;
    }

    /**
     * Exclut les lignes {@code hasFile = false} (données réelles : cases de checklist
     * "IsCheckedButNotAttached" — {@code Category} commence bien par "Dossier de vente" et
     * {@code CategoryId} correspond, mais aucun fichier n'a jamais été déposé).
     */
    public List<PieceJointeDto> findByDossier(UUID dossierId) {
        if (!folderRepository.existsById(dossierId)) {
            throw new ResourceNotFoundException("Dossier introuvable: " + dossierId);
        }
        return support.lister(dossierId);
    }

    /**
     * Récupère le contenu binaire — 404 si la pièce n'appartient pas à ce dossier, ou s'il s'agit
     * d'une case de checklist sans fichier réel (voir {@link #findByDossier}).
     */
    public AdmAttachedFile findFichier(UUID dossierId, UUID pieceId) {
        if (!folderRepository.existsById(dossierId)) {
            throw new ResourceNotFoundException("Dossier introuvable: " + dossierId);
        }
        return support.fichier(dossierId, pieceId, "ce dossier");
    }

    /**
     * {@code POST /api/dossiers/{id}/pieces-jointes} — dépose un nouveau fichier sur le Dossier.
     * Reproduit la convention legacy du {@code Category} ("Dossier de vente N° {code} du {date}")
     * pour rester cohérent avec les données existantes, mais ce n'est plus une condition de
     * visibilité : {@link com.tsix.immocrm.repositories.adm.AdmAttachedFileSummaryRepository#findByCategoryId}
     * filtre uniquement sur {@code CategoryId} désormais (voir sa doc), donc le fichier réapparaît
     * dans {@link #findByDossier} juste après le dépôt quel que soit le {@code Category}.
     */
    @Transactional
    public PieceJointeDto create(UUID dossierId, MultipartFile fichier, String description, UUID typeId,
                                  AuthenticatedUser principal) {
        ComFolder folder = folderRepository.findById(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable: " + dossierId));
        String category = "Dossier de vente N° " + folder.getCode() + " du " + LocalDateTime.now().format(DATE_LEGACY);
        return support.deposer(dossierId, category, fichier, description, typeId, principal);
    }
}

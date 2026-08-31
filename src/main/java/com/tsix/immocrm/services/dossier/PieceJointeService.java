package com.tsix.immocrm.services.dossier;

import com.tsix.immocrm.dto.dossier.PieceJointeDto;
import com.tsix.immocrm.entities.adm.AdmAttachedFile;
import com.tsix.immocrm.entities.adm.AdmAttachedFileSummary;
import com.tsix.immocrm.entities.adm.AdmAttachedFileType;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmAttachedFileRepository;
import com.tsix.immocrm.repositories.adm.AdmAttachedFileSummaryRepository;
import com.tsix.immocrm.repositories.adm.AdmAttachedFileTypeRepository;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * {@code /api/dossiers/{id}/pieces-jointes[/...]} — accès (lecture + dépôt) aux pièces jointes
 * d'un Dossier ({@code AdmAttachedFile}). Voir la doc de {@link AdmAttachedFileSummary} pour le
 * rattachement à {@code ComFolder} (confirmé malgré l'absence de FK déclarée en DB).
 */
@Service
@Transactional(readOnly = true)
public class PieceJointeService {

    private static final DateTimeFormatter DATE_LEGACY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ComFolderRepository folderRepository;
    private final AdmAttachedFileSummaryRepository summaryRepository;
    private final AdmAttachedFileRepository fileRepository;
    private final AdmUserRepository userRepository;
    private final AdmAttachedFileTypeRepository typeRepository;

    public PieceJointeService(ComFolderRepository folderRepository, AdmAttachedFileSummaryRepository summaryRepository,
                               AdmAttachedFileRepository fileRepository, AdmUserRepository userRepository,
                               AdmAttachedFileTypeRepository typeRepository) {
        this.folderRepository = folderRepository;
        this.summaryRepository = summaryRepository;
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.typeRepository = typeRepository;
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

    /**
     * {@code POST /api/dossiers/{id}/pieces-jointes} — dépose un nouveau fichier sur le Dossier.
     * Reproduit la convention legacy du {@code Category} ("Dossier de vente N° {code} du
     * {date}") pour rester cohérent avec les données existantes, mais ce n'est plus une
     * condition de visibilité : {@link AdmAttachedFileSummaryRepository#findByDossierId} filtre
     * uniquement sur {@code CategoryId} désormais (voir sa doc), donc le fichier réapparaît dans
     * {@link #findByDossier} juste après le dépôt quel que soit le {@code Category}.
     * {@code description} sert de légende libre (ex. "CIN client", "Contrat signé") ; à défaut,
     * le nom de fichier original est utilisé. {@code typeId} référence un {@code AdmAttachedFileType}
     * (voir {@code GET /api/referentiels/types-piece-jointe}) — {@code null} si non précisé, 404
     * si l'id ne correspond à aucun type connu. {@code principal} sert à renseigner
     * {@code UserCreate} (nom affiché de l'utilisateur connecté, résolu via
     * {@code AdmUser.Description} pour rester cohérent avec la convention legacy — ex. "Dounya
     * SAFSAF") ; jamais demandé au front, déduit du token — {@code null} si absent (ex. appel non
     * authentifié dans un contexte de test).
     */
    @Transactional
    public PieceJointeDto create(UUID dossierId, MultipartFile fichier, String description, UUID typeId,
                                  AuthenticatedUser principal) {
        ComFolder folder = folderRepository.findById(dossierId)
                .orElseThrow(() -> new ResourceNotFoundException("Dossier introuvable: " + dossierId));
        if (fichier == null || fichier.isEmpty()) {
            throw new InvalidRequestException("Le fichier est requis et ne peut pas être vide");
        }
        AdmAttachedFileType type = null;
        if (typeId != null) {
            type = typeRepository.findById(typeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Type de pièce jointe introuvable: " + typeId));
        }

        String nomOriginal = fichier.getOriginalFilename();
        String extension = "";
        if (StringUtils.hasText(nomOriginal) && nomOriginal.contains(".")) {
            extension = nomOriginal.substring(nomOriginal.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        }
        String legende = StringUtils.hasText(description) ? description
                : (StringUtils.hasText(nomOriginal) ? nomOriginal : "Pièce jointe");

        AdmAttachedFile entity = new AdmAttachedFile();
        entity.setId(UUID.randomUUID());
        entity.setCategory("Dossier de vente N° " + folder.getCode() + " du " + LocalDateTime.now().format(DATE_LEGACY));
        entity.setCategoryId(dossierId);
        entity.setDescription(legende);
        entity.setExtension(extension);
        entity.setAdmAttachedFileTypeId(typeId);
        entity.setUserCreateDateTime(LocalDateTime.now());
        entity.setUserCreate(resolveDisplayName(principal));
        try {
            entity.setAttachedFile(fichier.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Lecture du fichier échouée", e);
        }
        entity = fileRepository.save(entity);

        return new PieceJointeDto(entity.getId(), entity.getDescription(), entity.getCategory(),
                type != null ? type.getDescription() : null, entity.getExtension(), entity.getUserCreateDateTime(),
                entity.getUserCreate());
    }

    private String resolveDisplayName(AuthenticatedUser principal) {
        if (principal == null) {
            return null;
        }
        return userRepository.findById(principal.admUserId())
                .map(AdmUser::getDescription)
                .filter(StringUtils::hasText)
                .orElse(principal.username());
    }

    private static PieceJointeDto toDto(AdmAttachedFileSummary a) {
        String type = a.getType() != null ? a.getType().getDescription() : null;
        return new PieceJointeDto(a.getId(), a.getDescription(), a.getCategory(), type, a.getExtension(),
                a.getUserCreateDateTime(), a.getUserCreate());
    }
}

package com.tsix.immocrm.services.piecejointe;

import com.tsix.immocrm.dto.dossier.PieceJointeDto;
import com.tsix.immocrm.entities.adm.AdmAttachedFile;
import com.tsix.immocrm.entities.adm.AdmAttachedFileSummary;
import com.tsix.immocrm.entities.adm.AdmAttachedFileType;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmAttachedFileRepository;
import com.tsix.immocrm.repositories.adm.AdmAttachedFileSummaryRepository;
import com.tsix.immocrm.repositories.adm.AdmAttachedFileTypeRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.utils.Champs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Mécanique commune aux pièces jointes, quel que soit l'objet porteur (Dossier, Article,
 * Règlement…).
 *
 * <p>{@code AdmAttachedFile} est polymorphe : le rattachement se fait par
 * {@code CategoryId = <porteur>.PKey}, sans FK déclarée. Tout ce qui suit ce rattachement —
 * lecture de la liste, récupération du binaire, dépôt d'un fichier, conversion en DTO — est
 * strictement identique d'un porteur à l'autre. Seules diffèrent (a) la vérification d'existence
 * du porteur et (b) la chaîne {@code Category} écrite au dépôt, toutes deux laissées à l'appelant.
 *
 * <p>Cette classe existe pour empêcher une 3ᵉ puis 4ᵉ copie : {@code PieceJointeService} (Dossier)
 * et {@code ArticlePieceJointeService} portaient déjà un {@code toDto} <b>identique au caractère
 * près</b>, un {@code findFichier} identique à la chaîne d'erreur près, et deux requêtes JPQL
 * identiques au nom de paramètre près. Même intention que {@code web.ContentTypeParExtension},
 * extrait précisément pour dédupliquer entre contrôleurs.
 */
@Service
@Transactional(readOnly = true)
public class PieceJointeSupport {

    private final AdmAttachedFileSummaryRepository summaryRepository;
    private final AdmAttachedFileRepository fileRepository;
    private final AdmAttachedFileTypeRepository typeRepository;
    private final UtilisateurAffiche utilisateurAffiche;

    public PieceJointeSupport(AdmAttachedFileSummaryRepository summaryRepository,
                              AdmAttachedFileRepository fileRepository,
                              AdmAttachedFileTypeRepository typeRepository,
                              UtilisateurAffiche utilisateurAffiche) {
        this.summaryRepository = summaryRepository;
        this.fileRepository = fileRepository;
        this.typeRepository = typeRepository;
        this.utilisateurAffiche = utilisateurAffiche;
    }

    /**
     * Pièces jointes du porteur, plus récentes d'abord. Exclut les lignes {@code hasFile = false}
     * — en données réelles ce sont des cases de checklist "cochée mais jamais déposée"
     * ({@code IsCheckedButNotAttached}) : {@code CategoryId} correspond bien, mais aucun fichier
     * n'a jamais été téléversé.
     */
    public List<PieceJointeDto> lister(UUID porteurId) {
        return summaryRepository.findByCategoryId(porteurId).stream()
                .filter(a -> Boolean.TRUE.equals(a.getHasFile()))
                .map(PieceJointeSupport::toDto)
                .toList();
    }

    /** {@code true} dès qu'au moins une pièce jointe réelle (binaire présent) porte cet id. */
    public boolean auMoinsUnFichier(UUID porteurId) {
        return summaryRepository.findByCategoryId(porteurId).stream()
                .anyMatch(a -> Boolean.TRUE.equals(a.getHasFile()));
    }

    /**
     * Contenu binaire d'une pièce jointe, borné au porteur : un id de pièce appartenant à un autre
     * porteur donne 404, jamais le fichier d'autrui. {@code libellePorteur} n'entre que dans le
     * message d'erreur ("ce dossier", "ce règlement"…).
     */
    public AdmAttachedFile fichier(UUID porteurId, UUID pieceId, String libellePorteur) {
        AdmAttachedFile fichier = fileRepository.findByIdAndCategoryId(pieceId, porteurId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pièce jointe introuvable pour " + libellePorteur + ": " + pieceId));
        if (fichier.getAttachedFile() == null) {
            throw new ResourceNotFoundException("Aucun fichier attaché pour cette pièce jointe: " + pieceId);
        }
        return fichier;
    }

    /**
     * Dépose un fichier sur le porteur. L'appelant a déjà vérifié que le porteur existe et lui
     * appartient ; il fournit la chaîne {@code category} à écrire (convention legacy propre à
     * chaque type de porteur, purement cosmétique : la relecture ne filtre que sur
     * {@code CategoryId}).
     *
     * <p>{@code description} est une légende libre ; à défaut le nom de fichier original est
     * utilisé. {@code typeId} référence un {@code AdmAttachedFileType} ({@code null} si non
     * précisé, 404 si inconnu). {@code UserCreate} reçoit le nom affiché de l'utilisateur connecté
     * ({@code AdmUser.Description}, convention legacy — ex. "Dounya SAFSAF"), jamais demandé au
     * front et {@code null} si non authentifiable.
     */
    @Transactional
    public PieceJointeDto deposer(UUID porteurId, String category, MultipartFile fichier, String description,
                                  UUID typeId, AuthenticatedUser principal) {
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
        entity.setCategory(category);
        entity.setCategoryId(porteurId);
        entity.setDescription(legende);
        entity.setExtension(extension);
        entity.setAdmAttachedFileTypeId(typeId);
        entity.setUserCreateDateTime(LocalDateTime.now());
        entity.setUserCreate(utilisateurAffiche.nom(principal));
        try {
            entity.setAttachedFile(fichier.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Lecture du fichier échouée", e);
        }
        entity = fileRepository.save(entity);

        return new PieceJointeDto(entity.getId(), entity.getDescription(), entity.getCategory(),
                Champs.de(type, AdmAttachedFileType::getDescription), entity.getExtension(),
                entity.getUserCreateDateTime(), entity.getUserCreate());
    }

    public static PieceJointeDto toDto(AdmAttachedFileSummary a) {
        String type = Champs.de(a.getType(), AdmAttachedFileType::getDescription);
        return new PieceJointeDto(a.getId(), a.getDescription(), a.getCategory(), type, a.getExtension(),
                a.getUserCreateDateTime(), a.getUserCreate());
    }
}

package com.tsix.immocrm.services.piecejointe;

import com.tsix.immocrm.dto.dossier.PieceJointeDto;
import com.tsix.immocrm.entities.adm.AdmAttachedFile;
import com.tsix.immocrm.entities.adm.AdmAttachedFileSummary;
import com.tsix.immocrm.entities.adm.AdmAttachedFileType;
import com.tsix.immocrm.entities.app.AppFileObject;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.adm.AdmAttachedFileRepository;
import com.tsix.immocrm.repositories.adm.AdmAttachedFileSummaryRepository;
import com.tsix.immocrm.repositories.adm.AdmAttachedFileTypeRepository;
import com.tsix.immocrm.repositories.app.AppFileObjectRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.storage.CleStockage;
import com.tsix.immocrm.storage.ObjetStocke;
import com.tsix.immocrm.storage.PorteurPieceJointe;
import com.tsix.immocrm.storage.StockageFichiers;
import com.tsix.immocrm.storage.StockageProperties;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.web.ContentTypeParExtension;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Pièces jointes de tout porteur : {@code AdmAttachedFile} est polymorphe ({@code CategoryId =
 * <porteur>.PKey}, sans FK). Binaire sur S3 pointé par {@code AppFileObject}, lu par URL présignée.
 */
@Service
@Transactional(readOnly = true)
public class PieceJointeSupport {

    private final AdmAttachedFileSummaryRepository summaryRepository;
    private final AdmAttachedFileRepository fileRepository;
    private final AdmAttachedFileTypeRepository typeRepository;
    private final AppFileObjectRepository fileObjectRepository;
    private final StockageFichiers stockage;
    private final StockageProperties stockageProperties;
    private final UtilisateurAffiche utilisateurAffiche;
    private final DepotBinaireSource depotBinaireSource;

    public PieceJointeSupport(AdmAttachedFileSummaryRepository summaryRepository,
                              AdmAttachedFileRepository fileRepository,
                              AdmAttachedFileTypeRepository typeRepository,
                              AppFileObjectRepository fileObjectRepository,
                              StockageFichiers stockage,
                              StockageProperties stockageProperties,
                              UtilisateurAffiche utilisateurAffiche,
                              DepotBinaireSource depotBinaireSource) {
        this.summaryRepository = summaryRepository;
        this.fileRepository = fileRepository;
        this.typeRepository = typeRepository;
        this.fileObjectRepository = fileObjectRepository;
        this.stockage = stockage;
        this.stockageProperties = stockageProperties;
        this.utilisateurAffiche = utilisateurAffiche;
        this.depotBinaireSource = depotBinaireSource;
    }

    /**
     * Plus récentes d'abord. Exclut {@code hasFile = false} : cases de checklist « cochée mais
     * jamais déposée » ({@code IsCheckedButNotAttached}), dont le {@code CategoryId} correspond.
     */
    public List<PieceJointeDto> lister(UUID porteurId) {
        return summaryRepository.findByCategoryId(porteurId).stream()
                .filter(a -> Boolean.TRUE.equals(a.getHasFile()))
                .map(PieceJointeSupport::toDto)
                .toList();
    }

    public boolean auMoinsUnFichier(UUID porteurId) {
        return summaryRepository.findByCategoryId(porteurId).stream()
                .anyMatch(a -> Boolean.TRUE.equals(a.getHasFile()));
    }

    /** Mêmes exclusions que {@link #lister}. */
    public long nombreDeFichiers(UUID porteurId) {
        return summaryRepository.findByCategoryId(porteurId).stream()
                .filter(a -> Boolean.TRUE.equals(a.getHasFile()))
                .count();
    }

    /**
     * Bornée au porteur : la pièce d'un autre porteur donne 404. L'URL <b>vaut autorisation</b>
     * pendant sa validité, d'où le contrôle d'appartenance avant et sa durée courte.
     */
    public URI urlTelechargement(UUID porteurId, UUID pieceId, String libellePorteur) {
        AdmAttachedFile piece = fileRepository.findByIdAndCategoryId(pieceId, porteurId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pièce jointe introuvable pour " + libellePorteur + ": " + pieceId));

        AppFileObject objet = fileObjectRepository
                .findByAdmAttachedFileIdAndDeletedAtIsNull(pieceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucun fichier stocké pour cette pièce jointe: " + pieceId));

        return stockage.urlTelechargement(objet.getStorageKey(),
                nomTelechargement(piece.getDescription(), piece.getExtension()),
                objet.getContentType());
    }

    /**
     * L'appelant a vérifié le porteur ; {@code category} est cosmétique (la relecture ne filtre que
     * sur {@code CategoryId}). <b>S3 d'abord, la base ensuite</b> : au pire un objet orphelin, jamais
     * une ligne pointant vers le vide — voir {@code FEATURE-s3-storage.md} §3.4.
     */
    @Transactional
    public PieceJointeDto deposer(UUID porteurId, PorteurPieceJointe porteur, String category,
                                  MultipartFile fichier, String description, UUID typeId,
                                  AuthenticatedUser principal) {
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
            extension = CleStockage.normaliserExtension(
                    nomOriginal.substring(nomOriginal.lastIndexOf('.')));
        }
        String legende = StringUtils.hasText(description) ? description
                : (StringUtils.hasText(nomOriginal) ? nomOriginal : "Pièce jointe");

        UUID pieceId = UUID.randomUUID();
        String cle = CleStockage.construire(stockageProperties.getPrefix(), porteur, porteurId,
                pieceId, extension);
        String contentType = ContentTypeParExtension.resolve(extension).toString();

        byte[] contenu;
        try {
            contenu = fichier.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Lecture du fichier échouée", e);
        }

        ObjetStocke stocke = stockage.deposer(cle, contenu, contentType,
                nomTelechargement(legende, extension));
        supprimerSiTransactionAnnulee(cle);

        AdmAttachedFile entity = new AdmAttachedFile();
        entity.setId(pieceId);
        entity.setCategory(category);
        entity.setCategoryId(porteurId);
        entity.setDescription(legende);
        entity.setExtension(extension);
        entity.setAdmAttachedFileTypeId(typeId);
        entity.setUserCreateDateTime(LocalDateTime.now());
        entity.setUserCreate(utilisateurAffiche.nom(principal));
        entity = fileRepository.save(entity);

        // OMRAN ne lit que la colonne binaire de la source ; no-op en mono-base.
        depotBinaireSource.deposer(pieceId, contenu);

        AppFileObject objet = new AppFileObject();
        objet.setAdmAttachedFileId(pieceId);
        objet.setStorageKey(stocke.cle());
        objet.setContentType(stocke.contentType());
        objet.setSizeBytes(stocke.tailleOctets());
        objet.setSha256(stocke.sha256());
        objet.setUploadedAt(LocalDateTime.now());
        objet.setUploadedBy(utilisateurAffiche.nom(principal));
        fileObjectRepository.save(objet);

        return new PieceJointeDto(entity.getId(), entity.getDescription(), entity.getCategory(),
                Champs.de(type, AdmAttachedFileType::getDescription), entity.getExtension(),
                entity.getUserCreateDateTime(), entity.getUserCreate());
    }

    /**
     * 404 si la pièce n'est pas à ce porteur ou n'a aucun objet <b>vivant</b>. Séparée de
     * {@link #supprimer} : sinon une pièce inconnue sur un règlement pointé donnerait 409, pas 404.
     */
    public AppFileObject exigerFichier(UUID porteurId, UUID pieceId, String libellePorteur) {
        fileRepository.findByIdAndCategoryId(pieceId, porteurId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pièce jointe introuvable pour " + libellePorteur + ": " + pieceId));

        return fileObjectRepository.findByAdmAttachedFileIdAndDeletedAtIsNull(pieceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucun fichier stocké pour cette pièce jointe: " + pieceId));
    }

    /**
     * Suppression logique ; ni la ligne {@code AdmAttachedFile} ni l'objet S3 ne sont supprimés
     * ({@code FEATURE-suppression-pieces-jointes.md} §5 R1–R2).
     */
    @Transactional
    public void marquerSupprime(AppFileObject objet, AuthenticatedUser principal) {
        objet.setDeletedAt(LocalDateTime.now());
        objet.setDeletedBy(utilisateurAffiche.nom(principal));
        // saveAndFlush : hasFile est une @Formula sur AppFileObject, hors des espaces d'auto-flush
        // d'Hibernate ; sans flush, une relecture dans la même transaction voit encore la pièce.
        fileObjectRepository.saveAndFlush(objet);
    }

    @Transactional
    public void supprimer(UUID porteurId, UUID pieceId, String libellePorteur,
                          AuthenticatedUser principal) {
        marquerSupprime(exigerFichier(porteurId, pieceId, libellePorteur), principal);
    }

    private void supprimerSiTransactionAnnulee(String cle) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    stockage.supprimer(cle);
                }
            }
        });
    }

    /** Publique : la pièce jointe d'un email doit porter le même nom que le téléchargement. */
    public static String nomTelechargement(String description, String extension) {
        String base = StringUtils.hasText(description) ? description.trim() : "piece-jointe";
        if (StringUtils.hasText(extension) && !base.toLowerCase().endsWith(extension)) {
            base = base + extension;
        }
        return base;
    }

    public static PieceJointeDto toDto(AdmAttachedFileSummary a) {
        String type = Champs.de(a.getType(), AdmAttachedFileType::getDescription);
        return new PieceJointeDto(a.getId(), a.getDescription(), a.getCategory(), type, a.getExtension(),
                a.getUserCreateDateTime(), a.getUserCreate());
    }
}

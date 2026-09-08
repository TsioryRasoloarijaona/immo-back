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
 * Mécanique commune aux pièces jointes, quel que soit l'objet porteur (Dossier, Article,
 * Règlement…).
 *
 * <p>{@code AdmAttachedFile} est polymorphe : le rattachement se fait par
 * {@code CategoryId = <porteur>.PKey}, sans FK déclarée. Tout ce qui suit ce rattachement est
 * strictement identique d'un porteur à l'autre ; seules diffèrent (a) la vérification
 * d'existence du porteur et (b) la chaîne {@code Category} écrite au dépôt, laissées à
 * l'appelant.
 *
 * <p><b>Les binaires ne sont plus en base.</b> Ils vivent sur S3/MinIO ; {@code AppFileObject}
 * porte le pointeur, et {@code AdmAttachedFile} ne garde que les métadonnées. La lecture ne
 * renvoie donc plus d'octets mais une URL présignée : le fichier transite directement entre le
 * navigateur et le stockage, sans passer par le backend.
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

    public PieceJointeSupport(AdmAttachedFileSummaryRepository summaryRepository,
                              AdmAttachedFileRepository fileRepository,
                              AdmAttachedFileTypeRepository typeRepository,
                              AppFileObjectRepository fileObjectRepository,
                              StockageFichiers stockage,
                              StockageProperties stockageProperties,
                              UtilisateurAffiche utilisateurAffiche) {
        this.summaryRepository = summaryRepository;
        this.fileRepository = fileRepository;
        this.typeRepository = typeRepository;
        this.fileObjectRepository = fileObjectRepository;
        this.stockage = stockage;
        this.stockageProperties = stockageProperties;
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

    /** {@code true} dès qu'au moins une pièce jointe réelle porte cet id. */
    public boolean auMoinsUnFichier(UUID porteurId) {
        return summaryRepository.findByCategoryId(porteurId).stream()
                .anyMatch(a -> Boolean.TRUE.equals(a.getHasFile()));
    }

    /**
     * Nombre de pièces jointes réelles du porteur — mêmes exclusions que {@link #lister}. Sert à
     * un appelant qui doit savoir, <b>avant</b> de supprimer, si la pièce visée est la dernière
     * (voir {@code ReglementPieceJointeService#supprimer}).
     */
    public long nombreDeFichiers(UUID porteurId) {
        return summaryRepository.findByCategoryId(porteurId).stream()
                .filter(a -> Boolean.TRUE.equals(a.getHasFile()))
                .count();
    }

    /**
     * URL présignée de téléchargement, bornée au porteur : un id de pièce appartenant à un autre
     * porteur donne 404, jamais le fichier d'autrui. {@code libellePorteur} n'entre que dans le
     * message d'erreur ("ce dossier", "ce règlement"…).
     *
     * <p>L'URL <b>vaut autorisation d'accès</b> pour sa durée de validité : quiconque l'obtient
     * peut télécharger le fichier sans être authentifié. C'est pourquoi elle n'est délivrée
     * qu'après le contrôle d'appartenance ci-dessous, et pourquoi sa durée de vie est courte
     * ({@code app.stockage.presign-minutes}).
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
     * Dépose un fichier sur le porteur. L'appelant a déjà vérifié que le porteur existe et lui
     * appartient ; il fournit le {@link PorteurPieceJointe} (segment de clé) et la chaîne
     * {@code category} à écrire (convention legacy propre à chaque type de porteur, purement
     * cosmétique : la relecture ne filtre que sur {@code CategoryId}).
     *
     * <p><b>Ordre imposé : le stockage d'abord, la base ensuite.</b> S3 n'est pas dans la
     * transaction. Dans cet ordre, la seule fuite possible en cas d'échec est un objet orphelin
     * dans le bucket — un coût, sans conséquence fonctionnelle, et balayable. L'ordre inverse
     * laisserait des lignes pointant vers le vide, c'est-à-dire des 500 au téléchargement. Le
     * {@link TransactionSynchronization} enregistré ci-dessous supprime l'objet si la
     * transaction est finalement annulée, ce qui rattrape le cas courant.
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
     * Le fichier stocké de cette pièce, borné au porteur — ou 404. Deux échecs distincts, tous
     * deux invisibles pour l'appelant : la pièce n'appartient pas à ce porteur (ou n'existe pas),
     * ou elle n'a aucun objet stocké <b>vivant</b> (case de checklist jamais téléversée, ou pièce
     * déjà supprimée logiquement).
     *
     * <p>Exposée séparément de {@link #supprimer} pour l'appelant qui doit intercaler une règle
     * métier entre le contrôle d'existence et la suppression : sans elle, une pièce inconnue sur
     * un règlement pointé donnerait 409 au lieu de 404.
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
     * Suppression logique : {@code DeletedAt} + {@code DeletedBy} sur {@code AppFileObject}.
     *
     * <p><b>Deux choses ne sont volontairement pas faites ici.</b>
     *
     * <p>La ligne {@code AdmAttachedFile} <b>n'est pas supprimée</b> : elle porte les métadonnées
     * et l'auteur du dépôt, et la faire disparaître rendrait la suppression intraçable. Elle
     * bascule simplement à {@code hasFile = false} — la {@code @Formula} de
     * {@link AdmAttachedFileSummary} teste {@code DeletedAt IS NULL} — donc invisible de
     * {@link #lister} et de {@link #urlTelechargement} sans qu'aucun de ces deux chemins n'ait à
     * connaître la suppression.
     *
     * <p>L'objet S3 <b>n'est pas supprimé</b>, ni maintenant ni après : un balayage hors ligne
     * s'en charge ({@code FEATURE-s3-storage.md} §3.4). Le dépôt écrit le stockage avant la base
     * précisément pour que la seule fuite possible soit un objet orphelin ; supprimer le binaire
     * dans la transaction rouvrirait le cas inverse — une transaction annulée après un
     * {@code DELETE} S3 laisserait une ligne pointant vers le vide, c'est-à-dire un 500 au
     * téléchargement.
     *
     * <p><b>{@code saveAndFlush}, pas {@code save}</b> — et ce n'est pas une précaution
     * décorative. {@code hasFile} est une {@link org.hibernate.annotations.Formula} : sa
     * sous-requête sur {@code AppFileObject} est évaluée <b>par SQL Server</b> au chargement de
     * {@link AdmAttachedFileSummary}. Hibernate ne voit dans cette formule qu'un fragment de SQL
     * opaque, ignore donc que la liste dépend d'{@code AppFileObject}, et n'inclut pas cette table
     * dans les espaces de requête qui déclenchent l'auto-flush. Sans le flush explicite, une
     * lecture faite dans la même transaction que la suppression relit la ligne <i>d'avant</i> et
     * la pièce supprimée réapparaît dans la liste. En production le cas ne se voit pas — une
     * requête, une transaction — mais il est réel et permanent sous {@code @Transactional} côté
     * test, et il le serait pour tout appelant qui enchaînerait suppression et relecture.
     */
    @Transactional
    public void marquerSupprime(AppFileObject objet, AuthenticatedUser principal) {
        objet.setDeletedAt(LocalDateTime.now());
        objet.setDeletedBy(utilisateurAffiche.nom(principal));
        fileObjectRepository.saveAndFlush(objet);
    }

    /**
     * Cas courant : résout la pièce puis la marque supprimée. Équivaut à
     * {@link #exigerFichier} suivi de {@link #marquerSupprime}.
     */
    @Transactional
    public void supprimer(UUID porteurId, UUID pieceId, String libellePorteur,
                          AuthenticatedUser principal) {
        marquerSupprime(exigerFichier(porteurId, pieceId, libellePorteur), principal);
    }

    /**
     * Supprime l'objet déposé si la transaction est annulée après coup — sans quoi un rollback
     * (contrainte violée, erreur applicative plus loin) laisserait un fichier payant et jamais
     * référencé dans le bucket.
     */
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

    /** Nom proposé au navigateur : la description, suffixée de l'extension si elle manque. */
    private static String nomTelechargement(String description, String extension) {
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

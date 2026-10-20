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
import com.tsix.immocrm.storage.StockageFichiers;
import com.tsix.immocrm.storage.StockageProperties;
import com.tsix.immocrm.utils.Champs;
import com.tsix.immocrm.web.ContentTypeParExtension;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Pièces jointes de tout porteur : {@code AdmAttachedFile} est polymorphe ({@code CategoryId =
 * <porteur>.PKey}, sans FK). Binaire sur S3 pointé par {@code AppFileObject}, lu par URL présignée.
 */
@Service
@Transactional(readOnly = true)
public class PieceJointeSupport {

    /** Limite de {@code spring.servlet.multipart.max-file-size}, vérifiée ici aussi (MockMvc ne l'applique pas). */
    static final long TAILLE_MAX = 50L * 1024 * 1024;
    private static final String PIECE_INTROUVABLE = "Pièce jointe introuvable pour %s: %s";

    private final AdmAttachedFileSummaryRepository summaryRepository;
    private final AdmAttachedFileRepository fileRepository;
    private final AdmAttachedFileTypeRepository typeRepository;
    private final AppFileObjectRepository fileObjectRepository;
    private final StockageFichiers stockage;
    private final StockageProperties stockageProperties;
    private final UtilisateurAffiche utilisateurAffiche;
    private final DepotBinaireSource depotBinaireSource;
    private final Clock clock;

    public PieceJointeSupport(AdmAttachedFileSummaryRepository summaryRepository,
                              AdmAttachedFileRepository fileRepository,
                              AdmAttachedFileTypeRepository typeRepository,
                              AppFileObjectRepository fileObjectRepository,
                              StockageFichiers stockage,
                              StockageProperties stockageProperties,
                              UtilisateurAffiche utilisateurAffiche,
                              DepotBinaireSource depotBinaireSource,
                              Clock clock) {
        this.clock = clock;
        this.summaryRepository = summaryRepository;
        this.fileRepository = fileRepository;
        this.typeRepository = typeRepository;
        this.fileObjectRepository = fileObjectRepository;
        this.stockage = stockage;
        this.stockageProperties = stockageProperties;
        this.utilisateurAffiche = utilisateurAffiche;
        this.depotBinaireSource = depotBinaireSource;
    }

    /** Fichier lu et validé, avant toute écriture. */
    private record Contenu(byte[] octets, String extension, String legende) {
        @Override
        public boolean equals(Object autre) {
            return autre instanceof Contenu c && Arrays.equals(octets, c.octets)
                    && Objects.equals(extension, c.extension) && Objects.equals(legende, c.legende);
        }

        @Override
        public int hashCode() {
            return 31 * Objects.hash(extension, legende) + Arrays.hashCode(octets);
        }

        @Override
        public String toString() {
            return "Contenu[octets=" + octets.length + " o, extension=" + extension + ", legende=" + legende + "]";
        }
    }

    /**
     * Plus récentes d'abord. Exclut {@code hasFile = false} : cases de checklist « cochée mais
     * jamais déposée » ({@code IsCheckedButNotAttached}), dont le {@code CategoryId} correspond.
     */
    public List<PieceJointeDto> lister(UUID porteurId) {
        return avecFichier(porteurId).stream().map(PieceJointeSupport::toDto).toList();
    }

    public boolean auMoinsUnFichier(UUID porteurId) {
        return !avecFichier(porteurId).isEmpty();
    }

    /** Mêmes exclusions que {@link #lister}. */
    public long nombreDeFichiers(UUID porteurId) {
        return avecFichier(porteurId).size();
    }

    /** La plus récente pièce vivante du porteur de ce type (attachement en masse). */
    public Optional<UUID> pieceDuType(UUID porteurId, UUID typeId) {
        return avecFichier(porteurId).stream()
                .filter(a -> typeId.equals(Champs.de(a.getType(), AdmAttachedFileType::getId)))
                .map(AdmAttachedFileSummary::getId)
                .findFirst();
    }

    public List<String> groupes() {
        return summaryRepository.groupes();
    }

    private List<AdmAttachedFileSummary> avecFichier(UUID porteurId) {
        return summaryRepository.findByCategoryId(porteurId).stream()
                .filter(a -> Boolean.TRUE.equals(a.getHasFile()))
                .toList();
    }

    /**
     * Bornée au porteur : la pièce d'un autre porteur donne 404. L'URL <b>vaut autorisation</b>
     * pendant sa validité, d'où le contrôle d'appartenance avant et sa durée courte.
     */
    public URI urlTelechargement(UUID porteurId, UUID pieceId, String libellePorteur) {
        AdmAttachedFile piece = fileRepository.findByIdAndCategoryId(pieceId, porteurId)
                .orElseThrow(() -> new ResourceNotFoundException(PIECE_INTROUVABLE.formatted(libellePorteur, pieceId)));

        AppFileObject objet = fileObjectRepository
                .findByAdmAttachedFileIdAndDeletedAtIsNull(pieceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucun fichier stocké pour cette pièce jointe: " + pieceId));

        return stockage.urlTelechargement(objet.getStorageKey(),
                nomTelechargement(piece.getDescription(), piece.getExtension()),
                objet.getContentType());
    }

    /**
     * L'appelant a vérifié le porteur. <b>S3 d'abord, la base ensuite</b> : au pire un objet orphelin, jamais
     * une ligne pointant vers le vide — voir {@code FEATURE-s3-storage.md} §3.4.
     */
    @Transactional
    public PieceJointeDto deposer(PorteurDepot porteur, MultipartFile fichier, SaisiePieceJointe saisie,
                                  AuthenticatedUser principal) {
        AdmAttachedFile piece = new AdmAttachedFile();
        piece.setId(UUID.randomUUID());
        piece.setUserCreateDateTime(LocalDateTime.now(clock));
        piece.setUserCreate(utilisateurAffiche.nom(principal));
        return ecrire(piece, piece.getId(), porteur, fichier, saisie, principal);
    }

    /**
     * Même PKey, contenu sous une <b>nouvelle</b> clé S3 : réécrire l'ancienne écraserait l'objet avant le commit.
     * L'ancien objet reste orphelin, comme après une suppression logique.
     */
    @Transactional
    public PieceJointeDto remplacer(UUID pieceId, PorteurDepot porteur, MultipartFile fichier, SaisiePieceJointe saisie,
                                    AuthenticatedUser principal) {
        AdmAttachedFile piece = fileRepository.findByIdAndCategoryId(pieceId, porteur.id())
                .orElseThrow(() -> new ResourceNotFoundException(PIECE_INTROUVABLE.formatted(porteur.type().libelle(),
                        pieceId)));
        piece.setUserUpdateDateTime(LocalDateTime.now(clock));
        piece.setUserUpdate(utilisateurAffiche.nom(principal));
        return ecrire(piece, UUID.randomUUID(), porteur, fichier, saisie, principal);
    }

    private PieceJointeDto ecrire(AdmAttachedFile piece, UUID idCle, PorteurDepot porteur, MultipartFile fichier,
                                  SaisiePieceJointe saisie, AuthenticatedUser principal) {
        Contenu contenu = lire(fichier, saisie.description());
        AdmAttachedFileType type = null;
        if (saisie.typeId() != null) {
            type = typeRepository.findById(saisie.typeId()).orElseThrow(() -> new ResourceNotFoundException(
                    "Type de pièce jointe introuvable: " + saisie.typeId()));
        }

        String cle = CleStockage.construire(stockageProperties.getPrefix(), porteur.type(), porteur.id(), idCle,
                contenu.extension());
        ObjetStocke stocke = stockage.deposer(cle, contenu.octets(),
                ContentTypeParExtension.resolve(contenu.extension()).toString(),
                nomTelechargement(contenu.legende(), contenu.extension()));
        supprimerSiTransactionAnnulee(cle);

        piece.setCategory(porteur.category());
        piece.setCategoryId(porteur.id());
        piece.setCategoryDescription(porteur.categoryDescription());
        piece.setDescription(contenu.legende());
        piece.setExtension(contenu.extension());
        piece.setAdmAttachedFileTypeId(saisie.typeId());
        piece.setGroupe(StringUtils.hasText(saisie.groupe()) ? saisie.groupe().trim() : null);
        AdmAttachedFile ecrite = fileRepository.save(piece);
        ecrite.setLibelleAudit(porteur.code());

        // OMRAN ne lit que la colonne binaire de la source ; no-op en mono-base.
        depotBinaireSource.deposer(ecrite.getId(), contenu.octets());

        AppFileObject objet = fileObjectRepository.findById(ecrite.getId()).orElseGet(AppFileObject::new);
        objet.setAdmAttachedFileId(ecrite.getId());
        objet.setStorageKey(stocke.cle());
        objet.setContentType(stocke.contentType());
        objet.setSizeBytes(stocke.tailleOctets());
        objet.setSha256(stocke.sha256());
        objet.setUploadedAt(LocalDateTime.now(clock));
        objet.setUploadedBy(utilisateurAffiche.nom(principal));
        fileObjectRepository.save(objet);

        return new PieceJointeDto(ecrite.getId(), ecrite.getDescription(), ecrite.getCategory(),
                Champs.de(type, AdmAttachedFileType::getDescription), ecrite.getExtension(),
                ecrite.getUserCreateDateTime(), ecrite.getUserCreate(), ecrite.getGroupe());
    }

    /** Vide → 400, plus de 50 Mo → 413, extension absente ou hors liste blanche → 400 nommant la liste. */
    private static Contenu lire(MultipartFile fichier, String description) {
        if (fichier == null || fichier.isEmpty()) {
            throw new InvalidRequestException("Le fichier est requis et ne peut pas être vide");
        }
        if (fichier.getSize() > TAILLE_MAX) {
            throw new MaxUploadSizeExceededException(TAILLE_MAX);
        }
        String nomOriginal = fichier.getOriginalFilename();
        String extension = StringUtils.hasText(nomOriginal) && nomOriginal.contains(".")
                ? CleStockage.normaliserExtension(nomOriginal.substring(nomOriginal.lastIndexOf('.')))
                : "";
        if (!ContentTypeParExtension.admise(extension)) {
            throw new InvalidRequestException("Type de fichier non admis (%s). Extensions admises : %s."
                    .formatted(Objects.toString(nomOriginal, ""), ContentTypeParExtension.ADMISES));
        }
        try {
            return new Contenu(fichier.getBytes(), extension, legende(description, nomOriginal));
        } catch (IOException e) {
            throw new UncheckedIOException("Lecture du fichier échouée", e);
        }
    }

    /**
     * 404 si la pièce n'est pas à ce porteur ou n'a aucun objet <b>vivant</b>. Séparée de
     * {@link #supprimer} : sinon une pièce inconnue sur un règlement pointé donnerait 409, pas 404.
     */
    public AppFileObject exigerFichier(UUID porteurId, UUID pieceId, String libellePorteur) {
        fileRepository.findByIdAndCategoryId(pieceId, porteurId)
                .orElseThrow(() -> new ResourceNotFoundException(PIECE_INTROUVABLE.formatted(libellePorteur, pieceId)));

        return fileObjectRepository.findByAdmAttachedFileIdAndDeletedAtIsNull(pieceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Aucun fichier stocké pour cette pièce jointe: " + pieceId));
    }

    /**
     * Suppression logique ; ni la ligne {@code AdmAttachedFile} ni l'objet S3 ne sont supprimés
     * ({@code FEATURE-suppression-pieces-jointes.md} §5 R1–R2). {@code codePorteur} = libellé d'audit.
     */
    @Transactional
    public void marquerSupprime(AppFileObject objet, String codePorteur, AuthenticatedUser principal) {
        appliquerSuppressionLogique(objet, codePorteur, principal);
    }

    @Transactional
    public void supprimer(PorteurDepot porteur, UUID pieceId, AuthenticatedUser principal) {
        appliquerSuppressionLogique(exigerFichier(porteur.id(), pieceId, porteur.type().libelle()), porteur.code(),
                principal);
    }

    private void appliquerSuppressionLogique(AppFileObject objet, String codePorteur, AuthenticatedUser principal) {
        String auteur = utilisateurAffiche.nom(principal);
        LocalDateTime maintenant = LocalDateTime.now(clock);
        // La pièce est datée et marquée pour que l'audit écrive « Deleted » (FEATURE-patrimoine-article-onglets.md §11).
        fileRepository.findById(objet.getAdmAttachedFileId()).ifPresent(piece -> {
            piece.setUserUpdate(auteur);
            piece.setUserUpdateDateTime(maintenant);
            piece.setLibelleAudit(codePorteur);
            piece.setSuppressionLogique(true);
        });
        objet.setDeletedAt(maintenant);
        objet.setDeletedBy(auteur);
        // saveAndFlush : hasFile est une @Formula sur AppFileObject, hors des espaces d'auto-flush
        // d'Hibernate ; sans flush, une relecture dans la même transaction voit encore la pièce.
        fileObjectRepository.saveAndFlush(objet);
    }

    /** Légende par défaut = nom d'origine ; séparateurs de chemin et caractères de contrôle retirés. */
    private static String legende(String description, String nomOriginal) {
        String brute = StringUtils.hasText(description) ? description : nomOriginal;
        String propre = brute == null ? "" : assainir(brute);
        return propre.isEmpty() ? "Pièce jointe" : propre;
    }

    private static String assainir(String texte) {
        StringBuilder propre = new StringBuilder(texte.length());
        texte.codePoints()
                .filter(c -> c != '/' && c != '\\' && !Character.isISOControl(c))
                .forEach(propre::appendCodePoint);
        return propre.toString().trim();
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
                a.getUserCreateDateTime(), a.getUserCreate(), a.getGroupe());
    }
}

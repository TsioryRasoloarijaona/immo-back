package com.tsix.immocrm.storage;

import com.tsix.immocrm.entities.adm.AdmAttachedFile;
import com.tsix.immocrm.entities.app.AppFileObject;
import com.tsix.immocrm.repositories.adm.AdmAttachedFileRepository;
import com.tsix.immocrm.repositories.app.AppFileObjectRepository;
import com.tsix.immocrm.web.ContentTypeParExtension;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Construit une pièce jointe complète dans les tests.
 *
 * <p>Depuis le passage au stockage objet, une pièce jointe n'est plus une seule ligne : il faut
 * la ligne de métadonnées ({@code AdmAttachedFile}), le pointeur ({@code AppFileObject}) et le
 * contenu déposé. Oublier le pointeur donne un fixture invisible — {@code hasFile} interroge
 * {@code AppFileObject}, donc la pièce serait filtrée hors de toutes les listes, et le test
 * échouerait pour une raison sans rapport avec ce qu'il vérifie.
 *
 * <p>Ce composant existe pour que cet oubli soit impossible.
 */
@Component
public class FixturePieceJointe {

    private final AdmAttachedFileRepository fileRepository;
    private final AppFileObjectRepository fileObjectRepository;
    private final StockageMemoire stockage;

    public FixturePieceJointe(AdmAttachedFileRepository fileRepository,
                              AppFileObjectRepository fileObjectRepository,
                              StockageMemoire stockage) {
        this.fileRepository = fileRepository;
        this.fileObjectRepository = fileObjectRepository;
        this.stockage = stockage;
    }

    /**
     * Enregistre la pièce fournie (l'appelant a positionné {@code categoryId}, {@code category},
     * {@code description}, {@code extension}, éventuellement le type) et lui associe un contenu
     * réellement déposé sur le stockage de test.
     */
    public AdmAttachedFile deposer(AdmAttachedFile piece, byte[] contenu) {
        if (piece.getId() == null) {
            piece.setId(UUID.randomUUID());
        }
        AdmAttachedFile enregistree = fileRepository.save(piece);

        String extension = CleStockage.normaliserExtension(piece.getExtension());
        String contentType = ContentTypeParExtension.resolve(extension).toString();
        String cle = CleStockage.construire("test", PorteurPieceJointe.DOSSIER,
                piece.getCategoryId(), enregistree.getId(), extension);

        ObjetStocke stocke = stockage.deposer(cle, contenu, contentType, piece.getDescription());

        AppFileObject objet = new AppFileObject();
        objet.setAdmAttachedFileId(enregistree.getId());
        objet.setStorageKey(stocke.cle());
        objet.setContentType(stocke.contentType());
        objet.setSizeBytes(stocke.tailleOctets());
        objet.setSha256(stocke.sha256());
        objet.setUploadedAt(LocalDateTime.now());
        objet.setUploadedBy("test");
        fileObjectRepository.save(objet);

        return enregistree;
    }

    /** Clé de stockage attendue pour cette pièce — pour asserter sur l'URL renvoyée. */
    public String cleAttendue(AdmAttachedFile piece) {
        return CleStockage.construire("test", PorteurPieceJointe.DOSSIER, piece.getCategoryId(),
                piece.getId(), CleStockage.normaliserExtension(piece.getExtension()));
    }
}

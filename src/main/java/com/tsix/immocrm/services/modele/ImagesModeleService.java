package com.tsix.immocrm.services.modele;

import com.tsix.immocrm.dto.modele.ImageModeleDto;
import com.tsix.immocrm.entities.app.AppModeleImage;
import com.tsix.immocrm.exceptions.ConfigurationManquanteException;
import com.tsix.immocrm.exceptions.InvalidRequestException;
import com.tsix.immocrm.exceptions.ResourceNotFoundException;
import com.tsix.immocrm.repositories.app.AppModeleImageRepository;
import com.tsix.immocrm.security.UtilisateurAffiche;
import com.tsix.immocrm.storage.CleStockage;
import com.tsix.immocrm.storage.ObjetStocke;
import com.tsix.immocrm.storage.StockageFichiers;
import com.tsix.immocrm.storage.StockageProperties;
import com.tsix.immocrm.utils.SignatureImage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Images des modèles (FEATURE-modeles-documents.md §13) : octets dans S3, ligne {@code AppModeleImage} en base.
 * S3 d'abord, la base ensuite : au pire un objet orphelin, jamais lu. Une image n'est jamais supprimée.
 */
@Service
@Transactional(readOnly = true)
public class ImagesModeleService {

    static final int IMAGE_MAX_OCTETS = 500 * 1024;

    private final AppModeleImageRepository imageRepository;
    private final StockageFichiers stockage;
    private final StockageProperties stockageProperties;
    private final UtilisateurAffiche utilisateurAffiche;
    private final Clock clock;

    public ImagesModeleService(AppModeleImageRepository imageRepository, StockageFichiers stockage,
                               StockageProperties stockageProperties, UtilisateurAffiche utilisateurAffiche,
                               Clock clock) {
        this.imageRepository = imageRepository;
        this.stockage = stockage;
        this.stockageProperties = stockageProperties;
        this.utilisateurAffiche = utilisateurAffiche;
        this.clock = clock;
    }

    /** Octets et type d'une image lue dans le stockage. */
    public static final class ImageLue {
        private final byte[] octets;
        private final String contentType;

        ImageLue(byte[] octets, String contentType) {
            this.octets = octets;
            this.contentType = contentType;
        }

        public byte[] octets() {
            return octets;
        }

        public String contentType() {
            return contentType;
        }
    }

    /** PNG ou JPEG reconnu sur les octets (jamais sur le nom ni le type déclaré), 500 Ko au plus. */
    @Transactional
    public ImageModeleDto deposer(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw new InvalidRequestException("Le fichier est requis et ne peut pas être vide.");
        }
        if (fichier.getSize() > IMAGE_MAX_OCTETS) {
            throw new InvalidRequestException("Image trop lourde : %d Ko (500 Ko maximum).".formatted(fichier.getSize() / 1024));
        }
        byte[] octets = octets(fichier);
        String contentType = typeReconnu(octets);
        UUID id = UUID.randomUUID();
        String extension = "image/png".equals(contentType) ? ".png" : ".jpg";
        ObjetStocke stocke = stockage.deposer(CleStockage.imageModele(stockageProperties.getPrefix(), id, extension),
                octets, contentType, "image" + extension);

        AppModeleImage image = new AppModeleImage();
        image.setId(id);
        image.setStorageKey(stocke.cle());
        image.setContentType(contentType);
        image.setSizeBytes(stocke.tailleOctets());
        image.setSha256(stocke.sha256());
        image.setCreeLe(LocalDateTime.now(clock));
        image.setCreePar(Objects.requireNonNullElse(utilisateurAffiche.courant().nom(), ModeleDocumentService.AUTEUR_SYSTEME));
        imageRepository.save(image);
        return new ImageModeleDto(id, contentType, octets.length);
    }

    /** 404 si l'image n'existe pas ; 500 explicite si sa ligne existe mais pas l'objet S3. */
    public ImageLue lire(UUID id) {
        AppModeleImage image = imageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Image introuvable: " + id));
        return new ImageLue(octetsStockes(image), image.getContentType());
    }

    /** Contenu enregistré ou prévisualisé : toute image citée doit avoir été déposée (sinon 400). */
    public void verifierConnues(Set<UUID> ids) {
        if (ids.isEmpty()) {
            return;
        }
        Set<UUID> connues = imageRepository.findAllById(ids).stream().map(AppModeleImage::getId).collect(Collectors.toSet());
        ids.stream().filter(id -> !connues.contains(id)).findFirst().ifPresent(id -> {
            throw new InvalidRequestException("Image inconnue : " + id);
        });
    }

    /** Pour la génération : une lecture S3 par image ; une image absente arrête tout (500), jamais de document partiel. */
    public Map<UUID, String> sourcesEnMemoire(Set<UUID> ids) {
        Map<UUID, AppModeleImage> lignes = imageRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(AppModeleImage::getId, i -> i));
        Map<UUID, String> sources = new HashMap<>();
        for (UUID id : ids) {
            AppModeleImage image = lignes.get(id);
            if (image == null) {
                throw introuvable(id);
            }
            sources.put(id, "data:" + image.getContentType() + ";base64,"
                    + Base64.getEncoder().encodeToString(octetsStockes(image)));
        }
        return sources;
    }

    private byte[] octetsStockes(AppModeleImage image) {
        try {
            return stockage.lire(image.getStorageKey());
        } catch (RuntimeException e) {
            throw introuvable(image.getId());
        }
    }

    private static ConfigurationManquanteException introuvable(UUID id) {
        return new ConfigurationManquanteException("Image %s introuvable dans le stockage".formatted(id));
    }

    private static byte[] octets(MultipartFile fichier) {
        try {
            return fichier.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Lecture de l'image échouée", e);
        }
    }

    private static String typeReconnu(byte[] octets) {
        return SignatureImage.type(octets).orElseThrow(() ->
                new InvalidRequestException("Image refusée : PNG ou JPEG seulement (contenu du fichier vérifié)."));
    }
}

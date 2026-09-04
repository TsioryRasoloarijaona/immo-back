package com.tsix.immocrm.storage;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;

/** Implémentation MinIO / S3 de {@link StockageFichiers}. */
@Component
public class StockageS3 implements StockageFichiers {

    private final S3Client client;
    private final S3Presigner presigner;
    private final StockageProperties properties;

    public StockageS3(S3Client client, S3Presigner presigner, StockageProperties properties) {
        this.client = client;
        this.presigner = presigner;
        this.properties = properties;
    }

    @Override
    public ObjetStocke deposer(String cle, byte[] contenu, String contentType, String nomTelechargement) {
        byte[] empreinte = sha256(contenu);

        client.putObject(PutObjectRequest.builder()
                        .bucket(properties.getBucket())
                        .key(cle)
                        .contentType(contentType)
                        .contentDisposition(contentDisposition(nomTelechargement))
                        // Le serveur recalcule l'empreinte et refuse l'objet si elle diffère :
                        // l'intégrité est vérifiée par le stockage, pas seulement supposée.
                        .checksumSHA256(Base64.getEncoder().encodeToString(empreinte))
                        .build(),
                RequestBody.fromBytes(contenu));

        return new ObjetStocke(cle, contenu.length, contentType,
                HexFormat.of().formatHex(empreinte));
    }

    @Override
    public URI urlTelechargement(String cle, String nomTelechargement, String contentType) {
        // Les en-têtes sont surchargés à la signature plutôt que lus depuis l'objet : le nom
        // proposé au téléchargement suit ainsi la description courante en base, même si elle a
        // été modifiée après le dépôt.
        GetObjectRequest lecture = GetObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(cle)
                .responseContentType(contentType)
                .responseContentDisposition(contentDisposition(nomTelechargement))
                .build();

        return URI.create(presigner.presignGetObject(GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(properties.getPresignMinutes()))
                        .getObjectRequest(lecture)
                        .build())
                .url()
                .toString());
    }

    @Override
    public void supprimer(String cle) {
        client.deleteObject(DeleteObjectRequest.builder()
                .bucket(properties.getBucket())
                .key(cle)
                .build());
    }

    /**
     * {@code filename} en repli ASCII pour les clients anciens, {@code filename*} en UTF-8
     * (RFC 5987) pour le nom réel — les descriptions legacy sont accentuées et un en-tête HTTP
     * ne transporte pas d'UTF-8 brut.
     */
    private static String contentDisposition(String nom) {
        String propre = (nom == null || nom.isBlank()) ? "fichier" : nom.trim();
        String repli = propre.replaceAll("[^A-Za-z0-9._-]", "_");
        if (repli.length() > 120) {
            repli = repli.substring(0, 120);
        }
        return "attachment; filename=\"" + repli + "\"; filename*=UTF-8''"
                + URLEncoder.encode(propre, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static byte[] sha256(byte[] contenu) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(contenu);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible sur cette JVM", e);
        }
    }
}

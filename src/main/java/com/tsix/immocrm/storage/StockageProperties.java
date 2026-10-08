package com.tsix.immocrm.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code app.stockage} ; clés d'accès par l'environnement. Dev et prod ont les mêmes {@code PKey} (restaurations) : sur
 * un bucket partagé, un {@code prefix} distinct est obligatoire, sinon les clés s'écrasent.
 */
@ConfigurationProperties(prefix = "app.stockage")
public class StockageProperties {

    private String bucket;

    /** Vide pour AWS S3 (déduit de la région). */
    private String endpoint;

    /** Sans objet sur MinIO, mais exigée par le SDK. */
    private String region = "us-east-1";

    private String prefix;

    /** Assez long pour un gros fichier sur une connexion lente, assez court pour qu'une URL qui fuit ne dure pas. */
    private int presignMinutes = 15;

    private String accessKey;
    private String secretKey;

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public int getPresignMinutes() {
        return presignMinutes;
    }

    public void setPresignMinutes(int presignMinutes) {
        this.presignMinutes = presignMinutes;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
}

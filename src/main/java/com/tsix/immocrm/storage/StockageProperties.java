package com.tsix.immocrm.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration du stockage objet (MinIO / S3), préfixe {@code app.stockage}.
 *
 * <p><b>Aucun secret ici.</b> {@code accessKey}/{@code secretKey} sont alimentées depuis
 * l'environnement du processus, comme {@code DB_PWD} et {@code JWT_SECRET} —
 * {@code application.properties} est suivi par git.
 *
 * <p>{@code prefix} isole les environnements dans un même bucket ({@code dev}, {@code prod}).
 * Il est déterminant : les bases de dev et de prod étant des restaurations du même jeu de
 * données, les {@code PKey} y sont identiques et des clés sans préfixe distinct
 * s'écraseraient entre elles.
 */
@ConfigurationProperties(prefix = "app.stockage")
public class StockageProperties {

    /** Nom du bucket, ex. {@code immo-back-dev}. */
    private String bucket;

    /**
     * URL du serveur S3. Renseignée pour MinIO ({@code https://s3.exemple.com}), laissée vide
     * pour AWS S3 (le SDK déduit alors l'endpoint de la région).
     */
    private String endpoint;

    /** Sans objet sur MinIO, mais exigée par le SDK — {@code us-east-1} par convention. */
    private String region = "us-east-1";

    /** Premier segment de clé : {@code dev} ou {@code prod}. */
    private String prefix;

    /**
     * Durée de validité des URL présignées. Assez longue pour un téléchargement de 35 Mo sur
     * une connexion lente, assez courte pour qu'une URL qui fuite (historique, journal proxy,
     * capture d'écran) n'ouvre pas un accès durable.
     */
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

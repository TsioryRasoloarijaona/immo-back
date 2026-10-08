package com.tsix.immocrm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Coordonnées de la source de vérité (base OMRAN) : la seule base écrite, pour qu'{@code AdmSequence} ne frappe pas
 * deux fois le même numéro (FEATURE-source-de-verite-115.md). Défauts vides exprès : {@code host} vide = mono-base,
 * et la suite de tests démarre sans variable d'environnement.
 */
@ConfigurationProperties(prefix = "app.source-de-verite")
public class SourceDeVeriteProperties {

    /** Vide = mode mono-base. */
    private String host = "";

    private int port = 1433;

    private String nom = "";

    private String username = "";

    private String password = "";

    /** Lien par VPN : échouer vite plutôt qu'attendre le timeout du pilote (dizaines de secondes). */
    private int timeoutConnexionMs = 5000;

    /** Petit pool : seules les écritures, gestes d'utilisateur, passent ici. */
    private int taillePool = 5;

    /** Pool à part : la recopie tourne pendant que la requête tient encore sa connexion du pool principal. */
    private int taillePoolRemontee = 2;

    /** Plancher imposé par Hikari. Le keepalive empêche la box d'« oublier » une connexion inactive. */
    static final long KEEPALIVE_MIN_MS = 30_000;

    private long keepaliveMs = 60_000;

    private long dureeVieMaxMs = 600_000;

    private long validationTimeoutMs = 3_000;

    private int tentativesReconnexion = 3;

    private int intervalleReconnexionS = 5;

    /** 0 = désactivé. Doit couvrir la plus longue attente des tâches de fond sur ce pool, synchro et archivage
     * de la trace compris (FEATURE-stabilite-connexion-source.md §Mesures). */
    private int timeoutSocketMs = 300_000;

    public boolean estConfiguree() {
        return host != null && !host.isBlank();
    }

    /**
     * {@code trustServerCertificate=true} comme {@code spring.datasource.url} : certificats auto-signés. Jamais le mot
     * de passe ici. {@code connectRetry*} ne rejoue que la reconnexion d'une connexion inactive, pas une instruction.
     */
    public String urlJdbc() {
        return "jdbc:sqlserver://" + host + ":" + port
                + ";databaseName=" + nom
                + ";encrypt=true;trustServerCertificate=true"
                + ";loginTimeout=" + loginTimeoutS()
                + ";connectRetryCount=" + tentativesReconnexion
                + ";connectRetryInterval=" + intervalleReconnexionS
                + (timeoutSocketMs > 0 ? ";socketTimeout=" + timeoutSocketMs : "");
    }

    /** En secondes, arrondi au supérieur : 0 voudrait dire « attente par défaut du pilote ». */
    int loginTimeoutS() {
        return (int) Math.max(1, (timeoutConnexionMs + 999L) / 1000);
    }

    /** Hikari ignore en silence un keepalive ≥ durée de vie ou sous son plancher : refusé au démarrage. */
    public void verifierReglagesPool() {
        if (keepaliveMs < KEEPALIVE_MIN_MS) {
            throw new IllegalStateException("app.source-de-verite.keepalive-ms (SOURCE_DB_KEEPALIVE_MS) = "
                    + keepaliveMs + " : minimum " + KEEPALIVE_MIN_MS + " ms (plancher Hikari).");
        }
        if (keepaliveMs >= dureeVieMaxMs) {
            throw new IllegalStateException("app.source-de-verite.keepalive-ms (SOURCE_DB_KEEPALIVE_MS) = "
                    + keepaliveMs + " doit être inférieur à app.source-de-verite.duree-vie-max-ms "
                    + "(SOURCE_DB_MAX_LIFETIME_MS) = " + dureeVieMaxMs + ", sinon Hikari ignore le keepalive.");
        }
    }

    /** Sans identifiant : destiné aux journaux. */
    public String description() {
        return host + ":" + port + "/" + nom;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getTimeoutConnexionMs() {
        return timeoutConnexionMs;
    }

    public void setTimeoutConnexionMs(int timeoutConnexionMs) {
        this.timeoutConnexionMs = timeoutConnexionMs;
    }

    public int getTaillePool() {
        return taillePool;
    }

    public void setTaillePool(int taillePool) {
        this.taillePool = taillePool;
    }

    public int getTaillePoolRemontee() {
        return taillePoolRemontee;
    }

    public void setTaillePoolRemontee(int taillePoolRemontee) {
        this.taillePoolRemontee = taillePoolRemontee;
    }

    public long getKeepaliveMs() {
        return keepaliveMs;
    }

    public void setKeepaliveMs(long keepaliveMs) {
        this.keepaliveMs = keepaliveMs;
    }

    public long getDureeVieMaxMs() {
        return dureeVieMaxMs;
    }

    public void setDureeVieMaxMs(long dureeVieMaxMs) {
        this.dureeVieMaxMs = dureeVieMaxMs;
    }

    public long getValidationTimeoutMs() {
        return validationTimeoutMs;
    }

    public void setValidationTimeoutMs(long validationTimeoutMs) {
        this.validationTimeoutMs = validationTimeoutMs;
    }

    public int getTentativesReconnexion() {
        return tentativesReconnexion;
    }

    public void setTentativesReconnexion(int tentativesReconnexion) {
        this.tentativesReconnexion = tentativesReconnexion;
    }

    public int getIntervalleReconnexionS() {
        return intervalleReconnexionS;
    }

    public void setIntervalleReconnexionS(int intervalleReconnexionS) {
        this.intervalleReconnexionS = intervalleReconnexionS;
    }

    public int getTimeoutSocketMs() {
        return timeoutSocketMs;
    }

    public void setTimeoutSocketMs(int timeoutSocketMs) {
        this.timeoutSocketMs = timeoutSocketMs;
    }
}

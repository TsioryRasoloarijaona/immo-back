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

    public boolean estConfiguree() {
        return host != null && !host.isBlank();
    }

    /** {@code trustServerCertificate=true} comme {@code spring.datasource.url} : certificats auto-signés. */
    public String urlJdbc() {
        return "jdbc:sqlserver://" + host + ":" + port
                + ";databaseName=" + nom
                + ";encrypt=true;trustServerCertificate=true";
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
}

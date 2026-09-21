package com.tsix.immocrm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Coordonnées de la <b>source de vérité</b> — la base d'OMRAN Estate — préfixe
 * {@code app.source-de-verite}.
 *
 * <p>Le CRM cohabite avec un logiciel de bureau qu'une dizaine de personnes utilisent tous les
 * jours sur la même base. Les deux frappent leurs numéros de dossier depuis le compteur
 * {@code AdmSequence} ; deux bases écrites en parallèle produisent donc deux fois le même code
 * pour deux clients différents — c'est arrivé, neuf fois, sans qu'aucun index unique ne
 * l'empêche. La parade retenue est de n'écrire que dans <b>une</b> base : celle-ci.
 *
 * <p>Le backend lit donc dans sa copie locale ({@code spring.datasource.*}, inchangée) et
 * écrit ici. Voir {@link DataSourceConfig} pour le routage.
 *
 * <p><b>Aucun secret dans {@code application.properties}</b> : {@code username} et
 * {@code password} viennent de l'environnement, comme {@code DB_PWD} et {@code JWT_SECRET}.
 *
 * <p><b>Toutes les valeurs par défaut sont vides, et c'est délibéré.</b> La suite de tests
 * boote le contexte Spring complet ; une propriété sans défaut ferait échouer les 525 tests
 * sur une variable d'environnement absente — un échec qui ressemblerait à un bug de code.
 * {@code host} vide signifie « pas de source distincte » : l'application démarre alors en
 * mono-base et se comporte exactement comme avant cette feature.
 */
@ConfigurationProperties(prefix = "app.source-de-verite")
public class SourceDeVeriteProperties {

    /** Hôte SQL Server de la base OMRAN. <b>Vide = mode mono-base.</b> */
    private String host = "";

    private int port = 1433;

    /** Nom de la base sur cet hôte, ex. {@code YesRems}. */
    private String nom = "";

    private String username = "";

    private String password = "";

    /**
     * Le lien traverse un VPN. Mieux vaut échouer vite et dire pourquoi que laisser un
     * utilisateur attendre sur l'envoi d'un formulaire pendant le timeout par défaut du
     * pilote, qui se compte en dizaines de secondes.
     */
    private int timeoutConnexionMs = 5000;

    /**
     * Petit pool : seules les écritures passent ici, et une écriture est un geste d'utilisateur,
     * pas un flux. Les listes, fiches et tableaux de bord — l'écrasante majorité du trafic —
     * n'ouvrent jamais de connexion vers cette source.
     */
    private int taillePool = 5;

    /** {@code true} dès qu'un hôte est renseigné : c'est ce qui décide du mode de démarrage. */
    public boolean estConfiguree() {
        return host != null && !host.isBlank();
    }

    /**
     * {@code trustServerCertificate=true} reprend le réglage de {@code spring.datasource.url} :
     * les instances visées présentent un certificat auto-signé.
     */
    public String urlJdbc() {
        return "jdbc:sqlserver://" + host + ":" + port
                + ";databaseName=" + nom
                + ";encrypt=true;trustServerCertificate=true";
    }

    /** Hôte et base, sans identifiant — destiné aux journaux. */
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

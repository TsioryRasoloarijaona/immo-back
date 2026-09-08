package com.tsix.immocrm.whatsapp;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration de l'envoi WhatsApp via UltraMsg, préfixe {@code app.whatsapp}.
 *
 * <p><b>Le jeton est un secret et n'a aucune valeur par défaut.</b> Il vient de l'environnement
 * du processus, comme {@code DB_PWD}, {@code JWT_SECRET} et {@code MAIL_PASSWORD} — variable
 * {@code WP_TOKEN}. Sans lui le service se déclare inactif au démarrage et les messages sont
 * ignorés avec une trace, plutôt que d'échouer un appel sur deux en production.
 *
 * <p>L'identifiant d'instance, lui, n'est pas un secret : il figure dans l'URL publique du
 * compte. Il reste néanmoins configurable ({@code WP_INSTANCE}), parce qu'il change entre un
 * compte d'essai et un compte de production et qu'une valeur en dur obligerait à recompiler.
 */
@ConfigurationProperties(prefix = "app.whatsapp")
public class WhatsAppProperties {

    /**
     * Interrupteur général. À {@code false}, les envois sont ignorés et tracés, mais les
     * endpoints qui les déclenchent continuent de fonctionner normalement.
     */
    private boolean actif = true;

    /** Jeton UltraMsg — variable {@code WP_TOKEN}. Vide = service inactif. */
    private String token;

    /** Identifiant d'instance UltraMsg, ex. {@code instance110131}. Vide = service inactif. */
    private String instance;

    /** Racine de l'API. Configurable pour pouvoir la détourner vers un bouchon local. */
    private String urlBase = "https://api.ultramsg.com";

    /**
     * Délai maximal d'un appel, en secondes. Court volontairement : l'envoi se fait dans un fil
     * asynchrone, mais ce fil est partagé — un appel qui pend immobiliserait le pool et
     * retarderait les messages suivants.
     */
    private int timeoutSecondes = 10;

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public String getUrlBase() {
        return urlBase;
    }

    public void setUrlBase(String urlBase) {
        this.urlBase = urlBase;
    }

    public int getTimeoutSecondes() {
        return timeoutSecondes;
    }

    public void setTimeoutSecondes(int timeoutSecondes) {
        this.timeoutSecondes = timeoutSecondes;
    }
}

package com.tsix.immocrm.whatsapp;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code app.whatsapp}. Jeton secret sans défaut ({@code WP_TOKEN}) : absent, le service se déclare inactif au
 * démarrage et ignore les messages avec une trace. L'instance n'est pas secrète mais change entre essai et production.
 */
@ConfigurationProperties(prefix = "app.whatsapp")
public class WhatsAppProperties {

    /** Faux : envois ignorés et tracés, les endpoints qui les déclenchent fonctionnent normalement. */
    private boolean actif = true;

    /** Vide = service inactif. */
    private String token;

    /** Ex. {@code instance110131}. Vide = service inactif. */
    private String instance;

    /** Configurable pour viser un bouchon local. */
    private String urlBase = "https://api.ultramsg.com";

    /** Court : le fil asynchrone est partagé, un appel qui pend retarderait les messages suivants. */
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

package com.tsix.immocrm.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code app.mail} : expéditeur et URL seulement, les secrets SMTP passent par {@code spring.mail.*}. Pas
 * {@code MailProperties}, homonyme de Spring Boot (FEATURE-service-envoi-email.md §4).
 */
@ConfigurationProperties(prefix = "app.mail")
public class EmailProperties {

    /** Faux : envois ignorés et tracés, les endpoints qui les déclenchent fonctionnent normalement. */
    private boolean actif = true;

    /** Vide : repli sur le compte SMTP (voir {@code EnvoiEmailSmtp}). */
    private String expediteur;

    private String nomExpediteur = "Ayad Estate";

    /** Citée dans le corps du message ; jamais utilisée pour fabriquer un lien portant le mot de passe. */
    private String urlApplication;

    /**
     * Plafond cumulé : un message trop lourd est rejeté par la boîte destinataire, en silence
     * (FEATURE-mail-confirmation-visite.md §4).
     */
    private int piecesJointesMaxMo = 10;

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    public String getExpediteur() {
        return expediteur;
    }

    public void setExpediteur(String expediteur) {
        this.expediteur = expediteur;
    }

    public String getNomExpediteur() {
        return nomExpediteur;
    }

    public void setNomExpediteur(String nomExpediteur) {
        this.nomExpediteur = nomExpediteur;
    }

    public String getUrlApplication() {
        return urlApplication;
    }

    public void setUrlApplication(String urlApplication) {
        this.urlApplication = urlApplication;
    }

    public int getPiecesJointesMaxMo() {
        return piecesJointesMaxMo;
    }

    public void setPiecesJointesMaxMo(int piecesJointesMaxMo) {
        this.piecesJointesMaxMo = piecesJointesMaxMo;
    }

    public long piecesJointesMaxOctets() {
        return piecesJointesMaxMo * 1024L * 1024L;
    }
}

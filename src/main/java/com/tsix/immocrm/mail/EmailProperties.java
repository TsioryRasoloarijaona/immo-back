package com.tsix.immocrm.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration de l'envoi d'email, préfixe {@code app.mail}.
 *
 * <p><b>Aucun secret ici.</b> L'hôte, le port, l'identifiant et le mot de passe de la boîte
 * passent par les propriétés natives de Spring ({@code spring.mail.*}), qui alimentent
 * l'autoconfiguration de {@code JavaMailSender} ; ils viennent de l'environnement du
 * processus comme {@code DB_PWD} et {@code JWT_SECRET}. Cette classe ne porte que ce qui
 * relève de l'application : qui signe le message, et où l'utilisateur doit se connecter.
 *
 * <p>Nommée {@code EmailProperties} et non {@code MailProperties} pour ne pas entrer en
 * collision avec la classe homonyme de Spring Boot, qui porte justement les
 * {@code spring.mail.*}.
 *
 * <p>Voir {@code instructions/FEATURE-service-envoi-email.md}.
 */
@ConfigurationProperties(prefix = "app.mail")
public class EmailProperties {

    /**
     * Interrupteur général. À {@code false}, les envois sont ignorés et tracés, mais les
     * endpoints qui les déclenchent continuent de fonctionner normalement — le mot de passe
     * temporaire reste dans la réponse HTTP.
     */
    private boolean actif = true;

    /** Adresse d'expédition, ex. {@code no-reply@exemple.ma}. Vide = service inactif. */
    private String expediteur;

    /** Nom affiché à côté de l'adresse d'expédition dans le client de messagerie. */
    private String nomExpediteur = "Ayad Estate";

    /**
     * URL de l'application, citée dans le corps du message pour que le destinataire sache où
     * se connecter. Jamais utilisée pour fabriquer un lien portant le mot de passe.
     */
    private String urlApplication;

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
}

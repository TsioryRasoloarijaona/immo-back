package com.tsix.immocrm.mail;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Implémentation SMTP du port {@link EnvoiEmail} : rend le template Thymeleaf puis remet le
 * message à {@code JavaMailSender} (configuré par les {@code spring.mail.*}).
 *
 * <p><b>Cette classe n'échoue jamais vers l'appelant.</b> Voir {@link EnvoiEmail#envoyer}.
 */
@Component
public class EnvoiEmailSmtp implements EnvoiEmail {

    private static final Logger log = LoggerFactory.getLogger(EnvoiEmailSmtp.class);

    private final JavaMailSender expedieur;
    private final ITemplateEngine moteurTemplate;
    private final EmailProperties properties;

    /**
     * Calculé une fois au démarrage plutôt qu'à chaque envoi : une configuration incomplète
     * doit se voir dans le journal de démarrage, pas seulement le jour où un administrateur
     * crée un employé et croit que le mail est parti.
     */
    private final boolean actif;

    /**
     * Adresse d'expédition réellement utilisée : {@code app.mail.expediteur}, à défaut le compte
     * SMTP lui-même.
     *
     * <p>Ce repli n'est pas une commodité. Titan (comme la plupart des hébergeurs mutualisés)
     * <b>impose</b> que l'expéditeur soit la boîte authentifiée : toute autre valeur est rejetée.
     * Les deux ne peuvent donc pratiquement pas différer, et exiger {@code MAIL_FROM} en plus de
     * {@code MAIL_USERNAME} ne faisait qu'ajouter une variable à oublier — ce qui est arrivé, et
     * rendait le service silencieusement inactif alors que les identifiants étaient bons.
     */
    private final String expediteurEffectif;

    public EnvoiEmailSmtp(JavaMailSender expedieur, ITemplateEngine moteurTemplate,
                          EmailProperties properties) {
        this.expedieur = expedieur;
        this.moteurTemplate = moteurTemplate;
        this.properties = properties;
        this.expediteurEffectif = resoudreExpediteur(expedieur, properties);
        this.actif = determinerActivite(expedieur, properties, this.expediteurEffectif);
    }

    private static String resoudreExpediteur(JavaMailSender expedieur, EmailProperties properties) {
        if (StringUtils.hasText(properties.getExpediteur())) {
            return properties.getExpediteur().trim();
        }
        return (expedieur instanceof JavaMailSenderImpl impl) ? impl.getUsername() : null;
    }

    /**
     * Le service n'est réellement actif que si l'interrupteur est levé <b>et</b> que les trois
     * éléments sans lesquels aucun message ne peut partir sont renseignés : le serveur, le
     * compte SMTP, et l'adresse d'expédition.
     */
    private static boolean determinerActivite(JavaMailSender expedieur, EmailProperties properties,
                                              String expediteurEffectif) {
        if (!properties.isActif()) {
            log.info("Envoi d'email désactivé (app.mail.actif=false) : les messages seront ignorés.");
            return false;
        }
        if (expedieur instanceof JavaMailSenderImpl impl
                && (!StringUtils.hasText(impl.getHost()) || !StringUtils.hasText(impl.getUsername()))) {
            log.warn("Envoi d'email INACTIF : configuration SMTP incomplète (spring.mail.host / "
                    + "spring.mail.username, variables MAIL_HOST / MAIL_USERNAME). "
                    + "Les mots de passe temporaires devront être communiqués à la main.");
            return false;
        }
        if (!StringUtils.hasText(expediteurEffectif)) {
            log.warn("Envoi d'email INACTIF : aucune adresse d'expédition (ni app.mail.expediteur "
                    + "via MAIL_FROM, ni spring.mail.username via MAIL_USERNAME). "
                    + "Les mots de passe temporaires devront être communiqués à la main.");
            return false;
        }
        log.info("Envoi d'email actif, expéditeur {}.", expediteurEffectif);
        return true;
    }

    @Override
    public boolean estActif() {
        return actif;
    }

    @Override
    public void envoyer(MessageEmail message) {
        if (!actif) {
            log.info("Envoi d'email ignoré (service inactif) : sujet « {} ».", message.sujet());
            return;
        }
        try {
            MimeMessage mime = expedieur.createMimeMessage();
            // multipart=true : indispensable pour joindre l'alternative texte au HTML.
            MimeMessageHelper aide = new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());
            aide.setFrom(expediteurEffectif, properties.getNomExpediteur());
            aide.setTo(message.destinataire());
            aide.setSubject(message.sujet());

            Context contexte = new Context(Locale.FRENCH, message.variables());
            String html = moteurTemplate.process(message.template(), contexte);

            if (StringUtils.hasText(message.corpsTexte())) {
                // L'ordre des arguments porte le sens : texte d'abord, HTML ensuite. Le client
                // affiche la DERNIÈRE alternative qu'il sait rendre.
                aide.setText(message.corpsTexte(), html);
            } else {
                aide.setText(html, true);
            }

            // Les pièces sont ajoutées APRÈS le corps : MimeMessageHelper construit la structure
            // multipart à la première écriture de texte, et une pièce ajoutée avant se
            // retrouverait dans la partie « alternative » au lieu de la partie « mixed » —
            // certains clients ne l'afficheraient alors pas du tout.
            for (PieceJointeEmail piece : message.piecesJointes()) {
                aide.addAttachment(piece.nom(),
                        new ByteArrayResource(piece.contenu()), piece.contentType());
            }

            expedieur.send(mime);
            log.info("Email envoyé : sujet « {} », destinataire {}, {} pièce(s) jointe(s).",
                    message.sujet(), AdresseMasquee.de(message.destinataire()),
                    message.piecesJointes().size());
        } catch (Exception e) {
            // Volontairement large et volontairement avalée : cette méthode est appelée depuis
            // un écouteur @Async exécuté après commit. Propager n'annulerait rien et ferait
            // seulement disparaître la trace dans l'exécuteur.
            log.error("Échec de l'envoi d'email : sujet « {} », destinataire {}. L'appel d'origine "
                            + "n'est pas affecté : un mot de passe temporaire reste dans sa réponse "
                            + "HTTP, une confirmation de visite est renvoyable depuis la prospection.",
                    message.sujet(), AdresseMasquee.de(message.destinataire()), e);
        }
    }
}

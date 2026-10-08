package com.tsix.immocrm.mail;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * L'implémentation SMTP, éprouvée avec le <b>vrai</b> moteur Thymeleaf et un
 * {@code JavaMailSender} gréé à la main — jamais avec un serveur réel.
 *
 * <p>Cette classe couvre les deux promesses que ni le double de test ni le test de bout en bout
 * ne peuvent établir : que le template se rend effectivement, et qu'une panne SMTP ne remonte
 * pas à l'appelant.
 */
@SpringBootTest
class EnvoiEmailSmtpTest {

    @Autowired
    private ITemplateEngine moteurTemplate;

    @Autowired
    private EmailProperties properties;

    private static final MessageEmail MESSAGE = new MessageEmail(
            "jean.rakoto@exemple.ma",
            "Vos identifiants Arpent",
            "mail/mot-de-passe-temporaire",
            Map.of("nomComplet", "Jean Rakoto",
                    "identifiant", "j.rakoto",
                    "motDePasse", "Ab3xK9mQp2Rt",
                    "urlArpent", "https://arpent.ayaddeveloppement.com"),
            "Identifiant : j.rakoto");

    /** {@code JavaMailSenderImpl} qui capture au lieu d'expédier. */
    private static class ExpediteurCapteur extends JavaMailSenderImpl {
        private final List<MimeMessage> envoyes = new CopyOnWriteArrayList<>();

        ExpediteurCapteur() {
            // Requis par le contrôle de démarrage d'EnvoiEmailSmtp : sans hôte ni compte, le
            // service se déclarerait inactif et n'enverrait rien.
            setHost("smtp-test.local");
            setUsername("test");
        }

        @Override
        public void send(MimeMessage mimeMessage) {
            envoyes.add(mimeMessage);
        }
    }

    private static String contenuBrut(MimeMessage message) throws Exception {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        message.writeTo(sortie);
        return sortie.toString(StandardCharsets.UTF_8);
    }

    @Test
    void leTemplateSeRendEtPorteLeMotDePasse() {
        Context contexte = new Context(Locale.FRENCH, MESSAGE.variables());
        String html = moteurTemplate.process(MESSAGE.template(), contexte);

        assertThat(html)
                .contains("Ab3xK9mQp2Rt")
                .contains("j.rakoto")
                .contains("Jean Rakoto")
                .contains("Se connecter à Arpent")
                .contains("identifiant Omran")
                // Le mot de passe n'est plus « provisoire » ni à changer (FEATURE-acces-arpent-depuis-omran B2).
                .doesNotContain("provisoire")
                .doesNotContain("temporaire");
    }

    /**
     * FEATURE-acces-arpent-depuis-omran B2 : le lien vers Arpent est présent (bouton et URL en
     * clair), et aucun {@code href} ne porte le mot de passe ni l'identifiant.
     */
    @Test
    void leTemplatePorteLeLienArpentSansSecretDansLesHref() {
        String html = moteurTemplate.process(MESSAGE.template(),
                new Context(Locale.FRENCH, MESSAGE.variables()));

        assertThat(html).contains("href=\"https://arpent.ayaddeveloppement.com\"");
        assertThat(html.split("https://arpent.ayaddeveloppement.com", -1)).hasSizeGreaterThan(2);

        java.util.regex.Matcher hrefs = java.util.regex.Pattern.compile("href=\"([^\"]*)\"").matcher(html);
        int nombre = 0;
        while (hrefs.find()) {
            nombre++;
            assertThat(hrefs.group(1)).doesNotContain("Ab3xK9mQp2Rt").doesNotContain("j.rakoto");
        }
        assertThat(nombre).isPositive();
    }

    @Test
    void envoyer_remetUnMessageCompletAuTransport() throws Exception {
        ExpediteurCapteur capteur = new ExpediteurCapteur();
        new EnvoiEmailSmtp(capteur, moteurTemplate, properties).envoyer(MESSAGE);

        assertThat(capteur.envoyes).hasSize(1);
        MimeMessage envoye = capteur.envoyes.get(0);

        assertThat(envoye.getSubject()).isEqualTo("Vos identifiants Arpent");
        assertThat(envoye.getAllRecipients()[0]).hasToString("jean.rakoto@exemple.ma");
        assertThat(envoye.getFrom()[0].toString()).contains(properties.getExpediteur());

        // Le mot de passe est en ASCII pur (l'alphabet de génération exclut les caractères
        // ambigus), il traverse donc le codage de transfert sans être découpé.
        assertThat(contenuBrut(envoye)).contains("Ab3xK9mQp2Rt");
    }

    /**
     * La promesse centrale de la feature : une panne SMTP est journalisée, jamais propagée.
     * Sans ce test, l'asynchronie n'est qu'une intention — et une exception qui remonterait
     * ici ferait échouer la création d'un employé pourtant correctement enregistré.
     */
    @Test
    void envoyer_panneSmtp_neRemontePasALAppelant() {
        JavaMailSenderImpl expedieurEnPanne = new JavaMailSenderImpl() {
            @Override
            public void send(MimeMessage mimeMessage) {
                throw new MailSendException("SMTP injoignable");
            }
        };
        expedieurEnPanne.setHost("smtp-test.local");
        expedieurEnPanne.setUsername("test");

        EnvoiEmailSmtp envoi = new EnvoiEmailSmtp(expedieurEnPanne, moteurTemplate, properties);

        assertThat(envoi.estActif()).isTrue();
        assertThatCode(() -> envoi.envoyer(MESSAGE)).doesNotThrowAnyException();
    }

    /**
     * Configuration incomplète : le service se déclare inactif au démarrage plutôt que
     * d'échouer au premier envoi, c'est-à-dire au moment exact où un administrateur croit que
     * le mail est parti.
     */
    @Test
    void configurationIncomplete_serviceInactifEtAucunEnvoi() {
        ExpediteurCapteur capteur = new ExpediteurCapteur();
        capteur.setUsername(null);

        EnvoiEmailSmtp envoi = new EnvoiEmailSmtp(capteur, moteurTemplate, properties);

        assertThat(envoi.estActif()).isFalse();
        assertThatCode(() -> envoi.envoyer(MESSAGE)).doesNotThrowAnyException();
        assertThat(capteur.envoyes).isEmpty();
    }

    /**
     * MAIL_FROM absent : l'expéditeur retombe sur le compte SMTP au lieu de désactiver le
     * service. Cas rencontré pour de vrai — la configuration de lancement portait
     * MAIL_USERNAME et MAIL_PASSWORD mais pas MAIL_FROM, et le service se déclarait inactif
     * alors que les identifiants étaient bons. Titan impose de toute façon que l'expéditeur
     * soit la boîte authentifiée, les deux valeurs ne peuvent donc pas diverger.
     */
    @Test
    void expediteurAbsent_retombeSurLeCompteSmtp() throws Exception {
        EmailProperties sansExpediteur = new EmailProperties();
        sansExpediteur.setActif(true);
        sansExpediteur.setNomExpediteur("Ayad Estate");
        // setExpediteur() volontairement non appelé.

        ExpediteurCapteur capteur = new ExpediteurCapteur();
        EnvoiEmailSmtp envoi = new EnvoiEmailSmtp(capteur, moteurTemplate, sansExpediteur);

        assertThat(envoi.estActif()).isTrue();
        envoi.envoyer(MESSAGE);

        assertThat(capteur.envoyes).hasSize(1);
        assertThat(capteur.envoyes.get(0).getFrom()[0].toString()).contains("test");
    }

    @Test
    void interrupteurBaisse_serviceInactifEtAucunEnvoi() {
        EmailProperties eteint = new EmailProperties();
        eteint.setActif(false);
        eteint.setExpediteur(properties.getExpediteur());

        ExpediteurCapteur capteur = new ExpediteurCapteur();
        EnvoiEmailSmtp envoi = new EnvoiEmailSmtp(capteur, moteurTemplate, eteint);

        assertThat(envoi.estActif()).isFalse();
        envoi.envoyer(MESSAGE);
        assertThat(capteur.envoyes).isEmpty();
    }
}

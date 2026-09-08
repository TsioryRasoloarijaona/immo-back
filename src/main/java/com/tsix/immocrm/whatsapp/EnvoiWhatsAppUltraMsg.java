package com.tsix.immocrm.whatsapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Locale;

/**
 * Implémentation UltraMsg du port {@link EnvoiWhatsApp} :
 * {@code POST /{instance}/messages/chat} en {@code application/x-www-form-urlencoded}, avec le
 * jeton dans le corps.
 *
 * <p><b>Cette classe n'échoue jamais vers l'appelant.</b> Voir {@link EnvoiWhatsApp#envoyer}.
 *
 * <p><b>Pourquoi {@code RestClient} et pas OkHttp</b> : le besoin est un POST de formulaire et
 * la lecture d'une réponse. {@code RestClient} vient avec {@code spring-boot-starter-webmvc},
 * déjà au classpath ; ajouter OkHttp aurait fait vivre deux piles HTTP dans le même processus
 * pour une capacité identique. L'appel ci-dessous est la transposition littérale de l'exemple
 * de la documentation UltraMsg.
 */
@Component
public class EnvoiWhatsAppUltraMsg implements EnvoiWhatsApp {

    private static final Logger log = LoggerFactory.getLogger(EnvoiWhatsAppUltraMsg.class);

    private final RestClient client;
    private final WhatsAppProperties properties;

    /**
     * Calculé une fois au démarrage plutôt qu'à chaque envoi : une configuration incomplète doit
     * se voir dans le journal de démarrage, pas seulement le jour où un commercial prend un
     * rendez-vous et croit que le prospect a été prévenu.
     */
    private final boolean actif;

    public EnvoiWhatsAppUltraMsg(WhatsAppProperties properties) {
        this.properties = properties;
        this.client = construireClient(properties);
        this.actif = determinerActivite(properties);
    }

    /**
     * Le client est construit ici plutôt qu'injecté : Spring Boot 4 avec
     * {@code spring-boot-starter-webmvc} <b>n'autoconfigure pas</b> de bean
     * {@code RestClient.Builder} — le contexte refusait de démarrer sur cette dépendance.
     *
     * <p>Le construire nous-mêmes a un second mérite : c'est le seul endroit où les délais
     * peuvent réellement être posés. Ils ne sont pas décoratifs. L'appel part d'un fil du pool
     * {@code mailExecutor}, partagé avec les envois d'email ; sans délai, une API qui ne répond
     * pas immobiliserait ce fil indéfiniment et retarderait tout ce qui suit dans la file.
     */
    private static RestClient construireClient(WhatsAppProperties properties) {
        SimpleClientHttpRequestFactory fabrique = new SimpleClientHttpRequestFactory();
        Duration delai = Duration.ofSeconds(properties.getTimeoutSecondes());
        fabrique.setConnectTimeout(delai);
        fabrique.setReadTimeout(delai);

        return RestClient.builder()
                .baseUrl(properties.getUrlBase())
                .requestFactory(fabrique)
                .build();
    }

    private static boolean determinerActivite(WhatsAppProperties properties) {
        if (!properties.isActif()) {
            log.info("Envoi WhatsApp désactivé (app.whatsapp.actif=false) : les messages seront ignorés.");
            return false;
        }
        if (!StringUtils.hasText(properties.getToken())) {
            log.warn("Envoi WhatsApp INACTIF : aucun jeton (app.whatsapp.token, variable WP_TOKEN). "
                    + "Les confirmations de visite ne partiront que par email.");
            return false;
        }
        if (!StringUtils.hasText(properties.getInstance())) {
            log.warn("Envoi WhatsApp INACTIF : aucune instance (app.whatsapp.instance, variable "
                    + "WP_INSTANCE). Les confirmations de visite ne partiront que par email.");
            return false;
        }
        log.info("Envoi WhatsApp actif, instance {}.", properties.getInstance());
        return true;
    }

    @Override
    public boolean estActif() {
        return actif;
    }

    @Override
    public boolean envoyer(MessageWhatsApp message) {
        if (!actif) {
            log.info("Envoi WhatsApp ignoré (service inactif) : destinataire {}.",
                    NumeroMasque.de(message.destinataire()));
            return false;
        }
        try {
            MultiValueMap<String, String> corps = new LinkedMultiValueMap<>();
            corps.add("token", properties.getToken());
            corps.add("to", message.destinataire());
            corps.add("body", message.texte());

            String reponse = client.post()
                    .uri("/{instance}/messages/chat", properties.getInstance())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(corps)
                    .retrieve()
                    .body(String.class);

            if (!estAcceptee(reponse)) {
                // La réponse est journalisée telle quelle : elle ne contient pas le jeton, qui
                // n'est que dans la requête, et c'est le seul moyen de diagnostiquer un refus
                // (numéro invalide, quota dépassé, instance déconnectée du téléphone).
                log.error("UltraMsg a refusé le message pour {} : {}",
                        NumeroMasque.de(message.destinataire()), reponse);
                return false;
            }

            log.info("Message WhatsApp envoyé à {}.", NumeroMasque.de(message.destinataire()));
            return true;
        } catch (Exception e) {
            // Volontairement large et volontairement avalée : appelée depuis un écouteur
            // exécuté après commit, hors transaction et hors du fil de la requête. Propager
            // n'annulerait rien et ferait seulement disparaître la trace dans l'exécuteur.
            log.error("Échec de l'envoi WhatsApp vers {}.",
                    NumeroMasque.de(message.destinataire()), e);
            return false;
        }
    }

    /**
     * UltraMsg répond {@code 200} même pour un refus, en plaçant le motif dans le corps JSON —
     * un succès porte {@code "sent":"true"}, un échec une clé {@code "error"}. Se fier au seul
     * code HTTP ferait donc passer tous les refus pour des envois réussis, et le champ
     * {@code envoye} de l'API mentirait au commercial.
     */
    private static boolean estAcceptee(String reponse) {
        if (!StringUtils.hasText(reponse)) {
            return false;
        }
        String normalisee = reponse.toLowerCase(Locale.ROOT).replace(" ", "");
        return normalisee.contains("\"sent\":\"true\"") || normalisee.contains("\"sent\":true");
    }
}

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
 * Port {@link EnvoiWhatsApp} par UltraMsg ({@code POST /{instance}/messages/chat}, formulaire, jeton dans le corps) ;
 * n'échoue jamais vers l'appelant. Choix de {@code RestClient} : FEATURE-mail-confirmation-visite.md §4 bis.
 */
@Component
public class EnvoiWhatsAppUltraMsg implements EnvoiWhatsApp {

    private static final Logger log = LoggerFactory.getLogger(EnvoiWhatsAppUltraMsg.class);

    private final RestClient client;
    private final WhatsAppProperties properties;

    /** Calculé au démarrage : une configuration incomplète doit se voir dans le journal de démarrage. */
    private final boolean actif;

    public EnvoiWhatsAppUltraMsg(WhatsAppProperties properties) {
        this.properties = properties;
        this.client = construireClient(properties);
        this.actif = determinerActivite(properties);
    }

    /**
     * Construit ici : Spring Boot 4 + webmvc n'autoconfigure pas {@code RestClient.Builder}. Délais obligatoires : le fil
     * vient de {@code mailExecutor}, partagé avec l'email, qu'une API muette immobiliserait.
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
            if (log.isInfoEnabled()) {
                log.info("Envoi WhatsApp ignoré (service inactif) : destinataire {}.",
                        NumeroMasque.de(message.destinataire()));
            }
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
                // Réponse journalisée telle quelle : sans le jeton (il n'est que dans la requête), seul diagnostic d'un refus.
                if (log.isErrorEnabled()) {
                    log.error("UltraMsg a refusé le message pour {} : {}",
                            NumeroMasque.de(message.destinataire()), reponse);
                }
                return false;
            }

            if (log.isInfoEnabled()) {
                log.info("Message WhatsApp envoyé à {}.", NumeroMasque.de(message.destinataire()));
            }
            return true;
        } catch (Exception e) {
            // Large et avalée exprès : appelée après commit hors du fil de la requête, propager ne servirait à rien.
            log.error("Échec de l'envoi WhatsApp vers {}.",
                    NumeroMasque.de(message.destinataire()), e);
            return false;
        }
    }

    /** UltraMsg répond 200 même pour un refus : seul {@code "sent":"true"} dans le corps vaut acceptation. */
    private static boolean estAcceptee(String reponse) {
        if (!StringUtils.hasText(reponse)) {
            return false;
        }
        String normalisee = reponse.toLowerCase(Locale.ROOT).replace(" ", "");
        return normalisee.contains("\"sent\":\"true\"") || normalisee.contains("\"sent\":true");
    }
}

package com.tsix.immocrm.whatsapp;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Active la configuration de l'envoi WhatsApp.
 *
 * <p>Aucun exécuteur dédié n'est déclaré ici : les envois WhatsApp partagent le pool
 * {@code mailExecutor} de {@code EmailConfig}, puisqu'ils sont déclenchés par le même écouteur
 * et sur le même événement. Leur donner un pool séparé n'apporterait rien tant que les deux
 * canaux partent ensemble — et ferait deux dimensionnements à tenir au lieu d'un.
 */
@Configuration
@EnableConfigurationProperties(WhatsAppProperties.class)
public class WhatsAppConfig {
}

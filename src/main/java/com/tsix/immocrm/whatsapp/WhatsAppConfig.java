package com.tsix.immocrm.whatsapp;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Pas d'exécuteur dédié : WhatsApp part du même écouteur que l'email, sur {@code mailExecutor}. */
@Configuration
@EnableConfigurationProperties(WhatsAppProperties.class)
public class WhatsAppConfig {
}

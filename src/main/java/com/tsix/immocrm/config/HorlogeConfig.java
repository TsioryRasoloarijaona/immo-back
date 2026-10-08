package com.tsix.immocrm.config;

import com.tsix.immocrm.domain.FuseauMetier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Seule source de « maintenant » des services : l'heure de Casablanca, quel que soit le fuseau du serveur. */
@Configuration
public class HorlogeConfig {

    @Bean
    public Clock horlogeMetier() {
        return Clock.system(FuseauMetier.ZONE);
    }
}

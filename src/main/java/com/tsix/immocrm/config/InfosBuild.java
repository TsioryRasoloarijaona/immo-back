package com.tsix.immocrm.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Version déployée : jauge {@code immo_back_build_info{tag,commit} = 1} et {@code /actuator/info}. Tag et commit
 * viennent des {@code build-args} du pipeline via l'environnement (pas de plugin Maven) ; {@code inconnu} hors image.
 */
@Component
public class InfosBuild implements InfoContributor {

    private final String tag;
    private final String commit;

    public InfosBuild(MeterRegistry registry,
                      @Value("${app.build.tag:inconnu}") String tag,
                      @Value("${app.build.commit:inconnu}") String commit) {
        this.tag = tag;
        this.commit = commit;
        Gauge.builder("immo.back.build.info", () -> 1)
                .description("Version de l'application en cours d'exécution : tag d'image et commit "
                        + "(valeur constante 1, l'information est dans les labels)")
                .tag("tag", tag)
                .tag("commit", commit)
                .register(registry);
    }

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("build", Map.of("tag", tag, "commit", commit));
    }
}

package com.tsix.immocrm.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Quelle version de l'application tourne — lisible dans Grafana et sur {@code /actuator/info}.
 *
 * <p>Le tag d'image ({@code sha-15be641}) et le SHA complet du commit entrent dans l'image au
 * build : le workflow les passe en {@code build-args}, le {@code Dockerfile} les fixe en
 * variables d'environnement {@code APP_BUILD_TAG} / {@code APP_BUILD_COMMIT}, et
 * {@code application.properties} les relit. Hors image (IDE, tests), les deux valent
 * {@code inconnu}.
 *
 * <p>Deux sorties, pour deux usages :
 * <ul>
 *   <li>la jauge {@code immo_back_build_info{tag,commit} = 1}, le motif Prometheus classique
 *       pour porter une version : la valeur est constante, l'information est dans les labels.
 *       Un panneau Grafana « Version déployée » l'affiche par {@code {{tag}}} ; un changement de
 *       tag se voit sur l'axe du temps comme une nouvelle série ;</li>
 *   <li>{@code GET /actuator/info} → {@code {"build":{"tag":…,"commit":…}}}, pour un
 *       {@code curl} depuis l'hôte (port 9091, loopback seulement).</li>
 * </ul>
 *
 * <p>Sans plugin Maven {@code build-info} ni {@code git-commit-id} : le {@code pom.xml} ne bouge
 * pas, et la vérité vient du pipeline qui a réellement construit l'image, pas d'un checkout.
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

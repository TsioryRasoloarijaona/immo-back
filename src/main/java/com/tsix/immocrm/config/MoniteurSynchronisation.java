package com.tsix.immocrm.config;

import com.tsix.immocrm.config.SynchronisationDescendante.ResultatSync;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Métriques du pipeline de synchronisation descendante (FEATURE-moniteur-pipeline-synchro.md). Signal central :
 * {@code sync_watermark_lag} par table, version source moins filigrane ; un retard qui persiste = table bloquée.
 */
@Component
@ConditionalOnExpression("'${app.source-de-verite.host:}'.trim() != ''")
public class MoniteurSynchronisation {

    private final Timer dureeCycle;
    private final Counter lignesFusionnees;
    private final Counter suppressions;
    private final Counter echecsTable;
    private final AtomicLong dernierSuccesEpoch = new AtomicLong(0);
    private final AtomicLong tablesEnEchecDernierCycle = new AtomicLong(0);
    private final AtomicLong versionSource = new AtomicLong(0);
    private final MultiGauge retardParTable;

    public MoniteurSynchronisation(MeterRegistry registry) {
        this.dureeCycle = Timer.builder("sync.cycle.duration")
                .description("Durée d'un cycle de synchronisation descendante")
                .register(registry);
        this.lignesFusionnees = Counter.builder("sync.rows.merged")
                .description("Lignes fusionnées dans la copie de lecture, cumulées")
                .register(registry);
        this.suppressions = Counter.builder("sync.rows.deleted")
                .description("Suppressions répercutées dans la copie, cumulées")
                .register(registry);
        this.echecsTable = Counter.builder("sync.table.failures")
                .description("Passes de table en échec (rejouées au cycle suivant), cumulées")
                .register(registry);
        Gauge.builder("sync.last.success.timestamp", dernierSuccesEpoch, AtomicLong::get)
                .baseUnit("seconds")
                .description("Horodatage epoch du dernier cycle exécuté — alerter sur son âge")
                .register(registry);
        Gauge.builder("sync.tables.failed", tablesEnEchecDernierCycle, AtomicLong::get)
                .description("Nombre de tables en échec au dernier cycle")
                .register(registry);
        Gauge.builder("sync.source.version", versionSource, AtomicLong::get)
                .description("Version Change Tracking courante de la source au dernier cycle")
                .register(registry);
        this.retardParTable = MultiGauge.builder("sync.watermark.lag")
                .description("Retard par table : version source − filigrane copie (0 = à jour)")
                .register(registry);
    }

    /** Jamais appelé pour un cycle sauté : un autre tourne, le pipeline est donc vivant. */
    void enregistrerCycle(ResultatSync resultat, Map<String, Long> retardsParTable) {
        dureeCycle.record(resultat.dureeMs(), TimeUnit.MILLISECONDS);
        lignesFusionnees.increment(resultat.fusions());
        suppressions.increment(resultat.suppressions());
        echecsTable.increment(resultat.tablesEnEchec());
        tablesEnEchecDernierCycle.set(resultat.tablesEnEchec());
        if (resultat.version() != null) {
            versionSource.set(resultat.version());
        }
        dernierSuccesEpoch.set(Instant.now().getEpochSecond());
        this.retardParTable.register(retardsParTable.entrySet().stream()
                .map(e -> MultiGauge.Row.of(Tags.of("table", e.getKey()), e.getValue()))
                .toList(), true);
    }
}

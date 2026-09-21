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
 * Métriques Micrometer du pipeline de synchronisation descendante, exposées sur
 * {@code /actuator/prometheus} (déjà câblé, port de management). Elles répondent aux deux
 * questions d'un tableau de suivi : <b>le pipeline est-il vivant ?</b> et <b>tout ce qui est
 * écrit dans la source — par ce backend comme par le logiciel de bureau — est-il bien importé
 * dans la copie ?</b>
 *
 * <p>Le signal central est le <b>retard par table</b> ({@code sync.watermark.lag}) : version
 * Change Tracking courante de la source moins le filigrane de la copie. {@code 0} = tout est
 * importé ; un retard qui persiste = une table bloquée (typiquement un ordre de clés étrangères
 * pas encore satisfait, qui converge normalement au cycle suivant). Les deux flux d'écriture
 * convergent ici, puisqu'ils passent tous deux par le Change Tracking de la source.
 *
 * <p>Même condition d'activation que {@link SynchronisationDescendante} : en mono-base, il n'y a
 * pas de pipeline, donc ni ce bean ni ses métriques.
 *
 * <h2>Métriques exposées</h2>
 * <ul>
 *   <li>{@code sync_cycle_duration_seconds} (timer) — durée d'un cycle.</li>
 *   <li>{@code sync_rows_merged_total} / {@code sync_rows_deleted_total} (compteurs) — lignes
 *       fusionnées / suppressions répercutées, cumulées.</li>
 *   <li>{@code sync_table_failures_total} (compteur) — passes de table en échec, cumulées.</li>
 *   <li>{@code sync_tables_failed} (jauge) — tables en échec au <b>dernier</b> cycle.</li>
 *   <li>{@code sync_last_success_timestamp_seconds} (jauge) — epoch du dernier cycle exécuté ;
 *       {@code time() - cette valeur} donne l'âge, sur lequel alerter (pipeline muet).</li>
 *   <li>{@code sync_source_version} (jauge) — version Change Tracking courante de la source au
 *       dernier cycle.</li>
 *   <li>{@code sync_watermark_lag{table="…"}} (multi-jauge) — retard par table.</li>
 * </ul>
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

    /**
     * Enregistre le résultat d'un cycle et rafraîchit le retard par table. Appelé en fin de
     * cycle réel (jamais pour un cycle sauté : un cycle sauté signifie qu'un autre tourne, donc
     * que le pipeline est vivant).
     *
     * @param resultat        le résumé du cycle
     * @param retardsParTable retard (version source − filigrane) par nom de table
     */
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

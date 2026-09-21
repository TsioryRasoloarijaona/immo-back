package com.tsix.immocrm.config;

import com.tsix.immocrm.config.SynchronisationDescendante.ResultatSync;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test unitaire du moniteur : pas de contexte Spring ni de base — un {@link SimpleMeterRegistry}
 * suffit à vérifier compteurs, jauges et la multi-jauge de retard par table.
 */
class MoniteurSynchronisationTest {

    private static Map<String, Long> retards(Object... paires) {
        Map<String, Long> m = new LinkedHashMap<>();
        for (int i = 0; i < paires.length; i += 2) {
            m.put((String) paires[i], ((Number) paires[i + 1]).longValue());
        }
        return m;
    }

    @Test
    void enregistrerCycle_alimenteCompteursJaugesEtRetards() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MoniteurSynchronisation moniteur = new MoniteurSynchronisation(registry);

        moniteur.enregistrerCycle(
                new ResultatSync(true, 84L, 5, 1, 10, 2, 123L),
                retards("ComProspection", 0L, "CfgTier", 3L));

        assertThat(registry.get("sync.rows.merged").counter().count()).isEqualTo(5.0);
        assertThat(registry.get("sync.rows.deleted").counter().count()).isEqualTo(1.0);
        assertThat(registry.get("sync.table.failures").counter().count()).isEqualTo(2.0);
        assertThat(registry.get("sync.tables.failed").gauge().value()).isEqualTo(2.0);
        assertThat(registry.get("sync.source.version").gauge().value()).isEqualTo(84.0);
        assertThat(registry.get("sync.last.success.timestamp").gauge().value()).isGreaterThan(0.0);
        assertThat(registry.get("sync.cycle.duration").timer().count()).isEqualTo(1L);

        assertThat(registry.get("sync.watermark.lag").tag("table", "CfgTier").gauge().value()).isEqualTo(3.0);
        assertThat(registry.get("sync.watermark.lag").tag("table", "ComProspection").gauge().value()).isEqualTo(0.0);
    }

    @Test
    void cyclesSuccessifs_cumulentLesCompteursEtRafraichissentLesJauges() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MoniteurSynchronisation moniteur = new MoniteurSynchronisation(registry);

        moniteur.enregistrerCycle(new ResultatSync(true, 84L, 5, 1, 10, 2, 100L),
                retards("CfgTier", 3L));
        moniteur.enregistrerCycle(new ResultatSync(true, 90L, 3, 0, 11, 0, 80L),
                retards("CfgTier", 0L));

        // Compteurs cumulés.
        assertThat(registry.get("sync.rows.merged").counter().count()).isEqualTo(8.0);
        assertThat(registry.get("sync.table.failures").counter().count()).isEqualTo(2.0);
        // Jauges rafraîchies à la dernière valeur.
        assertThat(registry.get("sync.tables.failed").gauge().value()).isEqualTo(0.0);
        assertThat(registry.get("sync.source.version").gauge().value()).isEqualTo(90.0);
        assertThat(registry.get("sync.watermark.lag").tag("table", "CfgTier").gauge().value()).isEqualTo(0.0);
        assertThat(registry.get("sync.cycle.duration").timer().count()).isEqualTo(2L);
    }

    @Test
    void versionNulle_nEcrasePasLaVersionSource() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MoniteurSynchronisation moniteur = new MoniteurSynchronisation(registry);

        moniteur.enregistrerCycle(new ResultatSync(true, 84L, 0, 0, 5, 0, 10L), retards());
        // Un cycle sans version (CT momentanément indisponible) ne remet pas la jauge à zéro.
        moniteur.enregistrerCycle(new ResultatSync(true, null, 0, 0, 0, 0, 10L), retards());

        assertThat(registry.get("sync.source.version").gauge().value()).isEqualTo(84.0);
    }
}

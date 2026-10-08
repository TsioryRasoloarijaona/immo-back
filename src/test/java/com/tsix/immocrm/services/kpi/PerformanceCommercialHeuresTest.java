package com.tsix.immocrm.services.kpi;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.zone.ZoneOffsetTransition;

import static org.assertj.core.api.Assertions.assertThat;

/** FIX-sonar-fiabilite.md, correction 6 : délai de prise en charge en heures réelles, fuseau marocain. */
@SuppressWarnings("java:S8700") // comparaison exprès avec l'ancienne durée sur heure murale
class PerformanceCommercialHeuresTest {

    private static final ZoneId CASABLANCA = ZoneId.of("Africa/Casablanca");

    @Test
    void horsChangementDHeure_commeLHeureMurale() {
        LocalDateTime debut = LocalDateTime.of(2026, 10, 1, 9, 15);
        LocalDateTime fin = LocalDateTime.of(2026, 10, 3, 17, 45);
        assertThat(PerformanceCommercialService.heuresEcoulees(debut, fin))
                .isEqualTo(Duration.between(debut, fin).toHours())
                .isEqualTo(56);
        assertThat(PerformanceCommercialService.heuresEcoulees(fin, debut)).isEqualTo(-56);
    }

    @Test
    void aChevalSurUnChangementDHeure_heuresReelles() {
        ZoneOffsetTransition t = CASABLANCA.getRules().nextTransition(Instant.parse("2026-01-01T00:00:00Z"));
        LocalDateTime avant = t.getDateTimeBefore().minusHours(12);
        LocalDateTime apres = avant.plusHours(24);
        long decalage = t.getDuration().toHours();
        assertThat(PerformanceCommercialService.heuresEcoulees(avant, apres)).isEqualTo(24 - decalage);
    }
}

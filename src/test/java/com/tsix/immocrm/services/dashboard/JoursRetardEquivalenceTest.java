package com.tsix.immocrm.services.dashboard;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/** Revue FIX-sonar-fiabilite §7 : joursEntre (offset UTC fixe) == l'ancien Duration.between(..).toDays() sur LocalDateTime. */
@SuppressWarnings("java:S8700") // l'ancienne formule naïve est la référence comparée, exprès
class JoursRetardEquivalenceTest {

    private static long ancien(LocalDateTime a, LocalDateTime b) {
        return Duration.between(a, b).toDays();
    }

    @Test
    void intervallesAleatoires_identiques_negatifsCompris() {
        Random r = new Random(42);
        LocalDateTime base = LocalDateTime.of(2020, 1, 1, 0, 0);
        for (int i = 0; i < 200_000; i++) {
            LocalDateTime a = base.plusSeconds(r.nextInt(200_000_000)).plusNanos(r.nextInt(1_000_000_000));
            LocalDateTime b = a.plusSeconds(r.nextInt(200_000_000) - 100_000_000L).plusNanos(r.nextInt(1_000_000_000));
            assertThat(DashboardService.joursEntre(a, b)).as("%s → %s", a, b).isEqualTo(ancien(a, b));
        }
    }

    @Test
    void bornes_identiques() {
        LocalDateTime a = LocalDateTime.of(2026, 3, 1, 10, 0);
        assertThat(DashboardService.joursEntre(a, a.plusDays(1).minusNanos(1))).isZero();
        assertThat(DashboardService.joursEntre(a, a.plusDays(1))).isEqualTo(1);
        LocalDateTime b = a.minusDays(1).plusNanos(500_000_000);
        assertThat(DashboardService.joursEntre(a, b)).isEqualTo(ancien(a, b)).isEqualTo(-1);
        // À cheval sur la fin du Ramadan 2026 au Maroc : jours au calendrier mural, pas un de moins.
        LocalDateTime debut = LocalDateTime.of(2026, 3, 1, 0, 0);
        assertThat(DashboardService.joursEntre(debut, debut.plusDays(60))).isEqualTo(60);
    }
}

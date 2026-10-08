package com.tsix.immocrm.services.dashboard;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/** Revue FIX-sonar-fiabilite §7 : ChronoUnit.DAYS.between == Duration.between(..).toDays() sur LocalDateTime. */
class JoursRetardEquivalenceTest {

    @Test
    void intervallesPositifs_identiques() {
        Random r = new Random(42);
        LocalDateTime base = LocalDateTime.of(2020, 1, 1, 0, 0);
        for (int i = 0; i < 200_000; i++) {
            LocalDateTime a = base.plusSeconds(r.nextInt(200_000_000)).plusNanos(r.nextInt(1_000_000_000));
            LocalDateTime b = a.plusSeconds(r.nextInt(100_000_000)).plusNanos(r.nextInt(1_000_000_000));
            assertThat(ChronoUnit.DAYS.between(a, b)).as("%s → %s", a, b).isEqualTo(Duration.between(a, b).toDays());
        }
        LocalDateTime a = LocalDateTime.of(2026, 3, 1, 10, 0);
        assertThat(ChronoUnit.DAYS.between(a, a.plusDays(1).minusNanos(1))).isZero();
        assertThat(ChronoUnit.DAYS.between(a, a.plusDays(1))).isEqualTo(1);
    }

    /** Négatif : inatteignable (earliestOverdue exige échéance &lt; now) ; mesuré pour le rapport. */
    @Test
    void intervallesNegatifs_ecartUniquementSousLaSeconde() {
        LocalDateTime a = LocalDateTime.of(2026, 3, 2, 10, 0, 0);
        LocalDateTime b = a.minusDays(1).plusNanos(500_000_000);
        assertThat(ChronoUnit.DAYS.between(a, b)).isZero();
        assertThat(Duration.between(a, b).toDays()).isEqualTo(-1); // Duration normalise en secondes plancher
        assertThat(ChronoUnit.DAYS.between(a, a.minusDays(3).minusHours(2))).isEqualTo(Duration.between(a, a.minusDays(3).minusHours(2)).toDays());
    }
}

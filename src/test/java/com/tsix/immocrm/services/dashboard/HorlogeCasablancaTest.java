package com.tsix.immocrm.services.dashboard;

import com.tsix.immocrm.config.HorlogeConfig;
import com.tsix.immocrm.domain.EcheanceRelance;
import com.tsix.immocrm.domain.FuseauMetier;
import com.tsix.immocrm.domain.PorteeRelance;
import com.tsix.immocrm.dto.dashboard.RelanceProspectionDto;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** FIX-horloge-casablanca.md (bug 66) : « aujourd'hui » est le jour de Casablanca, pas celui du serveur UTC. */
class HorlogeCasablancaTest {

    /** 00:30 le 09/10 à Casablanca ; l'instant suit la base tz du JDK (les runners CI n'ont pas tous la même). */
    private static final Instant MINUIT_ET_DEMIE_CASABLANCA =
            LocalDateTime.of(2026, 10, 9, 0, 30).atZone(FuseauMetier.ZONE).toInstant();

    /** Ramadan 2026 : Casablanca repasse à UTC+0, 23:30 UTC est encore le 01/03 sur place. */
    private static final Instant RAMADAN_23H30_UTC = Instant.parse("2026-03-01T23:30:00Z");

    @Test
    void horlogeDeProd_estCasablanca() {
        assertThat(ZoneId.of("Africa/Casablanca")).isEqualTo(FuseauMetier.ZONE);
        assertThat(new HorlogeConfig().horlogeMetier().getZone()).isEqualTo(FuseauMetier.ZONE);
    }

    @Test
    void entreMinuitEtUneHeure_aujourdhuiEstLeLendemainDuJourUtc() {
        Clock clock = Clock.fixed(MINUIT_ET_DEMIE_CASABLANCA, FuseauMetier.ZONE);

        assertThat(LocalDate.now(clock)).isEqualTo("2026-10-09");
        // Relance posée pour le 09/10 : du jour (0), quel que soit le jour UTC du même instant.
        assertThat(joursDeRetard(clock, LocalDate.of(2026, 10, 9))).isZero();
        assertThat(joursDeRetard(clock, LocalDate.of(2026, 10, 8))).isEqualTo(1);
        long ecartUtc = LocalDate.ofInstant(MINUIT_ET_DEMIE_CASABLANCA, ZoneOffset.UTC).toEpochDay()
                - LocalDate.of(2026, 10, 9).toEpochDay();
        assertThat(joursDeRetard(Clock.fixed(MINUIT_ET_DEMIE_CASABLANCA, ZoneOffset.UTC),
                LocalDate.of(2026, 10, 9))).isEqualTo(ecartUtc);
    }

    @Test
    void pendantLeRamadan_leDecalageSuitLaBaseTz() {
        assertThat(FuseauMetier.ZONE.getRules().getOffset(RAMADAN_23H30_UTC)).isEqualTo(ZoneOffset.UTC);
        Clock clock = Clock.fixed(RAMADAN_23H30_UTC, FuseauMetier.ZONE);

        assertThat(LocalDate.now(clock)).isEqualTo("2026-03-01");
        assertThat(LocalDateTime.now(clock)).isEqualTo("2026-03-01T23:30");
        assertThat(joursDeRetard(clock, LocalDate.of(2026, 3, 1))).isZero();
        assertThat(joursDeRetard(clock, LocalDate.of(2026, 3, 2))).isEqualTo(-1);
    }

    @SuppressWarnings("unchecked")
    private static long joursDeRetard(Clock clock, LocalDate dateRelance) {
        ComActionMessageRepository messages = mock(ComActionMessageRepository.class);
        PerimetreCommercialService perimetre = mock(PerimetreCommercialService.class);
        when(perimetre.filtreInteractions()).thenReturn((root, query, cb) -> null);

        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        ComActionMessage relance = new ComActionMessage();
        relance.setId(UUID.randomUUID());
        relance.setComProspection(prospection);
        relance.setAlertValidityDate(dateRelance.atStartOfDay());
        when(messages.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(relance)));

        RelanceDashboardService service = new RelanceDashboardService(
                messages, mock(ComProspectionRepository.class), perimetre, clock);
        List<RelanceProspectionDto> page = service.relances(EcheanceRelance.AUJOURDHUI, PorteeRelance.EQUIPE,
                null, null, PageRequest.of(0, 20)).getContent();
        return page.getFirst().joursDeRetard();
    }
}

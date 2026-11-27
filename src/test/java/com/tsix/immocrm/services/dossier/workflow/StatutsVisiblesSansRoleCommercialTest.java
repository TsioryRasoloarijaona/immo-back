package com.tsix.immocrm.services.dossier.workflow;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.str.StrIntervener;
import com.tsix.immocrm.repositories.str.StrIntervenerRepository;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.services.perimetre.PerimetreCommercialService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Rôle Commercial absent de {@code AdmRole} : rien n'est ajouté, aucune erreur (FEATURE-agences-correctifs-passe.md R-S4). */
class StatutsVisiblesSansRoleCommercialTest {

    @AfterEach
    void viderContexte() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void agenceSansRoleCommercialEnBase_aucunStatutVisible() {
        StrIntervenerRepository depot = mock(StrIntervenerRepository.class);
        PerimetreCommercialService perimetre = mock(PerimetreCommercialService.class);
        when(perimetre.estAgenceCourant()).thenReturn(true);
        when(depot.rolesParCode(any())).thenReturn(List.of());
        StrIntervener autreRole = new StrIntervener();
        autreRole.setAdmRoleId(UUID.randomUUID());
        when(depot.deStatuts(StatutDossier.CONFIG_ENTITY_ID))
                .thenReturn(List.<Object[]>of(new Object[]{UUID.randomUUID(), autreRole}));
        AuthenticatedUser principal = new AuthenticatedUser(UUID.randomUUID(), "agence", null, false, true, 0);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Agence"))));

        StatutsVisibles statuts = new StatutsVisibles(depot, new IntervenantCourant(depot, perimetre));

        assertThat(statuts.pourUtilisateurCourant()).hasValueSatisfying(ids -> assertThat(ids).isEmpty());
    }
}

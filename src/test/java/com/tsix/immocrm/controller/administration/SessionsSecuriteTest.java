package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureSessions;
import com.tsix.immocrm.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureSessions.ETAT;
import static com.tsix.immocrm.fixtures.FixtureSessions.FERMEE;
import static com.tsix.immocrm.fixtures.FixtureSessions.VERROUILLEE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Accès — FEATURE-admin-sessions.md, règle 9. Les 403 sont verts avant le code (règle d'URL de {@code SecurityConfig}) :
 * les témoins Administrateur prouvent que les routes existent.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SessionsSecuriteTest {

    private static final String URL = "/api/administration/sessions";
    private static final String URL_UTILISATEURS = URL + "/utilisateurs";
    private static final String URL_FERMER = URL + "/{id}/fermer";
    private static final String COMMERCIAL = "Commercial";
    private static final String DIRECTION_GENERALE = "Direction Générale";
    private static final LocalDateTime DATE = LocalDateTime.of(2026, 3, 10, 9, 15, 30);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureSessions fixture;

    private AdmUser admin;

    @BeforeEach
    void setUp() {
        fixture.assurerTables();
        admin = patrimoine.administrateur();
    }

    private RequestPostProcessor avecRole(String role) {
        AuthenticatedUser principal = new AuthenticatedUser(admin.getId(), admin.getCode(), null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    @ParameterizedTest
    @ValueSource(strings = {COMMERCIAL, DIRECTION_GENERALE})
    void r9_sansRoleAdministrateur_403_surLaListe(String role) throws Exception {
        mockMvc.perform(get(URL).with(avecRole(role))).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {COMMERCIAL, DIRECTION_GENERALE})
    void r9_sansRoleAdministrateur_403_surLesUtilisateurs(String role) throws Exception {
        mockMvc.perform(get(URL_UTILISATEURS).with(avecRole(role))).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {COMMERCIAL, DIRECTION_GENERALE})
    void r9_sansRoleAdministrateur_403_surFermer_aucuneLigneModifiee(String role) throws Exception {
        UUID ouverte = fixture.session(admin, DATE).inserer();
        UUID verrouillee = fixture.session(admin, DATE).avec(ETAT, VERROUILLEE).inserer();
        Map<String, String> ouverteAvant = fixture.ligne(ouverte);
        Map<String, String> verrouilleeAvant = fixture.ligne(verrouillee);

        mockMvc.perform(post(URL_FERMER, ouverte).with(avecRole(role))).andExpect(status().isForbidden());
        mockMvc.perform(post(URL_FERMER, verrouillee).with(avecRole(role))).andExpect(status().isForbidden());

        assertAll(
                () -> assertThat(fixture.ligne(ouverte)).isEqualTo(ouverteAvant),
                () -> assertThat(fixture.ligne(verrouillee)).isEqualTo(verrouilleeAvant));
    }

    @Test
    void r9_403_avantLe404_idInconnu() throws Exception {
        mockMvc.perform(post(URL_FERMER, UUID.randomUUID()).with(avecRole(COMMERCIAL)))
                .andExpect(status().isForbidden());
    }

    @Test
    void r9_403_avantLe409_sessionDejaFermee() throws Exception {
        UUID fermee = fixture.session(admin, DATE).avec(ETAT, FERMEE).inserer();

        mockMvc.perform(post(URL_FERMER, fermee).with(avecRole(COMMERCIAL))).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {URL + "?size=0", URL + "?size=201", URL + "?page=-1",
            URL + "?dateDebut=2026-03-11&dateFin=2026-03-10", URL + "?dateDebut=pas-une-date", URL + "?ouverte=peut-etre"})
    void r9_403_avantToutRefusDeSaisie_liste(String url) throws Exception {
        mockMvc.perform(get(url).with(avecRole(COMMERCIAL))).andExpect(status().isForbidden());
    }

    @Test
    void r9_403_avantLe400_identifiantIllisible() throws Exception {
        mockMvc.perform(post(URL + "/pas-un-uuid/fermer").with(avecRole(COMMERCIAL)))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ témoins

    @Test
    void r9_temoin_administrateur_200_surLaListe() throws Exception {
        mockMvc.perform(get(URL).with(connecte(admin))).andExpect(status().isOk());
    }

    @Test
    void r9_temoin_administrateur_200_surLesUtilisateurs() throws Exception {
        mockMvc.perform(get(URL_UTILISATEURS).with(connecte(admin))).andExpect(status().isOk());
    }

    @Test
    void r9_temoin_administrateur_200_surFermer() throws Exception {
        UUID ouverte = fixture.session(admin, DATE).inserer();

        mockMvc.perform(post(URL_FERMER, ouverte).with(connecte(admin))).andExpect(status().isOk());
    }
}

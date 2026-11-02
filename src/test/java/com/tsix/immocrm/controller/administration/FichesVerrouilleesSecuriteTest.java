package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureFichesVerrouillees;
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
import static com.tsix.immocrm.fixtures.FixtureFichesVerrouillees.ETAT;
import static com.tsix.immocrm.fixtures.FixtureFichesVerrouillees.NON_VERROUILLEE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Accès — FEATURE-admin-fiches-verrouillees.md, règle 10. Les 403 sont verts avant le code (règle d'URL de
 * {@code SecurityConfig}) : les témoins Administrateur prouvent que les routes existent.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FichesVerrouilleesSecuriteTest {

    private static final String URL = "/api/administration/fiches-verrouillees";
    private static final String URL_DEVERROUILLER = URL + "/{id}/deverrouiller";
    private static final String URL_TOUT = URL + "/deverrouiller-tout";
    private static final String COMMERCIAL = "Commercial";
    private static final String DIRECTION_GENERALE = "Direction Générale";
    private static final LocalDateTime DATE = LocalDateTime.of(2026, 3, 10, 9, 15, 30);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureSessions sessions;
    @Autowired private FixtureFichesVerrouillees fixture;

    private AdmUser admin;
    private UUID session;

    @BeforeEach
    void setUp() {
        sessions.assurerTables();
        fixture.assurerTable();
        admin = patrimoine.administrateur();
        session = sessions.session(admin, DATE).inserer();
    }

    private RequestPostProcessor avecRole(String role) {
        AuthenticatedUser principal = new AuthenticatedUser(admin.getId(), admin.getCode(), null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private FixtureFichesVerrouillees.Ligne verrou() {
        return fixture.verrou(session, UUID.randomUUID().toString(), DATE);
    }

    @ParameterizedTest
    @ValueSource(strings = {COMMERCIAL, DIRECTION_GENERALE})
    void r10_sansRoleAdministrateur_403_surLaListe(String role) throws Exception {
        mockMvc.perform(get(URL).with(avecRole(role))).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {COMMERCIAL, DIRECTION_GENERALE})
    void r10_sansRoleAdministrateur_403_surDeverrouiller_ligneInchangee(String role) throws Exception {
        UUID id = verrou().inserer();
        Map<String, String> avant = fixture.ligne(id);

        mockMvc.perform(post(URL_DEVERROUILLER, id).with(avecRole(role))).andExpect(status().isForbidden());

        assertThat(fixture.ligne(id)).isEqualTo(avant);
    }

    @ParameterizedTest
    @ValueSource(strings = {COMMERCIAL, DIRECTION_GENERALE})
    void r10_sansRoleAdministrateur_403_surDeverrouillerTout_ligneInchangee(String role) throws Exception {
        UUID id = verrou().inserer();
        Map<String, String> avant = fixture.ligne(id);

        mockMvc.perform(post(URL_TOUT).with(avecRole(role))).andExpect(status().isForbidden());

        assertThat(fixture.ligne(id)).isEqualTo(avant);
    }

    @Test
    void r10_403_avantLe404_idInconnu() throws Exception {
        mockMvc.perform(post(URL_DEVERROUILLER, UUID.randomUUID()).with(avecRole(COMMERCIAL)))
                .andExpect(status().isForbidden());
    }

    @Test
    void r10_403_avantLe409_ligneDejaNonVerrouillee() throws Exception {
        UUID liberee = verrou().avec(ETAT, NON_VERROUILLEE).inserer();

        mockMvc.perform(post(URL_DEVERROUILLER, liberee).with(avecRole(COMMERCIAL))).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {URL + "?size=0", URL + "?size=201", URL + "?page=-1", URL + "?page=1000001",
            URL + "?dateDebut=2026-03-11&dateFin=2026-03-10", URL + "?dateDebut=pas-une-date", URL + "?etat=PEUT_ETRE"})
    void r10_403_avantToutRefusDeSaisie_liste(String url) throws Exception {
        mockMvc.perform(get(url).with(avecRole(COMMERCIAL))).andExpect(status().isForbidden());
    }

    @Test
    void r10_403_avantLe400_identifiantIllisible() throws Exception {
        mockMvc.perform(post(URL + "/pas-un-uuid/deverrouiller").with(avecRole(COMMERCIAL)))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ témoins

    @Test
    void r10_temoin_administrateur_200_surLaListe() throws Exception {
        mockMvc.perform(get(URL).with(connecte(admin))).andExpect(status().isOk());
    }

    @Test
    void r10_temoin_administrateur_200_surDeverrouiller() throws Exception {
        UUID id = verrou().inserer();

        mockMvc.perform(post(URL_DEVERROUILLER, id).with(connecte(admin))).andExpect(status().isOk());
    }

    @Test
    void r10_temoin_administrateur_200_surDeverrouillerTout() throws Exception {
        mockMvc.perform(post(URL_TOUT).with(connecte(admin))).andExpect(status().isOk());
    }
}

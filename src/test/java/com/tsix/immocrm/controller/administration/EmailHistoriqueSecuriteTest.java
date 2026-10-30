package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureEmailHistorique;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureEmailHistorique.OBJET;
import static com.tsix.immocrm.fixtures.FixtureEmailHistorique.SMTP_HOST;
import static com.tsix.immocrm.fixtures.FixtureEmailHistorique.UTILISATEUR;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.jeton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Accès et confidentialité — FEATURE-admin-email-historique.md, règles 7 et 8, lecture seule. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EmailHistoriqueSecuriteTest {

    private static final String URL = "/api/administration/emails";
    private static final String URL_DETAIL = URL + "/{id}";
    private static final String URL_UTILISATEURS = URL + "/utilisateurs";
    private static final String COMMERCIAL = "Commercial";
    private static final String DIRECTION_GENERALE = "Direction Générale";
    private static final LocalDateTime DATE = LocalDateTime.of(2026, 3, 10, 9, 15, 30);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureEmailHistorique fixture;

    private AdmUser admin;
    private String jeton;
    private UUID email;

    @BeforeEach
    void setUp() {
        fixture.assurerTable();
        admin = patrimoine.administrateur();
        jeton = jeton();
        email = fixture.email(DATE).avec(OBJET, jeton).avec(UTILISATEUR, "Karim " + jeton).inserer();
    }

    private RequestPostProcessor avecRole(String role) {
        AuthenticatedUser principal = new AuthenticatedUser(admin.getId(), admin.getCode(), null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    // ------------------------------------------------------------------ règle 7

    @ParameterizedTest
    @ValueSource(strings = {COMMERCIAL, DIRECTION_GENERALE})
    void r7_sansRoleAdministrateur_403_surLaListe(String role) throws Exception {
        mockMvc.perform(get(URL).with(avecRole(role))).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {COMMERCIAL, DIRECTION_GENERALE})
    void r7_sansRoleAdministrateur_403_surLeDetail(String role) throws Exception {
        mockMvc.perform(get(URL_DETAIL, email).with(avecRole(role))).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {COMMERCIAL, DIRECTION_GENERALE})
    void decision_sansRoleAdministrateur_403_surLesUtilisateurs(String role) throws Exception {
        mockMvc.perform(get(URL_UTILISATEURS).with(avecRole(role))).andExpect(status().isForbidden());
    }

    // Le 403 passe avant toute validation : un refus de droits ne doit pas devenir un 400.
    @ParameterizedTest
    @ValueSource(strings = {URL + "?size=0", URL + "?page=-1", URL + "?dateDebut=2026-03-11&dateFin=2026-03-10",
            URL + "/pas-un-uuid"})
    void r7_sansRoleAdministrateur_403_avantLaValidation(String url) throws Exception {
        mockMvc.perform(get(url).with(avecRole(COMMERCIAL))).andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ règle 8

    private void sansConfigurationSmtp(String reponse, String temoin) {
        assertThat(reponse).contains(temoin)
                .doesNotContain(SMTP_HOST)
                .doesNotContainIgnoringCase("smtp")
                .doesNotContainIgnoringCase("ssl");
    }

    private String lire(MockHttpServletRequestBuilder requete) throws Exception {
        return mockMvc.perform(requete.with(connecte(admin))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void r8_liste_sansConfigurationSmtp() throws Exception {
        sansConfigurationSmtp(lire(get(URL).param("texte", jeton)), email.toString());
    }

    @Test
    void r8_detail_sansConfigurationSmtp() throws Exception {
        sansConfigurationSmtp(lire(get(URL_DETAIL, email)), email.toString());
    }

    @Test
    void r8_utilisateurs_sansConfigurationSmtp() throws Exception {
        sansConfigurationSmtp(lire(get(URL_UTILISATEURS)), "Karim " + jeton);
    }

    // ------------------------------------------------------------------ hors périmètre : aucune écriture

    static Stream<Arguments> ecritures() {
        return Stream.of(
                Arguments.of(HttpMethod.POST, URL),
                Arguments.of(HttpMethod.PUT, URL_DETAIL),
                Arguments.of(HttpMethod.PATCH, URL_DETAIL),
                Arguments.of(HttpMethod.DELETE, URL_DETAIL));
    }

    @ParameterizedTest
    @MethodSource("ecritures")
    void lectureSeule_aucuneRouteDEcriture_405(HttpMethod methode, String url) throws Exception {
        mockMvc.perform(request(methode, url, email).with(connecte(admin)))
                .andExpect(status().isMethodNotAllowed());

        lire(get(URL_DETAIL, email));
    }
}

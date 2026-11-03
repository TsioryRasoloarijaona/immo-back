package com.tsix.immocrm.controller.administration;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureFichiersAttaches;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureFichiersAttaches.CATEGORIE;
import static com.tsix.immocrm.fixtures.FixtureFichiersAttaches.FACTURE_ACHAT;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.jeton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Accès, lecture seule et ouverture d'un fichier — FEATURE-admin-fichiers-attaches.md, décision 6, règles testables
 * 4, 5 et 6, réponse R1 ({@code GET {id}/fichier} : 302 vers l'URL présignée, 404 sans objet vivant, rien d'écrit).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FichiersAttachesSecuriteTest {

    private static final String URL = "/api/administration/fichiers-attaches";
    private static final String URL_FILTRES = URL + "/filtres";
    private static final String URL_PIECE = URL + "/{id}";
    private static final String URL_FICHIER = URL + "/{id}/fichier";
    private static final String COMMERCIAL = "Commercial";
    private static final String DIRECTION_GENERALE = "Direction Générale";
    private static final String LOCATION = "Location";
    private static final LocalDateTime DATE = LocalDateTime.of(2026, 3, 10, 9, 15, 30);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureFichiersAttaches fixture;

    private AdmUser admin;
    private String jeton;
    private UUID piece;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        jeton = "FA" + jeton();
        piece = fixture.piece(jeton, DATE).inserer();
    }

    private RequestPostProcessor avecRole(String role) {
        return FixtureDossierVente.connecte(admin, role);
    }

    private MockHttpServletResponse fichier(UUID id) throws Exception {
        return mockMvc.perform(get(URL_FICHIER, id).with(connecte(admin))).andReturn().getResponse();
    }

    // ------------------------------------------------------------------ règle 4 : Administrateur seul, témoin 200

    @Test
    void r4_temoin_administrateur_200SurLaListe() throws Exception {
        mockMvc.perform(get(URL).with(connecte(admin))).andExpect(status().isOk());
    }

    @Test
    void r4_temoin_administrateur_200SurLesFiltres() throws Exception {
        mockMvc.perform(get(URL_FILTRES).with(connecte(admin))).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {COMMERCIAL, DIRECTION_GENERALE})
    void r4_sansRoleAdministrateur_403SurLaListe(String role) throws Exception {
        mockMvc.perform(get(URL).with(avecRole(role))).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {COMMERCIAL, DIRECTION_GENERALE})
    void r4_sansRoleAdministrateur_403SurLesFiltres(String role) throws Exception {
        mockMvc.perform(get(URL_FILTRES).with(avecRole(role))).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {COMMERCIAL, DIRECTION_GENERALE})
    void r6_sansRoleAdministrateur_403SurLeFichier(String role) throws Exception {
        mockMvc.perform(get(URL_FICHIER, piece).with(avecRole(role))).andExpect(status().isForbidden());
    }

    // Un refus de droits ne devient pas un 400 de liaison.
    @ParameterizedTest
    @ValueSource(strings = {URL + "?typeIds=pas-un-uuid", URL + "?du=pas-une-date", URL + "/pas-un-uuid/fichier"})
    void r4_sansRoleAdministrateur_403AvantLaValidation(String url) throws Exception {
        mockMvc.perform(get(url).with(avecRole(COMMERCIAL))).andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ règle 5 : aucune route d'écriture

    static Stream<Arguments> ecritures() {
        return Stream.of(
                Arguments.of(HttpMethod.POST, URL),
                Arguments.of(HttpMethod.PUT, URL_PIECE),
                Arguments.of(HttpMethod.PATCH, URL_PIECE),
                Arguments.of(HttpMethod.DELETE, URL_PIECE),
                Arguments.of(HttpMethod.POST, URL_FICHIER),
                Arguments.of(HttpMethod.PUT, URL_FICHIER),
                Arguments.of(HttpMethod.DELETE, URL_FICHIER));
    }

    @ParameterizedTest
    @MethodSource("ecritures")
    void r5_aucuneRouteDEcriture_405ou404(HttpMethod methode, String url) throws Exception {
        List<Object> avant = fixture.empreinte(piece);

        int statut = mockMvc.perform(request(methode, url, piece).with(connecte(admin))).andReturn().getResponse()
                .getStatus();

        assertThat(statut).isIn(404, 405);
        assertThat(fixture.empreinte(piece)).isEqualTo(avant);
    }

    // ------------------------------------------------------------------ R1 / règle 6 : ouverture du fichier

    @Test
    void r1_fichier_302VersLUrlPresigneeDeLObjet() throws Exception {
        MockHttpServletResponse reponse = fichier(piece);

        assertThat(reponse.getStatus()).isEqualTo(302);
        assertThat(reponse.getHeader(LOCATION)).isNotNull().contains(FixtureFichiersAttaches.cle(piece));
    }

    // Par son seul id : une pièce sans porteur Arpent (facture d'achat) s'ouvre aussi.
    @Test
    void r1_fichier_pieceSansEcranArpent_302() throws Exception {
        UUID facture = fixture.piece(jeton, DATE).avec(CATEGORIE, FACTURE_ACHAT).inserer();

        MockHttpServletResponse reponse = fichier(facture);

        assertThat(reponse.getStatus()).isEqualTo(302);
        assertThat(reponse.getHeader(LOCATION)).isNotNull().contains(FixtureFichiersAttaches.cle(facture));
    }

    @Test
    void r6_fichier_redirection_jamaisLeBinaire() throws Exception {
        MockHttpServletResponse reponse = fichier(piece);

        assertThat(reponse.getStatus()).isEqualTo(302);
        assertThat(reponse.getContentAsByteArray()).isEmpty();
    }

    @Test
    void r1_fichier_rienNEstEcrit() throws Exception {
        List<Object> avant = fixture.empreinte(piece);

        assertThat(fichier(piece).getStatus()).isEqualTo(302);
        assertThat(fixture.empreinte(piece)).isEqualTo(avant);
    }

    @Test
    void r1_fichier_idInconnu_404() throws Exception {
        assertThat(fichier(UUID.randomUUID()).getStatus()).isEqualTo(404);
    }

    @Test
    void r6_fichier_cocheeSansFichier_404() throws Exception {
        UUID cochee = fixture.piece(jeton, DATE).cocheeSansFichier().inserer();

        assertThat(fichier(cochee).getStatus()).isEqualTo(404);
    }

    @Test
    void r6_fichier_objetSupprimeLogiquement_404() throws Exception {
        UUID supprime = fixture.piece(jeton, DATE).fichierSupprime().inserer();

        assertThat(fichier(supprime).getStatus()).isEqualTo(404);
    }
}

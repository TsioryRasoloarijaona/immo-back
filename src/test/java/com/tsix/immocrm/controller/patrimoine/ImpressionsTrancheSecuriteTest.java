package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.domain.PorteeCleApi;
import com.tsix.immocrm.entities.adm.AdmRole;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppApiClient;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureExportPatrimoine;
import com.tsix.immocrm.fixtures.FixtureImpressionsTranche;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixturePremieresRelances.Jeu;
import com.tsix.immocrm.fixtures.FixtureRapports;
import com.tsix.immocrm.repositories.app.AppApiClientRepository;
import com.tsix.immocrm.security.ApiKeyAuthenticationFilter;
import com.tsix.immocrm.security.CleApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureImpressionsTranche.CONCRETISATION;
import static com.tsix.immocrm.fixtures.LectureExport.executer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lot P9f, règle P1 (FEATURE-patrimoine-impressions-tranche.md §1.1, §4 ; décision Q2 du 25/10) : Relance 2 réservée à
 * Administrateur et Direction Générale — tout autre rôle (Commercial, rôle libre de rapports) 403 avant le 400 du statut ; clé
 * d'API 403 {@code HORS_PORTEE} ; tranche d'entité inactive ou inconnue 404. Chaque 404 a son témoin 200 sur une tranche visible,
 * pour ne pas confondre avec une route absente.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ImpressionsTrancheSecuriteTest {

    private static final String APERCU = "/api/tranches/{id}/relances-2/apercu";
    private static final String LETTRES = "/api/tranches/{id}/relances-2.pdf";
    private static final String DG = "Direction Générale";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureDossierVente dossiers;
    @Autowired private FixtureRapports rapports;
    @Autowired private FixtureImpressionsTranche fixture;
    @Autowired private AppApiClientRepository clients;
    @Autowired private CleApiService cles;

    private Jeu jeu;

    @BeforeEach
    void setUp() {
        jeu = fixture.jeu();
        fixture.dossier(jeu, jeu.jeton() + "-SEC", fixture.statut(CONCRETISATION));
        fixture.relire();
    }

    private int statut(String route, Object trancheId, RequestPostProcessor qui, String... statuts) throws Exception {
        MockHttpServletRequestBuilder requete = get(route, trancheId).with(qui);
        if (statuts.length > 0) {
            requete = requete.param("statut", statuts);
        }
        return executer(mockMvc, requete).getResponse().getStatus();
    }

    // ------------------------------------------------------------------ rôles

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {APERCU, LETTRES})
    void p1_commercial_403_avantLe400DuStatut(String route) throws Exception {
        RequestPostProcessor commercial = FixtureExportPatrimoine.commercial(patrimoine.administrateur());

        assertAll(
                () -> assertThat(statut(route, jeu.tranche().getId(), commercial)).isEqualTo(403),
                () -> assertThat(statut(route, jeu.tranche().getId(), commercial, "RESERVATION")).isEqualTo(403));
    }

    /** Un rôle qui ouvre tous les rapports ({@code WithReportRestriction = 0}) n'est pas pour autant autorisé. */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {APERCU, LETTRES})
    void p1_roleLibreDeRapports_403(String route) throws Exception {
        String code = "LIBRE-" + jeu.jeton();
        AdmRole libre = rapports.role(code, false);
        AdmUser compte = rapports.compte("libre", libre);

        assertThat(statut(route, jeu.tranche().getId(), FixtureDossierVente.connecte(compte, code))).isEqualTo(403);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {APERCU, LETTRES})
    void p1_directionGenerale_200(String route) throws Exception {
        AdmUser dg = dossiers.compte("dg", DG);

        assertThat(statut(route, jeu.tranche().getId(), FixtureDossierVente.connecte(dg, DG))).isEqualTo(200);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {APERCU, LETTRES})
    void p1_cleApiToutesPortees_403_horsPortee(String route) throws Exception {
        String cle = cles.genererCle();
        AppApiClient client = new AppApiClient();
        client.setId(UUID.randomUUID());
        client.setNom("cle-p9f-" + UUID.randomUUID().toString().substring(0, 8));
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(patrimoine.administrateur());
        client.setPortees(PorteeCleApi.versColonne(EnumSet.allOf(PorteeCleApi.class)));
        client.setActif(true);
        client.setUserCreateDateTime(LocalDateTime.now());
        clients.save(client);

        mockMvc.perform(get(route, jeu.tranche().getId()).header(ApiKeyAuthenticationFilter.EN_TETE, cle))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
    }

    // ------------------------------------------------------------------ visibilité

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {APERCU, LETTRES})
    void p1_trancheDUneEntiteInactive_404_alorsQueLaTrancheActive200(String route) throws Exception {
        Jeu inactive = fixture.jeu(false);
        fixture.dossier(inactive, inactive.jeton() + "-INA", fixture.statut(CONCRETISATION));
        fixture.relire();
        AdmUser admin = patrimoine.administrateur();

        assertAll(
                () -> assertThat(statut(route, inactive.tranche().getId(), connecte(admin))).isEqualTo(404),
                () -> assertThat(statut(route, jeu.tranche().getId(), connecte(admin))).as("témoin").isEqualTo(200));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {APERCU, LETTRES})
    void p1_trancheInconnue_404_alorsQueLaTrancheActive200(String route) throws Exception {
        AdmUser admin = patrimoine.administrateur();

        assertAll(
                () -> assertThat(statut(route, UUID.randomUUID(), connecte(admin))).isEqualTo(404),
                () -> assertThat(statut(route, jeu.tranche().getId(), connecte(admin))).as("témoin").isEqualTo(200));
    }
}

package com.tsix.immocrm.controller.referentiel;

import com.tsix.immocrm.domain.PorteeCleApi;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppApiClient;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureReferentielsAdmin;
import com.tsix.immocrm.repositories.app.AppApiClientRepository;
import com.tsix.immocrm.security.ApiKeyAuthenticationFilter;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.security.CleApiService;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.ANNEE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.CODE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.ENTITE_DOSSIER_VENTE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.LIGNE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.OPERATEUR;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.ORIENTATIONS;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.QUOTES_PARTS_EXONEREES;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.RACINE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.RATIO;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.REGLEMENT_TOUJOURS;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.SEARCH;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.SEUILS_VERSEMENT;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.SORTING;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.STATUT_ID;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.TABLE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.TRANCHE_ID;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.TYPES_PRESENTATION;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.TYPE_REGLEMENT_ID;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.jeton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sécurité et visibilité du patron {@code /api/referentiels-admin} — FEATURE-patrimoine-referentiels.md §0 (écriture
 * Administrateur), §1 (lecture pour tous, slug inconnu 404), §5.1.8 / §8 (hors portée {@code REFERENTIELS_LIRE} :
 * 403 par clé d'API) et §5.2 « Pièges » R4 (tranche d'une entité inactive : ni listée ni acceptée, 404).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReferentielAdminSecuriteTest {

    private static final UUID ID = UUID.fromString("5B7C9D1E-2F3A-4B5C-8D6E-7F8091A2B3C4");
    private static final String TABLE_ORIENTATIONS = RACINE + "/" + ORIENTATIONS;
    private static final String TABLE_SEUILS = RACINE + "/" + SEUILS_VERSEMENT;
    private static final BigDecimal RATIO_TEST = new BigDecimal("0.25");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureReferentielsAdmin fixture;
    @Autowired private AppApiClientRepository clientRepository;
    @Autowired private CleApiService cleApiService;

    private AdmUser admin;
    private String jeton;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        jeton = jeton();
    }

    static Stream<Arguments> endpointsDEcriture() {
        return Stream.of(
                Arguments.of(HttpMethod.POST, TABLE_ORIENTATIONS),
                Arguments.of(HttpMethod.PATCH, TABLE_ORIENTATIONS + "/" + ID),
                Arguments.of(HttpMethod.DELETE, TABLE_ORIENTATIONS + "/" + ID),
                Arguments.of(HttpMethod.POST, TABLE_SEUILS),
                Arguments.of(HttpMethod.PATCH, TABLE_SEUILS + "/" + ID),
                Arguments.of(HttpMethod.DELETE, TABLE_SEUILS + "/" + ID));
    }

    private static RequestPostProcessor commercial(AdmUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getCode(), null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Commercial"))));
    }

    private String cleToutesPortees() {
        String cle = cleApiService.genererCle();
        AppApiClient client = new AppApiClient();
        client.setId(UUID.randomUUID());
        client.setNom("cle-referentiels-" + jeton);
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(admin);
        client.setPortees(PorteeCleApi.versColonne(EnumSet.allOf(PorteeCleApi.class)));
        client.setActif(true);
        client.setUserCreateDateTime(LocalDateTime.now());
        clientRepository.save(client);
        return cle;
    }

    private ResultActions poster(String table, CorpsJson corps) throws Exception {
        return mockMvc.perform(post(TABLE, table).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    // ------------------------------------------------------------------ rôles

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("endpointsDEcriture")
    void s0_commercial_403_surChaqueEcriture(HttpMethod methode, String url) throws Exception {
        mockMvc.perform(request(methode, url).with(commercial(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void s0_commercial_403_creationRefusee_rienEcrit() throws Exception {
        mockMvc.perform(post(TABLE, ORIENTATIONS).with(commercial(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec(CODE, "COM-" + jeton).avec(DESCRIPTION, "x").avec(SORTING, 1).json()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(TABLE, ORIENTATIONS).with(connecte(admin)).param(SEARCH, "COM-" + jeton))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..[?(@.code=='COM-" + jeton + "')]").doesNotExist());
    }

    @ParameterizedTest(name = "GET {0}")
    @ValueSource(strings = {RACINE, TABLE_ORIENTATIONS, TABLE_SEUILS})
    void s1_commercial_lectureOuverte_200(String url) throws Exception {
        mockMvc.perform(get(url).with(commercial(admin))).andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ clé d'API (hors REFERENTIELS_LIRE)

    @ParameterizedTest(name = "GET {0}")
    @ValueSource(strings = {RACINE, TABLE_ORIENTATIONS, TABLE_SEUILS})
    void r8_cleApi_403_horsPortee_lecture(String url) throws Exception {
        mockMvc.perform(get(url).header(ApiKeyAuthenticationFilter.EN_TETE, cleToutesPortees()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("endpointsDEcriture")
    void r8_cleApi_403_horsPortee_ecriture(HttpMethod methode, String url) throws Exception {
        mockMvc.perform(request(methode, url).header(ApiKeyAuthenticationFilter.EN_TETE, cleToutesPortees())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
    }

    // ------------------------------------------------------------------ slug inconnu

    @Test
    void s1_slugInconnu_404_enEcriture() throws Exception {
        poster(ORIENTATIONS, corps().avec(CODE, "X-" + jeton).avec(DESCRIPTION, "x").avec(SORTING, 1))
                .andExpect(status().isCreated());

        poster("table-inconnue", corps().avec(CODE, "X-" + jeton).avec(DESCRIPTION, "x")).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ R4 : tranche d'une entité inactive

    private CfgTranche tranche(String code, boolean entiteActive) {
        return patrimoine.tranche(code, patrimoine.projet("PRJ-" + code, patrimoine.entite("EJ-" + code, entiteActive)));
    }

    private UUID statutDeVente() {
        fixture.parametrageSeuils();
        return fixture.statut(ENTITE_DOSSIER_VENTE, "TST-R4-" + jeton);
    }

    @Test
    void r4_presentation_creation_404_trancheEntiteInactive() throws Exception {
        CfgTranche inactive = tranche("TIN-" + jeton, false);
        poster(TYPES_PRESENTATION, corps().avec(CODE, "PR4-" + jeton).avec(DESCRIPTION, "x")
                .avec(TRANCHE_ID, tranche("TAC-" + jeton, true).getId())).andExpect(status().isCreated());

        poster(TYPES_PRESENTATION, corps().avec(CODE, "PR4-" + jeton).avec(DESCRIPTION, "x")
                .avec(TRANCHE_ID, inactive.getId())).andExpect(status().isNotFound());
    }

    @Test
    void r4_presentation_liste_exclutLaTrancheInactive() throws Exception {
        UUID visible = fixture.presentation(tranche("TAC-" + jeton, true).getId(), "PR4A-" + jeton);
        UUID cachee = fixture.presentation(tranche("TIN-" + jeton, false).getId(), "PR4I-" + jeton);

        mockMvc.perform(get(TABLE, TYPES_PRESENTATION).with(connecte(admin)).param(SEARCH, "PR4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..[?(@.id=='" + visible + "')]").exists())
                .andExpect(jsonPath("$..[?(@.id=='" + cachee + "')]").doesNotExist());
    }

    @Test
    void r4_presentation_filtreTranche_404_trancheEntiteInactive() throws Exception {
        CfgTranche active = tranche("TAC-" + jeton, true);
        CfgTranche inactive = tranche("TIN-" + jeton, false);
        mockMvc.perform(get(TABLE, TYPES_PRESENTATION).with(connecte(admin)).param(TRANCHE_ID, active.getId().toString()))
                .andExpect(status().isOk());

        mockMvc.perform(get(TABLE, TYPES_PRESENTATION).with(connecte(admin))
                        .param(TRANCHE_ID, inactive.getId().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void r4_presentation_fiche_404_trancheEntiteInactive() throws Exception {
        UUID visible = fixture.presentation(tranche("TAC-" + jeton, true).getId(), "PR4A-" + jeton);
        UUID cachee = fixture.presentation(tranche("TIN-" + jeton, false).getId(), "PR4I-" + jeton);
        mockMvc.perform(get(LIGNE, TYPES_PRESENTATION, visible).with(connecte(admin))).andExpect(status().isOk());

        mockMvc.perform(get(LIGNE, TYPES_PRESENTATION, cachee).with(connecte(admin))).andExpect(status().isNotFound());
    }

    @Test
    void r4_seuil_creation_404_trancheEntiteInactive() throws Exception {
        UUID statut = statutDeVente();
        CfgTranche inactive = tranche("TIN-" + jeton, false);
        CorpsJson corps = corps().avec(STATUT_ID, statut).avec(TYPE_REGLEMENT_ID, REGLEMENT_TOUJOURS)
                .avec(OPERATEUR, 1).avec(RATIO, RATIO_TEST);
        poster(SEUILS_VERSEMENT, corps.avec(TRANCHE_ID, tranche("TAC-" + jeton, true).getId()))
                .andExpect(status().isCreated());

        poster(SEUILS_VERSEMENT, corps.avec(TRANCHE_ID, inactive.getId())).andExpect(status().isNotFound());

        assertThat(fixture.compter("SELECT COUNT(*) FROM CfgTrancheStatus WHERE StrStatusId = :id", statut)).isEqualTo(1);
    }

    @Test
    void r4_seuil_liste_exclutLaTrancheInactive() throws Exception {
        UUID statut = statutDeVente();
        UUID visible = fixture.seuilDeTranche(tranche("TAC-" + jeton, true).getId(), statut, 1, RATIO_TEST);
        UUID cache = fixture.seuilDeTranche(tranche("TIN-" + jeton, false).getId(), statut, 1, RATIO_TEST);

        mockMvc.perform(get(TABLE, SEUILS_VERSEMENT).with(connecte(admin)).param("size", "2000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..[?(@.id=='" + visible + "')]").exists())
                .andExpect(jsonPath("$..[?(@.id=='" + cache + "')]").doesNotExist());
    }

    @Test
    void r4_exoneration_creation_404_trancheEntiteInactive() throws Exception {
        CfgTranche inactive = tranche("TIN-" + jeton, false);
        poster(QUOTES_PARTS_EXONEREES, corps().avec(ANNEE, 2032).avec(TRANCHE_ID, tranche("TAC-" + jeton, true).getId()))
                .andExpect(status().isCreated());

        poster(QUOTES_PARTS_EXONEREES, corps().avec(ANNEE, 2032).avec(TRANCHE_ID, inactive.getId()))
                .andExpect(status().isNotFound());
    }
}

package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.domain.PorteeCleApi;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppApiClient;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.AppelsImportGenerique;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.app.AppApiClientRepository;
import com.tsix.immocrm.security.ApiKeyAuthenticationFilter;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.security.CleApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.ARTICLES;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.FICHIER;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.IMPORT;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.MODELE;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.SOUS_ARTICLES;
import static com.tsix.immocrm.fixtures.AppelsImportPatrimoine.requete;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.ENTITES;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.PROJETS;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.PROSPECTIONS;
import static com.tsix.immocrm.fixtures.FixtureImportGenerique.TRANCHES;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Accès de l'import — FEATURE-patrimoine-import-export.md §0.1 et §5 I1 : écriture et modèle réservés à
 * l'Administrateur (403 <b>avant</b> tout 400 : tranche absente, fichier illisible) ; clé d'API → 403 {@code HORS_PORTEE}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ImportPatrimoineSecuriteTest {

    private static final byte[] ILLISIBLE = "pas un classeur".getBytes(StandardCharsets.UTF_8);
    private static final String TRANCHE_ID = "trancheId";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private AppApiClientRepository clientRepository;
    @Autowired private CleApiService cleApiService;

    private CfgTranche tranche;

    @BeforeEach
    void setUp() {
        String jeton = UUID.randomUUID().toString().substring(0, 8);
        tranche = fixture.tranche("TR-IMPS-" + jeton, fixture.projet("PRJ-IMPS-" + jeton,
                fixture.entite("ENT-IMPS-" + jeton, true)));
    }

    private AdmUser compte(String code) {
        AdmUser user = fixture.administrateur();
        user.setCode(code + "." + UUID.randomUUID());
        return user;
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
        client.setNom("cle-p9a-" + UUID.randomUUID().toString().substring(0, 8));
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(compte("service.p9a"));
        client.setPortees(PorteeCleApi.versColonne(EnumSet.allOf(PorteeCleApi.class)));
        client.setActif(true);
        client.setUserCreateDateTime(LocalDateTime.now());
        clientRepository.save(client);
        return cle;
    }

    /** I1 : fichier illisible et tranche absente — le 403 passe avant les deux 400. */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {ARTICLES, SOUS_ARTICLES})
    void i1_commercial_import_403_avantTout400(String type) throws Exception {
        mockMvc.perform(requete(type, null, false, ILLISIBLE, commercial(compte("commercial.p9a"))))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {ARTICLES, SOUS_ARTICLES})
    void i1_commercial_importSurTrancheValide_403(String type) throws Exception {
        mockMvc.perform(requete(type, tranche.getId(), true, ILLISIBLE, commercial(compte("commercial.p9a.tr"))))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {ARTICLES, SOUS_ARTICLES})
    void r1_1_commercial_modele_403(String type) throws Exception {
        mockMvc.perform(get(MODELE, type).param(TRANCHE_ID, tranche.getId().toString())
                        .with(commercial(compte("commercial.p9a.modele"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void r0_1_cleApi_import_403_horsPortee() throws Exception {
        mockMvc.perform(multipart(IMPORT, ARTICLES).file(new MockMultipartFile(FICHIER, "import.xlsx", null, ILLISIBLE))
                        .param(TRANCHE_ID, tranche.getId().toString())
                        .header(ApiKeyAuthenticationFilter.EN_TETE, cleToutesPortees()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
    }

    // ------------------------------------------------------------------ P9e (FEATURE-patrimoine-import-generique.md §4 V1)

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {ENTITES, PROJETS, TRANCHES, PROSPECTIONS})
    void v1_commercial_importGenerique_403_avantTout400(String type) throws Exception {
        mockMvc.perform(AppelsImportGenerique.importer(type, false, ILLISIBLE, "{pas du json",
                        commercial(compte("commercial.p9e"))))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {ENTITES, PROJETS, TRANCHES, PROSPECTIONS})
    void v1_commercial_analyse_403(String type) throws Exception {
        mockMvc.perform(AppelsImportGenerique.analyse(type, ILLISIBLE, null, commercial(compte("commercial.p9e.an"))))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {ENTITES, PROJETS, TRANCHES, PROSPECTIONS})
    void v1_commercial_colonnes_403(String type) throws Exception {
        mockMvc.perform(AppelsImportGenerique.lireColonnes(type, commercial(compte("commercial.p9e.col"))))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {ENTITES, PROJETS, TRANCHES, PROSPECTIONS})
    void v1_commercial_modele_403(String type) throws Exception {
        mockMvc.perform(get(MODELE, type).with(commercial(compte("commercial.p9e.mod"))))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {ENTITES, PROJETS, TRANCHES, PROSPECTIONS})
    void v1_cleApi_importGenerique_403_horsPortee(String type) throws Exception {
        mockMvc.perform(multipart(IMPORT, type).file(new MockMultipartFile(FICHIER, "import.xlsx", null, ILLISIBLE))
                        .header(ApiKeyAuthenticationFilter.EN_TETE, cleToutesPortees()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
    }

    @Test
    void v1_cleApi_analyse_403_horsPortee() throws Exception {
        mockMvc.perform(multipart(AppelsImportGenerique.ANALYSE, ENTITES)
                        .file(new MockMultipartFile(FICHIER, "import.xlsx", null, ILLISIBLE))
                        .header(ApiKeyAuthenticationFilter.EN_TETE, cleToutesPortees()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
    }
}

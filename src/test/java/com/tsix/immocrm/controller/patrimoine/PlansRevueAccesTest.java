package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.domain.PorteeCleApi;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppApiClient;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkFloor;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixturePlans;
import com.tsix.immocrm.repositories.app.AppApiClientRepository;
import com.tsix.immocrm.security.ApiKeyAuthenticationFilter;
import com.tsix.immocrm.security.CleApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Revue P3 (FEATURE-patrimoine-plans.md, réponses du manager) : sélecteur {@code GET /api/plans/tranches} soumis au
 * périmètre et à R4 ; toutes les routes de plans hors portée des clés d'API, même pour un compte Administrateur.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PlansRevueAccesTest {

    private static final String COMMERCIAL = "Commercial";
    private static final String SELECTEUR = "/api/plans/tranches";
    private static final String IDS = "$[*].trancheId";
    private static final String D = "M 0,0 h 10 v 10 z";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureDossierVente vente;
    @Autowired private FixturePlans plans;
    @Autowired private AppApiClientRepository clientRepository;
    @Autowired private CleApiService cleApiService;

    private String jeton;
    private AdmUser admin;
    private CfgTranche trancheA;
    private CfgTranche trancheB;
    private CfgTranche trancheInactive;
    private StkItem articleA;
    private UUID planA;
    private RequestPostProcessor commercial;

    @BeforeEach
    void setUp() {
        jeton = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        admin = patrimoine.administrateur();
        StkFloor etage = plans.etage("REV-" + jeton);
        trancheA = plans.tranche("REV-A-" + jeton, true);
        trancheB = plans.tranche("REV-B-" + jeton, true);
        trancheInactive = plans.tranche("REV-I-" + jeton, false);
        articleA = plans.article(trancheA, "ART-A-" + jeton, false, etage);
        plans.trace(articleA.getId(), etage.getId(), D);

        AdmUser compte = vente.compte("REV-COM-" + jeton, COMMERCIAL);
        plans.habiliter(compte, trancheA.getCfgProject());
        commercial = FixtureDossierVente.connecte(compte, COMMERCIAL);

        planA = plans.plan(trancheA.getId(), etage.getId(), "Etage", FixturePlans.dataUriPng());
        plans.plan(trancheB.getId(), etage.getId(), "Etage", FixturePlans.dataUriPng());
        plans.plan(trancheInactive.getId(), etage.getId(), "Etage", FixturePlans.dataUriPng());
    }

    @Test
    void selecteur_commercial_neVoitQueLesTranchesDeSonPerimetre() throws Exception {
        mockMvc.perform(get(SELECTEUR).with(commercial))
                .andExpect(status().isOk())
                .andExpect(jsonPath(IDS, hasItem(trancheA.getId().toString())))
                .andExpect(jsonPath(IDS, not(hasItem(trancheB.getId().toString()))))
                .andExpect(jsonPath(IDS, not(hasItem(trancheInactive.getId().toString()))));
    }

    @Test
    void selecteur_administrateur_trancheDEntiteInactiveExclue() throws Exception {
        mockMvc.perform(get(SELECTEUR).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(IDS, hasItem(trancheB.getId().toString())))
                .andExpect(jsonPath(IDS, not(hasItem(trancheInactive.getId().toString()))));
    }

    @ParameterizedTest(name = "GET {0}")
    @ValueSource(strings = {"tranche", "image", "traces", "article", "selecteur"})
    void cleApiReferentiels_compteAdministrateur_routesDePlans_403(String route) throws Exception {
        String cle = cleApi();
        String url = switch (route) {
            case "tranche" -> "/api/tranches/" + trancheA.getId() + "/plans";
            case "image" -> "/api/plans/" + planA + "/image";
            case "traces" -> "/api/plans/" + planA + "/traces";
            case "article" -> "/api/articles/" + articleA.getId() + "/plans";
            default -> SELECTEUR;
        };

        mockMvc.perform(get(url).header(ApiKeyAuthenticationFilter.EN_TETE, cle))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
    }

    private String cleApi() {
        AdmUser compte = vente.compte("N8N-" + jeton, "Administrateur");
        String cle = cleApiService.genererCle();
        AppApiClient client = new AppApiClient();
        client.setId(UUID.randomUUID());
        client.setNom("n8n-plans-" + jeton);
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(compte);
        client.setPortees(PorteeCleApi.versColonne(EnumSet.allOf(PorteeCleApi.class)));
        client.setActif(true);
        client.setUserCreateDateTime(LocalDateTime.now());
        clientRepository.save(client);
        return cle;
    }
}

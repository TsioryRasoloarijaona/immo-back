package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.domain.PorteeCleApi;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppApiClient;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.ApiFaisabilite;
import com.tsix.immocrm.fixtures.FixtureChampsSpecifiques;
import com.tsix.immocrm.fixtures.FixtureExportPatrimoine;
import com.tsix.immocrm.fixtures.FixtureFaisabilite;
import com.tsix.immocrm.fixtures.FixtureFaisabiliteWorkflow;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.app.AppApiClientRepository;
import com.tsix.immocrm.security.ApiKeyAuthenticationFilter;
import com.tsix.immocrm.security.CleApiService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Second tour de revue P9d (règle 6) : chemins de visibilité non épinglés ailleurs — étude lue par la Direction Générale
 * mais pas écrite (décision 3), clé d'API toutes portées refusée de bout en bout sur l'aperçu, la lettre et la lecture des
 * champs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChampsRelancesSecondeRevueTest {

    private static final String CHAMPS = "/api/champs-specifiques/{porteur}/{id}";

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager em;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureChampsSpecifiques champs;
    @Autowired private FixtureFaisabilite faisabilite;
    @Autowired private AppApiClientRepository clients;
    @Autowired private CleApiService cles;

    private AdmUser admin;
    private String jeton;
    private CfgCompany entite;
    private CfgProject projet;
    private CfgTranche tranche;

    @BeforeEach
    void setUp() {
        champs.schema();
        admin = patrimoine.administrateur();
        jeton = FixtureExportPatrimoine.jeton().replace("P9C", "P9R");
        entite = patrimoine.entite("ENT-RV-" + jeton, true);
        projet = patrimoine.projet("PRJ-RV-" + jeton, entite);
        tranche = patrimoine.tranche("TR-RV-" + jeton, projet);
    }

    private UUID etude() throws Exception {
        faisabilite.parametrage();
        UUID id = new ApiFaisabilite(mockMvc, admin).etude(FixtureFaisabilite.fiche("EF-RV-" + jeton, entite.getId()));
        em.flush();
        em.clear();
        return id;
    }

    private String cleToutesPortees() {
        String cle = cles.genererCle();
        AppApiClient client = new AppApiClient();
        client.setId(UUID.randomUUID());
        client.setNom("cle-p9d-rv-" + UUID.randomUUID().toString().substring(0, 8));
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(admin);
        client.setPortees(PorteeCleApi.versColonne(EnumSet.allOf(PorteeCleApi.class)));
        client.setActif(true);
        client.setUserCreateDateTime(LocalDateTime.now());
        clients.save(client);
        return cle;
    }

    @Test
    void etude_directionGenerale_lit200() throws Exception {
        UUID etude = etude();

        mockMvc.perform(get(CHAMPS, "etude", etude)
                        .with(FixtureFaisabiliteWorkflow.avecRole(admin, FixtureFaisabiliteWorkflow.ROLE_DG)))
                .andExpect(status().isOk());
    }

    @Test
    void etude_directionGenerale_ecrit403() throws Exception {
        UUID etude = etude();

        mockMvc.perform(put(CHAMPS, "etude", etude)
                        .with(FixtureFaisabiliteWorkflow.avecRole(admin, FixtureFaisabiliteWorkflow.ROLE_DG))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valeurs\":[]}"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"/api/tranches/{id}/premieres-relances/apercu", "/api/tranches/{id}/premieres-relances.pdf"})
    void cleApiToutesPortees_relances_403HorsPortee(String route) throws Exception {
        String cle = cleToutesPortees();
        em.flush();

        mockMvc.perform(get(route, tranche.getId()).header(ApiKeyAuthenticationFilter.EN_TETE, cle))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
    }

    @Test
    void cleApiToutesPortees_lectureDesChamps_403HorsPortee() throws Exception {
        String cle = cleToutesPortees();
        em.flush();

        mockMvc.perform(get(CHAMPS, "projet", projet.getId()).header(ApiKeyAuthenticationFilter.EN_TETE, cle))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
    }
}

package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.domain.PorteeCleApi;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.app.AppApiClient;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixtureChampsSpecifiques;
import com.tsix.immocrm.fixtures.FixtureChampsSpecifiques.Definition;
import com.tsix.immocrm.fixtures.FixtureExportPatrimoine;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.app.AppApiClientRepository;
import com.tsix.immocrm.security.ApiKeyAuthenticationFilter;
import com.tsix.immocrm.security.CleApiService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.LectureExport.executer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lot P9d, règle I1 (FEATURE-patrimoine-import-export.md §0.1, §5) : relances et écritures des champs spécifiques réservées à
 * l'Administrateur, 403 <b>avant</b> tout 400 (corps invalide, tranche sans date de relance) ; clé d'API : 403
 * {@code HORS_PORTEE} sur toute écriture. Les cas sont posés sur des objets réels pour que le 400 soit le seul autre motif.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChampsSpecifiquesSecuriteTest {

    private static final String PUT_CHAMPS = "PUT /api/champs-specifiques/projet/{projet}";
    private static final String POST_DEFINITION = "POST /api/champs-specifiques/projet/definitions";
    private static final String PATCH_DEFINITION = "PATCH /api/champs-specifiques/projet/definitions/{champ}";
    private static final String DELETE_DEFINITION = "DELETE /api/champs-specifiques/projet/definitions/{champ}";
    private static final String POST_RELANCES = "POST /api/tranches/{tranche}/premieres-relances";
    private static final String APERCU = "GET /api/tranches/{tranche}/premieres-relances/apercu";
    private static final String LETTRES = "GET /api/tranches/{tranche}/premieres-relances.pdf";

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager em;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureChampsSpecifiques champs;
    @Autowired private AppApiClientRepository clients;
    @Autowired private CleApiService cles;

    private CfgProject projet;
    private CfgTranche tranche;
    private UUID champ;

    @BeforeEach
    void setUp() {
        champs.schema();
        String jeton = FixtureExportPatrimoine.jeton();
        projet = patrimoine.projet("PRJ-SEC-" + jeton, patrimoine.entite("ENT-SEC-" + jeton, true));
        tranche = patrimoine.tranche("TR-SEC-" + jeton, projet);
        champ = champs.champ(FixtureChampsSpecifiques.PROJET, new Definition("G", "Decimal obligatoire " + jeton,
                FixtureChampsSpecifiques.DECIMAL, null, true, 1, 1, null));
        em.flush();
        em.clear();
    }

    /** Requête dont le corps (ou la tranche sans {@code Revival1Date}) vaudrait 400 à un Administrateur. */
    private MockHttpServletRequestBuilder requete(String cas) {
        String[] parties = cas.split(" ", 2);
        String url = parties[1].replace("{projet}", projet.getId().toString())
                .replace("{tranche}", tranche.getId().toString()).replace("{champ}", champ.toString());
        String corps = cas.startsWith("PUT")
                ? "{\"valeurs\":[{\"champId\":\"" + champ + "\",\"valeur\":\"abc\"}]}"
                : "{}";
        return request(HttpMethod.valueOf(parties[0]), url).contentType(MediaType.APPLICATION_JSON).content(corps);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {PUT_CHAMPS, POST_DEFINITION, PATCH_DEFINITION, DELETE_DEFINITION, POST_RELANCES, APERCU,
            LETTRES})
    void i1_commercial_403_avantTout400(String cas) throws Exception {
        AdmUser compte = patrimoine.administrateur();

        int statut = executer(mockMvc, requete(cas).with(FixtureExportPatrimoine.commercial(compte)))
                .getResponse().getStatus();

        assertThat(statut).isEqualTo(403);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {PUT_CHAMPS, POST_DEFINITION, PATCH_DEFINITION, DELETE_DEFINITION, POST_RELANCES})
    void i1_cleApi_403_horsPortee_surChaqueEcriture(String cas) throws Exception {
        String cle = cles.genererCle();
        AppApiClient client = new AppApiClient();
        client.setId(UUID.randomUUID());
        client.setNom("cle-p9d-" + UUID.randomUUID().toString().substring(0, 8));
        client.setCleHash(CleApiService.hacher(cle));
        client.setClePrefixe(CleApiService.prefixeAffiche(cle));
        client.setAdmUser(patrimoine.administrateur());
        client.setPortees(PorteeCleApi.versColonne(EnumSet.allOf(PorteeCleApi.class)));
        client.setActif(true);
        client.setUserCreateDateTime(LocalDateTime.now());
        clients.save(client);

        mockMvc.perform(requete(cas).header(ApiKeyAuthenticationFilter.EN_TETE, cle))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiKeyAuthenticationFilter.CODE_HORS_PORTEE));
    }
}

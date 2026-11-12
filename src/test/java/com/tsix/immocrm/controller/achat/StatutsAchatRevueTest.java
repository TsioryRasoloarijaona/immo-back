package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.fixtures.FixtureConfigurationAchat;
import com.tsix.immocrm.fixtures.FixtureConfigurationAchat.Statut;
import com.tsix.immocrm.fixtures.FixtureStatutsAchat;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.CODE;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.corps;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.creation;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.id;
import static com.tsix.immocrm.controller.achat.LectureConfigurationAchat.TRANSITIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Revue du lot statuts-achat-parite : les listes MandatoryFields / SupplierMandatoryFields ne doivent jamais être NULL
 * (OMRAN fait {@code .Split(',')} dessus : Yes.Buy.Front.Config/BuyDocumentStatus.cs:118, Yes.Buy.Config/BuyDocumentWorkFlow.cs:569 ;
 * spec règle 7 « vide = chaîne vide ») ; unicité du code sans espaces de bord (règle 3).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StatutsAchatRevueTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper json;
    @Autowired private EntityManager em;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private FixtureStatutsAchat statuts;
    @Autowired private FixtureConfigurationAchat config;

    private String jeton;
    private RequestPostProcessor admin;
    private UUID type;

    @BeforeEach
    void setUp() {
        jeton = FixtureConfigurationAchat.jeton("K2R");
        statuts.tables();
        admin = statuts.administrateur(jeton);
        type = config.typeEtat("Nouveau " + jeton);
    }

    private long nulls(String table, UUID id) {
        em.flush();
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE PKey = ? AND (MandatoryFields IS NULL "
                + "OR SupplierMandatoryFields IS NULL)", Long.class, id.toString());
        return n == null ? 0 : n;
    }

    @Test
    void statutSansChampsObligatoires_listesStockeesEnChaineVide_pasNull() throws Exception {
        UUID id = id(mockMvc.perform(creation(json, corps("K2R-" + jeton, type)).with(admin))
                .andExpect(status().isCreated()).andReturn());

        assertThat(nulls("BuyDocumentStatus", id)).as("MandatoryFields / SupplierMandatoryFields NULL").isZero();
    }

    @Test
    void transitionCreee_listesDeChampsNonNull() throws Exception {
        UUID depart = config.statut(Statut.simple("RD-" + jeton, 1));
        UUID arrivee = config.statut(Statut.simple("RA-" + jeton, 2));
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("ordre", 1);
        c.put("description", "Revue " + jeton);
        c.put("statutDepartId", depart.toString());
        c.put("statutArriveeId", arrivee.toString());
        UUID id = id(mockMvc.perform(post(TRANSITIONS).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(c)).with(admin))
                .andExpect(status().isCreated()).andReturn());

        assertThat(nulls("BuyDocumentWorkFlow", id)).as("MandatoryFields / SupplierMandatoryFields NULL").isZero();
    }

    /** PUT = remplacement entier : sans gestionId en lecture, un aller-retour lecture → PUT déplace la transition en « Sans gestion ». */
    @Test
    void transitionLue_exposeGestionId_pourLeRemplacementEntier() throws Exception {
        UUID gestion = config.gestion("GR-" + jeton, "Gestion " + jeton);
        UUID depart = config.statut(Statut.simple("GD-" + jeton, 1));
        UUID arrivee = config.statut(Statut.simple("GA-" + jeton, 2));
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("description", "Gérée " + jeton);
        c.put("statutDepartId", depart.toString());
        c.put("statutArriveeId", arrivee.toString());
        c.put("gestionId", gestion.toString());
        String reponse = mockMvc.perform(post(TRANSITIONS).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(c)).with(admin))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        Map<String, Object> lu = json.readValue(reponse, Map.class);
        assertThat(String.valueOf(lu.get("gestionId"))).isEqualToIgnoringCase(gestion.toString());
    }

    @Test
    void codeAvecEspaceInsecableDeBord_409() throws Exception {
        String code = "K2R-" + jeton;
        mockMvc.perform(creation(json, corps(code, type)).with(admin)).andExpect(status().isCreated());
        Map<String, Object> doublon = corps(code, type);
        doublon.put(CODE, code + " ");

        mockMvc.perform(creation(json, doublon).with(admin)).andExpect(status().isConflict());
    }
}

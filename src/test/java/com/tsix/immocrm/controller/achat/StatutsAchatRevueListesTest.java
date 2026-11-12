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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.corps;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.modification;
import static com.tsix.immocrm.controller.achat.LectureConfigurationAchat.TRANSITIONS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Deuxième revue : listes MandatoryFields / SupplierMandatoryFields au PUT (lignes préexistantes NULL ou remplies). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StatutsAchatRevueListesTest {

    private static final String SQL_LISTES = "SELECT MandatoryFields, SupplierMandatoryFields FROM %s WHERE PKey = ?";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper json;
    @Autowired private EntityManager em;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private FixtureStatutsAchat statuts;
    @Autowired private FixtureConfigurationAchat config;

    private String jeton;
    private RequestPostProcessor admin;
    private UUID depart;
    private UUID arrivee;

    @BeforeEach
    void setUp() {
        jeton = FixtureConfigurationAchat.jeton("K2L");
        statuts.tables();
        admin = statuts.administrateur(jeton);
        depart = config.statut(Statut.simple("LD-" + jeton, 1));
        arrivee = config.statut(Statut.simple("LA-" + jeton, 2));
    }

    private List<String> listes(String table, UUID id) {
        em.flush();
        em.clear();
        return jdbc.queryForObject(SQL_LISTES.formatted(table), (rs, i) -> java.util.Arrays.asList(rs.getString(1),
                rs.getString(2)), id.toString());
    }

    private Map<String, Object> corpsTransition() {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("ordre", 4);
        c.put("description", "Modifiée " + jeton);
        c.put("statutDepartId", depart.toString());
        c.put("statutArriveeId", arrivee.toString());
        return c;
    }

    private void putTransition(UUID id) throws Exception {
        mockMvc.perform(put(TRANSITIONS + "/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(corpsTransition())).with(admin)).andExpect(status().isOk());
    }

    @Test
    void transitionOmranAvecListesRemplies_put_lesConserve() throws Exception {
        UUID id = config.transition(null, 1, "OMRAN " + jeton, depart, arrivee);
        jdbc.update("UPDATE BuyDocumentWorkFlow SET MandatoryFields = N',DocRef,Notes', "
                + "SupplierMandatoryFields = N',Rib' WHERE PKey = ?", id.toString());

        putTransition(id);

        assertThat(listes("BuyDocumentWorkFlow", id)).containsExactly(",DocRef,Notes", ",Rib");
    }

    @Test
    void transitionOmranAvecListesNull_put_chaineVide() throws Exception {
        UUID id = config.transition(null, 1, "OMRAN " + jeton, depart, arrivee);

        putTransition(id);

        assertThat(listes("BuyDocumentWorkFlow", id)).containsExactly("", "");
    }

    @Test
    void statutOmranAvecListesNull_putSansChamps_chaineVide() throws Exception {
        UUID type = config.typeEtat("Type " + jeton);
        UUID id = config.statut(Statut.simple("LS-" + jeton, 5));

        mockMvc.perform(modification(json, id, corps("LS-" + jeton, type)).with(admin)).andExpect(status().isOk());

        assertThat(listes("BuyDocumentStatus", id)).containsExactly("", "");
    }

    @Test
    void statutPutAvecChamps_stockeVirguleDeTete() throws Exception {
        UUID type = config.typeEtat("Type " + jeton);
        UUID id = config.statut(Statut.simple("LC-" + jeton, 6));
        Map<String, Object> c = corps("LC-" + jeton, type);
        c.put(EcritureStatutsAchat.CHAMPS_PIECE, List.of("DocRef", "Notes"));
        c.put(EcritureStatutsAchat.CHAMPS_FOURNISSEUR, List.of("Rib"));

        mockMvc.perform(modification(json, id, c).with(admin)).andExpect(status().isOk());

        assertThat(listes("BuyDocumentStatus", id)).containsExactly(",DocRef,Notes", ",Rib");
    }
}

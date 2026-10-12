package com.tsix.immocrm.controller.note;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixturePatrimoine.ROLE_ADMIN;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.connecte;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FEATURE-patrimoine-audit-actions.md §4 et §8. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NotesControllerTest {

    private static final String URL = "/api/notes";
    private static final String PORTEUR = "porteur";
    private static final String TRANCHE = "tranche";
    private static final String TEXTE = "texteHtml";
    private static final String AUTEUR = "Compte ADM-NOTE";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private EntityManager em;
    @Autowired private JdbcTemplate jdbc;

    private AdmUser admin;
    private CfgTranche tranche;

    @BeforeEach
    void setUp() {
        admin = fixture.utilisateur("ADM-NOTE");
        tranche = fixture.tranche("TR-NOTE", fixture.projet("PRJ-NOTE", fixture.entite("ENT-NOTE", true)));
    }

    private Map<String, Object> corps(String porteur, UUID id, String texte) {
        Map<String, Object> corps = new HashMap<>();
        corps.put(PORTEUR, porteur);
        corps.put("id", id);
        corps.put(TEXTE, texte);
        return corps;
    }

    private ResultActions creer(Map<String, Object> corps, String... roles) throws Exception {
        return mockMvc.perform(post(URL).with(connecte(admin, roles))
                .contentType(MediaType.APPLICATION_JSON).content(json(corps)));
    }

    @Test
    void creation_201_assainieEtColonnesOmran() throws Exception {
        String reponse = creer(corps(TRANCHE, tranche.getId(),
                        "<p onclick=\"x()\">Bornage <b>lundi</b></p><script>alert(1)</script>"), ROLE_ADMIN)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.texteHtml").value("<p>Bornage <b>lundi</b></p>"))
                .andExpect(jsonPath("$.auteur").value(AUTEUR))
                .andReturn().getResponse().getContentAsString();
        em.flush();

        UUID id = UUID.fromString(JsonPath.read(reponse, "$.id"));
        Map<String, Object> ligne = jdbc.queryForMap("SELECT * FROM AdmMessage WHERE PKey = ?", id);
        assertThat(ligne)
                .containsEntry("Origin", "")
                .containsEntry("IsIn", null)
                .containsEntry("UserName", AUTEUR)
                .containsEntry("ReminderDate", null);
        assertThat(ligne.get("CategoryId").toString()).isEqualToIgnoringCase(tranche.getId().toString());
        assertThat(ligne.get("Description").toString()).matches("\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2} " + AUTEUR);
        assertThat(ligne.get("DocDate")).isNotNull();
    }

    @Test
    void liste_plusRecentesDAbord() throws Exception {
        creer(corps(TRANCHE, tranche.getId(), "première"), ROLE_ADMIN).andExpect(status().isCreated());
        em.flush();
        jdbc.update("UPDATE AdmMessage SET DocDate = DATEADD(MINUTE, -1, DocDate) WHERE CategoryId = ?",
                tranche.getId());
        creer(corps(TRANCHE, tranche.getId(), "seconde"), ROLE_ADMIN).andExpect(status().isCreated());

        mockMvc.perform(get(URL).param(PORTEUR, TRANCHE).param("id", tranche.getId().toString())
                        .with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].texteHtml").value(contains("seconde", "première")));
    }

    @Test
    void creation_400_noteVide() throws Exception {
        creer(corps(TRANCHE, tranche.getId(), "<p> &nbsp; </p>"), ROLE_ADMIN)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champ").value(TEXTE))
                .andExpect(jsonPath("$.detail").value("veuillez saisir une note !"));
    }

    @Test
    void creation_400_porteurInvalide() throws Exception {
        creer(corps("dossier", tranche.getId(), "x"), ROLE_ADMIN)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champ").value(PORTEUR));
    }

    @Test
    void creation_404_ficheInconnue() throws Exception {
        creer(corps("article", tranche.getId(), "x"), ROLE_ADMIN).andExpect(status().isNotFound());
    }

    @Test
    void creation_403_nonAdministrateur() throws Exception {
        creer(corps(TRANCHE, tranche.getId(), "x")).andExpect(status().isForbidden());
    }

    @Test
    void liste_404_ficheInconnue() throws Exception {
        mockMvc.perform(get(URL).param(PORTEUR, "projet").param("id", UUID.randomUUID().toString())
                        .with(connecte(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void suppression_204_puis404() throws Exception {
        String reponse = creer(corps(TRANCHE, tranche.getId(), "à supprimer"), ROLE_ADMIN)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(reponse, "$.id");

        mockMvc.perform(delete(URL + "/{id}", id).with(connecte(admin, ROLE_ADMIN))).andExpect(status().isNoContent());
        mockMvc.perform(delete(URL + "/{id}", id).with(connecte(admin, ROLE_ADMIN))).andExpect(status().isNotFound());
    }

    @Test
    void suppression_403_nonAdministrateur() throws Exception {
        mockMvc.perform(delete(URL + "/{id}", UUID.randomUUID()).with(connecte(admin)))
                .andExpect(status().isForbidden());
    }
}

package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureInteraction;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.services.prospection.ContexteInteraction;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.FixturePatrimoine.ROLE_ADMIN;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.connecte;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FEATURE-patrimoine-audit-actions.md §3 et §8. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ActionsArticleTest {

    private static final String URL = "/api/articles/{id}/actions";
    private static final String DESCRIPTION = "description";
    private static final String TYPE_ID = "actionTypeId";
    private static final String DATE_ALERTE = "dateAlerte";
    private static final String AUTEUR = "Compte ADM-ACT-ART";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureInteraction interactions;
    @Autowired private EntityManager em;
    @Autowired private JdbcTemplate jdbc;

    private AdmUser admin;
    private StkItem article;

    @BeforeEach
    void setUp() {
        admin = fixture.utilisateur("ADM-ACT-ART");
        article = fixture.article(fixture.chaine("ACT-ART"), "ART-ACT");
        interactions.categorieProspection();
    }

    private Map<String, Object> note(String message) {
        Map<String, Object> corps = new HashMap<>();
        corps.put(TYPE_ID, interactions.typeNote().getId());
        corps.put(DESCRIPTION, message);
        return corps;
    }

    private ResultActions creer(UUID articleId, Map<String, Object> corps, String... roles) throws Exception {
        return mockMvc.perform(post(URL, articleId).with(connecte(admin, roles))
                .contentType(MediaType.APPLICATION_JSON).content(json(corps)));
    }

    private UUID creerId(Map<String, Object> corps) throws Exception {
        String reponse = creer(article.getId(), corps, ROLE_ADMIN).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(reponse, "$.id"));
    }

    @Test
    void creation_201_colonnesDeLaLigneModeleOmran() throws Exception {
        creer(article.getId(), note("Visite du lot avec l'architecte"), ROLE_ADMIN)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.utilisateur").value(AUTEUR))
                .andExpect(jsonPath("$.typeAction").value("Note"))
                .andExpect(jsonPath("$.message").value("Visite du lot avec l'architecte"))
                .andExpect(jsonPath("$.articleCode").value("ART-ACT"))
                .andExpect(jsonPath("$.categorie").value("Prospection"));
        em.flush();

        Map<String, Object> ligne = jdbc.queryForMap("SELECT * FROM ComActionMessage WHERE StkItemId = ?",
                article.getId());
        assertThat(ligne)
                .containsEntry("UserName", AUTEUR)
                .containsEntry("UserCreate", AUTEUR)
                .containsEntry("UserUpdate", AUTEUR)
                .containsEntry("IsSystem", false)
                .containsEntry("IsNotRead", false)
                .containsEntry("IsUnpaid", false)
                .containsEntry("WithoutRevival", false)
                .containsEntry("ComProspectionId", null)
                .containsEntry("ComFolderId", null);
        assertThat(ligne.get("Description").toString()).matches("\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2} " + AUTEUR);
        assertThat(Stream.of("DocDate", "UserCreateDateTime", "UserUpdateDateTime", "AdmUserId",
                        "ComActionMessageTypeId", "ComActionMessageCategoryId", "AlertMessage").map(ligne::get))
                .doesNotContainNull();
        assertThat(ligne.get("ComActionMessageCategoryId").toString())
                .isEqualToIgnoringCase(ContexteInteraction.CATEGORIE_PROSPECTION.toString());
    }

    @Test
    void liste_plusRecentesDAbord_etSeulementCellesDeLArticle() throws Exception {
        UUID premiere = creerId(note("première"));
        em.flush();
        // Deux créations peuvent tomber sur le même pas de datetime (1/300 s) : on recule la première.
        jdbc.update("UPDATE ComActionMessage SET DocDate = DATEADD(MINUTE, -1, DocDate) WHERE PKey = ?", premiere);
        creerId(note("seconde"));
        StkItem autre = fixture.article(fixture.chaine("ACT-ART2"), "ART-ACT2");
        creer(autre.getId(), note("ailleurs"), ROLE_ADMIN).andExpect(status().isCreated());

        mockMvc.perform(get(URL, article.getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].message").value(contains("seconde", "première")));
    }

    @Test
    void nouvelleAlerte_desactiveLaPrecedenteDeLArticle() throws Exception {
        Map<String, Object> premiere = note("rappeler le notaire");
        premiere.put(DATE_ALERTE, LocalDateTime.now().plusDays(3).toString());
        UUID ancienne = creerId(premiere);
        Map<String, Object> seconde = note("rappeler le géomètre");
        seconde.put(DATE_ALERTE, LocalDateTime.now().plusDays(5).toString());
        UUID nouvelle = creerId(seconde);
        em.flush();

        assertThat(jdbc.queryForObject("SELECT WithoutRevival FROM ComActionMessage WHERE PKey = ?",
                Boolean.class, ancienne)).isTrue();
        assertThat(jdbc.queryForObject("SELECT Active FROM AdmAlertData WHERE RelatedKey = ?", Boolean.class, ancienne))
                .isFalse();
        assertThat(jdbc.queryForObject("SELECT Active FROM AdmAlertData WHERE RelatedKey = ?", Boolean.class, nouvelle))
                .isTrue();
    }

    @Test
    void creation_400_noteSansMessage() throws Exception {
        creer(article.getId(), note("  "), ROLE_ADMIN).andExpect(status().isBadRequest());
    }

    @Test
    void creation_400_actionAutreQueNoteSansResultat() throws Exception {
        Map<String, Object> corps = note("appel");
        corps.put(TYPE_ID, interactions.typeTelephone().getId());
        creer(article.getId(), corps, ROLE_ADMIN).andExpect(status().isBadRequest());
    }

    @Test
    void creation_400_typeInconnu() throws Exception {
        Map<String, Object> corps = note("x");
        corps.put(TYPE_ID, UUID.randomUUID());
        creer(article.getId(), corps, ROLE_ADMIN).andExpect(status().isBadRequest());
    }

    @Test
    void creation_403_nonAdministrateur() throws Exception {
        creer(article.getId(), note("x")).andExpect(status().isForbidden());
    }

    @Test
    void creation_404_articleInconnu() throws Exception {
        creer(UUID.randomUUID(), note("x"), ROLE_ADMIN).andExpect(status().isNotFound());
    }

    @Test
    void liste_404_articleInconnu() throws Exception {
        mockMvc.perform(get(URL, UUID.randomUUID()).with(connecte(admin))).andExpect(status().isNotFound());
    }
}

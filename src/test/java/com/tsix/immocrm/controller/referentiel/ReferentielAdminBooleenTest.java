package com.tsix.immocrm.controller.referentiel;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureReferentielsAdmin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.CATEGORIES;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.LIGNE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.TABLE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.jeton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Un champ BOOLEEN n'accepte que true / false : toute autre valeur est refusée en 400 sans rien écrire. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReferentielAdminBooleenTest {

    private static final String CHAMP = "pourSousArticles";
    private static final String DETAIL_INVALIDE = "$.detail";
    private static final String CATEGORIES_DE_LA_NATURE = "SELECT COUNT(*) FROM StkItemCategory WHERE StkKindId = :id";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureReferentielsAdmin fixture;

    private AdmUser admin;
    private String jeton;
    private UUID nature;
    private UUID comptable;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        jeton = jeton();
        nature = fixture.nature("NBO-" + jeton);
        comptable = fixture.categorieComptable("SBO-" + jeton);
    }

    private String corps(Object valeur) {
        return "{\"natureId\":\"%s\",\"code\":\"CBO-%s\",\"description\":\"Avant\",\"categorieComptableId\":\"%s\",\"%s\":%s}"
                .formatted(nature, jeton, comptable, CHAMP, valeur);
    }

    private ResultActions ecrire(MockHttpServletRequestBuilder requete, String json) throws Exception {
        return mockMvc.perform(requete.with(connecte(admin)).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"abc\"", "1"})
    void creation_valeurNonBooleenne_400_rienEcrit(String valeur) throws Exception {
        ecrire(post(TABLE, CATEGORIES), corps(valeur)).andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL_INVALIDE).value(org.hamcrest.Matchers.containsString(CHAMP + " : valeur invalide")));

        assertThat(fixture.compter(CATEGORIES_DE_LA_NATURE, nature)).isZero();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void creation_booleenValide_201(boolean valeur) throws Exception {
        ecrire(post(TABLE, CATEGORIES), corps(valeur)).andExpect(status().isCreated());

        assertThat(fixture.compter(CATEGORIES_DE_LA_NATURE, nature)).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"abc\"", "1"})
    void modification_valeurNonBooleenne_400_valeurConservee(String valeur) throws Exception {
        UUID id = creerAvec(true);

        ecrire(patch(LIGNE, CATEGORIES, id), "{\"%s\":%s}".formatted(CHAMP, valeur)).andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL_INVALIDE).value(org.hamcrest.Matchers.containsString(CHAMP + " : valeur invalide")));

        assertThat(fixture.compter("SELECT COUNT(*) FROM StkItemCategory WHERE PKey = :id AND ForUnderItem = 1", id))
                .isEqualTo(1);
    }

    @Test
    void modification_booleenValide_200() throws Exception {
        UUID id = creerAvec(true);

        ecrire(patch(LIGNE, CATEGORIES, id), "{\"%s\":false}".formatted(CHAMP)).andExpect(status().isOk());

        assertThat(fixture.compter("SELECT COUNT(*) FROM StkItemCategory WHERE PKey = :id AND ForUnderItem = 0", id))
                .isEqualTo(1);
    }

    private UUID creerAvec(boolean valeur) throws Exception {
        String json = ecrire(post(TABLE, CATEGORIES), corps(valeur)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }
}

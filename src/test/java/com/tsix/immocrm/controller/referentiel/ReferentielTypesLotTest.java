package com.tsix.immocrm.controller.referentiel;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureProjetConfig;
import com.tsix.immocrm.fixtures.FixtureReferentielsAdmin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureProjetConfig.SLUG_TYPES_LOT;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Types de lot ({@code PrjMarketType}) par le moteur {@code /api/referentiels-admin/types-lot} — FEATURE-projet-config.md
 * §8.5 et réponse 1 du manager : règles 3 (lecture pour tout authentifié), 4 (code unique) et 5 (409 si un lot l'utilise).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReferentielTypesLotTest {

    private static final String TABLE_SQL = "PrjMarketType";
    private static final String COMPTE_TYPE = "SELECT COUNT(*) FROM PrjMarketType WHERE PKey = :id";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureReferentielsAdmin fixture;
    @Autowired private FixtureProjetConfig projetConfig;

    private AdmUser admin;
    private String jeton;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        jeton = jeton();
    }

    private ResultActions ecrire(MockHttpServletRequestBuilder requete, CorpsJson corps) throws Exception {
        return mockMvc.perform(requete.with(connecte(admin)).contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private static CorpsJson corpsType(String code) {
        return corps().avec(CODE, code).avec(DESCRIPTION, "Type " + code);
    }

    @Test
    void p85_typesLot_familleProjetDansLesDescripteurs() throws Exception {
        mockMvc.perform(get(RACINE).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.slug == '" + SLUG_TYPES_LOT + "')].famille", hasItem("PROJET")));
    }

    @Test
    void creation_201_ligneEcrite() throws Exception {
        String code = "TN-" + jeton;

        String json = ecrire(post(TABLE, SLUG_TYPES_LOT), corpsType(code))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        UUID id = UUID.fromString(JsonPath.read(json, "$.id"));
        assertThat(fixture.lignes(TABLE_SQL, id, code, "Type " + code)).isEqualTo(1);
    }

    @Test
    void r3_lecture_200_pourUnNonAdministrateur() throws Exception {
        UUID type = fixture.typeMarche("TL-" + jeton);

        mockMvc.perform(get(TABLE, SLUG_TYPES_LOT).with(FixtureDossierVente.connecte(admin, "Commercial")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(type.toString())));
    }

    @Test
    void r4_creation_codeEnDoubleCasseEtEspaces_409() throws Exception {
        fixture.typeMarche("TLD-" + jeton);

        ecrire(post(TABLE, SLUG_TYPES_LOT), corpsType(" tld-" + jeton.toLowerCase(Locale.ROOT) + " "))
                .andExpect(status().isConflict());
    }

    @Test
    void r4_creation_codeManquant_400() throws Exception {
        ecrire(post(TABLE, SLUG_TYPES_LOT), corpsType("TC-" + jeton).sans(CODE)).andExpect(status().isBadRequest());
    }

    @Test
    void modification_200_puisIdInconnu_404() throws Exception {
        UUID type = fixture.typeMarche("TU-" + jeton);

        ecrire(patch(LIGNE, SLUG_TYPES_LOT, type), corps().avec(DESCRIPTION, "Renomme " + jeton))
                .andExpect(status().isOk());
        ecrire(patch(LIGNE, SLUG_TYPES_LOT, UUID.randomUUID()), corps().avec(DESCRIPTION, "Renomme " + jeton))
                .andExpect(status().isNotFound());

        assertThat(fixture.lignes(TABLE_SQL, type, "TU-" + jeton, "Renomme " + jeton)).isEqualTo(1);
    }

    @Test
    void r5_suppression_typeUtiliseParUnLot_409_rienSupprime() throws Exception {
        UUID type = fixture.typeMarche("TM-" + jeton);
        UUID lot = projetConfig.lotDeType("LOT-" + jeton, type);

        mockMvc.perform(delete(LIGNE, SLUG_TYPES_LOT, type).with(connecte(admin))).andExpect(status().isConflict());

        assertThat(new int[]{fixture.compter(COMPTE_TYPE, type),
                fixture.compter("SELECT COUNT(*) FROM PrjMarket WHERE PKey = :id", lot)}).containsExactly(1, 1);
    }

    @Test
    void r5_suppression_typeUtiliseParUnePieceAchat_409() throws Exception {
        UUID type = fixture.typeMarche("TB-" + jeton);
        projetConfig.reference("INSERT INTO BuyDocument (PKey, Code, PrjMarketTypeId) VALUES (:id, :code, :projet)",
                "BD-" + jeton, type);

        mockMvc.perform(delete(LIGNE, SLUG_TYPES_LOT, type).with(connecte(admin))).andExpect(status().isConflict());

        assertThat(fixture.compter(COMPTE_TYPE, type)).isEqualTo(1);
    }

    @Test
    void r5_suppression_typeLibre_204_puisIdInconnu_404() throws Exception {
        UUID type = fixture.typeMarche("TS-" + jeton);

        mockMvc.perform(delete(LIGNE, SLUG_TYPES_LOT, type).with(connecte(admin))).andExpect(status().isNoContent());
        mockMvc.perform(delete(LIGNE, SLUG_TYPES_LOT, UUID.randomUUID()).with(connecte(admin)))
                .andExpect(status().isNotFound());

        assertThat(fixture.compter(COMPTE_TYPE, type)).isZero();
    }
}

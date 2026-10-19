package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureArticleFiche;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Compléments à ArticleFicheTest — FEATURE-patrimoine-article-fiche.md §11.2, §11.3, §11.5, §11.6. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ArticleFicheComplementsTest {

    private static final String DETAIL = "$.detail";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureArticleFiche fiche;

    private AdmUser admin;
    private String jeton;
    private CfgProject projet;
    private CfgTranche tranche;

    @BeforeEach
    void setUp() {
        fixture.referentielArticle();
        admin = fixture.administrateur();
        jeton = "P2AC" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        projet = fixture.projet("PRJ-" + jeton, fixture.entite("ENT-" + jeton, true));
        tranche = fixture.tranche("TR-" + jeton, projet);
    }

    private CorpsJson article(String code, boolean sous) {
        return corps().avec("code", code + "-" + jeton).avec("description", "Article " + code)
                .avec("categorieId", fixture.categorie("CAT-" + code + jeton, fixture.natureBat(), sous).getId())
                .avec("typeId", fixture.type("TYP-" + code + jeton, fixture.natureBat(), sous).getId())
                .avec("immeubleId", fixture.immeuble("IMM-" + code + jeton).getId())
                .avec("etageId", fixture.etage("ETG-" + code + jeton).getId())
                .avec("surfaceProvisoire", BigDecimal.TEN).avec("surfaceDefinitive", BigDecimal.TEN);
    }

    private ResultActions poster(String url, CorpsJson corps, Object... variables) throws Exception {
        return mockMvc.perform(post(url, variables).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(corps.json()));
    }

    private UUID creer(CorpsJson corps) throws Exception {
        String json = poster("/api/articles", corps.avec("trancheId", tranche.getId()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    private ResultActions modifier(UUID id, CorpsJson corps) throws Exception {
        return mockMvc.perform(patch("/api/articles/{id}", id).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    @Test
    void r11_2_creationAvecPresentation_contenancesDuTypeRecopiees() throws Exception {
        UUID chambre = fiche.contenance("CH-" + jeton, "", false);
        UUID presentation = fiche.presentation("PT-" + jeton, tranche.getId());
        fiche.contenanceDePresentation(presentation, chambre, 2, "", false, 0);

        UUID id = creer(article("CREA", false).avec("presentationId", presentation));

        assertThat(fiche.guids("SELECT StkContenanceId FROM StkItemContenance WHERE StkItemId = :id", id))
                .containsExactly(guid(chambre));
    }

    @Test
    void r11_3_presentationDUneAutreTranche_400() throws Exception {
        UUID ailleurs = fiche.presentation("PT-A-" + jeton, fixture.tranche("TR-A-" + jeton, projet).getId());

        poster("/api/articles", article("AUTRE", false).avec("trancheId", tranche.getId())
                .avec("presentationId", ailleurs))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString("presentationId")));
    }

    @Test
    void r11_5_patchDUnSousArticle_vocationId_400() throws Exception {
        UUID parent = creer(article("PAR", false));
        String json = poster("/api/articles/{id}/sous-articles", article("SA", true), parent)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID sous = UUID.fromString(JsonPath.read(json, "$.id"));

        modifier(sous, corps().avec("vocationId", FixturePatrimoine.VOCATION_VENTE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString("vocation")));
    }

    @Test
    void r9_7_champsAVider_surfaceInterieure_remiseAZero() throws Exception {
        UUID id = creer(article("VID", false).avec("surfaceInterieure", new BigDecimal("8")));

        modifier(id, corps().avec("champsAVider", List.of("surfaceInterieure"))).andExpect(status().isOk());

        assertThat(decimal(fixture.ligneArticle(id).get("InteriorArea"))).isEqualByComparingTo("0");
    }

    @Test
    void r5_4_4_articleOccupe_viderUneRemise_409() throws Exception {
        UUID id = creer(article("OCC", false));
        fiche.occuper(id, "RntFolderId", fiche.location("LOC-R-" + jeton));

        modifier(id, corps().avec("champsAVider", List.of("remiseMax1"))).andExpect(status().isConflict());
    }

    @Test
    void r11_6_articleLoue_409_nommeLeCodeDuDossierDeLocation() throws Exception {
        UUID id = creer(article("LOUE", false));
        fiche.occuper(id, "RntFolderId", fiche.location("LOC-" + jeton));

        modifier(id, corps().avec("surfaceDefinitive", new BigDecimal("12")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(DETAIL).value(containsString("LOC-" + jeton)));
    }
}

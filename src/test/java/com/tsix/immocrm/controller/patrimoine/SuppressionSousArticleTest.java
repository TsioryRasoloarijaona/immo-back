package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureArticleFiche;
import com.tsix.immocrm.fixtures.FixtureEntites;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureSousArticles;
import com.tsix.immocrm.fixtures.FixtureSousArticles.Contexte;
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
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.VOCATION_VENTE;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Suppression d'un sous-article — FEATURE-patrimoine-sous-articles.md §3, recalage R2 (refus de
 * {@code BuildItem.cs:DeleteUnderItem}) et R4-5 : refus « affecté par défaut » sur {@code DELETE /api/sous-articles/{id}}
 * et {@code DELETE /api/articles/{id}} d'un sous-article, refus dossier de vente / location, FK non mappée → 409,
 * sinon 204 et recalcul du parent ; la suppression d'un article emporte toujours ses sous-articles affectés.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SuppressionSousArticleTest {

    private static final String SOUS_ARTICLE = "/api/sous-articles/{id}";
    private static final String ARTICLE = "/api/articles/{id}";
    private static final String EXISTE = "SELECT COUNT(*) FROM StkItem WHERE PKey = :id";
    private static final String MESSAGE_AFFECTE_PAR_DEFAUT =
            "Vous ne pouvez pas supprimer un sous article affecté par défaut à l'article en cours !";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureSousArticles sousArticles;
    @Autowired private FixtureArticleFiche fiche;
    @Autowired private FixtureEntites entites;

    private AdmUser admin;
    private Contexte c;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        c = sousArticles.contexte("P4S");
    }

    private String code(String suffixe) {
        return suffixe + "-" + c.jeton();
    }

    private UUID creerParent(String code) throws Exception {
        String json = mockMvc.perform(post("/api/articles").with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec("trancheId", c.tranche().getId()).avec("code", code)
                                .avec("description", "Parent " + code).avec("vocationId", VOCATION_VENTE)
                                .avec("categorieId", c.categorie().getId()).avec("typeId", c.type().getId())
                                .avec("immeubleId", c.immeuble().getId()).avec("etageId", c.etage().getId())
                                .avec("surfaceProvisoire", new BigDecimal("63"))
                                .avec("surfaceDefinitive", new BigDecimal("63"))
                                .avec("prixM2ProvisoireTTC", new BigDecimal("13000"))
                                .avec("prixM2DefinitifTTC", new BigDecimal("13000"))
                                .avec("tauxTva", new BigDecimal("0.20")).json()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    private UUID creerRattache(UUID parentId, String code, boolean toujoursAffecte) throws Exception {
        String json = mockMvc.perform(post("/api/articles/{id}/sous-articles", parentId).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec("code", code).avec("description", "Parking " + code)
                                .avec("categorieId", c.categorieSous().getId()).avec("typeId", c.typeSous().getId())
                                .avec("etageId", c.etage().getId())
                                .avec("surfaceProvisoire", BigDecimal.TEN).avec("surfaceDefinitive", BigDecimal.TEN)
                                .avec("prixM2ProvisoireHT", new BigDecimal("5000"))
                                .avec("prixM2DefinitifHT", new BigDecimal("5000"))
                                .avec("tauxTva", new BigDecimal("0.20"))
                                .avec("toujoursAffecte", toujoursAffecte).json()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    private ResultActions supprimer(String url, UUID id) throws Exception {
        return mockMvc.perform(delete(url, id).with(connecte(admin)));
    }

    private int existe(UUID id) {
        return fiche.compter(EXISTE, id);
    }

    // ------------------------------------------------------------------ refus (1) « affecté par défaut »

    @Test
    void r2_refus1_affecteParDefaut_surSousArticles_409_messageOmran() throws Exception {
        UUID parentId = creerParent(code("PAR1"));
        UUID sous = creerRattache(parentId, code("TA1"), true);

        supprimer(SOUS_ARTICLE, sous)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString(MESSAGE_AFFECTE_PAR_DEFAUT)));

        assertThat(existe(sous)).isEqualTo(1);
    }

    /** R4-5 : une seule règle, dans SuppressionArticle, donc aussi sur DELETE /api/articles/{id} d'un sous-article. */
    @Test
    void r4_5_refus1_affecteParDefaut_surArticles_409_messageOmran() throws Exception {
        UUID parentId = creerParent(code("PAR2"));
        UUID sous = creerRattache(parentId, code("TA2"), true);

        supprimer(ARTICLE, sous)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString(MESSAGE_AFFECTE_PAR_DEFAUT)));

        assertThat(existe(sous)).isEqualTo(1);
    }

    /** R4-5 : la suppression d'un article emporte toujours ses sous-articles toujours affectés. */
    @Test
    void r4_5_suppressionDuParent_emporteSesSousArticlesToujoursAffectes() throws Exception {
        UUID parentId = creerParent(code("PAR3"));
        UUID sous = creerRattache(parentId, code("TA3"), true);

        supprimer(ARTICLE, parentId).andExpect(status().isNoContent());

        assertAll(
                () -> assertThat(existe(parentId)).isZero(),
                () -> assertThat(existe(sous)).isZero());
    }

    // ------------------------------------------------------------------ refus (2) et (3) : dossier du sous-article

    @Test
    void r2_refus2_dossierDeVente_409() throws Exception {
        StkItem sous = sousArticles.sousArticle(c, code("VEN"), null);
        sous.setComFolderId(fixture.dossier("DOS-" + code("VEN")).getId());
        sousArticles.enregistrer(sous);

        supprimer(SOUS_ARTICLE, sous.getId()).andExpect(status().isConflict());

        assertThat(existe(sous.getId())).isEqualTo(1);
    }

    @Test
    void r2_refus3_dossierDeLocation_409() throws Exception {
        StkItem sous = sousArticles.sousArticle(c, code("LOC"), null);
        fiche.occuper(sous.getId(), "RntFolderId", fiche.location("LOC-" + code("LOC")));

        supprimer(SOUS_ARTICLE, sous.getId()).andExpect(status().isConflict());

        assertThat(existe(sous.getId())).isEqualTo(1);
    }

    @Test
    void r2_fkNonMappee_prospection_409_nommeLeBloquant() throws Exception {
        StkItem sous = sousArticles.sousArticle(c, code("PRO"), null);
        fiche.prospection("PROS-" + code("PRO"), sous.getId());

        supprimer(SOUS_ARTICLE, sous.getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(matchesPattern("(?is).*(ComProspection|prospection).*")));

        assertThat(existe(sous.getId())).isEqualTo(1);
    }

    // ------------------------------------------------------------------ 204 et recalcul du parent

    @Test
    void r3_204_rattacheNonAffecteParDefaut_supprime_parentRecalcule() throws Exception {
        UUID parentId = creerParent(code("PAR4"));
        UUID sous = creerRattache(parentId, code("SR4"), false);
        assertThat(decimal(fixture.ligneArticle(parentId).get("ComSalePrice"))).isEqualByComparingTo("879000");

        supprimer(SOUS_ARTICLE, sous).andExpect(status().isNoContent());

        assertAll(
                () -> assertThat(existe(sous)).isZero(),
                () -> assertThat(decimal(fixture.ligneArticle(parentId).get("ComSalePrice")))
                        .isEqualByComparingTo("819000"));
    }

    @Test
    void r3_204_libre_supprime() throws Exception {
        StkItem sous = sousArticles.sousArticle(c, code("LIB"), null);

        supprimer(SOUS_ARTICLE, sous.getId()).andExpect(status().isNoContent());

        assertThat(existe(sous.getId())).isZero();
    }

    // ------------------------------------------------------------------ 404

    @Test
    void r3_404_sousArticleInconnu() throws Exception {
        supprimer(SOUS_ARTICLE, UUID.randomUUID()).andExpect(status().isNotFound());
    }

    @Test
    void visibiliteR4_entiteInactive_404_ligneGardee() throws Exception {
        StkItem sous = sousArticles.sousArticle(c, code("R4"), null);
        entites.actif(c.entite().getId(), false);

        supprimer(SOUS_ARTICLE, sous.getId()).andExpect(status().isNotFound());

        assertThat(existe(sous.getId())).isEqualTo(1);
    }
}

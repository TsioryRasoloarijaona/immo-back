package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureNomenclatureAchat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.ARTICLE;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.ARTICLES;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.CATEGORIE;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.CODE;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.DROIT_ARTICLES;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.DROIT_ARTICLES_NOUVEAU;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.DROIT_ARTICLES_SUPPRIMER;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.SOUS_FAMILLE;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.jeton;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * D6 (FEATURE-n8-nomenclature.md) : écran Articles gardé par {@code Yes.Buy.Front.Config.BuyItemList} (lecture),
 * {@code .toolNew} (création) et {@code .toolDelete} (suppression), comptes non administrateurs. Chaque 403 a son témoin 2xx.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ArticlesAchatSecuriteTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureNomenclatureAchat fixture;

    private UUID categorie;
    private UUID sousFamille;
    private UUID articleId;

    @BeforeEach
    void setUp() {
        fixture.preparer();
        categorie = fixture.categorie(jeton("CAT"));
        sousFamille = fixture.sousFamille(jeton("SF"), fixture.famille(jeton("FAM"), categorie), categorie);
        articleId = fixture.article(jeton("ART"), "Article garde", "U", sousFamille, categorie);
    }

    private ResultActions creer(RequestPostProcessor qui) throws Exception {
        CorpsJson corps = corps().avec(CODE, jeton("ART")).avec(DESCRIPTION, "Article").avec(CATEGORIE, categorie)
                .avec(SOUS_FAMILLE, sousFamille);
        return mockMvc.perform(post(ARTICLES).with(qui).contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    @Test
    void d6_lecture_sans_droit_403_avec_droit_200() throws Exception {
        mockMvc.perform(get(ARTICLES).with(fixture.avecDroits())).andExpect(status().isForbidden());
        mockMvc.perform(get(ARTICLES).with(fixture.avecDroits(DROIT_ARTICLES))).andExpect(status().isOk());
    }

    @Test
    void d6_creation_sans_toolNew_403_avec_toolNew_201() throws Exception {
        creer(fixture.avecDroits(DROIT_ARTICLES)).andExpect(status().isForbidden());
        creer(fixture.avecDroits(DROIT_ARTICLES, DROIT_ARTICLES_NOUVEAU)).andExpect(status().isCreated());
    }

    @Test
    void d6_suppression_sans_toolDelete_403_avec_toolDelete_204() throws Exception {
        mockMvc.perform(delete(ARTICLE, articleId).with(fixture.avecDroits(DROIT_ARTICLES, DROIT_ARTICLES_NOUVEAU)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(ARTICLE, articleId).with(fixture.avecDroits(DROIT_ARTICLES, DROIT_ARTICLES_SUPPRIMER)))
                .andExpect(status().isNoContent());
    }
}

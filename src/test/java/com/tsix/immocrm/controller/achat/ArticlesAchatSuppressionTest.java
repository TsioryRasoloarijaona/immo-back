package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.fixtures.FixtureNomenclatureAchat;
import com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.TableCitante;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.ARTICLE;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.jeton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * D3 / R6 (FEATURE-n8-nomenclature.md) : un article cité par une ligne de pièce, de décompte ou de lot n'est pas supprimé,
 * même là où la base ferait SET_NULL ({@code PrjMarketLine}, {@code PrjMarketLineEstimation}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ArticlesAchatSuppressionTest {

    private static final String DETAIL = "$.detail";
    private static final String COMPTE_ARTICLE = "SELECT COUNT(*) FROM BuyItem WHERE PKey = :id";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureNomenclatureAchat fixture;

    private RequestPostProcessor admin;
    private UUID articleId;

    @BeforeEach
    void setUp() {
        fixture.preparer();
        admin = fixture.administrateur();
        UUID categorie = fixture.categorie(jeton("CAT"));
        UUID sousFamille = fixture.sousFamille(jeton("SF"), fixture.famille(jeton("FAM"), categorie), categorie);
        articleId = fixture.article(jeton("ART"), "Article supprimable", "U", sousFamille, categorie);
    }

    @ParameterizedTest(name = "article cité par {0} : 409")
    @EnumSource(TableCitante.class)
    void d3_article_cite_409_et_conserve(TableCitante citante) throws Exception {
        fixture.citer(citante, articleId);

        mockMvc.perform(delete(ARTICLE, articleId).with(admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(DETAIL).value(startsWith("Suppression impossible : ")))
                .andExpect(jsonPath(DETAIL).value(containsString("est utilisé par 1 ")));
        assertThat(fixture.compter(COMPTE_ARTICLE, articleId)).isEqualTo(1);
    }

    @Test
    void d3_article_non_cite_supprime_204() throws Exception {
        mockMvc.perform(delete(ARTICLE, articleId).with(admin)).andExpect(status().isNoContent());

        assertThat(fixture.compter(COMPTE_ARTICLE, articleId)).isZero();
    }
}

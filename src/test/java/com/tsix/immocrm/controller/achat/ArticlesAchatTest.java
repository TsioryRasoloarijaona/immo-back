package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureNomenclatureAchat;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.ARTICLES;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.CATEGORIE;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.CODE;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.DECIMAUX;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.FAMILLE;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.NOTES;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.SOUS_FAMILLE;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.UNITE;
import static com.tsix.immocrm.fixtures.FixtureNomenclatureAchat.jeton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Écran Articles d'achat {@code /api/achat/config/articles} (FEATURE-n8-nomenclature.md R1, R2, R3, R4, R7).
 * Les lignes de liste sont lues par parcours profond ({@code $..}) : un tableau ou une page conviennent.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ArticlesAchatTest {

    private static final String DETAIL = "$.detail";
    private static final String R3_MESSAGE = "La sous famille sélectionnée n'appartient pas à la famille sélectionnée !";
    private static final String SEARCH = "search";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureNomenclatureAchat fixture;

    private RequestPostProcessor admin;
    private UUID categorie;
    private UUID familleId;
    private UUID sousFamilleId;

    @BeforeEach
    void setUp() {
        fixture.preparer();
        admin = fixture.administrateur();
        categorie = fixture.categorie(jeton("CAT"));
        familleId = fixture.famille(jeton("FAM"), categorie);
        sousFamilleId = fixture.sousFamille(jeton("SF"), familleId, categorie);
    }

    // ------------------------------------------------------------------ outils

    private CorpsJson article(String code) {
        return corps().avec(CODE, code).avec(DESCRIPTION, "Article " + code).avec(CATEGORIE, categorie)
                .avec(SOUS_FAMILLE, sousFamilleId);
    }

    private ResultActions poster(CorpsJson corps) throws Exception {
        return mockMvc.perform(post(ARTICLES).with(admin).contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private static String json(ResultActions reponse) throws Exception {
        return reponse.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    /** Codes d'articles de la liste commençant par {@code prefixe}, dans l'ordre reçu. */
    private List<String> codesListes(String prefixe, String search) throws Exception {
        var requete = get(ARTICLES).with(admin);
        if (search != null) {
            requete = requete.param(SEARCH, search);
        }
        List<String> codes = JsonPath.read(json(mockMvc.perform(requete).andExpect(status().isOk())), "$..code");
        return codes.stream().filter(c -> c.startsWith(prefixe)).toList();
    }

    // ------------------------------------------------------------------ R1

    @Test
    void r1_article_cree_avec_ses_champs() throws Exception {
        String code = jeton("ART");

        poster(article(code).avec(UNITE, "M3").avec(NOTES, "Note " + code))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.description").value("Article " + code))
                .andExpect(jsonPath("$." + CATEGORIE).value(categorie.toString()))
                .andExpect(jsonPath("$." + SOUS_FAMILLE).value(sousFamilleId.toString()))
                .andExpect(jsonPath("$." + UNITE).value("M3"))
                .andExpect(jsonPath("$." + NOTES).value("Note " + code));
    }

    @Test
    void r1_article_codeEnDouble_409() throws Exception {
        String code = jeton("ART");
        fixture.article(code, "Existant", "U", sousFamilleId, categorie);

        poster(article(code))
                .andExpect(status().isConflict())
                .andExpect(jsonPath(DETAIL).value(containsString("Ce code existe déjà.")));
    }

    // ------------------------------------------------------------------ R2

    @ParameterizedTest(name = "article sans {0}")
    @ValueSource(strings = {CODE, DESCRIPTION, CATEGORIE, SOUS_FAMILLE})
    void r2_article_champObligatoireManquant_400(String champ) throws Exception {
        poster(article(jeton("ART")).sans(champ)).andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ R3 famille dérivée

    @Test
    void r3_famille_derivee_de_la_sousFamille() throws Exception {
        poster(article(jeton("ART")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$." + FAMILLE).value(familleId.toString()));
    }

    @Test
    void r3_famille_coherente_acceptee() throws Exception {
        poster(article(jeton("ART")).avec(FAMILLE, familleId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$." + FAMILLE).value(familleId.toString()));
    }

    @Test
    void r3_famille_differente_de_celle_de_la_sousFamille_400() throws Exception {
        UUID autreFamille = fixture.famille(jeton("FAM"), categorie);

        poster(article(jeton("ART")).avec(FAMILLE, autreFamille))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString(R3_MESSAGE)));
    }

    // ------------------------------------------------------------------ R4 décimaux

    @Test
    void r4_decimaux_absents_valent_zero_et_aucun_ttc() throws Exception {
        String json = json(poster(article(jeton("ART"))).andExpect(status().isCreated()));
        Map<String, Object> article = JsonPath.read(json, "$");

        assertThat(article).containsKeys(DECIMAUX.toArray(String[]::new));
        assertThat(DECIMAUX.stream().map(champ -> new BigDecimal(String.valueOf(article.get(champ)))).toList())
                .isNotEmpty().allSatisfy(v -> assertThat(v).isZero());
        assertThat(article.keySet()).isNotEmpty().noneMatch(cle -> cle.toLowerCase(Locale.ROOT).contains("ttc"));
    }

    @Test
    void r4_decimaux_saisis_relus_tels_quels() throws Exception {
        CorpsJson corps = article(jeton("ART"));
        for (int i = 0; i < DECIMAUX.size(); i++) {
            corps.avec(DECIMAUX.get(i), new BigDecimal("1" + i + ".25"));
        }

        Map<String, Object> article = JsonPath.read(json(poster(corps).andExpect(status().isCreated())), "$");

        Function<String, BigDecimal> lu = champ -> new BigDecimal(String.valueOf(article.get(champ)));
        for (int i = 0; i < DECIMAUX.size(); i++) {
            assertThat(lu.apply(DECIMAUX.get(i))).as(DECIMAUX.get(i)).isEqualByComparingTo("1" + i + ".25");
        }
    }

    // ------------------------------------------------------------------ R7 liste

    @Test
    void r7_liste_triee_par_code_par_defaut() throws Exception {
        String prefixe = jeton("TRI");
        fixture.article(prefixe + "C", "Gamma", "U", sousFamilleId, categorie);
        fixture.article(prefixe + "A", "Alpha", "U", sousFamilleId, categorie);
        fixture.article(prefixe + "B", "Beta", "U", sousFamilleId, categorie);

        assertThat(codesListes(prefixe, null)).containsExactly(prefixe + "A", prefixe + "B", prefixe + "C");
    }

    @ParameterizedTest(name = "recherche par {0}")
    @ValueSource(strings = {"code", "description", "unite", "codeFamille", "codeSousFamille"})
    void r7_recherche_multicritere(String critere) throws Exception {
        String prefixe = jeton("RCH");
        String familleCode = jeton("FMR");
        String sousFamilleCode = jeton("SFR");
        UUID autreFamille = fixture.famille(familleCode, categorie);
        UUID autreSousFamille = fixture.sousFamille(sousFamilleCode, autreFamille, categorie);
        String description = "Desc" + jeton("");
        String unite = "Un" + jeton("");
        String cherche = prefixe + "1";
        fixture.article(cherche, description, unite, autreSousFamille, categorie);
        fixture.article(prefixe + "2", "Autre", "U", sousFamilleId, categorie);
        String terme = switch (critere) {
            case "code" -> cherche;
            case "description" -> description;
            case "unite" -> unite;
            case "codeFamille" -> familleCode;
            default -> sousFamilleCode;
        };

        assertThat(codesListes(prefixe, terme)).containsExactly(cherche);
    }
}

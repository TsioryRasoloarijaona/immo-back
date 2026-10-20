package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.dto.patrimoine.ArticleListItemDto;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureSousArticles;
import com.tsix.immocrm.fixtures.FixtureSousArticles.Contexte;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Catalogue {@code ColonneArticle} et projection {@code LigneArticleDto} — FEATURE-patrimoine-sous-articles.md §1,
 * recalage R1, R3, R5, réponses du manager (20/10) n° 1 et 2 : {@code GET /api/referentiels/colonnes/articles}, 87
 * colonnes de l'aide + 8, défauts = champs d'{@code ArticleListItemDto}, « Avance » = {@code ComSaleMinAdvance},
 * colonnes de vue calculées, 400 {@code colonnesValides}, pour {@code /api/articles} et {@code /api/sous-articles}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ColonnesArticleTest {

    private static final String CATALOGUE = "/api/referentiels/colonnes/articles";
    private static final String ARTICLES = "/api/articles";
    private static final String SOUS_ARTICLES = "/api/sous-articles";
    private static final String COLONNES = "colonnes";
    private static final String TRANCHE_ID = "trancheId";
    private static final String SIZE = "size";
    private static final String TOUS = "1000";
    private static final String PARENT_CODE = "parentCode";
    private static final String COLONNE_INCONNUE = "pasUneColonne";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureSousArticles sousArticles;

    private AdmUser admin;
    private Contexte c;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        c = sousArticles.contexte("P4C");
    }

    private String catalogue() throws Exception {
        return mockMvc.perform(get(CATALOGUE).with(connecte(admin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /** Nom technique de la colonne de ce libellé (libellés du tableau Liste de l'aide ARTICLES). */
    private String nomDuLibelle(String libelle) throws Exception {
        List<String> noms = JsonPath.read(catalogue(), "$[?(@.libelle == '" + libelle + "')].nom");
        assertThat(noms).as("colonne « %s » du catalogue", libelle).hasSize(1);
        return noms.get(0);
    }

    private String valeurDansLaListe(String url, StkItem item, String colonne) throws Exception {
        String json = mockMvc.perform(get(url).param(TRANCHE_ID, c.tranche().getId().toString())
                        .param(COLONNES, "code," + colonne).param(SIZE, TOUS).with(connecte(admin)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Object> valeurs = JsonPath.read(json, "$.content[?(@.id == '" + item.getId() + "')]." + colonne);
        assertThat(valeurs).as("%s de %s", colonne, item.getCode()).hasSize(1);
        return String.valueOf(valeurs.get(0));
    }

    private static String ligne(StkItem item) {
        return "$.content[?(@.id=='" + item.getId() + "')]";
    }

    // ------------------------------------------------------------------ catalogue

    @Test
    void reponse2_catalogue_87ColonnesDeLAidePlus8() throws Exception {
        mockMvc.perform(get(CATALOGUE).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(95)))
                .andExpect(jsonPath("$[?(@.nom == '" + PARENT_CODE + "')]").isNotEmpty());
    }

    @Test
    void r3_catalogue_nomsUniques() throws Exception {
        List<String> noms = JsonPath.read(catalogue(), "$[*].nom");

        assertThat(noms).isNotEmpty().doesNotHaveDuplicates();
    }

    /** R3 : défauts = exactement les champs servis aujourd'hui par ArticleListItemDto (id compris). */
    @Test
    void r3_defauts_egauxAuxChampsDArticleListItemDto() throws Exception {
        List<String> defauts = JsonPath.read(catalogue(), "$[?(@.parDefaut == true)].nom");
        List<String> attendus = Arrays.stream(ArticleListItemDto.class.getRecordComponents())
                .map(RecordComponent::getName).toList();

        assertThat(defauts).containsExactlyInAnyOrderElementsOf(attendus);
    }

    @Test
    void reponse1_avance_colonneNonParDefaut_valeurComSaleMinAdvance() throws Exception {
        StkItem article = sousArticles.principal(c, "AV-" + c.jeton(), c.tranche());
        article.setComSaleMinAdvance(new BigDecimal("12345"));
        sousArticles.enregistrer(article);
        String avance = nomDuLibelle("Avance");

        String valeur = valeurDansLaListe(ARTICLES, article, avance);

        assertThat(new BigDecimal(valeur)).isEqualByComparingTo("12345");
        mockMvc.perform(get(CATALOGUE).with(connecte(admin)))
                .andExpect(jsonPath("$[?(@.nom == '" + avance + "')].parDefaut").value(false));
    }

    // ------------------------------------------------------------------ colonnes de vue calculées (jamais lues en table)

    @Test
    void r3_prixSousArticleTTC_sommeDesSalePriceDesEnfants() throws Exception {
        StkItem parent = sousArticles.principal(c, "PSA-" + c.jeton(), c.tranche());
        sousArticles.sousArticle(c, "PSA1-" + c.jeton(), parent);
        StkItem second = sousArticles.sousArticle(c, "PSA2-" + c.jeton(), parent);
        second.setSalePrice(new BigDecimal("30000"));
        sousArticles.enregistrer(second);

        String valeur = valeurDansLaListe(ARTICLES, parent, nomDuLibelle("Prix Sous Article TTC"));

        assertThat(new BigDecimal(valeur)).isEqualByComparingTo("90000");
    }

    @Test
    void r3_loyerMensuelTTC_amountHTFoisUnPlusTva() throws Exception {
        StkItem article = sousArticles.principal(c, "LOY-" + c.jeton(), c.tranche());
        article.setAmountHT(new BigDecimal("1000"));
        sousArticles.enregistrer(article);

        String valeur = valeurDansLaListe(ARTICLES, article, nomDuLibelle("Loyer mensuel TTC"));

        assertThat(new BigDecimal(valeur)).isEqualByComparingTo("1200");
    }

    @Test
    void r3_etat_libreSansDossier() throws Exception {
        StkItem article = sousArticles.principal(c, "ETA-" + c.jeton(), c.tranche());

        assertThat(valeurDansLaListe(ARTICLES, article, nomDuLibelle("État"))).isEqualTo("Libre");
    }

    // ------------------------------------------------------------------ projection /api/articles

    @Test
    void r5_articles_projection_seulesLesColonnesDemandeesPlusId() throws Exception {
        StkItem article = sousArticles.principal(c, "PROJ-" + c.jeton(), c.tranche());

        mockMvc.perform(get(ARTICLES).param(TRANCHE_ID, c.tranche().getId().toString()).param(COLONNES, "code")
                        .param(SIZE, TOUS).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(article) + ".code").value(article.getCode()))
                .andExpect(jsonPath("$.content[0].id").exists())
                .andExpect(jsonPath("$.content[0].description").doesNotExist())
                .andExpect(jsonPath("$.content[0].prixSuggere").doesNotExist());
    }

    @Test
    void r5_articles_sansColonnes_colonnesParDefautSeules() throws Exception {
        StkItem article = sousArticles.principal(c, "DEF-" + c.jeton(), c.tranche());

        mockMvc.perform(get(ARTICLES).param(TRANCHE_ID, c.tranche().getId().toString()).param(SIZE, TOUS)
                        .with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne(article) + ".description").value(article.getDescription()))
                .andExpect(jsonPath("$.content[0].prixSuggereEstPlancher").exists())
                .andExpect(jsonPath("$.content[0]." + PARENT_CODE).doesNotExist());
    }

    @Test
    void r1_articles_colonneInconnue_400_nommeLaColonneEtLesNomsValides() throws Exception {
        mockMvc.perform(get(ARTICLES).param(COLONNES, COLONNE_INCONNUE).with(connecte(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString(COLONNE_INCONNUE)))
                .andExpect(content().string(containsString(PARENT_CODE)));
    }

    // ------------------------------------------------------------------ même catalogue pour /api/sous-articles

    @Test
    void r1_sousArticles_colonneParentCode_codeDuParent() throws Exception {
        StkItem parent = sousArticles.principal(c, "PCODE-" + c.jeton(), c.tranche());
        StkItem enfant = sousArticles.sousArticle(c, "ECODE-" + c.jeton(), parent);

        assertThat(valeurDansLaListe(SOUS_ARTICLES, enfant, PARENT_CODE)).isEqualTo(parent.getCode());
    }

    @Test
    void r1_sousArticles_colonneInconnue_400() throws Exception {
        mockMvc.perform(get(SOUS_ARTICLES).param(COLONNES, COLONNE_INCONNUE).with(connecte(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString(COLONNE_INCONNUE)))
                .andExpect(content().string(containsString(PARENT_CODE)));
    }
}

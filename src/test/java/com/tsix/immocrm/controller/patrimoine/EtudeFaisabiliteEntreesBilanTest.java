package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.fixtures.ApiFaisabilite;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureFaisabilite;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ApiFaisabilite.d;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.*;
import static com.tsix.immocrm.fixtures.FixtureFaisabiliteFinance.*;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Entrées du bilan ajoutées à la fiche, nombre d'articles calculé, champ retiré — FEATURE-patrimoine-faisabilite-workflow.md
 * §0 point 7, §1.3, §3 et §10 (impact front).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EtudeFaisabiliteEntreesBilanTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureFaisabilite fixture;

    private ApiFaisabilite api;
    private CfgCompany societe;
    private String jeton;

    @BeforeEach
    void setUp() {
        fixture.parametrage();
        api = new ApiFaisabilite(mockMvc, patrimoine.administrateur());
        jeton = jeton();
        societe = patrimoine.entite("ENT-" + jeton, true);
    }

    private CorpsJson fiche() {
        return FixtureFaisabilite.fiche("ENB-" + jeton, societe.getId());
    }

    // ------------------------------------------------------------------ §1.3 trois entrées

    @Test
    void r1_3_patch_troisEntrees_stockeesEtRelues() throws Exception {
        UUID id = api.etude(fiche());

        String json = api.modifier(id, corps().avec(POURCENTAGE_CONCRETISATION, d("0.3")).avec(TAUX_IS, d("0.25"))
                        .avec(PRIX_VENTE_M2, d("12000")).json())
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        Tuple e = fixture.etude(id);
        assertAll(
                () -> assertThat(lu(json, POURCENTAGE_CONCRETISATION)).isEqualByComparingTo("0.3"),
                () -> assertThat(lu(json, TAUX_IS)).isEqualByComparingTo("0.25"),
                () -> assertThat(lu(json, PRIX_VENTE_M2)).isEqualByComparingTo("12000"),
                () -> assertThat(decimal(e.get("percentageConcretisation"))).isEqualByComparingTo("0.3"),
                () -> assertThat(decimal(e.get("ISRatio"))).isEqualByComparingTo("0.25"),
                () -> assertThat(decimal(e.get("UnitSalePrice"))).isEqualByComparingTo("12000"));
    }

    private static BigDecimal lu(String json, String champ) {
        return new BigDecimal(String.valueOf((Object) JsonPath.read(json, "$." + champ)));
    }

    @Test
    void r1_3_creation_entreesParDefautA0() throws Exception {
        UUID id = api.etude(fiche());

        Tuple e = fixture.etude(id);
        assertAll(
                () -> assertThat(decimal(e.get("percentageConcretisation"))).isEqualByComparingTo("0"),
                () -> assertThat(decimal(e.get("ISRatio"))).isEqualByComparingTo("0"),
                () -> assertThat(decimal(e.get("UnitSalePrice"))).isEqualByComparingTo("0"));
    }

    @ParameterizedTest(name = "{0} = {1}")
    @CsvSource({"pourcentageConcretisation, 1.5", "pourcentageConcretisation, -0.1", "tauxIs, 2", "tauxIs, -0.5"})
    void r1_3_fractionHorsDe0a1_400(String champ, String valeur) throws Exception {
        UUID id = api.etude(fiche());

        api.modifier(id, corps().avec(champ, d(valeur)).json()).andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {ECART_TYPE_TRAVAUX, ECART_TYPE_CONCRETISATION, ECART_TYPE_FINALISATION})
    void r1_3_ecartTypeInferieurA0_5_400(String champ) throws Exception {
        UUID id = api.etude(fiche());

        api.modifier(id, corps().avec(champ, d("0.4")).json()).andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {ECART_TYPE_TRAVAUX, ECART_TYPE_CONCRETISATION, ECART_TYPE_FINALISATION})
    void r1_3_ecartTypeEgalA0_5_accepte(String champ) throws Exception {
        UUID id = api.etude(fiche());

        api.modifier(id, corps().avec(champ, d("0.5")).json()).andExpect(status().isOk());
    }

    @Test
    void r1_3_calcul_ecartTypeInferieurA0_5_400() throws Exception {
        api.calculer(fiche().avec(ECART_TYPE_TRAVAUX, d("0.4"))).andExpect(status().isBadRequest());
    }

    @Test
    void r1_3_calcul_accepteLesTroisEntrees() throws Exception {
        api.calculer(fiche().avec(POURCENTAGE_CONCRETISATION, d("0.3")).avec(TAUX_IS, d("0.25"))
                        .avec(PRIX_VENTE_M2, d("12000")))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ §0 point 7 : nombre d'articles calculé

    /**
     * Σ quantités des unités ayant une catégorie d'article ({@code ForUnderItem} faux) et non surface : 10 ;
     * l'unité surface (50), l'unité sans catégorie (7) et l'unité de sous-article (3) ne comptent pas.
     */
    @Test
    void r0_7_nombreArticles_calculeDepuisLesUnites() throws Exception {
        UUID id = api.etude(fiche());
        UUID article = patrimoine.categorie("CA-" + jeton, patrimoine.natureBat(), false).getId();
        UUID sousArticle = patrimoine.categorie("CS-" + jeton, patrimoine.natureBat(), true).getId();
        UUID compte = api.uniteArticle("UA-" + jeton, false, article);
        UUID surface = api.uniteArticle("US-" + jeton, true, article);
        UUID sansCategorie = api.unite("UN-" + jeton, false, null);
        UUID sous = api.uniteArticle("UB-" + jeton, false, sousArticle);

        api.unites(id, lignes(ligneUniteCorps(1, compte, d("10")), ligneUniteCorps(2, surface, d("50")),
                ligneUniteCorps(3, sansCategorie, d("7")), ligneUniteCorps(4, sous, d("3")))).andExpect(status().isOk());

        String json = api.lire(FICHE, id).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertAll(
                () -> assertThat(decimal(fixture.etude(id).get("NumberOfItemForSale"))).isEqualByComparingTo("10"),
                () -> assertThat(lu(json, NOMBRE_ARTICLES)).isEqualByComparingTo("10"));
    }

    /** §10 : {@code nombreArticlesAVendre} reçu dans le corps est ignoré, sans 400. */
    @Test
    void r10_nombreArticlesRecu_ignore_sans400() throws Exception {
        UUID id = api.etude(fiche());

        api.modifier(id, corps().avec(NOMBRE_ARTICLES, 99).json()).andExpect(status().isOk());

        assertThat(decimal(fixture.etude(id).get("NumberOfItemForSale"))).isEqualByComparingTo("0");
    }

    /** §3 / §10 : {@code previsionsARecalculer} disparaît de la fiche. */
    @Test
    void r10_previsionsARecalculer_retire() throws Exception {
        UUID id = api.etude(fiche());
        fixture.prevision(id);

        api.lire(FICHE, id).andExpect(status().isOk()).andExpect(jsonPath("$." + PREVISIONS_A_RECALCULER).doesNotExist());
    }
}

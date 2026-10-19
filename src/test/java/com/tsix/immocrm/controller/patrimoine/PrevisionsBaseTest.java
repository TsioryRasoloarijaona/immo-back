package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureTrancheOnglets;
import com.tsix.immocrm.fixtures.FixtureTrancheRegles;
import com.tsix.immocrm.fixtures.LignesJson;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import jakarta.persistence.Tuple;
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
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prévisions de base — FEATURE-patrimoine-tranche-regles.md §3, §7 (PrevisionsBaseTest), §9.4 ({@code CfgPrevision}
 * liée par {@code StkHierarchyId}), §9.13 (stock recalculé sur les tables, définition de la vue
 * {@code StkTrancheValorisation} : {@code CountItem} = articles vocation Vente hors sous-articles,
 * {@code PriceAllItem} = Σ {@code SalePrice} des articles vocation Vente, sous-articles compris).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PrevisionsBaseTest {

    private static final String PREVISIONS = "/api/tranches/{id}/previsions-base";
    private static final String DETAIL = "$.detail";
    private static final String DEBUT_C = "2027-01-01T00:00:00";
    private static final String DE_LA_TRANCHE = "SELECT COUNT(*) FROM CfgPrevision WHERE StkHierarchyId = :id";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureTrancheOnglets onglets;
    @Autowired private FixtureTrancheRegles regles;
    @Autowired private CfgTrancheRepository trancheRepository;

    private AdmUser admin;
    private CfgTranche tranche;
    private String jeton;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        jeton = "P1CP" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        tranche = fixture.tranche("TR-" + jeton, fixture.projet("PRJ-" + jeton, fixture.entite("ENT-" + jeton, true)));
    }

    private static CorpsJson ligne(int annee, int mois, int concretisation, int finalisation) {
        return corps().avec("date", LocalDateTime.of(annee, mois, 1, 0, 0).toString() + ":00")
                .avec("annee", annee).avec("mois", mois)
                .avec("quantiteConcretisation", concretisation).avec("quantiteFinalisation", finalisation);
    }

    /** En-tête valide de 3 articles, suivi des lignes. */
    private static String previsions(CorpsJson... lignes) {
        String entete = corps().avec("nombreArticles", 3).avec("valorisationStock", new BigDecimal("3000000"))
                .avec("pourcentageConcretisation", new BigDecimal("0.4"))
                .avec("debutConcretisation", DEBUT_C).avec("finConcretisation", "2027-03-01T00:00:00")
                .avec("debutFinalisation", "2027-03-01T00:00:00").avec("finFinalisation", "2027-05-01T00:00:00")
                .avec("ecartTypeConcretisation", BigDecimal.ONE).avec("ecartTypeFinalisation", BigDecimal.ONE).json();
        String corpsLignes = LignesJson.lignes(lignes);
        return entete.substring(0, entete.length() - 1) + "," + corpsLignes.substring(1);
    }

    private ResultActions enregistrer(String corps) throws Exception {
        return mockMvc.perform(put(PREVISIONS, tranche.getId()).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    // ------------------------------------------------------------------ lecture

    @Test
    void r3_get_enTeteDeLaTrancheEtLignesCfgPrevision() throws Exception {
        tranche.setCountItemBasePrevision(12);
        tranche.setBaseMarketingStartDate(LocalDateTime.of(2027, 1, 1, 0, 0));
        trancheRepository.saveAndFlush(tranche);
        UUID ligne = regles.lignePrevision(tranche.getId(), 2027, 3, 12, 0);

        mockMvc.perform(get(PREVISIONS, tranche.getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreArticles").value(12))
                .andExpect(jsonPath("$.debutConcretisation").value(containsString("2027-01-01")))
                .andExpect(jsonPath("$.lignes[*].id", hasItem(ligne.toString())))
                .andExpect(jsonPath("$.lignes[0].annee").value(2027))
                .andExpect(jsonPath("$.lignes[0].mois").value(3))
                .andExpect(jsonPath("$.lignes[0].quantiteConcretisation").value(12));
    }

    /** §3 « Charger à partir du stock » : mêmes valeurs que la ligne de la vue pour cette tranche. */
    @Test
    void r3_stock_recalculeSurLesTables_commeLaVueStkTrancheValorisation() throws Exception {
        UUID a1 = onglets.article("A1-" + jeton, tranche.getId(), fixture.natureBat(), fixture.vocationVente());
        UUID a2 = onglets.article("A2-" + jeton, tranche.getId(), fixture.natureBat(), fixture.vocationVente());
        UUID sous = onglets.article("SA-" + jeton, tranche.getId(), fixture.natureBat(), fixture.vocationVente());
        UUID projet = onglets.article("PJ-" + jeton, tranche.getId(), fixture.natureBat(), fixture.vocationProjet());
        regles.prixArticle(a1, new BigDecimal("100"), false);
        regles.prixArticle(a2, new BigDecimal("200"), false);
        regles.prixArticle(sous, new BigDecimal("50"), true);
        regles.prixArticle(projet, new BigDecimal("1000"), false);

        mockMvc.perform(get(PREVISIONS + "/stock", tranche.getId()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreArticles").value(2))
                .andExpect(jsonPath("$.valorisationStock").value(350));
    }

    // ------------------------------------------------------------------ Générer (sans écriture)

    @Test
    void r3_generer_sansLesQuatreDates_400_messageOmran() throws Exception {
        mockMvc.perform(post(PREVISIONS + "/generer", tranche.getId()).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec("nombreArticles", 3).avec("debutConcretisation", DEBUT_C).json()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString("Toutes les dates doivent avoir une valeur !")));
    }

    @Test
    void r3_generer_nEcritAucuneLigne() throws Exception {
        mockMvc.perform(post(PREVISIONS + "/generer", tranche.getId()).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(previsions()))
                .andExpect(status().isOk());

        assertThat(regles.compter(DE_LA_TRANCHE, tranche.getId())).isZero();
    }

    // ------------------------------------------------------------------ PUT : Verif l.450-471

    @Test
    void r3_put_sommeDesConcretisationsDifferenteDuStock_400() throws Exception {
        enregistrer(previsions(ligne(2027, 1, 2, 0), ligne(2027, 2, 0, 3)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString(
                        "La somme des quantités de concrétisation prévisionnelles doit être égal au nombre d'article en stock !")));
    }

    @Test
    void r3_put_sommeDesFinalisationsDifferenteDuStock_400() throws Exception {
        enregistrer(previsions(ligne(2027, 1, 2, 0), ligne(2027, 2, 1, 2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString(
                        "La somme des quantités de finalisation prévisionnelles doit être égal au nombre d'article en stock !")));
    }

    @Test
    void r3_put_moisEnDouble_400() throws Exception {
        enregistrer(previsions(ligne(2027, 1, 2, 0), ligne(2027, 1, 1, 3)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString(
                        "Il ne faut pas que les prévisions contiennent des mois en double !")));
    }

    @Test
    void r3_put_valide_ecritLEnTeteEtRemplaceLesLignes() throws Exception {
        UUID ancienne = regles.lignePrevision(tranche.getId(), 2026, 5, 3, 3);

        enregistrer(previsions(ligne(2027, 1, 2, 0), ligne(2027, 2, 1, 3))).andExpect(status().isOk());

        Tuple enTete = fixture.ligneTranche(tranche.getId());
        assertAll(
                () -> assertThat(regles.compter("SELECT COUNT(*) FROM CfgPrevision WHERE PKey = :id", ancienne)).isZero(),
                () -> assertThat(regles.compter(DE_LA_TRANCHE, tranche.getId())).isEqualTo(2),
                () -> assertThat(regles.compter(DE_LA_TRANCHE + " AND [Year] = 2027 AND [Month] = 2 AND Quantity = 1 "
                        + "AND TurnoverQuantity = 3", tranche.getId())).isEqualTo(1),
                () -> assertThat(((Number) enTete.get("CountItemBasePrevision")).intValue()).isEqualTo(3),
                () -> assertThat(FixturePatrimoine.decimal(enTete.get("BaseConcretisationPercent"))).isEqualByComparingTo("0.4"),
                () -> assertThat(enTete.get("BaseMarketingEndDate").toString()).startsWith("2027-05-01"));
    }
}

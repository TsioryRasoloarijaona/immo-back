package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.CorpsJson;
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
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Répartition en cloche de « Générer » — FEATURE-patrimoine-tranche-regles.md §3, §7 (PrevisionsClocheTest),
 * formule et arrondi de {@code ProjectPrevision.cs:399-448} (surcharge {@code GetGaussGraph(…, Quantity)} : loi normale
 * normalisée sur les mois, {@code Math.Round}, écart reporté au mois du milieu, lignes à zéro retirées). Le spec ne
 * donne pas la signature de {@code domain/PrevisionsCloche} : testée par {@code POST …/previsions-base/generer}.
 * Valeurs attendues calculées à la main depuis le code OMRAN (aucun cas à demi-unité : l'arrondi bancaire de .NET
 * et HALF_UP y divergent).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PrevisionsClocheTest {

    private static final String GENERER = "/api/tranches/{id}/previsions-base/generer";
    private static final String CONCRETISATION = "$.lignes[*].quantiteConcretisation";
    private static final String FINALISATION = "$.lignes[*].quantiteFinalisation";
    private static final String MOIS = "$.lignes[*].mois";
    private static final String JANVIER = "2027-01-01T00:00:00";
    private static final String MAI = "2027-05-01T00:00:00";
    private static final String JUILLET = "2027-07-01T00:00:00";
    private static final String NOVEMBRE = "2027-11-01T00:00:00";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;

    private AdmUser admin;
    private CfgTranche tranche;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        String jeton = "P1CG" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        tranche = fixture.tranche("TR-" + jeton, fixture.projet("PRJ-" + jeton, fixture.entite("ENT-" + jeton, true)));
    }

    private static CorpsJson entete(int articles, String debutConcretisation, String finConcretisation,
                                    String debutFinalisation, String finFinalisation) {
        return corps().avec("nombreArticles", articles).avec("valorisationStock", new BigDecimal("1000000"))
                .avec("pourcentageConcretisation", new BigDecimal("0.5"))
                .avec("debutConcretisation", debutConcretisation).avec("finConcretisation", finConcretisation)
                .avec("debutFinalisation", debutFinalisation).avec("finFinalisation", finFinalisation)
                .avec("ecartTypeConcretisation", BigDecimal.ONE).avec("ecartTypeFinalisation", BigDecimal.ONE);
    }

    private ResultActions generer(CorpsJson entete) throws Exception {
        return mockMvc.perform(post(GENERER, tranche.getId()).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(entete.json()))
                .andExpect(status().isOk());
    }

    /** 5 mois, σ = 1 : poids normalisés 0,0545 / 0,2442 / 0,4026 / 0,2442 / 0,0545 × 10 → 1, 2, 4, 2, 1. */
    @Test
    void r3_cinqMois_dixArticles_ecartType1_cloche12421() throws Exception {
        generer(entete(10, JANVIER, MAI, JUILLET, NOVEMBRE))
                .andExpect(jsonPath(MOIS).value(contains(1, 2, 3, 4, 5, 7, 8, 9, 10, 11)))
                .andExpect(jsonPath(CONCRETISATION).value(contains(1, 2, 4, 2, 1, 0, 0, 0, 0, 0)))
                .andExpect(jsonPath(FINALISATION).value(contains(0, 0, 0, 0, 0, 1, 2, 4, 2, 1)));
    }

    /** × 6 → 0, 1, 2, 1, 0 (somme 4) : les 2 manquants vont au mois du milieu ; les mois à zéro sont retirés. */
    @Test
    void r3_sommeArrondieInferieure_ecartAuMoisDuMilieu_moisAZeroRetires() throws Exception {
        generer(entete(6, JANVIER, MAI, JUILLET, NOVEMBRE))
                .andExpect(jsonPath(MOIS).value(contains(2, 3, 4, 8, 9, 10)))
                .andExpect(jsonPath(CONCRETISATION).value(contains(1, 4, 1, 0, 0, 0)))
                .andExpect(jsonPath(FINALISATION).value(contains(0, 0, 0, 1, 4, 1)));
    }

    /** × 11 → 1, 3, 4, 3, 1 (somme 12) : un retiré au mois du milieu → 1, 3, 3, 3, 1. */
    @Test
    void r3_sommeArrondieSuperieure_unRetireAuMoisDuMilieu() throws Exception {
        generer(entete(11, JANVIER, MAI, JUILLET, NOVEMBRE))
                .andExpect(jsonPath(CONCRETISATION).value(contains(1, 3, 3, 3, 1, 0, 0, 0, 0, 0)));
    }

    /**
     * 3 mois : σ = 0,5 → 0,1065 / 0,7870 / 0,1065 × 10 → 1, 8, 1 ; σ = 1 → 0,2741 / 0,4519 / 0,2741 × 10 → 3, 5, 3
     * (somme 11) puis un retiré au mois du milieu → 3, 4, 3.
     */
    @Test
    void r3_troisMois_ecartType05_cloche181_ecartType1_cloche343() throws Exception {
        generer(entete(10, JANVIER, "2027-03-01T00:00:00", JUILLET, "2027-09-01T00:00:00")
                .avec("ecartTypeConcretisation", new BigDecimal("0.5")))
                .andExpect(jsonPath(CONCRETISATION).value(contains(1, 8, 1, 0, 0, 0)))
                .andExpect(jsonPath(FINALISATION).value(contains(0, 0, 0, 3, 4, 3)));
    }

    @Test
    void r3_unSeulMois_toutLeStockCeMoisLa() throws Exception {
        generer(entete(7, JANVIER, JANVIER, JUILLET, JUILLET))
                .andExpect(jsonPath(MOIS).value(contains(1, 7)))
                .andExpect(jsonPath(CONCRETISATION).value(contains(7, 0)))
                .andExpect(jsonPath(FINALISATION).value(contains(0, 7)));
    }

    /** Mois commun aux deux périodes : une seule ligne, notes OMRAN (« Concretisation » sans accent, sic). */
    @Test
    void r3_moisCommun_uneLigne_concretisationEtFinalisation() throws Exception {
        generer(entete(10, JANVIER, MAI, MAI, "2027-09-01T00:00:00"))
                .andExpect(jsonPath(MOIS).value(contains(1, 2, 3, 4, 5, 6, 7, 8, 9)))
                .andExpect(jsonPath(CONCRETISATION).value(contains(1, 2, 4, 2, 1, 0, 0, 0, 0)))
                .andExpect(jsonPath(FINALISATION).value(contains(0, 0, 0, 0, 1, 2, 4, 2, 1)))
                .andExpect(jsonPath("$.lignes[0].notes").value("Concretisation"))
                .andExpect(jsonPath("$.lignes[4].notes").value("Concrétisation et Finalisation"))
                .andExpect(jsonPath("$.lignes[8].notes").value("Finalisation"));
    }
}

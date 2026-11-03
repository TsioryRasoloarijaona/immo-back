package com.tsix.immocrm.controller.administration;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.FixtureOngletsTiers;
import com.tsix.immocrm.fixtures.FixtureTiers;
import com.tsix.immocrm.fixtures.FixtureTiers.Jeu;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureOngletsTiers.DEPENSES;
import static com.tsix.immocrm.fixtures.FixtureOngletsTiers.ids;
import static com.tsix.immocrm.fixtures.FixtureOngletsTiers.onglet;
import static com.tsix.immocrm.fixtures.FixtureOngletsTiers.total;
import static com.tsix.immocrm.fixtures.FixtureTiers.TypeTiers.FOURNISSEURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pagination serveur — FEATURE-admin-onglets-tiers.md, règles testables 2 et 7, bornes « comme {@code TiersController.lister} »
 * (page 0 à 1 000 000, size 1 à 200). Jeu : 51 dépenses d'un fournisseur.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OngletsTiersPaginationTest {

    private static final int LIGNES = 51;
    private static final String PAGE = "page";
    private static final String SIZE = "size";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureTiers tiers;
    @Autowired private FixtureOngletsTiers fixture;
    @Autowired private EntityManager em;

    private Jeu j;
    private UUID fournisseur;

    @BeforeEach
    void setUp() {
        j = tiers.jeu();
        fixture.preparer();
        fournisseur = j.tiers(FOURNISSEURS, "F").inserer();
        LocalDate depart = LocalDate.of(2024, 1, 1);
        for (int k = 0; k < LIGNES; k++) {
            fixture.depense(j, fournisseur, depart.plusDays(k));
        }
    }

    private MockHttpServletRequestBuilder depenses() {
        return onglet(FOURNISSEURS.famille(), fournisseur, DEPENSES, connecte(j.admin()));
    }

    private String corps(MockHttpServletRequestBuilder requete) throws Exception {
        return mockMvc.perform(requete).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------ règle 2

    @Test
    void regle2_tailleParDefaut50_totalExact() throws Exception {
        String premiere = corps(depenses());

        assertAll(
                () -> assertThat(ids(premiere)).hasSize(50).doesNotHaveDuplicates(),
                () -> assertThat(((Number) JsonPath.read(premiere, "$.size")).intValue()).isEqualTo(50),
                () -> assertThat(((Number) JsonPath.read(premiere, "$.page")).intValue()).isZero(),
                () -> assertThat(total(premiere)).isEqualTo(LIGNES));
    }

    @Test
    void regle2_secondePage_rendLeReste_sansRecouvrement() throws Exception {
        String premiere = corps(depenses());
        String seconde = corps(depenses().param(PAGE, "1"));

        assertAll(
                () -> assertThat(ids(seconde)).hasSize(1).doesNotContainAnyElementsOf(ids(premiere)),
                () -> assertThat(total(seconde)).isEqualTo(LIGNES));
    }

    @ParameterizedTest
    @CsvSource({"-1, 50", "1000001, 50", "0, 0", "0, 201"})
    void regle2_horsBornes_400(int numero, int taille) throws Exception {
        mockMvc.perform(depenses().param(PAGE, String.valueOf(numero)).param(SIZE, String.valueOf(taille)))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @CsvSource({"0, 1", "1000000, 200"})
    void regle2_bornesIncluses_200(int numero, int taille) throws Exception {
        mockMvc.perform(depenses().param(PAGE, String.valueOf(numero)).param(SIZE, String.valueOf(taille)))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ règle 7 : une requête par page, pas par ligne

    @Test
    void regle7_depenses_nombreDeRequetesIndependantDuNombreDeLignes() throws Exception {
        long pourDeux = compterRequetes(2);
        long pourCinquante = compterRequetes(50);

        assertThat(pourCinquante).as("2 lignes : %d requêtes, 50 lignes : %d", pourDeux, pourCinquante)
                .isEqualTo(pourDeux);
    }

    private long compterRequetes(int taille) throws Exception {
        Statistics stats = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        boolean actives = stats.isStatisticsEnabled();
        stats.setStatisticsEnabled(true);
        try {
            em.flush();
            em.clear();
            stats.clear();
            String page = corps(depenses().param(SIZE, String.valueOf(taille)));
            assertThat(ids(page)).hasSize(taille);
            return stats.getPrepareStatementCount();
        } finally {
            stats.setStatisticsEnabled(actives);
        }
    }
}

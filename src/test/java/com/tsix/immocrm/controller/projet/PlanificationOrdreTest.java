package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixturePlanification;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.AppelsPlanification.taches;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** PUT {@code …/taches/ordre} : sœurs réordonnées, rangs et prédécesseurs réécrits (FEATURE-n9-planification.md R9, R10). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PlanificationOrdreTest {

    private static final LocalDate DEBUT = LocalDate.of(2026, 3, 2);
    private static final String LIGNE = "SELECT Sorting, Predecessors FROM CfgGanttTask WHERE PKey = ?";
    private static final String SORTING = "Sorting";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePlanification fixture;
    @Autowired private FixtureLotsParite parite;

    private RequestPostProcessor admin;
    private UUID planif;

    @BeforeEach
    void setUp() {
        fixture.tables();
        String jeton = FixtureProjetLots.jeton();
        admin = connecte(parite.administrateur("n9b.ord." + jeton), "Administrateur");
        planif = fixture.modele("ORD-" + jeton, DEBUT);
    }

    private int ordonner(UUID... ids) throws Exception {
        String liste = String.join(",", Arrays.stream(ids).map(id -> "\"" + id + "\"").toList());
        return mockMvc.perform(put(taches(planif) + "/ordre").with(admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"tacheIds\":[" + liste + "]}")).andReturn().getResponse().getStatus();
    }

    private Map<String, Object> ligne(UUID tache) {
        return fixture.lignes(LIGNE, tache).get(0);
    }

    @Test
    void ordre_soeursReordonnees_rangsEtPredecesseursReecrits() throws Exception {
        UUID a = fixture.tache(planif, null, "A", 1, 0, DEBUT, 2);
        UUID b = fixture.tache(planif, null, "B", 2, 0, DEBUT, 2);
        UUID c = fixture.tache(planif, null, "C", 3, 0, DEBUT, 2);
        fixture.predecesseurs(c, "1|0");

        int statut = ordonner(c, a, b);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(statut).isEqualTo(200);
        s.assertThat(ligne(c)).containsEntry(SORTING, 1).containsEntry("Predecessors", "2|0");
        s.assertThat(ligne(a)).containsEntry(SORTING, 2);
        s.assertThat(ligne(b)).containsEntry(SORTING, 3);
        s.assertAll();
    }

    @Test
    void ordre_listeIncompleteOuInconnue_400_rangsInchanges() throws Exception {
        UUID a = fixture.tache(planif, null, "A", 1, 0, DEBUT, 2);
        UUID b = fixture.tache(planif, null, "B", 2, 0, DEBUT, 2);

        int incomplete = ordonner(b);
        int inconnue = ordonner(b, a, UUID.randomUUID());

        SoftAssertions s = new SoftAssertions();
        s.assertThat(List.of(incomplete, inconnue)).containsOnly(400);
        s.assertThat(ligne(a)).containsEntry(SORTING, 1);
        s.assertThat(ligne(b)).containsEntry(SORTING, 2);
        s.assertAll();
    }
}

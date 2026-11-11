package com.tsix.immocrm.controller.projet;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixtureDepensesProjet;
import com.tsix.immocrm.fixtures.FixtureDepensesProjet.Jeu;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.controller.projet.AppelsProjet.LOTS;
import static com.tsix.immocrm.controller.projet.LectureDepenses.DEPENSES;
import static com.tsix.immocrm.controller.projet.LectureDepenses.PROJET_IDS;
import static com.tsix.immocrm.controller.projet.LectureDepenses.REGLEMENTS;
import static com.tsix.immocrm.controller.projet.LectureDepenses.SIZE;
import static com.tsix.immocrm.controller.projet.LectureDepenses.TAILLE;
import static com.tsix.immocrm.controller.projet.LectureDepenses.lot;
import static com.tsix.immocrm.controller.projet.LectureDepenses.nombre;
import static com.tsix.immocrm.controller.projet.LectureDepenses.projet;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cohérence règle 6 entre la synthèse A2, la fiche lot P1 et la liste des règlements (FEATURE-achat-depenses-projet.md
 * §3.3, §3.6 ; §6 test 7). Jeu : {@link FixtureDepensesProjet} (toutes les factures du lot A1 ont un décompte).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DepensesProjetCoherenceTest {

    private static final String REGLE = "montantRegle";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDepensesProjet fixture;
    @Autowired private EntityManager em;
    @Autowired private JdbcTemplate jdbc;

    private AppelsProjet appels;
    private Jeu jeu;
    private RequestPostProcessor a;

    @BeforeEach
    void setUp() {
        appels = new AppelsProjet(mockMvc);
        jeu = fixture.jeu();
        a = connecte(jeu.compteA(), FixtureDepensesProjet.ROLE);
        // Lots du jeu sans statut : rôle exempté de la clause de statut (FEATURE-visibilite-omran.md R1).
        em.flush();
        jdbc.update("UPDATE AdmRole SET WithSequenceFreeRestriction = 0 WHERE Code = ?", FixtureDepensesProjet.ROLE);
    }

    private Reponse ok(String url, String... params) throws Exception {
        Reponse r = appels.lire(a, url, params);
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        return r;
    }

    @Test
    void t7_lotDontToutesLesFacturesOntUnDecompte_aPayerEgalFactureP1_regleEgalRegleP1() throws Exception {
        Reponse lotsP1 = ok(LOTS, PROJET_IDS, jeu.projetA().toString(), SIZE, TAILLE);
        Map<String, Object> lotA2 = lot(projet(ok(DEPENSES).corps(), jeu.projetA()), jeu.lotA1());
        String filtre = "$.content[?(@.id == '" + jeu.lotA1() + "')].";

        BigDecimal totalFacture = premier(lotsP1, filtre + "totalFacture");
        BigDecimal totalRegle = premier(lotsP1, filtre + "totalRegle");

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(nombre(lotA2, "montantAPayer")).isEqualByComparingTo(totalFacture);
            s.assertThat(nombre(lotA2, REGLE)).isEqualByComparingTo(totalRegle);
        });
    }

    @Test
    void t7_sommeDesLignesNonAnnulees_egaleRegleDuProjet() throws Exception {
        Reponse reglements = ok(REGLEMENTS, PROJET_IDS, jeu.projetA().toString(), SIZE, TAILLE);
        Map<String, Object> synthese = projet(ok(DEPENSES).corps(), jeu.projetA());

        List<Object> montants = JsonPath.read(reglements.corps(), "$.content[?(@.annule == false)].montant");
        BigDecimal somme = montants.stream().map(v -> new BigDecimal(String.valueOf(v)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(montants).as("RG1, RG3, RG4 (part A)").hasSize(3);
            s.assertThat(nombre(synthese, REGLE)).isEqualByComparingTo(somme);
        });
    }

    private static BigDecimal premier(Reponse r, String chemin) {
        List<Object> valeurs = JsonPath.read(r.corps(), chemin);
        assertThat(valeurs).as(chemin).hasSize(1);
        return new BigDecimal(String.valueOf(valeurs.get(0)));
    }
}

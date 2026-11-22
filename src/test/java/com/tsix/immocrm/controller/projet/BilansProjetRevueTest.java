package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureBilansProjet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.AppelsBilans.PROJETS;
import static com.tsix.immocrm.controller.projet.AppelsBilans.occurrences;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Seconde revue N5a Bilans : R10 « StlCategory dépenses » (OMRAN {@code Yes.Prj.Tdb.Review/Project.cs:290-304} filtre
 * {@code IsCredit == false}, enfants du même sens) et masquage « elle-même et tous ses descendants valent 0 ».
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BilansProjetRevueTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureBilansProjet f;
    @Autowired private JdbcTemplate jdbc;

    private AppelsBilans appels;
    private UUID projet;

    @BeforeEach
    void setUp() {
        appels = new AppelsBilans(mockMvc);
        f.preparer();
        projet = f.projet("RV", false);
    }

    private Reponse lire() throws Exception {
        Reponse r = appels.lire(connecte(f.compte("rv." + UUID.randomUUID().toString().substring(0, 6), true, projet),
                "Commercial"), PROJETS, projet.toString());
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        return r;
    }

    @Test
    void categorieDeRecetteAbsenteDuBilan() throws Exception {
        UUID depense = f.categorie("Revue dépense", null, 1);
        UUID recette = f.categorie("Revue recette", null, 2);
        UUID enfantRecette = f.categorie("Revue enfant recette", depense, 1);
        jdbc.update("UPDATE StlCategory SET IsCredit = 1 WHERE PKey IN (?, ?)", recette.toString(), enfantRecette.toString());
        UUID tranche = f.tranche("T", projet);
        f.ventilationTranche(projet, tranche, "1");
        UUID etude = f.etude("S");
        f.poids(etude, tranche, "1");
        f.prevision(etude, depense, "100", "120", true);
        f.prevision(etude, recette, "1000", "1200", true);
        f.prevision(etude, enfantRecette, "500", "600", true);

        Reponse r = lire();

        assertThat(occurrences(r, depense)).as(r.corps()).isEqualTo(1);
        assertThat(occurrences(r, recette)).as("racine de recette affichée : %s", r.corps()).isZero();
        assertThat(occurrences(r, enfantRecette)).as("enfant de recette sous une dépense : %s", r.corps()).isZero();
        assertThat(AppelsBilans.valeur(r, depense, FixtureBilansProjet.FAISABILITE)).isEqualByComparingTo("100");
    }

    @Test
    void enfantsNonNulsVisiblesQuandLeParentSeCompenseAZero() throws Exception {
        UUID racine = f.categorie("Revue racine", null, 1);
        UUID a = f.categorie("Revue A", racine, 1);
        UUID b = f.categorie("Revue B", racine, 2);
        UUID lotA = f.lot("RA", projet, false);
        f.ventiler(lotA, a, "1");
        f.piece(lotA, projet, "RFA", FixtureAchat.FACTURE, "120", LocalDate.of(2026, 4, 1), null);
        UUID lotB = f.lot("RB", projet, false);
        f.ventiler(lotB, b, "1");
        f.piece(lotB, projet, "RAV", FixtureAchat.AVOIR, "120", LocalDate.of(2026, 4, 2), null);

        Reponse r = lire();

        assertThat(occurrences(r, a)).as("A vaut +100 en coût réel : %s", r.corps()).isEqualTo(1);
        assertThat(occurrences(r, b)).as("B vaut -100 en coût réel : %s", r.corps()).isEqualTo(1);
    }
}

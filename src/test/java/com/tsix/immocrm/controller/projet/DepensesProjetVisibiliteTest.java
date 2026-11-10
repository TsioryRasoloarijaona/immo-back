package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureDepensesProjet;
import com.tsix.immocrm.fixtures.FixtureDepensesProjet.Jeu;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.controller.projet.LectureDepenses.DEPENSES;
import static com.tsix.immocrm.controller.projet.LectureDepenses.PROJET_IDS;
import static com.tsix.immocrm.controller.projet.LectureDepenses.REGLEMENTS;
import static com.tsix.immocrm.controller.projet.LectureDepenses.SIZE;
import static com.tsix.immocrm.controller.projet.LectureDepenses.TAILLE;
import static com.tsix.immocrm.controller.projet.LectureDepenses.lignesDuReglement;
import static com.tsix.immocrm.controller.projet.LectureDepenses.nombre;
import static com.tsix.immocrm.controller.projet.LectureDepenses.total;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Visibilité des dépenses (FEATURE-achat-depenses-projet.md §3.1 ; §6 tests 1, 2, 3). Jeu : {@link FixtureDepensesProjet}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DepensesProjetVisibiliteTest {

    private static final String PROJETS = "$[*].projetId";
    private static final String LIGNES = "$.content[*].id";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDepensesProjet fixture;
    @Autowired private FixtureAchat achat;

    private AppelsProjet appels;
    private Jeu jeu;
    private RequestPostProcessor a;
    private RequestPostProcessor ab;

    @BeforeEach
    void setUp() {
        appels = new AppelsProjet(mockMvc);
        jeu = fixture.jeu();
        a = connecte(jeu.compteA(), FixtureDepensesProjet.ROLE);
        ab = connecte(jeu.compteAB(), FixtureDepensesProjet.ROLE);
    }

    private Reponse ok(RequestPostProcessor qui, String url, String... params) throws Exception {
        Reponse r = appels.lire(qui, url, params);
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        return r;
    }

    // ------------------------------------------------------------------ §6.1 habilité A seul

    @Test
    void t1_habiliteA_synthese_neContientQueA() throws Exception {
        Reponse r = ok(a, DEPENSES);

        assertThat(r.ids(PROJETS)).containsExactly(jeu.projetA().toString());
    }

    @Test
    void t1_habiliteA_projetIdsB_listeVide200() throws Exception {
        Reponse r = ok(a, DEPENSES, PROJET_IDS, jeu.projetB().toString());

        assertThat(r.ids(PROJETS)).isEmpty();
    }

    @Test
    void t1_habiliteA_projetIdsAetB_seulementA() throws Exception {
        Reponse r = ok(a, DEPENSES, PROJET_IDS, jeu.projetA().toString(), PROJET_IDS, jeu.projetB().toString());

        assertThat(r.ids(PROJETS)).containsExactly(jeu.projetA().toString());
    }

    @Test
    void t1_habiliteA_reglements_lotIdDeB_pageVide() throws Exception {
        Reponse horsPerimetre = ok(a, REGLEMENTS, "lotId", jeu.lotB().toString(), SIZE, TAILLE);
        Reponse temoin = ok(a, REGLEMENTS, "lotId", jeu.lotA1().toString(), SIZE, TAILLE);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(total(horsPerimetre.corps())).isZero();
            s.assertThat(horsPerimetre.ids(LIGNES)).isEmpty();
            s.assertThat(temoin.ids(LIGNES)).as("témoin lot A1")
                    .containsExactlyInAnyOrder(jeu.ligneEnCours().toString(), jeu.ligneAnnulee().toString());
        });
    }

    @Test
    void t1_habiliteA_reglements_projetIdsB_pageVide() throws Exception {
        Reponse r = ok(a, REGLEMENTS, PROJET_IDS, jeu.projetB().toString(), SIZE, TAILLE);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(total(r.corps())).isZero();
            s.assertThat(r.ids(LIGNES)).isEmpty();
        });
    }

    @Test
    void t1_habiliteA_reglements_aucuneLigneDeB() throws Exception {
        Reponse r = ok(a, REGLEMENTS, SIZE, TAILLE);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.ids("$.content[*].projetId")).isNotEmpty().containsOnly(jeu.projetA().toString());
            s.assertThat(r.ids(LIGNES)).doesNotContain(jeu.ligneAChevalB().toString());
        });
    }

    // ------------------------------------------------------------------ §6.2 aucune exemption de rôle

    @Test
    void t2_administrateurSansHabilitation_syntheseVideEtPageVide() throws Exception {
        RequestPostProcessor admin = connecte(achat.compte("DEPADM-" + jeu.jeton()), "Administrateur");

        Reponse synthese = ok(admin, DEPENSES);
        Reponse reglements = ok(admin, REGLEMENTS, SIZE, TAILLE);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(synthese.ids(PROJETS)).isEmpty();
            s.assertThat(total(reglements.corps())).isZero();
        });
    }

    @Test
    @WithMockUser
    void t2_appelantNonIdentifiable_syntheseVideEtPageVide() throws Exception {
        Reponse synthese = ok(null, DEPENSES);
        Reponse reglements = ok(null, REGLEMENTS, SIZE, TAILLE);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(synthese.ids(PROJETS)).isEmpty();
            s.assertThat(total(reglements.corps())).isZero();
        });
    }

    // ------------------------------------------------------------------ §6.3 règlement à cheval sur A et B

    @Test
    void t3_reglementACheval_habiliteA_voitSeulementSaLigneEtSonMontant() throws Exception {
        Reponse r = ok(a, REGLEMENTS, SIZE, TAILLE);

        List<Map<String, Object>> lignes = lignesDuReglement(r.corps(), jeu.code("-RG4"));

        assertThat(lignes).hasSize(1);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(lignes.get(0)).containsEntry("id", jeu.ligneAChevalA().toString())
                    .containsEntry("factureId", jeu.factureACheval().toString())
                    .containsEntry("projetId", jeu.projetA().toString());
            s.assertThat(nombre(lignes.get(0), "montant")).isEqualByComparingTo("150");
        });
    }

    @Test
    void t3_reglementACheval_habiliteAB_voitLesDeuxLignes() throws Exception {
        Reponse r = ok(ab, REGLEMENTS, SIZE, TAILLE);

        List<Map<String, Object>> lignes = lignesDuReglement(r.corps(), jeu.code("-RG4"));

        assertThat(lignes).extracting(l -> l.get("id"))
                .containsExactlyInAnyOrder(jeu.ligneAChevalA().toString(), jeu.ligneAChevalB().toString());
    }

    @Test
    void t3_reglementACheval_habiliteA_syntheseNeCompteQueSaLigne() throws Exception {
        Reponse r = ok(a, DEPENSES);

        Map<String, Object> horsLot = LectureDepenses.lot(LectureDepenses.projet(r.corps(), jeu.projetA()), null);

        assertThat(nombre(horsLot, "montantRegle")).as("320 (FH1) + 150 (FH2)").isEqualByComparingTo("470");
    }
}

package com.tsix.immocrm.controller.projet;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Piece;
import com.tsix.immocrm.fixtures.FixtureDepensesProjet;
import com.tsix.immocrm.fixtures.FixtureDepensesProjet.Jeu;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.LectureDepenses.DEPENSES;
import static com.tsix.immocrm.controller.projet.LectureDepenses.PROJET_IDS;
import static com.tsix.immocrm.controller.projet.LectureDepenses.SIZE;
import static com.tsix.immocrm.controller.projet.LectureDepenses.TAILLE;
import static com.tsix.immocrm.controller.projet.LectureDepenses.lot;
import static com.tsix.immocrm.controller.projet.LectureDepenses.lots;
import static com.tsix.immocrm.controller.projet.LectureDepenses.nombre;
import static com.tsix.immocrm.controller.projet.LectureDepenses.projet;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Synthèse des dépenses par projet et par lot (FEATURE-achat-depenses-projet.md §3.2-3.4, §4.2 ; §6 tests 4 et 5).
 * Jeu : {@link FixtureDepensesProjet}. Attendus du projet A : 4 factures (FA1 1000, FA2 500, FH1 300, FH2 TTC 400 /
 * à payer 380), réglé 600 + 320 + 150 = 1070 (RG2 annulé exclu), restes 400, 500, −20, 230.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DepensesProjetSyntheseTest {

    private static final String DOCUMENTS_ACHAT = "/api/achat/documents";
    private static final String NB_FACTURES = "nbFactures";
    private static final String TTC = "montantTTC";
    private static final String A_PAYER = "montantAPayer";
    private static final String REGLE = "montantRegle";
    private static final String RESTE = "resteARegler";
    private static final String NON_SOLDEES = "nbNonSoldees";
    private static final String TROP_REGLEES = "nbTropReglees";
    private static final String AVEC_DECOMPTE = "nbFacturesDecompte";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDepensesProjet fixture;
    @Autowired private FixtureAchat achat;

    private AppelsProjet appels;
    private Jeu jeu;
    private RequestPostProcessor a;

    @BeforeEach
    void setUp() {
        appels = new AppelsProjet(mockMvc);
        jeu = fixture.jeu();
        a = connecte(jeu.compteA(), FixtureDepensesProjet.ROLE);
    }

    private Reponse ok(RequestPostProcessor qui, String url, String... params) throws Exception {
        Reponse r = appels.lire(qui, url, params);
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        return r;
    }

    private Map<String, Object> projetA() throws Exception {
        return projet(ok(a, DEPENSES).corps(), jeu.projetA());
    }

    private static BigDecimal somme(Reponse liste, String champ) {
        List<Object> valeurs = JsonPath.read(liste.corps(), "$.content[*]." + champ);
        return valeurs.stream().map(v -> new BigDecimal(String.valueOf(v))).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ------------------------------------------------------------------ §6.5 même calcul que /api/achat/documents

    @Test
    void t5_projetA_aPayerRegleReste_egauxALaSommeDeLaListeDesFactures() throws Exception {
        Reponse factures = ok(a, DOCUMENTS_ACHAT, PROJET_IDS, jeu.projetA().toString(), "natures", "INVOICE",
                SIZE, TAILLE);
        Map<String, Object> synthese = projetA();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(nombre(synthese, NB_FACTURES)).isEqualByComparingTo(factures.nombre("$.totalElements"));
            s.assertThat(nombre(synthese, A_PAYER)).isEqualByComparingTo(somme(factures, A_PAYER));
            s.assertThat(nombre(synthese, REGLE)).isEqualByComparingTo(somme(factures, REGLE));
            s.assertThat(nombre(synthese, RESTE)).isEqualByComparingTo(somme(factures, RESTE));
            s.assertThat(nombre(synthese, TTC)).isEqualByComparingTo(somme(factures, TTC));
        });
    }

    @Test
    void t5_projetA_valeursDuJeu() throws Exception {
        Map<String, Object> synthese = projetA();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(synthese).containsEntry("projetCode", jeu.projetACode())
                    .containsEntry("projetDescription", "Projet " + jeu.projetACode());
            s.assertThat(nombre(synthese, NB_FACTURES)).isEqualByComparingTo("4");
            s.assertThat(nombre(synthese, TTC)).isEqualByComparingTo("2200");
            s.assertThat(nombre(synthese, A_PAYER)).isEqualByComparingTo("2180");
            s.assertThat(nombre(synthese, REGLE)).isEqualByComparingTo("1070");
            s.assertThat(nombre(synthese, RESTE)).isEqualByComparingTo("1110");
            s.assertThat(nombre(synthese, AVEC_DECOMPTE)).isEqualByComparingTo("2");
        });
    }

    @Test
    void t5_documentAPayer_niCompteNiSomme() throws Exception {
        Map<String, Object> synthese = projetA();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(nombre(synthese, NB_FACTURES)).as("DTP de 700 non compté").isEqualByComparingTo("4");
            s.assertThat(nombre(synthese, A_PAYER)).as("DTP de 700 non sommé").isEqualByComparingTo("2180");
        });
    }

    @Test
    void t5_projetVisibleSansFacture_absentDeLaSynthese() throws Exception {
        UUID projetC = achat.projet(jeu.code("-PC"), jeu.societe());
        achat.piece(Piece.facture(jeu.code("-DTPC"), LocalDate.of(2026, 3, 9), jeu.fournisseur(),
                jeu.fournisseurNom(), jeu.societe(), projetC, new BigDecimal("90")).nature("DOCUMENT_TO_PAY"));
        achat.habiliter(jeu.compteA(), projetC);

        Reponse r = ok(a, DEPENSES);

        assertThat(r.ids("$[*].projetId")).containsExactly(jeu.projetA().toString());
    }

    @Test
    void t5_tropReglee_compteeDansTropRegleesPasDansNonSoldees() throws Exception {
        Map<String, Object> synthese = projetA();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(nombre(synthese, TROP_REGLEES)).isEqualByComparingTo("1");
            s.assertThat(nombre(synthese, NON_SOLDEES)).as("FA1, FA2, FH2 ; FH1 trop réglée exclue")
                    .isEqualByComparingTo("3");
            s.assertThat(nombre(synthese, "resteNonSoldees")).as("400 + 500 + 230").isEqualByComparingTo("1130");
        });
    }

    // ------------------------------------------------------------------ §6.4 ligne CANCELED exclue du réglé

    @Test
    void t4_reglementAnnule_exclusDuRegleDuLot() throws Exception {
        Map<String, Object> lotA1 = lot(projetA(), jeu.lotA1());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(nombre(lotA1, REGLE)).as("600 en cours, 200 annulé exclu").isEqualByComparingTo("600");
            s.assertThat(nombre(lotA1, RESTE)).isEqualByComparingTo("900");
        });
    }

    // ------------------------------------------------------------------ §4.2 lignes de lot

    @Test
    void t5_ligneDuLotA1() throws Exception {
        Map<String, Object> lotA1 = lot(projetA(), jeu.lotA1());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(lotA1).containsEntry("lotCode", jeu.lotA1Code());
            s.assertThat(nombre(lotA1, NB_FACTURES)).isEqualByComparingTo("2");
            s.assertThat(nombre(lotA1, TTC)).isEqualByComparingTo("1500");
            s.assertThat(nombre(lotA1, A_PAYER)).isEqualByComparingTo("1500");
            s.assertThat(nombre(lotA1, NON_SOLDEES)).isEqualByComparingTo("2");
            s.assertThat(nombre(lotA1, TROP_REGLEES)).isEqualByComparingTo("0");
            s.assertThat(nombre(lotA1, AVEC_DECOMPTE)).isEqualByComparingTo("2");
        });
    }

    @Test
    void t5_ligneHorsLot_lotIdNul() throws Exception {
        Map<String, Object> horsLot = lot(projetA(), null);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(nombre(horsLot, NB_FACTURES)).isEqualByComparingTo("2");
            s.assertThat(nombre(horsLot, TTC)).isEqualByComparingTo("700");
            s.assertThat(nombre(horsLot, A_PAYER)).isEqualByComparingTo("680");
            s.assertThat(nombre(horsLot, REGLE)).isEqualByComparingTo("470");
            s.assertThat(nombre(horsLot, RESTE)).isEqualByComparingTo("210");
            s.assertThat(nombre(horsLot, NON_SOLDEES)).isEqualByComparingTo("1");
            s.assertThat(nombre(horsLot, TROP_REGLEES)).isEqualByComparingTo("1");
            s.assertThat(nombre(horsLot, AVEC_DECOMPTE)).isEqualByComparingTo("0");
        });
    }

    @Test
    void lots_triesParCode_horsLotEnDernier() throws Exception {
        UUID lotA0 = achat.lot(jeu.code("-LA0"), jeu.projetA());
        fixture.facture(jeu.code("-FA0"), LocalDate.of(2026, 3, 1), jeu.fournisseur(), jeu.fournisseurNom(),
                jeu.societe(), jeu.projetA(), lotA0, "100");

        List<Map<String, Object>> lignes = lots(projetA());

        assertThat(lignes).extracting(l -> l.get("lotId"))
                .containsExactly(lotA0.toString(), jeu.lotA1().toString(), null);
    }

    @Test
    void synthese_trieeParCodeDeProjet() throws Exception {
        Reponse r = ok(connecte(jeu.compteAB(), FixtureDepensesProjet.ROLE), DEPENSES);

        assertThat(r.ids("$[*].projetCode")).containsExactly(jeu.projetACode(), jeu.projetBCode());
    }

    @Test
    void synthese_projetB_pourHabiliteAB() throws Exception {
        Reponse r = ok(connecte(jeu.compteAB(), FixtureDepensesProjet.ROLE), DEPENSES);

        Map<String, Object> b = projet(r.corps(), jeu.projetB());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(nombre(b, NB_FACTURES)).isEqualByComparingTo("1");
            s.assertThat(nombre(b, A_PAYER)).isEqualByComparingTo("800");
            s.assertThat(nombre(b, REGLE)).as("ligne de 250 du règlement à cheval").isEqualByComparingTo("250");
            s.assertThat(nombre(b, RESTE)).isEqualByComparingTo("550");
        });
    }
}

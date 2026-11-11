package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureDepensesProjet;
import com.tsix.immocrm.fixtures.FixtureDepensesProjet.Jeu;
import com.tsix.immocrm.fixtures.FixtureVisibiliteOmran;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
 * Visibilité des dépenses (FEATURE-achat-depenses-projet.md §3.1 ; §6 tests 1, 2, 3 ; FEATURE-visibilite-omran.md R5 =
 * société ET (projet OU sans projet) ET module PRJ). Jeu : {@link FixtureDepensesProjet}.
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
    @Autowired private FixtureVisibiliteOmran visibilite;

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

    // ------------------------------------------------------------------ FEATURE-visibilite-omran.md R5 : R3 + module PRJ

    @Test
    void r5_projetHabiliteSansSociete_syntheseVideEtPageVide() throws Exception {
        AdmUser projetSeul = achat.compte("DEPP-" + jeu.jeton(), FixtureDepensesProjet.ROLE);
        achat.habiliter(projetSeul, jeu.projetA());
        RequestPostProcessor qui = connecte(projetSeul, FixtureDepensesProjet.ROLE);

        Reponse synthese = ok(qui, DEPENSES);
        Reponse reglements = ok(qui, REGLEMENTS, SIZE, TAILLE);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(synthese.ids(PROJETS)).doesNotContain(jeu.projetA().toString());
            s.assertThat(reglements.ids(LIGNES)).doesNotContain(jeu.ligneEnCours().toString());
        });
    }

    @Test
    void r5_pieceDUnAutreModule_exclueDeLaSyntheseEtDesReglements() throws Exception {
        UUID autre = fixture.facture(jeu.code("-AUT"), LocalDate.of(2026, 3, 9), jeu.fournisseur(), jeu.fournisseurNom(),
                jeu.societe(), jeu.projetA(), null, "90");
        visibilite.modulePiece(autre, visibilite.autreModule("STLX-" + jeu.jeton()));
        UUID rg = fixture.reglement(jeu.code("-RGX"), FixtureAchat.EN_COURS, LocalDate.of(2026, 4, 14),
                reglementType(), "90");
        UUID ligne = fixture.ligne(rg, autre, "90");

        Reponse synthese = ok(a, DEPENSES);
        Reponse reglements = ok(a, REGLEMENTS, SIZE, TAILLE);

        Map<String, Object> horsLot = LectureDepenses.lot(LectureDepenses.projet(synthese.corps(), jeu.projetA()), null);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(nombre(horsLot, "montantRegle")).as("320 + 150, sans les 90 de l'autre module")
                    .isEqualByComparingTo("470");
            s.assertThat(reglements.ids(LIGNES)).doesNotContain(ligne.toString())
                    .contains(jeu.ligneEnCours().toString());
        });
    }

    @Test
    void r5_pieceSansProjetDuModulePrj_reglementVisibleAuxComptesDeSaSociete() throws Exception {
        UUID sansProjet = fixture.facture(jeu.code("-F0"), LocalDate.of(2026, 3, 9), jeu.fournisseur(),
                jeu.fournisseurNom(), jeu.societe(), null, null, "60");
        UUID rg = fixture.reglement(jeu.code("-RG0"), FixtureAchat.EN_COURS, LocalDate.of(2026, 4, 14),
                reglementType(), "60");
        UUID ligne = fixture.ligne(rg, sansProjet, "60");
        AdmUser projetSeul = achat.compte("DEPP-" + jeu.jeton(), FixtureDepensesProjet.ROLE);
        achat.habiliter(projetSeul, jeu.projetA());

        Reponse avecSociete = ok(a, REGLEMENTS, SIZE, TAILLE);
        Reponse sansSociete = ok(connecte(projetSeul, FixtureDepensesProjet.ROLE), REGLEMENTS, SIZE, TAILLE);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(avecSociete.ids(LIGNES)).contains(ligne.toString());
            s.assertThat(sansSociete.ids(LIGNES)).doesNotContain(ligne.toString());
        });
    }

    private UUID reglementType() {
        return achat.typeReglement("VIRX-" + jeu.jeton());
    }
}

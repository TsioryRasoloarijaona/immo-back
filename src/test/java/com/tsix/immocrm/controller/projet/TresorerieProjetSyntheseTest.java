package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.domain.FuseauMetier;
import com.tsix.immocrm.fixtures.FixtureTresorerieProjet;
import com.tsix.immocrm.fixtures.FixtureTresorerieProjet.Jeu;
import com.tsix.immocrm.fixtures.FixtureTresorerieProjet.Ventile;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;

import static com.tsix.immocrm.controller.projet.LectureTresorerie.ANNEE;
import static com.tsix.immocrm.controller.projet.LectureTresorerie.ANNEE_TEST;
import static com.tsix.immocrm.controller.projet.LectureTresorerie.ONGLET_FOURNISSEUR;
import static com.tsix.immocrm.controller.projet.LectureTresorerie.ONGLET_PROJET;
import static com.tsix.immocrm.controller.projet.LectureTresorerie.PROJETS;
import static com.tsix.immocrm.controller.projet.LectureTresorerie.SOCIETE;
import static com.tsix.immocrm.controller.projet.LectureTresorerie.TRESORERIE;
import static com.tsix.immocrm.controller.projet.LectureTresorerie.enfants;
import static com.tsix.immocrm.controller.projet.LectureTresorerie.libelles;
import static com.tsix.immocrm.controller.projet.LectureTresorerie.mois;
import static com.tsix.immocrm.controller.projet.LectureTresorerie.nombre;
import static com.tsix.immocrm.controller.projet.LectureTresorerie.noeud;
import static com.tsix.immocrm.controller.projet.LectureTresorerie.racine;
import static com.tsix.immocrm.controller.projet.LectureTresorerie.total;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * N5a Trésorerie projet, arbre mensuel (FEATURE-n5-tableaux-de-bord-projet.md R1–R4, R14, D5 remplacé, formules sur
 * tables des « Réponses aux questions du test-writer »). Jeu : {@link FixtureTresorerieProjet} ; aujourd'hui = 15/06/2077.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TresorerieProjetSyntheseTest {

    private static final boolean DEBIT = false;
    private static final boolean CREDIT = true;
    private static final String REGLE = "regle";

    @TestBean(name = "horlogeMetier")
    private Clock horloge;

    static Clock horloge() {
        return Clock.fixed(FixtureTresorerieProjet.AUJOURDHUI.atTime(10, 0).atZone(FuseauMetier.ZONE).toInstant(),
                FuseauMetier.ZONE);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureTresorerieProjet fixture;

    private AppelsProjet appels;
    private Jeu jeu;

    @BeforeEach
    void setUp() {
        appels = new AppelsProjet(mockMvc);
        jeu = fixture.jeu();
    }

    private String lire(String annee) throws Exception {
        Reponse r = appels.lire(connecte(jeu.compteA(), FixtureTresorerieProjet.ROLE), TRESORERIE, ANNEE, annee,
                PROJETS, jeu.projetA().toString(), PROJETS, jeu.projetB().toString(), SOCIETE, jeu.societe().toString());
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        return r.corps();
    }

    // ------------------------------------------------------------------ R1 année civile

    @Test
    void r1_douzeMois_moisEgalSommeDesLignesDeLAnnee() throws Exception {
        Map<String, Object> f = noeud(lire(ANNEE_TEST), ONGLET_PROJET, DEBIT, jeu.fournisseurNom());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat((List<?>) f.get("montants")).hasSize(12);
            s.assertThat(mois(f, 6)).as("juin : P0 40 + P1 100 + P6 50 + FA2 120 reportés").isEqualByComparingTo("310");
            s.assertThat(mois(f, 9)).as("septembre : P2").isEqualByComparingTo("200");
            s.assertThat(mois(f, 11)).as("novembre : reste FA1").isEqualByComparingTo("600");
            s.assertThat(mois(f, 1)).as("janvier : P7 est en 2078").isEqualByComparingTo("0");
            s.assertThat(total(f)).isEqualByComparingTo("1110");
        });
    }

    @Test
    void r1_autreAnnee_neLitQueLesLignesDeCetteAnnee() throws Exception {
        Map<String, Object> f = noeud(lire("2078"), ONGLET_PROJET, DEBIT, jeu.fournisseurNom());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(mois(f, 1)).as("P7 2078-01-10").isEqualByComparingTo("500");
            s.assertThat(total(f)).isEqualByComparingTo("500");
        });
    }

    @Test
    void r1_anneeAbsente_anneeDuJour() throws Exception {
        Reponse r = appels.lire(connecte(jeu.compteA(), FixtureTresorerieProjet.ROLE), TRESORERIE,
                PROJETS, jeu.projetA().toString(), SOCIETE, jeu.societe().toString());

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.nombre("$.annee")).isEqualByComparingTo("2077");
    }

    // ------------------------------------------------------------------ R2 onglet Projet

    @Test
    void r2_ongletProjet_racinesEncaissementDecaissementParEstCredit() throws Exception {
        String corps = lire(ANNEE_TEST);

        Map<String, Object> debit = racine(corps, ONGLET_PROJET, DEBIT).orElseThrow();
        Map<String, Object> credit = racine(corps, ONGLET_PROJET, CREDIT).orElseThrow();
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(debit).containsEntry("libelle", "Décaissement");
            s.assertThat(credit).containsEntry("libelle", "Encaissement");
            s.assertThat(total(debit)).as("1110 + 300").isEqualByComparingTo("1410");
        });
    }

    @Test
    void r2_ongletProjet_racinePuisProjetPuisFournisseurOuLot() throws Exception {
        Map<String, Object> debit = racine(lire(ANNEE_TEST), ONGLET_PROJET, DEBIT).orElseThrow();

        List<Map<String, Object>> projets = enfants(debit);
        assertThat(projets).as("un nœud projet (A) sous Décaissement").hasSize(1);
        assertThat(enfants(projets.get(0))).extracting(n -> n.get("libelle"))
                .containsExactlyInAnyOrder(jeu.fournisseurNom(), jeu.lotA2Libelle());
    }

    @Test
    void formule_lotSansFournisseur_groupeSurLaDescriptionDuLot() throws Exception {
        Map<String, Object> lot = noeud(lire(ANNEE_TEST), ONGLET_PROJET, DEBIT, jeu.lotA2Libelle());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(mois(lot, 8)).as("P3").isEqualByComparingTo("300");
            s.assertThat(total(lot)).isEqualByComparingTo("300");
        });
    }

    @Test
    void formule_avoir_enEncaissement() throws Exception {
        Map<String, Object> avoir = noeud(lire(ANNEE_TEST), ONGLET_PROJET, CREDIT, jeu.fournisseurNom());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(mois(avoir, 8).abs()).as("AV1 échéance 12/08").isEqualByComparingTo("70");
            s.assertThat(total(avoir).abs()).isEqualByComparingTo("70");
        });
    }

    // ------------------------------------------------------------------ R3 onglet Fournisseur

    @Test
    void r3_ongletFournisseur_fournisseurSousLaRacineSansNiveauProjet() throws Exception {
        Map<String, Object> debit = racine(lire(ANNEE_TEST), ONGLET_FOURNISSEUR, DEBIT).orElseThrow();

        assertThat(enfants(debit)).extracting(n -> n.get("libelle"))
                .containsExactlyInAnyOrder(jeu.fournisseurNom(), jeu.lotA2Libelle());
    }

    @Test
    void r3_totalDUnOnglet_egalTotalDeLAutre() throws Exception {
        String corps = lire(ANNEE_TEST);

        SoftAssertions.assertSoftly(s -> {
            for (boolean credit : new boolean[] {DEBIT, CREDIT}) {
                Map<String, Object> projet = racine(corps, ONGLET_PROJET, credit).orElseThrow();
                Map<String, Object> fournisseur = racine(corps, ONGLET_FOURNISSEUR, credit).orElseThrow();
                s.assertThat(total(fournisseur)).as("total, crédit = %s", credit).isEqualByComparingTo(total(projet));
                for (int m = 1; m <= 12; m++) {
                    s.assertThat(mois(fournisseur, m)).as("mois %d, crédit = %s", m, credit)
                            .isEqualByComparingTo(mois(projet, m));
                }
            }
        });
    }

    // ------------------------------------------------------------------ R4 ligne nulle

    @Test
    void r4_ligneDontLesDouzeMoisSontNuls_absente() throws Exception {
        assertThat(libelles(lire(ANNEE_TEST))).isNotEmpty().doesNotContain(jeu.lotA3Libelle());
    }

    // ------------------------------------------------------------------ formules sur tables (exclusions)

    @Test
    void formule_factureSansLot_absente() throws Exception {
        assertThat(libelles(lire(ANNEE_TEST))).isNotEmpty().doesNotContain(jeu.absentSansLot());
    }

    @Test
    void formule_factureNonPayable_absente() throws Exception {
        assertThat(libelles(lire(ANNEE_TEST))).isNotEmpty().doesNotContain(jeu.absentNonPayable());
    }

    @Test
    void formule_resteInferieurA50Centimes_absent() throws Exception {
        assertThat(libelles(lire(ANNEE_TEST))).isNotEmpty().doesNotContain(jeu.absentSolde());
    }

    @Test
    void formule_isFinancialRefundFaux_absente() throws Exception {
        assertThat(libelles(lire(ANNEE_TEST))).isNotEmpty().doesNotContain(jeu.absentRefundFaux());
    }

    // ------------------------------------------------------------------ R14 / D4 règlement annulé

    @Test
    void r14_reglementAnnule_neReduitPasLeReste() throws Exception {
        Map<String, Object> f = noeud(lire(ANNEE_TEST), ONGLET_PROJET, DEBIT, jeu.fournisseurNom());

        assertThat(mois(f, 11)).as("1000 − 400 (IN_WORK), sans les 250 CANCELED").isEqualByComparingTo("600");
    }

    // ------------------------------------------------------------------ formule : ventilation par tranche

    /** Lot du projet V ventilé à 0,5 sur sa tranche : prévision 800 → 400 (montant × VentilationRate). */
    @Test
    void formule_ventilationTranche_montantFoisTaux() throws Exception {
        Ventile v = fixture.ventile(jeu, "0.5");

        Reponse r = appels.lire(connecte(jeu.compteA(), FixtureTresorerieProjet.ROLE), TRESORERIE, ANNEE, ANNEE_TEST,
                PROJETS, v.projet().toString(), SOCIETE, jeu.societe().toString());
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        Map<String, Object> f = noeud(r.corps(), ONGLET_PROJET, DEBIT, v.fournisseurNom());

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(mois(f, 9)).isEqualByComparingTo("400");
            s.assertThat(total(f)).isEqualByComparingTo("400");
        });
    }

    // ------------------------------------------------------------------ R5 Réglé (date reportée)

    /** R5 : Réglé = Σ des lignes à date < aujourd'hui, date reportée comprise ; aucune ligne n'est avant aujourd'hui → 0. */
    @Test
    void r5_regle_surLaDateReportee_vautZero() throws Exception {
        String corps = lire(ANNEE_TEST);
        Map<String, Object> f = noeud(corps, ONGLET_PROJET, DEBIT, jeu.fournisseurNom());
        Map<String, Object> debit = racine(corps, ONGLET_PROJET, DEBIT).orElseThrow();

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(f).containsKey(REGLE);
            s.assertThat(nombre(f.get(REGLE))).as("P0, P1, FA2 reportés au 15/06").isEqualByComparingTo("0");
            s.assertThat(nombre(debit.get(REGLE))).isEqualByComparingTo("0");
        });
    }

    // ------------------------------------------------------------------ D5 remplacé : report à aujourd'hui

    @Test
    void d5_previsionAnterieure_reporteeAuJour_absenteDeSonAnnee() throws Exception {
        assertThat(libelles(lire("2076"))).doesNotContain(jeu.fournisseurNom());
    }

    // ------------------------------------------------------------------ ProjetChoixDto

    @Test
    void projets_choixDuSelecteur_projetsHabilitesDeLaSociete() throws Exception {
        Reponse r = appels.lire(connecte(jeu.compteA(), FixtureTresorerieProjet.ROLE), TRESORERIE, ANNEE, ANNEE_TEST,
                PROJETS, jeu.projetA().toString(), SOCIETE, jeu.societe().toString());

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        assertThat(r.ids("$.projets[*].id")).map(String::toLowerCase)
                .contains(jeu.projetA().toString()).doesNotContain(jeu.projetB().toString());
    }
}

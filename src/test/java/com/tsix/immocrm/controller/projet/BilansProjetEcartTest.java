package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureBilansProjet;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.AppelsBilans.COLONNE1;
import static com.tsix.immocrm.controller.projet.AppelsBilans.COLONNE2;
import static com.tsix.immocrm.controller.projet.AppelsBilans.PROJETS;
import static com.tsix.immocrm.controller.projet.AppelsBilans.TTC;
import static com.tsix.immocrm.controller.projet.AppelsBilans.valeur;
import static com.tsix.immocrm.fixtures.FixtureBilansProjet.COUT_REEL;
import static com.tsix.immocrm.fixtures.FixtureBilansProjet.ESTIMATION_BASE;
import static com.tsix.immocrm.fixtures.FixtureBilansProjet.MARCHE;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.controller.projet.AppelsBilans.optionnel;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * N5a R12 — écart = colonne1 − colonne2, HT ou TTC selon {@code ttc} ; paramètres absents → {@code ecart1 = ecart2 = null},
 * écart 0 (réponses du manager au test-writer Bilans). Lot ventilé E1 0,6 / E2 0,4 sous R : estimation de base 1000 HT,
 * devis retenu 800 HT (Marché), facture 600 HT (Coût réel).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BilansProjetEcartTest {

    private static final BigDecimal PRECISION = new BigDecimal("0.01");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureBilansProjet f;

    private AppelsBilans appels;
    private UUID projet;
    private UUID racine;
    private UUID enfant1;
    private UUID enfant2;

    @BeforeEach
    void setUp() {
        appels = new AppelsBilans(mockMvc);
        f.preparer();
        projet = f.projet("EC", false);
        racine = f.categorie("Ecart racine", null, 1);
        enfant1 = f.categorie("Ecart enfant 1", racine, 1);
        enfant2 = f.categorie("Ecart enfant 2", racine, 2);
        UUID lot = f.lot("LEC", projet, false);
        f.ventiler(lot, enfant1, "0.6");
        f.ventiler(lot, enfant2, "0.4");
        f.estimationBase(lot, "1000");
        f.devis(lot, projet, "EDV", "960", LocalDate.of(2026, 2, 10), true);
        f.piece(lot, projet, "EFA", FixtureAchat.FACTURE, "720", LocalDate.of(2026, 4, 1), null);
    }

    private Reponse lire(String... autres) throws Exception {
        String[] params = new String[autres.length + 2];
        params[0] = PROJETS;
        params[1] = projet.toString();
        System.arraycopy(autres, 0, params, 2, autres.length);
        Reponse r = appels.lire(connecte(f.compte("ec." + UUID.randomUUID().toString().substring(0, 6), true, projet),
                "Commercial"), params);
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        return r;
    }

    private static void ecart(SoftAssertions s, Reponse r, UUID categorie, String attendu) {
        s.assertThat(valeur(r, categorie, null)).as("ecart %s", categorie).isCloseTo(new BigDecimal(attendu), within(PRECISION));
    }

    @Test
    void r12_ecart_marcheMoinsEstimationBase_ht_surChaqueNoeud() throws Exception {
        Reponse r = lire(COLONNE1, MARCHE, COLONNE2, ESTIMATION_BASE);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(r.valeur("$.ecart1")).isEqualTo(MARCHE);
        s.assertThat(r.valeur("$.ecart2")).isEqualTo(ESTIMATION_BASE);
        ecart(s, r, enfant1, "-120");
        ecart(s, r, enfant2, "-80");
        ecart(s, r, racine, "-200");
        s.assertAll();
    }

    /** Ordre inverse : Coût réel (600) − Marché (800) ≠ Marché − Coût réel. */
    @Test
    void r12_ecart_ordreDesColonnes_coutReelMoinsMarche() throws Exception {
        Reponse r = lire(COLONNE1, COUT_REEL, COLONNE2, MARCHE);

        SoftAssertions s = new SoftAssertions();
        ecart(s, r, enfant1, "-120");
        ecart(s, r, racine, "-200");
        s.assertAll();
    }

    /** TTC : Marché 960 − estimation 1200 = −240 ; × 0,6 = −144. */
    @Test
    void r12_ecart_ttc_surLesValeursTtc() throws Exception {
        Reponse r = lire(COLONNE1, MARCHE, COLONNE2, ESTIMATION_BASE, TTC, "true");

        SoftAssertions s = new SoftAssertions();
        ecart(s, r, enfant1, "-144");
        ecart(s, r, racine, "-240");
        s.assertAll();
    }

    /** colonne1 seule : l'écart exige les deux ; ecart1 renvoyé tel quel, ecart2 null, écart 0 partout. */
    @Test
    void r12_colonne1Seule_ecartZero_ecart2Nul() throws Exception {
        Reponse r = lire(COLONNE1, MARCHE);

        SoftAssertions s = new SoftAssertions();
        s.assertThat(r.valeur("$.ecart1")).isEqualTo(MARCHE);
        s.assertThat(optionnel(r, "$.ecart2")).isNull();
        ecart(s, r, enfant1, "0");
        ecart(s, r, racine, "0");
        s.assertAll();
    }

    @Test
    void r12_sansColonnes_ecart1Ecart2Nuls_ecartZero() throws Exception {
        Reponse r = lire();

        SoftAssertions s = new SoftAssertions();
        s.assertThat(optionnel(r, "$.ecart1")).isNull();
        s.assertThat(optionnel(r, "$.ecart2")).isNull();
        ecart(s, r, enfant1, "0");
        ecart(s, r, racine, "0");
        s.assertAll();
    }
}

package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.controller.rapport.RapportsProjetSupport.Cadre;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureLotFiche;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureRapportsAchat;
import com.tsix.immocrm.fixtures.FixtureRapportsProjet;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.rapport.AppelsRapports.section;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.RESTE;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.RETENUE;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.codes;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.ligne;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.montant;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.premiere;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.requete;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static com.tsix.immocrm.fixtures.FixtureRapportsProjet.DEC;
import static com.tsix.immocrm.fixtures.FixtureRapportsProjet.PRORATA;
import static com.tsix.immocrm.fixtures.FixtureRapportsProjet.RETENUES;

/** 2e revue N5b-a (règle 6) : chemins que les tests verrouillés ne couvrent pas (FEATURE-n5b-rapports-projet.md). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RapportsProjetRevue2Test {

    private static final String REMBOURSEMENTS = "remboursements";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat achat;
    @Autowired private FixtureRapportsAchat comptes;
    @Autowired private FixtureRapportsProjet fixture;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureLotFiche fiche;

    private AppelsRapports appels;
    private Cadre c;

    @BeforeEach
    void setUp() {
        appels = new AppelsRapports(mockMvc);
        c = RapportsProjetSupport.cadre(achat, comptes, fixture, parite, fiche);
    }

    private UUID lot(UUID projet, String code) {
        UUID lot = parite.lot(projet, c.code(code), ENTREPRENEURS, c.statutMono());
        fixture.fournisseur(lot, c.fournisseur());
        return lot;
    }

    @Test
    void r4_remboursements_lotDUnProjetHorsPerimetre_absent_avecOuSansProjetDemande() throws Exception {
        UUID visible = lot(c.pa1(), "RV");
        UUID cache = lot(c.pb(), "RH");
        fixture.decompte(visible, c.code("RV") + "-D", 1, "0", "0", "50", LocalDate.of(2026, 3, 1));
        fixture.decompte(cache, c.code("RH") + "-D", 1, "0", "0", "60", LocalDate.of(2026, 3, 2));

        List<Object> avecPb = codes(section(appels.sections(requete(PRORATA, c.compte(), c.pa1(), c.pb())), REMBOURSEMENTS));
        List<Object> sansProjet = codes(section(appels.sections(requete(PRORATA, c.compte())), REMBOURSEMENTS));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(avecPb).contains(c.code("RV")).doesNotContain(c.code("RH"));
            s.assertThat(sansProjet).contains(c.code("RV")).doesNotContain(c.code("RH"));
        });
    }

    @Test
    void r1_lotMono_decompteeNulle_factureeNonNulle_presentParLeReste() throws Exception {
        UUID lot = lot(c.pa1(), "MZ");
        fixture.facture(lot, c.code("MZ") + "-F", "80", "0", "0");

        Map<String, Object> l = ligne(premiere(appels.sections(requete(RETENUES, c.compte(), c.pa1()))), c.code("MZ"));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(montant(l, RETENUE)).as("décomptée, mono").isEqualByComparingTo("0");
            s.assertThat(montant(l, RESTE)).as("sur la facturée").isEqualByComparingTo("80");
        });
    }

    @Test
    void r2_lotSansDevis_ratioNonNul_estimeZero_pas500() throws Exception {
        UUID lot = lot(c.pa1(), "SD");
        fixture.marche(lot, "InsuranceDECRatio", new BigDecimal("0.01"));
        fixture.facture(lot, c.code("SD") + "-F", "80", "30", "0");

        Map<String, Object> l = ligne(premiere(appels.sections(requete(DEC, c.compte(), c.pa1()))), c.code("SD"));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(montant(l, "montantCollecte")).isEqualByComparingTo("30");
            s.assertThat(l).containsKey("montantEstime");
            s.assertThat(montant(l, "montantEstime")).as("sans devis retenu : marché initial 0").isEqualByComparingTo("0");
        });
    }
}

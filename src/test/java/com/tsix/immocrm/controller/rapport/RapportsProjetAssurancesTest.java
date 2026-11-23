package com.tsix.immocrm.controller.rapport;

import com.tsix.immocrm.controller.rapport.RapportsProjetSupport.Cadre;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureLotFiche;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureRapportsAchat;
import com.tsix.immocrm.fixtures.FixtureRapportsProjet;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

import static com.tsix.immocrm.controller.rapport.AppelsRapports.lignes;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.nombre;
import static com.tsix.immocrm.controller.rapport.AppelsRapports.totaux;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.codes;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.ligne;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.montant;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.premiere;
import static com.tsix.immocrm.controller.rapport.RapportsProjetSupport.requete;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * R2 {@code PRJInsuranceDECStatus} et R3 {@code PRJInsuranceTRCStatus} (FEATURE-n5b-rapports-projet.md), identiques à DEC / TRC
 * près : chaque cas tourne sur les deux rapports. Facture : RG 80, DEC 30, TRC 20 ; devis retenu 1 200 TTC sans avenant.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RapportsProjetAssurancesTest {

    private static final String COLLECTE = "montantCollecte";
    private static final String ESTIME = "montantEstime";
    private static final String CAS = """
            PRJInsuranceDECStatus, InsuranceDECRatio, InsuranceTRCRatio, 30
            PRJInsuranceTRCStatus, InsuranceTRCRatio, InsuranceDECRatio, 20""";

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

    /** Lot du projet PA1 : ratio {@code ratio} sur {@code colonneRatio} ; {@code facture} : RG 80, DEC 30, TRC 20 sinon rien. */
    private UUID lot(String code, String colonneRatio, String ratio, boolean facture) {
        UUID lot = parite.lot(c.pa1(), c.code(code), ENTREPRENEURS, c.statutMono());
        fixture.fournisseur(lot, c.fournisseur());
        fiche.devisRetenu(lot, c.code(code) + "-Q", "1000", "1200");
        fixture.marche(lot, colonneRatio, new BigDecimal(ratio));
        if (facture) {
            fixture.decompte(lot, c.code(code) + "-D", 1, "80", "0", "0", LocalDate.of(2026, 3, 5));
            fixture.facture(lot, c.code(code) + "-F", "80", "30", "20");
        }
        return lot;
    }

    private Map<String, Object> section(String rapport) throws Exception {
        return premiere(appels.sections(requete(rapport, c.compte(), c.pa1())));
    }

    @ParameterizedTest
    @CsvSource(textBlock = CAS)
    void r2r3_lotARatioEtRetenue_collecteFactureEtEstimeMarcheInitialFoisTaux(String rapport, String ratio,
                                                                              @SuppressWarnings("unused") String autre,
                                                                              String collecte) throws Exception {
        lot("LD", ratio, "0.01", true);

        Map<String, Object> l = ligne(section(rapport), c.code("LD"));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(montant(l, COLLECTE)).as("Σ assurance des factures").isEqualByComparingTo(collecte);
            s.assertThat(montant(l, ESTIME)).as("devis 1 200 TTC × 1 %").isEqualByComparingTo("12");
            s.assertThat(l).containsEntry("fournisseur", c.nom());
            s.assertThat(l).containsEntry("projet", "PA1-" + c.jeton());
            s.assertThat(l).containsEntry("description", "Lot " + c.code("LD"));
        });
    }

    @ParameterizedTest
    @CsvSource(textBlock = CAS)
    void r2r3_lotARatioSansRetenue_absent(String rapport, String ratio) throws Exception {
        lot("LR", ratio, "0.01", true);
        lot("LN", ratio, "0.01", false);

        assertThat(codes(section(rapport))).contains(c.code("LR")).doesNotContain(c.code("LN"));
    }

    @ParameterizedTest
    @CsvSource(textBlock = CAS)
    void r2r3_lotAvecRetenueMaisRatioNul_absent(String rapport, String ratio, String autre) throws Exception {
        lot("LR", ratio, "0.01", true);
        lot("LO", autre, "0.01", true);

        assertThat(codes(section(rapport))).contains(c.code("LR")).doesNotContain(c.code("LO"));
    }

    @ParameterizedTest
    @CsvSource(textBlock = CAS)
    void r2r3_totaux_sommeDesDeuxMontants(String rapport, String ratio, @SuppressWarnings("unused") String autre,
                                          String collecte) throws Exception {
        lot("T1", ratio, "0.01", true);
        lot("T2", ratio, "0.02", true);

        Map<String, Object> section = section(rapport);
        Map<String, Object> t = totaux(section);
        List<Map<String, Object>> lignes = lignes(section);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(lignes).hasSize(2);
            s.assertThat(nombre(t.get(COLLECTE))).isEqualByComparingTo(new BigDecimal(collecte).multiply(BigDecimal.TWO));
            s.assertThat(nombre(t.get(ESTIME))).as("12 + 24").isEqualByComparingTo("36");
        });
    }
}

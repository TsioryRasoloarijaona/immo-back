package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.AppelsProjet.DECOMPTES;
import static com.tsix.immocrm.controller.projet.AppelsProjet.LOTS;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.A_PAYER;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.EXTRA;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.FLUCTUATIONS;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.RG;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.TTC;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Décomptes : net à payer recalculé et écart avec OMRAN, facture et reste à régler
 * (FEATURE-projet-lots-decomptes.md §2.3, §2.4, §3 règles 5 à 8). Un projet, un lot, un compte habilité sur le projet.
 * <p>Règle 5 se teste par l'API ({@code GET /api/projet/decomptes/{id}}) : le spec ne fixe pas la signature de
 * {@code NetAPayerDecompte}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DecomptesProjetTest {

    private static final String NET = "$.netAPayerRecalcule";
    private static final String ECART = "$.ecartNetAPayer";
    private static final String SIGNIFICATIF = "$.ecartSignificatif";
    private static final String FACTURE_ID = "$.factureId";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureProjetLots fixture;

    private AppelsProjet appels;
    private String jeton;
    private UUID lot;
    private RequestPostProcessor habilite;

    @BeforeEach
    void setUp() {
        appels = new AppelsProjet(mockMvc);
        jeton = FixtureProjetLots.jeton();
        UUID projet = fixture.projet("PD-" + jeton);
        lot = fixture.lot(projet, "LD-" + jeton, fixture.fournisseur("FOU-" + jeton));
        AdmUser compte = fixture.compte("prj.d." + jeton);
        fixture.habiliter(compte, projet);
        habilite = connecte(compte, "Commercial");
    }

    private Reponse fiche(UUID decompte) throws Exception {
        Reponse r = appels.lire(habilite, DECOMPTES + "/" + decompte);
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        return r;
    }

    private static List<Object> liste(Reponse r, String chemin) {
        return r.valeur(chemin) instanceof List<?> l ? List.copyOf(l) : List.of();
    }

    private static Map<String, BigDecimal> montants(String... colonneValeur) {
        Map<String, BigDecimal> m = new HashMap<>();
        for (int i = 0; i + 1 < colonneValeur.length; i += 2) {
            m.put(colonneValeur[i], new BigDecimal(colonneValeur[i + 1]));
        }
        return m;
    }

    // ------------------------------------------------------------------ règle 5 : formule terme à terme

    /** Un seul terme rempli, les autres NULL (= 0), AmountToPay = 0 : le net vaut ± le terme, l'écart son opposé. */
    @ParameterizedTest(name = "r5 {0} compte pour {1}")
    @CsvSource({
            "AmountTTC, 123.45",
            "AdvanceAmount, -123.45",
            "WorkingOutAdvanceAmount, -123.45",
            "WarrantyAmount, -123.45",
            "nonRefundableRetainer, -123.45",
            "extraAmount, 123.45",
            "ProrataRefundAmount, 123.45",
            "StockAmountToPay, 123.45",
            "FluctuationsToPay, 123.45"
    })
    void r5_chaqueTerme_signeDansLeNet_autresNullValentZero(String colonne, String netAttendu) throws Exception {
        UUID d = fixture.decompte(lot, "D5-" + jeton, 1, montants(colonne, "123.45", A_PAYER, "0"));

        Reponse r = fiche(d);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.nombre(NET)).isEqualByComparingTo(netAttendu);
            s.assertThat(r.nombre(ECART)).isEqualByComparingTo(new BigDecimal(netAttendu).negate());
            s.assertThat(r.valeur(SIGNIFICATIF)).isEqualTo(true);
        });
    }

    @Test
    void r5_tousTermesNull_netZero_ecartNul() throws Exception {
        UUID d = fixture.decompte(lot, "D5N-" + jeton, 1, montants(A_PAYER, "0"));

        Reponse r = fiche(d);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.nombre(NET)).isEqualByComparingTo("0");
            s.assertThat(r.nombre(ECART)).isEqualByComparingTo("0");
            s.assertThat(r.valeur(SIGNIFICATIF)).isEqualTo(false);
        });
    }

    @Test
    void r5_tousLesTermes_ensemble() throws Exception {
        // 1000 − 10 − 20 − 100 − 5 + 50 + 3 + 7 + 0,40 = 925,40
        UUID d = fixture.decompte(lot, "D5T-" + jeton, 1, montants(TTC, "1000", FixtureProjetLots.AVANCE, "10",
                FixtureProjetLots.AVANCE_DECOMPTE, "20", RG, "100", FixtureProjetLots.RETENUE_NON_REMBOURSABLE, "5",
                EXTRA, "50", FixtureProjetLots.PRORATA, "3", FixtureProjetLots.STOCK, "7", FLUCTUATIONS, "0.40",
                A_PAYER, "925.40"));

        Reponse r = fiche(d);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.nombre(NET)).isEqualByComparingTo("925.40");
            s.assertThat(r.nombre(ECART)).isEqualByComparingTo("0");
            s.assertThat(r.valeur(SIGNIFICATIF)).isEqualTo(false);
        });
    }

    @ParameterizedTest(name = "r5 arrondi : TTC {0} → net {1}")
    @CsvSource({"100.004, 100.00", "100.006, 100.01"})
    void r5_netArrondiADeuxDecimales(String ttc, String netAttendu) throws Exception {
        UUID d = fixture.decompte(lot, "D5A-" + jeton, 1, montants(TTC, ttc, A_PAYER, netAttendu));

        Reponse r = fiche(d);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.nombre(NET)).isEqualByComparingTo(netAttendu);
            s.assertThat(r.nombre(ECART)).isEqualByComparingTo("0");
        });
    }

    /** Net recalculé = 1 000 ; écart = AmountToPay − net, signé ; significatif si |écart| > 0,02 strictement. */
    @ParameterizedTest(name = "r5 AmountToPay {0} → écart {1}, significatif {2}")
    @CsvSource({
            "1000.00, 0.00, false",
            "1000.02, 0.02, false",
            "999.98, -0.02, false",
            "1000.03, 0.03, true",
            "999.97, -0.03, true",
            "1010.00, 10.00, true",
            "990.00, -10.00, true"
    })
    void r5_ecartSigne_seuilSignificatif(String aPayer, String ecartAttendu, boolean significatif) throws Exception {
        UUID d = fixture.decompte(lot, "D5S-" + jeton, 1, montants(TTC, "1000", A_PAYER, aPayer));

        Reponse r = fiche(d);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.nombre(NET)).isEqualByComparingTo("1000");
            s.assertThat(r.nombre(ECART)).isEqualByComparingTo(ecartAttendu);
            s.assertThat(r.valeur(SIGNIFICATIF)).isEqualTo(significatif);
        });
    }

    // ------------------------------------------------------------------ règle 6 : jeu de trois décomptes

    @Test
    void r6_deuxDecomptesJustes_unFausseDeDix_dansLaListeDuLot() throws Exception {
        UUID d1 = fixture.decompte(lot, "D61-" + jeton, 1, montants(TTC, "12000", RG, "1200", A_PAYER, "10800"));
        UUID d2 = fixture.decompte(lot, "D62-" + jeton, 2, montants(TTC, "6000", RG, "600", EXTRA, "500",
                FLUCTUATIONS, "0.39", A_PAYER, "5900.39"));
        UUID d3 = fixture.decompte(lot, "D63-" + jeton, 3, montants(TTC, "3000", RG, "300", A_PAYER, "2710"));

        Reponse r = appels.lire(habilite, LOTS + "/" + lot + "/decomptes");

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.ids("$[*].id")).containsExactly(d1.toString(), d2.toString(), d3.toString());
            s.assertThat(r.nombre("$[0].netAPayerRecalcule")).isEqualByComparingTo("10800");
            s.assertThat(r.nombre("$[0].ecartNetAPayer")).isEqualByComparingTo("0");
            s.assertThat(r.valeur("$[0].ecartSignificatif")).isEqualTo(false);
            s.assertThat(r.nombre("$[1].netAPayerRecalcule")).isEqualByComparingTo("5900.39");
            s.assertThat(r.nombre("$[1].ecartNetAPayer")).isEqualByComparingTo("0");
            s.assertThat(r.valeur("$[1].ecartSignificatif")).isEqualTo(false);
            s.assertThat(r.nombre("$[2].netAPayerRecalcule")).isEqualByComparingTo("2700");
            s.assertThat(r.nombre("$[2].ecartNetAPayer")).isEqualByComparingTo("10");
            s.assertThat(r.valeur("$[2].ecartSignificatif")).isEqualTo(true);
        });
    }

    @Test
    void r6_decompteFausseDeDix_dansLaListeGlobale() throws Exception {
        UUID d3 = fixture.decompte(lot, "D63-" + jeton, 3, montants(TTC, "3000", RG, "300", A_PAYER, "2710"));

        Reponse r = appels.lire(habilite, DECOMPTES, "size", AppelsProjet.TAILLE);

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        String element = "$.content[?(@.id == '" + d3 + "')]";
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.ids(element + ".id")).containsExactly(d3.toString());
            s.assertThat((List<?>) r.valeur(element + ".ecartNetAPayer")).hasSize(1)
                    .first().satisfies(e -> assertThat(new BigDecimal(String.valueOf(e))).isEqualByComparingTo("10"));
            s.assertThat(liste(r, element + ".ecartSignificatif")).containsExactly(true);
        });
    }

    // ------------------------------------------------------------------ règle 7 : facture par PrjWorkingOut.BuyDocumentId

    @Test
    void r7_factureLueParBuyDocumentIdDuDecompte() throws Exception {
        UUID d = fixture.decompte(lot, "D7-" + jeton, 1, montants(TTC, "1000", A_PAYER, "1000"));
        UUID facture = fixture.facture(d, "F7-" + jeton, new BigDecimal("1000"));

        Reponse r = fiche(d);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.valeur(FACTURE_ID)).isEqualTo(facture.toString());
            s.assertThat(r.valeur("$.facture.id")).isEqualTo(facture.toString());
        });
    }

    @Test
    void r7_pieceQuiPointeLeDecompteSansReciproque_nEstPasSaFacture() throws Exception {
        UUID d = fixture.decompte(lot, "D7S-" + jeton, 1, montants(TTC, "1000", A_PAYER, "1000"));
        fixture.pieceSansReciproque(d, "F7S-" + jeton, new BigDecimal("1000"));

        Reponse r = fiche(d);
        Reponse liste = appels.lire(habilite, LOTS + "/" + lot + "/decomptes");

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.valeur(FACTURE_ID)).isNull();
            s.assertThat(r.valeur("$.facture")).isNull();
            s.assertThat(liste.statut()).isEqualTo(200);
            s.assertThat(liste.ids("$[*].id")).containsExactly(d.toString());
            s.assertThat(liste.valeur("$[0].factureId")).isNull();
        });
    }

    // ------------------------------------------------------------------ règle 8 : reste à régler

    @Test
    void r8_reglementAnnule_exclu_duRegle_maisListeAnnule() throws Exception {
        UUID d = fixture.decompte(lot, "D8-" + jeton, 1, montants(TTC, "1000", A_PAYER, "1000"));
        UUID facture = fixture.facture(d, "F8-" + jeton, new BigDecimal("1000"));
        fixture.reglement(facture, "RG8A-" + jeton, new BigDecimal("600"), "POINTED");
        fixture.reglement(facture, "RG8B-" + jeton, new BigDecimal("300"), "CANCELED");

        Reponse r = fiche(d);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.nombre("$.montantRegle")).isEqualByComparingTo("600");
            s.assertThat(r.nombre("$.resteARegler")).isEqualByComparingTo("400");
            s.assertThat((List<?>) r.valeur("$.reglements")).hasSize(2);
            s.assertThat(liste(r, "$.reglements[?(@.annule == true)].code")).containsExactly("RG8B-" + jeton);
            s.assertThat(liste(r, "$.reglements[?(@.annule == false)].code")).containsExactly("RG8A-" + jeton);
        });
    }

    @Test
    void r8_resteARegler_dansLaListeDuLot() throws Exception {
        UUID d = fixture.decompte(lot, "D8L-" + jeton, 1, montants(TTC, "1000", A_PAYER, "1000"));
        UUID facture = fixture.facture(d, "F8L-" + jeton, new BigDecimal("1000"));
        fixture.reglement(facture, "RG8C-" + jeton, new BigDecimal("600"), null);
        fixture.reglement(facture, "RG8D-" + jeton, new BigDecimal("300"), "CANCELED");

        Reponse r = appels.lire(habilite, LOTS + "/" + lot + "/decomptes");

        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.nombre("$[0].montantRegle")).isEqualByComparingTo("600");
            s.assertThat(r.nombre("$[0].resteARegler")).isEqualByComparingTo("400");
        });
    }
}

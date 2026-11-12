package com.tsix.immocrm.domain.projet;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.FixtureLotsParite;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Coût prévisionnel du lot (FEATURE-lots-liste-parite.md E1, R1, R2, Q5), lu sur la ligne de {@code GET /api/projet/lots} :
 * lot vide = zéros ; réglé = Σ {@code StlSettlementLine.Amount} des pièces {@code BuyDocument.PrjMarketId} = lot, règlements
 * annulés compris (définition de {@code AmountSettled} dans la vue) ; reste = net à payer − réglé.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CoutPrevisionnelLotTest {

    private static final String HT = "coutPrevisionnelHT";
    private static final String TTC = "coutPrevisionnelTTC";
    private static final String NET = "coutPrevisionnelNetAPayer";
    private static final String REGLE = "coutPrevisionnelRegle";
    private static final String RESTE = "coutPrevisionnelReste";
    private static final String AVANCEMENT = "avancement";
    private static final String CANCELED = "CANCELED";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite fixture;
    @Autowired private FixtureProjetLots lots;

    private String jeton;
    private UUID projet;
    private UUID statut;
    private RequestPostProcessor qui;

    @BeforeEach
    void setUp() {
        jeton = FixtureProjetLots.jeton();
        fixture.genresDeLot();
        projet = fixture.projet("CP-" + jeton, fixture.entiteJuridique("CE-" + jeton));
        statut = fixture.statut(ENTREPRENEURS, "CS-" + jeton);
        qui = connecte(fixture.administrateur("lots.cout." + jeton, projet), "Administrateur");
    }

    private Map<String, Object> ligne(UUID lot, String code) throws Exception {
        String corps = mockMvc.perform(get("/api/projet/lots").with(qui).param("search", code).param("size", "500"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<Map<String, Object>> trouvees = JsonPath.read(corps, "$.content[?(@.id == '" + lot + "')]");
        assertThat(trouvees).as(corps).hasSize(1);
        return trouvees.get(0);
    }

    private static BigDecimal montant(Map<String, Object> ligne, String champ) {
        Object v = ligne.get(champ);
        return v == null ? null : new BigDecimal(String.valueOf(v));
    }

    private UUID lot(String code) {
        return fixture.lot(projet, code + "-" + jeton, ENTREPRENEURS, statut);
    }

    @Test
    void r1_lotSansDecompteNiPiece_tousLesMontantsNuls() throws Exception {
        UUID vide = lot("CV");

        Map<String, Object> l = ligne(vide, "CV-" + jeton);

        SoftAssertions.assertSoftly(s -> {
            for (String champ : List.of(HT, TTC, NET, REGLE, RESTE, AVANCEMENT)) {
                s.assertThat(montant(l, champ)).as(champ).isNotNull().isEqualByComparingTo("0");
            }
        });
    }

    @Test
    void q5_regle_sommeDesReglementsDesPiecesDuLot_annulesCompris() throws Exception {
        UUID lot = lot("CR");
        UUID piece = fixture.pieceDuLot(lot, "CR-P-" + jeton, new BigDecimal("2000.00"));
        lots.reglement(piece, "CR-R1-" + jeton, new BigDecimal("300.00"), null);
        lots.reglement(piece, "CR-R2-" + jeton, new BigDecimal("50.00"), "POINTED");
        lots.reglement(piece, "CR-R3-" + jeton, new BigDecimal("200.00"), CANCELED);
        UUID autreLot = lot("CO");
        lots.reglement(fixture.pieceDuLot(autreLot, "CO-P-" + jeton, new BigDecimal("999.00")), "CO-R-" + jeton,
                new BigDecimal("999.00"), null);

        assertThat(montant(ligne(lot, "CR-" + jeton), REGLE)).isEqualByComparingTo("550.00");
    }

    @Test
    void r1_regle_factureDeDecompteSansLienPrjMarketId_nonComptee() throws Exception {
        UUID lot = lot("CD");
        UUID decompte = lots.decompte(lot, "CD-D-" + jeton, 1, Map.of(FixtureProjetLots.TTC, new BigDecimal("1200.00"),
                FixtureProjetLots.A_PAYER, new BigDecimal("1200.00")));
        UUID facture = lots.facture(decompte, "CD-F-" + jeton, new BigDecimal("1200.00"));
        lots.reglement(facture, "CD-R-" + jeton, new BigDecimal("400.00"), null);

        assertThat(montant(ligne(lot, "CD-" + jeton), REGLE)).as("la vue ne lit que BuyDocument.PrjMarketId")
                .isEqualByComparingTo("0");
    }

    @Test
    void r2_reste_egalNetAPayerMoinsRegle_avecRetenuesEtReglementAnnule() throws Exception {
        UUID lot = lot("CG");
        lots.decompte(lot, "CG-D-" + jeton, 1, Map.of(FixtureProjetLots.TTC, new BigDecimal("1200.00"),
                FixtureProjetLots.RG, new BigDecimal("120.00"), FixtureProjetLots.A_PAYER, new BigDecimal("1080.00")));
        UUID piece = fixture.pieceDuLot(lot, "CG-P-" + jeton, new BigDecimal("1080.00"));
        lots.reglement(piece, "CG-R1-" + jeton, new BigDecimal("500.00"), null);
        lots.reglement(piece, "CG-R2-" + jeton, new BigDecimal("80.00"), CANCELED);

        Map<String, Object> l = ligne(lot, "CG-" + jeton);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(montant(l, REGLE)).isEqualByComparingTo("580.00");
            s.assertThat(montant(l, NET)).isNotNull();
            s.assertThat(montant(l, RESTE)).isNotNull();
            if (montant(l, NET) != null && montant(l, RESTE) != null) {
                s.assertThat(montant(l, RESTE)).as("R2").isEqualByComparingTo(montant(l, NET).subtract(montant(l, REGLE)));
            }
        });
    }

    @Test
    void r2_reste_egalNetAPayerMoinsRegle_surToutesLesLignes() throws Exception {
        lot("CL1");
        UUID regle = lot("CL2");
        lots.reglement(fixture.pieceDuLot(regle, "CL2-P-" + jeton, new BigDecimal("700.00")), "CL2-R-" + jeton,
                new BigDecimal("700.00"), null);

        String corps = mockMvc.perform(get("/api/projet/lots").with(qui).param("projetIds", projet.toString())
                .param("size", "500")).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<Map<String, Object>> lignes = JsonPath.read(corps, "$.content");

        assertThat(lignes).isNotEmpty().allSatisfy(l -> assertThat(montant(l, RESTE)).as(String.valueOf(l.get("code")))
                .isEqualByComparingTo(montant(l, NET).subtract(montant(l, REGLE))));
    }
}

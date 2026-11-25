package com.tsix.immocrm.controller.projet;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureDecomptes;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureProjetLots;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.AppelsProjet.DECOMPTES;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.A_PAYER;
import static com.tsix.immocrm.fixtures.FixtureProjetLots.TTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Seconde revue (règle 6) de FEATURE-lc5-decompte-avancement.md R3 : invariant ancienne + période = % × quantité du
 * marché avec un coefficient différent de 1 (0,656976 : valeur réelle de yesRemsProd), dans les deux modes du lot.
 * Bordereau A : 10 u ; décompte 2 (dernier) : A = 1 sur 4 antérieurs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DecomptesAvancementSecondeRevueTest {

    private static final String ADMINISTRATEUR = "Administrateur";
    private static final String TVA = "0.2";
    private static final String MODE_EXECUTEE = "ExecutedQuantityBasedInsert";
    private static final String LIGNES = "$.lignes";
    private static final String COEF = "0.656976";
    private static final BigDecimal SIX_DECIMALES = new BigDecimal("0.000001");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureProjetLots fixture;
    @Autowired private FixtureDossierVente vente;
    @Autowired private FixtureDecomptes decomptes;

    private AppelsProjet appels;
    private String jeton;
    private UUID lot;
    private UUID ligneA;
    private UUID d2;
    private RequestPostProcessor admin;

    @BeforeEach
    void setUp() {
        appels = new AppelsProjet(mockMvc);
        jeton = FixtureProjetLots.jeton();
        UUID projet = fixture.projet("PSR-" + jeton);
        lot = fixture.lot(projet, "LSR-" + jeton, fixture.fournisseur("FSR-" + jeton));
        decomptes.poser("PrjMarket", lot, "WarrantyRatio", BigDecimal.ZERO, "AdvanceRatio", BigDecimal.ZERO,
                "ProportionRatio", BigDecimal.ZERO, "InsuranceDECRatio", BigDecimal.ZERO, "InsuranceTRCRatio",
                BigDecimal.ZERO, "InsuranceDECAmount", BigDecimal.ZERO, "InsuranceTRCAmount", BigDecimal.ZERO,
                "StockPileRatio", BigDecimal.ZERO, "DefaultVat", new BigDecimal(TVA), MODE_EXECUTEE, false,
                "ApplyWithholdingTaxOnAdditionalWork", false);
        AdmUser compte = vente.compte("prj.sr.a." + jeton, ADMINISTRATEUR);
        fixture.habiliter(compte, projet);
        admin = connecte(compte, ADMINISTRATEUR);
        ligneA = decomptes.ligneMarche(lot, 1, "10", "100", TVA);
        UUID d1 = decompte("DS1-", 1);
        decomptes.ligne(d1, ligneA, 1, "4", "0", "100", TVA);
        d2 = decompte("DS2-", 2);
        UUID l2 = decomptes.ligne(d2, ligneA, 1, "1", "4", "100", TVA);
        decomptes.poser("PrjWorkingOutLine", l2, "Coefficient", new BigDecimal(COEF));
    }

    private UUID decompte(String code, int numero) {
        UUID d = fixture.decompte(lot, code + jeton, numero,
                Map.of(TTC, new BigDecimal("1000"), A_PAYER, new BigDecimal("1000")));
        decomptes.poser("PrjWorkingOut", d, "WarrantyRatio", BigDecimal.ZERO, "AdvanceRatio", BigDecimal.ZERO,
                "proportionRatio", BigDecimal.ZERO, "insuranceDECRatio", BigDecimal.ZERO, "insuranceTRCRatio",
                BigDecimal.ZERO, "StockPileRatio", BigDecimal.ZERO, "WorkingOutAdvanceAmount", BigDecimal.ZERO,
                "ProrataRefundAmount", BigDecimal.ZERO, "Fluctuations", BigDecimal.ZERO, "FluctuationsOld",
                BigDecimal.ZERO, "StockAmount", BigDecimal.ZERO, "StockAmountOld", BigDecimal.ZERO, "advanceRest",
                BigDecimal.ZERO, "insuranceDECRest", BigDecimal.ZERO, "insuranceTRCRest", BigDecimal.ZERO,
                "InsuranceDECAmountMax", BigDecimal.ZERO, "InsuranceTRCAmountMax", BigDecimal.ZERO,
                "WorkingOutAdvanceAmountRestAtCreation", BigDecimal.ZERO, "AmountRegul", BigDecimal.ZERO,
                "AdvanceBasedOnAmount", false, "WarrantyBasedOnAmount", false, "TRCBasedOnAmount", false,
                "DECBasedOnAmount", false, "ProrataBasedOnAmount", false, "FinalWorkingOut", false);
        return d;
    }

    /** Fiche relue ; si {@code avancement} non nul, la ligne A le reçoit (quantiteSaisie à null). */
    private String corps(String avancement) throws Exception {
        Reponse r = appels.lire(admin, DECOMPTES + "/" + d2);
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        DocumentContext c = JsonPath.parse(r.corps());
        if (avancement != null) {
            List<Map<String, Object>> lignes = c.read(LIGNES);
            lignes.forEach(l -> {
                l.put("quantiteSaisie", null);
                l.put("avancement", new BigDecimal(avancement));
            });
            c.set(LIGNES, lignes);
        }
        return c.jsonString();
    }

    private MockHttpServletResponse enregistrer(String corps) throws Exception {
        decomptes.flush();
        return mockMvc.perform(put(DECOMPTES + "/" + d2).contentType(MediaType.APPLICATION_JSON).content(corps)
                .with(admin)).andReturn().getResponse();
    }

    private BigDecimal cumulEnBase() {
        Map<String, Object> l = decomptes.lignes(d2).getFirst();
        return new BigDecimal(String.valueOf(l.get("QuantityOld"))).add(new BigDecimal(String.valueOf(l.get("Quantity"))));
    }

    private void verifierInvariant(boolean executee, BigDecimal cumulAttendu) throws Exception {
        decomptes.poser("PrjMarket", lot, MODE_EXECUTEE, executee);

        MockHttpServletResponse r = enregistrer(corps("84.5"));

        assertThat(r.getStatus()).as(r.getContentAsString(StandardCharsets.UTF_8)).isEqualTo(200);
        assertThat(cumulEnBase()).isCloseTo(cumulAttendu, within(SIX_DECIMALES));
        MockHttpServletResponse reenregistre = enregistrer(corps(null));
        assertThat(reenregistre.getStatus()).as(reenregistre.getContentAsString(StandardCharsets.UTF_8)).isEqualTo(200);
        assertThat(cumulEnBase()).as("après un second enregistrement sans saisie")
                .isCloseTo(cumulAttendu, within(SIX_DECIMALES));
    }

    @Test
    void r3_modeCumul_coefficientReel_quantiteCumulXCoefMoinsAncienne_stableAuReenregistrement() throws Exception {
        verifierInvariant(false, new BigDecimal("8.45").multiply(new BigDecimal(COEF)));
    }

    @Test
    void r3_modeExecutee_coefficientReel_cumulMoinsAncienneXCoef_stableAuReenregistrement() throws Exception {
        BigDecimal periode = new BigDecimal("4.45").multiply(new BigDecimal(COEF));
        verifierInvariant(true, periode.add(new BigDecimal("4")));
    }

    /** Hors règle écrite : un avancement démesuré dépasse decimal(26,6) ; attendu un refus propre, pas une erreur SQL. */
    @Test
    void avancementDemesure_refus400_pasErreurBase() throws Exception {
        MockHttpServletResponse r = enregistrer(corps("1E+22"));

        assertThat(r.getStatus()).as(r.getContentAsString(StandardCharsets.UTF_8)).isEqualTo(400);
    }
}

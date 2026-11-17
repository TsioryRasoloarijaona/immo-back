package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureGenerationAchat;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsAvoirAchat.*;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ROLE_JWT;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.appeler;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.detail;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.fiche;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.id;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.statut;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.supprimer;
import static com.tsix.immocrm.controller.achat.AppelsGenerationAchat.avecSources;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.LECTURE_SEULE;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.creer;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.decimalDe;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.guidDe;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.modifier;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lot Avoirs (FEATURE-achat-avoirs.md) — modification et net à payer de la facture : PUT d'un avoir (R5, R11), recalcul de
 * {@code AmountToPay} de la facture source à la création, à la modification et à la suppression (R6, D5), avoirs non
 * financiers seulement, sans double soustraction.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PieceAchatAvoirModificationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;
    @Autowired private FixtureGenerationAchat generation;

    private Contexte x;
    private UUID facture;

    @BeforeEach
    void setUp() {
        x = preparer(fixture, generation, "AAM");
        facture = facture(fixture, x, "-FA");
    }

    private MvcResult par(MockHttpServletRequestBuilder requete) throws Exception {
        return appeler(mockMvc, requete, connecte(x.redacteur(), ROLE_JWT));
    }

    private static void assertStatut(MvcResult r, int attendu) throws Exception {
        assertThat(statut(r)).as(corps(r)).isEqualTo(attendu);
    }

    private BigDecimal netDeLaFacture() {
        fixture.oublier();
        return decimalDe(fixture.pieceEnBase(facture), A_PAYER);
    }

    // ------------------------------------------------------------------ R6 création

    /** R6 : création d'un avoir non financier de 300 → facture 1 200 − 300 = 900. */
    @Test
    void r6_creation_avoirNonFinancier_factureRecalculee() throws Exception {
        assertStatut(par(creer(avecSources(corpsSansLigne(x, "250", "50", false), facture))), 201);

        // 1 200 − 300 = 900
        assertThat(netDeLaFacture()).isEqualByComparingTo("900");
    }

    /** R6 / D3 : un avoir financier ne change pas la facture (reste 1 200). */
    @Test
    void r6_creation_avoirFinancier_factureInchangee() throws Exception {
        assertStatut(par(creer(avecSources(corpsSansLigne(x, "250", "50", true), facture))), 201);

        // avoir financier exclu : 1 200
        assertThat(netDeLaFacture()).isEqualByComparingTo("1200");
    }

    /** R6 : deux avoirs non financiers (100 existant, déjà déduit, + 300) → 1 200 − 100 − 300 = 800, sans double soustraction. */
    @Test
    void r6_creation_deuxiemeAvoir_sommeDesAvoirs_sansDoubleSoustraction() throws Exception {
        avoirExistant(fixture, x, facture, false, "100", "100");
        fixture.poser(facture, A_PAYER, new BigDecimal("1100"));

        assertStatut(par(creer(avecSources(corpsSansLigne(x, "250", "50", false), facture))), 201);

        // 1 200 − (100 + 300) = 800
        assertThat(netDeLaFacture()).isEqualByComparingTo("800");
    }

    // ------------------------------------------------------------------ R6 modification

    /** R6 : PUT de l'avoir non financier 300 → 600 : facture 1 200 − 600 = 600 (pas 900 − 600). */
    @Test
    void r6_modification_montantChange_factureRecalculee() throws Exception {
        UUID avoir = avoirExistant(fixture, x, facture, false, "250", "300");
        fixture.poser(facture, A_PAYER, new BigDecimal("900"));

        assertStatut(par(modifier(avoir, corpsSansLigne(x, "500", "100", false))), 200);

        // 1 200 − (500 + 100) = 600
        assertThat(netDeLaFacture()).isEqualByComparingTo("600");
    }

    /** R6 / R11 : PUT qui passe un avoir financier de 300 en non financier → facture 1 200 − 300 = 900. */
    @Test
    void r6_modification_devientNonFinancier_factureReduite() throws Exception {
        UUID avoir = avoirExistant(fixture, x, facture, true, "250", "300");

        assertStatut(par(modifier(avoir, corpsSansLigne(x, "250", "50", false))), 200);

        // 1 200 − 300 = 900
        assertThat(netDeLaFacture()).isEqualByComparingTo("900");
    }

    /** R6 / R11 : PUT qui passe un avoir non financier de 300 en financier → facture revient à 1 200. */
    @Test
    void r6_modification_devientFinancier_factureRetablie() throws Exception {
        UUID avoir = avoirExistant(fixture, x, facture, false, "250", "300");
        fixture.poser(facture, A_PAYER, new BigDecimal("900"));

        assertStatut(par(modifier(avoir, corpsSansLigne(x, "250", "50", true))), 200);

        // avoir devenu financier : 1 200
        assertThat(netDeLaFacture()).isEqualByComparingTo("1200");
    }

    // ------------------------------------------------------------------ R6 suppression

    /** R6 : DELETE /api/achat/documents/{id} d'un avoir non financier de 300 → facture 900 → 1 200. */
    @Test
    void r6_suppression_avoirNonFinancier_factureRetablie() throws Exception {
        UUID avoir = avoirExistant(fixture, x, facture, false, "250", "300");
        fixture.poser(facture, A_PAYER, new BigDecimal("900"));

        assertStatut(par(supprimer(avoir)), 204);

        // plus aucun avoir : 1 200
        assertThat(netDeLaFacture()).isEqualByComparingTo("1200");
    }

    /** R6 : suppression d'un des deux avoirs non financiers (100 et 300) → 1 200 − 100 = 1 100. */
    @Test
    void r6_suppression_unDeDeuxAvoirs_resteLAutre() throws Exception {
        avoirExistant(fixture, x, facture, false, "100", "100");
        UUID avoir = avoirExistant(fixture, x, facture, false, "250", "300");
        fixture.poser(facture, A_PAYER, new BigDecimal("800"));

        assertStatut(par(supprimer(avoir)), 204);

        // 1 200 − 100 = 1 100
        assertThat(netDeLaFacture()).isEqualByComparingTo("1100");
    }

    // ------------------------------------------------------------------ R5 / R11 PUT

    /** R5 / R11 / D9 : PUT sans ligne change avoirFinancier et HT/TVA (400 + 80 = 480), rendus par la fiche. */
    @Test
    void r5_put_sansLigne_changeAvoirFinancierEtMontants() throws Exception {
        UUID avoir = avoirExistant(fixture, x, facture, true, "250", "300");

        assertStatut(par(modifier(avoir, corpsSansLigne(x, "400", "80", false))), 200);

        fixture.oublier();
        Map<String, Object> d = fixture.pieceEnBase(avoir);
        String json = corps(par(fiche(avoir)));
        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(d).containsEntry(FINANCIER, false);
        sa.assertThat(decimalDe(d, "AmountHT")).isEqualByComparingTo("400");
        sa.assertThat(decimalDe(d, "AmountVat")).isEqualByComparingTo("80");
        // 400 + 80 = 480
        sa.assertThat(decimalDe(d, "AmountTTC")).isEqualByComparingTo("480");
        sa.assertThat((Boolean) JsonPath.read(json, "$.avoirFinancier")).isFalse();
        sa.assertAll();
    }

    /** R5 : la facture source, le lot et le code ne changent jamais, même si le corps en propose d'autres. */
    @Test
    void r5_put_factureSourceLotEtCode_jamaisChanges() throws Exception {
        UUID avoir = avoirExistant(fixture, x, facture, true, "250", "300");
        UUID autreFacture = facture(fixture, x, "-FA2");
        UUID lot = fixture.lot("LOT-" + x.c().jeton(), x.c().projet());
        String code = (String) fixture.pieceEnBase(avoir).get("Code");

        MvcResult r = par(modifier(avoir, avecSources(corpsSansLigne(x, "250", "50", true), autreFacture).avec("lotId", lot)));

        fixture.oublier();
        Map<String, Object> d = fixture.pieceEnBase(avoir);
        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(statut(r)).as(corps(r)).isLessThan(500);
        sa.assertThat(guidDe(d, SOURCE)).isEqualTo(id(facture));
        sa.assertThat(d).containsEntry("PrjMarketId", null).containsEntry("Code", code);
        sa.assertAll();
    }

    /** R5 / D4 : avoir dans un statut en lecture seule → 409 « lecture seule », rien modifié. */
    @Test
    void r5_put_avoirLectureSeule_409_rienModifie() throws Exception {
        UUID avoir = avoirExistant(fixture, x, facture, true, "250", "300");
        fixture.poser(avoir, "BuyDocumentStatusId", x.s().valideDt());

        MvcResult r = par(modifier(avoir, corpsSansLigne(x, "400", "80", false).avec("notes", "modifiée")));

        assertStatut(r, 409);
        assertThat(detail(r)).startsWith(LECTURE_SEULE);
        fixture.oublier();
        Map<String, Object> d = fixture.pieceEnBase(avoir);
        SoftAssertions sa = new SoftAssertions();
        sa.assertThat(d).containsEntry(FINANCIER, true);
        sa.assertThat(decimalDe(d, "AmountTTC")).isEqualByComparingTo("300");
        sa.assertAll();
    }

    /** R5 : PUT avec une ligne 2 × 100 à 20 % → totaux des lignes (200 / 40 / 240), HT/TVA saisis ignorés. */
    @Test
    void r5_put_avecLignes_totauxDesLignes() throws Exception {
        UUID avoir = avoirExistant(fixture, x, facture, true, "250", "300");

        assertStatut(par(modifier(avoir, corpsSansLigne(x, "999", "1", true)
                .avec("lignes", List.of(AppelsSaisieAchat.ligne("ART-1", "2", "100", x.s().categorie()))))), 200);

        fixture.oublier();
        Map<String, Object> d = fixture.pieceEnBase(avoir);
        SoftAssertions sa = new SoftAssertions();
        // 2 × 100 = 200 HT
        sa.assertThat(decimalDe(d, "AmountHT")).isEqualByComparingTo("200");
        // 200 × 20 % = 40 ; 200 + 40 = 240
        sa.assertThat(decimalDe(d, "AmountTTC")).isEqualByComparingTo("240");
        sa.assertThat(fixture.nombre("BuyDocumentLine", "BuyDocumentId", avoir)).isEqualTo(1);
        sa.assertAll();
    }
}

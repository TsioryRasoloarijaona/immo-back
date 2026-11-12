package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureAchat.ReglementPointe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ROLE_JWT;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.appeler;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.fiche;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.statut;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * A2b-1, E-B5 (onglet Règlements du BC, pointage), Vérification n° 6. Pour un BC : règlements dont une ligne pointe une
 * pièce de {@code BuyOrderId} = ce BC et ({@code ReferenceType = 'ORDER_ADVANCE'} ou {@code DocType = 'INVOICE'}), un rang
 * par règlement, {@code montant} = {@code StlSettlement.Amount} ; colonnes de pointage recalculées (table de vérité de
 * {@code PointageReglement}, aujourd'hui = date de Casablanca). Hors BC : les six champs sont {@code null}.
 * <pre>
 *   BC ; F1 facture (BuyOrderId = BC) ; D1 avance DOCUMENT_TO_PAY ORDER_ADVANCE (BuyOrderId = BC) ;
 *   BL (BuyOrderId = BC, exclu) ; F2 facture liée par BuyOrderInvoice seulement (exclue, Q-E)
 * </pre>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PieceAchatReglementsBonCommandeTest {

    private static final LocalDate AUJOURDHUI = LocalDate.now(ZoneId.of("Africa/Casablanca"));
    private static final String BUY_ORDER_ID = "BuyOrderId";
    private static final String POINTE = FixtureAchat.POINTE;
    private static final String EN_COURS = FixtureAchat.EN_COURS;

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;

    private Cadre c;
    private AdmUser compte;
    private UUID type;
    private UUID bc;
    private UUID f1;
    private UUID d1;

    @BeforeEach
    void setUp() {
        c = fixture.cadre("ARB");
        compte = fixture.compteHabilite(c, "ARB", ROLE_JWT);
        fixture.sansPersonnel(compte);
        type = fixture.typeReglement("TR-" + c.jeton());
        bc = fixture.piece(c, "-BC", FixtureAchat.BON_COMMANDE);
        f1 = fixture.piece(c, "-F1", FixtureAchat.FACTURE);
        fixture.poser(f1, BUY_ORDER_ID, bc);
        d1 = fixture.piece(c, "-D1", FixtureAchat.DOCUMENT_A_REGLER);
        fixture.poser(d1, BUY_ORDER_ID, bc);
        fixture.poser(d1, "ReferenceType", FixtureAchat.AVANCE_COMMANDE);
    }

    private String code(String suffixe) {
        return c.jeton() + "-" + suffixe;
    }

    /** Règlement d'une ligne sur {@code piece} ; montant de ligne 10, montant du règlement 100, brut 110. */
    private void reglement(String suffixe, UUID piece, String etat, LocalDate pointage, LocalDate echeance) {
        UUID r = fixture.reglement(new ReglementPointe(code(suffixe), new BigDecimal("100"), new BigDecimal("110"), etat,
                LocalDate.of(2026, 5, 4), pointage, echeance), type);
        fixture.ligneReglement(r, piece, BigDecimal.TEN);
    }

    private String ficheDe(UUID piece) throws Exception {
        MvcResult r = appeler(mockMvc, fiche(piece), connecte(compte, ROLE_JWT));
        assertThat(statut(r)).as(corps(r)).isEqualTo(200);
        return corps(r);
    }

    private static Map<String, Object> rang(String json, String code) {
        List<Map<String, Object>> rangs = JsonPath.read(json, "$.reglements[?(@.code == '" + code + "')]");
        assertThat(rangs).as("rangs du règlement %s", code).hasSize(1);
        return rangs.get(0);
    }

    private static String date(LocalDate d) {
        return d == null ? null : d.toString();
    }

    private static BigDecimal nombre(Object valeur) {
        return new BigDecimal(String.valueOf(valeur));
    }

    /** Colonnes de pointage attendues d'un rang : état, date pointage, prévue, reporté, reportée. */
    private static void assertPointage(Map<String, Object> rang, String etat, LocalDate pointage, LocalDate prevue,
                                       boolean reporte, LocalDate reportee) {
        assertThat(rang)
                .containsEntry("etat", etat)
                .containsEntry("datePointage", date(pointage))
                .containsEntry("datePointagePrevue", date(prevue))
                .containsEntry("pointageReporte", reporte)
                .containsEntry("datePointageReportee", date(reportee));
    }

    // ------------------------------------------------------------------ définition des règlements du BC

    @Test
    void eb5_reglementsDuBc_viaFactureEtAvance_exclutBLetBuyOrderInvoice() throws Exception {
        UUID bl = fixture.piece(c, "-BL", FixtureAchat.BON_LIVRAISON);
        fixture.poser(bl, BUY_ORDER_ID, bc);
        UUID f2 = fixture.piece(c, "-F2", FixtureAchat.FACTURE);
        fixture.lierFactureACommande(bc, f2);
        reglement("R-FAC", f1, EN_COURS, null, AUJOURDHUI.plusDays(30));
        reglement("R-AV", d1, EN_COURS, null, AUJOURDHUI.plusDays(30));
        reglement("R-BL", bl, EN_COURS, null, AUJOURDHUI.plusDays(30));
        reglement("R-BOI", f2, EN_COURS, null, AUJOURDHUI.plusDays(30));

        List<String> codes = JsonPath.read(ficheDe(bc), "$.reglements[*].code");

        assertThat(codes).containsExactlyInAnyOrder(code("R-FAC"), code("R-AV"));
    }

    /** Une avance au {@code ReferenceType} autre qu'ORDER_ADVANCE ne compte pas. */
    @Test
    void eb5_documentARegler_sansOrderAdvance_exclu() throws Exception {
        UUID autre = fixture.piece(c, "-D2", FixtureAchat.DOCUMENT_A_REGLER);
        fixture.poser(autre, BUY_ORDER_ID, bc);
        fixture.poser(autre, "ReferenceType", FixtureAchat.GARANTIE_COMMANDE);
        reglement("R-GAR", autre, EN_COURS, null, AUJOURDHUI.plusDays(30));

        List<String> codes = JsonPath.read(ficheDe(bc), "$.reglements[*].code");

        assertThat(codes).isEmpty();
    }

    /** Un rang par règlement : deux lignes (facture + avance du même BC) → un rang, montant = StlSettlement.Amount. */
    @Test
    void eb5_reglementADeuxLignes_unSeulRang_montantDuReglement_montantBrut() throws Exception {
        UUID r = fixture.reglement(new ReglementPointe(code("R2L"), new BigDecimal("80"), new BigDecimal("90"), POINTE,
                LocalDate.of(2026, 5, 4), AUJOURDHUI.minusDays(10), AUJOURDHUI.minusDays(20)), type);
        fixture.ligneReglement(r, f1, new BigDecimal("50"));
        fixture.ligneReglement(r, d1, new BigDecimal("30"));

        Map<String, Object> rang = rang(ficheDe(bc), code("R2L"));

        assertAll(
                () -> assertThat(nombre(rang.get("montant"))).isEqualByComparingTo("80"),
                () -> assertThat(nombre(rang.get("montantBrut"))).isEqualByComparingTo("90"));
    }

    // ------------------------------------------------------------------ PointageReglement : table de vérité

    @Test
    void eb5_pointe_passe_pasReporte_reporteeEgalePrevue() throws Exception {
        LocalDate pointage = AUJOURDHUI.minusDays(10);
        reglement("P-PASSE", f1, POINTE, pointage, AUJOURDHUI.minusDays(20));

        assertPointage(rang(ficheDe(bc), code("P-PASSE")), POINTE, pointage, pointage, false, pointage);
    }

    @Test
    void eb5_pointe_futur_pasReporte_reporteeEgalePrevue() throws Exception {
        LocalDate pointage = AUJOURDHUI.plusDays(5);
        reglement("P-FUTUR", f1, POINTE, pointage, null);

        assertPointage(rang(ficheDe(bc), code("P-FUTUR")), POINTE, pointage, pointage, false, pointage);
    }

    @Test
    void eb5_nonPointe_passe_reporte_reporteeAujourdhui() throws Exception {
        LocalDate pointage = AUJOURDHUI.minusDays(3);
        reglement("N-PASSE", f1, EN_COURS, pointage, AUJOURDHUI.plusDays(30));

        assertPointage(rang(ficheDe(bc), code("N-PASSE")), EN_COURS, pointage, pointage, true, AUJOURDHUI);
    }

    @Test
    void eb5_nonPointe_futur_pasReporte_reporteeEgalePrevue() throws Exception {
        LocalDate pointage = AUJOURDHUI.plusDays(10);
        reglement("N-FUTUR", d1, EN_COURS, pointage, null);

        assertPointage(rang(ficheDe(bc), code("N-FUTUR")), EN_COURS, pointage, pointage, false, pointage);
    }

    /** SettlementDate nulle : prévue = échéance. */
    @Test
    void eb5_settlementDateNulle_prevueEgaleEcheance_passee_reporte() throws Exception {
        LocalDate echeance = AUJOURDHUI.minusDays(5);
        reglement("E-PASSE", f1, EN_COURS, null, echeance);

        assertPointage(rang(ficheDe(bc), code("E-PASSE")), EN_COURS, null, echeance, true, AUJOURDHUI);
    }

    @Test
    void eb5_settlementDateNulle_prevueEgaleEcheance_future_pasReporte() throws Exception {
        LocalDate echeance = AUJOURDHUI.plusDays(7);
        reglement("E-FUTUR", f1, EN_COURS, null, echeance);

        assertPointage(rang(ficheDe(bc), code("E-FUTUR")), EN_COURS, null, echeance, false, echeance);
    }

    /** Prévue = aujourd'hui : « prévue ≤ aujourd'hui » vrai → reporté si non pointé. */
    @Test
    void eb5_prevueAujourdhui_nonPointe_reporte() throws Exception {
        reglement("N-JOUR", f1, EN_COURS, AUJOURDHUI, null);

        assertPointage(rang(ficheDe(bc), code("N-JOUR")), EN_COURS, AUJOURDHUI, AUJOURDHUI, true, AUJOURDHUI);
    }

    // ------------------------------------------------------------------ hors BC : inchangé, champs null

    @Test
    void eb5_horsBc_facture_sixChampsNull_montantDeLaLigne() throws Exception {
        reglement("F-SEUL", f1, POINTE, AUJOURDHUI.minusDays(10), null);

        Map<String, Object> rang = rang(ficheDe(f1), code("F-SEUL"));

        assertAll(
                () -> assertThat(rang).containsEntry("etat", null).containsEntry("datePointage", null)
                        .containsEntry("datePointagePrevue", null).containsEntry("datePointageReportee", null)
                        .containsEntry("montantBrut", null),
                () -> assertThat(rang).containsKey("pointageReporte"),
                () -> assertThat(nombre(rang.get("montant"))).isEqualByComparingTo("10"));
    }
}

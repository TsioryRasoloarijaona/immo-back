package com.tsix.immocrm.controller.projet;

import com.tsix.immocrm.controller.projet.AppelsProjet.Reponse;
import com.tsix.immocrm.fixtures.FixtureDecomptes;
import com.tsix.immocrm.fixtures.FixtureLotFiche;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code GET /api/projet/lots/{id}/suivi} : onglets Suivi et Retenues (FEATURE-lot-fiche-suivi.md E3, E4, R5, R6, R7).
 * Aucun lot de prod n'a d'avoir, d'avenant, de pénalité ni de lot terminé : chaque bloc a sa fixture, montants calculés à la main
 * depuis {@code PrjMarketMonitoringView}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotSuiviRetenuesTest {

    private static final String DECOMPTES = "$.colonnes.decomptes.";
    private static final String FACTURES = "$.colonnes.factures.";
    private static final String AVANCE = "$.avanceMarche.";
    private static final String AVANCE_DECOMPTE = "$.avanceDecompte.";
    private static final String GARANTIE = "$.retenueGarantie.";
    private static final String DROITE_DECOMPTE = "$.retenues.decompte.";
    private static final String DROITE_FACTURE = "$.retenues.facture.";
    private static final String RETENUES = "retenues";
    private static final String BLOCAGE = "blocageRg";
    private static final String PENALITE_AVANCE = "penaliteAvance";
    private static final String COUT = "coutTTC";
    private static final String PRJ_MARKET = "PrjMarket";
    private static final String PRJ_WORKING_OUT = "PrjWorkingOut";
    private static final String BUY_DOCUMENT = "BuyDocument";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureProjetLots lots;
    @Autowired private FixtureLotFiche fiche;
    @Autowired private FixtureDecomptes decomptes;

    private AppelsProjet appels;
    private String jeton;
    private UUID projet;
    private UUID statut;
    private RequestPostProcessor qui;

    @BeforeEach
    void setUp() {
        appels = new AppelsProjet(mockMvc);
        jeton = FixtureProjetLots.jeton();
        parite.genresDeLot();
        fiche.entitesPiecesLot();
        projet = parite.projet("SP-" + jeton, parite.entiteJuridique("SE-" + jeton));
        statut = parite.statut(ENTREPRENEURS, "SS-" + jeton);
        qui = connecte(parite.administrateur("lot.suivi." + jeton, projet), "Administrateur");
    }

    private UUID lot(String code) {
        return parite.lot(projet, code + "-" + jeton, ENTREPRENEURS, statut);
    }

    private Reponse brut(UUID lot) throws Exception {
        return appels.lire(qui, AppelsProjet.LOTS + "/" + lot + "/suivi");
    }

    private Reponse suivi(UUID lot) throws Exception {
        Reponse r = brut(lot);
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        return r;
    }

    private UUID decompte(UUID lot, String code, Map<String, String> montants) {
        Map<String, BigDecimal> m = new HashMap<>();
        montants.forEach((k, v) -> m.put(k, new BigDecimal(v)));
        return lots.decompte(lot, code + "-" + jeton, 1, m);
    }

    private UUID facture(UUID lot, String code, String ht, String ttc) {
        return fiche.factureLot(lot, code + "-" + jeton, ht, ttc);
    }

    private void regler(UUID piece, String code, String montant) {
        lots.reglement(piece, code + "-" + jeton, new BigDecimal(montant), null);
    }

    // ------------------------------------------------------------------ visibilité : lot invisible = 404, témoin 200

    @Test
    void suivi_lotInvisible_404_temoinVisible200() throws Exception {
        UUID visible = lot("SV");
        UUID autreProjet = parite.projet("SX-" + jeton, parite.entiteJuridique("SXE-" + jeton));
        UUID invisible = parite.lot(autreProjet, "SX-" + jeton, ENTREPRENEURS, statut);

        Reponse temoin = brut(visible);
        Reponse refus = brut(invisible);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(temoin.statut()).as("témoin visible").isEqualTo(200);
            s.assertThat(refus.statut()).as("lot hors périmètre").isEqualTo(404);
        });
    }

    // ------------------------------------------------------------------ R5 : reste à amortir = réglée − amortie, négatif conservé

    @Test
    void r5_avanceMarche_regleeZero_amortiePositive_resteNegatif() throws Exception {
        UUID lot = lot("SA");
        fiche.avanceEtSignature(lot, new BigDecimal("0.1"), null);
        fiche.devisRetenu(lot, "SA-Q-" + jeton, "900", "1080");
        fiche.avanceMarche(lot, "SA-AV-" + jeton, "1000");
        UUID f = facture(lot, "SA-F", "800", "960");
        decomptes.poser(BUY_DOCUMENT, f, "AdvanceAmount", new BigDecimal("300"));

        Reponse r = suivi(lot);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.nombre(AVANCE + "taux")).as("taux en %").isEqualByComparingTo("10");
            s.assertThat(r.nombre(AVANCE + "marche")).as("10 % × marché TTC 1 080").isEqualByComparingTo("108");
            s.assertThat(r.nombre(AVANCE + "validee")).isEqualByComparingTo("1000");
            s.assertThat(r.nombre(AVANCE + "reglee")).isEqualByComparingTo("0");
            s.assertThat(r.nombre(AVANCE + "amortie")).isEqualByComparingTo("300");
            s.assertThat(r.nombre(AVANCE + "resteAAmortir")).as("réglée 0 − amortie 300").isEqualByComparingTo("-300");
        });
    }

    @Test
    void r5_avanceDecompte_resteAAmortir_regleeMoinsAmortie() throws Exception {
        UUID lot = lot("SB");
        UUID avance = fiche.avanceDecompte(lot, "SB-AV-" + jeton, "500");
        regler(avance, "SB-R", "100");
        UUID f = facture(lot, "SB-F", "800", "960");
        decomptes.poser(BUY_DOCUMENT, f, "WorkingOutAdvanceAmount", new BigDecimal("250"));

        Reponse r = suivi(lot);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.nombre(AVANCE_DECOMPTE + "validee")).isEqualByComparingTo("500");
            s.assertThat(r.nombre(AVANCE_DECOMPTE + "reglee")).isEqualByComparingTo("100");
            s.assertThat(r.nombre(AVANCE_DECOMPTE + "amortie")).isEqualByComparingTo("250");
            s.assertThat(r.nombre(AVANCE_DECOMPTE + "resteAAmortir")).as("100 − 250").isEqualByComparingTo("-150");
        });
    }

    // ------------------------------------------------------------------ R6 : Total TTC décomptes = TTC + appro + extras + fluctuations

    @Test
    void r6_totalTTCDecomptes_avecApproFluctuationsEtExtras() throws Exception {
        UUID lot = lot("SC");
        UUID d = decompte(lot, "SC-D", Map.of(FixtureProjetLots.TTC, "1200", FixtureProjetLots.STOCK, "60",
                FixtureProjetLots.FLUCTUATIONS, "36"));
        decomptes.poser(PRJ_WORKING_OUT, d, "AmountHT", new BigDecimal("1000"), "AmountVat", new BigDecimal("200"),
                "StockAmountToPayHT", new BigDecimal("50"), "FluctuationsToPayHT", new BigDecimal("30"));
        decomptes.extra(d, "2", "10", "0.2");

        Reponse r = suivi(lot);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.nombre(DECOMPTES + "montantHT")).isEqualByComparingTo("1000");
            s.assertThat(r.nombre(DECOMPTES + "tva")).isEqualByComparingTo("200");
            s.assertThat(r.nombre(DECOMPTES + "montantTTC")).isEqualByComparingTo("1200");
            s.assertThat(r.nombre(DECOMPTES + "approTTC")).isEqualByComparingTo("60");
            s.assertThat(r.nombre(DECOMPTES + "travauxSuppTTC")).as("2 × 10 × 1,2").isEqualByComparingTo("24");
            s.assertThat(r.nombre(DECOMPTES + "fluctuationsTTC")).isEqualByComparingTo("36");
            s.assertThat(r.nombre(DECOMPTES + "depassementsTTC")).isEqualByComparingTo("0");
            s.assertThat(r.nombre(DECOMPTES + "totalTTC")).as("1 200 + 60 + 24 + 36").isEqualByComparingTo("1320");
            s.assertThat(r.nombre(DECOMPTES + COUT)).as("sans avenant").isEqualByComparingTo("1320");
            s.assertThat(r.valeur(DECOMPTES + "regulation")).as("pas de régulation côté décomptes").isNull();
        });
    }

    // ------------------------------------------------------------------ R7 : retenues de la colonne droite = retenues du Suivi

    @Test
    void r7_retenues_colonneDroite_egalesAuSuivi_retenueForfaitaire() throws Exception {
        UUID lot = lot("SD");
        UUID d = decompte(lot, "SD-D", Map.of(FixtureProjetLots.TTC, "1200", FixtureProjetLots.A_PAYER, "1167"));
        decomptes.poser(PRJ_WORKING_OUT, d, "proportionAmount", new BigDecimal("12"), "insuranceDECAmount", new BigDecimal("6"));
        fiche.retenueForfaitaire(d, "15");
        facture(lot, "SD-F", "1000", "1200");

        Reponse r = suivi(lot);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.nombre(DECOMPTES + RETENUES)).as("prorata 12 + DEC 6 + forfaitaire 15").isEqualByComparingTo("33");
            s.assertThat(r.nombre(DROITE_DECOMPTE + RETENUES)).isEqualByComparingTo(r.nombre(DECOMPTES + RETENUES));
            s.assertThat(r.valeur(FACTURES + RETENUES)).as("retenues factures").isNotNull();
            s.assertThat(r.nombre(DROITE_FACTURE + RETENUES)).isEqualByComparingTo(r.nombre(FACTURES + RETENUES));
            s.assertThat(r.nombre(DROITE_DECOMPTE + "retenueForfaitaire")).isEqualByComparingTo("15");
            // la vue lit la retenue forfaitaire des décomptes aussi côté facture (InvoiceCostRetainedLineAmount)
            s.assertThat(r.nombre(DROITE_FACTURE + "retenueForfaitaire")).isEqualByComparingTo("15");
            s.assertThat(r.valeur(DROITE_DECOMPTE + "penaliteCompteProrata")).as("côté décompte : null").isNull();
        });
    }

    // ------------------------------------------------------------------ E3 : colonne Avoirs, champs hors liste = null

    @Test
    void e3_colonneAvoirs_quatreChamps_autresNull() throws Exception {
        UUID lot = lot("SE");
        fiche.avoirLot(lot, "SE-A-" + jeton, "100", "120");

        Reponse r = suivi(lot);
        @SuppressWarnings("unchecked")
        Map<String, Object> avoirs = (Map<String, Object>) r.valeur("$.colonnes.avoirs");

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.nombre("$.colonnes.avoirs.montantHT")).isEqualByComparingTo("100");
            s.assertThat(r.nombre("$.colonnes.avoirs.tva")).isEqualByComparingTo("20");
            s.assertThat(r.nombre("$.colonnes.avoirs.montantTTC")).isEqualByComparingTo("120");
            s.assertThat(avoirs).as("régulation des avoirs").extractingByKey("regulation").isNotNull();
            for (String champ : List.of("approTTC", "depassementsTTC", "travauxSuppTTC", "fluctuationsTTC", "totalTTC",
                    "avenantsTTC", COUT, RETENUES, "remboursement")) {
                s.assertThat(avoirs).as(champ).containsEntry(champ, null);
            }
        });
    }

    // ------------------------------------------------------------------ Question 1 : avenants prévus et facturés

    @Test
    void q1_avenants_prevuCoteDecomptes_factureCoteFactures() throws Exception {
        UUID lot = lot("SF");
        UUID avenant = fiche.avenant(lot, "SF-AV-" + jeton, "500", "100", "600");
        fiche.factureAvenant(lot, avenant, "SF-FA-" + jeton, "250", "300");

        Reponse r = suivi(lot);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.nombre(DECOMPTES + "avenantsTTC")).isEqualByComparingTo("600");
            s.assertThat(r.nombre(DECOMPTES + COUT)).as("0 + avenant 600").isEqualByComparingTo("600");
            s.assertThat(r.nombre(FACTURES + "avenantsTTC")).as("facture d'avenant").isEqualByComparingTo("300");
            s.assertThat(r.nombre(FACTURES + "montantTTC")).as("la facture d'avenant n'est pas une facture de décompte")
                    .isEqualByComparingTo("0");
            s.assertThat(r.nombre(FACTURES + COUT)).isEqualByComparingTo("300");
        });
    }

    // ------------------------------------------------------------------ retenue de garantie : compte prorata géré

    @Test
    void e3_gestionnaireCompteProrata_seulLeLotDesigneParLeProjet() throws Exception {
        UUID gestionnaire = lot("SG");
        UUID autre = lot("SH");
        decomptes.poser("PrjProject", projet, "ManageProrataPrjMarketId", gestionnaire.toString());

        Reponse oui = suivi(gestionnaire);
        Reponse non = suivi(autre);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(oui.valeur(GARANTIE + "gestionnaireCompteProrata")).isEqualTo(true);
            s.assertThat(non.valeur(GARANTIE + "gestionnaireCompteProrata")).isEqualTo(false);
        });
    }

    // ------------------------------------------------------------------ BlokWarrantyRestitution : blocage RG, reste à restituer 0

    private void garantie(UUID lot, String code) {
        decompte(lot, code + "-D", Map.of(FixtureProjetLots.TTC, "1200", FixtureProjetLots.RG, "100", FixtureProjetLots.A_PAYER, "1100"));
        UUID f = facture(lot, code + "-F", "800", "960");
        decomptes.poser(BUY_DOCUMENT, f, "WarrantyAmount", new BigDecimal("96"));
    }

    @Test
    void e4_blocageRestitutionGarantie_penaliteBlocage_resteARestituerNul() throws Exception {
        UUID bloque = lot("SI");
        UUID libre = lot("SJ");
        decomptes.poser(PRJ_MARKET, bloque, "BlokWarrantyRestitution", true);
        decomptes.poser(PRJ_MARKET, libre, "BlokWarrantyRestitution", false);
        garantie(bloque, "SI");
        garantie(libre, "SJ");

        Reponse b = suivi(bloque);
        Reponse l = suivi(libre);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(b.nombre(GARANTIE + "retenue")).as("RG des factures").isEqualByComparingTo("96");
            s.assertThat(b.nombre(GARANTIE + "resteARestituer")).as("bloqué").isEqualByComparingTo("0");
            s.assertThat(b.nombre(DROITE_FACTURE + BLOCAGE)).as("RG facturée − validée").isEqualByComparingTo("96");
            s.assertThat(b.nombre(DROITE_DECOMPTE + BLOCAGE)).as("RG décomptée − validée").isEqualByComparingTo("100");
            s.assertThat(l.nombre(GARANTIE + "resteARestituer")).as("non bloqué").isEqualByComparingTo("96");
            s.assertThat(l.nombre(DROITE_FACTURE + BLOCAGE)).isEqualByComparingTo("0");
            s.assertThat(l.nombre(DROITE_DECOMPTE + BLOCAGE)).isEqualByComparingTo("0");
        });
    }

    // ------------------------------------------------------------------ pénalité d'avance : seulement sur un lot terminé

    /** Avances réglées 400 + 100 ; amorties 150 + 50 sur décomptes, 200 + 20 sur factures. */
    private void avancesAmorties(UUID lot, String code) {
        regler(fiche.avanceMarche(lot, code + "-AM-" + jeton, "1000"), code + "-RAM", "400");
        regler(fiche.avanceDecompte(lot, code + "-AD-" + jeton, "500"), code + "-RAD", "100");
        decompte(lot, code + "-D", Map.of(FixtureProjetLots.TTC, "1200", FixtureProjetLots.AVANCE, "150",
                FixtureProjetLots.AVANCE_DECOMPTE, "50", FixtureProjetLots.A_PAYER, "1000"));
        UUID f = facture(lot, code + "-F", "800", "960");
        decomptes.poser(BUY_DOCUMENT, f, "AdvanceAmount", new BigDecimal("200"), "WorkingOutAdvanceAmount", new BigDecimal("20"));
    }

    @Test
    void e4_penaliteAvance_lotTermine_regleesMoinsAmorties_nonTermine_zero() throws Exception {
        UUID statutTermine = parite.statut(ENTREPRENEURS, "ST-" + jeton);
        decomptes.poser("StrStatus", statutTermine, "BoolField002", true);
        UUID termine = parite.lot(projet, "ST-" + jeton, ENTREPRENEURS, statutTermine);
        UUID enCours = lot("SU");
        avancesAmorties(termine, "ST");
        avancesAmorties(enCours, "SU");

        Reponse t = suivi(termine);
        Reponse c = suivi(enCours);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(t.nombre(DROITE_DECOMPTE + PENALITE_AVANCE)).as("400 + 100 − 150 − 50").isEqualByComparingTo("300");
            s.assertThat(t.nombre(DROITE_FACTURE + PENALITE_AVANCE)).as("400 + 100 − 200 − 20").isEqualByComparingTo("280");
            s.assertThat(c.nombre(DROITE_DECOMPTE + PENALITE_AVANCE)).as("non terminé").isEqualByComparingTo("0");
            s.assertThat(c.nombre(DROITE_FACTURE + PENALITE_AVANCE)).as("non terminé").isEqualByComparingTo("0");
        });
    }

    // ------------------------------------------------------------------ E4 : blocs compte prorata / DEC / TRC

    @Test
    void e4_blocsRetenues_tauxEnPourcent_quotePartNullePourLeProrata() throws Exception {
        UUID lot = lot("SK");
        decomptes.poser(PRJ_MARKET, lot, "ProportionRatio", new BigDecimal("0.02"), "InsuranceDECRatio", new BigDecimal("0.01"),
                "InsuranceDECAmount", new BigDecimal("50"), "InsuranceTRCRatio", new BigDecimal("0.005"));

        Reponse r = suivi(lot);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(r.nombre("$.retenues.compteProrata.taux")).isEqualByComparingTo("2");
            s.assertThat(r.valeur("$.retenues.compteProrata.quotePart")).as("pas de quote-part pour le prorata").isNull();
            s.assertThat(r.nombre("$.retenues.assuranceDecennale.taux")).isEqualByComparingTo("1");
            s.assertThat(r.nombre("$.retenues.assuranceDecennale.quotePart")).isEqualByComparingTo("50");
            s.assertThat(r.nombre("$.retenues.assuranceTrc.taux")).isEqualByComparingTo("0.5");
        });
    }
}

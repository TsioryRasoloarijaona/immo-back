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
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.projet.LotsPariteSupport.nombre;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.ENTREPRENEURS;
import static com.tsix.immocrm.fixtures.FixtureLotsParite.FOURNISSEURS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Onglet Général de la fiche lot et avancements (FEATURE-lot-fiche-suivi.md E2, R3, R4, Corrections 1 et 3, Question 1).
 * Montants attendus calculés à la main depuis {@code PrjMarketMonitoringView} : bordereau 10 × 100 HT, TVA 20 % = 1 000 / 1 200 ;
 * « Coût total » = {@code CostHT/Vat/TTC} (avenants compris) ; lot terminé = coûts lus sur les factures − avoirs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LotGeneralMontantsTest {

    private static final String PREVU = "$.general.coutPrevisionnel.";
    private static final String REEL = "$.general.coutReel.";
    private static final String HT = "ht";
    private static final String TVA = "tva";
    private static final String TTC = "ttc";
    private static final String NET = "netAPayer";
    private static final String REGLE = "regle";
    private static final String RESTE = "reste";
    private static final String TOTAL = "total.";
    private static final String LISTE_HT = "coutPrevisionnelHT";
    private static final String LISTE_TTC = "coutPrevisionnelTTC";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureLotsParite parite;
    @Autowired private FixtureProjetLots lots;
    @Autowired private FixtureLotFiche fiche;
    @Autowired private FixtureDecomptes decomptes;

    private AppelsProjet appels;
    private LotsPariteSupport liste;
    private String jeton;
    private UUID projet;
    private UUID statut;
    private RequestPostProcessor qui;

    @BeforeEach
    void setUp() {
        appels = new AppelsProjet(mockMvc);
        liste = new LotsPariteSupport(mockMvc);
        jeton = FixtureProjetLots.jeton();
        parite.genresDeLot();
        fiche.entitesPiecesLot();
        projet = parite.projet("GP-" + jeton, parite.entiteJuridique("GE-" + jeton));
        statut = parite.statut(ENTREPRENEURS, "GS-" + jeton);
        qui = connecte(parite.administrateur("lot.gen." + jeton, projet), "Administrateur");
    }

    private UUID lot(String code) {
        return parite.lot(projet, code + "-" + jeton, ENTREPRENEURS, statut);
    }

    private Reponse ficheLot(UUID lot) throws Exception {
        Reponse r = appels.lire(qui, AppelsProjet.LOTS + "/" + lot);
        assertThat(r.statut()).as(r.corps()).isEqualTo(200);
        return r;
    }

    private Map<String, Object> ligne(UUID lot, String code) throws Exception {
        return liste.ligne(qui, lot, code + "-" + jeton);
    }

    private void bordereau(UUID lot) {
        decomptes.ligneMarche(lot, 1, "10", "100", "0.2");
    }

    private static Map<String, BigDecimal> decompte(String ttc, String rg, String aPayer) {
        return Map.of(FixtureProjetLots.TTC, new BigDecimal(ttc), FixtureProjetLots.RG, new BigDecimal(rg),
                FixtureProjetLots.A_PAYER, new BigDecimal(aPayer));
    }

    /** Décompte 1 200 TTC (RG 120, net 1 080), facture 900 / 1 080, règlements 500 et 80 annulé (réglé = 580, L1 Q5). */
    private void decompteFactureEtReglements(UUID lot, String code) {
        lots.decompte(lot, code + "-D-" + jeton, 1, decompte("1200", "120", "1080"));
        UUID facture = fiche.factureLot(lot, code + "-F-" + jeton, "900", "1080");
        lots.reglement(facture, code + "-R1-" + jeton, new BigDecimal("500"), null);
        lots.reglement(facture, code + "-R2-" + jeton, new BigDecimal("80"), "CANCELED");
    }

    private static void trio(SoftAssertions s, Reponse r, String chemin, String ht, String tva, String ttc) {
        s.assertThat(r.nombre(chemin + HT)).as(chemin + HT).isEqualByComparingTo(ht);
        s.assertThat(r.nombre(chemin + TVA)).as(chemin + TVA).isEqualByComparingTo(tva);
        s.assertThat(r.nombre(chemin + TTC)).as(chemin + TTC).isEqualByComparingTo(ttc);
    }

    // ------------------------------------------------------------------ R3 : net / réglé / reste = ceux de la liste

    @Test
    void r3_netRegleResteEtCoutTotal_egauxALaListe_avecRetenuesEtReglementAnnule() throws Exception {
        UUID lot = lot("G1");
        decompteFactureEtReglements(lot, "G1");

        Reponse f = ficheLot(lot);
        Map<String, Object> l = ligne(lot, "G1");

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(f.nombre(PREVU + REGLE)).as("réglé, annulé compris").isEqualByComparingTo("580");
            s.assertThat(f.nombre(PREVU + NET)).isEqualByComparingTo(nombre(l.get("coutPrevisionnelNetAPayer")));
            s.assertThat(f.nombre(PREVU + REGLE)).isEqualByComparingTo(nombre(l.get("coutPrevisionnelRegle")));
            s.assertThat(f.nombre(PREVU + RESTE)).isEqualByComparingTo(nombre(l.get("coutPrevisionnelReste")));
            s.assertThat(f.nombre(PREVU + TOTAL + HT)).isEqualByComparingTo(nombre(l.get(LISTE_HT)));
            s.assertThat(f.nombre(PREVU + TOTAL + TTC)).isEqualByComparingTo(nombre(l.get(LISTE_TTC)));
        });
    }

    @Test
    void r3_lotSansDecompte_montantsNulsEtEgauxALaListe() throws Exception {
        UUID lot = lot("G0");

        Reponse f = ficheLot(lot);
        Map<String, Object> l = ligne(lot, "G0");

        SoftAssertions.assertSoftly(s -> {
            for (String champ : new String[]{TOTAL + HT, TOTAL + TTC, NET, REGLE, RESTE}) {
                s.assertThat(f.nombre(PREVU + champ)).as(PREVU + champ).isEqualByComparingTo("0");
                s.assertThat(f.nombre(REEL + champ)).as(REEL + champ).isEqualByComparingTo("0");
            }
            s.assertThat(f.nombre(PREVU + NET)).isEqualByComparingTo(nombre(l.get("coutPrevisionnelNetAPayer")));
            s.assertThat(f.nombre("$.avancementFacture")).isEqualByComparingTo("0");
            s.assertThat(f.nombre("$.avancementDecompte")).isEqualByComparingTo("0");
        });
    }

    // ------------------------------------------------------------------ R4 : reste = net à payer − réglé, deux colonnes

    @Test
    void r4_reste_egalNetAPayerMoinsRegle_previsionnelEtReel() throws Exception {
        UUID lot = lot("G4");
        bordereau(lot);
        decompteFactureEtReglements(lot, "G4");

        Reponse f = ficheLot(lot);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(f.nombre(PREVU + RESTE)).as("prévisionnel")
                    .isEqualByComparingTo(f.nombre(PREVU + NET).subtract(f.nombre(PREVU + REGLE)));
            s.assertThat(f.nombre(REEL + RESTE)).as("réel")
                    .isEqualByComparingTo(f.nombre(REEL + NET).subtract(f.nombre(REEL + REGLE)));
        });
    }

    // ------------------------------------------------------------------ Question 1 : avenants (CostClause*, CostHT/Vat/TTC)

    @Test
    void q1_avenant_ligneAvenantsEtCoutTotal_ficheEtListe() throws Exception {
        UUID avecAvenant = lot("GA");
        UUID sansAvenant = lot("GB");
        bordereau(avecAvenant);
        bordereau(sansAvenant);
        fiche.avenant(avecAvenant, "AV-" + jeton, "500", "100", "600");

        Reponse f = ficheLot(avecAvenant);
        Map<String, Object> l = ligne(avecAvenant, "GA");
        Map<String, Object> temoin = ligne(sansAvenant, "GB");

        SoftAssertions.assertSoftly(s -> {
            trio(s, f, PREVU + "avenants.", "500", "100", "600");
            trio(s, f, PREVU + TOTAL, "1500", "300", "1800");
            s.assertThat(nombre(l.get(LISTE_HT))).as("liste HT, avenant compris").isEqualByComparingTo("1500");
            s.assertThat(nombre(l.get(LISTE_TTC))).as("liste TTC, avenant compris").isEqualByComparingTo("1800");
            s.assertThat(nombre(temoin.get(LISTE_TTC))).as("témoin sans avenant").isEqualByComparingTo("1200");
        });
    }

    // ------------------------------------------------------------------ Question 1 : lot terminé (IsTerminated = 1)

    @Test
    void q1_lotTermine_parStatut_coutPrevisionnelLuSurLesFactures() throws Exception {
        UUID termine = parite.statut(ENTREPRENEURS, "GT-" + jeton);
        decomptes.poser("StrStatus", termine, "BoolField002", true);
        UUID lot = parite.lot(projet, "GT-" + jeton, ENTREPRENEURS, termine);
        bordereau(lot);
        lots.decompte(lot, "GT-D-" + jeton, 1, decompte("1200", "0", "1200"));
        fiche.factureLot(lot, "GT-F-" + jeton, "800", "960");

        Reponse f = ficheLot(lot);
        Map<String, Object> l = ligne(lot, "GT");

        SoftAssertions.assertSoftly(s -> {
            trio(s, f, PREVU + TOTAL, "800", "160", "960");
            trio(s, f, REEL + TOTAL, "800", "160", "960");
            s.assertThat(nombre(l.get(LISTE_HT))).as("liste HT, lot terminé").isEqualByComparingTo("800");
            s.assertThat(nombre(l.get(LISTE_TTC))).as("liste TTC, lot terminé").isEqualByComparingTo("960");
        });
    }

    @Test
    void q1_lotMultiFournisseurClos_termine_coutPrevisionnelLuSurLesFactures() throws Exception {
        UUID statutFournisseurs = parite.statut(FOURNISSEURS, "GMS-" + jeton);
        UUID lot = parite.lot(projet, "GM-" + jeton, FOURNISSEURS, statutFournisseurs);
        fiche.clore(lot);
        fiche.factureLot(lot, "GM-F-" + jeton, "800", "960");

        Reponse f = ficheLot(lot);
        Map<String, Object> l = ligne(lot, "GM");

        SoftAssertions.assertSoftly(s -> {
            trio(s, f, PREVU + TOTAL, "800", "160", "960");
            trio(s, f, REEL + TOTAL, "800", "160", "960");
            s.assertThat(nombre(l.get(LISTE_TTC))).as("liste TTC = fiche").isEqualByComparingTo("960");
        });
    }

    // ------------------------------------------------------------------ E2 : Coût réel = factures − avoirs

    @Test
    void e2_coutReel_facturesMoinsAvoirs() throws Exception {
        UUID lot = lot("GR");
        bordereau(lot);
        fiche.factureLot(lot, "GR-F-" + jeton, "800", "960");
        fiche.avoirLot(lot, "GR-A-" + jeton, "100", "120");

        Reponse f = ficheLot(lot);

        SoftAssertions.assertSoftly(s -> trio(s, f, REEL + TOTAL, "700", "140", "840"));
    }

    // ------------------------------------------------------------------ E2 : Marché, Marché ajusté

    @Test
    void e2_marche_devisRetenu_marcheAjuste_bordereau() throws Exception {
        UUID lot = lot("GD");
        bordereau(lot);
        fiche.devisRetenu(lot, "GD-Q-" + jeton, "900", "1080");

        Reponse f = ficheLot(lot);

        SoftAssertions.assertSoftly(s -> {
            trio(s, f, "$.general.marche.", "900", "180", "1080");
            trio(s, f, "$.general.marcheAjuste.", "1000", "200", "1200");
        });
    }

    // ------------------------------------------------------------------ Correction 3 : Est. de base / Est. confidentielle

    @Test
    void correction3_estimationBase_forfaitSaisi_estimationConfidentielle_calculee() throws Exception {
        UUID lot = lot("GE");
        decomptes.poser("PrjMarket", lot, "EstimatedPriceHTManual", new BigDecimal("1000"),
                "EstimatedVatAmountManual", new BigDecimal("200"), "EstimatedPriceTTCManual", new BigDecimal("1200"));
        fiche.ligneEstimative(lot, "GE-E-" + jeton, "2", "300");

        Reponse f = ficheLot(lot);

        SoftAssertions.assertSoftly(s -> {
            trio(s, f, "$.general.estimationBase.", "1000", "200", "1200");
            trio(s, f, "$.general.estimationConfidentielle.", "600", "120", "720");
            s.assertThat(f.nombre("$.general.estimationBase.ht")).isEqualByComparingTo(f.nombre("$.montantForfaitaireHT"));
            s.assertThat(f.nombre("$.general.estimationConfidentielle.ht")).isEqualByComparingTo(f.nombre("$.estimationHT"));
            s.assertThat(f.nombre("$.general.estimationConfidentielle.tva")).isEqualByComparingTo(f.nombre("$.estimationTVA"));
            s.assertThat(f.nombre("$.general.estimationConfidentielle.ttc")).isEqualByComparingTo(f.nombre("$.estimationTTC"));
        });
    }

    // ------------------------------------------------------------------ Correction 1 : avancements facturé / décompté

    @Test
    void correction1_avancementFacture_egalALaListe_avancementDecompte_surLesDecomptes() throws Exception {
        UUID lot = lot("GV");
        bordereau(lot);
        UUID d = lots.decompte(lot, "GV-D-" + jeton, 1, decompte("480", "0", "480"));
        decomptes.poser("PrjWorkingOut", d, "AmountHT", new BigDecimal("400"));
        fiche.factureLot(lot, "GV-F-" + jeton, "250", "300");

        Reponse f = ficheLot(lot);
        Map<String, Object> l = ligne(lot, "GV");

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(f.nombre("$.avancementDecompte")).as("(Σ HT décomptes) / bordereau HT").isEqualByComparingTo("0.4");
            s.assertThat(f.nombre("$.avancementFacture")).as("(Σ HT factures) / bordereau HT").isEqualByComparingTo("0.25");
            s.assertThat(f.nombre("$.avancementFacture")).as("= avancement de la liste")
                    .isEqualByComparingTo(nombre(l.get("avancement")));
        });
    }

    // ------------------------------------------------------------------ visibilité V1 : lot invisible = 404

    @Test
    void lotInvisible_404_temoinVisiblePorteGeneral() throws Exception {
        UUID visible = lot("GW");
        UUID autreProjet = parite.projet("GX-" + jeton, parite.entiteJuridique("GXE-" + jeton));
        UUID invisible = parite.lot(autreProjet, "GX-" + jeton, ENTREPRENEURS, statut);

        Reponse temoin = appels.lire(qui, AppelsProjet.LOTS + "/" + visible);
        Reponse refus = appels.lire(qui, AppelsProjet.LOTS + "/" + invisible);

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(temoin.statut()).as("témoin").isEqualTo(200);
            s.assertThat(temoin.corps()).as("témoin porte general").contains("\"general\"");
            s.assertThat(refus.statut()).isEqualTo(404);
        });
    }
}

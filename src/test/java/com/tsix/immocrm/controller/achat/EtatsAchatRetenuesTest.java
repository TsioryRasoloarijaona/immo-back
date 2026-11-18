package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureEtatsAchat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.PIECE_ID;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.RETENUES;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.annee;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.lignesDe;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.nombre;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureEtatsAchat.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Suivi des retenues de garantie — FEATURE-n7-etats-achat.md R6 a/b, R7, D4, R9, filtre lot.
 * <p>Fixture (2093, une société, un lot) :
 * R1 facture, retenue 250 → retenue 250 ; R2 avoir, retenue 40 → retenue −40 ;
 * R3 nature B602 (restitution), à payer 300, régularisation 20 → pénalité 20, remboursé 300 ;
 * R4 nature A71B4F91, à payer 90 → remboursé 90 ; R5 facture annulée, retenue 999 → listée, hors totaux ;
 * R6 facture sans fournisseur, retenue 60 → visible (R7) ; R8 facture sans retenue ni nature → absente.
 * Totaux : retenue 250 − 40 + 60 = 270 ; pénalité 20 ; remboursé 300 + 90 = 390.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EtatsAchatRetenuesTest {

    private static final String RETENUE = "retenue";
    private static final String PENALITE = "penalite";
    private static final String REMBOURSE = "rembourse";
    private static final String FOURNISSEUR = "fournisseur";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat achat;
    @Autowired private FixtureEtatsAchat fixture;

    private String jeton;
    private UUID societe;
    private UUID projet;
    private UUID lot;
    private UUID fournisseur;
    private String nom;
    private AdmUser compte;
    private UUID r1;
    private UUID r2;
    private UUID r3;
    private UUID r4;
    private UUID r5;
    private UUID r6;
    private UUID r8;

    @BeforeEach
    void setUp() {
        jeton = FixtureAchat.jeton("N7R");
        societe = achat.societe("SOC-" + jeton);
        projet = achat.projet("PRJ-" + jeton, societe);
        lot = achat.lot("LOTRG-" + jeton, projet);
        nom = "Fournisseur RG " + jeton;
        fournisseur = achat.fournisseur("FOU-" + jeton, nom);
        UUID annule = fixture.statutAnnule("ANN-" + jeton);
        compte = fixture.compte("N7R-" + jeton, societe, projet, FixtureEtatsAchat.DROIT_RETENUES);

        r1 = piece("-R1", FixtureAchat.FACTURE, null, "1200");
        fixture.retenue(r1, "250");
        r2 = piece("-R2", FixtureAchat.AVOIR, null, "120");
        fixture.retenue(r2, "40");
        r3 = piece("-R3", FixtureAchat.DOCUMENT_A_REGLER, null, "300");
        fixture.nature(r3, FixtureEtatsAchat.RESTITUTION_MARCHE);
        fixture.regularisation(r3, "20");
        r4 = piece("-R4", FixtureAchat.DOCUMENT_A_REGLER, null, "90");
        fixture.nature(r4, FixtureEtatsAchat.RESTITUTION_MULTI);
        r5 = piece("-R5", FixtureAchat.FACTURE, annule, "1200");
        fixture.retenue(r5, "999");
        r6 = piece("-R6", FixtureAchat.FACTURE, null, "600");
        fixture.retenue(r6, "60");
        achat.effacer(r6, "CfgTierId");
        achat.effacer(r6, "SupplierDescription");
        r8 = piece("-R8", FixtureAchat.FACTURE, null, "480");
    }

    private UUID piece(String suffixe, String nature, UUID statut, String aPayer) {
        UUID id = fixture.piece(jeton + suffixe, nature, date(9, 5), fournisseur, nom, societe, projet, statut, aPayer);
        achat.poser(id, "PrjMarketId", lot);
        return id;
    }

    private RequestPostProcessor qui() {
        return connecte(compte, FixtureEtatsAchat.role());
    }

    private String etat(String... params) throws Exception {
        return corps(mockMvc, annee(RETENUES, qui(), params));
    }

    /** « retenue|penalite|rembourse » d'une ligne, nombres sans zéros de queue. */
    private static String montants(Map<String, Object> l) {
        return texte(nombre(l, RETENUE)) + "|" + texte(nombre(l, PENALITE)) + "|" + texte(nombre(l, REMBOURSE));
    }

    private static String texte(BigDecimal b) {
        return b.stripTrailingZeros().toPlainString();
    }

    @Test
    void r6a_factureAvecRetenue_retenue() throws Exception {
        assertThat(lignesDe(etat(), PIECE_ID, r1)).extracting(EtatsAchatRetenuesTest::montants).containsExactly("250|0|0");
    }

    @Test
    void r6a_avoir_retenueNegative() throws Exception {
        assertThat(lignesDe(etat(), PIECE_ID, r2)).extracting(EtatsAchatRetenuesTest::montants).containsExactly("-40|0|0");
    }

    @Test
    void r6b_restitutionSurMarche_penaliteRegularisation_rembourseAPayer() throws Exception {
        assertThat(lignesDe(etat(), PIECE_ID, r3)).extracting(EtatsAchatRetenuesTest::montants).containsExactly("0|20|300");
    }

    @Test
    void r6b_restitutionMultiFournisseur_comptee() throws Exception {
        assertThat(lignesDe(etat(), PIECE_ID, r4)).extracting(EtatsAchatRetenuesTest::montants).containsExactly("0|0|90");
    }

    @Test
    void r6_pieceSansRetenueNiNature_absente() throws Exception {
        assertThat(lignesDe(etat(), PIECE_ID, r8)).isEmpty();
    }

    @Test
    void r7_pieceSansFournisseur_visible_colonneVide() throws Exception {
        List<Map<String, Object>> lignes = lignesDe(etat(), PIECE_ID, r6);

        assertThat(lignes).singleElement().satisfies(l -> assertAll(
                () -> assertThat(montants(l)).isEqualTo("60|0|0"),
                () -> assertThat(l.get(FOURNISSEUR)).as(FOURNISSEUR).isIn(null, "")));
    }

    @Test
    void d4_pieceAnnulee_listee_annuleeVrai() throws Exception {
        List<Map<String, Object>> lignes = lignesDe(etat(), PIECE_ID, r5);

        assertThat(lignes).singleElement().satisfies(l -> assertThat(l).containsEntry("annulee", true));
    }

    @Test
    void r9_d4_totaux_horsAnnulees() throws Exception {
        String json = etat();

        assertAll(
                () -> assertThat(nombre(json, "$.totaux.retenue")).isEqualByComparingTo("270"),
                () -> assertThat(nombre(json, "$.totaux.penalite")).isEqualByComparingTo("20"),
                () -> assertThat(nombre(json, "$.totaux.rembourse")).isEqualByComparingTo("390"));
    }

    @Test
    void filtreLot_rechercheTexte_neGardeQueLeLot() throws Exception {
        UUID autreLot = achat.lot("AUTRELOT-" + jeton, projet);
        UUID b = fixture.piece(jeton + "-B", FixtureAchat.FACTURE, date(9, 6), fournisseur, nom, societe, projet, null, "60");
        achat.poser(b, "PrjMarketId", autreLot);
        fixture.retenue(b, "10");

        String json = etat("lot", "AUTRELOT-" + jeton);

        assertAll(
                () -> assertThat(lignesDe(json, PIECE_ID, b)).singleElement()
                        .satisfies(l -> assertThat(l).containsEntry("lotId", autreLot.toString())),
                () -> assertThat(lignesDe(json, PIECE_ID, r1)).isEmpty());
    }
}

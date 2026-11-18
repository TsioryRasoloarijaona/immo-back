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
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.AVANCES;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.PIECE_ID;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.annee;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.lignesDe;
import static com.tsix.immocrm.controller.achat.AppelsEtatsAchat.nombre;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static com.tsix.immocrm.fixtures.FixtureEtatsAchat.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Suivi des avances — FEATURE-n7-etats-achat.md R5 a/b, D4, R9, filtres texte.
 * <p>Fixture (2093, une société, un lot) :
 * A1 facture, avance 300 + avance sur décompte 200 → amortissement 500 ;
 * A2 nature B601, à payer 1 000, régularisation 50 → avance 1 000, ajustement 50 ;
 * A3 avoir, avance 100 → amortissement −100 ;
 * A4 nature B603, à payer 200, avance 40 → deux lignes : amortissement 40, puis avance 200 ;
 * A5 nature D35918ED, à payer 80 → avance 80 ; A6 nature C004 annulée, à payer 700 → listée, hors totaux ;
 * A7 facture sans avance ni nature → absente.
 * Totaux : avance 1 000 + 200 + 80 = 1 280 ; ajustement 50 ; amortissement 500 − 100 + 40 = 440.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EtatsAchatAvancesTest {

    private static final String AVANCE = "avance";
    private static final String AJUSTEMENT = "ajustement";
    private static final String AMORTISSEMENT = "amortissement";

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
    private UUID a1;
    private UUID a2;
    private UUID a3;
    private UUID a4;
    private UUID a5;
    private UUID a6;
    private UUID a7;
    private String libelleAvanceMarche;

    @BeforeEach
    void setUp() {
        jeton = FixtureAchat.jeton("N7A");
        societe = achat.societe("SOC-" + jeton);
        projet = achat.projet("PRJ-" + jeton, societe);
        lot = achat.lot("LOT-" + jeton, projet);
        nom = "Fournisseur AVA " + jeton;
        fournisseur = achat.fournisseur("FOU-" + jeton, nom);
        UUID annule = fixture.statutAnnule("ANN-" + jeton);
        compte = fixture.compte("N7A-" + jeton, societe, projet, FixtureEtatsAchat.DROIT_AVANCES);

        a1 = piece("-A1", FixtureAchat.FACTURE, null, "1200");
        fixture.avance(a1, "300", "200");
        a2 = piece("-A2", FixtureAchat.DOCUMENT_A_REGLER, null, "1000");
        libelleAvanceMarche = fixture.nature(a2, FixtureEtatsAchat.AVANCE_MARCHE);
        fixture.regularisation(a2, "50");
        a3 = piece("-A3", FixtureAchat.AVOIR, null, "120");
        fixture.avance(a3, "100", "0");
        a4 = piece("-A4", FixtureAchat.DOCUMENT_A_REGLER, null, "200");
        fixture.nature(a4, FixtureEtatsAchat.AVANCE_DECOMPTE);
        fixture.avance(a4, "40", "0");
        a5 = piece("-A5", FixtureAchat.DOCUMENT_A_REGLER, null, "80");
        fixture.nature(a5, FixtureEtatsAchat.AVANCE_COMMANDE_MULTI);
        a6 = piece("-A6", FixtureAchat.DOCUMENT_A_REGLER, annule, "700");
        fixture.nature(a6, FixtureEtatsAchat.AVANCE_COMMANDE);
        a7 = piece("-A7", FixtureAchat.FACTURE, null, "999");
    }

    private UUID piece(String suffixe, String nature, UUID statut, String aPayer) {
        UUID id = fixture.piece(jeton + suffixe, nature, date(4, 10), fournisseur, nom, societe, projet, statut, aPayer);
        achat.poser(id, "PrjMarketId", lot);
        return id;
    }

    private RequestPostProcessor qui() {
        return connecte(compte, FixtureEtatsAchat.role());
    }

    private String etat(String... params) throws Exception {
        return corps(mockMvc, annee(AVANCES, qui(), params));
    }

    /** « avance|ajustement|amortissement » d'une ligne, nombres sans zéros de queue. */
    private static String montants(Map<String, Object> l) {
        return texte(nombre(l, AVANCE)) + "|" + texte(nombre(l, AJUSTEMENT)) + "|" + texte(nombre(l, AMORTISSEMENT));
    }

    private static String texte(BigDecimal b) {
        return b.stripTrailingZeros().toPlainString();
    }

    @Test
    void r5a_avanceEtAvanceSurDecompte_amortissementSomme() throws Exception {
        assertThat(lignesDe(etat(), PIECE_ID, a1)).extracting(EtatsAchatAvancesTest::montants).containsExactly("0|0|500");
    }

    @Test
    void r5a_avoir_amortissementNegatif() throws Exception {
        assertThat(lignesDe(etat(), PIECE_ID, a3)).extracting(EtatsAchatAvancesTest::montants).containsExactly("0|0|-100");
    }

    @Test
    void r5b_natureAvanceSurMarche_avanceAPayer_ajustementRegularisation() throws Exception {
        assertThat(lignesDe(etat(), PIECE_ID, a2)).extracting(EtatsAchatAvancesTest::montants).containsExactly("1000|50|0");
    }

    @Test
    void r5b_natureAvanceCommandeMultiFournisseur_comptee() throws Exception {
        assertThat(lignesDe(etat(), PIECE_ID, a5)).extracting(EtatsAchatAvancesTest::montants).containsExactly("80|0|0");
    }

    @Test
    void r5_pieceDansLesDeuxBlocs_deuxLignes() throws Exception {
        assertThat(lignesDe(etat(), PIECE_ID, a4)).extracting(EtatsAchatAvancesTest::montants)
                .containsExactlyInAnyOrder("0|0|40", "200|0|0");
    }

    @Test
    void r5_pieceSansAvanceNiNature_absente() throws Exception {
        assertThat(lignesDe(etat(), PIECE_ID, a7)).isEmpty();
    }

    @Test
    void r5_colonnes_natureLibelleStrEntity_lotEtFournisseur() throws Exception {
        List<Map<String, Object>> lignes = lignesDe(etat(), PIECE_ID, a2);

        assertThat(lignes).singleElement().satisfies(l -> assertThat(l)
                .containsEntry("nature", libelleAvanceMarche)
                .containsEntry("code", jeton + "-A2")
                .containsEntry("fournisseur", nom)
                .containsEntry("lotId", lot.toString())
                .containsEntry("annulee", false));
    }

    @Test
    void d4_pieceAnnulee_listee_annuleeVrai() throws Exception {
        List<Map<String, Object>> lignes = lignesDe(etat(), PIECE_ID, a6);

        assertThat(lignes).singleElement().satisfies(l -> assertThat(l).containsEntry("annulee", true));
    }

    @Test
    void r9_d4_totaux_horsAnnulees() throws Exception {
        String json = etat();

        assertAll(
                () -> assertThat(nombre(json, "$.totaux.avance")).isEqualByComparingTo("1280"),
                () -> assertThat(nombre(json, "$.totaux.ajustement")).isEqualByComparingTo("50"),
                () -> assertThat(nombre(json, "$.totaux.amortissement")).isEqualByComparingTo("440"));
    }

    @Test
    void filtreTiers_rechercheTexte_neGardeQueLeFournisseur() throws Exception {
        String autreNom = "Autre Prestataire " + jeton;
        UUID autre = achat.fournisseur("AUT-" + jeton, autreNom);
        UUID b = fixture.piece(jeton + "-B", FixtureAchat.FACTURE, date(4, 11), autre, autreNom, societe, projet, null, "60");
        fixture.avance(b, "10", "0");

        String json = etat("tiers", "Prestataire " + jeton);

        assertAll(
                () -> assertThat(lignesDe(json, PIECE_ID, b)).hasSize(1),
                () -> assertThat(lignesDe(json, PIECE_ID, a1)).isEmpty());
    }
}

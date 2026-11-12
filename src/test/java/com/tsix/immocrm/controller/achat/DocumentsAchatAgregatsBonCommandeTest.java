package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Piece;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsAchat.champ;
import static com.tsix.immocrm.controller.achat.AppelsAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsAchat.liste;
import static com.tsix.immocrm.controller.achat.AppelsAgregats.AGREGATS;
import static com.tsix.immocrm.controller.achat.AppelsAgregats.agregat;
import static com.tsix.immocrm.controller.achat.AppelsAgregats.attendus;
import static com.tsix.immocrm.controller.achat.AppelsAgregats.decouper;
import static com.tsix.immocrm.controller.achat.AppelsAgregats.montantAgregat;
import static com.tsix.immocrm.fixtures.FixtureAchat.AVANCE_COMMANDE;
import static com.tsix.immocrm.fixtures.FixtureAchat.BON_COMMANDE;
import static com.tsix.immocrm.fixtures.FixtureAchat.BON_LIVRAISON;
import static com.tsix.immocrm.fixtures.FixtureAchat.DOCUMENT_A_REGLER;
import static com.tsix.immocrm.fixtures.FixtureAchat.FACTURE;
import static com.tsix.immocrm.fixtures.FixtureAchat.GARANTIE_COMMANDE;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Lot A1 bis — FEATURE-achat-bc-agregats.md E1 et « Cas limites » n° 1 à 8 et 11 (via HTTP) : objet {@code agregats}
 * des lignes de la liste des pièces et de la fiche. Liste sans filtre {@code natures} : pas de restriction « mes BC ».
 * Toutes les pièces liées sont fabriquées : la prod n'a ni BC partiellement facturé, ni avance, ni garantie liée.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentsAchatAgregatsBonCommandeTest {

    private static final String ROLE = "Commercial";
    private static final String BUY_ORDER_ID = "BuyOrderId";
    private static final String REFERENCE_TYPE = "ReferenceType";
    private static final String STATUT = "BuyDocumentStatusId";
    private static final String RESTE_TOTAL = "resteAPayerTotal";
    private static final String RESTE_SUR_FACTURE = "resteAPayerSurFacture";
    private static final String FACTURE_HT = "factureHT";
    private static final String LIVRE_HT = "livreHT";
    private static final String RESTE_A_LIVRER_HT = "resteALivrerHT";
    private static final String REGLE = "regle";
    private static final String FACTURE_NET = "factureNetAPayer";
    private static final String FACTURE_TTC = "factureTTC";
    private static final String FACTURE_TVA = "factureTVA";
    private static final String CODES_BL = "bonsLivraisonCodes";
    private static final String TAUX_EFFECTIF = "avanceTauxEffectif";
    private static final String TAUX_PREVU = "avanceTauxPrevu";
    private static final String DEVIS_CODE = "devisCode";
    private static final LocalDate MARS_10 = LocalDate.of(2026, 3, 10);
    private static final LocalDate MARS_20 = LocalDate.of(2026, 3, 20);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;

    private String jeton;
    private UUID societe;
    private UUID fournisseur;
    private String nom;
    private UUID typeReglement;
    private AdmUser compte;

    @BeforeEach
    void setUp() {
        jeton = FixtureAchat.jeton("ABA");
        societe = fixture.societe("SOC-" + jeton);
        nom = "Fournisseur " + jeton;
        fournisseur = fixture.fournisseur("FOU-" + jeton, nom);
        typeReglement = fixture.typeReglement("VIR-" + jeton);
        compte = fixture.compte("ACH-" + jeton, ROLE);
        fixture.habiliterSociete(compte, societe);
    }

    private RequestPostProcessor qui() {
        return connecte(compte, ROLE);
    }

    private String code(String suffixe) {
        return jeton + suffixe;
    }

    private UUID piece(String suffixe, String nature, String ht, String ttc, String aPayer) {
        UUID id = fixture.piece(Piece.facture(code(suffixe), MARS_10, fournisseur, nom, societe, null,
                new BigDecimal(aPayer)).nature(nature));
        fixture.montants(id, ht, ttc, aPayer);
        return id;
    }

    private UUID bc(String suffixe, String ht, String ttc, String aPayer) {
        return piece(suffixe, BON_COMMANDE, ht, ttc, aPayer);
    }

    /** Pièce liée au BC par {@code BuyOrderId}. */
    private UUID liee(UUID bc, String suffixe, String nature, String ht, String ttc, String aPayer) {
        UUID id = piece(suffixe, nature, ht, ttc, aPayer);
        fixture.poser(id, BUY_ORDER_ID, bc);
        return id;
    }

    private UUID regler(UUID pieceId, String suffixe, String montant, String statut) {
        return fixture.reglement(pieceId, "RG" + suffixe + jeton, new BigDecimal(montant), statut, MARS_20,
                typeReglement);
    }

    private void statutIntermediaire(UUID bc, String suffixe) {
        fixture.poser(bc, STATUT, fixture.statutDEtat("ST" + suffixe + jeton, FixtureAchat.ETAT_INTERMEDIAIRE, null));
    }

    private String json() throws Exception {
        return corps(mockMvc, liste(qui()));
    }

    // ------------------------------------------------------------------ n° 1 : BC sans facture ni BL

    @Test
    void cas1_bcSansPieceLiee_montantsAZero_resteALivrerEgalBc_resteTotalEgalAPayer_codesNuls() throws Exception {
        bc("-NU", "1000", "1200", "1150");
        String c = code("-NU");

        String json = json();

        attendus(json, c, FACTURE_HT, "0", FACTURE_TVA, "0", FACTURE_TTC, "0", FACTURE_NET, "0",
                LIVRE_HT, "0", "livreTTC", "0", "livreNetAPayer", "0",
                RESTE_A_LIVRER_HT, "1000", "resteALivrerTTC", "1200", "resteALivrerNetAPayer", "1150",
                "avance", "0", TAUX_PREVU, "0", TAUX_EFFECTIF, "0", "avanceAmortie", "0",
                "avanceRegle", "0", "avanceReste", "0", "avanceResteAAmortir", "0",
                "garantieRetenue", "0", "garantieRestituee", "0", "garantieReste", "0",
                REGLE, "0", RESTE_SUR_FACTURE, "0", RESTE_TOTAL, "1150");
        assertAll(
                () -> assertThat(agregat(json, c, CODES_BL)).isNull(),
                () -> assertThat(agregat(json, c, "facturesCodes")).isNull(),
                () -> assertThat(agregat(json, c, DEVIS_CODE)).isNull());
    }

    @Test
    void cas1_bcHtZero_tauxEffectifZero_pasDeDivisionParZero() throws Exception {
        bc("-Z0", "0", "0", "0");

        String json = json();

        attendus(json, code("-Z0"), TAUX_EFFECTIF, "0", RESTE_A_LIVRER_HT, "0", RESTE_TOTAL, "0");
    }

    @Test
    void e1_avanceTauxPrevu_egalOrderAdvanceRatio_enFraction() throws Exception {
        UUID bc = bc("-TX", "1000", "1200", "1200");
        fixture.poser(bc, "OrderAdvanceRatio", new BigDecimal("0.25"));

        String json = json();

        attendus(json, code("-TX"), TAUX_PREVU, "0.25");
    }

    // ------------------------------------------------------------------ n° 2 : partiellement facturé

    @Test
    void cas2_uneFactureA40Pourcent_montantsFacturesExacts_aCloturerFaux() throws Exception {
        UUID bc = bc("-P1", "1000", "1200", "1200");
        statutIntermediaire(bc, "P1");
        liee(bc, "-P1-F", FACTURE, "400", "480", "470");
        String c = code("-P1");

        String json = json();

        attendus(json, c, FACTURE_HT, "400", FACTURE_TVA, "80", FACTURE_TTC, "480", FACTURE_NET, "470");
        assertThat(champ(json, c, "aCloturer")).isEqualTo(false);
    }

    @Test
    void cas2_deuxFactures_sommes() throws Exception {
        UUID bc = bc("-P2", "1000", "1200", "1200");
        liee(bc, "-P2-F1", FACTURE, "400", "480", "480");
        liee(bc, "-P2-F2", FACTURE, "200", "240", "230");

        String json = json();

        attendus(json, code("-P2"), FACTURE_HT, "600", FACTURE_TVA, "120", FACTURE_TTC, "720",
                FACTURE_NET, "710");
    }

    // ------------------------------------------------------------------ n° 3 : facturé en totalité

    @Test
    void cas3_factureEnTotalite_regleEnTotalite_restesAZero_aCloturerVrai() throws Exception {
        UUID bc = bc("-T1", "1000", "1200", "1200");
        statutIntermediaire(bc, "T1");
        UUID f = liee(bc, "-T1-F", FACTURE, "1000", "1200", "1200");
        regler(f, "T1", "1200", FixtureAchat.EN_COURS);
        String c = code("-T1");

        String json = json();

        attendus(json, c, FACTURE_HT, "1000", REGLE, "1200", RESTE_SUR_FACTURE, "0", RESTE_TOTAL, "0");
        assertThat(champ(json, c, "aCloturer")).isEqualTo(true);
    }

    @Test
    void cas3_factureEnTotalite_regleAMoitie_restesALaMoitie() throws Exception {
        UUID bc = bc("-T2", "1000", "1200", "1200");
        UUID f = liee(bc, "-T2-F", FACTURE, "1000", "1200", "1200");
        regler(f, "T2", "600", FixtureAchat.EN_COURS);

        String json = json();

        attendus(json, code("-T2"), REGLE, "600", RESTE_SUR_FACTURE, "600", RESTE_TOTAL, "600");
    }

    // ------------------------------------------------------------------ n° 4 : dépassement

    @Test
    void cas4_depassement_sommesBrutesNonPlafonnees_resteTotalNegatifSigne() throws Exception {
        UUID bc = bc("-DP", "50000", "60000", "60000");
        UUID f = liee(bc, "-DP-F", FACTURE, "65000", "78000", "78000");
        regler(f, "DP", "78000", FixtureAchat.EN_COURS);

        String json = json();

        attendus(json, code("-DP"), FACTURE_HT, "65000", FACTURE_TTC, "78000", FACTURE_NET, "78000",
                REGLE, "78000", RESTE_SUR_FACTURE, "0", RESTE_TOTAL, "-18000", RESTE_A_LIVRER_HT, "50000");
    }

    // ------------------------------------------------------------------ n° 5 : règlements

    @Test
    void cas5_reglementAnnuleExcluDuRegle() throws Exception {
        UUID bc = bc("-RA", "1000", "1200", "1200");
        UUID f = liee(bc, "-RA-F", FACTURE, "1000", "1200", "1200");
        regler(f, "RA1", "1200", FixtureAchat.ANNULE);
        regler(f, "RA2", "200", FixtureAchat.EN_COURS);

        String json = json();

        attendus(json, code("-RA"), REGLE, "200", RESTE_SUR_FACTURE, "1000", RESTE_TOTAL, "1000");
    }

    @Test
    void cas5_reglementEnDeuxLignesSurLaMemeFacture_additionne() throws Exception {
        UUID bc = bc("-RL", "1000", "1200", "1200");
        UUID f = liee(bc, "-RL-F", FACTURE, "1000", "1200", "1200");
        UUID reglement = regler(f, "RL", "100", FixtureAchat.EN_COURS);
        fixture.ligneReglement(reglement, f, new BigDecimal("150"));

        String json = json();

        attendus(json, code("-RL"), REGLE, "250", RESTE_SUR_FACTURE, "950", RESTE_TOTAL, "950");
    }

    @Test
    void cas5_factureAnnulee_compteeCommeOmran() throws Exception {
        UUID bc = bc("-FA", "1000", "1200", "1200");
        UUID f = liee(bc, "-FA-F", FACTURE, "300", "360", "360");
        fixture.poser(f, STATUT, fixture.statutDEtat("STFA" + jeton, FixtureAchat.ETAT_ANNULATION, null));

        String json = json();

        attendus(json, code("-FA"), FACTURE_HT, "300", FACTURE_TTC, "360", FACTURE_NET, "360");
    }

    @Test
    void cas5_reglementAnnuleDUneAvance_excluDeAvanceRegle() throws Exception {
        UUID bc = bc("-AA", "1000", "1200", "1200");
        UUID avance = liee(bc, "-AA-AV", DOCUMENT_A_REGLER, "200", "240", "240");
        fixture.poser(avance, REFERENCE_TYPE, AVANCE_COMMANDE);
        regler(avance, "AA1", "240", FixtureAchat.ANNULE);
        regler(avance, "AA2", "40", FixtureAchat.EN_COURS);

        String json = json();

        attendus(json, code("-AA"), "avanceRegle", "40", "avanceReste", "200", REGLE, "40");
    }

    // ------------------------------------------------------------------ n° 6 : livraison

    @Test
    void cas6_deuxBlPartiels_livreAdditionne_resteALivrerZero() throws Exception {
        UUID bc = bc("-L2", "1800", "2160", "2160");
        liee(bc, "-L2-BL1", BON_LIVRAISON, "450", "540", "540");
        liee(bc, "-L2-BL2", BON_LIVRAISON, "1350", "1620", "1620");

        String json = json();

        attendus(json, code("-L2"), LIVRE_HT, "1800", "livreTTC", "2160", "livreNetAPayer", "2160",
                RESTE_A_LIVRER_HT, "0", "resteALivrerTTC", "0", "resteALivrerNetAPayer", "0", FACTURE_HT, "0");
    }

    @Test
    void cas6_blSurDeuxBcDistincts_nonMelanges() throws Exception {
        UUID a = bc("-LA", "1000", "1200", "1200");
        UUID b = bc("-LB", "500", "600", "600");
        liee(a, "-LA-BL", BON_LIVRAISON, "300", "360", "360");
        liee(b, "-LB-BL", BON_LIVRAISON, "100", "120", "120");

        String json = json();

        assertAll(
                () -> assertThat(montantAgregat(json, code("-LA"), LIVRE_HT)).isEqualByComparingTo("300"),
                () -> assertThat(montantAgregat(json, code("-LB"), LIVRE_HT)).isEqualByComparingTo("100"),
                () -> assertThat(montantAgregat(json, code("-LA"), RESTE_A_LIVRER_HT)).isEqualByComparingTo("700"),
                () -> assertThat(montantAgregat(json, code("-LB"), RESTE_A_LIVRER_HT)).isEqualByComparingTo("400"),
                () -> assertThat(agregat(json, code("-LA"), CODES_BL)).isEqualTo(code("-LA-BL")),
                () -> assertThat(agregat(json, code("-LB"), CODES_BL)).isEqualTo(code("-LB-BL")));
    }

    /** Codes triés, séparés par « , » (même séparateur que {@code categories}, A1). */
    @Test
    void cas6_bonsLivraisonCodes_triesSepares() throws Exception {
        UUID bc = bc("-LC", "1000", "1200", "1200");
        liee(bc, "-LC-BL2", BON_LIVRAISON, "100", "120", "120");
        liee(bc, "-LC-BL1", BON_LIVRAISON, "100", "120", "120");

        String json = json();

        assertThat(agregat(json, code("-LC"), CODES_BL))
                .isEqualTo(code("-LC-BL1") + ", " + code("-LC-BL2"));
    }

    @Test
    void cas6_facturesCodes_factureDuBlComprise_sansDoublon_montantsDesSeulesFacturesLiees() throws Exception {
        UUID bc = bc("-FC", "1000", "1200", "1200");
        UUID directe = liee(bc, "-FC-F1", FACTURE, "200", "240", "240");
        UUID duBl = piece("-FC-F2", FACTURE, "300", "360", "360");
        UUID bl1 = liee(bc, "-FC-BL1", BON_LIVRAISON, "200", "240", "240");
        UUID bl2 = liee(bc, "-FC-BL2", BON_LIVRAISON, "300", "360", "360");
        fixture.poser(bl1, "BuyInvoiceId", directe);
        fixture.poser(bl2, "BuyInvoiceId", duBl);
        String c = code("-FC");

        String json = json();

        assertAll(
                () -> assertThat(decouper(agregat(json, c, "facturesCodes")))
                        .containsExactlyInAnyOrder(code("-FC-F1"), code("-FC-F2")),
                () -> assertThat(montantAgregat(json, c, FACTURE_HT)).isEqualByComparingTo("200"));
    }

    @Test
    void e1_devisCode_docRefSinonCode_nullSansDevis() throws Exception {
        UUID avecRef = bc("-DR", "100", "120", "120");
        UUID sansRef = bc("-DC", "100", "120", "120");
        UUID devis1 = piece("-DR-DV", FixtureAchat.DEVIS, "100", "120", "120");
        fixture.poser(devis1, "DocRef", "DEVREF-" + jeton);
        UUID devis2 = piece("-DC-DV", FixtureAchat.DEVIS, "100", "120", "120");
        fixture.poser(devis2, "DocRef", "");
        fixture.poser(avecRef, "BuyQuoteId", devis1);
        fixture.poser(sansRef, "BuyQuoteId", devis2);

        String json = json();

        assertAll(
                () -> assertThat(agregat(json, code("-DR"), DEVIS_CODE)).isEqualTo("DEVREF-" + jeton),
                () -> assertThat(agregat(json, code("-DC"), DEVIS_CODE)).isEqualTo(code("-DC-DV")));
    }

    // ------------------------------------------------------------------ n° 7 : avance et garantie

    @Test
    void cas7_avanceEtGarantie_toutesLesFormules_resteNegatifConserve() throws Exception {
        UUID bc = bc("-AG", "1000", "1200", "1200");
        fixture.poser(bc, "OrderAdvanceRatio", new BigDecimal("0.2"));
        UUID avance = liee(bc, "-AG-AV", DOCUMENT_A_REGLER, "200", "240", "240");
        fixture.poser(avance, REFERENCE_TYPE, AVANCE_COMMANDE);
        regler(avance, "AG", "120", FixtureAchat.EN_COURS);
        UUID f = liee(bc, "-AG-F", FACTURE, "500", "600", "480");
        fixture.poser(f, "AdvanceAmount", new BigDecimal("100"));
        fixture.poser(f, "WarrantyAmount", new BigDecimal("30"));
        UUID restitution = liee(bc, "-AG-RG", DOCUMENT_A_REGLER, "50", "60", "60");
        fixture.poser(restitution, REFERENCE_TYPE, GARANTIE_COMMANDE);

        String json = json();

        attendus(json, code("-AG"), "avance", "240", TAUX_PREVU, "0.2", TAUX_EFFECTIF, "0.2",
                "avanceRegle", "120", "avanceReste", "120", "avanceAmortie", "100", "avanceResteAAmortir", "140",
                "garantieRetenue", "30", "garantieRestituee", "50", "garantieReste", "-20",
                REGLE, "120", RESTE_SUR_FACTURE, "360", RESTE_TOTAL, "1080",
                FACTURE_HT, "500", FACTURE_NET, "480");
    }

    @Test
    void cas7_avance_touteNatureAvecReferenceOrderAdvance() throws Exception {
        UUID bc = bc("-AN", "1000", "1200", "1200");
        UUID avanceFacture = liee(bc, "-AN-AV", FACTURE, "100", "120", "120");
        fixture.poser(avanceFacture, REFERENCE_TYPE, AVANCE_COMMANDE);

        String json = json();

        attendus(json, code("-AN"), "avance", "120", TAUX_EFFECTIF, "0.1");
    }

    // ------------------------------------------------------------------ n° 8 : autres natures

    @ParameterizedTest(name = "nature {0}")
    @ValueSource(strings = {"INVOICE", "QUOTE", "DELIVRY", "DOCUMENT_TO_PAY", "REFUND"})
    void cas8_autreNature_agregatsNul(String nature) throws Exception {
        piece("-AUT", nature, "100", "120", "120");

        String json = json();

        assertThat(champ(json, code("-AUT"), AGREGATS)).isNull();
    }

    @Test
    void cas8_listeToutesNatures_objetSeulementSurLesBc() throws Exception {
        bc("-M-BC", "100", "120", "120");
        piece("-M-F", FACTURE, "100", "120", "120");

        String json = json();

        assertAll(
                () -> assertThat(champ(json, code("-M-BC"), AGREGATS)).isNotNull(),
                () -> assertThat(champ(json, code("-M-F"), AGREGATS)).isNull());
    }

    // ------------------------------------------------------------------ E1 : fiche

    @Test
    void e1_ficheDuBc_memesAgregatsQueLaListe() throws Exception {
        UUID bc = bc("-FI", "1000", "1200", "1200");
        liee(bc, "-FI-F", FACTURE, "400", "480", "480");

        String fiche = corps(mockMvc, get(AppelsAchat.DOCUMENT, bc).with(qui()));

        assertAll(
                () -> assertThat(AppelsImpayes.nombre(fiche, "$.agregats.factureHT")).isEqualByComparingTo("400"),
                () -> assertThat(AppelsImpayes.nombre(fiche, "$.agregats.resteAPayerTotal"))
                        .isEqualByComparingTo("1200"));
    }

    @Test
    void e1_ficheDUneFacture_agregatsNul() throws Exception {
        UUID f = piece("-FIF", FACTURE, "100", "120", "120");

        String fiche = corps(mockMvc, get(AppelsAchat.DOCUMENT, f).with(qui()));

        Map<String, Object> racine = JsonPath.read(fiche, "$");
        assertThat(racine).containsEntry(AGREGATS, null);
    }

    // ------------------------------------------------------------------ n° 11 : arrondi HALF_UP à 2 décimales

    @Test
    void cas11_arrondiHalfUpADeuxDecimales() throws Exception {
        UUID bc = bc("-AR", "100.125", "120.15", "120.15");
        liee(bc, "-AR-F", FACTURE, "40.125", "48.15", "48.15");

        String json = json();

        attendus(json, code("-AR"), FACTURE_HT, "40.13", RESTE_A_LIVRER_HT, "100.13");
    }
}

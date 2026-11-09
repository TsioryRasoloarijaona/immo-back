package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Piece;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsAchat.champ;
import static com.tsix.immocrm.controller.achat.AppelsAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsAchat.liste;
import static com.tsix.immocrm.controller.achat.AppelsAchat.montant;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Règles 2 et 3 (solde par {@code SoldeDocumentAchat}, règlements annulés exclus des sommes) —
 * FEATURE-achat-lecture-factures.md §2, §7 test 4.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentsAchatSoldeTest {

    private static final String ROLE = "Commercial";
    private static final String REGLE = "montantRegle";
    private static final String RESTE = "resteARegler";
    private static final String SOLDE = "solde";
    private static final String A_PAYER = "montantAPayer";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;

    private String jeton;
    private UUID societe;
    private UUID projet;
    private UUID fournisseur;
    private String nom;
    private UUID type;
    private AdmUser compte;

    @BeforeEach
    void setUp() {
        jeton = FixtureAchat.jeton("ASO");
        societe = fixture.societe("SOC-" + jeton);
        projet = fixture.projet("P-" + jeton, societe);
        nom = "Fournisseur " + jeton;
        fournisseur = fixture.fournisseur("FOU-" + jeton, nom);
        type = fixture.typeReglement("VIR-" + jeton);
        compte = fixture.compte("ACH-" + jeton, ROLE);
        fixture.habiliter(compte, projet);
    }

    private RequestPostProcessor qui() {
        return connecte(compte, ROLE);
    }

    private UUID facture(String code, String aPayer) {
        return fixture.piece(Piece.facture(code, LocalDate.of(2026, 4, 1), fournisseur, nom, societe, projet,
                new BigDecimal(aPayer)));
    }

    private void regler(UUID piece, String suffixe, String montantLigne, String statut) {
        fixture.reglement(piece, "RG-" + suffixe + "-" + jeton, new BigDecimal(montantLigne), statut,
                LocalDate.of(2026, 4, 15), type);
    }

    // ------------------------------------------------------------------ §7.4

    @Test
    void r2r3_liste_reglementEnCoursEtAnnule_regleEgaleLaLigneEnCoursSeule_positive() throws Exception {
        String code = jeton + "-F1";
        UUID piece = facture(code, "1000");
        regler(piece, "OK", "300", FixtureAchat.EN_COURS);
        regler(piece, "KO", "200", FixtureAchat.ANNULE);

        String json = corps(mockMvc, liste(qui()));

        assertAll(
                () -> assertThat(montant(json, code, A_PAYER)).isEqualByComparingTo("1000"),
                () -> assertThat(montant(json, code, REGLE)).isEqualByComparingTo("300"),
                () -> assertThat(montant(json, code, RESTE)).isEqualByComparingTo("700"),
                () -> assertThat(champ(json, code, SOLDE)).isEqualTo(false));
    }

    @Test
    void r2r3_fiche_reglementEnCoursEtAnnule_memeSoldeQueLaListe() throws Exception {
        UUID piece = facture(jeton + "-F2", "1000");
        regler(piece, "OK", "300", FixtureAchat.EN_COURS);
        regler(piece, "KO", "200", FixtureAchat.ANNULE);

        String json = corps(mockMvc, get(AppelsAchat.DOCUMENT, piece).with(qui()));

        assertAll(
                () -> assertThat(AppelsAchat.montant(json, "$." + REGLE)).isEqualByComparingTo("300"),
                () -> assertThat(AppelsAchat.montant(json, "$." + RESTE)).isEqualByComparingTo("700"),
                () -> assertThat((Object) JsonPath.read(json, "$." + SOLDE)).isEqualTo(false));
    }

    @Test
    void r2_reglementsPointeEtSansStatut_comptentDansLeRegle() throws Exception {
        String code = jeton + "-F3";
        UUID piece = facture(code, "1000");
        regler(piece, "PT", "400", FixtureAchat.POINTE);
        regler(piece, "NS", "100", null);

        String json = corps(mockMvc, liste(qui()));

        assertAll(
                () -> assertThat(montant(json, code, REGLE)).isEqualByComparingTo("500"),
                () -> assertThat(montant(json, code, RESTE)).isEqualByComparingTo("500"));
    }

    @Test
    void r2_sansReglement_regleZero_resteEgalAPayer() throws Exception {
        String code = jeton + "-F4";
        facture(code, "850");

        String json = corps(mockMvc, liste(qui()));

        assertAll(
                () -> assertThat(montant(json, code, REGLE)).isEqualByComparingTo("0"),
                () -> assertThat(montant(json, code, RESTE)).isEqualByComparingTo("850"),
                () -> assertThat(champ(json, code, SOLDE)).isEqualTo(false));
    }

    @Test
    void r2_solde_auSeuil005_resteDe004EstSolde() throws Exception {
        String code = jeton + "-F5";
        UUID piece = facture(code, "500");
        regler(piece, "OK", "499.96", FixtureAchat.EN_COURS);

        String json = corps(mockMvc, liste(qui()));

        assertAll(
                () -> assertThat(montant(json, code, RESTE)).isEqualByComparingTo("0.04"),
                () -> assertThat(champ(json, code, SOLDE)).isEqualTo(true));
    }

    @Test
    void r2_solde_auSeuil005_resteDe006NEstPasSolde() throws Exception {
        String code = jeton + "-F6";
        UUID piece = facture(code, "500");
        regler(piece, "OK", "499.94", FixtureAchat.EN_COURS);

        String json = corps(mockMvc, liste(qui()));

        assertAll(
                () -> assertThat(montant(json, code, RESTE)).isEqualByComparingTo("0.06"),
                () -> assertThat(champ(json, code, SOLDE)).isEqualTo(false));
    }

    @Test
    void reponse1_tropRegle_resteMoins20_tropRegleVrai_nonSolde() throws Exception {
        String code = jeton + "-F8";
        UUID piece = facture(code, "3000");
        regler(piece, "OK", "3020", FixtureAchat.EN_COURS);

        String json = corps(mockMvc, liste(qui()));

        assertAll(
                () -> assertThat(montant(json, code, RESTE)).isEqualByComparingTo("-20"),
                () -> assertThat(champ(json, code, SOLDE)).isEqualTo(false),
                () -> assertThat(champ(json, code, "tropRegle")).isEqualTo(true));
    }

    @Test
    void reponse1_resteAuSeuil_tropRegleFaux() throws Exception {
        String code = jeton + "-F9";
        UUID piece = facture(code, "500");
        regler(piece, "OK", "500.04", FixtureAchat.EN_COURS);

        String json = corps(mockMvc, liste(qui()));

        assertAll(
                () -> assertThat(champ(json, code, SOLDE)).isEqualTo(true),
                () -> assertThat(champ(json, code, "tropRegle")).isEqualTo(false));
    }

    @Test
    void r2_resteCalculeSurAmountToPay_pasSurLeTTC() throws Exception {
        String code = jeton + "-F7";
        UUID piece = facture(code, "1000");
        fixture.poser(piece, "AmountToPay", new BigDecimal("900"));
        regler(piece, "OK", "100", FixtureAchat.EN_COURS);

        String json = corps(mockMvc, liste(qui()));

        assertAll(
                () -> assertThat(montant(json, code, A_PAYER)).isEqualByComparingTo("900"),
                () -> assertThat(montant(json, code, "montantTTC")).isEqualByComparingTo("1000"),
                () -> assertThat(montant(json, code, RESTE)).isEqualByComparingTo("800"));
    }
}

package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.NON_SOLDES;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.SOLDES;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.appel;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.champ;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.fournisseurs;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.montant;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.nombre;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Solde fournisseurs : regroupement (fournisseur, société), natures, compteurs, filtres, tri, total —
 * FEATURE-achat-impayes.md §3.1, §3.4, §4, §8 test 5.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SoldesFournisseursTest {

    private static final String ROLE = "Commercial";
    private static final String FACTURE = FixtureAchat.FACTURE;
    private static final String A_PAYER = "montantAPayer";
    private static final String REGLE = "montantRegle";
    private static final String RESTE = "resteARegler";
    private static final String NOMBRE_PIECES = "nombrePieces";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;

    private String jeton;
    private UUID societe1;
    private UUID societe2;
    private String soc1;
    private String soc2;
    private UUID projet1;
    private UUID projet2;
    private UUID statutOk;
    private UUID statutEnCours;
    private UUID type;
    private UUID fournisseurX;
    private String nomX;
    private AdmUser compte;

    @BeforeEach
    void setUp() {
        jeton = FixtureAchat.jeton("ASF");
        soc1 = "SOC1-" + jeton;
        soc2 = "SOC2-" + jeton;
        societe1 = fixture.societe(soc1);
        societe2 = fixture.societe(soc2);
        projet1 = fixture.projet("P1-" + jeton, societe1);
        projet2 = fixture.projet("P2-" + jeton, societe2);
        statutOk = fixture.statut("VPP-" + jeton, true, false, 10);
        statutEnCours = fixture.statut("ENC-" + jeton, false, false, 1);
        type = fixture.typeReglement("VIR-" + jeton);
        nomX = "Fournisseur X " + jeton;
        fournisseurX = fixture.fournisseur("FOUX-" + jeton, nomX);
        compte = fixture.compte("ACH-" + jeton, ROLE);
        fixture.habiliterSociete(compte, societe1);
        fixture.habiliterSociete(compte, societe2);
        fixture.habiliter(compte, projet1);
        fixture.habiliter(compte, projet2);
    }

    private RequestPostProcessor qui() {
        return connecte(compte, ROLE);
    }

    private UUID pieceX(String suffixe, String nature, UUID statut, String aPayer) {
        return fixture.piece(jeton + suffixe, nature, fournisseurX, nomX, societe1, projet1, statut, aPayer);
    }

    private void regler(UUID piece, String suffixe, String montant) {
        fixture.reglement(piece, "RG" + suffixe + "-" + jeton, new BigDecimal(montant), FixtureAchat.EN_COURS,
                LocalDate.of(2026, 5, 20), type);
    }

    // ------------------------------------------------------------------ §8.5

    @Test
    void s85_factureDocumentNegatifEtAvoir_ligneSansLAvoir() throws Exception {
        pieceX("-F", FACTURE, statutOk, "100");
        pieceX("-D", FixtureAchat.DOCUMENT_A_REGLER, null, "-40");
        pieceX("-AV", FixtureAchat.AVOIR, null, "30");

        String json = corps(mockMvc, appel(SOLDES, qui()));

        assertAll(
                () -> assertThat(montant(json, fournisseurX, soc1, A_PAYER)).isEqualByComparingTo("60"),
                () -> assertThat(montant(json, fournisseurX, soc1, REGLE)).isEqualByComparingTo("0"),
                () -> assertThat(montant(json, fournisseurX, soc1, RESTE)).isEqualByComparingTo("60"),
                () -> assertThat(montant(json, fournisseurX, soc1, "montantADeduire")).isEqualByComparingTo("-40"),
                () -> assertThat(montant(json, fournisseurX, soc1, NOMBRE_PIECES)).isEqualByComparingTo("2"),
                () -> assertThat(champ(json, fournisseurX, soc1, "solde")).isEqualTo(false),
                () -> assertThat(champ(json, fournisseurX, soc1, "fournisseurNom")).isEqualTo(nomX));
    }

    @Test
    void s85_deuxSocietes_deuxLignes() throws Exception {
        pieceX("-F1", FACTURE, statutOk, "100");
        fixture.piece(jeton + "-F2", FACTURE, fournisseurX, nomX, societe2, projet2, statutOk, "200");

        String json = corps(mockMvc, appel(SOLDES, qui()));

        assertAll(
                () -> assertThat(fournisseurs(json)).containsExactly(fournisseurX.toString(), fournisseurX.toString()),
                () -> assertThat(montant(json, fournisseurX, soc1, A_PAYER)).isEqualByComparingTo("100"),
                () -> assertThat(montant(json, fournisseurX, soc2, A_PAYER)).isEqualByComparingTo("200"));
    }

    @Test
    void s85_nonSoldes_masqueLeFournisseurSolde() throws Exception {
        pieceX("-F", FACTURE, statutOk, "100");
        String nomY = "Fournisseur Y " + jeton;
        UUID y = fixture.fournisseur("FOUY-" + jeton, nomY);
        UUID pieceY = fixture.piece(jeton + "-Y", FACTURE, y, nomY, societe1, projet1, statutOk, "500");
        regler(pieceY, "Y", "500");

        String tous = corps(mockMvc, appel(SOLDES, qui()));
        String nonSoldes = corps(mockMvc, appel(SOLDES, qui(), NON_SOLDES, "true"));

        assertAll(
                () -> assertThat(champ(tous, y, soc1, "solde")).isEqualTo(true),
                () -> assertThat(fournisseurs(nonSoldes)).containsExactly(fournisseurX.toString()));
    }

    // ------------------------------------------------------------------ §4 compteurs de la ligne

    @Test
    void s4_compteurs_validees_aRegler() throws Exception {
        pieceX("-OK", FACTURE, statutOk, "100");
        pieceX("-ENC", FACTURE, statutEnCours, "70");
        UUID soldee = pieceX("-SOL", FACTURE, statutOk, "50");
        regler(soldee, "S", "50");

        String json = corps(mockMvc, appel(SOLDES, qui()));

        assertAll(
                () -> assertThat(montant(json, fournisseurX, soc1, NOMBRE_PIECES)).isEqualByComparingTo("3"),
                () -> assertThat(montant(json, fournisseurX, soc1, "nombreARegler")).isEqualByComparingTo("2"),
                () -> assertThat(montant(json, fournisseurX, soc1, "nombreValideesARegler")).isEqualByComparingTo("1"),
                () -> assertThat(montant(json, fournisseurX, soc1, "resteValideARegler")).isEqualByComparingTo("100"),
                () -> assertThat(montant(json, fournisseurX, soc1, RESTE)).isEqualByComparingTo("170"));
    }

    @Test
    void s31_tropRegle_compteSigneDansLeResteDuFournisseur() throws Exception {
        UUID trop = pieceX("-TRO", FACTURE, statutOk, "3000");
        regler(trop, "T", "3020");
        pieceX("-F", FACTURE, statutOk, "100");

        String json = corps(mockMvc, appel(SOLDES, qui()));

        assertAll(
                () -> assertThat(montant(json, fournisseurX, soc1, RESTE)).isEqualByComparingTo("80"),
                () -> assertThat(montant(json, fournisseurX, soc1, "resteValideARegler")).isEqualByComparingTo("100"));
    }

    // ------------------------------------------------------------------ §4 total, tri, recherche

    @Test
    void s4_total_sommeDesLignes_sansFournisseurNiSociete() throws Exception {
        pieceX("-F", FACTURE, statutOk, "100");
        String nomY = "Fournisseur Y " + jeton;
        UUID y = fixture.fournisseur("FOUY-" + jeton, nomY);
        UUID pieceY = fixture.piece(jeton + "-Y", FACTURE, y, nomY, societe2, projet2, statutOk, "300");
        regler(pieceY, "Y", "100");

        String json = corps(mockMvc, appel(SOLDES, qui()));
        Map<String, Object> total = JsonPath.read(json, "$.total");

        assertAll(
                () -> assertThat(nombre(json, "$.total." + A_PAYER)).isEqualByComparingTo("400"),
                () -> assertThat(nombre(json, "$.total." + REGLE)).isEqualByComparingTo("100"),
                () -> assertThat(nombre(json, "$.total." + RESTE)).isEqualByComparingTo("300"),
                () -> assertThat(nombre(json, "$.total." + NOMBRE_PIECES)).isEqualByComparingTo("2"),
                () -> assertThat(total).extractingByKeys("fournisseurId", "fournisseurNom", "societeCode")
                        .containsOnlyNulls());
    }

    @Test
    void s4_tri_fournisseurNomPuisSocieteCode() throws Exception {
        String nomA = "A-Fourn " + jeton;
        String nomB = "B-Fourn " + jeton;
        UUID a = fixture.fournisseur("FOUA-" + jeton, nomA);
        UUID b = fixture.fournisseur("FOUB-" + jeton, nomB);
        fixture.piece(jeton + "-B1", FACTURE, b, nomB, societe1, projet1, statutOk, "10");
        fixture.piece(jeton + "-A2", FACTURE, a, nomA, societe2, projet2, statutOk, "10");
        fixture.piece(jeton + "-A1", FACTURE, a, nomA, societe1, projet1, statutOk, "10");

        String json = corps(mockMvc, appel(SOLDES, qui()));
        List<String> noms = JsonPath.read(json, "$.lignes[*].fournisseurNom");
        List<String> societes = JsonPath.read(json, "$.lignes[*].societeCode");

        assertAll(
                () -> assertThat(noms).containsExactly(nomA, nomA, nomB),
                () -> assertThat(societes).containsExactly(soc1, soc2, soc1));
    }

    @Test
    void s4_recherche_nomFournisseur_espacesIgnores_casseIgnoree() throws Exception {
        String nomZ = "Fourn Zqx " + jeton;
        UUID z = fixture.fournisseur("FOUZ-" + jeton, nomZ);
        fixture.piece(jeton + "-Z", FACTURE, z, nomZ, societe1, projet1, statutOk, "10");
        pieceX("-X", FACTURE, statutOk, "10");

        String json = corps(mockMvc, appel(SOLDES, qui(), "search", ("fournzqx" + jeton).toLowerCase()));

        assertThat(fournisseurs(json)).containsExactly(z.toString());
    }

    @Test
    void s4_filtreSocieteCodes() throws Exception {
        pieceX("-F1", FACTURE, statutOk, "100");
        fixture.piece(jeton + "-F2", FACTURE, fournisseurX, nomX, societe2, projet2, statutOk, "200");

        String json = corps(mockMvc, appel(SOLDES, qui(), "societeCodes", soc2));
        List<String> societes = JsonPath.read(json, "$.lignes[*].societeCode");

        assertThat(societes).containsExactly(soc2);
    }
}

package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.achat.SoldeDocumentAchat;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import jakarta.persistence.EntityManager;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.AppelsAchat.champ;
import static com.tsix.immocrm.controller.achat.AppelsAchat.codes;
import static com.tsix.immocrm.controller.achat.AppelsAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsAchat.liste;
import static com.tsix.immocrm.controller.achat.AppelsAchat.total;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.NATURES;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.NON_SOLDES;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.OK_POUR_PAIEMENT;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.TOTAUX;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.appel;
import static com.tsix.immocrm.controller.achat.AppelsImpayes.nombre;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Impayés : filtre {@code okPourPaiement}, {@code aDeduire} / {@code tropRegle}, totaux de la liste, {@code SQL_REGLE} —
 * FEATURE-achat-impayes.md §3.1-3.4, §4, §8 tests 2 à 4.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ImpayesAchatTest {

    private static final String ROLE = "Commercial";
    private static final String VRAI = "true";
    private static final String FACTURE = FixtureAchat.FACTURE;
    private static final String DOCUMENT = FixtureAchat.DOCUMENT_A_REGLER;
    private static final String RESTE = "resteARegler";
    private static final LocalDate DATE_REGLEMENT = LocalDate.of(2026, 5, 20);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;
    @Autowired private EntityManager em;

    private String jeton;
    private UUID societe;
    private UUID projet;
    private UUID fournisseur;
    private String nom;
    private UUID statutOk;
    private UUID statutEnCours;
    private UUID type;
    private AdmUser compte;

    @BeforeEach
    void setUp() {
        jeton = FixtureAchat.jeton("AIM");
        societe = fixture.societe("SOC-" + jeton);
        projet = fixture.projet("P-" + jeton, societe);
        nom = "Fournisseur " + jeton;
        fournisseur = fixture.fournisseur("FOU-" + jeton, nom);
        statutOk = fixture.statut("VPP-" + jeton, true, false, 10);
        statutEnCours = fixture.statut("ENC-" + jeton, false, false, 1);
        type = fixture.typeReglement("VIR-" + jeton);
        compte = fixture.compte("ACH-" + jeton, ROLE);
        fixture.habiliter(compte, projet);
    }

    private RequestPostProcessor qui() {
        return connecte(compte, ROLE);
    }

    private UUID piece(String suffixe, String nature, UUID statut, String aPayer) {
        return fixture.piece(jeton + suffixe, nature, fournisseur, nom, societe, projet, statut, aPayer);
    }

    private void regler(UUID piece, String suffixe, String montant, String statut) {
        fixture.reglement(piece, "RG" + suffixe + "-" + jeton, new BigDecimal(montant), statut, DATE_REGLEMENT, type);
    }

    private static String[] validesNonReglees() {
        return new String[]{NATURES, FACTURE, NATURES, DOCUMENT, NON_SOLDES, VRAI, OK_POUR_PAIEMENT, VRAI};
    }

    // ------------------------------------------------------------------ §8.2 annulé exclu (cas 23/00629)

    @Test
    void s82_factureRegleeParUnReglementAnnule_resteDansLesValideesNonReglees() throws Exception {
        UUID annulee = piece("-ANN", FACTURE, statutOk, "19200");
        regler(annulee, "A", "19200", FixtureAchat.ANNULE);
        piece("-TEM", FACTURE, statutEnCours, "500");

        String json = corps(mockMvc, liste(qui(), validesNonReglees()));

        assertThat(codes(json)).containsExactly(jeton + "-ANN");
    }

    @Test
    void s82_totaux_reglementAnnule_resteDuEgalAPayer() throws Exception {
        UUID annulee = piece("-ANN", FACTURE, statutOk, "19200");
        regler(annulee, "A", "19200", FixtureAchat.ANNULE);

        String json = corps(mockMvc, appel(TOTAUX, qui(), validesNonReglees()));

        assertAll(
                () -> assertThat(nombre(json, "$.nombre")).isEqualByComparingTo("1"),
                () -> assertThat(nombre(json, "$.montantRegle")).isEqualByComparingTo("0"),
                () -> assertThat(nombre(json, "$.resteDu")).isEqualByComparingTo("19200"));
    }

    // ------------------------------------------------------------------ §8.3 validées

    private void troisFacturesRestantes() {
        UUID ok = piece("-OK", FACTURE, statutOk, "1000");
        regler(ok, "1", "250", FixtureAchat.EN_COURS);
        piece("-ENC", FACTURE, statutEnCours, "2000");
        UUID soldee = piece("-SOL", FACTURE, statutOk, "300");
        regler(soldee, "2", "300", FixtureAchat.EN_COURS);
    }

    @Test
    void s83_validees_neRendQueLaFactureOkNonSoldee() throws Exception {
        troisFacturesRestantes();

        String json = corps(mockMvc, liste(qui(), validesNonReglees()));

        assertThat(codes(json)).containsExactly(jeton + "-OK");
    }

    @Test
    void s83_validees_totaux_nombre1_resteDuEgalSonReste() throws Exception {
        troisFacturesRestantes();

        String json = corps(mockMvc, appel(TOTAUX, qui(), validesNonReglees()));

        assertAll(
                () -> assertThat(nombre(json, "$.nombre")).isEqualByComparingTo("1"),
                () -> assertThat(nombre(json, "$.resteDu")).isEqualByComparingTo("750"));
    }

    @Test
    void s4_okPourPaiementFaux_pasDeFiltre() throws Exception {
        troisFacturesRestantes();

        String json = corps(mockMvc, liste(qui(), OK_POUR_PAIEMENT, "false"));

        assertThat(codes(json)).containsExactlyInAnyOrder(jeton + "-OK", jeton + "-ENC", jeton + "-SOL");
    }

    @Test
    void s4_okPourPaiementVrai_seulSansNonSoldes_rendLesStatutsOkSoldeesComprises() throws Exception {
        troisFacturesRestantes();

        String json = corps(mockMvc, liste(qui(), OK_POUR_PAIEMENT, VRAI));

        assertThat(codes(json)).containsExactlyInAnyOrder(jeton + "-OK", jeton + "-SOL");
    }

    @Test
    void s4_export_prendOkPourPaiement() throws Exception {
        troisFacturesRestantes();

        List<String> textes = AppelsAchat.textesExport(mockMvc,
                appel(AppelsAchat.EXPORT, qui(), validesNonReglees()));

        assertThat(textes).contains(jeton + "-OK").doesNotContain(jeton + "-ENC", jeton + "-SOL");
    }

    // ------------------------------------------------------------------ §3.1-3.2 aDeduire / tropRegle sur le fil

    @Test
    void s32_documentNegatifSansReglement_aDeduireVrai_tropRegleFaux() throws Exception {
        piece("-NEG", DOCUMENT, null, "-2000");

        String json = corps(mockMvc, liste(qui()));

        assertAll(
                () -> assertThat(champ(json, jeton + "-NEG", "aDeduire")).isEqualTo(true),
                () -> assertThat(champ(json, jeton + "-NEG", "tropRegle")).isEqualTo(false));
    }

    @Test
    void s32_factureTropReglee_tropRegleVrai_aDeduireFaux() throws Exception {
        UUID trop = piece("-TRO", FACTURE, statutOk, "3000");
        regler(trop, "T", "3020", FixtureAchat.EN_COURS);

        String json = corps(mockMvc, liste(qui()));

        assertAll(
                () -> assertThat(champ(json, jeton + "-TRO", "tropRegle")).isEqualTo(true),
                () -> assertThat(champ(json, jeton + "-TRO", "aDeduire")).isEqualTo(false));
    }

    @Test
    void s3_decisionQ2_documentNegatif_exclusDesImpayes() throws Exception {
        piece("-NEG", DOCUMENT, null, "-2000");
        piece("-POS", DOCUMENT, null, "2000");

        String json = corps(mockMvc, liste(qui(), NATURES, FACTURE, NATURES, DOCUMENT, NON_SOLDES, VRAI));

        assertThat(codes(json)).containsExactly(jeton + "-POS");
    }

    // ------------------------------------------------------------------ §8.4 totaux = liste

    /** F1 reste 750, F2 trop réglé −20, D1 avance −40, F3 reste 500, F4 soldée ; avoir hors natures. */
    private void cinqPiecesEtUnAvoir() {
        UUID f1 = piece("-F1", FACTURE, statutOk, "1000");
        regler(f1, "1", "250", FixtureAchat.EN_COURS);
        UUID f2 = piece("-F2", FACTURE, statutOk, "3000");
        regler(f2, "2", "3020", FixtureAchat.EN_COURS);
        piece("-D1", DOCUMENT, null, "-40");
        piece("-F3", FACTURE, statutEnCours, "500");
        UUID f4 = piece("-F4", FACTURE, statutOk, "800");
        regler(f4, "4", "800", FixtureAchat.EN_COURS);
        piece("-AV", FixtureAchat.AVOIR, null, "30");
    }

    @Test
    void s84_totaux_nombreEtResteEgauxALaListeSurToutesLesPages() throws Exception {
        cinqPiecesEtUnAvoir();
        String[] filtre = {NATURES, FACTURE, NATURES, DOCUMENT};

        String totaux = corps(mockMvc, appel(TOTAUX, qui(), filtre));
        String page0 = corps(mockMvc, appel(AppelsAchat.DOCUMENTS, qui(), filtre).param("size", "2").param("page", "0"));
        List<Object> restes = new ArrayList<>();
        for (int page = 0; page < 3; page++) {
            String json = corps(mockMvc, appel(AppelsAchat.DOCUMENTS, qui(), filtre)
                    .param("size", "2").param("page", String.valueOf(page)));
            List<Object> restesDeLaPage = JsonPath.read(json, "$.content[*]." + RESTE);
            restes.addAll(restesDeLaPage);
        }
        BigDecimal attendu = restes.stream().map(r -> new BigDecimal(String.valueOf(r)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertAll(
                () -> assertThat(nombre(totaux, "$.nombre").longValue()).isEqualTo(total(page0)).isEqualTo(5L),
                () -> assertThat(nombre(totaux, "$." + RESTE)).isEqualByComparingTo(attendu)
                        .isEqualByComparingTo("1190"));
    }

    @Test
    void s4_totaux_champs_resteDuNeSommeQueLesRestesPositifs() throws Exception {
        cinqPiecesEtUnAvoir();

        String json = corps(mockMvc, appel(TOTAUX, qui(), NATURES, FACTURE, NATURES, DOCUMENT));

        assertAll(
                () -> assertThat(nombre(json, "$.montantTTC")).isEqualByComparingTo("5260"),
                () -> assertThat(nombre(json, "$.montantAPayer")).isEqualByComparingTo("5260"),
                () -> assertThat(nombre(json, "$.montantRegle")).isEqualByComparingTo("4070"),
                () -> assertThat(nombre(json, "$." + RESTE)).isEqualByComparingTo("1190"),
                () -> assertThat(nombre(json, "$.nombreARegler")).isEqualByComparingTo("2"),
                () -> assertThat(nombre(json, "$.resteDu")).isEqualByComparingTo("1250"));
    }

    // ------------------------------------------------------------------ §3.1 SQL_REGLE

    @Test
    void s31_sqlRegle_sommeLesLignesNonAnnuleesDeLaPieceSeulement() throws Exception {
        UUID cible = piece("-SQL", FACTURE, statutOk, "1000");
        regler(cible, "C", "300", FixtureAchat.EN_COURS);
        regler(cible, "K", "200", FixtureAchat.ANNULE);
        regler(cible, "N", "100", null);
        UUID autre = piece("-AUT", FACTURE, statutOk, "1000");
        regler(autre, "X", "999", FixtureAchat.EN_COURS);
        String sqlRegle = (String) SoldeDocumentAchat.class.getField("SQL_REGLE").get(null);
        em.flush();

        Object regle = em.createNativeQuery("SELECT COALESCE(( " + sqlRegle + " ), 0) FROM BuyDocument d WHERE d.PKey = :id")
                .setParameter("id", cible).getSingleResult();

        assertThat(new BigDecimal(String.valueOf(regle))).isEqualByComparingTo("400");
    }

    @Test
    void s4_totaux_memeRequeteQueLaListe_filtreFournisseur() throws Exception {
        piece("-F1", FACTURE, statutOk, "100");

        String json = corps(mockMvc, get(TOTAUX).param("fournisseurId", UUID.randomUUID().toString()).with(qui()));

        assertThat(nombre(json, "$.nombre")).isEqualByComparingTo("0");
    }
}

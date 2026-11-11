package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Piece;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

import static com.tsix.immocrm.controller.achat.AppelsAchat.champ;
import static com.tsix.immocrm.controller.achat.AppelsAchat.codes;
import static com.tsix.immocrm.controller.achat.AppelsAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsAchat.liste;
import static com.tsix.immocrm.controller.achat.AppelsAchat.montant;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Liste, filtres, tri par défaut, 400 et référentiels — FEATURE-achat-lecture-factures.md §3, §7 test 5
 * (+ « Réponses du manager » n° 1 et 3).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentsAchatFiltresTest {

    private static final String ROLE = "Commercial";
    private static final String NATURES = "natures";
    private static final String STATUT_IDS = "statutIds";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;

    private String jeton;
    private UUID societe;
    private UUID projet;
    private UUID fournisseur;
    private String nom;
    private AdmUser compte;

    @BeforeEach
    void setUp() {
        jeton = FixtureAchat.jeton("AFI");
        societe = fixture.societe("SOC-" + jeton);
        projet = fixture.projet("P-" + jeton, societe);
        nom = "Fournisseur " + jeton;
        fournisseur = fixture.fournisseur("FOU-" + jeton, nom);
        compte = fixture.compte("ACH-" + jeton, ROLE);
        fixture.habiliterSociete(compte, societe);
        fixture.habiliter(compte, projet);
    }

    private RequestPostProcessor qui() {
        return connecte(compte, ROLE);
    }

    private Piece facture(String suffixe, LocalDate date, String aPayer) {
        return Piece.facture(jeton + suffixe, date, fournisseur, nom, societe, projet, new BigDecimal(aPayer));
    }

    private static final LocalDate MARS_10 = LocalDate.of(2026, 3, 10);

    // ------------------------------------------------------------------ §3 : champs de la ligne

    @Test
    void s3_ligneDeListe_champsDuSpec() throws Exception {
        UUID statut = fixture.statut("VPP-" + jeton, true, false, 10);
        UUID lot = fixture.lot("LOT-" + jeton, projet);
        UUID piece = fixture.piece(facture("-M", MARS_10, "1200").lot(lot).statut(statut));
        String code = jeton + "-M";

        String json = corps(mockMvc, liste(qui()));

        assertAll(
                () -> assertThat(champ(json, code, "id")).isEqualTo(piece.toString()),
                () -> assertThat(champ(json, code, "nature")).isEqualTo("INVOICE"),
                () -> assertThat(champ(json, code, "natureLibelle")).isEqualTo("Facture"),
                () -> assertThat(champ(json, code, "reference")).isEqualTo("REF-" + code),
                () -> assertThat(champ(json, code, "description")).isEqualTo("Description " + code),
                () -> assertThat(champ(json, code, "dateDocument")).isEqualTo("2026-03-10"),
                () -> assertThat(champ(json, code, "dateEcheance")).isEqualTo("2026-04-09"),
                () -> assertThat(champ(json, code, "fournisseurId")).isEqualTo(fournisseur.toString()),
                () -> assertThat(champ(json, code, "fournisseurNom")).isEqualTo(nom),
                () -> assertThat(champ(json, code, "projetId")).isEqualTo(projet.toString()),
                () -> assertThat(champ(json, code, "projetCode")).isEqualTo("P-" + jeton),
                () -> assertThat(champ(json, code, "lotId")).isEqualTo(lot.toString()),
                () -> assertThat(champ(json, code, "lotCode")).isEqualTo("LOT-" + jeton),
                () -> assertThat(champ(json, code, "societeCode")).isEqualTo("SOC-" + jeton),
                () -> assertThat(champ(json, code, "statutId")).isEqualTo(statut.toString()),
                () -> assertThat(champ(json, code, "statutCode")).isEqualTo("VPP-" + jeton),
                () -> assertThat(champ(json, code, "statutDescription")).isEqualTo("Statut VPP-" + jeton),
                () -> assertThat(champ(json, code, "okPourPaiement")).isEqualTo(true),
                () -> assertThat(montant(json, code, "montantHT")).isEqualByComparingTo("1000"),
                () -> assertThat(montant(json, code, "montantTVA")).isEqualByComparingTo("200"),
                () -> assertThat(montant(json, code, "montantTTC")).isEqualByComparingTo("1200"),
                () -> assertThat(montant(json, code, "montantAPayer")).isEqualByComparingTo("1200"));
    }

    @Test
    void s3_devisSansStatutNiLot_statutEtLotNuls_okPourPaiementFaux() throws Exception {
        fixture.piece(facture("-Q", MARS_10, "100").nature("QUOTE"));
        String code = jeton + "-Q";

        String json = corps(mockMvc, liste(qui()));

        assertAll(
                () -> assertThat(champ(json, code, "natureLibelle")).isEqualTo("Devis"),
                () -> assertThat(champ(json, code, "statutId")).isNull(),
                () -> assertThat(champ(json, code, "statutCode")).isNull(),
                () -> assertThat(champ(json, code, "lotCode")).isNull(),
                () -> assertThat(champ(json, code, "okPourPaiement")).isEqualTo(false));
    }

    // ------------------------------------------------------------------ tri par défaut

    @Test
    void s3_triParDefaut_dateDocumentDescPuisCodeDesc() throws Exception {
        fixture.piece(facture("-A", MARS_10, "100"));
        fixture.piece(facture("-B", LocalDate.of(2026, 3, 12), "100"));
        fixture.piece(facture("-C", MARS_10, "100"));

        String json = corps(mockMvc, liste(qui()));

        assertThat(codes(json)).containsExactly(jeton + "-B", jeton + "-C", jeton + "-A");
    }

    // ------------------------------------------------------------------ filtres

    @Test
    void r5_filtreNatures_multiple() throws Exception {
        fixture.piece(facture("-I", MARS_10, "100"));
        fixture.piece(facture("-O", MARS_10, "100").nature("ORDER"));
        fixture.piece(facture("-Q", MARS_10, "100").nature("QUOTE"));

        String json = corps(mockMvc, liste(qui(), NATURES, "INVOICE", NATURES, "ORDER"));

        assertThat(codes(json)).containsExactlyInAnyOrder(jeton + "-I", jeton + "-O");
    }

    @Test
    void r5_filtreStatuts_multiple() throws Exception {
        UUID s1 = fixture.statut("S1-" + jeton, false, false, 1);
        UUID s2 = fixture.statut("S2-" + jeton, false, false, 2);
        UUID s3 = fixture.statut("S3-" + jeton, false, false, 3);
        fixture.piece(facture("-1", MARS_10, "100").statut(s1));
        fixture.piece(facture("-2", MARS_10, "100").statut(s2));
        fixture.piece(facture("-3", MARS_10, "100").statut(s3));

        String json = corps(mockMvc, liste(qui(), STATUT_IDS, s1.toString(), STATUT_IDS, s2.toString()));

        assertThat(codes(json)).containsExactlyInAnyOrder(jeton + "-1", jeton + "-2");
    }

    @Test
    void r5_filtrePeriode_surDateDocument() throws Exception {
        fixture.piece(facture("-AV", LocalDate.of(2026, 3, 1), "100"));
        fixture.piece(facture("-DS", LocalDate.of(2026, 3, 15), "100"));
        fixture.piece(facture("-AP", LocalDate.of(2026, 3, 30), "100"));

        String json = corps(mockMvc, liste(qui(), "du", "2026-03-10", "au", "2026-03-20"));

        assertThat(codes(json)).containsExactly(jeton + "-DS");
    }

    @Test
    void r5_filtreFournisseur() throws Exception {
        String autreNom = "Autre " + jeton;
        UUID autre = fixture.fournisseur("FOU2-" + jeton, autreNom);
        fixture.piece(facture("-F1", MARS_10, "100"));
        fixture.piece(Piece.facture(jeton + "-F2", MARS_10, autre, autreNom, societe, projet, new BigDecimal("100")));

        String json = corps(mockMvc, liste(qui(), "fournisseurId", autre.toString()));

        assertThat(codes(json)).containsExactly(jeton + "-F2");
    }

    @Test
    void r5_filtreLot() throws Exception {
        UUID lot1 = fixture.lot("L1-" + jeton, projet);
        UUID lot2 = fixture.lot("L2-" + jeton, projet);
        fixture.piece(facture("-L1", MARS_10, "100").lot(lot1));
        fixture.piece(facture("-L2", MARS_10, "100").lot(lot2));
        fixture.piece(facture("-L0", MARS_10, "100"));

        String json = corps(mockMvc, liste(qui(), "lotId", lot1.toString()));

        assertThat(codes(json)).containsExactly(jeton + "-L1");
    }

    /** Réponse du manager n° 1 : {@code nonSoldes} = reste > 0,05 ; ni le soldé au seuil, ni le trop réglé. */
    @Test
    void r5_reponse1_filtreNonSoldes_resteStrictementSuperieurA005() throws Exception {
        UUID type = fixture.typeReglement("VIR-" + jeton);
        UUID impaye = fixture.piece(facture("-IMP", MARS_10, "1000"));
        UUID auSeuil = fixture.piece(facture("-SEU", MARS_10, "500"));
        UUID justeAuDessus = fixture.piece(facture("-DES", MARS_10, "500"));
        UUID tropRegle = fixture.piece(facture("-TRO", MARS_10, "3000"));
        UUID solde = fixture.piece(facture("-SOL", MARS_10, "800"));
        LocalDate date = LocalDate.of(2026, 3, 20);
        fixture.reglement(impaye, "RG1-" + jeton, new BigDecimal("100"), FixtureAchat.EN_COURS, date, type);
        fixture.reglement(auSeuil, "RG2-" + jeton, new BigDecimal("499.96"), FixtureAchat.EN_COURS, date, type);
        fixture.reglement(justeAuDessus, "RG3-" + jeton, new BigDecimal("499.94"), FixtureAchat.EN_COURS, date, type);
        fixture.reglement(tropRegle, "RG4-" + jeton, new BigDecimal("3020"), FixtureAchat.EN_COURS, date, type);
        fixture.reglement(solde, "RG5-" + jeton, new BigDecimal("800"), FixtureAchat.EN_COURS, date, type);

        String json = corps(mockMvc, liste(qui(), "nonSoldes", "true"));

        assertThat(codes(json)).containsExactlyInAnyOrder(jeton + "-IMP", jeton + "-DES");
    }

    @Test
    void r5_reponse1_nonSoldes_unReglementAnnuleNeSoldePas() throws Exception {
        UUID type = fixture.typeReglement("VIR-" + jeton);
        UUID piece = fixture.piece(facture("-ANN", MARS_10, "1000"));
        fixture.reglement(piece, "RGA-" + jeton, new BigDecimal("1000"), FixtureAchat.ANNULE, MARS_10, type);

        String json = corps(mockMvc, liste(qui(), "nonSoldes", "true"));

        assertThat(codes(json)).containsExactly(jeton + "-ANN");
    }

    @ParameterizedTest(name = "recherche sur {0}")
    @CsvSource({"code", "reference", "description", "fournisseur"})
    void r5_recherche_surCodeReferenceDescriptionFournisseur(String champ) throws Exception {
        String autreNom = "Tiers ZQX" + jeton;
        UUID autre = fixture.fournisseur("FOU3-" + jeton, autreNom);
        fixture.piece(facture("-CODEZQX", MARS_10, "100"));
        fixture.piece(facture("-REF", MARS_10, "100").textes("REFZQX" + jeton, "Description R"));
        fixture.piece(facture("-DSC", MARS_10, "100").textes("R-" + jeton, "Travaux DSCZQX" + jeton));
        fixture.piece(Piece.facture(jeton + "-FOU", MARS_10, autre, autreNom, societe, projet, new BigDecimal("100")));
        fixture.piece(facture("-RIEN", MARS_10, "100").textes("R2-" + jeton, "Rien"));
        Map<String, String[]> attendus = Map.of(
                "code", new String[]{"CODEZQX", jeton + "-CODEZQX"},
                "reference", new String[]{"REFZQX" + jeton, jeton + "-REF"},
                "description", new String[]{"DSCZQX" + jeton, jeton + "-DSC"},
                "fournisseur", new String[]{"Tiers ZQX" + jeton, jeton + "-FOU"});
        String[] cas = attendus.get(champ);

        String json = corps(mockMvc, liste(qui(), "search", cas[0]));

        assertThat(codes(json)).containsExactly(cas[1]);
    }

    // ------------------------------------------------------------------ 400

    @ParameterizedTest(name = "{0}={1} → 400")
    @CsvSource({
            "natures, INCONNUE",
            "projetIds, pas-un-uuid",
            "statutIds, pas-un-uuid",
            "fournisseurId, pas-un-uuid",
            "lotId, pas-un-uuid",
            "du, pas-une-date",
            "au, 2026-13-45"})
    void r5_filtreMalForme_400(String parametre, String valeur) throws Exception {
        mockMvc.perform(get(AppelsAchat.DOCUMENTS).param(parametre, valeur).with(qui()))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ référentiels

    @Test
    void s3_referentiels_naturesDansLOrdreFixeAvecLibelles() throws Exception {
        String json = corps(mockMvc, get(AppelsAchat.REFERENTIELS).with(qui()));

        List<String> natures = JsonPath.read(json, "$.natures[*].code");
        List<String> libelles = JsonPath.read(json, "$.natures[*].libelle");
        assertAll(
                () -> assertThat(natures)
                        .containsExactly("INVOICE", "ORDER", "QUOTE", "DOCUMENT_TO_PAY", "DELIVRY", "REFUND"),
                () -> assertThat(libelles).containsExactly("Facture", "Bon de commande", "Devis",
                        "Document à régler", "Bon de livraison", "Avoir"));
    }

    @Test
    void s3_referentiels_statutsAvecDrapeauxEtOrdre() throws Exception {
        UUID statut = fixture.statut("REF-" + jeton, false, true, 42);

        String json = corps(mockMvc, get(AppelsAchat.REFERENTIELS).with(qui()));

        String chemin = "$.statuts[?(@.id == '" + statut + "')]";
        assertAll(
                () -> assertThat((List<Object>) JsonPath.read(json, chemin + ".code")).containsExactly("REF-" + jeton),
                () -> assertThat((List<Object>) JsonPath.read(json, chemin + ".description"))
                        .containsExactly("Statut REF-" + jeton),
                () -> assertThat((List<Object>) JsonPath.read(json, chemin + ".okPourPaiement")).containsExactly(false),
                () -> assertThat((List<Object>) JsonPath.read(json, chemin + ".valideComptabilite"))
                        .containsExactly(true),
                () -> assertThat((List<Object>) JsonPath.read(json, chemin + ".ordre")).containsExactly(42));
    }

    @Test
    void s3_referentiels_projetVisible_avecCodeEtDescription() throws Exception {
        String json = corps(mockMvc, get(AppelsAchat.REFERENTIELS).with(qui()));

        String chemin = "$.projets[?(@.id == '" + projet + "')]";
        assertAll(
                () -> assertThat((List<Object>) JsonPath.read(json, chemin + ".code")).containsExactly("P-" + jeton),
                () -> assertThat((List<Object>) JsonPath.read(json, chemin + ".description"))
                        .containsExactly("Projet P-" + jeton));
    }
}

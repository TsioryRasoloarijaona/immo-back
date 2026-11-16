package com.tsix.immocrm.controller.achat;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureAchat;
import com.tsix.immocrm.fixtures.FixtureAchat.Cadre;
import com.tsix.immocrm.fixtures.FixtureAchat.Saisie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.ROLE_JWT;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.appeler;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.corps;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.detail;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.id;
import static com.tsix.immocrm.controller.achat.AppelsEcrituresAchat.statut;
import static com.tsix.immocrm.controller.achat.AppelsSaisieAchat.*;
import static com.tsix.immocrm.fixtures.FixtureDossierVente.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * A2b-2a E-C2 (FEATURE-achat-fiche-creation.md, Vérification n° 1 et 2) prouvé par le {@code POST} : formules de ligne d'OMRAN
 * ({@code BaseLine.cs}), totaux de pièce, net à payer, colonnes de ligne, refus 400 nommant le champ. Les montants attendus
 * tombent juste au centime : le mode d'arrondi (Q-3) n'est pas jugé ici.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PieceAchatMontantsTest {

    private static final String LIGNES = "SELECT * FROM BuyDocumentLine WHERE BuyDocumentId = :id ORDER BY Sorting ASC";
    private static final String HT = "AmountHT";
    private static final String TVA = "AmountVat";
    private static final String TTC = "AmountTTC";
    private static final String A_PAYER = "AmountToPay";
    private static final String LIGNES_CHAMP = "lignes";
    private static final String LINES_COUNT = "LinesCount";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureAchat fixture;

    private Cadre c;
    private Saisie s;
    private AdmUser redacteur;

    @BeforeEach
    void setUp() {
        c = fixture.cadre("AMT");
        s = fixture.configurationSaisie(c.jeton());
        redacteur = fixture.compteHabilite(c, "AMT", ROLE_JWT, NOUVEAU_BC, NOUVEAU_FACTURE);
        fixture.sansPersonnel(redacteur);
    }

    private MvcResult creerFacture(CorpsJson corps) throws Exception {
        return appeler(mockMvc, creer(corps), connecte(redacteur, ROLE_JWT));
    }

    private Map<String, Object> creerOk(List<CorpsJson> lignes) throws Exception {
        return creerOk(corpsValide(c, s, FixtureAchat.FACTURE).avec(LIGNES_CHAMP, lignes));
    }

    private Map<String, Object> creerOk(CorpsJson corps) throws Exception {
        MvcResult r = creerFacture(corps);
        assertThat(statut(r)).as(corps(r)).isEqualTo(201);
        return fixture.pieceEnBase(idCree(r));
    }

    private static void assertMontants(Map<String, Object> d, String ht, String tva, String ttc, String aPayer) {
        assertAll(
                () -> assertThat(decimalDe(d, HT)).as(HT).isEqualByComparingTo(ht),
                () -> assertThat(decimalDe(d, TVA)).as(TVA).isEqualByComparingTo(tva),
                () -> assertThat(decimalDe(d, TTC)).as(TTC).isEqualByComparingTo(ttc),
                () -> assertThat(decimalDe(d, A_PAYER)).as(A_PAYER).isEqualByComparingTo(aPayer));
    }

    // ------------------------------------------------------------------ E-C2.1 / E-C2.2 formules

    /** E-C2.1 : PU 1000, remise 10 %, FODEC 1 %, TVA 20 %, qté 3 → PU HT 909, HT 2 727, TVA 545,4 ; PU TTC stocké 1 090,8. */
    @Test
    void ec21_remiseFodecTva_formulesOmran() throws Exception {
        Map<String, Object> d = creerOk(List.of(ligne("ART-1", "3", "1000", "0.1", "0.01", "0.2", s.categorie())));

        assertMontants(d, "2727", "545.4", "3272.4", "3272.4");
        assertThat(fixture.lire(LIGNES, UUID.fromString(d.get("PKey").toString()))).singleElement().satisfies(l -> assertAll(
                () -> assertThat(decimalDe(l, "UnitPriceHT")).isEqualByComparingTo("1000"),
                () -> assertThat(decimalDe(l, "UnitPriceTTC")).isEqualByComparingTo("1090.8"),
                () -> assertThat(decimalDe(l, "DiscountRatio")).isEqualByComparingTo("0.1"),
                () -> assertThat(decimalDe(l, "FODECRatio")).isEqualByComparingTo("0.01"),
                () -> assertThat(decimalDe(l, "VatRatio")).isEqualByComparingTo("0.2"),
                () -> assertThat(decimalDe(l, "Quantity")).isEqualByComparingTo("3")));
    }

    /** E-C2.2 : deux taux de TVA → HT 250, TVA 20 + 15, TTC 285. */
    @Test
    void ec22_deuxTauxDeTva_sommeParGroupe() throws Exception {
        Map<String, Object> d = creerOk(List.of(ligne("A", "1", "100", "0", "0", "0.2", s.categorie()),
                ligne("B", "1", "150", "0", "0", "0.1", s.categorie())));

        assertMontants(d, "250", "35", "285", "285");
    }

    /** Vérification n° 1 : la facture 21/00001 (ART1 100 × 120, ART2 220 × 320, TVA 20 %) → 82 400 / 16 480 / 98 880. */
    @Test
    void verif1_facture2100001_82400_16480_98880() throws Exception {
        Map<String, Object> d = creerOk(List.of(ligne("ART1", "100", "120", s.categorie()),
                ligne("ART2", "220", "320", s.categorie())));

        assertMontants(d, "82400", "16480", "98880", "98880");
    }

    /** E-C2.4 : quantité négative acceptée (26 lignes de facture en prod). */
    @Test
    void ec24_quantiteNegative_acceptee() throws Exception {
        Map<String, Object> d = creerOk(List.of(ligne("AV", "-2", "100", s.categorie())));

        assertMontants(d, "-200", "-40", "-240", "-240");
    }

    /** E-C2.4 : PU 0 permis. */
    @Test
    void ec24_prixUnitaireZero_accepte() throws Exception {
        Map<String, Object> d = creerOk(List.of(ligne("GR", "5", "0", s.categorie())));

        assertMontants(d, "0", "0", "0", "0");
    }

    /** E-C2.3 : sans ligne, HT = TVA = TTC = 0 et à payer = régul + timbre. */
    @Test
    void ec23_sansLigne_montantsNuls_aPayerRegulPlusTimbre() throws Exception {
        Map<String, Object> d = creerOk(corpsValide(c, s, FixtureAchat.FACTURE).avec(LIGNES_CHAMP, List.of())
                .avec("montantRegul", new BigDecimal("50")).avec("timbreFiscal", new BigDecimal("10")));

        assertAll(
                () -> assertMontants(d, "0", "0", "0", "60"),
                () -> assertThat(decimalDe(d, LINES_COUNT)).isZero());
    }

    /** E-C2.2 : à payer = TTC + régul + timbre ; régul et timbre écrits. */
    @Test
    void ec22_aPayer_ttcPlusRegulPlusTimbre() throws Exception {
        Map<String, Object> d = creerOk(corpsValide(c, s, FixtureAchat.FACTURE)
                .avec("montantRegul", new BigDecimal("50")).avec("timbreFiscal", new BigDecimal("10")));

        assertAll(
                () -> assertMontants(d, "1000", "200", "1200", "1260"),
                () -> assertThat(decimalDe(d, "AmountRegul")).isEqualByComparingTo("50"),
                () -> assertThat(decimalDe(d, "FiscalStamp")).isEqualByComparingTo("10"));
    }

    // ------------------------------------------------------------------ E-C2.4 / E-C2.5 colonnes de ligne

    @Test
    void ec24_colonnesDeLigne_ordreDuCorps_constantes_uniteNullVide() throws Exception {
        UUID autreCategorie = fixture.categorie("CAT2-" + c.jeton());
        Map<String, Object> d = creerOk(List.of(
                ligne("Z-PREMIER", "1", "10", s.categorie()).avec("notes", "note 1"),
                ligne("A-SECOND", "2", "20", autreCategorie).avec("unite", null),
                ligne("M-TROISIEME", "3", "30", s.categorie()).avec("remise", null).avec("fodec", null)));

        List<Map<String, Object>> l = fixture.lire(LIGNES, UUID.fromString(d.get("PKey").toString()));
        assertAll(
                () -> assertThat(decimalDe(d, LINES_COUNT)).isEqualByComparingTo("3"),
                () -> assertThat(l).extracting(x -> x.get("Code")).containsExactly("Z-PREMIER", "A-SECOND", "M-TROISIEME"),
                () -> assertThat(l).extracting(x -> ((Number) x.get("Sorting")).intValue()).containsExactly(1, 2, 3),
                () -> assertThat(l).extracting(x -> x.get("Description"))
                        .containsExactly("Article Z-PREMIER", "Article A-SECOND", "Article M-TROISIEME"),
                () -> assertThat(l.get(0)).containsEntry("Unity", "U").containsEntry("Notes", "note 1")
                        .containsEntry("BuyItemId", null).containsEntry("PrjMarketLineId", null),
                () -> assertThat(l.get(1)).containsEntry("Unity", ""),
                () -> assertThat(guidDe(l.get(1), "StlCategoryId")).isEqualTo(id(autreCategorie)),
                () -> assertThat(decimalDe(l.get(2), "DiscountRatio")).as("null → 0").isEqualByComparingTo("0"),
                () -> assertThat(decimalDe(l.get(2), "FODECRatio")).as("null → 0").isEqualByComparingTo("0"),
                () -> assertThat(l).isNotEmpty().allSatisfy(x -> assertAll(
                        () -> assertThat(decimalDe(x, "Coefficient")).isEqualByComparingTo("1"),
                        () -> assertThat(decimalDe(x, "QuantityOld")).isEqualByComparingTo("0"),
                        () -> assertThat(decimalDe(x, "QuantityOrigin")).isEqualByComparingTo("0"))));
    }

    // ------------------------------------------------------------------ E-C2.4 refus 400 (champ nommé)

    static Stream<Arguments> champsInvalides() {
        return Stream.of(
                Arguments.of("quantite", null),
                Arguments.of("prixUnitaireHT", null),
                Arguments.of("remise", new BigDecimal("1.5")),
                Arguments.of("remise", new BigDecimal("-0.1")),
                Arguments.of("fodec", new BigDecimal("2")),
                Arguments.of("tva", new BigDecimal("1.2")),
                Arguments.of("tva", new BigDecimal("-0.2")),
                Arguments.of("description", ""),
                Arguments.of("description", null),
                Arguments.of("categorieId", null));
    }

    @ParameterizedTest(name = "{0} = {1}")
    @MethodSource("champsInvalides")
    void ec24_ligneInvalide_400_detailNommeLeChamp(String champ, Object valeur) throws Exception {
        CorpsJson ligne = ligne("ART-1", "1", "100", s.categorie()).avec(champ, valeur);

        MvcResult r = creerFacture(corpsValide(c, s, FixtureAchat.FACTURE).avec(LIGNES_CHAMP, List.of(ligne)));

        assertThat(statut(r)).as(corps(r)).isEqualTo(400);
        assertThat(detail(r)).contains(champ);
        fixture.oublier();
        assertThat(fixture.valeurSequence(FixtureAchat.SEQUENCES_SAISIE.get(FixtureAchat.FACTURE).code())).isEqualTo(800);
    }

    /** E-C2.4 : catégorie de ligne inconnue de {@code StlCategory} → 400 (id jamais écrit : refus avant écriture). */
    @Test
    void ec24_categorieInconnue_400() throws Exception {
        CorpsJson ligne = ligne("ART-1", "1", "100", UUID.randomUUID());

        MvcResult r = creerFacture(corpsValide(c, s, FixtureAchat.FACTURE).avec(LIGNES_CHAMP, List.of(ligne)));

        assertThat(statut(r)).as(corps(r)).isEqualTo(400);
        assertThat(detail(r)).contains("categorieId");
    }

    /** Spec (DTO) : champs d'en-tête obligatoires → 400 nommant le champ, rien d'écrit. */
    @ParameterizedTest
    @ValueSource(strings = {"dateDocument", "societeId", "moduleId", "fournisseurId",
            "typeMarcheId"})
    void dto_enTeteObligatoire_absent_400_detailNommeLeChamp(String champ) throws Exception {
        MvcResult r = creerFacture(corpsValide(c, s, FixtureAchat.FACTURE).avec(champ, null));

        assertThat(statut(r)).as(corps(r)).isEqualTo(400);
        assertThat(detail(r)).contains(champ);
        fixture.oublier();
        assertThat(fixture.nombre("BuyDocument", "CfgTierId", c.fournisseur())).isZero();
    }
}

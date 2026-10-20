package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.domain.FuseauMetier;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureEntites;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureTerrains;
import com.tsix.immocrm.fixtures.FixtureTerrains.Contexte;
import com.tsix.immocrm.fixtures.FixtureTerrains.Sequence;
import com.tsix.immocrm.fixtures.FixtureTerrains.TypeFrais;
import com.tsix.immocrm.security.AuthenticatedUser;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.AUTEUR;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static com.tsix.immocrm.fixtures.FixtureTerrains.MESSAGE_DEJA_VALIDE;
import static com.tsix.immocrm.fixtures.FixtureTerrains.STATUT_EN_COURS;
import static com.tsix.immocrm.fixtures.FixtureTerrains.VALIDER;
import static com.tsix.immocrm.fixtures.FixtureTerrains.horodatage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * « Valider pour règlement » — FEATURE-patrimoine-terrains.md §3 et §6.2 (ValiderFraisTerrainTest), règle 6 (argent) :
 * garde-fous avant toute écriture (404, 409 déjà validé ou déjà facturé, 400 fournisseur / type, 500 configuration
 * manquante sans écriture), facture conforme colonne par colonne à la mesure, une ligne, une ventilation (Σ taux = 1),
 * un historique, code lu dans la ligne AdmSequence et incrément de 1. Horloge fixée au 10/02/2027 11:30 (Casablanca) :
 * l'année du code et les dates viennent du Clock, pas du système. Atomicité, concurrence et audit :
 * {@link ValiderFraisTerrainAtomiciteTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ValiderFraisTerrainTest {

    private static final LocalDateTime MAINTENANT = LocalDateTime.of(2027, 2, 10, 11, 30);
    private static final LocalDateTime AUJOURDHUI = MAINTENANT.toLocalDate().atStartOfDay();
    private static final BigDecimal MONTANT = new BigDecimal("8000.123456");
    private static final String FACTURE_DU_FRAIS = "SELECT * FROM BuyDocument WHERE StkExpenseId = :id";
    private static final String NB_FACTURES = "SELECT COUNT(*) FROM BuyDocument WHERE StkExpenseId = :id";
    private static final String FRAIS_PAR_ID = "SELECT * FROM StkExpense WHERE Pkey = :id";
    private static final String DETAIL = "$.detail";

    @TestBean(name = "horlogeMetier")
    private Clock horloge;

    static Clock horloge() {
        return Clock.fixed(MAINTENANT.atZone(FuseauMetier.ZONE).toInstant(), FuseauMetier.ZONE);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureTerrains terrains;
    @Autowired private FixtureEntites entites;

    private AdmUser admin;
    private Contexte c;
    private UUID terrain;
    private TypeFrais type;
    private UUID fournisseur;
    private UUID frais;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        c = terrains.contexte("P7V");
        terrains.configurationFacture();
        terrains.sequence(Sequence.commeEnProduction(3013));
        terrain = terrains.terrain(c, "TER-" + c.jeton());
        type = terrains.typeFrais(c.jeton(), true, true);
        fournisseur = terrains.fournisseur("FRN-" + c.jeton(), null);
        frais = terrains.frais(terrain, type.id(), fournisseur, MONTANT, false);
    }

    // ------------------------------------------------------------------ outils

    private ResultActions valider(UUID terrainId, UUID fraisId) throws Exception {
        return mockMvc.perform(post(VALIDER, terrainId, fraisId).with(connecte(admin)));
    }

    private Tuple facture() {
        return terrains.ligne(FACTURE_DU_FRAIS, frais);
    }

    private UUID factureId() {
        return UUID.fromString(guid(facture().get("PKey")));
    }

    /** §3.1 : un refus laisse la base intacte — aucune facture, frais non validé, séquence inchangée. */
    private void verifierRienEcrit() {
        assertAll(
                () -> assertThat(terrains.compter(NB_FACTURES, frais)).isZero(),
                () -> assertThat(terrains.ligne(FRAIS_PAR_ID, frais).get("IsValidated")).isEqualTo(false),
                () -> assertThat(terrains.valeurSequence()).isEqualTo(3013));
    }

    private static String detail(ResultActions resultat) throws Exception {
        return JsonPath.read(resultat.andReturn().getResponse().getContentAsString(), DETAIL);
    }

    // ------------------------------------------------------------------ §3.2 la facture, colonne par colonne

    @Test
    void r3_reponse200_factureIdCodeMontant() throws Exception {
        String json = valider(terrain, frais).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Tuple f = facture();
        assertAll(
                () -> assertThat(guid((Object) JsonPath.read(json, "$.factureId"))).isEqualTo(guid(f.get("PKey"))),
                () -> assertThat((String) JsonPath.read(json, "$.code")).isEqualTo("27/03014"),
                () -> assertThat(new BigDecimal(JsonPath.read(json, "$.montant").toString()))
                        .isEqualByComparingTo(MONTANT));
    }

    @Test
    void r3_2_facture_identiteEtRattachements() throws Exception {
        valider(terrain, frais).andExpect(status().isOk());

        Tuple f = facture();
        assertAll(
                () -> assertThat(f.get("Code")).isEqualTo("27/03014"),
                () -> assertThat(f.get("Description")).isEqualTo("Facture N° 27/03014 du 10/02/2027"),
                () -> assertThat(f.get("DocType")).isEqualTo("INVOICE"),
                () -> assertThat(f.get("ReferenceType")).isEqualTo("LAND_EXPENSE"),
                () -> assertThat(guid(f.get("StkExpenseId"))).isEqualTo(guid(frais)),
                () -> assertThat(guid(f.get("CfgTierId"))).isEqualTo(guid(fournisseur)),
                () -> assertThat(f.get("SupplierDescription")).isEqualTo("Fournisseur FRN-" + c.jeton()),
                () -> assertThat(guid(f.get("CfgCompanyId"))).isEqualTo(guid(c.entite().getId())),
                () -> assertThat(guid(f.get("PrjMarketTypeId"))).isEqualTo(guid(type.typeMarche())),
                () -> assertThat(f.get("Notes"))
                        .isEqualTo("Frais : " + type.description() + " sur terrain : Terrain TER-" + c.jeton()),
                () -> assertThat(guid(f.get("AdmModuleId"))).isEqualTo(guid(terrains.moduleStl())),
                () -> assertThat(guid(f.get("StlCurrencyId"))).isEqualTo(guid(terrains.deviseParDefaut())),
                () -> assertThat(guid(f.get("StrEntityId"))).isEqualTo(guid(terrains.entiteFactureTerrain())),
                () -> assertThat(guid(f.get("BuyDocumentStatusId"))).isEqualTo(guid(STATUT_EN_COURS)),
                () -> assertThat(f.get("CfgTierTriggerId")).isNull(),
                () -> assertThat(f.get("UserCreate")).isEqualTo(AUTEUR),
                () -> assertThat(f.get("UserCreateDateTime")).isNotNull());
    }

    /** §3.2 : DocDate = MaturityDate = PrevisionDeliveryDate = aujourd'hui (Clock, minuit), pas StkExpense.Date. */
    @Test
    void r3_2_facture_datesDuJour_horlogeFixee() throws Exception {
        valider(terrain, frais).andExpect(status().isOk());

        Tuple f = facture();
        assertAll(
                () -> assertThat(LocalDate.now(horloge)).isEqualTo(MAINTENANT.toLocalDate()),
                () -> assertThat(horodatage(f.get("DocDate"))).isEqualTo(AUJOURDHUI),
                () -> assertThat(horodatage(f.get("MaturityDate"))).isEqualTo(AUJOURDHUI),
                () -> assertThat(horodatage(f.get("PrevisionDeliveryDate"))).isEqualTo(AUJOURDHUI));
    }

    /** §3.2 et décision 8 bis n° 3 : MaturityDate = DocDate + CfgTier.PaymentDelay (jours) s'il est > 0. */
    @Test
    void r3_2_echeance_delaiDuFournisseur() throws Exception {
        UUID avecDelai = terrains.fournisseur("FRD-" + c.jeton(), 30);
        UUID fraisDelai = terrains.frais(terrain, type.id(), avecDelai, MONTANT, false);

        valider(terrain, fraisDelai).andExpect(status().isOk());

        Tuple f = terrains.ligne(FACTURE_DU_FRAIS, fraisDelai);
        assertAll(
                () -> assertThat(horodatage(f.get("DocDate"))).isEqualTo(AUJOURDHUI),
                () -> assertThat(horodatage(f.get("MaturityDate"))).isEqualTo(AUJOURDHUI.plusDays(30)));
    }

    /** §3.2 : montants = Amount sans arrondi (decimal(26,6) tel quel), TVA 0, cumuls et soldes à 0. */
    @Test
    void r3_2_facture_montantsSansArrondi_etZeros() throws Exception {
        valider(terrain, frais).andExpect(status().isOk());

        Tuple f = facture();
        assertAll(
                () -> assertThat(decimal(f.get("AmountHT"))).isEqualByComparingTo(MONTANT),
                () -> assertThat(decimal(f.get("AmountTTC"))).isEqualByComparingTo(MONTANT),
                () -> assertThat(decimal(f.get("AmountToPay"))).isEqualByComparingTo(MONTANT),
                () -> assertThat(List.of("AmountVat", "FiscalStamp", "AmountRegul", "StockAmountToPay",
                        "StockAmountToPayHT", "WorkingOutAdvanceAmount", "RetainedAmount", "ProrataRefundAmount",
                        "FluctuationsToPay", "FluctuationsToPayHT"))
                        .isNotEmpty()
                        .allSatisfy(col -> assertThat(decimal(f.get(col))).as(col).isEqualByComparingTo("0")));
    }

    /** §3.2 : les 13 colonnes à DEFAULT(0) en prod et en CI sont écrites explicitement (yesRemsTest n'a aucun DEFAULT). */
    @Test
    void r3_2_facture_treizeColonnesAZeroExplicitement() throws Exception {
        valider(terrain, frais).andExpect(status().isOk());

        Tuple f = facture();
        assertThat(List.of("AdvanceRatio", "AdvanceAmount", "AdvanceRest", "WarrantyRatio", "WarrantyAmount",
                "ProportionRatio", "ProportionAmount", "InsuranceDECRatio", "InsuranceDECAmount", "InsuranceDECRest",
                "InsuranceTRCRatio", "InsuranceTRCAmount", "InsuranceTRCRest"))
                .hasSize(13)
                .allSatisfy(col -> assertThat(decimal(f.get(col))).as(col).isNotNull().isEqualByComparingTo("0"));
    }

    @Test
    void r3_2_facture_booleensChainesVidesEtNull() throws Exception {
        valider(terrain, frais).andExpect(status().isOk());

        Tuple f = facture();
        assertAll(
                () -> assertThat(f.get("IsSelected")).isEqualTo(false),
                () -> assertThat(f.get("IsClosed")).isEqualTo(false),
                () -> assertThat(f.get("CanBePayed")).isEqualTo(true),
                () -> assertThat(f.get("IsPropotionCharged")).isEqualTo(false),
                () -> assertThat(f.get("IsAccounted")).isEqualTo(false),
                () -> assertThat(((Number) f.get("LinesCount")).intValue()).isEqualTo(1),
                () -> assertThat(List.of("DocRef", "PersonToContact", "Notes2", "OfficeOrderRef"))
                        .isNotEmpty()
                        .allSatisfy(col -> assertThat(f.get(col)).as(col).isEqualTo("")),
                () -> assertThat(List.of("ComFolderId", "PrjProjectId", "PrjMarketId", "BuyFolderId", "BuyOrderId",
                        "BuyQuoteId", "BuyInvoiceId", "InvoiceSourceId", "RntFolderId", "SynFolderId", "State",
                        "IsRejected", "AmountToPay_OLD"))
                        .isNotEmpty()
                        .allSatisfy(col -> assertThat(f.get(col)).as(col).isNull()));
    }

    /** §3.2 : CfgTierTriggerId = AdmUser.CfgEmployeeId du compte connecté. */
    @Test
    void r3_2_cfgTierTrigger_employeDuCompte() throws Exception {
        UUID employe = terrains.fournisseur("EMP-" + c.jeton(), null);
        terrains.employeDuCompte(admin.getId(), employe);

        valider(terrain, frais).andExpect(status().isOk());

        assertThat(guid(facture().get("CfgTierTriggerId"))).isEqualTo(guid(employe));
    }

    // ------------------------------------------------------------------ §3.2 lignes compagnes

    @Test
    void r3_2_uneLigneDeFacture_conformeALaMesure() throws Exception {
        valider(terrain, frais).andExpect(status().isOk());

        List<Tuple> lignes = terrains.lignes("SELECT * FROM BuyDocumentLine WHERE BuyDocumentId = :id", factureId());
        assertThat(lignes).singleElement().satisfies(l -> assertAll(
                () -> assertThat(l.get("Code")).isEqualTo(type.code()),
                () -> assertThat(l.get("Description")).isEqualTo("Frais : " + type.description()),
                () -> assertThat(l.get("Unity")).isEqualTo("U"),
                () -> assertThat(decimal(l.get("Quantity"))).isEqualByComparingTo("1"),
                () -> assertThat(((Number) l.get("Sorting")).intValue()).isEqualTo(1),
                () -> assertThat(decimal(l.get("UnitPriceHT"))).isEqualByComparingTo(MONTANT),
                () -> assertThat(decimal(l.get("UnitPriceTTC"))).isEqualByComparingTo(MONTANT),
                () -> assertThat(decimal(l.get("VatRatio"))).isEqualByComparingTo("0"),
                () -> assertThat(decimal(l.get("DiscountRatio"))).isEqualByComparingTo("0"),
                () -> assertThat(decimal(l.get("FODECRatio"))).isEqualByComparingTo("0"),
                () -> assertThat(decimal(l.get("Coefficient"))).isEqualByComparingTo("1"),
                () -> assertThat(decimal(l.get("QuantityOld"))).isEqualByComparingTo("0"),
                () -> assertThat(decimal(l.get("QuantityOrigin"))).isEqualByComparingTo("0"),
                () -> assertThat(l.get("Notes")).isEqualTo(""),
                () -> assertThat(guid(l.get("StlCategoryId"))).isEqualTo(guid(type.categorieComptable())),
                () -> assertThat(l.get("UserCreate")).isEqualTo(AUTEUR)));
    }

    /** §3.2 : une ventilation, catégorie du type ; DocumentBase.Verif exige Σ VentilationRate = 1. */
    @Test
    void r3_2_ventilation_uneLigne_sommeDesTauxEgaleUn() throws Exception {
        valider(terrain, frais).andExpect(status().isOk());

        UUID facture = factureId();
        List<Tuple> ventilations =
                terrains.lignes("SELECT * FROM BuyVentilationCategory WHERE BuyDocumentId = :id", facture);
        Object somme = terrains.lignes("""
                SELECT SUM(VentilationRate) AS somme FROM BuyVentilationCategory WHERE BuyDocumentId = :id""", facture)
                .get(0).get("somme");
        assertAll(
                () -> assertThat(ventilations).singleElement()
                        .satisfies(v -> assertThat(guid(v.get("StlCategoryId"))).isEqualTo(guid(type.categorieComptable()))),
                () -> assertThat(decimal(somme)).isEqualByComparingTo("1"));
    }

    @Test
    void r3_2_historique_uneLigne_statutDeDepart() throws Exception {
        valider(terrain, frais).andExpect(status().isOk());

        List<Tuple> historique =
                terrains.lignes("SELECT * FROM BuyDocumentStatusHistory WHERE BuyDocumentId = :id", factureId());
        assertThat(historique).singleElement().satisfies(h -> assertAll(
                () -> assertThat(guid(h.get("BuyDocumentStatusId"))).isEqualTo(guid(STATUT_EN_COURS)),
                () -> assertThat(guid(h.get("AdmUserId"))).isEqualTo(guid(admin.getId())),
                () -> assertThat(h.get("Notes")).isEqualTo(""),
                () -> assertThat(h.get("buyDocumentWorkFlowId")).isNull(),
                () -> assertThat(horodatage(h.get("DocDate")).toLocalDate()).isEqualTo(MAINTENANT.toLocalDate())));
    }

    @Test
    void r3_2_frais_isValidatedEtTraceDeModification() throws Exception {
        valider(terrain, frais).andExpect(status().isOk());

        Tuple f = terrains.ligne(FRAIS_PAR_ID, frais);
        assertAll(
                () -> assertThat(f.get("IsValidated")).isEqualTo(true),
                () -> assertThat(f.get("UserUpdate")).isEqualTo(AUTEUR),
                () -> assertThat(f.get("UserUpdateDateTime")).isNotNull());
    }

    // ------------------------------------------------------------------ §3.3 code et séquence

    /** §3.3 : AA/NNNNN selon la ligne (année du Clock, 5 chiffres), SeqValue incrémentée de 1 exactement. */
    @Test
    void r3_3_code_selonLaLigneSequence_etIncrementDeUn() throws Exception {
        valider(terrain, frais).andExpect(status().isOk());

        assertAll(
                () -> assertThat(facture().get("Code")).isEqualTo("27/03014"),
                () -> assertThat(terrains.valeurSequence()).isEqualTo(3014));
    }

    /** §3.3 : préfixe, séparateur, largeur et suffixe lus dans la ligne, rien en dur. */
    @Test
    void r3_3_code_prefixeSeparateurSuffixeEtLargeurLusDansLaLigne() throws Exception {
        terrains.sequence(new Sequence(41, "FF", "-", "/T", 6, 2, true, 2021));

        valider(terrain, frais).andExpect(status().isOk());

        assertAll(
                () -> assertThat(facture().get("Code")).isEqualTo("FF27-000042/T"),
                () -> assertThat(terrains.valeurSequence()).isEqualTo(42));
    }

    /** §3.3 : BasedOnSystemYear = 0 → AA tiré de SeqYear. */
    @Test
    void r3_3_code_anneeDeSeqYear_siPasAnneeSysteme() throws Exception {
        terrains.sequence(new Sequence(3013, "", "/", "", 5, 2, false, 2021));

        valider(terrain, frais).andExpect(status().isOk());

        assertThat(facture().get("Code")).isEqualTo("21/03014");
    }

    // ------------------------------------------------------------------ §3.1.2 une seule facture par frais (409)

    @Test
    void r3_1_2_deuxiemeValidation_409_uneSeuleFacture_sequenceIncrementeeUneFois() throws Exception {
        valider(terrain, frais).andExpect(status().isOk());

        valider(terrain, frais).andExpect(status().isConflict())
                .andExpect(jsonPath(DETAIL).value(containsString(MESSAGE_DEJA_VALIDE)));

        assertAll(
                () -> assertThat(terrains.compter(NB_FACTURES, frais)).isEqualTo(1),
                () -> assertThat(terrains.valeurSequence()).isEqualTo(3014));
    }

    /** §3.1.2 : une BuyDocument porte déjà ce StkExpenseId (frais non validé, cas orphelin OMRAN) → 409. */
    @Test
    void r3_1_2_factureExistantePourCeFrais_409_rienDePlus() throws Exception {
        terrains.factureExistante(frais, "21/00004");

        valider(terrain, frais).andExpect(status().isConflict())
                .andExpect(jsonPath(DETAIL).value(containsString(MESSAGE_DEJA_VALIDE)));

        assertAll(
                () -> assertThat(terrains.compter(NB_FACTURES, frais)).isEqualTo(1),
                () -> assertThat(terrains.ligne(FRAIS_PAR_ID, frais).get("IsValidated")).isEqualTo(false),
                () -> assertThat(terrains.valeurSequence()).isEqualTo(3013));
    }

    @Test
    void r3_1_2_fraisDejaValide_409_rienEcrit() throws Exception {
        UUID dejaValide = terrains.frais(terrain, type.id(), fournisseur, MONTANT, true);

        valider(terrain, dejaValide).andExpect(status().isConflict())
                .andExpect(jsonPath(DETAIL).value(containsString(MESSAGE_DEJA_VALIDE)));

        assertAll(
                () -> assertThat(terrains.compter(NB_FACTURES, dejaValide)).isZero(),
                () -> assertThat(terrains.valeurSequence()).isEqualTo(3013));
    }

    // ------------------------------------------------------------------ §3.1.3 fournisseur, §3.1.4 type (400)

    /** §3.1.3 : fournisseur du frais absent → celui du terrain. */
    @Test
    void r3_1_3_fournisseurDuTerrainParDefaut() throws Exception {
        UUID proprietaire = terrains.fournisseur("PRO-" + c.jeton(), null);
        terrains.definir(terrain, "CfgSupplierId", proprietaire);
        UUID sansFournisseur = terrains.frais(terrain, type.id(), null, MONTANT, false);

        valider(terrain, sansFournisseur).andExpect(status().isOk());

        Tuple f = terrains.ligne(FACTURE_DU_FRAIS, sansFournisseur);
        assertAll(
                () -> assertThat(guid(f.get("CfgTierId"))).isEqualTo(guid(proprietaire)),
                () -> assertThat(f.get("SupplierDescription")).isEqualTo("Fournisseur PRO-" + c.jeton()));
    }

    @Test
    void r3_1_3_aucunFournisseurNiSurLeFraisNiSurLeTerrain_400_rienEcrit() throws Exception {
        frais = terrains.frais(terrain, type.id(), null, MONTANT, false);

        valider(terrain, frais).andExpect(status().isBadRequest());

        verifierRienEcrit();
    }

    /** §3.1.4 : type sans StlCategoryId → 400 nommant le type. */
    @Test
    void r3_1_4_typeSansCategorieComptable_400_nommeLeType_rienEcrit() throws Exception {
        TypeFrais incomplet = terrains.typeFrais("I" + c.jeton(), false, true);
        frais = terrains.frais(terrain, incomplet.id(), fournisseur, MONTANT, false);

        ResultActions resultat = valider(terrain, frais).andExpect(status().isBadRequest());

        assertThat(detail(resultat)).containsAnyOf(incomplet.code(), incomplet.description());
        verifierRienEcrit();
    }

    @Test
    void r3_1_4_typeSansTypeDeMarche_400_nommeLeType_rienEcrit() throws Exception {
        TypeFrais incomplet = terrains.typeFrais("M" + c.jeton(), true, false);
        frais = terrains.frais(terrain, incomplet.id(), fournisseur, MONTANT, false);

        ResultActions resultat = valider(terrain, frais).andExpect(status().isBadRequest());

        assertThat(detail(resultat)).containsAnyOf(incomplet.code(), incomplet.description());
        verifierRienEcrit();
    }

    // ------------------------------------------------------------------ §3.1.5 configuration lue en base

    /** §3.3 : ligne « Facture fournisseur » absente → 500, rien d'écrit, et surtout pas d'amorçage. */
    @Test
    void r3_3_sequenceAbsente_500_rienEcrit_pasDAmorcage() throws Exception {
        terrains.executer(
                "UPDATE AdmSequence SET Code = N'Facture fournisseur-MASQUEE' WHERE Code = N'Facture fournisseur'");

        valider(terrain, frais).andExpect(status().isInternalServerError());

        assertAll(
                () -> assertThat(terrains.compter(NB_FACTURES, frais)).isZero(),
                () -> assertThat(terrains.ligne(FRAIS_PAR_ID, frais).get("IsValidated")).isEqualTo(false),
                () -> assertThat(terrains.compter(
                        "SELECT COUNT(*) FROM AdmSequence WHERE Code = N'Facture fournisseur' AND PKey <> :id",
                        UUID.randomUUID())).isZero());
    }

    static Stream<Arguments> configurationsManquantes() {
        return Stream.of(
                Arguments.of("module STL", "UPDATE AdmModule SET Code = N'STL-MASQUE' WHERE Code = N'STL'"),
                Arguments.of("entite StkInvoiceDebitLand", """
                        UPDATE StrEntity SET Code = N'StkInvoiceDebitLand-MASQUEE' WHERE Code = N'StkInvoiceDebitLand'"""),
                Arguments.of("devise DefaultCurrency", """
                        UPDATE AdmParamsGuid SET Code = N'DefaultCurrency-MASQUEE' WHERE Code = N'DefaultCurrency'"""));
    }

    /** §3.1.5 : ligne de configuration absente → ConfigurationManquanteException (500), rien d'écrit, pas d'amorçage. */
    @ParameterizedTest(name = "{0}")
    @MethodSource("configurationsManquantes")
    void r3_1_5_configurationManquante_500_rienEcrit(String configuration, String masquer) throws Exception {
        terrains.executer(masquer);
        assertThat(configuration).isNotBlank();

        valider(terrain, frais).andExpect(status().isInternalServerError());

        verifierRienEcrit();
    }

    @Test
    void r3_1_5_aucuneLigneDeWorkflowPourLEntite_500_rienEcrit() throws Exception {
        if (terrains.tableExiste("BuyDocumentWorkFlow")) {
            terrains.executer("""
                    DELETE FROM BuyDocumentWorkFlow
                    WHERE StrEntityId IN (SELECT PKey FROM StrEntity WHERE Code = N'StkInvoiceDebitLand')""");
        }

        valider(terrain, frais).andExpect(status().isInternalServerError());

        verifierRienEcrit();
    }

    /** §3.1.5 : statut de départ = StartStatusId d'une ligne qui n'est EndStatusId d'aucune autre, lu en base. */
    @Test
    void r3_1_5_statutDeDepart_luDansLeWorkflowDeLEntite() throws Exception {
        UUID brouillon = terrains.statutFacture("BROUILLON-" + c.jeton());
        terrains.transitionFacture(UUID.randomUUID(), brouillon, STATUT_EN_COURS);

        valider(terrain, frais).andExpect(status().isOk());

        assertAll(
                () -> assertThat(guid(facture().get("BuyDocumentStatusId"))).isEqualTo(guid(brouillon)),
                () -> assertThat(terrains.lignes("SELECT * FROM BuyDocumentStatusHistory WHERE BuyDocumentId = :id",
                        factureId())).singleElement()
                        .satisfies(h -> assertThat(guid(h.get("BuyDocumentStatusId"))).isEqualTo(guid(brouillon))));
    }

    /** §3.1.5 : devise = AdmParamsGuid DefaultCurrency, pas la devise de la société. */
    @Test
    void r3_1_5_devise_parDefaut_pasCelleDeLaSociete() throws Exception {
        entites.deviseDeLEntite(c.entite().getId(), entites.devise("EUR-" + c.jeton()));

        valider(terrain, frais).andExpect(status().isOk());

        assertThat(guid(facture().get("StlCurrencyId"))).isEqualTo(guid(terrains.deviseParDefaut()));
    }

    // ------------------------------------------------------------------ §3.1.1 404, §0 403

    @Test
    void r3_1_1_fraisDUnAutreTerrain_404_rienEcrit() throws Exception {
        UUID autreTerrain = terrains.terrain(c, "TER2-" + c.jeton());
        mockMvc.perform(get("/api/terrains/{id}", autreTerrain).with(connecte(admin))).andExpect(status().isOk());

        valider(autreTerrain, frais).andExpect(status().isNotFound());

        verifierRienEcrit();
    }

    @Test
    void r3_1_1_fraisInconnu_404() throws Exception {
        mockMvc.perform(get("/api/terrains/{id}", terrain).with(connecte(admin))).andExpect(status().isOk());

        valider(terrain, UUID.randomUUID()).andExpect(status().isNotFound());
    }

    @Test
    void r3_1_1_terrainDUneSocieteInactive_404_rienEcrit() throws Exception {
        mockMvc.perform(get("/api/terrains/{id}", terrain).with(connecte(admin))).andExpect(status().isOk());
        entites.actif(c.entite().getId(), false);

        valider(terrain, frais).andExpect(status().isNotFound());

        verifierRienEcrit();
    }

    @Test
    void r0_nonAdministrateur_403_rienEcrit() throws Exception {
        AuthenticatedUser principal = new AuthenticatedUser(admin.getId(), admin.getCode(), null, false, true, 0);

        mockMvc.perform(post(VALIDER, terrain, frais).with(authentication(new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_Commercial"))))))
                .andExpect(status().isForbidden());

        verifierRienEcrit();
    }
}

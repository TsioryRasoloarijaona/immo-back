package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkBuilding;
import com.tsix.immocrm.entities.stk.StkFloor;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.AUTEUR;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.ENTITE_STK_ITEM;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.NATURE_BAT;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.VOCATION_PROJET;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.VOCATION_VENTE;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Article principal en écriture — FEATURE-patrimoine-ecriture.md §2, §5, §11 (ArticleEcritureTest).
 * Cas chiffré §5.5 (ART344) : 63 m², 10 833,333333 HT/m², TVA 0,20 → 13 000 TTC/m², 819 000 TTC.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ArticleEcritureTest {

    private static final String ARTICLES = "/api/articles";
    private static final String ARTICLE = "/api/articles/{id}";
    private static final String TRANCHE_ID = "trancheId";
    private static final String CODE = "code";
    private static final String DESCRIPTION = "description";
    private static final String CATEGORIE_ID = "categorieId";
    private static final String TYPE_ID = "typeId";
    private static final String VOCATION_ID = "vocationId";
    private static final String SURFACE_PROVISOIRE = "surfaceProvisoire";
    private static final String SURFACE_DEFINITIVE = "surfaceDefinitive";
    private static final String PRIX_PROVISOIRE = "prixM2ProvisoireHT";
    private static final String PRIX_DEFINITIF = "prixM2DefinitifHT";
    private static final String PRIX_PROVISOIRE_TTC = "prixM2ProvisoireTTC";
    private static final String PRIX_DEFINITIF_TTC = "prixM2DefinitifTTC";
    private static final String TAUX_TVA = "tauxTva";
    private static final String PRIX_ART344_HT = "10833.333333";
    private static final String PRIX_M2_TTC = "13000";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private StkItemRepository stkItemRepository;

    private AdmUser admin;
    private CfgProject projet;
    private CfgTranche tranche;
    private StkBuilding immeuble;
    private StkFloor etage;
    private StkItemCategory categorie;
    private StkItemType type;

    @BeforeEach
    void setUp() {
        fixture.referentielArticle();
        admin = fixture.administrateur();
        CfgCompany entite = fixture.entite("ENT-ART-ECR", true);
        projet = fixture.projet("PRJ-ART-ECR", entite);
        tranche = fixture.tranche("TR-ART-ECR", projet);
        immeuble = fixture.immeuble("IMM-ART-ECR");
        etage = fixture.etage("ETG-ART-ECR");
        categorie = fixture.categorie("CAT-ART-ECR", fixture.natureBat(), false);
        type = fixture.type("TYP-ART-ECR", fixture.natureBat(), false);
    }

    /** Cas fictif du §5.5 en saisie TTC (§14.1) : 63 m² à 13 000 TTC/m², TVA 0,20 → 819 000 TTC / 682 500 HT. */
    private CorpsJson corpsArticle(String code) {
        return corps().avec(TRANCHE_ID, tranche.getId()).avec(CODE, code).avec(DESCRIPTION, "Article " + code)
                .avec(VOCATION_ID, VOCATION_VENTE).avec(CATEGORIE_ID, categorie.getId()).avec(TYPE_ID, type.getId())
                .avec("immeubleId", immeuble.getId()).avec("etageId", etage.getId())
                .avec(SURFACE_PROVISOIRE, new BigDecimal("63")).avec(SURFACE_DEFINITIVE, new BigDecimal("63"))
                .avec(PRIX_PROVISOIRE_TTC, new BigDecimal(PRIX_M2_TTC))
                .avec(PRIX_DEFINITIF_TTC, new BigDecimal(PRIX_M2_TTC))
                .avec(TAUX_TVA, new BigDecimal("0.20"));
    }

    /** Même corps, prix au m² saisis HT (§14.1 : un seul sens par paire provisoire / définitive). */
    private CorpsJson enHT(CorpsJson corps, String prixProvisoireHT, String prixDefinitifHT) {
        return corps.sans(PRIX_PROVISOIRE_TTC).sans(PRIX_DEFINITIF_TTC)
                .avec(PRIX_PROVISOIRE, new BigDecimal(prixProvisoireHT))
                .avec(PRIX_DEFINITIF, new BigDecimal(prixDefinitifHT));
    }

    private ResultActions creer(CorpsJson corps) throws Exception {
        return mockMvc.perform(post(ARTICLES).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private ResultActions modifier(UUID id, CorpsJson corps) throws Exception {
        return mockMvc.perform(patch(ARTICLE, id).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private UUID creerOk(CorpsJson corps) throws Exception {
        String json = creer(corps).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    private UUID creerOccupe(String code) throws Exception {
        UUID id = creerOk(corpsArticle(code));
        ComFolder dossier = fixture.dossier("DOS-" + code);
        StkItem article = stkItemRepository.findById(id).orElseThrow();
        article.setComFolderId(dossier.getId());
        stkItemRepository.saveAndFlush(article);
        return id;
    }

    // ------------------------------------------------------------------ §5.5 prix calculés

    /**
     * §14.1, contrôle prod ART344 : 69 m², saisie TTC 13 000, TVA 0,20. Le HT exact (747 500,000000) n'est atteint que
     * si UnitHT = 13 000 / 1,2 reste non arrondi jusqu'au stockage (10 833,333333 × 69 donnerait 747 499,999977).
     */
    @Test
    void r14_1_art344Prod_saisieTTC_prixExacts() throws Exception {
        Tuple a = fixture.ligneArticle(creerOk(corpsArticle("ART344-PROD")
                .avec(SURFACE_PROVISOIRE, new BigDecimal("69")).avec(SURFACE_DEFINITIVE, new BigDecimal("69"))));

        assertAll(
                () -> assertThat(decimal(a.get("Area"))).isEqualByComparingTo("69"),
                () -> assertThat(decimal(a.get("AreaFloor"))).isEqualByComparingTo("69"),
                () -> assertThat(decimal(a.get("Vat"))).isEqualByComparingTo("0.2"),
                () -> assertThat(decimal(a.get("ComSaleUnitCost"))).isEqualByComparingTo("13000"),
                () -> assertThat(decimal(a.get("ComSaleUnitCostFloor"))).isEqualByComparingTo("13000"),
                () -> assertThat(decimal(a.get("ComSaleUnitCostHT"))).isEqualByComparingTo(PRIX_ART344_HT),
                () -> assertThat(decimal(a.get("ComSaleUnitCostFloorHT"))).isEqualByComparingTo(PRIX_ART344_HT),
                () -> assertThat(decimal(a.get("SalePrice"))).isEqualByComparingTo("897000"),
                () -> assertThat(decimal(a.get("ComSalePrice"))).isEqualByComparingTo("897000"),
                () -> assertThat(decimal(a.get("SalePriceFloor"))).isEqualByComparingTo("897000"),
                () -> assertThat(decimal(a.get("ComSalePriceFloor"))).isEqualByComparingTo("897000"),
                () -> assertThat(decimal(a.get("SalePriceHT"))).isEqualByComparingTo("747500.000000"),
                () -> assertThat(decimal(a.get("ComSalePriceHT"))).isEqualByComparingTo("747500.000000"),
                () -> assertThat(decimal(a.get("SalePriceFloorHT"))).isEqualByComparingTo("747500.000000"),
                () -> assertThat(decimal(a.get("ComSalePriceFloorHT"))).isEqualByComparingTo("747500.000000"));
    }

    /** §14.1, cas fictif §5.5 en saisie TTC : 63 m² × 13 000 = 819 000 TTC ; HT = (13 000 / 1,2) × 63 = 682 500. */
    @Test
    void r14_1_saisieTTC_cas63m2_prixExacts() throws Exception {
        Tuple a = fixture.ligneArticle(creerOk(corpsArticle("ART-ECR-TTC63")));

        assertAll(
                () -> assertThat(decimal(a.get("ComSaleUnitCostHT"))).isEqualByComparingTo(PRIX_ART344_HT),
                () -> assertThat(decimal(a.get("SalePrice"))).isEqualByComparingTo("819000"),
                () -> assertThat(decimal(a.get("ComSalePrice"))).isEqualByComparingTo("819000"),
                () -> assertThat(decimal(a.get("SalePriceFloor"))).isEqualByComparingTo("819000"),
                () -> assertThat(decimal(a.get("SalePriceHT"))).isEqualByComparingTo("682500"),
                () -> assertThat(decimal(a.get("ComSalePriceHT"))).isEqualByComparingTo("682500"),
                () -> assertThat(decimal(a.get("SalePriceFloorHT"))).isEqualByComparingTo("682500"));
    }

    /**
     * §14.1, saisie HT 10 833,333333 sur 63 m² : UnitTTC = 12 999,9999996 (stocké 13 000,000000), PrixHT = 682 499,999979,
     * PrixTTC = 12 999,9999996 × 63 = 818 999,9999748 → 818 999,999975 (arrondi au stockage seulement).
     */
    @Test
    void r14_1_saisieHT_cas63m2_prixExacts() throws Exception {
        Tuple a = fixture.ligneArticle(creerOk(enHT(corpsArticle("ART-ECR-HT63"), PRIX_ART344_HT, PRIX_ART344_HT)));

        assertAll(
                () -> assertThat(decimal(a.get("ComSaleUnitCostHT"))).isEqualByComparingTo(PRIX_ART344_HT),
                () -> assertThat(decimal(a.get("ComSaleUnitCostFloorHT"))).isEqualByComparingTo(PRIX_ART344_HT),
                () -> assertThat(decimal(a.get("ComSaleUnitCost"))).isEqualByComparingTo("13000.000000"),
                () -> assertThat(decimal(a.get("ComSaleUnitCostFloor"))).isEqualByComparingTo("13000.000000"),
                () -> assertThat(decimal(a.get("SalePriceHT"))).isEqualByComparingTo("682499.999979"),
                () -> assertThat(decimal(a.get("ComSalePriceHT"))).isEqualByComparingTo("682499.999979"),
                () -> assertThat(decimal(a.get("SalePriceFloorHT"))).isEqualByComparingTo("682499.999979"),
                () -> assertThat(decimal(a.get("ComSalePriceFloorHT"))).isEqualByComparingTo("682499.999979"),
                () -> assertThat(decimal(a.get("SalePrice"))).isEqualByComparingTo("818999.999975"),
                () -> assertThat(decimal(a.get("ComSalePrice"))).isEqualByComparingTo("818999.999975"),
                () -> assertThat(decimal(a.get("SalePriceFloor"))).isEqualByComparingTo("818999.999975"),
                () -> assertThat(decimal(a.get("ComSalePriceFloor"))).isEqualByComparingTo("818999.999975"));
    }

    @Test
    void r14_1_definitifSaisiHTetTTC_400() throws Exception {
        creer(corpsArticle("ART-ECR-DEUX-DEF").avec(PRIX_DEFINITIF, new BigDecimal(PRIX_ART344_HT)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void r14_1_provisoireSaisiHTetTTC_400() throws Exception {
        creer(corpsArticle("ART-ECR-DEUX-PROV").avec(PRIX_PROVISOIRE, new BigDecimal(PRIX_ART344_HT)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void r14_1_patch_prixSaisiHTetTTC_400() throws Exception {
        UUID id = creerOk(corpsArticle("ART-ECR-PATCH-DEUX"));

        modifier(id, corps().avec(PRIX_DEFINITIF, new BigDecimal("10000")).avec(PRIX_DEFINITIF_TTC, new BigDecimal("12000")))
                .andExpect(status().isBadRequest());
    }

    /** Provisoire saisi HT, définitif saisi TTC : chaque paire suit son propre sens de saisie. */
    @Test
    void r5_5_provisoireEtDefinitifCalculesSeparement() throws Exception {
        Tuple a = fixture.ligneArticle(creerOk(corpsArticle("ART-ECR-PROV").sans(PRIX_PROVISOIRE_TTC)
                .avec(SURFACE_PROVISOIRE, new BigDecimal("60")).avec(PRIX_PROVISOIRE, new BigDecimal("10000"))));

        assertAll(
                () -> assertThat(decimal(a.get("AreaFloor"))).isEqualByComparingTo("60"),
                () -> assertThat(decimal(a.get("ComSaleUnitCostFloorHT"))).isEqualByComparingTo("10000"),
                () -> assertThat(decimal(a.get("ComSaleUnitCostFloor"))).isEqualByComparingTo("12000"),
                () -> assertThat(decimal(a.get("SalePriceFloorHT"))).isEqualByComparingTo("600000"),
                () -> assertThat(decimal(a.get("SalePriceFloor"))).isEqualByComparingTo("720000"),
                () -> assertThat(decimal(a.get("ComSalePriceFloorHT"))).isEqualByComparingTo("600000"),
                () -> assertThat(decimal(a.get("ComSalePriceFloor"))).isEqualByComparingTo("720000"),
                () -> assertThat(decimal(a.get("Area"))).isEqualByComparingTo("63"),
                () -> assertThat(decimal(a.get("SalePrice"))).isEqualByComparingTo("819000"),
                () -> assertThat(decimal(a.get("ComSalePrice"))).isEqualByComparingTo("819000"));
    }

    @Test
    void r5_5_tvaZero_ttcEgalHt() throws Exception {
        Tuple a = fixture.ligneArticle(creerOk(enHT(corpsArticle("ART-ECR-TVA0"), "8000.5", "8000.5")
                .avec(TAUX_TVA, BigDecimal.ZERO)
                .avec(SURFACE_PROVISOIRE, new BigDecimal("50")).avec(SURFACE_DEFINITIVE, new BigDecimal("50"))));

        assertAll(
                () -> assertThat(decimal(a.get("ComSaleUnitCost"))).isEqualByComparingTo("8000.5"),
                () -> assertThat(decimal(a.get("SalePrice"))).isEqualByComparingTo("400025"),
                () -> assertThat(decimal(a.get("SalePriceHT"))).isEqualByComparingTo("400025"),
                () -> assertThat(decimal(a.get("ComSalePrice"))).isEqualByComparingTo("400025"),
                () -> assertThat(decimal(a.get("ComSalePriceHT"))).isEqualByComparingTo("400025"));
    }

    /** §5.5 : échelle 6, HALF_UP — 1,000003 × 1,5 = 1,5000045 → 1,500005 (HALF_EVEN donnerait 1,500004). */
    @Test
    void r5_5_arrondiEchelle6HalfUp() throws Exception {
        Tuple a = fixture.ligneArticle(creerOk(enHT(corpsArticle("ART-ECR-ARRONDI"), "1.000003", "1.000003")
                .avec(TAUX_TVA, new BigDecimal("0.5"))
                .avec(SURFACE_PROVISOIRE, BigDecimal.ONE).avec(SURFACE_DEFINITIVE, BigDecimal.ONE)));

        assertAll(
                () -> assertThat(decimal(a.get("ComSaleUnitCost"))).isEqualByComparingTo("1.500005"),
                () -> assertThat(decimal(a.get("ComSaleUnitCostFloor"))).isEqualByComparingTo("1.500005"),
                () -> assertThat(decimal(a.get("SalePrice"))).isEqualByComparingTo("1.500005"));
    }

    /** §14.1 : UnitHT = 1 / 1,2 = 0,8333…, stocké 0,833333 ; PrixHT = 0,8333… × 3 = 2,500000 (et non 0,833333 × 3 = 2,499999). */
    @Test
    void r14_1_saisieTTC_htArrondiAuStockageSeulement() throws Exception {
        Tuple a = fixture.ligneArticle(creerOk(corpsArticle("ART-ECR-TTC-ARR")
                .avec(SURFACE_PROVISOIRE, new BigDecimal("3")).avec(SURFACE_DEFINITIVE, new BigDecimal("3"))
                .avec(PRIX_PROVISOIRE_TTC, BigDecimal.ONE).avec(PRIX_DEFINITIF_TTC, BigDecimal.ONE)));

        assertAll(
                () -> assertThat(decimal(a.get("ComSaleUnitCost"))).isEqualByComparingTo("1"),
                () -> assertThat(decimal(a.get("ComSaleUnitCostHT"))).isEqualByComparingTo("0.833333"),
                () -> assertThat(decimal(a.get("SalePrice"))).isEqualByComparingTo("3"),
                () -> assertThat(decimal(a.get("SalePriceHT"))).isEqualByComparingTo("2.500000"),
                () -> assertThat(decimal(a.get("SalePriceFloorHT"))).isEqualByComparingTo("2.500000"));
    }

    @Test
    void r14_1_patch_saisieTTC_recalculeLeHT() throws Exception {
        UUID id = creerOk(corpsArticle("ART-ECR-PATCH-TTC"));

        modifier(id, corps().avec(PRIX_DEFINITIF_TTC, new BigDecimal("12000"))).andExpect(status().isOk());

        Tuple a = fixture.ligneArticle(id);
        assertAll(
                () -> assertThat(decimal(a.get("ComSaleUnitCost"))).isEqualByComparingTo("12000"),
                () -> assertThat(decimal(a.get("ComSaleUnitCostHT"))).isEqualByComparingTo("10000"),
                () -> assertThat(decimal(a.get("SalePrice"))).isEqualByComparingTo("756000"),
                () -> assertThat(decimal(a.get("SalePriceHT"))).isEqualByComparingTo("630000"));
    }

    @Test
    void r5_5_patch_prixRecalcule() throws Exception {
        UUID id = creerOk(corpsArticle("ART-ECR-RECALC"));

        modifier(id, corps().avec(PRIX_DEFINITIF, new BigDecimal("10000"))).andExpect(status().isOk());

        Tuple a = fixture.ligneArticle(id);
        assertAll(
                () -> assertThat(decimal(a.get("ComSaleUnitCost"))).isEqualByComparingTo("12000"),
                () -> assertThat(decimal(a.get("SalePrice"))).isEqualByComparingTo("756000"),
                () -> assertThat(decimal(a.get("ComSalePrice"))).isEqualByComparingTo("756000"),
                () -> assertThat(decimal(a.get("ComSalePriceFloor"))).as("le provisoire ne bouge pas")
                        .isEqualByComparingTo("819000"));
    }

    // ------------------------------------------------------------------ §5.3 colonnes posées par le serveur

    @Test
    void r5_3_creation_colonnesPoseesParLeServeur() throws Exception {
        Tuple a = fixture.ligneArticle(creerOk(corpsArticle("ART-ECR-SERVEUR")));

        assertAll(
                () -> assertThat(guid(a.get("StrEntityId"))).isEqualTo(guid(ENTITE_STK_ITEM)),
                () -> assertThat(guid(a.get("StkKindId"))).isEqualTo(guid(NATURE_BAT)),
                () -> assertThat(guid(a.get("StkHierarchyId"))).isEqualTo(guid(tranche.getId())),
                () -> assertThat(guid(a.get("StkVocationId"))).isEqualTo(guid(VOCATION_VENTE)),
                () -> assertThat(guid(a.get("StkBuildingId"))).isEqualTo(guid(immeuble.getId())),
                () -> assertThat(guid(a.get("StkFloorId"))).isEqualTo(guid(etage.getId())),
                () -> assertThat(a.get("IsTitled")).isEqualTo(true),
                () -> assertThat(a.get("IsUnderItem")).isEqualTo(false),
                () -> assertThat(a.get("IsUnderItemOrigin")).isEqualTo(false),
                () -> assertThat(a.get("AlwaysAffected")).isEqualTo(false),
                () -> assertThat(a.get("IsAvailableForSaleInWebSite")).isEqualTo(false),
                () -> assertThat(a.get("IsAvailableForRentInWebSite")).isEqualTo(false));
    }

    @Test
    void r5_3_creation_colonnesLaisseesNulles() throws Exception {
        Tuple a = fixture.ligneArticle(creerOk(corpsArticle("ART-ECR-NULLES")));

        assertThat(List.of("ParentId", "CfgCompanyId", "CfgProjectId", "ComFolderId", "RntFolderId", "SynFolderId",
                "DocDate", "StkPresentationTypeId", "StkOrientationId", "StkFacadeId", "StkCostCategoryId",
                "CfgLawyerId"))
                .isNotEmpty()
                .allSatisfy(colonne -> assertThat(a.get(colonne)).as(colonne).isNull());
    }

    /** §2.4 : toute colonne non NULL sur la ligne modèle ART344 de yesRemsProd est non NULL sur nos lignes. */
    @Test
    void colonnesNonNullesCommeOmran() throws Exception {
        Tuple a = fixture.ligneArticle(creerOk(corpsArticle("ART-ECR-NONNUL")));

        assertThat(List.of("PKey", "Code", "Description", "Address", "Area", "Notes", "ComSaleUnitCost",
                "ComSaleMinAdvance", "ComSalePrice", "SalePrice", "Contenance", "IsUnderItem", "AreaFloor",
                "AlwaysAffected", "ElectricityRef", "ElectricityMeter", "ElectricityMeterIndex",
                "ElectricityMeterPolice", "GasMeter", "GasMeterIndex", "GasMeterPolice", "WaterMeterPolice",
                "WaterMeter", "WaterMeterIndex", "Anomalies", "Cos", "Cuf", "AlinmentStop", "NeighborStop",
                "MaxHeight", "RegistryNumber", "RegistryPlace", "ImmatriculationCPF", "AmountHT", "CautionNumber",
                "StkItemTypeId", "StkItemCategoryId", "StkVocationId", "StkKindId", "UserCreate",
                "UserCreateDateTime", "UserUpdate", "UserUpdateDateTime", "WaterRef", "GasRef", "TitleNumberMother",
                "TitleNumber", "TitleName", "TitleNotes", "StkFloorId", "StkBuildingId", "StkHierarchyId",
                "PropertyCalled", "FractionDivided", "DividedPart", "UndividedPart", "Vat", "AreaCos", "AreaCuf",
                "ComSaleUnitCostFloor", "SalePriceFloor", "ComSalePriceFloor", "IsUnderItemOrigin",
                "FractionDividedLand", "DiscountMax", "ComSaleUnitCostFloorHT", "ComSaleUnitCostHT",
                "GroupLandQuota", "SalePriceHT", "ComSalePriceHT", "SalePriceFloorHT", "ComSalePriceFloorHT",
                "IsAvailableForRentInWebSite", "IsAvailableForSaleInWebSite", "IsTitled", "InteriorArea",
                "WallsArea", "CommonPartsArea", "DiscountMax2", "StrEntityId", "MapLatitude", "MapLongitude"))
                .hasSize(82)
                .allSatisfy(colonne -> assertThat(a.get(colonne)).as(colonne).isNotNull());
    }

    @Test
    void r5_2_creation_champsFacultatifsEcritsDansLeursColonnes() throws Exception {
        Tuple a = fixture.ligneArticle(creerOk(corpsArticle("ART-ECR-FACULT")
                .avec("titreFoncier", "TF-123").avec("estTitre", false)
                .avec("remiseMax1", new BigDecimal("0.05")).avec("remiseMax2", new BigDecimal("0.1"))
                .avec("adresse", "Bloc A").avec("notes", "Vue mer")
                .avec("surfaceInterieure", new BigDecimal("55")).avec("surfaceMurs", new BigDecimal("4"))
                .avec("surfacePartiesCommunes", new BigDecimal("4"))));

        assertAll(
                () -> assertThat(a.get("TitleNumber")).isEqualTo("TF-123"),
                () -> assertThat(a.get("IsTitled")).isEqualTo(false),
                () -> assertThat(decimal(a.get("DiscountMax"))).isEqualByComparingTo("0.05"),
                () -> assertThat(decimal(a.get("DiscountMax2"))).isEqualByComparingTo("0.1"),
                () -> assertThat(a.get("Address")).isEqualTo("Bloc A"),
                () -> assertThat(a.get("Notes")).isEqualTo("Vue mer"),
                () -> assertThat(decimal(a.get("InteriorArea"))).isEqualByComparingTo("55"),
                () -> assertThat(decimal(a.get("WallsArea"))).isEqualByComparingTo("4"),
                () -> assertThat(decimal(a.get("CommonPartsArea"))).isEqualByComparingTo("4"));
    }

    @Test
    void r2_3_creation_trace() throws Exception {
        Tuple a = fixture.ligneArticle(creerOk(corpsArticle("ART-ECR-TRACE")));

        assertAll(
                () -> assertThat(a.get("UserCreate")).isEqualTo(AUTEUR),
                () -> assertThat(a.get("UserUpdate")).isEqualTo(AUTEUR),
                () -> assertThat(a.get("UserCreateDateTime")).isNotNull(),
                () -> assertThat(a.get("UserUpdateDateTime")).isEqualTo(a.get("UserCreateDateTime")));
    }

    @Test
    void r10_creation_reponseExposeLesNouveauxChamps() throws Exception {
        creer(corpsArticle("ART-ECR-REPONSE").avec(SURFACE_PROVISOIRE, new BigDecimal("60")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("ART-ECR-REPONSE"))
                .andExpect(jsonPath("$.trancheId").value(tranche.getId().toString()))
                .andExpect(jsonPath("$.area").value(63))
                .andExpect(jsonPath("$.surfaceProvisoire").value(60))
                .andExpect(jsonPath("$.tauxTVA").value(0.2))
                .andExpect(jsonPath("$.prixM2DefinitifHT").value(10833.333333))
                .andExpect(jsonPath("$.prixM2DefinitifTTC").value(13000))
                .andExpect(jsonPath("$.prixM2ProvisoireHT").value(10833.333333))
                .andExpect(jsonPath("$.prixM2ProvisoireTTC").value(13000))
                // §14.7 : prixArticle* = SalePrice* (article seul), prixVente* = ComSalePrice* (avec sous-articles).
                .andExpect(jsonPath("$.prixArticleProvisoireTTC").value(780000))
                .andExpect(jsonPath("$.prixArticleProvisoireHT").value(650000))
                .andExpect(jsonPath("$.prixArticleDefinitifTTC").value(819000))
                .andExpect(jsonPath("$.prixArticleDefinitifHT").value(682500))
                .andExpect(jsonPath("$.prixVenteProvisoireTTC").value(780000))
                .andExpect(jsonPath("$.prixVenteProvisoireHT").value(650000))
                .andExpect(jsonPath("$.prixVenteDefinitifTTC").value(819000))
                .andExpect(jsonPath("$.prixVenteDefinitifHT").value(682500))
                .andExpect(jsonPath("$.immeubleId").value(immeuble.getId().toString()))
                .andExpect(jsonPath("$.etageId").value(etage.getId().toString()))
                .andExpect(jsonPath("$.estTitre").value(true))
                .andExpect(jsonPath("$.estSousArticle").value(false))
                .andExpect(jsonPath("$.toujoursAffecte").value(false))
                .andExpect(jsonPath("$.occupe").value(false))
                .andExpect(jsonPath("$.creePar").value(AUTEUR))
                .andExpect(jsonPath("$.modifiePar").value(AUTEUR));
    }

    // ------------------------------------------------------------------ §14.3 défauts appliqués (pas de 400)

    @Test
    void r14_3_vocationAbsente_defautVente() throws Exception {
        Tuple a = fixture.ligneArticle(creerOk(corpsArticle("ART-ECR-DEF-VOC").sans(VOCATION_ID)));

        assertThat(guid(a.get("StkVocationId"))).isEqualTo(guid(VOCATION_VENTE));
    }

    @Test
    void r14_3_vocationNull_defautVente() throws Exception {
        Tuple a = fixture.ligneArticle(creerOk(corpsArticle("ART-ECR-DEF-VOC-NULL").avec(VOCATION_ID, null)));

        assertThat(guid(a.get("StkVocationId"))).isEqualTo(guid(VOCATION_VENTE));
    }

    @Test
    void r14_3_surfacesAbsentes_provisoire1_definitiveEgaleProvisoire() throws Exception {
        Tuple a = fixture.ligneArticle(creerOk(corpsArticle("ART-ECR-DEF-SURF")
                .sans(SURFACE_PROVISOIRE).sans(SURFACE_DEFINITIVE)));

        assertAll(
                () -> assertThat(decimal(a.get("AreaFloor"))).isEqualByComparingTo("1"),
                () -> assertThat(decimal(a.get("Area"))).isEqualByComparingTo("1"),
                () -> assertThat(decimal(a.get("SalePrice"))).isEqualByComparingTo(PRIX_M2_TTC));
    }

    @Test
    void r14_3_surfaceDefinitiveAbsente_egaleALaProvisoire() throws Exception {
        Tuple a = fixture.ligneArticle(creerOk(corpsArticle("ART-ECR-DEF-SURFDEF")
                .avec(SURFACE_PROVISOIRE, new BigDecimal("50")).sans(SURFACE_DEFINITIVE)));

        assertThat(decimal(a.get("Area"))).isEqualByComparingTo("50");
    }

    @Test
    void r14_3_prixAbsents_zero() throws Exception {
        Tuple a = fixture.ligneArticle(creerOk(corpsArticle("ART-ECR-DEF-PRIX")
                .sans(PRIX_PROVISOIRE_TTC).sans(PRIX_DEFINITIF_TTC)));

        assertThat(List.of("ComSaleUnitCostHT", "ComSaleUnitCost", "ComSaleUnitCostFloorHT", "ComSaleUnitCostFloor",
                "SalePrice", "SalePriceHT", "SalePriceFloor", "SalePriceFloorHT", "ComSalePrice", "ComSalePriceHT",
                "ComSalePriceFloor", "ComSalePriceFloorHT"))
                .isNotEmpty()
                .allSatisfy(colonne -> assertThat(decimal(a.get(colonne))).as(colonne).isEqualByComparingTo("0"));
    }

    // ------------------------------------------------------------------ §5.4.1 / §5.2 refus 400

    @Test
    void r5_4_1_surfaceProvisoireZero_400_messageOmran() throws Exception {
        creer(corpsArticle("ART-ECR-SURF0").avec(SURFACE_PROVISOIRE, BigDecimal.ZERO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("strictement supérieure à 0")));
    }

    @Test
    void r5_4_1_surfaceDefinitiveZero_400() throws Exception {
        creer(corpsArticle("ART-ECR-SURFDEF0").avec(SURFACE_DEFINITIVE, BigDecimal.ZERO))
                .andExpect(status().isBadRequest());
    }

    @Test
    void r5_4_1_immeubleManquant_400() throws Exception {
        creer(corpsArticle("ART-ECR-SANS-IMM").sans("immeubleId")).andExpect(status().isBadRequest());
    }

    @Test
    void r5_4_1_etageManquant_400() throws Exception {
        creer(corpsArticle("ART-ECR-SANS-ETG").sans("etageId")).andExpect(status().isBadRequest());
    }

    @Test
    void r5_4_1_categoriePourSousArticle_400() throws Exception {
        StkItemCategory pourSous = fixture.categorie("CAT-ART-ECR-SOUS", fixture.natureBat(), true);

        creer(corpsArticle("ART-ECR-CAT-SOUS").avec(CATEGORIE_ID, pourSous.getId())).andExpect(status().isBadRequest());
    }

    @Test
    void r5_2_typePourSousArticle_400() throws Exception {
        StkItemType pourSous = fixture.type("TYP-ART-ECR-SOUS", fixture.natureBat(), true);

        creer(corpsArticle("ART-ECR-TYP-SOUS").avec(TYPE_ID, pourSous.getId())).andExpect(status().isBadRequest());
    }

    @Test
    void r5_4_1_categorieDUneAutreNatureQueBat_400() throws Exception {
        StkKind terrain = fixture.nature(UUID.randomUUID(), "TER-ART-ECR");
        StkItemCategory categorieTerrain = fixture.categorie("CAT-ART-ECR-TER", terrain, false);

        creer(corpsArticle("ART-ECR-CAT-TER").avec(CATEGORIE_ID, categorieTerrain.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void r5_2_vocationProjet_400() throws Exception {
        creer(corpsArticle("ART-ECR-VOC-PRJ").avec(VOCATION_ID, VOCATION_PROJET)).andExpect(status().isBadRequest());
    }

    @Test
    void r5_2_tvaSuperieureA1_400() throws Exception {
        creer(corpsArticle("ART-ECR-TVA15").avec(TAUX_TVA, new BigDecimal("1.5"))).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"remiseMax1", "remiseMax2"})
    void r5_2_remiseHorsFraction_400(String champ) throws Exception {
        creer(corpsArticle("ART-ECR-REM-" + champ).avec(champ, new BigDecimal("1.5"))).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {PRIX_PROVISOIRE_TTC, PRIX_DEFINITIF_TTC, "surfaceInterieure", "surfaceMurs",
            "surfacePartiesCommunes"})
    void r5_2_valeurNegative_400(String champ) throws Exception {
        creer(corpsArticle("ART-ECR-NEG-" + champ).avec(champ, new BigDecimal("-1"))).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {TRANCHE_ID, CODE, DESCRIPTION, CATEGORIE_ID, TYPE_ID})
    void r5_2_champObligatoireManquant_400(String champ) throws Exception {
        creer(corpsArticle("ART-ECR-OBL-" + champ).sans(champ)).andExpect(status().isBadRequest());
    }

    @Test
    void r2_8_trancheInconnue_400() throws Exception {
        creer(corpsArticle("ART-ECR-TR-INC").avec(TRANCHE_ID, UUID.randomUUID())).andExpect(status().isBadRequest());
    }

    @Test
    void r2_8_immeubleInconnu_400() throws Exception {
        creer(corpsArticle("ART-ECR-IMM-INC").avec("immeubleId", UUID.randomUUID())).andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ §5.4.2 unicité par tranche

    @Test
    void r5_4_2_memeCodeMemeTranche_409() throws Exception {
        creerOk(corpsArticle("ART-ECR-DOUBLON"));

        creer(corpsArticle("art-ecr-doublon")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("ART-ECR-DOUBLON")));
    }

    @Test
    void r5_4_2_memeCodeAutreTranche_201() throws Exception {
        CfgTranche autre = fixture.tranche("TR-ART-ECR-AUTRE", projet);
        creerOk(corpsArticle("ART-ECR-PARTAGE"));

        creer(corpsArticle("ART-ECR-PARTAGE").avec(TRANCHE_ID, autre.getId())).andExpect(status().isCreated());
    }

    // ------------------------------------------------------------------ §5.4.3 / §5.4.4 modification

    @Test
    void r5_1_patch_idInconnu_404() throws Exception {
        modifier(UUID.randomUUID(), corps().avec(DESCRIPTION, "x")).andExpect(status().isNotFound());
    }

    @Test
    void r5_4_3_patch_changementDeTranche_400() throws Exception {
        UUID id = creerOk(corpsArticle("ART-ECR-CHG-TR"));
        CfgTranche autre = fixture.tranche("TR-ART-ECR-CHG", projet);

        modifier(id, corps().avec(TRANCHE_ID, autre.getId())).andExpect(status().isBadRequest());
    }

    @Test
    void r2_9_patch_champAbsentInchange_traceModification() throws Exception {
        UUID id = creerOk(corpsArticle("ART-ECR-PATCH").avec("notes", "Avant"));

        modifier(id, corps().avec(DESCRIPTION, "Nouvelle description")).andExpect(status().isOk());

        Tuple a = fixture.ligneArticle(id);
        assertAll(
                () -> assertThat(a.get("Description")).isEqualTo("Nouvelle description"),
                () -> assertThat(a.get("Notes")).isEqualTo("Avant"),
                () -> assertThat(decimal(a.get("ComSalePrice"))).isEqualByComparingTo("819000"),
                () -> assertThat(a.get("UserUpdate")).isEqualTo(AUTEUR),
                () -> assertThat(a.get("UserUpdateDateTime")).isNotNull());
    }

    @ParameterizedTest
    @ValueSource(strings = {PRIX_DEFINITIF, PRIX_PROVISOIRE, SURFACE_DEFINITIVE, SURFACE_PROVISOIRE, TAUX_TVA,
            "remiseMax1"})
    void r5_4_4_articleOccupe_modificationProtegee_409(String champ) throws Exception {
        UUID id = creerOccupe("ART-OCC-" + champ);

        modifier(id, corps().avec(champ, new BigDecimal("0.1"))).andExpect(status().isConflict());
    }

    @Test
    void r5_4_4_articleOccupe_modificationDuPrix_409_messageNommeLeDossier() throws Exception {
        UUID id = creerOccupe("ART-ECR-OCC-PRIX");

        modifier(id, corps().avec(PRIX_DEFINITIF, new BigDecimal("20000")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("DOS-ART-ECR-OCC-PRIX")));
    }

    @Test
    void r5_4_4_articleOccupe_modificationDuCode_409() throws Exception {
        UUID id = creerOccupe("ART-ECR-OCC-CODE");

        modifier(id, corps().avec(CODE, "ART-ECR-OCC-CODE-2")).andExpect(status().isConflict());
    }

    @Test
    void r5_4_4_articleOccupe_modificationDeLaCategorie_409() throws Exception {
        UUID id = creerOccupe("ART-ECR-OCC-CAT");
        StkItemCategory autre = fixture.categorie("CAT-ART-ECR-AUTRE", fixture.natureBat(), false);

        modifier(id, corps().avec(CATEGORIE_ID, autre.getId())).andExpect(status().isConflict());
    }

    @Test
    void r5_4_4_articleOccupe_modificationDeLaDescription_200() throws Exception {
        UUID id = creerOccupe("ART-ECR-OCC-DESC");

        modifier(id, corps().avec(DESCRIPTION, "Description modifiee").avec("notes", "Note")
                .avec("titreFoncier", "TF-9").avec("immeubleId", fixture.immeuble("IMM-ART-ECR-2").getId()))
                .andExpect(status().isOk());

        assertThat(fixture.ligneArticle(id).get("Description")).isEqualTo("Description modifiee");
    }

    @Test
    void r2_5_patch_comFolderIdInchange() throws Exception {
        UUID id = creerOccupe("ART-ECR-OCC-FOLDER");
        String avant = guid(fixture.ligneArticle(id).get("ComFolderId"));

        modifier(id, corps().avec(DESCRIPTION, "Autre description")).andExpect(status().isOk());

        assertThat(guid(fixture.ligneArticle(id).get("ComFolderId"))).isNotNull().isEqualTo(avant);
    }

    @Test
    void r10_detail_articleOccupe_exposeOccupe() throws Exception {
        UUID id = creerOccupe("ART-ECR-OCC-DTO");

        modifier(id, corps().avec(DESCRIPTION, "x")).andExpect(status().isOk())
                .andExpect(jsonPath("$.occupe").value(true));
    }
}

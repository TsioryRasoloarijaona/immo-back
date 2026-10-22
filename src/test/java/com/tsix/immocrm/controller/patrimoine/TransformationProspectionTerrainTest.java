package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.security.AuthenticatedUser;
import com.tsix.immocrm.fixtures.ApiProspectionsTerrain;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureEntites;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.Contexte;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.Statut;
import com.tsix.immocrm.fixtures.FixtureTerrains;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * « Transformer en terrain » — FEATURE-patrimoine-prospections-terrain.md §5.7 (TransformationProspectionTerrainTest) :
 * terrain créé comme par la fiche Terrain de P7a (statut initial LandProject, StrEntityId, société, code, nature TER),
 * colonnes foncières copiées une à une, prix, LndLandId, prospection archivée et transformée ; garde-fous 409 / 400 / 404.
 * Le rollback (échec de completerDepuisProspection) est dans TransformationProspectionTerrainRollbackTest (non transactionnel).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TransformationProspectionTerrainTest {

    private static final String TERRAIN_SQL = "SELECT * FROM StkItem WHERE LndLandId = :id";
    private static final UUID INCONNU = UUID.fromString("7C8D9EAF-B0C1-4D2E-9F3A-4B5C6D7E8F90");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureProspectionsTerrain fixture;
    @Autowired private FixtureTerrains terrains;
    @Autowired private FixtureEntites entites;

    private ApiProspectionsTerrain api;
    private Contexte c;
    private CfgCompany societe;
    private UUID categorieTerrain;
    private UUID transformable;

    @BeforeEach
    void setUp() {
        api = new ApiProspectionsTerrain(mockMvc, patrimoine.administrateur());
        c = fixture.contexte("P7X");
        patrimoine.referentielArticle();
        terrains.configurationTerrain();
        societe = patrimoine.entite("ENT-" + c.jeton(), true);
        categorieTerrain = patrimoine.categorie("CTE-" + c.jeton(), terrains.natureTer(), false).getId();
        transformable = fixture.statut("TRF-" + c.jeton(), 250, TYPE_SUCCES, new Statut(false, true, true, 0));
    }

    private String code(String suffixe) {
        return suffixe + "-" + c.jeton();
    }

    private CorpsJson dialogue() {
        return corps().avec(ENTITE_ID, societe.getId()).avec(VOCATION_ID, FixturePatrimoine.VOCATION_PROJET)
                .avec(CATEGORIE_ID, categorieTerrain);
    }

    private UUID prospectionTransformable(String suffixe) {
        return fixture.prospection(c, code(suffixe), transformable);
    }

    private List<Tuple> terrainsIssus(UUID prospection) {
        return fixture.lignes(TERRAIN_SQL, prospection);
    }

    private UUID transformerOk(UUID prospection, CorpsJson corps) throws Exception {
        String json = api.transformer(prospection, corps).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.terrainId"));
    }

    // ------------------------------------------------------------------ terrain créé

    /** §5.7 : terrain identique à celui de P7a pour les colonnes communes ; code = celui de la prospection par défaut. */
    @Test
    void r5_7_terrainCree_commeP7a_colonnesCommunes() throws Exception {
        UUID p = prospectionTransformable("TRA");

        String json = api.transformer(p, dialogue()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(code("TRA")))
                .andReturn().getResponse().getContentAsString();

        List<Tuple> issus = terrainsIssus(p);
        assertThat(issus).hasSize(1);
        Tuple t = issus.get(0);
        assertAll(
                () -> assertThat(guid(t.get("PKey"))).isEqualTo(guid(JsonPath.<String>read(json, "$.terrainId"))),
                () -> assertThat(t.get("Code")).isEqualTo(code("TRA")),
                () -> assertThat(t.get("Description")).isEqualTo("Prospection " + code("TRA")),
                () -> assertThat(guid(t.get("CfgCompanyId"))).isEqualTo(guid(societe.getId())),
                () -> assertThat(guid(t.get("StkKindId"))).isEqualTo(guid(FixtureTerrains.NATURE_TER)),
                () -> assertThat(guid(t.get("StkVocationId"))).isEqualTo(guid(FixturePatrimoine.VOCATION_PROJET)),
                () -> assertThat(guid(t.get("StkItemCategoryId"))).isEqualTo(guid(categorieTerrain)),
                () -> assertThat(guid(t.get("StkItemTypeId"))).isEqualTo(guid(c.type())),
                () -> assertThat(guid(t.get("StrEntityId"))).isEqualTo(guid(terrains.entiteLandProject())),
                () -> assertThat(guid(t.get("StrStatusId"))).isEqualTo(guid(terrains.statutInitial())),
                () -> assertThat(guid(t.get("AdmCountryId"))).isEqualTo(guid(c.pays().getId())),
                () -> assertThat(guid(t.get("AdmCityId"))).isEqualTo(guid(c.ville())),
                () -> assertThat(t.get("IsFinished")).isEqualTo(true),
                () -> assertThat(guid(t.get("LndLandId"))).isEqualTo(guid(p)));
    }

    /** §5.7 : code du dialogue prioritaire sur celui de la prospection. */
    @Test
    void r5_7_codeDuDialogue_prioritaire() throws Exception {
        UUID p = prospectionTransformable("TCD");

        UUID terrain = transformerOk(p, dialogue().avec(CODE, code("TERRAIN")));

        assertThat(terrains.ligne("SELECT * FROM StkItem WHERE PKey = :id", terrain).get("Code"))
                .isEqualTo(code("TERRAIN"));
    }

    /** §5.7 : IsFinished du terrain = celui de la prospection (Brut → faux). */
    @Test
    void r5_7_isFinished_celuiDeLaProspection() throws Exception {
        UUID p = prospectionTransformable("TBR");
        fixture.definir(p, "IsFinished", false);

        transformerOk(p, dialogue());

        assertThat(terrainsIssus(p).get(0).get("IsFinished")).isEqualTo(false);
    }

    /** §5.7 tableau : colonnes foncières copiées une à une (valeurs stockées de la prospection). */
    @Test
    void r5_7_colonnesFoncieresCopiees() throws Exception {
        UUID p = prospectionTransformable("TCO");
        fixture.definir(p, "CommerceCoef", new BigDecimal("0.1"));
        fixture.definir(p, "CommerceArea", new BigDecimal("120"));
        fixture.definir(p, "OfficeCoef", new BigDecimal("0.05"));
        fixture.definir(p, "OfficeArea", new BigDecimal("60"));
        fixture.definir(p, "UndergroundParkingCoef", new BigDecimal("0.2"));
        fixture.definir(p, "UndergroundParkingArea", new BigDecimal("240"));
        fixture.definir(p, "AreaEquipmentTransferred", new BigDecimal("7"));
        fixture.definir(p, "AreaRegularized", new BigDecimal("8"));
        fixture.definir(p, "AreaCeded", new BigDecimal("9"));
        fixture.definir(p, "HabitatArea", new BigDecimal("2570"));
        fixture.definir(p, "RegistryDate", LocalDate.of(2026, 3, 2).atStartOfDay());
        fixture.definir(p, "ImmatriculationDate", LocalDate.of(2026, 4, 3).atStartOfDay());

        transformerOk(p, dialogue());

        Tuple t = terrainsIssus(p).get(0);
        assertAll(
                () -> assertThat(guid(t.get("LndLevelId"))).isEqualTo(guid(c.niveau())),
                () -> assertThat(guid(t.get("LndVocationId"))).isEqualTo(guid(c.vocation())),
                () -> assertThat(guid(t.get("StkOrientationId"))).isEqualTo(guid(c.orientation())),
                () -> assertThat(guid(t.get("CfgMiddlemanId"))).isEqualTo(guid(c.courtier())),
                () -> assertThat(guid(t.get("CfgLawyerId"))).isEqualTo(guid(c.notaire())),
                () -> assertThat(t.get("GmapLink")).isEqualTo("https://www.google.com/maps/@33.5,-7.6"),
                () -> assertThat(t.get("Address")).isEqualTo("12 route cotiere"),
                () -> assertThat(t.get("Owner")).isEqualTo("Proprietaire A et B"),
                () -> assertThat(t.get("Notes")).isEqualTo("Notes prospection"),
                () -> assertThat(t.get("Anomalies")).isEqualTo("Anomalie voisinage"),
                () -> assertThat(decimal(t.get("AreaFloor"))).isEqualByComparingTo("1200"),
                () -> assertThat(decimal(t.get("Area"))).isEqualByComparingTo("1200"),
                () -> assertThat(decimal(t.get("EquipementsArea"))).isEqualByComparingTo("100"),
                () -> assertThat(decimal(t.get("EquipementsCoef"))).isEqualByComparingTo("0.090909"),
                () -> assertThat(decimal(t.get("AreaEquipmentTransferred"))).isEqualByComparingTo("7"),
                () -> assertThat(decimal(t.get("AreaRegularized"))).isEqualByComparingTo("8"),
                () -> assertThat(decimal(t.get("AreaCeded"))).isEqualByComparingTo("9"),
                () -> assertThat(decimal(t.get("Cos"))).isEqualByComparingTo("0.3"),
                () -> assertThat(decimal(t.get("AreaCos"))).isEqualByComparingTo("330"),
                () -> assertThat(decimal(t.get("Cuf"))).isEqualByComparingTo("2.5"),
                () -> assertThat(decimal(t.get("AreaCuf"))).isEqualByComparingTo("2750"),
                () -> assertThat(decimal(t.get("CoefSaleAbleArea"))).isEqualByComparingTo("1"),
                () -> assertThat(decimal(t.get("SalableArea"))).isEqualByComparingTo("2750"),
                () -> assertThat(decimal(t.get("HabitatArea"))).isEqualByComparingTo("2570"),
                () -> assertThat(decimal(t.get("CommerceCoef"))).isEqualByComparingTo("0.1"),
                () -> assertThat(decimal(t.get("CommerceArea"))).isEqualByComparingTo("120"),
                () -> assertThat(decimal(t.get("OfficeCoef"))).isEqualByComparingTo("0.05"),
                () -> assertThat(decimal(t.get("OfficeArea"))).isEqualByComparingTo("60"),
                () -> assertThat(decimal(t.get("UndergroundParkingCoef"))).isEqualByComparingTo("0.2"),
                () -> assertThat(decimal(t.get("UndergroundParkingArea"))).isEqualByComparingTo("240"),
                () -> assertThat(((Number) t.get("AlinmentStop")).intValue()).isEqualTo(4),
                () -> assertThat(((Number) t.get("MaxHeight")).intValue()).isEqualTo(30),
                () -> assertThat(decimal(t.get("ProspectionUnitCost"))).isEqualByComparingTo("4000"),
                () -> assertThat(decimal(t.get("ProspectionPrice"))).isEqualByComparingTo("4800000"),
                () -> assertThat(decimal(t.get("ProspectionPriceProposed"))).isEqualByComparingTo("4000000"),
                () -> assertThat(t.get("RegistryNumber")).isEqualTo("RG-1"),
                () -> assertThat(t.get("RegistryPlace")).isEqualTo("Casablanca"),
                () -> assertThat(t.get("ImmatriculationCPF")).isEqualTo("CPF-1"),
                () -> assertThat(FixtureTerrains.horodatage(t.get("RegistryDate")).toLocalDate())
                        .isEqualTo(LocalDate.of(2026, 3, 2)),
                () -> assertThat(FixtureTerrains.horodatage(t.get("ImmatriculationDate")).toLocalDate())
                        .isEqualTo(LocalDate.of(2026, 4, 3)));
    }

    /** §5.7 prix : 4 000 000 / 1 200 → 3 333,333333 ; SalePriceFloor = SalePrice = PurchaseCost = prix proposé. */
    @Test
    void r5_7_prix_depuisLePrixPropose() throws Exception {
        UUID p = prospectionTransformable("TPX");

        transformerOk(p, dialogue());

        Tuple t = terrainsIssus(p).get(0);
        BigDecimal centime = new BigDecimal("0.01");
        assertAll(
                () -> assertThat(decimal(t.get("PurchaseCost"))).isEqualByComparingTo("4000000"),
                () -> assertThat(decimal(t.get("PurchaseUnitCost"))).isEqualByComparingTo("3333.333333"),
                () -> assertThat(decimal(t.get("SalePrice"))).isCloseTo(new BigDecimal("4000000"), within(centime)),
                () -> assertThat(decimal(t.get("SalePriceFloor"))).isCloseTo(new BigDecimal("4000000"), within(centime)));
    }

    /** §5.7 : prix proposé 0 → prix 0, pas d'erreur. */
    @Test
    void r5_7_prixProposeNul_prixZero() throws Exception {
        UUID p = prospectionTransformable("TP0");
        fixture.definir(p, "ProspectionPriceProposed", BigDecimal.ZERO);

        transformerOk(p, dialogue());

        Tuple t = terrainsIssus(p).get(0);
        assertAll(
                () -> assertThat(decimal(t.get("PurchaseCost"))).isEqualByComparingTo("0"),
                () -> assertThat(decimal(t.get("SalePrice"))).isEqualByComparingTo("0"));
    }

    /** §5.7 : prospection archivée et transformée ; aucune ligne StrHistory écrite (OMRAN n'en écrit pas ici). */
    @Test
    void r5_7_prospectionArchiveeEtTransformee_sansHistorique() throws Exception {
        UUID p = prospectionTransformable("TAT");

        transformerOk(p, dialogue());

        Tuple l = fixture.ligne(p);
        assertAll(
                () -> assertThat(l.get("IsInArchive")).isEqualTo(true),
                () -> assertThat(l.get("IsTransformedToLand")).isEqualTo(true),
                () -> assertThat(guid(l.get("StrStatusId"))).isEqualTo(guid(transformable)),
                () -> assertThat(fixture.compter("SELECT COUNT(*) FROM StrHistory WHERE ObjectId = :id", p)).isZero());
    }

    /** §5.2 : la fiche de la prospection rend ensuite terrainId / terrainCode et peutTransformer faux. */
    @Test
    void r5_2_ficheApresTransformation_lienVersLeTerrain() throws Exception {
        UUID p = prospectionTransformable("TFI");
        UUID terrain = transformerOk(p, dialogue());

        api.fiche(p)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.terrainId").value(terrain.toString()))
                .andExpect(jsonPath("$.terrainCode").value(code("TFI")))
                .andExpect(jsonPath("$.estTransformee").value(true))
                .andExpect(jsonPath("$.peutTransformer").value(false));
    }

    // ------------------------------------------------------------------ garde-fous

    /** §5.7 : deuxième appel → 409 « déjà transformée » avec terrainId dans le ProblemDetail ; un seul terrain. */
    @Test
    void r5_7_deuxiemeAppel_409_avecTerrainId() throws Exception {
        UUID p = prospectionTransformable("T2X");
        UUID terrain = transformerOk(p, dialogue());

        api.transformer(p, dialogue().avec(CODE, code("AUTRE")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString(MESSAGE_DEJA_TRANSFORMEE)))
                .andExpect(jsonPath("$.terrainId").value(terrain.toString()));

        assertThat(terrainsIssus(p)).hasSize(1);
    }

    /** §5.7 : statut courant sans BoolField001 (toute la production) → 409, rien d'écrit. */
    @Test
    void r5_7_statutSansBoolField001_409_rienEcrit() throws Exception {
        UUID p = fixture.prospection(c, code("TNB"));

        api.transformer(p, dialogue())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString(MESSAGE_PAS_TRANSFORMABLE)));

        assertAll(
                () -> assertThat(terrainsIssus(p)).isEmpty(),
                () -> assertThat(fixture.ligne(p).get("IsTransformedToLand")).isEqualTo(false));
    }

    /** §5.7 : code déjà pris dans la société (contrôle de EcritureTerrain.creer) → 409, prospection intacte. */
    @Test
    void r5_7_codeDejaPrisDansLaSociete_409() throws Exception {
        UUID p = prospectionTransformable("TDB");
        FixtureTerrains.Contexte t = terrains.contexte("P7Y");
        UUID existant = terrains.terrain(t, code("TDB"));
        terrains.definir(existant, "CfgCompanyId", societe.getId());

        api.transformer(p, dialogue()).andExpect(status().isConflict());

        assertAll(
                () -> assertThat(terrainsIssus(p)).isEmpty(),
                () -> assertThat(fixture.ligne(p).get("IsInArchive")).isEqualTo(false));
    }

    /** §5.7 : société inactive → 400. */
    @Test
    void r5_7_societeInactive_400() throws Exception {
        UUID p = prospectionTransformable("TSI");
        entites.actif(societe.getId(), false);

        api.transformer(p, dialogue()).andExpect(status().isBadRequest());

        assertThat(terrainsIssus(p)).isEmpty();
    }

    /** §5.7 / §13 décision 7 : catégorie du dialogue de nature PROS TER (et non Terrain) → 400. */
    @Test
    void d13_7_categorieNonTerrain_400() throws Exception {
        UUID p = prospectionTransformable("TCN");

        api.transformer(p, dialogue().avec(CATEGORIE_ID, c.categorie())).andExpect(status().isBadRequest());

        assertThat(terrainsIssus(p)).isEmpty();
    }

    /** §5.7 : société, vocation et catégorie obligatoires dans le dialogue. */
    @Test
    void r5_7_dialogueIncomplet_400() throws Exception {
        UUID p = prospectionTransformable("TDI");

        api.transformer(p, dialogue().sans(ENTITE_ID)).andExpect(status().isBadRequest());
        api.transformer(p, dialogue().sans(VOCATION_ID)).andExpect(status().isBadRequest());
        api.transformer(p, dialogue().sans(CATEGORIE_ID)).andExpect(status().isBadRequest());

        assertThat(terrainsIssus(p)).isEmpty();
    }

    /** §5.7 : type de la prospection nul → 400 typeId. */
    @Test
    void r5_7_typeNul_400() throws Exception {
        UUID p = prospectionTransformable("TTN");
        fixture.definir(p, "LndTypeId", null);

        api.transformer(p, dialogue())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString(TYPE_ID)));

        assertThat(terrainsIssus(p)).isEmpty();
    }

    /** §5.7 : prospection inconnue → 404. */
    @Test
    void r5_7_prospectionInconnue_404() throws Exception {
        api.transformer(INCONNU, dialogue()).andExpect(status().isNotFound());
    }

    /** §5.7 : non-Administrateur → 403, rien d'écrit. */
    @Test
    void r5_7_commercial_403() throws Exception {
        UUID p = prospectionTransformable("TCM");
        AdmUser commercial = patrimoine.administrateur();
        AuthenticatedUser principal = new AuthenticatedUser(commercial.getId(), commercial.getCode(), null, false, true, 0);

        mockMvc.perform(post(TRANSFORMER, p).with(authentication(new UsernamePasswordAuthenticationToken(principal, null,
                                List.of(new SimpleGrantedAuthority("ROLE_Commercial")))))
                        .contentType(MediaType.APPLICATION_JSON).content(dialogue().json()))
                .andExpect(status().isForbidden());

        assertThat(terrainsIssus(p)).isEmpty();
    }
}

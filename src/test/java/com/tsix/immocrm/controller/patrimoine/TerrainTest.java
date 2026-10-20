package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureArticleFiche;
import com.tsix.immocrm.fixtures.FixtureEntites;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureTerrains;
import com.tsix.immocrm.fixtures.FixtureTerrains.Contexte;
import com.tsix.immocrm.fixtures.FixtureTerrains.TypeFrais;
import com.tsix.immocrm.security.AuthenticatedUser;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static com.tsix.immocrm.fixtures.FixtureTerrains.CATEGORIE_ID;
import static com.tsix.immocrm.fixtures.FixtureTerrains.CODE;
import static com.tsix.immocrm.fixtures.FixtureTerrains.COUT_TOTAL;
import static com.tsix.immocrm.fixtures.FixtureTerrains.COUT_UNITAIRE_FINAL;
import static com.tsix.immocrm.fixtures.FixtureTerrains.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureTerrains.ENTITE_ID;
import static com.tsix.immocrm.fixtures.FixtureTerrains.NATURE_ID;
import static com.tsix.immocrm.fixtures.FixtureTerrains.PAYS_ID;
import static com.tsix.immocrm.fixtures.FixtureTerrains.PRIX_ACHAT;
import static com.tsix.immocrm.fixtures.FixtureTerrains.PRIX_ACHAT_M2;
import static com.tsix.immocrm.fixtures.FixtureTerrains.PRIX_M2_DEFINITIF_HT;
import static com.tsix.immocrm.fixtures.FixtureTerrains.PRIX_M2_DEFINITIF_TTC;
import static com.tsix.immocrm.fixtures.FixtureTerrains.PRIX_M2_PROVISOIRE_HT;
import static com.tsix.immocrm.fixtures.FixtureTerrains.SOUS_TERRAINS;
import static com.tsix.immocrm.fixtures.FixtureTerrains.SURFACE_DEFINITIVE;
import static com.tsix.immocrm.fixtures.FixtureTerrains.SURFACE_PROVISOIRE;
import static com.tsix.immocrm.fixtures.FixtureTerrains.TAUX_TVA;
import static com.tsix.immocrm.fixtures.FixtureTerrains.TERRAIN;
import static com.tsix.immocrm.fixtures.FixtureTerrains.TERRAINS;
import static com.tsix.immocrm.fixtures.FixtureTerrains.TITRE_FONCIER;
import static com.tsix.immocrm.fixtures.FixtureTerrains.TYPE_ID;
import static com.tsix.immocrm.fixtures.FixtureTerrains.VILLE_ID;
import static com.tsix.immocrm.fixtures.FixtureTerrains.VOCATION_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fiche terrain — FEATURE-patrimoine-terrains.md §1 et §6.2 (TerrainTest) : création, unicité Code + société,
 * obligatoires, nature Terrain des références, statut initial lu en base, valorisations, coûts, TotalCost /
 * FinalUnitCost à la lecture, champs protégés si occupé, sous-terrains, visibilité par la société, porteurs, liste.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TerrainTest {

    private static final String ENFANTS = "SELECT COUNT(*) FROM StkItem WHERE ParentId = :id";
    private static final String LIGNE_ARTICLE = "SELECT * FROM StkItem WHERE PKey = :id";
    private static final BigDecimal MILLE = new BigDecimal("1000");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureTerrains terrains;
    @Autowired private FixtureEntites entites;
    @Autowired private FixtureArticleFiche fiche;

    private AdmUser admin;
    private Contexte c;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        c = terrains.contexte("P7T");
    }

    private String code(String suffixe) {
        return suffixe + "-" + c.jeton();
    }

    // ------------------------------------------------------------------ outils

    private CorpsJson corpsTerrain(String code) {
        return corps().avec(CODE, code).avec(DESCRIPTION, "Terrain " + code)
                .avec(ENTITE_ID, c.entite().getId()).avec(PAYS_ID, c.pays().getId()).avec(VILLE_ID, c.ville())
                .avec(NATURE_ID, c.natureTer().getId()).avec(VOCATION_ID, FixturePatrimoine.VOCATION_PROJET)
                .avec(CATEGORIE_ID, c.categorie().getId()).avec(TYPE_ID, c.type().getId())
                .avec(SURFACE_PROVISOIRE, MILLE).avec(SURFACE_DEFINITIVE, MILLE);
    }

    private ResultActions creer(CorpsJson corps) throws Exception {
        return mockMvc.perform(post(TERRAINS).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private UUID creerOk(CorpsJson corps) throws Exception {
        String json = creer(corps).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    private ResultActions modifier(UUID id, CorpsJson corps) throws Exception {
        return mockMvc.perform(patch(TERRAIN, id).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private Tuple ligne(UUID id) {
        return terrains.ligne(LIGNE_ARTICLE, id);
    }

    private int articlesDeCode(String code) {
        return terrains.articlesDeCode(code);
    }

    private static BigDecimal lireDecimal(String json, String chemin) {
        Object valeur = JsonPath.read(json, chemin);
        return valeur == null ? null : new BigDecimal(valeur.toString());
    }

    private void occuper(UUID terrainId) {
        terrains.definir(terrainId, "ComFolderId", fixture.dossier("DOS-" + c.jeton()).getId());
    }

    // ------------------------------------------------------------------ §1.2 création

    /** R1.2 : colonnes écrites, StrEntityId = LandProject, statut initial = premier statut (Sorting) lu en base. */
    @Test
    void r1_2_creation_201_colonnesEcrites_strEntityEtPremierStatutLuEnBase() throws Exception {
        UUID premier = terrains.statutTerrain("PREM-" + c.jeton(), -999_999, null);

        Tuple t = ligne(creerOk(corpsTerrain(code("CRE"))));

        assertAll(
                () -> assertThat(t.get("Code")).isEqualTo(code("CRE")),
                () -> assertThat(t.get("Description")).isEqualTo("Terrain " + code("CRE")),
                () -> assertThat(guid(t.get("StkKindId"))).isEqualTo(guid(c.natureTer().getId())),
                () -> assertThat(guid(t.get("StkVocationId"))).isEqualTo(guid(FixturePatrimoine.VOCATION_PROJET)),
                () -> assertThat(guid(t.get("StkItemCategoryId"))).isEqualTo(guid(c.categorie().getId())),
                () -> assertThat(guid(t.get("StkItemTypeId"))).isEqualTo(guid(c.type().getId())),
                () -> assertThat(guid(t.get("CfgCompanyId"))).isEqualTo(guid(c.entite().getId())),
                () -> assertThat(guid(t.get("AdmCountryId"))).isEqualTo(guid(c.pays().getId())),
                () -> assertThat(guid(t.get("AdmCityId"))).isEqualTo(guid(c.ville())),
                () -> assertThat(decimal(t.get("AreaFloor"))).isEqualByComparingTo(MILLE),
                () -> assertThat(decimal(t.get("Area"))).isEqualByComparingTo(MILLE),
                () -> assertThat(t.get("StkHierarchyId")).isNull(),
                () -> assertThat(guid(t.get("StrEntityId"))).isEqualTo(guid(terrains.entiteLandProject())),
                () -> assertThat(guid(t.get("StrStatusId"))).isEqualTo(guid(premier)));
    }

    /** R1.2 : aucun statut pour l'entité LandProject → ConfigurationManquanteException (500), rien d'écrit. */
    @Test
    void r1_2_aucunStatutLandProject_500_rienEcrit() throws Exception {
        terrains.executer("""
                UPDATE StrStatus SET StrConfigEntityId = (SELECT TOP 1 PKey FROM StrEntity WHERE Code = N'StkItem')
                WHERE StrConfigEntityId IN (SELECT PKey FROM StrEntity WHERE Code = N'LandProject')""");

        creer(corpsTerrain(code("SST"))).andExpect(status().isInternalServerError());

        assertThat(articlesDeCode(code("SST"))).isZero();
    }

    @Test
    void r1_2_codeDejaPrisDansLaSociete_409() throws Exception {
        terrains.terrain(c, code("DBL"));

        creer(corpsTerrain(code("DBL"))).andExpect(status().isConflict());

        assertThat(articlesDeCode(code("DBL"))).isEqualTo(1);
    }

    @Test
    void r1_2_memeCodeDansUneAutreSociete_201() throws Exception {
        terrains.terrain(c, code("DBS"));
        UUID autre = fixture.entite("ENT2-" + c.jeton(), true).getId();

        creer(corpsTerrain(code("DBS")).avec(ENTITE_ID, autre)).andExpect(status().isCreated());

        assertThat(articlesDeCode(code("DBS"))).isEqualTo(2);
    }

    /** R1.2 : Code, Description, vocation, catégorie, type, AreaFloor, société, pays, ville obligatoires. */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {CODE, DESCRIPTION, VOCATION_ID, CATEGORIE_ID, TYPE_ID, SURFACE_PROVISOIRE, ENTITE_ID,
            PAYS_ID, VILLE_ID})
    void r1_2_champObligatoireAbsent_400_rienEcrit(String champ) throws Exception {
        String code = code("OBL");

        creer(corpsTerrain(code).sans(champ)).andExpect(status().isBadRequest());

        assertThat(articlesDeCode(code)).isZero();
    }

    /** R1.2 : référence ReferencesPatrimoine inconnue → 400. */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {CATEGORIE_ID, TYPE_ID, VOCATION_ID})
    void r1_2_referenceInconnue_400(String champ) throws Exception {
        creer(corpsTerrain(code("INC")).avec(champ, UUID.randomUUID())).andExpect(status().isBadRequest());
    }

    /** R1.2 : le serveur vérifie la nature Terrain des références (écart P1b à ne pas reproduire). */
    @Test
    void r1_2_categorieDUneAutreNature_400() throws Exception {
        UUID categorieBat = fixture.categorie("CATB-" + c.jeton(), fixture.natureBat(), false).getId();

        creer(corpsTerrain(code("CNB")).avec(CATEGORIE_ID, categorieBat)).andExpect(status().isBadRequest());
    }

    @Test
    void r1_2_typeDUneAutreNature_400() throws Exception {
        UUID typeBat = fixture.type("TYPB-" + c.jeton(), fixture.natureBat(), false).getId();

        creer(corpsTerrain(code("TNB")).avec(TYPE_ID, typeBat)).andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ §1.2 valorisations et coûts

    /** R1.2 : prix ↔ m² HT/TTC par CalculPrixArticle (même échelle que les articles), provisoire et définitive. */
    @Test
    void r1_2_valorisations_prixDepuisLeM2HT() throws Exception {
        Tuple t = ligne(creerOk(corpsTerrain(code("VAL")).avec(SURFACE_PROVISOIRE, new BigDecimal("900"))
                .avec(PRIX_M2_PROVISOIRE_HT, new BigDecimal("400")).avec(PRIX_M2_DEFINITIF_HT, new BigDecimal("500"))
                .avec(TAUX_TVA, new BigDecimal("0.20"))));

        assertAll(
                () -> assertThat(decimal(t.get("ComSaleUnitCostHT"))).isEqualByComparingTo("500"),
                () -> assertThat(decimal(t.get("ComSaleUnitCost"))).isEqualByComparingTo("600"),
                () -> assertThat(decimal(t.get("SalePriceHT"))).isEqualByComparingTo("500000"),
                () -> assertThat(decimal(t.get("SalePrice"))).isEqualByComparingTo("600000"),
                () -> assertThat(decimal(t.get("ComSaleUnitCostFloorHT"))).isEqualByComparingTo("400"),
                () -> assertThat(decimal(t.get("ComSaleUnitCostFloor"))).isEqualByComparingTo("480"),
                () -> assertThat(decimal(t.get("SalePriceFloorHT"))).isEqualByComparingTo("360000"),
                () -> assertThat(decimal(t.get("SalePriceFloor"))).isEqualByComparingTo("432000"));
    }

    @Test
    void r1_2_valorisations_m2HTDepuisLeTTC() throws Exception {
        Tuple t = ligne(creerOk(corpsTerrain(code("VTT")).avec(PRIX_M2_DEFINITIF_TTC, new BigDecimal("600"))
                .avec(TAUX_TVA, new BigDecimal("0.20"))));

        assertAll(
                () -> assertThat(decimal(t.get("ComSaleUnitCostHT"))).isEqualByComparingTo("500"),
                () -> assertThat(decimal(t.get("SalePrice"))).isEqualByComparingTo("600000"));
    }

    /** R1.2 Coûts : PurchaseCost = PurchaseUnitCost × Area. */
    @Test
    void r1_2_couts_prixAchatM2_donneLePrixDAchat() throws Exception {
        Tuple t = ligne(creerOk(corpsTerrain(code("PAM")).avec(PRIX_ACHAT_M2, new BigDecimal("200"))));

        assertAll(
                () -> assertThat(decimal(t.get("PurchaseUnitCost"))).isEqualByComparingTo("200"),
                () -> assertThat(decimal(t.get("PurchaseCost"))).isEqualByComparingTo("200000"));
    }

    /** R1.2 Coûts : PurchaseUnitCost = PurchaseCost / Area (Area ≠ 0). */
    @Test
    void r1_2_couts_prixDAchat_donneLePrixAchatM2() throws Exception {
        Tuple t = ligne(creerOk(corpsTerrain(code("PAC")).avec(PRIX_ACHAT, new BigDecimal("300000"))));

        assertAll(
                () -> assertThat(decimal(t.get("PurchaseCost"))).isEqualByComparingTo("300000"),
                () -> assertThat(decimal(t.get("PurchaseUnitCost"))).isEqualByComparingTo("300"));
    }

    // ------------------------------------------------------------------ §1.1 TotalCost / FinalUnitCost à la lecture

    /** R1.1 : TotalCost = Σ StkExpense.Amount, FinalUnitCost = TotalCost / Area, calculés à la lecture. */
    @Test
    void r1_1_coutTotalEtUnitaireFinal_sommeDesFrais() throws Exception {
        UUID terrain = terrains.terrain(c, code("CT"));
        TypeFrais type = terrains.typeFrais(c.jeton(), true, true);
        UUID fournisseur = terrains.fournisseur("FRN-" + c.jeton(), null);
        terrains.frais(terrain, type.id(), fournisseur, new BigDecimal("1000"), false);
        terrains.frais(terrain, type.id(), fournisseur, new BigDecimal("2500"), true);

        String json = mockMvc.perform(get(TERRAIN, terrain).with(connecte(admin)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertAll(
                () -> assertThat(lireDecimal(json, "$." + COUT_TOTAL)).isEqualByComparingTo("3500"),
                () -> assertThat(lireDecimal(json, "$." + COUT_UNITAIRE_FINAL)).isEqualByComparingTo("3.5"));
    }

    @Test
    void r1_1_surfaceNulle_coutUnitaireFinalZero() throws Exception {
        UUID terrain = terrains.terrain(c, code("CT0"));
        TypeFrais type = terrains.typeFrais(c.jeton(), true, true);
        terrains.frais(terrain, type.id(), terrains.fournisseur("FRN-" + c.jeton(), null), MILLE, false);
        terrains.definir(terrain, "Area", BigDecimal.ZERO);

        String json = mockMvc.perform(get(TERRAIN, terrain).with(connecte(admin)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertAll(
                () -> assertThat(lireDecimal(json, "$." + COUT_TOTAL)).isEqualByComparingTo("1000"),
                () -> assertThat(lireDecimal(json, "$." + COUT_UNITAIRE_FINAL)).isEqualByComparingTo("0"));
    }

    // ------------------------------------------------------------------ §1.2 modification, champs protégés

    @Test
    void r1_2_patch_description_200() throws Exception {
        UUID terrain = terrains.terrain(c, code("PDE"));

        modifier(terrain, corps().avec(DESCRIPTION, "Terrain renomme")).andExpect(status().isOk());

        assertThat(ligne(terrain).get("Description")).isEqualTo("Terrain renomme");
    }

    /** R1.2 : surface, prix m² HT/TTC et n° de titre protégés si le terrain est occupé (409). */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {SURFACE_DEFINITIVE, SURFACE_PROVISOIRE, PRIX_M2_DEFINITIF_HT, PRIX_M2_DEFINITIF_TTC,
            PRIX_M2_PROVISOIRE_HT, TITRE_FONCIER})
    void r1_2_occupe_champProtege_409_ligneInchangee(String champ) throws Exception {
        UUID terrain = terrains.terrain(c, code("OCC"));
        occuper(terrain);
        Object valeur = TITRE_FONCIER.equals(champ) ? "TF-" + c.jeton() : new BigDecimal("1234");

        modifier(terrain, corps().avec(champ, valeur)).andExpect(status().isConflict());

        Tuple t = ligne(terrain);
        assertAll(
                () -> assertThat(decimal(t.get("Area"))).isEqualByComparingTo(MILLE),
                () -> assertThat(decimal(t.get("AreaFloor"))).isEqualByComparingTo(MILLE),
                () -> assertThat(t.get("TitleNumber")).isNull());
    }

    @Test
    void r1_2_occupe_champNonProtege_200() throws Exception {
        UUID terrain = terrains.terrain(c, code("OCD"));
        occuper(terrain);

        modifier(terrain, corps().avec(DESCRIPTION, "Terrain occupe renomme")).andExpect(status().isOk());

        assertThat(ligne(terrain).get("Description")).isEqualTo("Terrain occupe renomme");
    }

    /** §0 : PATCH /api/articles/{terrain} reste refusé, la fiche terrain est le seul chemin d'écriture. */
    @Test
    void r0_patchArticlesSurUnTerrain_400() throws Exception {
        UUID terrain = terrains.terrain(c, code("ART"));

        mockMvc.perform(patch("/api/articles/{id}", terrain).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(corps().avec(DESCRIPTION, "x").json()))
                .andExpect(status().isBadRequest());

        assertThat(ligne(terrain).get("Description")).isEqualTo("Terrain " + code("ART"));
    }

    // ------------------------------------------------------------------ §1.2 sous-terrains

    private ResultActions creerSousTerrain(UUID parent, String code) throws Exception {
        return mockMvc.perform(post(SOUS_TERRAINS, parent).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corps().avec(CODE, code).avec(DESCRIPTION, "Sous-terrain " + code)
                        .avec(SURFACE_PROVISOIRE, new BigDecimal("100"))
                        .avec(SURFACE_DEFINITIVE, new BigDecimal("100")).json()));
    }

    /** R1.2 : statut du parent sans BoolField001 (cas de toute la production) → refus, aucun sous-terrain. */
    @Test
    void r1_2_sousTerrain_statutSansBoolField001_refuse() throws Exception {
        UUID parent = terrains.terrain(c, code("PST"));

        int statut = creerSousTerrain(parent, code("SST")).andReturn().getResponse().getStatus();

        assertAll(
                () -> assertThat(statut).isIn(400, 409),
                () -> assertThat(terrains.compter(ENFANTS, parent)).isZero());
    }

    /** R1.2 CreateUnderLandProject : copie société, statut, vocation, catégorie, nature, pays, ville, niveau… */
    @Test
    void r1_2_sousTerrain_statutAvecBoolField001_201_copieDuParent() throws Exception {
        UUID parent = terrains.terrain(c, code("PSA"));
        UUID statut = terrains.statutTerrain("SOUS-" + c.jeton(), 20, true);
        UUID niveau = terrains.referenceFonciere("LndLevel", "NIV-" + c.jeton());
        UUID vocationTerrain = terrains.referenceFonciere("LndVocation", "VT-" + c.jeton());
        UUID orientation = fiche.reference(FixtureArticleFiche.ORIENTATION, "ORI-" + c.jeton());
        UUID charge = fiche.reference(FixtureArticleFiche.CATEGORIE_CHARGE, "CHG-" + c.jeton());
        terrains.definir(parent, "StrStatusId", statut);
        terrains.definir(parent, "LndLevelId", niveau);
        terrains.definir(parent, "LndVocationId", vocationTerrain);
        terrains.definir(parent, "StkOrientationId", orientation);
        terrains.definir(parent, "StkCostCategoryId", charge);
        terrains.definir(parent, "Address", "12 rue du Terrain");
        terrains.definir(parent, "GmapLink", "https://maps.example/terrain");
        terrains.definir(parent, "IsFinished", false);

        creerSousTerrain(parent, code("SSA")).andExpect(status().isCreated());

        List<String> enfants = terrains.guids("SELECT PKey FROM StkItem WHERE ParentId = :id", parent);
        assertThat(enfants).hasSize(1);
        Tuple s = ligne(UUID.fromString(enfants.get(0)));
        assertAll(
                () -> assertThat(s.get("IsFinished")).isEqualTo(true),
                () -> assertThat(guid(s.get("CfgCompanyId"))).isEqualTo(guid(c.entite().getId())),
                () -> assertThat(guid(s.get("StrStatusId"))).isEqualTo(guid(statut)),
                () -> assertThat(guid(s.get("StkVocationId"))).isEqualTo(guid(FixturePatrimoine.VOCATION_PROJET)),
                () -> assertThat(guid(s.get("StkItemCategoryId"))).isEqualTo(guid(c.categorie().getId())),
                () -> assertThat(guid(s.get("StkKindId"))).isEqualTo(guid(c.natureTer().getId())),
                () -> assertThat(guid(s.get("StkItemTypeId"))).isEqualTo(guid(c.type().getId())),
                () -> assertThat(guid(s.get("AdmCountryId"))).isEqualTo(guid(c.pays().getId())),
                () -> assertThat(guid(s.get("AdmCityId"))).isEqualTo(guid(c.ville())),
                () -> assertThat(guid(s.get("LndLevelId"))).isEqualTo(guid(niveau)),
                () -> assertThat(guid(s.get("LndVocationId"))).isEqualTo(guid(vocationTerrain)),
                () -> assertThat(guid(s.get("StkOrientationId"))).isEqualTo(guid(orientation)),
                () -> assertThat(guid(s.get("StkCostCategoryId"))).isEqualTo(guid(charge)),
                () -> assertThat(s.get("Address")).isEqualTo("12 rue du Terrain"),
                () -> assertThat(s.get("GmapLink")).isEqualTo("https://maps.example/terrain"));
    }

    // ------------------------------------------------------------------ §1.2 visibilité (règle 6)

    @Test
    void r1_2_visibilite_societeInactive_404_enLectureEtEnEcriture() throws Exception {
        UUID terrain = terrains.terrain(c, code("INA"));
        mockMvc.perform(get(TERRAIN, terrain).with(connecte(admin))).andExpect(status().isOk());
        entites.actif(c.entite().getId(), false);

        mockMvc.perform(get(TERRAIN, terrain).with(connecte(admin))).andExpect(status().isNotFound());
        modifier(terrain, corps().avec(DESCRIPTION, "x")).andExpect(status().isNotFound());

        assertThat(ligne(terrain).get("Description")).isEqualTo("Terrain " + code("INA"));
    }

    /** R1.2 : findVisible gagne la société directe — un porteur « article » terrain d'une société inactive → 404. */
    @Test
    void r1_2_visibilite_porteurArticle_societeInactive_404() throws Exception {
        UUID terrain = terrains.terrain(c, code("PIN"));
        entites.actif(c.entite().getId(), false);

        mockMvc.perform(get("/api/articles/{id}/actions", terrain).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }

    /** R1.2 : audit, notes, actions et pièces jointes acceptent un terrain comme porteur « article ». */
    @Test
    void r1_2_porteursArticle_acceptentUnTerrain() throws Exception {
        UUID terrain = terrains.terrain(c, code("POR"));

        mockMvc.perform(get("/api/articles/{id}/actions", terrain).with(connecte(admin))).andExpect(status().isOk());
        mockMvc.perform(get("/api/articles/{id}/pieces-jointes", terrain).with(connecte(admin)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/notes").param("porteur", "article").param("id", terrain.toString())
                .with(connecte(admin))).andExpect(status().isOk());
        mockMvc.perform(get("/api/audit").param("objetId", terrain.toString()).with(connecte(admin)))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ §1.2 liste

    private static String ligneListe(UUID id) {
        return "$.content[?(@.id=='" + id + "')]";
    }

    @Test
    void r1_2_liste_filtreSocieteEtRecherche_terrainsSeulement() throws Exception {
        UUID terrain = terrains.terrain(c, code("LST"));
        UUID bati = terrains.terrain(c, code("LSB"));
        terrains.definir(bati, "StkKindId", fixture.natureBat().getId());
        Contexte autre = terrains.contexte("P7L");
        UUID ailleurs = terrains.terrain(autre, "LSA-" + c.jeton());

        mockMvc.perform(get(TERRAINS).param("entiteId", c.entite().getId().toString()).param("size", "200")
                        .with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligneListe(terrain)).isNotEmpty())
                .andExpect(jsonPath(ligneListe(bati)).isEmpty())
                .andExpect(jsonPath(ligneListe(ailleurs)).isEmpty());
        mockMvc.perform(get(TERRAINS).param("search", c.jeton()).param("size", "200").with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligneListe(terrain)).isNotEmpty())
                .andExpect(jsonPath(ligneListe(ailleurs)).isNotEmpty())
                .andExpect(jsonPath(ligneListe(bati)).isEmpty());
    }

    @Test
    void r1_2_liste_societeInactive_exclue() throws Exception {
        UUID terrain = terrains.terrain(c, code("LIN"));
        entites.actif(c.entite().getId(), false);

        mockMvc.perform(get(TERRAINS).param("search", c.jeton()).param("size", "200").with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligneListe(terrain)).isEmpty());
    }

    // ------------------------------------------------------------------ §0 écriture Administrateur

    @Test
    void r0_creation_commercial_403_rienEcrit() throws Exception {
        AuthenticatedUser principal = new AuthenticatedUser(admin.getId(), admin.getCode(), null, false, true, 0);

        mockMvc.perform(post(TERRAINS).with(authentication(new UsernamePasswordAuthenticationToken(principal, null,
                                List.of(new SimpleGrantedAuthority("ROLE_Commercial")))))
                        .contentType(MediaType.APPLICATION_JSON).content(corpsTerrain(code("COM")).json()))
                .andExpect(status().isForbidden());

        assertThat(articlesDeCode(code("COM"))).isZero();
    }
}

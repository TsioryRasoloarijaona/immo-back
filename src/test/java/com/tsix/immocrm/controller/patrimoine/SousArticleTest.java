package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkBuilding;
import com.tsix.immocrm.entities.stk.StkFloor;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.VOCATION_VENTE;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.decimal;
import static com.tsix.immocrm.fixtures.FixturePatrimoine.guid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sous-articles : création rattachée, affectation, désaffectation, liste des libres —
 * FEATURE-patrimoine-ecriture.md §5.1, §5.4 (règles 5 à 8), §5.5, §11 (SousArticleTest).
 * Parent = cas §5.5 saisi TTC (63 m² × 13 000 = 819 000 TTC, 682 500 HT) ; sous-article = 10 m² à 5 000 HT, TVA 0,20 → 60 000 TTC / 50 000 HT.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SousArticleTest {

    private static final String SOUS_ARTICLES = "/api/articles/{id}/sous-articles";
    private static final String AFFECTER = "/api/articles/{id}/sous-articles/{sousArticleId}/affecter";
    private static final String DESAFFECTER = "/api/articles/{id}/sous-articles/{sousArticleId}/desaffecter";
    private static final String LIBRES = "/api/tranches/{id}/sous-articles-libres";
    private static final String PRIX_PARENT = "819000";
    private static final String PRIX_PARENT_AVEC_SOUS = "879000";
    private static final String TOUJOURS_AFFECTE = "toujoursAffecte";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private StkItemRepository stkItemRepository;

    private AdmUser admin;
    private CfgProject projet;
    private CfgTranche tranche;
    private StkBuilding immeuble;
    private StkFloor etage;
    private StkItemCategory categorieArticle;
    private StkItemType typeArticle;
    private StkItemCategory categorieSous;
    private StkItemType typeSous;

    @BeforeEach
    void setUp() {
        fixture.referentielArticle();
        admin = fixture.administrateur();
        CfgCompany entite = fixture.entite("ENT-SA-ECR", true);
        projet = fixture.projet("PRJ-SA-ECR", entite);
        tranche = fixture.tranche("TR-SA-ECR", projet);
        immeuble = fixture.immeuble("IMM-SA-ECR");
        etage = fixture.etage("ETG-SA-ECR");
        categorieArticle = fixture.categorie("CAT-SA-ART", fixture.natureBat(), false);
        typeArticle = fixture.type("TYP-SA-ART", fixture.natureBat(), false);
        categorieSous = fixture.categorie("CAT-SA-SOUS", fixture.natureBat(), true);
        typeSous = fixture.type("TYP-SA-SOUS", fixture.natureBat(), true);
    }

    // ------------------------------------------------------------------ fixtures

    private UUID creerParent(String code, CfgTranche trancheParent) throws Exception {
        String corps = corps().avec("trancheId", trancheParent.getId()).avec("code", code)
                .avec("description", "Parent " + code).avec("vocationId", VOCATION_VENTE)
                .avec("categorieId", categorieArticle.getId()).avec("typeId", typeArticle.getId())
                .avec("immeubleId", immeuble.getId()).avec("etageId", etage.getId())
                .avec("surfaceProvisoire", new BigDecimal("63")).avec("surfaceDefinitive", new BigDecimal("63"))
                .avec("prixM2ProvisoireTTC", new BigDecimal("13000"))
                .avec("prixM2DefinitifTTC", new BigDecimal("13000"))
                .avec("tauxTva", new BigDecimal("0.20")).json();
        String json = mockMvc.perform(post("/api/articles").with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    private CorpsJson corpsSousArticle(String code) {
        return corps().avec("code", code).avec("description", "Parking " + code)
                .avec("categorieId", categorieSous.getId()).avec("typeId", typeSous.getId())
                .avec("etageId", etage.getId())
                .avec("surfaceProvisoire", BigDecimal.TEN).avec("surfaceDefinitive", BigDecimal.TEN)
                .avec("prixM2ProvisoireHT", new BigDecimal("5000")).avec("prixM2DefinitifHT", new BigDecimal("5000"))
                .avec("tauxTva", new BigDecimal("0.20"));
    }

    private ResultActions creerSousArticle(UUID parentId, CorpsJson corps) throws Exception {
        return mockMvc.perform(post(SOUS_ARTICLES, parentId).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps.json()));
    }

    private UUID creerSousArticleOk(UUID parentId, String code) throws Exception {
        String json = creerSousArticle(parentId, corpsSousArticle(code)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    private ResultActions affecter(UUID parentId, UUID sousArticleId, boolean toujoursAffecte) throws Exception {
        return mockMvc.perform(post(AFFECTER, parentId, sousArticleId).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON).content(corps().avec(TOUJOURS_AFFECTE, toujoursAffecte).json()));
    }

    private ResultActions desaffecter(UUID parentId, UUID sousArticleId) throws Exception {
        return mockMvc.perform(post(DESAFFECTER, parentId, sousArticleId).with(connecte(admin)));
    }

    /** Sous-article libre écrit comme OMRAN : prix stockés cohérents avec ses prix au m². */
    private StkItem sousArticleLibre(String code, CfgTranche trancheSous) {
        StkItem item = new StkItem();
        item.setId(UUID.randomUUID());
        item.setCode(code);
        item.setDescription("Parking " + code);
        item.setStkHierarchy(trancheSous);
        item.setStkVocation(fixture.vocationVente());
        item.setStkKind(fixture.natureBat());
        item.setStkItemCategory(categorieSous);
        item.setStkItemType(typeSous);
        item.setStkBuilding(immeuble);
        item.setStkFloor(etage);
        item.setIsUnderItem(true);
        item.setIsUnderItemOrigin(true);
        item.setIsTitled(false);
        item.setAlwaysAffected(false);
        item.setArea(BigDecimal.TEN);
        item.setAreaFloor(BigDecimal.TEN);
        item.setVat(new BigDecimal("0.2"));
        item.setComSaleUnitCostHT(new BigDecimal("5000"));
        item.setComSaleUnitCostFloorHT(new BigDecimal("5000"));
        item.setComSaleUnitCost(new BigDecimal("6000"));
        item.setComSaleUnitCostFloor(new BigDecimal("6000"));
        item.setSalePrice(new BigDecimal("60000"));
        item.setSalePriceFloor(new BigDecimal("60000"));
        item.setComSalePrice(new BigDecimal("60000"));
        item.setComSalePriceFloor(new BigDecimal("60000"));
        item.setSalePriceHT(new BigDecimal("50000"));
        item.setSalePriceFloorHT(new BigDecimal("50000"));
        item.setComSalePriceHT(new BigDecimal("50000"));
        item.setComSalePriceFloorHT(new BigDecimal("50000"));
        return stkItemRepository.saveAndFlush(item);
    }

    private void occuper(UUID articleId, String codeDossier) {
        StkItem article = stkItemRepository.findById(articleId).orElseThrow();
        article.setComFolderId(fixture.dossier(codeDossier).getId());
        stkItemRepository.saveAndFlush(article);
    }

    private void rattacher(StkItem sousArticle, UUID parentId, boolean toujoursAffecte) {
        StkItem fraisSousArticle = stkItemRepository.findById(sousArticle.getId()).orElseThrow();
        fraisSousArticle.setParent(stkItemRepository.findById(parentId).orElseThrow());
        fraisSousArticle.setAlwaysAffected(toujoursAffecte);
        stkItemRepository.saveAndFlush(fraisSousArticle);
    }

    // ------------------------------------------------------------------ §5.4.5 création rattachée

    @Test
    void r5_4_5_creation_rattacheeAuParent_memeTrancheVocationImmeuble() throws Exception {
        UUID parentId = creerParent("SA-PARENT-CRE", tranche);

        UUID sousId = creerSousArticleOk(parentId, "SA-CRE");

        Tuple s = fixture.ligneArticle(sousId);
        assertAll(
                () -> assertThat(guid(s.get("ParentId"))).isEqualTo(guid(parentId)),
                () -> assertThat(s.get("IsUnderItem")).isEqualTo(true),
                () -> assertThat(s.get("IsUnderItemOrigin")).isEqualTo(true),
                () -> assertThat(s.get("IsTitled")).isEqualTo(false),
                () -> assertThat(s.get("AlwaysAffected")).isEqualTo(false),
                () -> assertThat(guid(s.get("StkHierarchyId"))).isEqualTo(guid(tranche.getId())),
                () -> assertThat(guid(s.get("StkVocationId"))).isEqualTo(guid(VOCATION_VENTE)),
                () -> assertThat(guid(s.get("StkBuildingId"))).as("immeuble hérité du parent")
                        .isEqualTo(guid(immeuble.getId())),
                () -> assertThat(decimal(s.get("SalePrice"))).isEqualByComparingTo("60000"),
                () -> assertThat(decimal(s.get("ComSalePrice"))).as("sous-article : ComSalePrice = SalePrice")
                        .isEqualByComparingTo("60000"));
    }

    @Test
    void r5_5_creation_recalculDuParent() throws Exception {
        UUID parentId = creerParent("SA-PARENT-RECALC", tranche);

        creerSousArticleOk(parentId, "SA-RECALC");

        Tuple p = fixture.ligneArticle(parentId);
        assertAll(
                () -> assertThat(decimal(p.get("SalePrice"))).isEqualByComparingTo(PRIX_PARENT),
                () -> assertThat(decimal(p.get("ComSalePrice"))).isEqualByComparingTo(PRIX_PARENT_AVEC_SOUS),
                () -> assertThat(decimal(p.get("ComSalePriceFloor"))).isEqualByComparingTo(PRIX_PARENT_AVEC_SOUS),
                () -> assertThat(decimal(p.get("SalePriceHT"))).isEqualByComparingTo("682500"),
                () -> assertThat(decimal(p.get("ComSalePriceHT"))).isEqualByComparingTo("732500"),
                () -> assertThat(decimal(p.get("ComSalePriceFloorHT"))).isEqualByComparingTo("732500"));
    }

    /** §14.7 : prixArticle* = article seul (SalePrice*), prixVente* = article + sous-articles (ComSalePrice*). */
    @Test
    void r14_7_detailDuParent_prixArticleSeulEtPrixDeVente() throws Exception {
        UUID parentId = creerParent("SA-PARENT-DTO", tranche);
        creerSousArticleOk(parentId, "SA-DTO");

        mockMvc.perform(get("/api/articles/{id}", parentId).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prixArticleDefinitifTTC").value(819000))
                .andExpect(jsonPath("$.prixArticleDefinitifHT").value(682500))
                .andExpect(jsonPath("$.prixArticleProvisoireTTC").value(819000))
                .andExpect(jsonPath("$.prixVenteDefinitifTTC").value(879000))
                .andExpect(jsonPath("$.prixVenteDefinitifHT").value(732500))
                .andExpect(jsonPath("$.prixVenteProvisoireTTC").value(879000))
                .andExpect(jsonPath("$.prixVenteProvisoireHT").value(732500));
    }

    @Test
    void r14_4_trancheIdDansLeCorpsDUnSousArticle_400() throws Exception {
        UUID parentId = creerParent("SA-PARENT-TR", tranche);

        creerSousArticle(parentId, corpsSousArticle("SA-TR").avec("trancheId", tranche.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail")
                        .value(containsString("La tranche d'un sous-article est celle de son article")));
    }

    /** FEATURE-patrimoine-article-fiche.md §4.1, §9.5 : la vocation d'un sous-article est celle de son article. */
    @Test
    void fiche_r9_5_vocationIdDansLeCorpsDUnSousArticle_400_surLeChamp() throws Exception {
        UUID parentId = creerParent("SA-PARENT-VOC", tranche);

        creerSousArticle(parentId, corpsSousArticle("SA-VOC").avec("vocationId", VOCATION_VENTE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsStringIgnoringCase("vocation")));
    }

    @Test
    void r5_2_creation_immeubleFourniPrimeSurLeParent() throws Exception {
        UUID parentId = creerParent("SA-PARENT-IMM", tranche);
        StkBuilding autre = fixture.immeuble("IMM-SA-AUTRE");

        String json = creerSousArticle(parentId, corpsSousArticle("SA-IMM").avec("immeubleId", autre.getId()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        assertThat(guid(fixture.ligneArticle(UUID.fromString(JsonPath.read(json, "$.id"))).get("StkBuildingId")))
                .isEqualTo(guid(autre.getId()));
    }

    @Test
    void r5_4_5_parentEstUnSousArticle_400() throws Exception {
        UUID parentId = creerParent("SA-PARENT-NIV", tranche);
        UUID sousId = creerSousArticleOk(parentId, "SA-NIV1");

        creerSousArticle(sousId, corpsSousArticle("SA-NIV2")).andExpect(status().isBadRequest());
    }

    @Test
    void r5_4_5_parentOccupe_409() throws Exception {
        UUID parentId = creerParent("SA-PARENT-OCC", tranche);
        occuper(parentId, "DOS-SA-PARENT-OCC");

        creerSousArticle(parentId, corpsSousArticle("SA-PARENT-OCC-1")).andExpect(status().isConflict());
    }

    @Test
    void r5_4_1_categoriePourArticlePrincipal_400() throws Exception {
        UUID parentId = creerParent("SA-PARENT-CAT", tranche);

        creerSousArticle(parentId, corpsSousArticle("SA-CAT").avec("categorieId", categorieArticle.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void r5_4_2_codeDejaPrisDansLaTranche_409() throws Exception {
        UUID parentId = creerParent("SA-PARENT-DBL", tranche);

        creerSousArticle(parentId, corpsSousArticle("SA-PARENT-DBL")).andExpect(status().isConflict());
    }

    @Test
    void r5_1_parentInconnu_404() throws Exception {
        creerSousArticle(UUID.randomUUID(), corpsSousArticle("SA-404")).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ §5.4.6 affecter

    @Test
    void r5_4_6_affecterUnLibre_200_parentRecalcule() throws Exception {
        UUID parentId = creerParent("SA-PARENT-AFF", tranche);
        StkItem libre = sousArticleLibre("SA-LIBRE-AFF", tranche);

        affecter(parentId, libre.getId(), true).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(parentId.toString()))
                .andExpect(jsonPath("$.prixVenteDefinitifTTC").value(879000));

        Tuple s = fixture.ligneArticle(libre.getId());
        Tuple p = fixture.ligneArticle(parentId);
        assertAll(
                () -> assertThat(guid(s.get("ParentId"))).isEqualTo(guid(parentId)),
                () -> assertThat(s.get("AlwaysAffected")).isEqualTo(true),
                () -> assertThat(decimal(p.get("ComSalePrice"))).isEqualByComparingTo(PRIX_PARENT_AVEC_SOUS));
    }

    @Test
    void r5_4_6_affecter_toujoursAffecteParDefautFalse() throws Exception {
        UUID parentId = creerParent("SA-PARENT-AFF-DEF", tranche);
        StkItem libre = sousArticleLibre("SA-LIBRE-AFF-DEF", tranche);

        mockMvc.perform(post(AFFECTER, parentId, libre.getId()).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());

        assertThat(fixture.ligneArticle(libre.getId()).get("AlwaysAffected")).isEqualTo(false);
    }

    @Test
    void r5_4_6_affecter_sousArticleToujoursAffecte_409() throws Exception {
        UUID parentId = creerParent("SA-PARENT-AFF-TA", tranche);
        StkItem nonLibre = sousArticleLibre("SA-NONLIBRE-TA", tranche);
        nonLibre.setAlwaysAffected(true);
        stkItemRepository.saveAndFlush(nonLibre);

        affecter(parentId, nonLibre.getId(), false).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("Ce sous article n'est pas libre")));
    }

    @Test
    void r5_4_6_affecter_sousArticleDUneAutreTranche_409() throws Exception {
        UUID parentId = creerParent("SA-PARENT-AFF-TR", tranche);
        StkItem ailleurs = sousArticleLibre("SA-AILLEURS", fixture.tranche("TR-SA-AUTRE", projet));

        affecter(parentId, ailleurs.getId(), false).andExpect(status().isConflict());
    }

    @Test
    void r5_4_6_affecter_sousArticleDejaRattache_409() throws Exception {
        UUID parentId = creerParent("SA-PARENT-AFF-RAT", tranche);
        UUID autreParentId = creerParent("SA-AUTRE-PARENT", tranche);
        StkItem rattache = sousArticleLibre("SA-RATTACHE", tranche);
        rattacher(rattache, autreParentId, false);

        affecter(parentId, rattache.getId(), false).andExpect(status().isConflict());
    }

    @Test
    void r5_4_6_affecter_sousArticleOccupe_409() throws Exception {
        UUID parentId = creerParent("SA-PARENT-AFF-OCC", tranche);
        StkItem occupe = sousArticleLibre("SA-OCCUPE", tranche);
        occuper(occupe.getId(), "DOS-SA-OCCUPE");

        affecter(parentId, occupe.getId(), false).andExpect(status().isConflict());
    }

    @Test
    void r5_4_6_affecter_parentOccupe_409() throws Exception {
        UUID parentId = creerParent("SA-PARENT-AFF-POCC", tranche);
        occuper(parentId, "DOS-SA-PARENT-AFF-POCC");
        StkItem libre = sousArticleLibre("SA-LIBRE-POCC", tranche);

        affecter(parentId, libre.getId(), false).andExpect(status().isConflict());
    }

    @Test
    void r5_4_6_affecter_cibleArticlePrincipal_409() throws Exception {
        UUID parentId = creerParent("SA-PARENT-AFF-PRINC", tranche);
        UUID principalId = creerParent("SA-PRINCIPAL", tranche);

        affecter(parentId, principalId, false).andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------ §5.4.7 désaffecter

    @Test
    void r5_4_7_desaffecter_200_parentNullAlwaysAffectedFaux_parentRecalcule() throws Exception {
        UUID parentId = creerParent("SA-PARENT-DES", tranche);
        UUID sousId = creerSousArticleOk(parentId, "SA-DES");
        assertThat(decimal(fixture.ligneArticle(parentId).get("ComSalePrice"))).isEqualByComparingTo(PRIX_PARENT_AVEC_SOUS);

        desaffecter(parentId, sousId).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(parentId.toString()));

        Tuple s = fixture.ligneArticle(sousId);
        Tuple p = fixture.ligneArticle(parentId);
        assertAll(
                () -> assertThat(s.get("ParentId")).isNull(),
                () -> assertThat(s.get("AlwaysAffected")).isEqualTo(false),
                () -> assertThat(decimal(p.get("ComSalePrice"))).isEqualByComparingTo(PRIX_PARENT));
    }

    @Test
    void r5_4_7_desaffecter_toujoursAffecteRemisAFaux() throws Exception {
        UUID parentId = creerParent("SA-PARENT-DES-TA", tranche);
        StkItem sous = sousArticleLibre("SA-DES-TA", tranche);
        rattacher(sous, parentId, true);

        desaffecter(parentId, sous.getId()).andExpect(status().isOk());

        assertThat(fixture.ligneArticle(sous.getId()).get("AlwaysAffected")).isEqualTo(false);
    }

    @Test
    void r5_4_7_desaffecter_sousArticleOccupe_409() throws Exception {
        UUID parentId = creerParent("SA-PARENT-DES-OCC", tranche);
        UUID sousId = creerSousArticleOk(parentId, "SA-DES-OCC");
        occuper(sousId, "DOS-SA-DES-OCC");

        desaffecter(parentId, sousId).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("désaffecter")));
    }

    @Test
    void r5_4_7_desaffecter_parentOccupe_409() throws Exception {
        UUID parentId = creerParent("SA-PARENT-DES-POCC", tranche);
        UUID sousId = creerSousArticleOk(parentId, "SA-DES-POCC");
        occuper(parentId, "DOS-SA-PARENT-DES-POCC");

        desaffecter(parentId, sousId).andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------ §5.4.8 modification d'un rattaché

    @Test
    void r5_4_8_modificationDuPrixDUnSousArticleRattache_recalculeLeParent() throws Exception {
        UUID parentId = creerParent("SA-PARENT-MOD", tranche);
        UUID sousId = creerSousArticleOk(parentId, "SA-MOD");

        modifierPrix(sousId).andExpect(status().isOk());

        // 10 m² × 10 000 × 1,2 = 120 000 ; parent = 819 000 + 120 000.
        assertThat(decimal(fixture.ligneArticle(parentId).get("ComSalePrice"))).isEqualByComparingTo("939000");
    }

    @Test
    void r5_4_8_parentOccupe_rendLeSousArticleOccupe_409() throws Exception {
        UUID parentId = creerParent("SA-PARENT-MOD-OCC", tranche);
        UUID sousId = creerSousArticleOk(parentId, "SA-MOD-OCC");
        occuper(parentId, "DOS-SA-PARENT-MOD-OCC");

        modifierPrix(sousId).andExpect(status().isConflict());
    }

    private ResultActions modifierPrix(UUID articleId) throws Exception {
        return mockMvc.perform(patch("/api/articles/{id}", articleId).with(connecte(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corps().avec("prixM2DefinitifHT", new BigDecimal("10000")).json()));
    }

    // ------------------------------------------------------------------ §5.1 sous-articles libres

    @Test
    void r5_1_sousArticlesLibres_exclutRattachesToujoursAffectesOccupesEtPrincipaux() throws Exception {
        UUID parentId = creerParent("SA-PARENT-LIB", tranche);
        StkItem libre = sousArticleLibre("SA-LIB-OK", tranche);
        StkItem rattache = sousArticleLibre("SA-LIB-RAT", tranche);
        rattacher(rattache, parentId, false);
        StkItem toujoursAffecte = sousArticleLibre("SA-LIB-TA", tranche);
        toujoursAffecte.setAlwaysAffected(true);
        stkItemRepository.saveAndFlush(toujoursAffecte);
        StkItem occupe = sousArticleLibre("SA-LIB-OCC", tranche);
        occuper(occupe.getId(), "DOS-SA-LIB-OCC");
        StkItem ailleurs = sousArticleLibre("SA-LIB-AILLEURS", fixture.tranche("TR-SA-LIB-AUTRE", projet));

        mockMvc.perform(get(LIBRES, tranche.getId()).param("size", "1000").with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(contenu(libre.getId())).exists())
                .andExpect(jsonPath(contenu(rattache.getId())).doesNotExist())
                .andExpect(jsonPath(contenu(toujoursAffecte.getId())).doesNotExist())
                .andExpect(jsonPath(contenu(occupe.getId())).doesNotExist())
                .andExpect(jsonPath(contenu(ailleurs.getId())).doesNotExist())
                .andExpect(jsonPath(contenu(parentId)).doesNotExist());
    }

    private static String contenu(UUID id) {
        return "$.content[?(@.id=='" + id + "')]";
    }
}

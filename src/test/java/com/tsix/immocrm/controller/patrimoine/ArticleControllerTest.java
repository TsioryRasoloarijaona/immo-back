package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkBuilding;
import com.tsix.immocrm.entities.stk.StkFloor;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkKind;
import com.tsix.immocrm.entities.stk.StkPresentationType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkBuildingRepository;
import com.tsix.immocrm.repositories.stk.StkFloorRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkKindRepository;
import com.tsix.immocrm.repositories.stk.StkPresentationTypeRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ArticleControllerTest {

    @Autowired
    private FixtureReferentielStk fixtureReferentielStk;
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CfgCompanyRepository companyRepository;

    @Autowired
    private CfgProjectRepository projectRepository;

    @Autowired
    private CfgTrancheRepository trancheRepository;

    @Autowired
    private StkItemRepository stkItemRepository;

    @Autowired
    private StkItemTypeRepository stkItemTypeRepository;

    @Autowired
    private StkItemCategoryRepository stkItemCategoryRepository;

    @Autowired
    private StkVocationRepository stkVocationRepository;

    @Autowired
    private StkBuildingRepository stkBuildingRepository;

    @Autowired
    private StkFloorRepository stkFloorRepository;

    @Autowired
    private StkKindRepository stkKindRepository;

    @Autowired
    private StkPresentationTypeRepository stkPresentationTypeRepository;

    @Autowired
    private ComFolderRepository folderRepository;

    @Autowired
    private StrStatusRepository strStatusRepository;

    @Autowired
    private CfgTierRepository tierRepository;

    private StkItemType itemType;
    private StkItemCategory itemCategory;
    private StkVocation vocation;
    private CfgTranche tranche;

    @BeforeEach
    void setUp() {
        itemType = new StkItemType();
        itemType.setId(UUID.randomUUID());
        itemType.setCode("TYPE-TEST");
        itemType.setDescription("2 CHAMBRES + SALON");
        itemType.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemTypeRepository.save(itemType);

        itemCategory = new StkItemCategory();
        itemCategory.setId(UUID.randomUUID());
        itemCategory.setCode("CAT-TEST");
        itemCategory.setDescription("APPARTEMENT");
        itemCategory.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemCategoryRepository.save(itemCategory);

        vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-TEST");
        stkVocationRepository.save(vocation);

        CfgCompany company = new CfgCompany();
        company.setId(UUID.randomUUID());
        company.setCode("ADMOD");
        company.setDescription("ADMOD");
        company.setIsActive(true);
        companyRepository.save(company);

        CfgProject project = new CfgProject();
        project.setId(UUID.randomUUID());
        project.setCode("PRJ-ART-TEST");
        project.setCfgCompany(company);
        projectRepository.save(project);

        tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-ART-TEST");
        tranche.setCfgProject(project);
        trancheRepository.save(tranche);
    }

    private StkItem newItem(String code, UUID comFolderId) {
        StkItem item = new StkItem();
        item.setId(UUID.randomUUID());
        item.setCode(code);
        item.setDescription("Lot de test " + code);
        item.setStkItemType(itemType);
        item.setStkItemCategory(itemCategory);
        item.setStkVocation(vocation);
        item.setStkHierarchy(tranche);
        item.setComFolderId(comFolderId);
        item.setStkKind(fixtureReferentielStk.natureParDefaut());
        return item;
    }

    @Test
    void findArticles_defaultsToDisponible() throws Exception {
        StkItem disponible = newItem("ART-DISPO", null);
        StkItem occupe = newItem("ART-OCCUPE", UUID.randomUUID());
        stkItemRepository.save(disponible);
        stkItemRepository.save(occupe);

        mockMvc.perform(get("/api/tranches/{id}/articles", tranche.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + disponible.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + occupe.getId() + "')]").doesNotExist());
    }

    @Test
    void findArticles_statutOccupe() throws Exception {
        StkItem disponible = newItem("ART-DISPO-2", null);
        StkItem occupe = newItem("ART-OCCUPE-2", UUID.randomUUID());
        stkItemRepository.save(disponible);
        stkItemRepository.save(occupe);

        mockMvc.perform(get("/api/tranches/{id}/articles", tranche.getId()).param("statut", "OCCUPE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + occupe.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + disponible.getId() + "')]").doesNotExist());
    }

    @Test
    void findArticles_unknownTranche_returns404() throws Exception {
        mockMvc.perform(get("/api/tranches/{id}/articles", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void findArticles_exposesCategorieEtTypeLibelles() throws Exception {
        StkItem item = newItem("ART-LIBELLE", null);
        stkItemRepository.save(item);

        mockMvc.perform(get("/api/tranches/{id}/articles", tranche.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + item.getId() + "' && @.categorieId=='"
                        + itemCategory.getId() + "' && @.categorieLibelle=='" + itemCategory.getDescription()
                        + "' && @.typeId=='" + itemType.getId() + "' && @.typeLibelle=='"
                        + itemType.getDescription() + "')]").exists());
    }

    @Test
    void findArticles_filtreParCategorieEtType() throws Exception {
        StkItemCategory autreCategorie = new StkItemCategory();
        autreCategorie.setId(UUID.randomUUID());
        autreCategorie.setCode("CAT-AUTRE");
        autreCategorie.setDescription("VILLA");
        autreCategorie.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemCategoryRepository.save(autreCategorie);

        StkItemType autreType = new StkItemType();
        autreType.setId(UUID.randomUUID());
        autreType.setCode("TYPE-AUTRE");
        autreType.setDescription("3 CHAMBRES + SALON");
        autreType.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemTypeRepository.save(autreType);

        StkItem appartementT2 = newItem("ART-APP-T2", null);
        stkItemRepository.save(appartementT2);

        StkItem villaT4 = newItem("ART-VILLA-T4", null);
        villaT4.setStkItemCategory(autreCategorie);
        villaT4.setStkItemType(autreType);
        stkItemRepository.save(villaT4);

        mockMvc.perform(get("/api/tranches/{id}/articles", tranche.getId())
                        .param("categorieId", itemCategory.getId().toString())
                        .param("typeId", itemType.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + appartementT2.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + villaT4.getId() + "')]").doesNotExist());
    }

    @Test
    void findById_returnsDetailWithSousLots() throws Exception {
        StkItem main = newItem("ART-MAIN", null);
        stkItemRepository.save(main);

        StkItem annexe = newItem("ART-ANNEXE", null);
        annexe.setParent(main);
        stkItemRepository.save(annexe);

        mockMvc.perform(get("/api/articles/{id}", main.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(main.getId().toString()))
                .andExpect(jsonPath("$.statut").value("DISPONIBLE"))
                .andExpect(jsonPath("$.trancheId").value(tranche.getId().toString()))
                .andExpect(jsonPath("$.trancheCode").value(tranche.getCode()))
                .andExpect(jsonPath("$.sousLots[?(@.id=='" + annexe.getId() + "')]").exists());
    }

    @Test
    void findById_comSalePriceZeroButFloorSet_exposesFloorPriceSeparately() throws Exception {
        // Repro of a real bug reported on ARS0804: ComSalePrice/SalePrice = 0 (never entered for
        // this lot) while SalePriceFloor/ComSalePriceFloor carry the real price (743400/619500) —
        // confirmed via INFORMATION_SCHEMA + real-data queries not to be an isolated case (99
        // other "disponible" articles share the same pattern). salePrice/comSalePrice must stay
        // exactly StkItem.SalePrice/ComSalePrice (untouched, still 0 here) — the floor price is
        // exposed as separate fields, not silently substituted.
        StkItem item = newItem("ARS0804-TEST", null);
        item.setSalePrice(BigDecimal.ZERO);
        item.setComSalePrice(BigDecimal.ZERO);
        item.setSalePriceFloor(new BigDecimal("743400.00"));
        item.setComSalePriceFloor(new BigDecimal("619500.00"));
        stkItemRepository.save(item);

        mockMvc.perform(get("/api/articles/{id}", item.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.salePrice").value(0))
                .andExpect(jsonPath("$.comSalePrice").value(0))
                .andExpect(jsonPath("$.salePriceFloor").value(743400.00))
                .andExpect(jsonPath("$.comSalePriceFloor").value(619500.00));
    }

    @Test
    void findById_exposesTauxTvaPresentationTypeAndSuperficieSousArticles() throws Exception {
        // instructions/from_front.md #4: TVA, Type Présentation and Superficie Sous Articles were
        // all confirmed-missing columns from the legacy "Articles" grid.
        StkPresentationType presentation = new StkPresentationType();
        presentation.setId(UUID.randomUUID());
        presentation.setCode("F");
        presentation.setDescription("F");
        presentation.setStkHierarchy(tranche);
        stkPresentationTypeRepository.save(presentation);

        StkItem main = newItem("ART-TVA-PRES", null);
        main.setVat(new BigDecimal("0.20"));
        main.setStkPresentationType(presentation);
        stkItemRepository.save(main);

        StkItem sousArticle1 = newItem("ART-TVA-PRES-SOUS1", null);
        sousArticle1.setParent(main);
        sousArticle1.setArea(new BigDecimal("12.50"));
        stkItemRepository.save(sousArticle1);

        StkItem sousArticle2 = newItem("ART-TVA-PRES-SOUS2", null);
        sousArticle2.setParent(main);
        sousArticle2.setArea(new BigDecimal("7.50"));
        stkItemRepository.save(sousArticle2);

        mockMvc.perform(get("/api/articles/{id}", main.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tauxTVA").value(0.20))
                .andExpect(jsonPath("$.presentationTypeId").value(presentation.getId().toString()))
                .andExpect(jsonPath("$.presentationTypeCode").value("F"))
                .andExpect(jsonPath("$.presentationTypeLibelle").value("F"))
                .andExpect(jsonPath("$.superficieSousArticles").value(20.0));
    }

    @Test
    void findById_sansSousArticle_superficieSousArticlesIsZero() throws Exception {
        StkItem item = newItem("ART-SANS-SOUS", null);
        stkItemRepository.save(item);

        mockMvc.perform(get("/api/articles/{id}", item.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.superficieSousArticles").value(0));
    }

    @Test
    void findArticles_filtreParNatureEtageVocationEtEntite() throws Exception {
        StkKind natureVilla = new StkKind();
        natureVilla.setId(UUID.randomUUID());
        natureVilla.setCode("VILLA-TEST");
        stkKindRepository.save(natureVilla);

        StkFloor floor = new StkFloor();
        floor.setId(UUID.randomUUID());
        floor.setCode("R1-TEST");
        stkFloorRepository.save(floor);

        StkItem correspond = newItem("ART-FILTRE-MATCH", null);
        correspond.setStkKind(natureVilla);
        correspond.setStkFloor(floor);
        stkItemRepository.save(correspond);

        StkItem autre = newItem("ART-FILTRE-AUTRE", null);
        stkItemRepository.save(autre);

        mockMvc.perform(get("/api/tranches/{id}/articles", tranche.getId())
                        .param("natureId", natureVilla.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + correspond.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + autre.getId() + "')]").doesNotExist());

        mockMvc.perform(get("/api/tranches/{id}/articles", tranche.getId())
                        .param("etageId", floor.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + correspond.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + autre.getId() + "')]").doesNotExist());

        mockMvc.perform(get("/api/tranches/{id}/articles", tranche.getId())
                        .param("vocationId", vocation.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + correspond.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + autre.getId() + "')]").exists());
    }

    @Test
    void findArticles_filtreParPrixEtSurfaceDeA() throws Exception {
        StkItem petit = newItem("ART-PRIX-PETIT", null);
        petit.setComSalePrice(new BigDecimal("500000"));
        petit.setArea(new BigDecimal("40"));
        stkItemRepository.save(petit);

        StkItem grand = newItem("ART-PRIX-GRAND", null);
        grand.setComSalePrice(new BigDecimal("1500000"));
        grand.setArea(new BigDecimal("120"));
        stkItemRepository.save(grand);

        mockMvc.perform(get("/api/tranches/{id}/articles", tranche.getId())
                        .param("prixDe", "1000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + grand.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + petit.getId() + "')]").doesNotExist());

        mockMvc.perform(get("/api/tranches/{id}/articles", tranche.getId())
                        .param("surfaceA", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + petit.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + grand.getId() + "')]").doesNotExist());
    }

    @Test
    void findAll_filtreParEntite() throws Exception {
        // GET /api/articles is already restricted to active entities (ADMOD/GHRIZA) — this
        // confirms entiteId further narrows within that set rather than only working on the
        // tranche-scoped endpoint.
        StkItem item = newItem("ART-ENTITE-TEST", null);
        stkItemRepository.save(item);

        CfgCompany autreEntite = new CfgCompany();
        autreEntite.setId(UUID.randomUUID());
        autreEntite.setCode("GHRIZA");
        autreEntite.setDescription("GHRIZA");
        autreEntite.setIsActive(true);
        companyRepository.save(autreEntite);

        mockMvc.perform(get("/api/articles")
                        .param("entiteId", tranche.getCfgProject().getCfgCompany().getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + item.getId() + "')]").exists());

        mockMvc.perform(get("/api/articles")
                        .param("entiteId", autreEntite.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + item.getId() + "')]").doesNotExist());
    }

    @Test
    void findById_sousArticle_exposesParentLotAndSousArticleFlag() throws Exception {
        StkItem main = newItem("ART-MAIN-2", null);
        main.setDescription("Lot principal");
        stkItemRepository.save(main);

        StkItem annexe = newItem("ART-ANNEXE-2", null);
        annexe.setParent(main);
        stkItemRepository.save(annexe);

        // Recherche inverse : partir directement de l'id du sous-article.
        mockMvc.perform(get("/api/articles/{id}", annexe.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lotParentId").value(main.getId().toString()))
                .andExpect(jsonPath("$.lotParentCode").value("ART-MAIN-2"))
                .andExpect(jsonPath("$.lotParentDescription").value("Lot principal"))
                .andExpect(jsonPath("$.trancheCode").value(tranche.getCode()));

        mockMvc.perform(get("/api/tranches/{id}/articles", tranche.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + annexe.getId() + "' && @.sousArticle==true)]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + main.getId() + "' && @.sousArticle==false)]").exists());
    }

    @Test
    void findById_exposesImmeubleEtageEtNature() throws Exception {
        StkBuilding building = new StkBuilding();
        building.setId(UUID.randomUUID());
        building.setCode("IMM01");
        building.setDescription("Immeuble 01");
        stkBuildingRepository.save(building);

        StkFloor floor = new StkFloor();
        floor.setId(UUID.randomUUID());
        floor.setCode("RDC");
        floor.setDescription("Rez de chaussée");
        stkFloorRepository.save(floor);

        StkKind kind = new StkKind();
        kind.setId(UUID.randomUUID());
        kind.setCode("BAT");
        kind.setDescription("Bâti");
        stkKindRepository.save(kind);

        StkItem item = newItem("ART-IMM-ETAGE", null);
        item.setStkBuilding(building);
        item.setStkFloor(floor);
        item.setStkKind(kind);
        stkItemRepository.save(item);

        mockMvc.perform(get("/api/articles/{id}", item.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.immeubleId").value(building.getId().toString()))
                .andExpect(jsonPath("$.immeubleCode").value("IMM01"))
                .andExpect(jsonPath("$.immeubleLibelle").value("Immeuble 01"))
                .andExpect(jsonPath("$.etageId").value(floor.getId().toString()))
                .andExpect(jsonPath("$.etageCode").value("RDC"))
                .andExpect(jsonPath("$.etageLibelle").value("Rez de chaussée"))
                .andExpect(jsonPath("$.natureId").value(kind.getId().toString()))
                .andExpect(jsonPath("$.natureCode").value("BAT"))
                .andExpect(jsonPath("$.natureLibelle").value("Bâti"));
    }

    @Test
    void findById_sansImmeubleNiEtage_champsNull() throws Exception {
        StkItem item = newItem("ART-SANS-IMM", null);
        stkItemRepository.save(item);

        mockMvc.perform(get("/api/articles/{id}", item.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.immeubleId").doesNotExist())
                .andExpect(jsonPath("$.etageId").doesNotExist())
                // natureId n'est volontairement pas asserté absent : StkItem.StkKindId est NOT NULL
                // en base, donc un article a toujours une nature. L'assertion précédente ne tenait
                // qu'à une fixture qui omettait la colonne, ce que la base réelle refuse.
                .andExpect(jsonPath("$.natureId").exists());
    }

    @Test
    void findById_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/articles/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void findSousArticles_defaultsToDisponible() throws Exception {
        StkItem main = newItem("ART-SA-MAIN", null);
        stkItemRepository.save(main);

        StkItem disponible = newItem("ART-SA-DISPO", null);
        disponible.setParent(main);
        stkItemRepository.save(disponible);

        StkItem occupe = newItem("ART-SA-OCCUPE", UUID.randomUUID());
        occupe.setParent(main);
        stkItemRepository.save(occupe);

        mockMvc.perform(get("/api/articles/{id}/sous-articles", main.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + disponible.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + occupe.getId() + "')]").doesNotExist());
    }

    @Test
    void findSousArticles_statutOccupe() throws Exception {
        StkItem main = newItem("ART-SA-MAIN-2", null);
        stkItemRepository.save(main);

        StkItem occupe = newItem("ART-SA-OCCUPE-2", UUID.randomUUID());
        occupe.setParent(main);
        stkItemRepository.save(occupe);

        mockMvc.perform(get("/api/articles/{id}/sous-articles", main.getId()).param("statut", "OCCUPE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + occupe.getId() + "')]").exists());
    }

    @Test
    void findSousArticles_unknownArticle_returns404() throws Exception {
        mockMvc.perform(get("/api/articles/{id}/sous-articles", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void findAll_defaultsToAllStatuts() throws Exception {
        StkItem disponible = newItem("ART-CAT-DISPO", null);
        StkItem occupe = newItem("ART-CAT-OCCUPE", UUID.randomUUID());
        stkItemRepository.save(disponible);
        stkItemRepository.save(occupe);

        mockMvc.perform(get("/api/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + disponible.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + occupe.getId() + "')]").exists());
    }

    @Test
    void findAll_filtreParSearchSurCodeOuDescription() throws Exception {
        StkItem parCode = newItem("ART-UNIQUE-CODE", null);
        stkItemRepository.save(parCode);

        StkItem parDescription = newItem("ART-AUTRE", null);
        parDescription.setDescription("Lot avec un nom distinctif");
        stkItemRepository.save(parDescription);

        StkItem sansRapport = newItem("ART-SANS-RAPPORT", null);
        stkItemRepository.save(sansRapport);

        mockMvc.perform(get("/api/articles").param("search", "unique-code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + parCode.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + sansRapport.getId() + "')]").doesNotExist());

        mockMvc.perform(get("/api/articles").param("search", "distinctif"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + parDescription.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + sansRapport.getId() + "')]").doesNotExist());
    }

    @Test
    void findAll_filtreParTrancheId() throws Exception {
        CfgTranche autreTranche = new CfgTranche();
        autreTranche.setId(UUID.randomUUID());
        autreTranche.setCode("TR-ART-AUTRE");
        autreTranche.setCfgProject(tranche.getCfgProject());
        trancheRepository.save(autreTranche);

        StkItem itemTranche1 = newItem("ART-TR1", null);
        stkItemRepository.save(itemTranche1);

        StkItem itemTranche2 = newItem("ART-TR2", null);
        itemTranche2.setStkHierarchy(autreTranche);
        stkItemRepository.save(itemTranche2);

        mockMvc.perform(get("/api/articles").param("trancheId", tranche.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + itemTranche1.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + itemTranche2.getId() + "')]").doesNotExist());
    }

    @Test
    void findArticles_exposeDossierEtClientQuandArticleReserve() throws Exception {
        StrStatus statutReservation = new StrStatus();
        statutReservation.setId(UUID.randomUUID());
        statutReservation.setCode("RESERVATION");
        strStatusRepository.save(statutReservation);

        CfgTier client = new CfgTier();
        client.setId(UUID.randomUUID());
        client.setCode("CLI-ART-TEST");
        client.setDescription("Client Test Article");
        tierRepository.save(client);

        ComFolder dossier = new ComFolder();
        dossier.setId(UUID.randomUUID());
        dossier.setCode("DOS-ART-TEST");
        dossier.setStkVocation(vocation);
        dossier.setStrStatus(statutReservation);
        dossier.setCfgCustomer(client);
        folderRepository.save(dossier);

        StkItem reserve = newItem("ART-RESERVE", dossier.getId());
        stkItemRepository.save(reserve);

        mockMvc.perform(get("/api/tranches/{id}/articles", tranche.getId()).param("statut", "OCCUPE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + reserve.getId() + "')].dossierId").value(dossier.getId().toString()))
                .andExpect(jsonPath("$.content[?(@.id=='" + reserve.getId() + "')].dossierCode").value("DOS-ART-TEST"))
                .andExpect(jsonPath("$.content[?(@.id=='" + reserve.getId() + "')].dossierStatut").value("RESERVATION"))
                .andExpect(jsonPath("$.content[?(@.id=='" + reserve.getId() + "')].clientId").value(client.getId().toString()))
                .andExpect(jsonPath("$.content[?(@.id=='" + reserve.getId() + "')].clientNom").value("Client Test Article"));

        mockMvc.perform(get("/api/articles/{id}", reserve.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dossierId").value(dossier.getId().toString()))
                .andExpect(jsonPath("$.dossierCode").value("DOS-ART-TEST"))
                .andExpect(jsonPath("$.dossierStatut").value("RESERVATION"))
                .andExpect(jsonPath("$.clientId").value(client.getId().toString()))
                .andExpect(jsonPath("$.clientNom").value("Client Test Article"));
    }

    @Test
    void findArticles_articleLibre_dossierEtClientSontNull() throws Exception {
        StkItem libre = newItem("ART-LIBRE-TEST", null);
        stkItemRepository.save(libre);

        mockMvc.perform(get("/api/tranches/{id}/articles", tranche.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + libre.getId() + "')].dossierId").value(Matchers.hasItem(Matchers.nullValue())))
                .andExpect(jsonPath("$.content[?(@.id=='" + libre.getId() + "')].clientId").value(Matchers.hasItem(Matchers.nullValue())));
    }

    @Test
    void findAll_excludesEntitesJuridiquesInactives() throws Exception {
        CfgCompany inactiveCompany = new CfgCompany();
        inactiveCompany.setId(UUID.randomUUID());
        inactiveCompany.setCode("YESTEST");
        inactiveCompany.setDescription("YESTEST");
        companyRepository.save(inactiveCompany);

        CfgProject inactiveProject = new CfgProject();
        inactiveProject.setId(UUID.randomUUID());
        inactiveProject.setCode("PRJ-INACTIVE");
        inactiveProject.setCfgCompany(inactiveCompany);
        projectRepository.save(inactiveProject);

        CfgTranche inactiveTranche = new CfgTranche();
        inactiveTranche.setId(UUID.randomUUID());
        inactiveTranche.setCode("TR-INACTIVE");
        inactiveTranche.setCfgProject(inactiveProject);
        trancheRepository.save(inactiveTranche);

        StkItem itemActif = newItem("ART-ACTIF", null);
        stkItemRepository.save(itemActif);

        StkItem itemInactif = newItem("ART-INACTIF", null);
        itemInactif.setStkHierarchy(inactiveTranche);
        stkItemRepository.save(itemInactif);

        mockMvc.perform(get("/api/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + itemActif.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + itemInactif.getId() + "')]").doesNotExist());
    }
}

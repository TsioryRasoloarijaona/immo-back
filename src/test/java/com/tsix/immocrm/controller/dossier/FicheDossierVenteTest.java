package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderItem;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderItemRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/dossiers/{id}/documents/fiche-dossier-vente} — reproduces the legacy
 * "FICHE COMPLETE" template (page 1: ENTETE/MONTANTS/DATE, page 2: AUTRES, page 3: SCHEMA DE
 * FINANCEMENT/SOUS ARTICLES/HISTORIQUE). See {@code FicheDossierVenteService}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class FicheDossierVenteTest {

    @Autowired
    private FixtureReferentielStk fixtureReferentielStk;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StkVocationRepository vocationRepository;
    @Autowired
    private StkItemTypeRepository itemTypeRepository;
    @Autowired
    private StkItemCategoryRepository itemCategoryRepository;
    @Autowired
    private StkItemRepository itemRepository;
    @Autowired
    private CfgProjectRepository projectRepository;
    @Autowired
    private CfgTrancheRepository trancheRepository;
    @Autowired
    private CfgCompanyRepository companyRepository;
    @Autowired
    private CfgTierRepository tierRepository;
    @Autowired
    private ComFolderRepository folderRepository;
    @Autowired
    private ComFolderItemRepository folderItemRepository;
    @Autowired
    private ComSettlementCategoryRepository settlementCategoryRepository;
    @Autowired
    private ComDocumentRepository documentRepository;
    @Autowired
    private StlSettlementRepository settlementRepository;
    @Autowired
    private StlSettlementLineRepository settlementLineRepository;

    @Test
    void getFicheDossierVente_pdf_returnsFullSummaryAcross3Pages() throws Exception {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-FDV-1");
        vocationRepository.save(vocation);

        StkItemType type = new StkItemType();
        type.setId(UUID.randomUUID());
        type.setCode("TYPE-FDV-1");
        type.setDescription("2 CHAMBRES + SALON");
        type.setStkKind(fixtureReferentielStk.natureParDefaut());
        itemTypeRepository.save(type);

        StkItemCategory category = new StkItemCategory();
        category.setId(UUID.randomUUID());
        category.setCode("CAT-FDV-1");
        category.setDescription("APPARTEMENT");
        category.setStkKind(fixtureReferentielStk.natureParDefaut());
        itemCategoryRepository.save(category);

        StkItemCategory categorieParking = new StkItemCategory();
        categorieParking.setId(UUID.randomUUID());
        categorieParking.setCode("CAT-FDV-PARK");
        categorieParking.setDescription("PARKING");
        categorieParking.setStkKind(fixtureReferentielStk.natureParDefaut());
        itemCategoryRepository.save(categorieParking);

        CfgCompany company = new CfgCompany();
        company.setId(UUID.randomUUID());
        company.setCode("CIE-FDV-1");
        company.setDescription("MAYLINE");
        companyRepository.save(company);

        CfgProject project = new CfgProject();
        project.setId(UUID.randomUUID());
        project.setCode("PRJ-FDV-1");
        project.setDescription("AYAD PRIVATE FERME BRETONE");
        project.setCfgCompany(company);
        projectRepository.save(project);

        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-FDV-1");
        tranche.setDescription("Tranche 1");
        tranche.setCfgProject(project);
        trancheRepository.save(tranche);

        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode("APF-0-1");
        article.setArea(new BigDecimal("60.00"));
        article.setStkItemType(type);
        article.setStkItemCategory(category);
        article.setStkVocation(vocation);
        article.setStkHierarchy(tranche);
        article.setStkKind(fixtureReferentielStk.natureParDefaut());
        itemRepository.save(article);

        StkItem parking = new StkItem();
        parking.setId(UUID.randomUUID());
        parking.setCode("APF-0-1-PARK");
        parking.setArea(new BigDecimal("1.00"));
        parking.setStkItemType(type);
        parking.setStkItemCategory(categorieParking);
        parking.setStkVocation(vocation);
        parking.setStkKind(fixtureReferentielStk.natureParDefaut());
        itemRepository.save(parking);

        CfgTier client = new CfgTier();
        client.setId(UUID.randomUUID());
        client.setCode("CLI-FDV-1");
        client.setDescription("Nor CHEMAOU");
        tierRepository.save(client);

        CfgTier notaire = new CfgTier();
        notaire.setId(UUID.randomUUID());
        notaire.setCode("NOT-FDV-1");
        notaire.setDescription("MAITRE FDV NOTAIRE");
        tierRepository.save(notaire);

        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode("APF 2022/00FDV1");
        folder.setDocDate(LocalDateTime.of(2022, 1, 10, 0, 0));
        folder.setStkVocation(vocation);
        folder.setStkItem(article);
        folder.setCfgCustomer(client);
        folder.setPartPercent(new BigDecimal("1.00"));
        folder.setUnitCost(new BigDecimal("30000"));
        folder.setItemPrice(new BigDecimal("1400000"));
        folder.setUnderItemsPrice(new BigDecimal("10000"));
        folder.setPriceBeforeDiscount(new BigDecimal("1410000"));
        folder.setDiscount(new BigDecimal("0.3617"));
        folder.setDiscountAmount(new BigDecimal("425000"));
        folder.setPriceAfterDiscount(new BigDecimal("900000"));
        folder.setAgreementPrice(new BigDecimal("900000"));
        folder.setVATRatio(new BigDecimal("0.20"));
        folder.setCfgLawyer(notaire);
        folder.setIsOldCustomer(true);
        folder.setNotes("Note libre du dossier FDV");
        folderRepository.save(folder);

        ComFolderItem mainItem = new ComFolderItem();
        mainItem.setId(UUID.randomUUID());
        mainItem.setComFolder(folder);
        mainItem.setStkItem(article);
        mainItem.setCode(article.getCode());
        mainItem.setIsUnderItem(false);
        mainItem.setPriceBeforeDiscount(new BigDecimal("1410000"));
        mainItem.setDiscount(new BigDecimal("0.3617"));
        mainItem.setPrice(new BigDecimal("900000"));
        folderItemRepository.save(mainItem);

        ComFolderItem sousArticle = new ComFolderItem();
        sousArticle.setId(UUID.randomUUID());
        sousArticle.setComFolder(folder);
        sousArticle.setStkItem(parking);
        sousArticle.setStkItemCategory(categorieParking);
        sousArticle.setCode(parking.getCode());
        sousArticle.setArea(parking.getArea());
        sousArticle.setIsUnderItem(true);
        sousArticle.setPrice(BigDecimal.ZERO);
        sousArticle.setDiscount(new BigDecimal("0.3617"));
        sousArticle.setAlwaysAffected(true);
        folderItemRepository.save(sousArticle);

        ComSettlementCategory autoFinancement = new ComSettlementCategory();
        autoFinancement.setId(UUID.randomUUID());
        autoFinancement.setCode("AUTO-FINANCEMENT");
        autoFinancement.setDescription("Auto-financement");
        autoFinancement.setIsSaleAutoFinancment(true);
        settlementCategoryRepository.save(autoFinancement);

        ComSettlementCategory fraisSyndic = new ComSettlementCategory();
        fraisSyndic.setId(UUID.randomUUID());
        fraisSyndic.setCode("FRAIS SYNDIC");
        fraisSyndic.setDescription("Frais syndic");
        fraisSyndic.setIsSaleExpense(true);
        settlementCategoryRepository.save(fraisSyndic);

        ComDocument echeanceAutoFinancement = new ComDocument();
        echeanceAutoFinancement.setId(UUID.randomUUID());
        echeanceAutoFinancement.setComFolder(folder);
        echeanceAutoFinancement.setComSettlementCategory(autoFinancement);
        echeanceAutoFinancement.setDocDate(LocalDateTime.of(2022, 2, 27, 0, 0));
        echeanceAutoFinancement.setAmount(new BigDecimal("900000"));
        documentRepository.save(echeanceAutoFinancement);

        ComDocument echeanceFrais = new ComDocument();
        echeanceFrais.setId(UUID.randomUUID());
        echeanceFrais.setComFolder(folder);
        echeanceFrais.setComSettlementCategory(fraisSyndic);
        echeanceFrais.setDocDate(LocalDateTime.of(2022, 1, 14, 0, 0));
        echeanceFrais.setAmount(new BigDecimal("16800"));
        documentRepository.save(echeanceFrais);

        StlSettlement settlement = new StlSettlement();
        settlement.setId(UUID.randomUUID());
        settlement.setDocDate(LocalDateTime.of(2022, 3, 10, 0, 0));
        settlement.setSettlementDate(LocalDateTime.of(2022, 3, 10, 0, 0));
        settlement.setCode("5980201");
        settlement.setDescription("CHÈQUE CAISSE DE DÉPÔT ET DE GESTION");
        settlement.setAmount(new BigDecimal("900000"));
        settlementRepository.save(settlement);

        StlSettlementLine line = new StlSettlementLine();
        line.setId(UUID.randomUUID());
        line.setStlSettlement(settlement);
        line.setComDocument(echeanceAutoFinancement);
        line.setAmount(new BigDecimal("900000"));
        settlementLineRepository.save(line);

        StlSettlement settlementFrais = new StlSettlement();
        settlementFrais.setId(UUID.randomUUID());
        settlementFrais.setDocDate(LocalDateTime.of(2022, 1, 14, 0, 0));
        settlementFrais.setSettlementDate(LocalDateTime.of(2022, 1, 14, 0, 0));
        settlementFrais.setCode("9628177");
        settlementFrais.setDescription("CHÉQUE SYNDIC CIH");
        settlementFrais.setAmount(new BigDecimal("16800"));
        settlementRepository.save(settlementFrais);

        StlSettlementLine lineFrais = new StlSettlementLine();
        lineFrais.setId(UUID.randomUUID());
        lineFrais.setStlSettlement(settlementFrais);
        lineFrais.setComDocument(echeanceFrais);
        lineFrais.setAmount(new BigDecimal("16800"));
        settlementLineRepository.save(lineFrais);

        byte[] pdf = mockMvc.perform(get("/api/dossiers/{id}/documents/fiche-dossier-vente", folder.getId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(pdf).isNotEmpty();
        String page1;
        String page2;
        String page3;
        try (PdfReader reader = new PdfReader(pdf)) {
            assertThat(reader.getNumberOfPages()).isEqualTo(3);
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            page1 = extractor.getTextFromPage(1);
            page2 = extractor.getTextFromPage(2);
            page3 = extractor.getTextFromPage(3);
        }

        // Page 1 : ENTETE + MONTANTS (fixed rows, not the raw settlement category codes)
        assertThat(page1)
                .contains("APF 2022/00FDV1")
                .contains("Nor CHEMAOU")
                .contains("MAYLINE")
                .contains("AYAD PRIVATE FERME BRETONE")
                .contains("APPARTEMENT")
                .contains("2 CHAMBRES + SALON")
                .contains("MONTANTS")
                .contains("Auto-financement")
                .contains("Net à payer")
                .contains("916 800.00 DH") // Frais 16 800 + Prix de vente 900 000
                .contains("900 000.00 DH")
                .contains("36.17 %");

        // Page 2 : AUTRES
        assertThat(page2)
                .contains("AUTRES")
                .contains("MAITRE FDV NOTAIRE")
                .contains("20.00 %");

        // Page 3 : SCHEMA DE FINANCEMENT (raw category code + règlement detail) + SOUS ARTICLES
        assertThat(page3)
                .contains("SCHEMA DE FINANCEMENT")
                .contains("AUTO-FINANCEMENT")
                .contains("FRAIS SYNDIC")
                .contains("CHÈQUE CAISSE DE DÉPÔT ET DE GESTION")
                .contains("CHÉQUE SYNDIC CIH")
                .contains("5980201")
                .contains("SOUS ARTICLES")
                .contains("APF-0-1-PARK")
                .contains("PARKING")
                .contains("HISTORIQUE");
    }

    @Test
    void getFicheDossierVente_unknownDossier_returns404() throws Exception {
        mockMvc.perform(get("/api/dossiers/{id}/documents/fiche-dossier-vente", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}

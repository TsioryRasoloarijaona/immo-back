package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderCustomer;
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
import com.tsix.immocrm.repositories.com.ComFolderCustomerRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
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
 * {@code GET /api/dossiers/{id}/documents/fiche-trop-percu} — modeled band-for-band on the legacy
 * Stimulsoft template {@code instructions/templates/ComSaleFicheTropPercu.yes}. See
 * {@code FicheTropPercuService} for field sourcing (acquéreurs via {@code ComFolder.cfgCustomer}/
 * {@code ComFolderCustomer}, versements via {@code StlSettlementLine} joined on
 * {@code ComDocument.comFolder}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class FicheTropPercuTest {

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
    private ComFolderCustomerRepository folderCustomerRepository;
    @Autowired
    private ComDocumentRepository documentRepository;
    @Autowired
    private StlSettlementRepository settlementRepository;
    @Autowired
    private StlSettlementLineRepository settlementLineRepository;

    private UUID setUpDossier(String suffix) {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-TP-" + suffix);
        vocationRepository.save(vocation);

        StkItemType type = new StkItemType();
        type.setId(UUID.randomUUID());
        type.setCode("TYPE-TP-" + suffix);
        type.setDescription("2 CHAMBRES + SALON");
        type.setStkKind(fixtureReferentielStk.natureParDefaut());
        itemTypeRepository.save(type);

        StkItemCategory category = new StkItemCategory();
        category.setId(UUID.randomUUID());
        category.setCode("CAT-TP-" + suffix);
        category.setDescription("APPARTEMENT");
        category.setStkKind(fixtureReferentielStk.natureParDefaut());
        itemCategoryRepository.save(category);

        CfgCompany company = new CfgCompany();
        company.setId(UUID.randomUUID());
        company.setCode("CIE-TP-" + suffix);
        company.setDescription("ADMOD");
        companyRepository.save(company);

        CfgProject project = new CfgProject();
        project.setId(UUID.randomUUID());
        project.setCode("PRJ-TP-" + suffix);
        project.setDescription("AYAD PRIVATE FERME BRETONE");
        project.setCity("Casablanca");
        project.setSignatureCity("Casablanca");
        project.setCfgCompany(company);
        projectRepository.save(project);

        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-TP-" + suffix);
        tranche.setCfgProject(project);
        trancheRepository.save(tranche);

        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode("APF-0-" + suffix);
        article.setArea(new BigDecimal("60.00"));
        article.setTitleNumber("TF-12345");
        article.setStkItemType(type);
        article.setStkItemCategory(category);
        article.setStkVocation(vocation);
        article.setStkHierarchy(tranche);
        article.setStkKind(fixtureReferentielStk.natureParDefaut());
        itemRepository.save(article);

        CfgTier client = new CfgTier();
        client.setId(UUID.randomUUID());
        client.setCode("CLI-TP-" + suffix);
        client.setDescription("Nor CHEMAOU");
        tierRepository.save(client);

        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode("APF 2022/00" + suffix);
        folder.setStkVocation(vocation);
        folder.setStkItem(article);
        folder.setCfgCustomer(client);
        folder.setDocDate(LocalDateTime.now());
        folder.setPriceAfterDiscount(new BigDecimal("1000000.00"));
        folder.setUnitCost(new BigDecimal("16666.67"));
        folderRepository.save(folder);

        ComDocument echeance = new ComDocument();
        echeance.setId(UUID.randomUUID());
        echeance.setComFolder(folder);
        echeance.setAmount(new BigDecimal("1000000.00"));
        documentRepository.save(echeance);

        StlSettlement settlement = new StlSettlement();
        settlement.setId(UUID.randomUUID());
        settlement.setDocDate(LocalDateTime.now());
        settlement.setDescription("Virement bancaire");
        settlement.setIsCredit(true);
        settlementRepository.save(settlement);

        StlSettlementLine ligne = new StlSettlementLine();
        ligne.setId(UUID.randomUUID());
        ligne.setStlSettlement(settlement);
        ligne.setComDocument(echeance);
        ligne.setAmount(new BigDecimal("1050000.00")); // trop perçu de 50 000,00 DH
        settlementLineRepository.save(ligne);

        return folder.getId();
    }

    @Test
    void getFicheTropPercu_pdf_showsMontantAndVersement() throws Exception {
        UUID dossierId = setUpDossier("1");

        byte[] pdf = mockMvc.perform(get("/api/dossiers/{id}/documents/fiche-trop-percu", dossierId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(pdf).isNotEmpty();
        String text;
        try (PdfReader reader = new PdfReader(pdf)) {
            // Le paragraphe justifié peut retourner à la ligne au milieu du nom de l'acquéreur
            // selon la largeur — on normalise les retours à la ligne avant de chercher un texte.
            text = new PdfTextExtractor(reader).getTextFromPage(1).replace('\n', ' ');
        }
        assertThat(text)
                .contains("TROP PERÇU N°")
                .contains("Nor")
                .contains("CHEMAOU")
                .contains("ADMOD")
                .contains("APPARTEMENT")
                .contains("Virement bancaire")
                .contains("50 000.00 DH")
                .contains("cinquante mille dirhams")
                .contains("Reste à payer");
    }

    @Test
    void getFicheTropPercu_unknownDossier_returns404() throws Exception {
        mockMvc.perform(get("/api/dossiers/{id}/documents/fiche-trop-percu", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}

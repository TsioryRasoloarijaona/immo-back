package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
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
 * {@code GET /api/dossiers/{id}/reglements/{reglementId}/documents/recu} — modeled on "Page1" of
 * the legacy Stimulsoft template
 * {@code instructions/templates/RECEIP/Yes_Stl_Front_Credit_Settlement_20240301102347.yes}. See
 * {@code RecuReglementService} for field sourcing (bénéficiaire via {@code StlSettlement.tier},
 * entreprise via {@code StlSettlement.cfgCompany}, situation des chèques via {@code StlSettlementLine}
 * joined on {@code ComDocument.comFolder}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class RecuReglementTest {

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
    private ComDocumentRepository documentRepository;
    @Autowired
    private StlSettlementRepository settlementRepository;
    @Autowired
    private StlSettlementLineRepository settlementLineRepository;

    private record DossierEtReglement(UUID dossierId, UUID reglementId) {
    }

    private DossierEtReglement setUpDossier(String suffix, boolean isCredit) {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-RR-" + suffix);
        vocationRepository.save(vocation);

        StkItemType type = new StkItemType();
        type.setId(UUID.randomUUID());
        type.setCode("TYPE-RR-" + suffix);
        type.setDescription("2 CHAMBRES + SALON");
        itemTypeRepository.save(type);

        StkItemCategory category = new StkItemCategory();
        category.setId(UUID.randomUUID());
        category.setCode("CAT-RR-" + suffix);
        category.setDescription("APPARTEMENT");
        itemCategoryRepository.save(category);

        CfgCompany reglementCompany = new CfgCompany();
        reglementCompany.setId(UUID.randomUUID());
        reglementCompany.setCode("CIE-RR-" + suffix);
        reglementCompany.setDescription("ADMOD");
        reglementCompany.setLegalForm("SARL");
        reglementCompany.setAddress("100 Boulevard Zerktouni");
        reglementCompany.setCity("Casablanca");
        reglementCompany.setSignatureCity("Casablanca");
        companyRepository.save(reglementCompany);

        CfgProject project = new CfgProject();
        project.setId(UUID.randomUUID());
        project.setCode("PRJ-RR-" + suffix);
        project.setDescription("AYAD PRIVATE FERME BRETONE");
        projectRepository.save(project);

        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-RR-" + suffix);
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
        itemRepository.save(article);

        CfgTier client = new CfgTier();
        client.setId(UUID.randomUUID());
        client.setCode("CLI-RR-" + suffix);
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
        settlement.setIsCredit(isCredit);
        settlement.setReceiptCode("RC-" + suffix);
        settlement.setTier(client);
        settlement.setCfgCompany(reglementCompany);
        settlementRepository.save(settlement);

        StlSettlementLine ligne = new StlSettlementLine();
        ligne.setId(UUID.randomUUID());
        ligne.setStlSettlement(settlement);
        ligne.setComDocument(echeance);
        ligne.setAmount(new BigDecimal("300000.00"));
        settlementLineRepository.save(ligne);

        return new DossierEtReglement(folder.getId(), settlement.getId());
    }

    @Test
    void getRecuReglement_versement_showsMontantAndBeneficiaire() throws Exception {
        DossierEtReglement ctx = setUpDossier("1", false);

        byte[] pdf = mockMvc.perform(get("/api/dossiers/{id}/reglements/{reglementId}/documents/recu",
                        ctx.dossierId(), ctx.reglementId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(pdf).isNotEmpty();
        String text;
        try (PdfReader reader = new PdfReader(pdf)) {
            text = new PdfTextExtractor(reader).getTextFromPage(1).replace('\n', ' ');
        }
        assertThat(text)
                .contains("ATTESTATION DE RÉCEPTION CHEQUE N°")
                .contains("RECU_RC-1")
                .contains("Nor")
                .contains("CHEMAOU")
                .contains("ADMOD")
                .contains("SARL")
                .contains("versement")
                .contains("APPARTEMENT")
                .contains("Virement bancaire")
                .contains("300 000.00 DH")
                .contains("trois cent mille dirhams")
                .contains("Reste à payer");
    }

    @Test
    void getRecuReglement_remboursement_usesInvertedWording() throws Exception {
        DossierEtReglement ctx = setUpDossier("2", true);

        byte[] pdf = mockMvc.perform(get("/api/dossiers/{id}/reglements/{reglementId}/documents/recu",
                        ctx.dossierId(), ctx.reglementId()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        String text;
        try (PdfReader reader = new PdfReader(pdf)) {
            text = new PdfTextExtractor(reader).getTextFromPage(1).replace('\n', ' ');
        }
        assertThat(text).contains("remboursement");
    }

    @Test
    void getRecuReglement_unknownDossier_returns404() throws Exception {
        mockMvc.perform(get("/api/dossiers/{id}/reglements/{reglementId}/documents/recu",
                        UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRecuReglement_unknownReglement_returns404() throws Exception {
        DossierEtReglement ctx = setUpDossier("3", false);

        mockMvc.perform(get("/api/dossiers/{id}/reglements/{reglementId}/documents/recu",
                        ctx.dossierId(), UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRecuReglement_reglementNotAttachedToDossier_returns404() throws Exception {
        DossierEtReglement ctxA = setUpDossier("4", false);
        DossierEtReglement ctxB = setUpDossier("5", false);

        mockMvc.perform(get("/api/dossiers/{id}/reglements/{reglementId}/documents/recu",
                        ctxA.dossierId(), ctxB.reglementId()))
                .andExpect(status().isNotFound());
    }
}

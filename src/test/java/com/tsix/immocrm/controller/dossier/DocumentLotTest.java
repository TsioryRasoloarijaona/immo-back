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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code POST /api/dossiers/documents/{type}} — "génération en lot" (select several dossiers,
 * get one multi-page PDF back). Scenario this was built for: select several dossiers in trop-perçu
 * and print all their "Fiche Trop Perçu" in one PDF. See {@code DocumentLotService}/{@code PdfMerger}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class DocumentLotTest {

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

    /** Même fixture minimale que {@code FicheTropPercuTest}/{@code FicheDossierVenteTest} — un dossier complet, prêt pour n'importe lequel des 6 générateurs. */
    private ComFolder setUpDossier(String suffix, String clientNom) {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-LOT-" + suffix);
        vocationRepository.save(vocation);

        StkItemType type = new StkItemType();
        type.setId(UUID.randomUUID());
        type.setCode("TYPE-LOT-" + suffix);
        type.setDescription("2 CHAMBRES + SALON");
        itemTypeRepository.save(type);

        StkItemCategory category = new StkItemCategory();
        category.setId(UUID.randomUUID());
        category.setCode("CAT-LOT-" + suffix);
        category.setDescription("APPARTEMENT");
        itemCategoryRepository.save(category);

        CfgCompany company = new CfgCompany();
        company.setId(UUID.randomUUID());
        company.setCode("CIE-LOT-" + suffix);
        company.setDescription("ADMOD");
        companyRepository.save(company);

        CfgProject project = new CfgProject();
        project.setId(UUID.randomUUID());
        project.setCode("PRJ-LOT-" + suffix);
        project.setDescription("AYAD PRIVATE FERME BRETONE");
        project.setCity("Casablanca");
        project.setSignatureCity("Casablanca");
        project.setCfgCompany(company);
        projectRepository.save(project);

        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-LOT-" + suffix);
        tranche.setCfgProject(project);
        trancheRepository.save(tranche);

        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode("APF-0-" + suffix);
        article.setArea(new BigDecimal("60.00"));
        article.setTitleNumber("TF-" + suffix);
        article.setStkItemType(type);
        article.setStkItemCategory(category);
        article.setStkVocation(vocation);
        article.setStkHierarchy(tranche);
        itemRepository.save(article);

        CfgTier client = new CfgTier();
        client.setId(UUID.randomUUID());
        client.setCode("CLI-LOT-" + suffix);
        client.setDescription(clientNom);
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

        return folder;
    }

    @Test
    void ficheTropPercuEnLot_deuxDossiers_mergeUnePageParDossier() throws Exception {
        ComFolder dossierA = setUpDossier("TPL1", "Nor CHEMAOU");
        ComFolder dossierB = setUpDossier("TPL2", "Amina RAKOTO");

        byte[] pdf = mockMvc.perform(post("/api/dossiers/documents/fiche-trop-percu")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dossierIds\":[\"" + dossierA.getId() + "\",\"" + dossierB.getId() + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(pdf).isNotEmpty();
        try (PdfReader reader = new PdfReader(pdf)) {
            assertThat(reader.getNumberOfPages()).isEqualTo(2);
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            String page1 = extractor.getTextFromPage(1).replace('\n', ' ');
            String page2 = extractor.getTextFromPage(2).replace('\n', ' ');
            assertThat(page1).contains("Nor").contains("CHEMAOU");
            assertThat(page2).contains("Amina").contains("RAKOTO");
        }
    }

    @Test
    void ficheDossierVenteEnLot_deuxDossiers_mergeToutesLesPages() throws Exception {
        ComFolder dossierA = setUpDossier("FDVL1", "Client Un");
        ComFolder dossierB = setUpDossier("FDVL2", "Client Deux");

        byte[] pdf = mockMvc.perform(post("/api/dossiers/documents/fiche-dossier-vente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dossierIds\":[\"" + dossierA.getId() + "\",\"" + dossierB.getId() + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(pdf).isNotEmpty();
        try (PdfReader reader = new PdfReader(pdf)) {
            // 3 pages par dossier (ENTETE/MONTANTS/DATE, AUTRES, SCHEMA/SOUS-ARTICLES/HISTORIQUE) x 2 dossiers.
            assertThat(reader.getNumberOfPages()).isEqualTo(6);
        }
    }

    @Test
    void genererLot_dossierIdsVide_returns400() throws Exception {
        mockMvc.perform(post("/api/dossiers/documents/fiche-trop-percu")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dossierIds\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void genererLot_dossierIdsManquant_returns400() throws Exception {
        mockMvc.perform(post("/api/dossiers/documents/fiche-trop-percu")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void genererLot_unDossierInconnuParmiPlusieurs_returns404() throws Exception {
        ComFolder dossierA = setUpDossier("TPL404", "Client Valide");

        mockMvc.perform(post("/api/dossiers/documents/fiche-trop-percu")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dossierIds\":[\"" + dossierA.getId() + "\",\"" + UUID.randomUUID() + "\"]}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void ficheReservationEnLot_unDossier_returnsOk() throws Exception {
        ComFolder dossier = setUpDossier("FRL1", "Client FR");

        mockMvc.perform(post("/api/dossiers/documents/fiche-reservation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dossierIds\":[\"" + dossier.getId() + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    @Test
    void acteReservationEnLot_unDossier_returnsOk() throws Exception {
        ComFolder dossier = setUpDossier("ARL1", "Client AR");

        mockMvc.perform(post("/api/dossiers/documents/acte-reservation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dossierIds\":[\"" + dossier.getId() + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    @Test
    void ficheBancaireEnLot_unDossier_returnsOk() throws Exception {
        ComFolder dossier = setUpDossier("FBL1", "Client FB");

        mockMvc.perform(post("/api/dossiers/documents/fiche-bancaire")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dossierIds\":[\"" + dossier.getId() + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    @Test
    void ficheVisiteTechniqueEnLot_unDossier_returnsOk() throws Exception {
        ComFolder dossier = setUpDossier("FVTL1", "Client FVT");

        mockMvc.perform(post("/api/dossiers/documents/fiche-visite-technique")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dossierIds\":[\"" + dossier.getId() + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }
}

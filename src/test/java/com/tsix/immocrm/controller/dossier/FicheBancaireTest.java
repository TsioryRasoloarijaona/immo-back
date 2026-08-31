package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.entities.adm.AdmProfession;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderCustomer;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkBuilding;
import com.tsix.immocrm.entities.stk.StkFloor;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.repositories.adm.AdmProfessionRepository;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderCustomerRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkBuildingRepository;
import com.tsix.immocrm.repositories.stk.StkFloorRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
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
 * {@code GET /api/dossiers/{id}/documents/fiche-bancaire} — modeled on the legacy
 * "FICHE BANCAIRE" template. See {@code FicheBancaireService} for field sourcing (main acquéreur
 * via {@code ComFolder.cfgCustomer}, co-acquéreurs via {@code ComFolderCustomer}, réglé/reste via
 * the same computation as {@code GET /api/dossiers/{id}/montants}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class FicheBancaireTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StkVocationRepository vocationRepository;
    @Autowired
    private StkItemTypeRepository itemTypeRepository;
    @Autowired
    private StkItemCategoryRepository itemCategoryRepository;
    @Autowired
    private StkBuildingRepository buildingRepository;
    @Autowired
    private StkFloorRepository floorRepository;
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
    private ComSettlementCategoryRepository settlementCategoryRepository;
    @Autowired
    private ComDocumentRepository documentRepository;
    @Autowired
    private AdmProfessionRepository professionRepository;

    private UUID setUpDossier(String suffix, boolean withBankRecordDate) {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-FB-" + suffix);
        vocationRepository.save(vocation);

        StkItemType type = new StkItemType();
        type.setId(UUID.randomUUID());
        type.setCode("TYPE-FB-" + suffix);
        itemTypeRepository.save(type);

        StkItemCategory category = new StkItemCategory();
        category.setId(UUID.randomUUID());
        category.setCode("CAT-FB-" + suffix);
        category.setDescription("APPARTEMENT");
        itemCategoryRepository.save(category);

        StkBuilding building = new StkBuilding();
        building.setId(UUID.randomUUID());
        building.setCode("IMM01-" + suffix);
        building.setDescription("Immeuble 01");
        buildingRepository.save(building);

        StkFloor floor = new StkFloor();
        floor.setId(UUID.randomUUID());
        floor.setDescription("Rez de chaussée");
        floorRepository.save(floor);

        CfgCompany company = new CfgCompany();
        company.setId(UUID.randomUUID());
        company.setCode("CIE-FB-" + suffix);
        company.setDescription("MAYLINE");
        companyRepository.save(company);

        CfgProject project = new CfgProject();
        project.setId(UUID.randomUUID());
        project.setCode("PRJ-FB-" + suffix);
        project.setDescription("AYAD PRIVATE FERME BRETONE");
        project.setCity("Casablanca");
        project.setCfgCompany(company);
        projectRepository.save(project);

        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-FB-" + suffix);
        tranche.setCfgProject(project);
        trancheRepository.save(tranche);

        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode("APF-0-" + suffix);
        article.setArea(new BigDecimal("60.00"));
        article.setDividedPart(new BigDecimal("50.00"));
        article.setUndividedPart(new BigDecimal("120.00"));
        article.setStkItemType(type);
        article.setStkItemCategory(category);
        article.setStkVocation(vocation);
        article.setStkBuilding(building);
        article.setStkFloor(floor);
        article.setStkHierarchy(tranche);
        itemRepository.save(article);

        AdmProfession profession = new AdmProfession();
        profession.setId(UUID.randomUUID());
        profession.setCode("PROF-FB-" + suffix);
        profession.setDescription("Ingénieur");
        professionRepository.save(profession);

        CfgTier client = new CfgTier();
        client.setId(UUID.randomUUID());
        client.setCode("CLI-FB-" + suffix);
        client.setDescription("Nor CHEMAOU");
        client.setAdmProfession(profession);
        tierRepository.save(client);

        CfgTier coAcquereur = new CfgTier();
        coAcquereur.setId(UUID.randomUUID());
        coAcquereur.setCode("CLI2-FB-" + suffix);
        coAcquereur.setDescription("Amal CHEMAOU");
        tierRepository.save(coAcquereur);

        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode("APF 2022/00" + suffix);
        folder.setStkVocation(vocation);
        folder.setStkItem(article);
        folder.setCfgCustomer(client);
        folder.setPriceAfterDiscount(new BigDecimal("1200000.00"));
        folder.setPartPercent(new BigDecimal("0.5"));
        if (withBankRecordDate) {
            folder.setBankRecordDate(LocalDateTime.now());
        }
        folderRepository.save(folder);

        ComFolderCustomer coCustomer = new ComFolderCustomer();
        coCustomer.setId(UUID.randomUUID());
        coCustomer.setComFolder(folder);
        coCustomer.setCfgTier(coAcquereur);
        coCustomer.setPartPercent(new BigDecimal("0.5"));
        folderCustomerRepository.save(coCustomer);

        ComSettlementCategory fraisDossier = new ComSettlementCategory();
        fraisDossier.setId(UUID.randomUUID());
        fraisDossier.setCode("FRAIS DOSSIER");
        settlementCategoryRepository.save(fraisDossier);

        ComDocument fraisLigne = new ComDocument();
        fraisLigne.setId(UUID.randomUUID());
        fraisLigne.setComFolder(folder);
        fraisLigne.setComSettlementCategory(fraisDossier);
        fraisLigne.setAmount(new BigDecimal("5000"));
        fraisLigne.setExcludeFromPrice(true);
        documentRepository.save(fraisLigne);

        return folder.getId();
    }

    @Test
    void getFicheBancaire_pdf_listsMainAndCoAcquereurs() throws Exception {
        UUID dossierId = setUpDossier("1", true);

        byte[] pdf = mockMvc.perform(get("/api/dossiers/{id}/documents/fiche-bancaire", dossierId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(pdf).isNotEmpty();
        String text;
        try (PdfReader reader = new PdfReader(pdf)) {
            text = new PdfTextExtractor(reader).getTextFromPage(1);
        }
        assertThat(text)
                .contains("Nor CHEMAOU")
                .contains("Amal CHEMAOU")
                .contains("Ingénieur")
                .contains("MAYLINE")
                .contains("FRAIS DE DOSSIER")
                .doesNotContain("Vous devez saisir la date");
    }

    @Test
    void getFicheBancaire_pdf_noBankRecordDate_showsWarning() throws Exception {
        UUID dossierId = setUpDossier("2", false);

        byte[] pdf = mockMvc.perform(get("/api/dossiers/{id}/documents/fiche-bancaire", dossierId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        String text;
        try (PdfReader reader = new PdfReader(pdf)) {
            text = new PdfTextExtractor(reader).getTextFromPage(1);
        }
        assertThat(text).contains("Vous devez saisir la date");
    }

    @Test
    void getFicheBancaire_unknownDossier_returns404() throws Exception {
        mockMvc.perform(get("/api/dossiers/{id}/documents/fiche-bancaire", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}

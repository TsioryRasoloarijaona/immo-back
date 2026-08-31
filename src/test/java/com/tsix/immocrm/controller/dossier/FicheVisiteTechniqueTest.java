package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.entities.adm.AdmTierTitle;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.repositories.adm.AdmTierTitleRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/dossiers/{id}/documents/fiche-visite-technique} — modeled on the legacy
 * "FICHE VISITE TECHNIQUE" template. See {@code FicheVisiteTechniqueService} for the "FAN"
 * tranche-code SAV-contact branching (confirmed against the real template).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class FicheVisiteTechniqueTest {

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
    private CfgTierRepository tierRepository;
    @Autowired
    private ComFolderRepository folderRepository;
    @Autowired
    private AdmTierTitleRepository titleRepository;

    private UUID setUpDossier(String suffix, String trancheCode) {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-FVT-" + suffix);
        vocationRepository.save(vocation);

        StkItemType type = new StkItemType();
        type.setId(UUID.randomUUID());
        type.setCode("TYPE-FVT-" + suffix);
        itemTypeRepository.save(type);

        StkItemCategory category = new StkItemCategory();
        category.setId(UUID.randomUUID());
        category.setCode("CAT-FVT-" + suffix);
        itemCategoryRepository.save(category);

        AdmTierTitle titre = new AdmTierTitle();
        titre.setId(UUID.randomUUID());
        titre.setCode("M.");
        titre.setDescription("Monsieur");
        titleRepository.save(titre);

        CfgTier responsable = new CfgTier();
        responsable.setId(UUID.randomUUID());
        responsable.setCode("RESP-FVT-" + suffix);
        responsable.setDescription("Karim BENANI");
        responsable.setAdmTierTitle(titre);
        responsable.setMobile("600000000");
        responsable.setEMail("karim.benani@ayaddeveloppement.com");
        tierRepository.save(responsable);

        CfgProject project = new CfgProject();
        project.setId(UUID.randomUUID());
        project.setCode("PRJ-FVT-" + suffix);
        project.setDescription("AYAD PRIVATE FERME BRETONE");
        project.setCfgTechnicalManager(responsable);
        projectRepository.save(project);

        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode(trancheCode);
        tranche.setCfgProject(project);
        tranche.setSavAddress("100 Boulevard Zerktouni, Casablanca");
        tranche.setWarrantyDelay(12);
        trancheRepository.save(tranche);

        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode("APF-0-" + suffix);
        article.setStkItemType(type);
        article.setStkItemCategory(category);
        article.setStkVocation(vocation);
        article.setStkHierarchy(tranche);
        itemRepository.save(article);

        CfgTier client = new CfgTier();
        client.setId(UUID.randomUUID());
        client.setCode("CLI-FVT-" + suffix);
        client.setDescription("Nor CHEMAOU");
        tierRepository.save(client);

        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode("APF 2022/00" + suffix);
        folder.setStkVocation(vocation);
        folder.setStkItem(article);
        folder.setCfgCustomer(client);
        folder.setReceptionDate(LocalDateTime.of(2026, 9, 1, 10, 30));
        folderRepository.save(folder);

        return folder.getId();
    }

    @Test
    void getFicheVisiteTechnique_pdf_nonFanTranche_showsFullTechnicalManagerContact() throws Exception {
        UUID dossierId = setUpDossier("1", "TR-STD");

        byte[] pdf = mockMvc.perform(get("/api/dossiers/{id}/documents/fiche-visite-technique", dossierId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(pdf).isNotEmpty();
        String text;
        try (PdfReader reader = new PdfReader(pdf)) {
            text = new PdfTextExtractor(reader).getTextFromPage(1);
        }
        assertThat(text).contains("Nor CHEMAOU").contains("APF-0-1");

        String textPage2;
        try (PdfReader reader = new PdfReader(pdf)) {
            textPage2 = new PdfTextExtractor(reader).getTextFromPage(2);
        }
        assertThat(textPage2).contains("responsable de tranche")
                .contains("Karim BENANI")
                .contains("600000000")
                .doesNotContain("responsable de projet par mail");
    }

    @Test
    void getFicheVisiteTechnique_pdf_fanTranche_showsEmailOnlyContact() throws Exception {
        UUID dossierId = setUpDossier("2", "FAN");

        byte[] pdf = mockMvc.perform(get("/api/dossiers/{id}/documents/fiche-visite-technique", dossierId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        String textPage2;
        try (PdfReader reader = new PdfReader(pdf)) {
            textPage2 = new PdfTextExtractor(reader).getTextFromPage(2);
        }
        assertThat(textPage2).contains("responsable de projet par mail")
                .contains("karim.benani@ayaddeveloppement.com")
                .doesNotContain("responsable de tranche");
    }

    @Test
    void getFicheVisiteTechnique_unknownDossier_returns404() throws Exception {
        mockMvc.perform(get("/api/dossiers/{id}/documents/fiche-visite-technique", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}

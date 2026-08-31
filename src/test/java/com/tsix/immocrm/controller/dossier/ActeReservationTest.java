package com.tsix.immocrm.controller.dossier;

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
import com.tsix.immocrm.entities.stk.StkVocation;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/dossiers/{id}/documents/acte-reservation} — modeled on the legacy
 * "ACTE RESERVATION" template. See {@code ActeReservationService} for the field sourcing
 * (CfgTranche pénalités/syndic, sous-articles via StkItem.ComFolderId).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ActeReservationTest {

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
    private CfgTierRepository tierRepository;
    @Autowired
    private ComFolderRepository folderRepository;
    @Autowired
    private CfgCompanyRepository companyRepository;
    @Autowired
    private StkKindRepository kindRepository;

    private UUID setUpDossier(String suffix) {
        return setUpDossier(suffix, false);
    }

    private UUID setUpDossier(String suffix, boolean terrain) {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-AR-" + suffix);
        vocationRepository.save(vocation);

        StkItemType type = new StkItemType();
        type.setId(UUID.randomUUID());
        type.setCode("TYPE-AR-" + suffix);
        itemTypeRepository.save(type);

        StkItemCategory category = new StkItemCategory();
        category.setId(UUID.randomUUID());
        category.setCode("CAT-AR-" + suffix);
        category.setDescription("APPARTEMENT");
        itemCategoryRepository.save(category);

        StkBuilding building = new StkBuilding();
        building.setId(UUID.randomUUID());
        building.setCode("IMM01-" + suffix);
        buildingRepository.save(building);

        StkFloor floor = new StkFloor();
        floor.setId(UUID.randomUUID());
        floor.setDescription("Rez de chaussée");
        floorRepository.save(floor);

        CfgCompany company = new CfgCompany();
        company.setId(UUID.randomUUID());
        company.setCode("CIE-AR-" + suffix);
        company.setDescription("MAYLINE");
        companyRepository.save(company);

        CfgProject project = new CfgProject();
        project.setId(UUID.randomUUID());
        project.setCode("PRJ-AR-" + suffix);
        project.setDescription("AYAD PRIVATE FERME BRETONE");
        project.setCity("Casablanca");
        project.setCfgCompany(company);
        projectRepository.save(project);

        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-AR-" + suffix);
        tranche.setCancellationPenalityPercent(new BigDecimal("15.00"));
        tranche.setCustomerTransferPenality(new BigDecimal("2.5"));
        tranche.setWarrantyDelay(12);
        tranche.setSyndicAmount(new BigDecimal("700.00"));
        tranche.setSyndicPeriod(24);
        tranche.setCfgProject(project);
        trancheRepository.save(tranche);

        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode("APF-0-" + suffix);
        article.setDescription("6");
        article.setArea(new BigDecimal("60.00"));
        article.setStkItemType(type);
        article.setStkItemCategory(category);
        article.setStkVocation(vocation);
        article.setStkBuilding(building);
        article.setStkFloor(floor);
        article.setCfgProject(project);
        article.setStkHierarchy(tranche);
        if (terrain) {
            StkKind kind = new StkKind();
            kind.setId(UUID.randomUUID());
            kind.setCode("TER");
            kindRepository.save(kind);
            article.setStkKind(kind);
        }
        itemRepository.save(article);

        CfgTier client = new CfgTier();
        client.setId(UUID.randomUUID());
        client.setCode("CLI-AR-" + suffix);
        client.setDescription("Nor CHEMAOU");
        client.setAddress("20 BIS CADI BEN HAMMADI");
        client.setIDNumber("A742122");
        tierRepository.save(client);

        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode("APF 2022/00" + suffix);
        folder.setStkVocation(vocation);
        folder.setStkItem(article);
        folder.setCfgCustomer(client);
        folder.setPrice(new BigDecimal("1200000.00"));
        folderRepository.save(folder);

        // second article linked to the same dossier, exercising the "sous-articles" designation.
        StkItem terrasse = new StkItem();
        terrasse.setId(UUID.randomUUID());
        terrasse.setCode("APF-0-" + suffix + "-TER");
        terrasse.setArea(new BigDecimal("16.00"));
        terrasse.setStkItemType(type);
        terrasse.setStkItemCategory(category);
        terrasse.setStkVocation(vocation);
        terrasse.setComFolderId(folder.getId());
        itemRepository.save(terrasse);

        return folder.getId();
    }

    @Test
    void getActeReservation_pdf_returnsPdfBytes() throws Exception {
        UUID dossierId = setUpDossier("PDF");

        byte[] pdf = mockMvc.perform(get("/api/dossiers/{id}/documents/acte-reservation", dossierId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }

    @Test
    void getActeReservation_docx_returnsDocxBytes() throws Exception {
        UUID dossierId = setUpDossier("DOCX");

        byte[] docx = mockMvc.perform(get("/api/dossiers/{id}/documents/acte-reservation", dossierId)
                        .param("format", "docx"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(docx).isNotEmpty();
        assertThat(docx[0]).isEqualTo((byte) 'P');
        assertThat(docx[1]).isEqualTo((byte) 'K');
    }

    @Test
    void getActeReservation_pdf_usesProjectCompanyNotHardcoded() throws Exception {
        UUID dossierId = setUpDossier("CIE");

        byte[] pdf = mockMvc.perform(get("/api/dossiers/{id}/documents/acte-reservation", dossierId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        String text = extractText(pdf);
        assertThat(text).contains("MAYLINE").doesNotContain("ADMOD");
    }

    @Test
    void getActeReservation_pdf_terrainArticle_omitsSavSectionAndFloorMention() throws Exception {
        UUID dossierId = setUpDossier("TER", true);

        byte[] pdf = mockMvc.perform(get("/api/dossiers/{id}/documents/acte-reservation", dossierId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        String text = extractText(pdf);
        assertThat(text).doesNotContain("SERVICE APRES VENTE")
                .doesNotContain("Rez de chaussée")
                .contains("8- PROTECTION DES DONNEES PERSONNELLES")
                .contains("9- ELECTION DE DOMICILE")
                .contains("10- ATTRIBUTION DE COMPÉTENCE");
    }

    private static String extractText(byte[] pdf) throws Exception {
        try (PdfReader reader = new PdfReader(pdf)) {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            StringBuilder sb = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                sb.append(extractor.getTextFromPage(page));
            }
            return sb.toString();
        }
    }

    @Test
    void getActeReservation_unknownDossier_returns404() throws Exception {
        mockMvc.perform(get("/api/dossiers/{id}/documents/acte-reservation", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}

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
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/dossiers/{id}/documents/fiche-reservation} — modeled on the legacy
 * "FICHE RESERVATION MBR" template. See {@code FicheReservationService} for the confirmed
 * StkBuilding/StkFloor linkage and the "AVANCE"-échéance-first règlement lookup.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class FicheReservationTest {

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
    private StlSettlementRepository settlementRepository;
    @Autowired
    private StlSettlementLineRepository settlementLineRepository;
    @Autowired
    private com.tsix.immocrm.repositories.com.ComDocumentRepository documentRepository;

    /** Par défaut la société correspond à la marque du code dossier (APF → ADMOD). */
    private UUID setUpDossier(String suffix) {
        return setUpDossier(suffix, "ADMOD");
    }

    private UUID setUpDossier(String suffix, String societe) {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-FR-" + suffix);
        vocationRepository.save(vocation);

        StkItemType type = new StkItemType();
        type.setId(UUID.randomUUID());
        type.setCode("TYPE-FR-" + suffix);
        type.setStkKind(fixtureReferentielStk.natureParDefaut());
        itemTypeRepository.save(type);

        StkItemCategory category = new StkItemCategory();
        category.setId(UUID.randomUUID());
        category.setCode("CAT-FR-" + suffix);
        category.setStkKind(fixtureReferentielStk.natureParDefaut());
        itemCategoryRepository.save(category);

        StkBuilding building = new StkBuilding();
        building.setId(UUID.randomUUID());
        building.setCode("IMM01-" + suffix);
        building.setDescription("Immeuble 01");
        buildingRepository.save(building);

        StkFloor floor = new StkFloor();
        floor.setId(UUID.randomUUID());
        floor.setCode("RDC-" + suffix);
        floor.setDescription("Rez de chaussée");
        floorRepository.save(floor);

        CfgCompany company = new CfgCompany();
        company.setId(UUID.randomUUID());
        company.setCode("CIE-FR-" + suffix);
        company.setDescription(societe);
        company.setIsActive(true);
        companyRepository.save(company);

        CfgProject project = new CfgProject();
        project.setId(UUID.randomUUID());
        project.setCode("PRJ-FR-" + suffix);
        project.setDescription("AYAD PRIVATE FERME BRETONE");
        project.setAddress("62 Impasse Abderrahim Bouabid");
        project.setCity("Casablanca");
        project.setCfgCompany(company);
        projectRepository.save(project);

        // FicheReservationService reads the project via the article's tranche
        // (StkHierarchyId -> CfgTranche.CfgProjectId) — StkItem.CfgProjectId is never populated
        // in real data (see the service's javadoc comment).
        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-FR-" + suffix);
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
        article.setStkKind(fixtureReferentielStk.natureParDefaut());
        itemRepository.save(article);

        CfgTier client = new CfgTier();
        client.setId(UUID.randomUUID());
        client.setCode("CLI-FR-" + suffix);
        client.setDescription("Nor CHEMAOU");
        client.setAddress("20 BIS CADI BEN HAMMADI");
        client.setCity("Casablanca");
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

        StlSettlement settlement = new StlSettlement();
        settlement.setId(UUID.randomUUID());
        settlement.setDescription("CHÈQUE NOTAIRE CAISSE DE DÉPÔT ET DE GESTION N° 5980120 du 27/10/2022");
        settlement.setAmount(new BigDecimal("1200000.00"));
        settlement.setTier(client);
        settlementRepository.save(settlement);

        // No "AVANCE"-category échéance configured in this fixture — FicheReservationService
        // falls back to the earliest règlement of the Dossier, linked here via a plain échéance.
        com.tsix.immocrm.entities.com.ComDocument echeance = new com.tsix.immocrm.entities.com.ComDocument();
        echeance.setId(UUID.randomUUID());
        echeance.setComFolder(folder);
        echeance.setAmount(new BigDecimal("1200000.00"));
        documentRepository.save(echeance);

        StlSettlementLine line = new StlSettlementLine();
        line.setId(UUID.randomUUID());
        line.setStlSettlement(settlement);
        line.setComDocument(echeance);
        line.setAmount(new BigDecimal("1200000.00"));
        settlementLineRepository.save(line);

        return folder.getId();
    }

    /** B9 (FEATURE-vente-socle.md) : la première impression pose ContractPrintDate, les suivantes ne la changent pas. */
    @Test
    void premiereImpression_poseContractPrintDate_uneSeuleFois() throws Exception {
        UUID dossierId = setUpDossier("B9");
        java.time.LocalDateTime ancienne = java.time.LocalDateTime.of(2020, 1, 2, 3, 4);

        mockMvc.perform(get("/api/dossiers/{id}/documents/fiche-reservation", dossierId)).andExpect(status().isOk());
        ComFolder apresPremiere = folderRepository.findById(dossierId).orElseThrow();
        assertThat(apresPremiere.getContractPrintDate()).isNotNull();
        apresPremiere.setContractPrintDate(ancienne);
        mockMvc.perform(get("/api/dossiers/{id}/documents/fiche-reservation", dossierId)).andExpect(status().isOk());

        assertThat(folderRepository.findById(dossierId).orElseThrow().getContractPrintDate()).isEqualTo(ancienne);
    }

    @Test
    void getFicheReservation_pdf_returnsPdfBytes() throws Exception {
        UUID dossierId = setUpDossier("PDF");

        byte[] pdf = mockMvc.perform(get("/api/dossiers/{id}/documents/fiche-reservation", dossierId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }

    @Test
    void getFicheReservation_docx_returnsDocxBytes() throws Exception {
        UUID dossierId = setUpDossier("DOCX");

        byte[] docx = mockMvc.perform(get("/api/dossiers/{id}/documents/fiche-reservation", dossierId)
                        .param("format", "docx"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(docx).isNotEmpty();
        // DOCX is a ZIP archive — starts with the "PK" local file header signature.
        assertThat(docx[0]).isEqualTo((byte) 'P');
        assertThat(docx[1]).isEqualTo((byte) 'K');
    }

    /**
     * Anciennement {@code usesProjectCompanyNotHardcoded}, qui lisait le nom de la société dans le
     * texte du PDF. Ce n'est plus vérifiable ainsi : la société fait partie du fond de page
     * pré-imprimé de la marque, comme dans le rapport légataire. La garantie n'a pas disparu, elle
     * est devenue un contrôle — un dossier dont l'entité ne correspond pas à la marque de son code
     * n'est pas émis du tout, plutôt qu'émis sous l'en-tête et les clauses d'une autre société.
     */
    @Test
    void getFicheReservation_societeIncoherenteAvecLaMarque_returns409() throws Exception {
        UUID dossierId = setUpDossier("CIE", "MAYLINE");

        mockMvc.perform(get("/api/dossiers/{id}/documents/fiche-reservation", dossierId))
                .andExpect(status().isConflict());
    }

    @Test
    void getFicheReservation_invalidFormat_returns400() throws Exception {
        UUID dossierId = setUpDossier("BADFMT");

        mockMvc.perform(get("/api/dossiers/{id}/documents/fiche-reservation", dossierId).param("format", "xml"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getFicheReservation_unknownDossier_returns404() throws Exception {
        mockMvc.perform(get("/api/dossiers/{id}/documents/fiche-reservation", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}

package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.entities.adm.AdmIdentityDocumentType;
import com.tsix.immocrm.entities.adm.AdmTierTitle;
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
import com.tsix.immocrm.entities.stl.StlAccount;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.repositories.adm.AdmIdentityDocumentTypeRepository;
import com.tsix.immocrm.repositories.adm.AdmTierTitleRepository;
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
import com.tsix.immocrm.repositories.stl.StlAccountRepository;
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
 * {@code GET /api/dossiers/{id}/reglements/{reglementId}/documents/autorisation-versement} —
 * modeled on the legacy Stimulsoft template
 * {@code instructions/templates/autorisation_virement/ComSaleDocumentAutorisationVersement.yes}.
 * Scoped to the règlement ({@code StlSettlement}), not the échéance (see
 * {@code AutorisationVersementService}/{@code AutorisationVersementData} javadoc for why: the
 * fields that matter — RIB, mode de versement, payeur — are confirmed on real data to live almost
 * exclusively on {@code StlSettlement}). Fixture mirrors {@code ReglementTest}: a
 * {@code ComDocument} (échéance) + a {@code StlSettlement}/{@code StlSettlementLine} attaching a
 * règlement to it.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class AutorisationVersementTest {

    @Autowired
    private FixtureReferentielStk fixtureReferentielStk;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StkVocationRepository vocationRepository;
    @Autowired
    private StkItemRepository itemRepository;
    @Autowired
    private StkItemTypeRepository itemTypeRepository;
    @Autowired
    private StkItemCategoryRepository itemCategoryRepository;
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
    private StlAccountRepository accountRepository;
    @Autowired
    private StlSettlementRepository settlementRepository;
    @Autowired
    private StlSettlementLineRepository settlementLineRepository;
    @Autowired
    private AdmTierTitleRepository tierTitleRepository;
    @Autowired
    private AdmIdentityDocumentTypeRepository identityDocumentTypeRepository;

    private record DossierEtReglement(UUID dossierId, UUID reglementId) {
    }

    private DossierEtReglement setUpDossier(String suffix) {
        AdmTierTitle titre = new AdmTierTitle();
        titre.setId(UUID.randomUUID());
        titre.setCode("Mr");
        tierTitleRepository.save(titre);

        AdmIdentityDocumentType cin = new AdmIdentityDocumentType();
        cin.setId(UUID.randomUUID());
        cin.setCode("CIN");
        identityDocumentTypeRepository.save(cin);

        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-AV-" + suffix);
        vocationRepository.save(vocation);

        CfgCompany company = new CfgCompany();
        company.setId(UUID.randomUUID());
        company.setCode("CIE-AV-" + suffix);
        company.setDescription("GHRIZA");
        company.setIsActive(true);
        companyRepository.save(company);

        CfgProject project = new CfgProject();
        project.setId(UUID.randomUUID());
        project.setCode("PRJ-AV-" + suffix);
        project.setDescription("AYAD RESIDENCE BEL AIR");
        project.setCfgCompany(company);
        projectRepository.save(project);

        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-AV-" + suffix);
        tranche.setDescription("AYAD RESIDENCE BEL AIR");
        tranche.setCfgProject(project);
        tranche.setWithdrawalDelay(48);
        trancheRepository.save(tranche);

        StkItemType type = new StkItemType();
        type.setId(UUID.randomUUID());
        type.setCode("TYPE-AV-" + suffix);
        type.setStkKind(fixtureReferentielStk.natureParDefaut());
        itemTypeRepository.save(type);

        StkItemCategory category = new StkItemCategory();
        category.setId(UUID.randomUUID());
        category.setCode("CAT-AV-" + suffix);
        category.setStkKind(fixtureReferentielStk.natureParDefaut());
        itemCategoryRepository.save(category);

        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode("ARS0632-" + suffix);
        article.setStkVocation(vocation);
        article.setStkItemType(type);
        article.setStkItemCategory(category);
        article.setStkHierarchy(tranche);
        article.setStkKind(fixtureReferentielStk.natureParDefaut());
        itemRepository.save(article);

        CfgTier client = new CfgTier();
        client.setId(UUID.randomUUID());
        client.setCode("CLI-AV-" + suffix);
        client.setAdmTierTitle(titre);
        client.setFirstName("Fouad");
        client.setLastName("EL IHFADI");
        client.setAdmIdentityDocumentType(cin);
        client.setIDNumber("E400556");
        tierRepository.save(client);

        CfgTier coAcquereur = new CfgTier();
        coAcquereur.setId(UUID.randomUUID());
        coAcquereur.setCode("CLI-AV-CO-" + suffix);
        coAcquereur.setAdmTierTitle(titre);
        coAcquereur.setFirstName("Salma");
        coAcquereur.setLastName("BENNANI");
        tierRepository.save(coAcquereur);

        CfgTier commercial = new CfgTier();
        commercial.setId(UUID.randomUUID());
        commercial.setCode("COM-AV-" + suffix);
        commercial.setAdmTierTitle(titre);
        commercial.setFirstName("Karim");
        commercial.setLastName("ALAOUI");
        tierRepository.save(commercial);

        CfgTier payeur = new CfgTier();
        payeur.setId(UUID.randomUUID());
        payeur.setCode("PAYEUR-AV-" + suffix);
        payeur.setAdmTierTitle(titre);
        payeur.setFirstName("Fouad");
        payeur.setLastName("EL IHFADI");
        payeur.setAdmIdentityDocumentType(cin);
        payeur.setIDNumber("E400556");
        tierRepository.save(payeur);

        CfgTier banque = new CfgTier();
        banque.setId(UUID.randomUUID());
        banque.setCode("BANK-AV-" + suffix);
        banque.setDescription("ATTIJARIWAFA BANK");
        tierRepository.save(banque);

        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode("ARS 2026/00" + suffix);
        folder.setStkVocation(vocation);
        folder.setStkItem(article);
        folder.setCfgCustomer(client);
        folder.setCfgCommercial(commercial);
        folder.setDocDate(LocalDateTime.now());
        folder.setPrice(new BigDecimal("800000.00"));
        folderRepository.save(folder);

        ComFolderCustomer coAcquereurLink = new ComFolderCustomer();
        coAcquereurLink.setId(UUID.randomUUID());
        coAcquereurLink.setComFolder(folder);
        coAcquereurLink.setCfgTier(coAcquereur);
        coAcquereurLink.setPartPercent(new BigDecimal("0.50"));
        folderCustomerRepository.save(coAcquereurLink);

        StlAccount compte = new StlAccount();
        compte.setId(UUID.randomUUID());
        compte.setCode("CPT-AV-" + suffix);
        compte.setCfgBank(banque);
        compte.setBankCode("230");
        compte.setAgencyCode("780");
        compte.setAccountNumber("3242161221009500");
        compte.setAccountKey("33");
        accountRepository.save(compte);

        ComDocument echeance = new ComDocument();
        echeance.setId(UUID.randomUUID());
        echeance.setCode("ECH-AV-" + suffix);
        echeance.setComFolder(folder);
        echeance.setAmount(new BigDecimal("80000.00"));
        documentRepository.save(echeance);

        StlSettlement settlement = new StlSettlement();
        settlement.setId(UUID.randomUUID());
        settlement.setCode("REF-AV-" + suffix);
        settlement.setDocDate(LocalDateTime.of(2026, 8, 28, 14, 5, 53));
        settlement.setAmount(new BigDecimal("80000.00"));
        settlement.setStlAccount(compte);
        settlement.setTier(payeur);
        settlementRepository.save(settlement);

        StlSettlementLine ligne = new StlSettlementLine();
        ligne.setId(UUID.randomUUID());
        ligne.setStlSettlement(settlement);
        ligne.setComDocument(echeance);
        ligne.setAmount(new BigDecimal("80000.00"));
        settlementLineRepository.save(ligne);

        return new DossierEtReglement(folder.getId(), settlement.getId());
    }

    @Test
    void getAutorisationVersement_showsDesignationMontantEtRib() throws Exception {
        DossierEtReglement ctx = setUpDossier("1");

        byte[] pdf = mockMvc.perform(get("/api/dossiers/{id}/reglements/{reglementId}/documents/autorisation-versement",
                        ctx.dossierId(), ctx.reglementId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(pdf).isNotEmpty();
        String text;
        try (PdfReader reader = new PdfReader(pdf)) {
            // Must fit on a single page, same as the legacy sample (Report.pdf).
            assertThat(reader.getNumberOfPages()).isEqualTo(1);
            text = new PdfTextExtractor(reader).getTextFromPage(1).replace('\n', ' ');
        }
        assertThat(text)
                .contains("AUTORISATION D'ENCAISSEMENT")
                .contains("AYAD RESIDENCE BEL AIR")
                .contains("ARS0632-1")
                .contains("800 000.00 DH")
                .contains("80 000.00 DH")
                .contains("quatre-vingt mille dirhams")
                .contains("28/08/2026 14:05:53")
                .contains("230 780 3242161221009500 33")
                .contains("ATTIJARIWAFA BANK")
                .contains("EL IHFADI")
                .contains("BENNANI")
                .contains("E400556")
                .contains("ALAOUI")
                .contains("48")
                .contains("GHRIZA")
                .contains("Cadre réservé à la banque");
    }

    @Test
    void getAutorisationVersement_unknownDossier_returns404() throws Exception {
        mockMvc.perform(get("/api/dossiers/{id}/reglements/{reglementId}/documents/autorisation-versement",
                        UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAutorisationVersement_unknownReglement_returns404() throws Exception {
        DossierEtReglement ctx = setUpDossier("2");

        mockMvc.perform(get("/api/dossiers/{id}/reglements/{reglementId}/documents/autorisation-versement",
                        ctx.dossierId(), UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAutorisationVersement_reglementNotAttachedToDossier_returns404() throws Exception {
        DossierEtReglement ctxA = setUpDossier("3");
        DossierEtReglement ctxB = setUpDossier("4");

        mockMvc.perform(get("/api/dossiers/{id}/reglements/{reglementId}/documents/autorisation-versement",
                        ctxA.dossierId(), ctxB.reglementId()))
                .andExpect(status().isNotFound());
    }
}

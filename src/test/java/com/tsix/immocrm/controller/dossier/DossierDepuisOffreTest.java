package com.tsix.immocrm.controller.dossier;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComFolderItem;
import com.tsix.immocrm.entities.com.ComPaymentMethod;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.stl.StlQuote;
import com.tsix.immocrm.entities.stl.StlQuoteDocument;
import com.tsix.immocrm.entities.stl.StlQuoteItem;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.fixtures.FixtureStatutsDossier;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderItemRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComPaymentMethodRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.stl.StlQuoteDocumentRepository;
import com.tsix.immocrm.repositories.stl.StlQuoteItemRepository;
import com.tsix.immocrm.repositories.stl.StlQuoteRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Dossier créé depuis une offre (FEATURE-offre-vers-dossier.md B1, B3) : contrôles de {@code GenerateComFolderFromQuote},
 * échéancier et sous-articles repris de l'offre. Lot 819 000 TTC avec un jardin enfant à 50 000 au catalogue,
 * 30 000 dans l'offre ; tranche à 30 % d'avance.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class DossierDepuisOffreTest {

    private static final String DOSSIERS = "/api/dossiers";

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private FixtureReferentielStk fixtureReferentielStk;
    @Autowired private FixtureStatutsDossier fixtureStatuts;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private ComFolderItemRepository folderItemRepository;
    @Autowired private ComDocumentRepository documentRepository;
    @Autowired private ComProspectionRepository prospectionRepository;
    @Autowired private ComPaymentMethodRepository paymentMethodRepository;
    @Autowired private ComSettlementCategoryRepository categoryRepository;
    @Autowired private StkVocationRepository vocationRepository;
    @Autowired private StkItemTypeRepository itemTypeRepository;
    @Autowired private StkItemCategoryRepository itemCategoryRepository;
    @Autowired private StkItemRepository itemRepository;
    @Autowired private CfgCompanyRepository companyRepository;
    @Autowired private CfgProjectRepository projectRepository;
    @Autowired private CfgTrancheRepository trancheRepository;
    @Autowired private CfgTierRepository tierRepository;
    @Autowired private AdmUserRepository userRepository;
    @Autowired private StlQuoteRepository quoteRepository;
    @Autowired private StlQuoteItemRepository quoteItemRepository;
    @Autowired private StlQuoteDocumentRepository quoteDocumentRepository;

    private CfgCompany societe;
    private CfgTranche tranche;
    private StkVocation vocation;
    private ComSettlementCategory avance;
    private ComSettlementCategory autoFinancement;
    private StkItem lot;
    private StkItem jardin;
    private ComPaymentMethod modePaie;
    private AdmUser agent;
    private ComProspection prospection;

    @BeforeEach
    void setUp() {
        fixtureStatuts.reservation();
        societe = new CfgCompany();
        societe.setId(UUID.randomUUID());
        societe.setCode("SOC-OFF-DOS");
        societe.setIsActive(true);
        companyRepository.save(societe);
        CfgProject projet = new CfgProject();
        projet.setId(UUID.randomUUID());
        projet.setCode("POD");
        projet.setCfgCompany(societe);
        projet = projectRepository.save(projet);
        tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-OFF-DOS");
        tranche.setCfgProject(projet);
        tranche.setAdvancePercent(new BigDecimal("0.3"));
        tranche = trancheRepository.save(tranche);

        vocation = nouvelleVocation("VOC-OFF-DOS");
        avance = categorie("AVANCE", true);
        autoFinancement = categorie("AUTO-FINANCEMENT", false);

        lot = article("LOT-OFF-DOS", "819000", null);
        jardin = article("JRD-OFF-DOS", "50000", lot);

        modePaie = new ComPaymentMethod();
        modePaie.setId(UUID.randomUUID());
        modePaie.setCode("VIR-OFF-DOS");
        paymentMethodRepository.save(modePaie);

        agent = new AdmUser();
        agent.setId(UUID.randomUUID());
        agent.setCode("agent.offdos." + UUID.randomUUID());
        agent.setDescription("Agent Offre Dossier");
        agent.setDisabled(false);
        agent = userRepository.save(agent);

        CfgTier prospect = new CfgTier();
        prospect.setId(UUID.randomUUID());
        prospect.setCode("CLI-OFF-DOS");
        prospect = tierRepository.save(prospect);
        prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-OFF-DOS");
        prospection.setCfgProspect(prospect);
        prospection = prospectionRepository.save(prospection);
    }

    private StkVocation nouvelleVocation(String code) {
        StkVocation v = new StkVocation();
        v.setId(UUID.randomUUID());
        v.setCode(code);
        return vocationRepository.save(v);
    }

    private ComSettlementCategory categorie(String code, boolean estAvance) {
        ComSettlementCategory c = new ComSettlementCategory();
        c.setId(UUID.randomUUID());
        c.setCode(code);
        c.setDescription(code);
        c.setStkVocation(vocation);
        c.setIsCredit(true);
        c.setIsSaleAdvance(estAvance);
        c.setIsSaleAutoFinancment(!estAvance);
        c.setExcludeFromPrice(false);
        c.setVatRatio(new BigDecimal("0.2"));
        return categoryRepository.save(c);
    }

    private StkItem article(String code, String prix, StkItem parent) {
        StkItemType type = new StkItemType();
        type.setId(UUID.randomUUID());
        type.setCode("TYPE-" + code);
        type.setStkKind(fixtureReferentielStk.natureParDefaut());
        itemTypeRepository.save(type);
        StkItemCategory categorieArticle = new StkItemCategory();
        categorieArticle.setId(UUID.randomUUID());
        categorieArticle.setCode("CAT-" + code);
        categorieArticle.setStkKind(fixtureReferentielStk.natureParDefaut());
        itemCategoryRepository.save(categorieArticle);
        StkItem item = new StkItem();
        item.setId(UUID.randomUUID());
        item.setCode(code);
        item.setStkItemType(type);
        item.setStkItemCategory(categorieArticle);
        item.setStkVocation(vocation);
        item.setStkKind(fixtureReferentielStk.natureParDefaut());
        item.setStkHierarchy(tranche);
        item.setParent(parent);
        item.setArea(new BigDecimal("50"));
        item.setVat(new BigDecimal("0.2"));
        item.setComSalePrice(new BigDecimal(prix));
        return itemRepository.save(item);
    }

    /** Offre sur le lot, jardin à 30 000 TTC (25 000 HT), commission 3 %. */
    private StlQuote offre(StkItem article, StkVocation vocationOffre) {
        StlQuote q = new StlQuote();
        q.setId(UUID.randomUUID());
        q.setCode("OFF-DOS-" + UUID.randomUUID().toString().substring(0, 8));
        q.setDocDate(LocalDateTime.of(2026, 10, 1, 0, 0));
        q.setComProspection(prospection);
        q.setCfgProspect(prospection.getCfgProspect());
        q.setCfgTranche(tranche);
        q.setStkItem(article);
        q.setStkVocation(vocationOffre);
        q.setCommissionRate(new BigDecimal("0.03"));
        q = quoteRepository.save(q);
        if (article != null) {
            StlQuoteItem ligne = new StlQuoteItem();
            ligne.setId(UUID.randomUUID());
            ligne.setStlQuote(q);
            ligne.setStkItem(jardin);
            ligne.setCode("JRD-OFF-DOS");
            ligne.setVatRatio(new BigDecimal("0.2"));
            ligne.setPriceBeforeDiscount(new BigDecimal("30000"));
            ligne.setPriceBeforeDiscountHT(new BigDecimal("25000"));
            ligne.setDiscount(BigDecimal.ZERO);
            ligne.setPriceAfterDiscount(new BigDecimal("30000"));
            ligne.setPriceAfterDiscountHT(new BigDecimal("25000"));
            quoteItemRepository.save(ligne);
        }
        return q;
    }

    private void ligneSchema(StlQuote q, ComSettlementCategory categorieLigne, String montant, LocalDateTime date) {
        StlQuoteDocument ligne = new StlQuoteDocument();
        ligne.setId(UUID.randomUUID());
        ligne.setStlQuote(q);
        ligne.setCode("SCH-" + categorieLigne.getCode());
        ligne.setComSettlementCategory(categorieLigne);
        ligne.setAmount(new BigDecimal(montant));
        ligne.setDocDate(date);
        ligne.setCfgCompany(societe);
        ligne.setDiscountRatio(BigDecimal.ZERO);
        quoteDocumentRepository.save(ligne);
    }

    private ComFolder dossierExistant(String code) {
        ComFolder dossier = new ComFolder();
        dossier.setId(UUID.randomUUID());
        dossier.setCode(code);
        dossier.setStkVocation(vocation);
        return folderRepository.save(dossier);
    }

    private String corps(StlQuote q) {
        return "{\"prospectionId\":\"%s\",\"articleIds\":[\"%s\"],\"modePaieId\":\"%s\",\"cin\":\"CIN-OFF\",\"prix\":849000,\"offreId\":\"%s\"}"
                .formatted(prospection.getId(), lot.getId(), modePaie.getId(), q.getId());
    }

    private ComFolder creer(StlQuote q) throws Exception {
        String json = mockMvc.perform(post(DOSSIERS).with(connecte(agent)).contentType(MediaType.APPLICATION_JSON)
                        .content(corps(q)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        entityManager.flush();
        entityManager.clear();
        return folderRepository.findById(UUID.fromString(JsonPath.read(json, "$.id"))).orElseThrow();
    }

    /** Refus sans aucune écriture : ni dossier, ni échéance, ni article occupé par la tentative. */
    private void refuse(StlQuote q, ResultMatcher statut, String message) throws Exception {
        entityManager.flush();
        long dossiers = folderRepository.count();
        long echeances = documentRepository.count();
        UUID occupantAvant = itemRepository.findById(lot.getId()).orElseThrow().getComFolderId();

        mockMvc.perform(post(DOSSIERS).with(connecte(agent)).contentType(MediaType.APPLICATION_JSON).content(corps(q)))
                .andExpect(statut)
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString(message)));

        entityManager.flush();
        entityManager.clear();
        assertAll(
                () -> assertThat(folderRepository.count()).as("dossiers").isEqualTo(dossiers),
                () -> assertThat(documentRepository.count()).as("échéances").isEqualTo(echeances),
                () -> assertThat(itemRepository.findById(lot.getId()).orElseThrow().getComFolderId())
                        .as("occupation du lot").isEqualTo(occupantAvant));
    }

    @Test
    void depuisOffreAvecSchema_reprendEcheancierSousArticlesEtCommission() throws Exception {
        StlQuote q = offre(lot, vocation);
        ligneSchema(q, avance, "300000", LocalDateTime.of(2026, 11, 2, 0, 0));
        ligneSchema(q, autoFinancement, "549000", LocalDateTime.of(2027, 1, 15, 0, 0));

        ComFolder f = creer(q);

        List<ComDocument> echeances = documentRepository.findByComFolderIdOrderByDocDateAsc(f.getId());
        List<ComFolderItem> sousArticles = folderItemRepository.findByComFolderId(f.getId());
        assertAll(
                () -> assertThat(f.getStlQuoteId()).isEqualTo(q.getId()),
                () -> assertThat(f.getCommissionRate()).isEqualByComparingTo("0.03"),
                () -> assertThat(f.getUnderItemsPrice()).isEqualByComparingTo("30000"),
                () -> assertThat(f.getPriceAfterDiscount()).isEqualByComparingTo("849000"),
                () -> assertThat(echeances).extracting(d -> d.getComSettlementCategory().getCode(),
                                d -> d.getAmount().stripTrailingZeros().toPlainString(), ComDocument::getDocDate)
                        .containsExactly(
                                org.assertj.core.groups.Tuple.tuple("AVANCE", "300000", LocalDateTime.of(2026, 11, 2, 0, 0)),
                                org.assertj.core.groups.Tuple.tuple("AUTO-FINANCEMENT", "549000",
                                        LocalDateTime.of(2027, 1, 15, 0, 0))),
                () -> assertThat(echeances).isNotEmpty()
                        .allSatisfy(d -> assertThat(d.getCfgCompany().getId()).isEqualTo(societe.getId())),
                () -> assertThat(f.getAgreementPrice()).isEqualByComparingTo("849000"),
                () -> assertThat(sousArticles).singleElement().satisfies(ligne -> {
                    assertThat(ligne.getStkItem().getId()).isEqualTo(jardin.getId());
                    assertThat(ligne.getPriceBeforeDiscount()).isEqualByComparingTo("30000");
                    assertThat(ligne.getPriceBeforeDiscountHT()).isEqualByComparingTo("25000");
                    assertThat(ligne.getPrice()).isEqualByComparingTo("30000");
                    assertThat(ligne.getPriceHT()).isEqualByComparingTo("25000");
                }));
    }

    @Test
    void depuisOffreSansSchema_echeancierParDefaut() throws Exception {
        StlQuote q = offre(lot, vocation);

        ComFolder f = creer(q);

        assertThat(documentRepository.findByComFolderIdOrderByDocDateAsc(f.getId()))
                .extracting(d -> d.getComSettlementCategory().getCode(), d -> d.getAmount().stripTrailingZeros().toPlainString())
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("AVANCE", "254700"),
                        org.assertj.core.groups.Tuple.tuple("AUTO-FINANCEMENT", "594300"));
    }

    @Test
    void offreSansArticle_400SansEcriture() throws Exception {
        refuse(offre(null, vocation), status().isBadRequest(), "Vous n'avez pas sélectionné un article !");
    }

    @Test
    void articleDeLOffreOccupe_400SansEcriture() throws Exception {
        StlQuote q = offre(lot, vocation);
        lot.setComFolderId(dossierExistant("DOS-OFF-OCC").getId());
        itemRepository.save(lot);

        refuse(q, status().isBadRequest(), "Cet article n'est pas disponible en stock !");
    }

    @Test
    void sousArticleDeLOffreOccupe_400SansEcriture() throws Exception {
        StlQuote q = offre(lot, vocation);
        jardin.setComFolderId(dossierExistant("DOS-OFF-JRD").getId());
        itemRepository.save(jardin);

        refuse(q, status().isBadRequest(), "Le sous article JRD-OFF-DOS n'est pas libre !");
    }

    @Test
    void vocationDifferente_400SansEcriture() throws Exception {
        refuse(offre(lot, nouvelleVocation("VOC-OFF-AUTRE")), status().isBadRequest(),
                "Cet article n'a pas la vocation correspondante !");
    }

    @Test
    void prospectionQuiADejaUnDossier_409SansEcriture() throws Exception {
        StlQuote q = offre(lot, vocation);
        ComFolder existant = dossierExistant("DOS-OFF-PSN");
        existant.setComProspection(prospection);
        folderRepository.save(existant);

        refuse(q, status().isConflict(), "Un dossier de vente est déjà généré sur cette prospection !");
    }

    /**
     * Revue : GenerateComFolderFromQuote pose toujours l'article de l'offre. Un autre article dans articleIds
     * donnait un dossier sur cet article, avec l'échéancier et la commission de l'offre (calés sur un autre prix),
     * et les contrôles 2 à 4 portaient sur l'article de l'offre, pas sur celui vendu.
     */
    @Test
    void articleDuFormulaireAutreQueCeluiDeLOffre_400SansEcriture() throws Exception {
        StlQuote q = offre(lot, vocation);
        ligneSchema(q, avance, "300000", LocalDateTime.of(2026, 11, 2, 0, 0));
        StkItem autre = article("LOT-AUTRE-OFF", "500000", null);
        entityManager.flush();
        long dossiers = folderRepository.count();
        long echeances = documentRepository.count();
        String body = "{\"prospectionId\":\"%s\",\"articleIds\":[\"%s\"],\"modePaieId\":\"%s\",\"cin\":\"CIN-OFF\",\"prix\":500000,\"offreId\":\"%s\"}"
                .formatted(prospection.getId(), autre.getId(), modePaie.getId(), q.getId());

        mockMvc.perform(post(DOSSIERS).with(connecte(agent)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        entityManager.flush();
        entityManager.clear();
        assertAll(
                () -> assertThat(folderRepository.count()).as("dossiers").isEqualTo(dossiers),
                () -> assertThat(documentRepository.count()).as("échéances").isEqualTo(echeances),
                () -> assertThat(itemRepository.findById(autre.getId()).orElseThrow().getComFolderId())
                        .as("occupation de l'autre article").isNull());
    }

    /** Revue : GenerateComFolderFromQuote pose {@code CfgCommercialId} = celui de l'offre (spec, « Reprise »). */
    @Test
    void commercialDeLOffre_reprisSurLeDossier() throws Exception {
        CfgTier commercialProspection = new CfgTier();
        commercialProspection.setId(UUID.randomUUID());
        commercialProspection.setCode("COM-PSN-OFF-DOS");
        commercialProspection = tierRepository.save(commercialProspection);
        CfgTier commercialOffre = new CfgTier();
        commercialOffre.setId(UUID.randomUUID());
        commercialOffre.setCode("COM-OFR-OFF-DOS");
        commercialOffre = tierRepository.save(commercialOffre);
        prospection.setCfgCommercial(commercialProspection);
        prospection = prospectionRepository.save(prospection);
        StlQuote q = offre(lot, vocation);
        q.setCfgCommercial(commercialOffre);
        quoteRepository.save(q);

        ComFolder f = creer(q);

        assertThat(f.getCfgCommercial()).isNotNull().extracting(CfgTier::getId).isEqualTo(commercialOffre.getId());
    }

    @Test
    void offreDejaTransformee_409AvecLeDossierSansEcriture() throws Exception {
        StlQuote q = offre(lot, vocation);
        ComFolder existant = dossierExistant("DOS-OFF-DEJA");
        existant.setStlQuoteId(q.getId());
        folderRepository.save(existant);
        entityManager.flush();

        mockMvc.perform(post(DOSSIERS).with(connecte(agent)).contentType(MediaType.APPLICATION_JSON).content(corps(q)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.dossierExistant.id").value(existant.getId().toString()))
                .andExpect(jsonPath("$.dossierExistant.code").value("DOS-OFF-DEJA"));
        refuse(q, status().isConflict(), "DOS-OFF-DEJA");
    }
}

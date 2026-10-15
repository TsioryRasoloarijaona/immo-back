package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.fixtures.ConnexionDeTest;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.services.dossier.ReferencesOmran;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.entities.stl.StlQuote;
import com.tsix.immocrm.repositories.stl.StlQuoteItemRepository;
import com.tsix.immocrm.repositories.stl.StlQuoteRepository;
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
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GAP-07 (low priority) — devis (offres) sur une prospection, avec détail HT/TTC. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class DevisTest {

    @Autowired
    private FixtureReferentielStk fixtureReferentielStk;
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComProspectionRepository prospectionRepository;

    @Autowired
    private CfgProjectRepository projectRepository;

    @Autowired
    private CfgTrancheRepository trancheRepository;

    @Autowired
    private StkItemRepository stkItemRepository;

    @Autowired
    private StkItemTypeRepository stkItemTypeRepository;

    @Autowired
    private StkItemCategoryRepository stkItemCategoryRepository;

    @Autowired
    private StlQuoteItemRepository quoteItemRepository;

    @Autowired
    private StkVocationRepository stkVocationRepository;

    @Autowired
    private CfgTierRepository tierRepository;

    @Autowired
    private StlQuoteRepository quoteRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Autowired
    private AdmUserRepository admUserRepository;

    @Autowired
    private ComFolderRepository folderRepository;

    private CfgTranche creerTranche(String code) {
        CfgProject project = new CfgProject();
        project.setId(UUID.randomUUID());
        project.setCode("PRJ-" + code);
        projectRepository.save(project);

        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode(code);
        tranche.setCfgProject(project);
        return trancheRepository.save(tranche);
    }

    private ComProspection nouvelleProspection(String code) {
        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode(code);
        return prospectionRepository.save(prospection);
    }

    private StkItem nouvelArticle(String code, CfgTranche tranche, StkItem parent, StkVocation vocation) {
        StkItemType type = new StkItemType();
        type.setId(UUID.randomUUID());
        type.setCode("TYPE-" + code);
        type.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemTypeRepository.save(type);
        StkItemCategory categorie = new StkItemCategory();
        categorie.setId(UUID.randomUUID());
        categorie.setCode("CAT-" + code);
        categorie.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemCategoryRepository.save(categorie);
        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode(code);
        article.setDescription("Désignation " + code);
        article.setStkItemType(type);
        article.setStkItemCategory(categorie);
        article.setStkVocation(vocation);
        article.setStkHierarchy(tranche);
        article.setParent(parent);
        article.setComSalePrice(new BigDecimal("500000"));
        article.setVat(new BigDecimal("0.2"));
        article.setStkKind(fixtureReferentielStk.natureParDefaut());
        return stkItemRepository.save(article);
    }

    private StkVocation nouvelleVocation(String code) {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode(code);
        return stkVocationRepository.save(vocation);
    }

    private void creerEnEchec(ComProspection prospection, String body, String messageAttendu) throws Exception {
        long avant = quoteRepository.count();
        mockMvc.perform(post("/api/prospections/{id}/devis", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(messageAttendu)));
        entityManager.flush();
        org.assertj.core.api.Assertions.assertThat(quoteRepository.count()).as("aucune offre écrite").isEqualTo(avant);
    }

    @Test
    void creerDevis_renseigneEntiteVocationEtAuteurCommeOmran() throws Exception {
        ComProspection prospection = nouvelleProspection("PSN-DEVIS-OMRAN-COL");
        CfgTranche tranche = creerTranche("TR-DEVIS-OMRAN-COL");
        StkVocation vocation = nouvelleVocation("VOC-DEVIS-OMRAN-COL");
        StkItem article = nouvelArticle("LOT-DEVIS-OMRAN-COL", tranche, null, vocation);
        AdmUser agent = new AdmUser();
        agent.setId(UUID.randomUUID());
        agent.setCode("agent.devis." + UUID.randomUUID());
        agent.setDescription("Agent Offre");
        agent.setDisabled(false);
        agent = admUserRepository.save(agent);

        String reponse = mockMvc.perform(post("/api/prospections/{id}/devis", prospection.getId())
                        .with(ConnexionDeTest.connecte(agent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trancheId\":\"" + tranche.getId() + "\",\"articleId\":\"" + article.getId() + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        entityManager.flush();
        entityManager.clear();
        StlQuote relue = quoteRepository.findById(
                UUID.fromString(com.jayway.jsonpath.JsonPath.read(reponse, "$.id").toString())).orElseThrow();
        org.junit.jupiter.api.Assertions.assertAll(
                () -> org.assertj.core.api.Assertions.assertThat(relue.getStrEntityId())
                        .isEqualTo(ReferencesOmran.ENTITE_OFFRE_VENTE),
                () -> org.assertj.core.api.Assertions.assertThat(relue.getStkVocation().getId()).isEqualTo(vocation.getId()),
                () -> org.assertj.core.api.Assertions.assertThat(relue.getUserCreate()).isEqualTo("Agent Offre"),
                () -> org.assertj.core.api.Assertions.assertThat(relue.getUserCreateDateTime()).isNotNull(),
                () -> org.assertj.core.api.Assertions.assertThat(relue.getUserUpdate()).isEqualTo("Agent Offre"));
    }

    @Test
    void creerDevis_articleDUneAutreTranche_returns400SansEcriture() throws Exception {
        ComProspection prospection = nouvelleProspection("PSN-DEVIS-A3-TR");
        CfgTranche choisie = creerTranche("TR-DEVIS-A3-TR-A");
        CfgTranche autre = creerTranche("TR-DEVIS-A3-TR-B");
        StkItem article = nouvelArticle("LOT-DEVIS-A3-TR", autre, null, nouvelleVocation("VOC-DEVIS-A3-TR"));

        creerEnEchec(prospection, "{\"trancheId\":\"" + choisie.getId() + "\",\"articleId\":\"" + article.getId() + "\"}",
                "n'appartient pas à la tranche");
    }

    @Test
    void creerDevis_sousArticleQuiNEstPasUnEnfant_returns400SansEcriture() throws Exception {
        ComProspection prospection = nouvelleProspection("PSN-DEVIS-A3-SA");
        CfgTranche tranche = creerTranche("TR-DEVIS-A3-SA");
        StkVocation vocation = nouvelleVocation("VOC-DEVIS-A3-SA");
        StkItem article = nouvelArticle("LOT-DEVIS-A3-SA", tranche, null, vocation);
        StkItem etranger = nouvelArticle("LOT-DEVIS-A3-SA-AUTRE", tranche, null, vocation);

        creerEnEchec(prospection, "{\"trancheId\":\"" + tranche.getId() + "\",\"articleId\":\"" + article.getId()
                        + "\",\"sousArticleIds\":[\"" + etranger.getId() + "\"]}",
                "n'est pas un sous-article");
    }

    @Test
    void creerDevis_articleDejaVendu_returns400SansEcriture() throws Exception {
        ComProspection prospection = nouvelleProspection("PSN-DEVIS-A3-VND");
        CfgTranche tranche = creerTranche("TR-DEVIS-A3-VND");
        StkItem article = nouvelArticle("LOT-DEVIS-A3-VND", tranche, null, nouvelleVocation("VOC-DEVIS-A3-VND"));
        ComFolder dossier = new ComFolder();
        dossier.setId(UUID.randomUUID());
        dossier.setCode("DOS-DEVIS-A3-VND");
        dossier.setStkVocation(article.getStkVocation());
        folderRepository.save(dossier);
        article.setComFolderId(dossier.getId());
        stkItemRepository.save(article);

        creerEnEchec(prospection, "{\"trancheId\":\"" + tranche.getId() + "\",\"articleId\":\"" + article.getId() + "\"}",
                "déjà vendu");
    }

    @Test
    void findAllDevis_rechercheParNomDuProspectEtCodeArticle_trouveLOffre() throws Exception {
        CfgTier prospect = new CfgTier();
        prospect.setId(UUID.randomUUID());
        prospect.setCode("PRO-DEVIS-A2");
        prospect.setFirstName("Yasmine");
        prospect.setLastName("Zouhairi-A2");
        prospect = tierRepository.save(prospect);
        ComProspection prospection = nouvelleProspection("PSN-DEVIS-A2");
        prospection.setCfgProspect(prospect);
        prospectionRepository.save(prospection);
        CfgTranche tranche = creerTranche("TR-DEVIS-A2");
        StkItem article = nouvelArticle("LOT-RECH-A2", tranche, null, nouvelleVocation("VOC-DEVIS-A2"));
        mockMvc.perform(post("/api/prospections/{id}/devis", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trancheId\":\"" + tranche.getId() + "\",\"articleId\":\"" + article.getId() + "\"}"))
                .andExpect(status().isOk());
        String filtre = "$.content[?(@.prospectionId=='" + prospection.getId() + "')]";

        mockMvc.perform(get("/api/devis").param("search", "yasmine zouhairi-a2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtre).isNotEmpty());
        mockMvc.perform(get("/api/devis").param("search", "lot-rech-a2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtre).isNotEmpty());
        mockMvc.perform(get("/api/devis").param("search", "PSN-DEVIS-A2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtre).isNotEmpty());
    }

    @Test
    void creerDevis_offreLibre_sansArticle_prixDevientPrixDeVenteTtc() throws Exception {
        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-DEVIS-1");
        prospectionRepository.save(prospection);

        CfgTranche tranche = creerTranche("TR-DEVIS-1");

        String body = "{\"trancheId\":\"" + tranche.getId() + "\",\"prix\":300000,\"notes\":\"Offre initiale\"}";

        mockMvc.perform(post("/api/prospections/{id}/devis", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trancheId").value(tranche.getId().toString()))
                .andExpect(jsonPath("$.montants.prixVenteTTC").value(300000))
                .andExpect(jsonPath("$.montants.totalTTC").value(300000))
                .andExpect(jsonPath("$.notes").value("Offre initiale"));

        mockMvc.perform(get("/api/prospections/{id}/devis", prospection.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.trancheId=='" + tranche.getId() + "')]").exists());
    }

    /**
     * {@code StlQuote.CommissionRate} — confirmed against the real legacy "Offre de prix"
     * Stimulsoft template (field "Commission" on the MONTANTS table). Not settable via the
     * create request yet (no legacy UI equivalent found), but the PDF must surface it when the
     * value is already present on the row.
     */
    @Test
    void getDevisDocument_avecCommission_afficheLaLigneCommissionDansLePdf() throws Exception {
        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-DEVIS-13");
        prospectionRepository.save(prospection);

        CfgTranche tranche = creerTranche("TR-DEVIS-13");

        String body = "{\"trancheId\":\"" + tranche.getId() + "\",\"prix\":300000,\"notes\":\"Offre initiale\"}";
        String responseJson = mockMvc.perform(post("/api/prospections/{id}/devis", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        UUID devisId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(responseJson, "$.id").toString());

        StlQuote quote = quoteRepository.findById(devisId).orElseThrow();
        quote.setCommissionRate(new BigDecimal("0.03"));
        quoteRepository.save(quote);

        byte[] pdf = mockMvc.perform(get("/api/prospections/{id}/devis/{devisId}/document", prospection.getId(), devisId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        String text;
        try (PdfReader reader = new PdfReader(pdf)) {
            text = new PdfTextExtractor(reader).getTextFromPage(1);
        }
        org.assertj.core.api.Assertions.assertThat(text).contains("Commission").contains("3.00");
    }

    @Test
    void creerDevis_avecArticle_calculeLeDetailHtTtcDepuisLePrixCommercial() throws Exception {
        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-DEVIS-2");
        prospectionRepository.save(prospection);

        CfgTranche tranche = creerTranche("TR-DEVIS-2");

        StkItemType itemType = new StkItemType();
        itemType.setId(UUID.randomUUID());
        itemType.setCode("TYPE-DEVIS-2");
        itemType.setDescription("1 CHAMBRES + SALON");
        itemType.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemTypeRepository.save(itemType);

        StkItemCategory itemCategory = new StkItemCategory();
        itemCategory.setId(UUID.randomUUID());
        itemCategory.setCode("CAT-DEVIS-2");
        itemCategory.setDescription("APPARTEMENT");
        itemCategory.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemCategoryRepository.save(itemCategory);

        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-DEVIS-2");
        stkVocationRepository.save(vocation);

        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode("LOT-DEVIS-2");
        article.setDescription("17");
        article.setStkItemType(itemType);
        article.setStkItemCategory(itemCategory);
        article.setStkVocation(vocation);
        article.setStkHierarchy(tranche);
        article.setArea(new BigDecimal("45.000000"));
        article.setComSalePriceHT(new BigDecimal("1290000.000000"));
        article.setComSalePrice(new BigDecimal("1548000.000000"));
        article.setVat(new BigDecimal("0.200000"));
        article.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemRepository.save(article);

        String body = "{\"trancheId\":\"" + tranche.getId() + "\",\"articleId\":\"" + article.getId() + "\"}";

        // Prix Article HT/TTC = 1 290 000 / 1 548 000 (repris tels quels de l'article) ;
        // Prix du m² HT = 1 290 000 / 45 = 28 666.666667 ; sans remise, total == prix de vente.
        mockMvc.perform(post("/api/prospections/{id}/devis", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.article").value("LOT-DEVIS-2"))
                .andExpect(jsonPath("$.articleCategorie").value("APPARTEMENT"))
                .andExpect(jsonPath("$.articleType").value("1 CHAMBRES + SALON"))
                .andExpect(jsonPath("$.montants.tvaRatio").value(0.2))
                .andExpect(jsonPath("$.montants.prixArticleHT").value(1290000))
                .andExpect(jsonPath("$.montants.prixArticleTTC").value(1548000))
                .andExpect(jsonPath("$.montants.prixM2HT").value(28666.666667))
                .andExpect(jsonPath("$.montants.prixSousArticlesTTC").value(0))
                .andExpect(jsonPath("$.montants.totalTTC").value(1548000))
                .andExpect(jsonPath("$.montants.prixVenteHT").value(1290000))
                .andExpect(jsonPath("$.montants.prixVenteTTC").value(1548000));
    }

    @Test
    void creerDevis_avecRemise_repercuteeSurLesMontantsApresRemise() throws Exception {
        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-DEVIS-3");
        prospectionRepository.save(prospection);

        CfgTranche tranche = creerTranche("TR-DEVIS-3");

        StkItemType itemType3 = new StkItemType();
        itemType3.setId(UUID.randomUUID());
        itemType3.setCode("TYPE-DEVIS-3");
        itemType3.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemTypeRepository.save(itemType3);

        StkItemCategory itemCategory3 = new StkItemCategory();
        itemCategory3.setId(UUID.randomUUID());
        itemCategory3.setCode("CAT-DEVIS-3");
        itemCategory3.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemCategoryRepository.save(itemCategory3);

        StkVocation vocation3 = new StkVocation();
        vocation3.setId(UUID.randomUUID());
        vocation3.setCode("VOC-DEVIS-3");
        stkVocationRepository.save(vocation3);

        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode("LOT-DEVIS-3");
        article.setStkItemType(itemType3);
        article.setStkItemCategory(itemCategory3);
        article.setStkVocation(vocation3);
        article.setStkHierarchy(tranche);
        article.setArea(new BigDecimal("100.000000"));
        article.setComSalePriceHT(new BigDecimal("100000.000000"));
        article.setComSalePrice(new BigDecimal("120000.000000"));
        article.setVat(new BigDecimal("0.200000"));
        article.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemRepository.save(article);

        String body = "{\"trancheId\":\"" + tranche.getId() + "\",\"articleId\":\"" + article.getId()
                + "\",\"remisePercent\":10}";

        // Total TTC = 120 000, remise 10% = 12 000 -> prix de vente TTC = 108 000.
        String reponse = mockMvc.perform(post("/api/prospections/{id}/devis", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montants.remisePercent").value(10.0))
                .andExpect(jsonPath("$.montants.remiseHT").value(10000))
                .andExpect(jsonPath("$.montants.remiseTTC").value(12000))
                .andExpect(jsonPath("$.montants.prixVenteTTC").value(108000))
                .andReturn().getResponse().getContentAsString();

        // Relue en base : ratio 0–1, montant HT et montant TTC, comme le logiciel de bureau.
        entityManager.flush();
        entityManager.clear();
        StlQuote relue = quoteRepository.findById(
                UUID.fromString(com.jayway.jsonpath.JsonPath.read(reponse, "$.id").toString())).orElseThrow();
        org.junit.jupiter.api.Assertions.assertAll(
                () -> org.assertj.core.api.Assertions.assertThat(relue.getDiscount()).isEqualByComparingTo("0.1"),
                () -> org.assertj.core.api.Assertions.assertThat(relue.getDiscountAmountHT()).isEqualByComparingTo("10000"),
                () -> org.assertj.core.api.Assertions.assertThat(relue.getDiscountAmount()).isEqualByComparingTo("12000"));
    }

    @Test
    void listerDevis_offreEcriteParLeLogicielDeBureau_remiseRatioExposeeEnPourcentage() throws Exception {
        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-DEVIS-OMRAN");
        prospectionRepository.save(prospection);
        CfgTranche tranche = creerTranche("TR-DEVIS-OMRAN");

        // Valeurs de l'offre réelle 26/00312 : Discount = 0,24 (ratio), montants HT et TTC.
        StlQuote omran = new StlQuote();
        omran.setId(UUID.randomUUID());
        omran.setCode("OMRAN-24");
        omran.setDocDate(java.time.LocalDateTime.of(2026, 9, 1, 0, 0));
        omran.setComProspection(prospection);
        omran.setCfgTranche(tranche);
        omran.setPriceBeforeDiscountHT(new BigDecimal("1440000"));
        omran.setPriceBeforeDiscount(new BigDecimal("1728000"));
        omran.setDiscount(new BigDecimal("0.240000"));
        omran.setDiscountAmountHT(new BigDecimal("345600"));
        omran.setDiscountAmount(new BigDecimal("414720"));
        omran.setPriceAfterDiscountHT(new BigDecimal("1094400"));
        omran.setPriceAfterDiscount(new BigDecimal("1313280"));
        quoteRepository.save(omran);

        mockMvc.perform(get("/api/prospections/{id}/devis", prospection.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].montants.remisePercent").value(24.0))
                .andExpect(jsonPath("$.content[0].montants.remiseHT").value(345600))
                .andExpect(jsonPath("$.content[0].montants.remiseTTC").value(414720));
    }

    /**
     * Quand l'offre a un prix, le "Schéma de financement" (AUTO-FINANCEMENT / AVANCE) doit être
     * calculé et renvoyé — même règle que {@code POST /api/dossiers/simulation-financement}.
     */
    @Test
    void creerDevis_avecPrix_rajouteLeSchemaDeFinancement() throws Exception {
        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-DEVIS-12");
        prospectionRepository.save(prospection);

        CfgTranche tranche = creerTranche("TR-DEVIS-12");
        tranche.setAdvancePercent(new BigDecimal("0.300000"));
        trancheRepository.save(tranche);

        String body = "{\"trancheId\":\"" + tranche.getId() + "\",\"prix\":300000}";

        mockMvc.perform(post("/api/prospections/{id}/devis", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemaFinancement[?(@.categorie=='AVANCE')].montant").value(90000.0))
                .andExpect(jsonPath("$.schemaFinancement[?(@.categorie=='AUTO-FINANCEMENT')].montant").value(210000.0));
    }

    @Test
    void creerDevis_missingTrancheId_returns400() throws Exception {
        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-DEVIS-4");
        prospectionRepository.save(prospection);

        mockMvc.perform(post("/api/prospections/{id}/devis", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"prix\":100000}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creerDevis_niArticleNiPrix_returns400() throws Exception {
        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-DEVIS-5");
        prospectionRepository.save(prospection);

        CfgTranche tranche = creerTranche("TR-DEVIS-5");

        mockMvc.perform(post("/api/prospections/{id}/devis", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"trancheId\":\"" + tranche.getId() + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getDevis_unknownProspection_returns404() throws Exception {
        mockMvc.perform(get("/api/prospections/{id}/devis", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getDevisById_unknownDevis_returns404() throws Exception {
        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-DEVIS-6");
        prospectionRepository.save(prospection);

        mockMvc.perform(get("/api/prospections/{id}/devis/{devisId}", prospection.getId(), UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void creerDevis_avecSousArticle_persisteLaLigneEtGenereLaFicheOffreVentePdf() throws Exception {
        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-DEVIS-7");
        prospectionRepository.save(prospection);

        CfgTranche tranche = creerTranche("TR-DEVIS-7");

        StkItemType typeAppart = new StkItemType();
        typeAppart.setId(UUID.randomUUID());
        typeAppart.setCode("TYPE-DEVIS-7");
        typeAppart.setDescription("2 CHAMBRES + SALON");
        typeAppart.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemTypeRepository.save(typeAppart);

        StkItemCategory categorieAppart = new StkItemCategory();
        categorieAppart.setId(UUID.randomUUID());
        categorieAppart.setCode("CAT-DEVIS-7");
        categorieAppart.setDescription("APPARTEMENT");
        categorieAppart.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemCategoryRepository.save(categorieAppart);

        StkItemCategory categorieJardin = new StkItemCategory();
        categorieJardin.setId(UUID.randomUUID());
        categorieJardin.setCode("CAT-DEVIS-7-JRD");
        categorieJardin.setDescription("JARDIN");
        categorieJardin.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemCategoryRepository.save(categorieJardin);

        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-DEVIS-7");
        stkVocationRepository.save(vocation);

        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode("LOT-DEVIS-7");
        article.setStkItemType(typeAppart);
        article.setStkItemCategory(categorieAppart);
        article.setStkVocation(vocation);
        article.setStkHierarchy(tranche);
        article.setArea(new BigDecimal("78.000000"));
        article.setComSalePriceHT(new BigDecimal("812500.000000"));
        article.setComSalePrice(new BigDecimal("975000.000000"));
        article.setVat(new BigDecimal("0.200000"));
        article.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemRepository.save(article);

        StkItem sousArticle = new StkItem();
        sousArticle.setId(UUID.randomUUID());
        sousArticle.setCode("LOT-DEVIS-7-JRD");
        sousArticle.setStkItemType(typeAppart);
        sousArticle.setStkItemCategory(categorieJardin);
        sousArticle.setStkVocation(vocation);
        sousArticle.setStkHierarchy(tranche);
        sousArticle.setParent(article);
        sousArticle.setArea(new BigDecimal("62.000000"));
        sousArticle.setComSalePriceHT(new BigDecimal("156792.890000"));
        sousArticle.setComSalePrice(new BigDecimal("188151.470000"));
        sousArticle.setVat(new BigDecimal("0.200000"));
        sousArticle.setAlwaysAffected(Boolean.TRUE);
        sousArticle.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemRepository.save(sousArticle);

        String body = "{\"trancheId\":\"" + tranche.getId() + "\",\"articleId\":\"" + article.getId()
                + "\",\"sousArticleIds\":[\"" + sousArticle.getId() + "\"]}";

        String responseJson = mockMvc.perform(post("/api/prospections/{id}/devis", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montants.prixSousArticlesTTC").value(188151.47))
                .andReturn().getResponse().getContentAsString();

        UUID devisId = UUID.fromString(
                com.jayway.jsonpath.JsonPath.read(responseJson, "$.id").toString());

        // La ligne StlQuoteItem doit avoir été persistée pour le sous-article inclus.
        var lignes = quoteItemRepository.findByStlQuoteIdOrderByCodeAsc(devisId);
        org.assertj.core.api.Assertions.assertThat(lignes).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(lignes.get(0).getCode()).isEqualTo("LOT-DEVIS-7-JRD");
        org.assertj.core.api.Assertions.assertThat(lignes.get(0).getPriceAfterDiscount())
                .isEqualByComparingTo(new BigDecimal("188151.470000"));

        mockMvc.perform(get("/api/prospections/{id}/devis/{devisId}/document", prospection.getId(), devisId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(
                        result.getResponse().getContentAsByteArray().length).isGreaterThan(1000));
    }

    @Test
    void getDevisDocument_unknownDevis_returns404() throws Exception {
        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-DEVIS-8");
        prospectionRepository.save(prospection);

        mockMvc.perform(get("/api/prospections/{id}/devis/{devisId}/document", prospection.getId(), UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    /**
     * Le "Numéro fiche d'offre" ({@code StlQuote.Code}) doit être généré via {@code AdmSequence}
     * (ligne {@code Code = "Offre"}), au même format que le legacy: "{année sur 2 chiffres}/
     * {numéro sur 5 chiffres}" (ex. "26/00313"), un compteur global qui s'incrémente à chaque
     * offre créée, quelle que soit l'année.
     */
    @Test
    void creerDevis_genereUnNumeroDeFicheOffreIncrementalAuFormatAnneeNumero() throws Exception {
        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-DEVIS-9");
        prospectionRepository.save(prospection);

        CfgTranche tranche = creerTranche("TR-DEVIS-9");
        String body = "{\"trancheId\":\"" + tranche.getId() + "\",\"prix\":300000}";

        String anneeCourante = String.format("%02d", java.time.LocalDateTime.now().getYear() % 100);

        var premiereReponse = mockMvc.perform(post("/api/prospections/{id}/devis", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.code", org.hamcrest.Matchers.matchesPattern(anneeCourante + "/\\d{5}")))
                .andReturn();

        String premierCode = com.jayway.jsonpath.JsonPath.read(
                premiereReponse.getResponse().getContentAsString(), "$.code");
        int premierNumero = Integer.parseInt(premierCode.substring(premierCode.indexOf('/') + 1));

        mockMvc.perform(post("/api/prospections/{id}/devis", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(anneeCourante + "/" + String.format("%05d", premierNumero + 1)));
    }

    /** {@code GET /api/devis} — liste globale, symétrique de {@code GET /api/dossiers}. */
    @Test
    void findAllDevis_listeGlobale_toutesProspectionsConfondues_avecProspectionIdEtCode() throws Exception {
        ComProspection prospectionA = new ComProspection();
        prospectionA.setId(UUID.randomUUID());
        prospectionA.setCode("PSN-DEVIS-10-A");
        prospectionRepository.save(prospectionA);

        ComProspection prospectionB = new ComProspection();
        prospectionB.setId(UUID.randomUUID());
        prospectionB.setCode("PSN-DEVIS-10-B");
        prospectionRepository.save(prospectionB);

        CfgTranche tranche = creerTranche("TR-DEVIS-10");

        mockMvc.perform(post("/api/prospections/{id}/devis", prospectionA.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trancheId\":\"" + tranche.getId() + "\",\"prix\":100000}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/prospections/{id}/devis", prospectionB.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trancheId\":\"" + tranche.getId() + "\",\"prix\":200000}"))
                .andExpect(status().isOk());

        // Liste globale (/api/devis, pas /api/prospections/{id}/devis) : les deux offres, malgré
        // des prospections différentes, avec prospectionId/prospectionCode renseignés pour
        // chacune (colonne "Prospection" côté frontend).
        mockMvc.perform(get("/api/devis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.prospectionId=='" + prospectionA.getId() + "')].prospectionCode")
                        .value("PSN-DEVIS-10-A"))
                .andExpect(jsonPath("$.content[?(@.prospectionId=='" + prospectionB.getId() + "')].prospectionCode")
                        .value("PSN-DEVIS-10-B"));
    }

    /**
     * Filtres trancheId (répétable) et commercial (unique) sur {@code GET /api/devis}, plus
     * sommePrixVenteTTCPage: la somme des prixVenteTTC des offres réellement renvoyées dans la
     * page (pas un total plein-filtre indépendant de la pagination).
     */
    @Test
    void findAllDevis_filtreParTrancheIdEtCommercial_etSommePageCourante() throws Exception {
        CfgTier commercial = new CfgTier();
        commercial.setId(UUID.randomUUID());
        commercial.setCode("COM-DEVIS-11");
        commercial.setLastName("Commercial 11");
        tierRepository.save(commercial);

        CfgTier autreCommercial = new CfgTier();
        autreCommercial.setId(UUID.randomUUID());
        autreCommercial.setCode("COM-DEVIS-11-AUTRE");
        autreCommercial.setLastName("Autre commercial");
        tierRepository.save(autreCommercial);

        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-DEVIS-11");
        prospectionRepository.save(prospection);

        CfgTranche trancheCiblee = creerTranche("TR-DEVIS-11-A");
        CfgTranche autreTranche = creerTranche("TR-DEVIS-11-B");

        // Offre attendue: bonne tranche + bon commercial, prix 100 000.
        mockMvc.perform(post("/api/prospections/{id}/devis", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trancheId\":\"" + trancheCiblee.getId() + "\",\"commercialId\":\""
                                + commercial.getId() + "\",\"prix\":100000}"))
                .andExpect(status().isOk());
        // Écartée par le filtre trancheId.
        mockMvc.perform(post("/api/prospections/{id}/devis", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trancheId\":\"" + autreTranche.getId() + "\",\"commercialId\":\""
                                + commercial.getId() + "\",\"prix\":999999}"))
                .andExpect(status().isOk());
        // Écartée par le filtre commercial.
        mockMvc.perform(post("/api/prospections/{id}/devis", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trancheId\":\"" + trancheCiblee.getId() + "\",\"commercialId\":\""
                                + autreCommercial.getId() + "\",\"prix\":888888}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/devis")
                        .param("trancheId", trancheCiblee.getId().toString())
                        .param("commercial", commercial.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].montants.prixVenteTTC").value(100000))
                .andExpect(jsonPath("$.sommePrixVenteTTCPage").value(100000));
    }
}

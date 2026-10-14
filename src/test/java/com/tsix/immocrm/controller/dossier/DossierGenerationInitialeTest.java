package com.tsix.immocrm.controller.dossier;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.cfg.CfgTrancheSettlementCategory;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComPaymentMethod;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.fixtures.FixtureStatutsDossier;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheSettlementCategoryRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComPaymentMethodRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Génération initiale complète à la création (FEATURE-vente-echeancier.md §1.6), sur le cas réel yesRemsProd
 * « YES 2022/00200 » : prix 756 000, tranche YES (pré-réservation 5 000, avance au montant 100 000, frais de
 * dossier 7 500, syndic 600 × 12).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class DossierGenerationInitialeTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private FixtureReferentielStk fixtureReferentielStk;
    @Autowired private FixtureStatutsDossier fixtureStatuts;
    @Autowired private ComProspectionRepository prospectionRepository;
    @Autowired private CfgTierRepository tierRepository;
    @Autowired private CfgProjectRepository projectRepository;
    @Autowired private CfgTrancheRepository trancheRepository;
    @Autowired private CfgTrancheSettlementCategoryRepository trancheCategorieRepository;
    @Autowired private StkItemRepository stkItemRepository;
    @Autowired private StkItemTypeRepository stkItemTypeRepository;
    @Autowired private StkItemCategoryRepository stkItemCategoryRepository;
    @Autowired private StkVocationRepository stkVocationRepository;
    @Autowired private ComPaymentMethodRepository paymentMethodRepository;
    @Autowired private ComSettlementCategoryRepository settlementCategoryRepository;
    @Autowired private ComDocumentRepository documentRepository;

    private StkVocation vocation;
    private CfgTranche tranche;

    @BeforeEach
    void trancheYes() {
        fixtureStatuts.reservation();
        vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-GEN-INIT");
        vocation = stkVocationRepository.save(vocation);
        categorie("PRÉ-RÉSERVATION", false, true);
        categorie("AVANCE", false, false);
        categorie("AUTO-FINANCEMENT", false, false);
        categorie("FRAIS DOSSIER", true, false);
        categorie("FRAIS SYNDIC", true, false);

        CfgProject projet = new CfgProject();
        projet.setId(UUID.randomUUID());
        projet.setCode("PRJ-GEN-INIT");
        projectRepository.save(projet);
        tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("YES-GEN-INIT");
        tranche.setCfgProject(projet);
        tranche.setBeforeReservationAmount(new BigDecimal("5000"));
        tranche.setAdvanceBasedOnAmount(true);
        tranche.setAdvanceAmount(new BigDecimal("100000"));
        tranche.setAdvancePercent(new BigDecimal("0.2"));
        tranche.setDefaultExpenseAmount(new BigDecimal("7500"));
        tranche.setSyndicAmount(new BigDecimal("600"));
        tranche.setSyndicPeriod(12);
        tranche = trancheRepository.save(tranche);
    }

    @Test
    void creation_casReelYes_cinqLignes_fraisNumerotes_prixEquilibre() throws Exception {
        UUID dossierId = creer();

        List<ComDocument> lignes = documentRepository.findByComFolderIdOrderByDocDateAsc(dossierId);
        assertThat(lignes)
                .extracting(d -> d.getComSettlementCategory().getCode(), d -> d.getAmount().stripTrailingZeros())
                .containsExactlyInAnyOrder(
                        tuple("PRÉ-RÉSERVATION", new BigDecimal("5000").stripTrailingZeros()),
                        tuple("AVANCE", new BigDecimal("95000").stripTrailingZeros()),
                        tuple("AUTO-FINANCEMENT", new BigDecimal("656000").stripTrailingZeros()),
                        tuple("FRAIS DOSSIER", new BigDecimal("7500").stripTrailingZeros()),
                        tuple("FRAIS SYNDIC", new BigDecimal("7200").stripTrailingZeros()));
        assertThat(lignes).filteredOn(d -> d.getComSettlementCategory().getIsSaleExpense())
                .hasSize(2)
                .allSatisfy(d -> assertThat(d.getQuittanceCode()).matches("\\d{2}/\\d{5}"));
        assertThat(lignes).filteredOn(d -> !d.getComSettlementCategory().getIsSaleExpense())
                .isNotEmpty()
                .allSatisfy(d -> assertThat(d.getQuittanceCode()).isEmpty());
    }

    @Test
    void creation_ligneDeTranche_pourcentageDuPrix() throws Exception {
        ComSettlementCategory notaire = categorie("FRAIS NOTAIRE", true, false);
        CfgTrancheSettlementCategory ligne = new CfgTrancheSettlementCategory();
        ligne.setId(UUID.randomUUID());
        ligne.setCfgTranche(tranche);
        ligne.setComSettlementCategory(notaire);
        ligne.setPercentOf(new BigDecimal("0.01"));
        ligne.setSorting(1);
        trancheCategorieRepository.save(ligne);
        entityManager.flush();
        entityManager.clear();

        UUID dossierId = creer();

        assertThat(documentRepository.findByComFolderIdOrderByDocDateAsc(dossierId))
                .filteredOn(d -> "FRAIS NOTAIRE".equals(d.getComSettlementCategory().getCode()))
                .singleElement()
                .satisfies(d -> assertThat(d.getAmount()).isEqualByComparingTo("7560"));
    }

    private UUID creer() throws Exception {
        StkItemType type = new StkItemType();
        type.setId(UUID.randomUUID());
        type.setCode("TYPE-GEN-INIT");
        type.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemTypeRepository.save(type);
        StkItemCategory categorieArticle = new StkItemCategory();
        categorieArticle.setId(UUID.randomUUID());
        categorieArticle.setCode("CAT-GEN-INIT");
        categorieArticle.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemCategoryRepository.save(categorieArticle);
        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode("LOT-GEN-INIT");
        article.setStkItemType(type);
        article.setStkItemCategory(categorieArticle);
        article.setStkVocation(stkVocationRepository.findById(vocation.getId()).orElseThrow());
        article.setStkHierarchy(trancheRepository.findById(tranche.getId()).orElseThrow());
        article.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemRepository.save(article);
        CfgTier prospect = new CfgTier();
        prospect.setId(UUID.randomUUID());
        prospect.setCode("CLI-GEN-INIT");
        tierRepository.save(prospect);
        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-GEN-INIT");
        prospection.setCfgProspect(prospect);
        prospectionRepository.save(prospection);
        ComPaymentMethod modePaie = new ComPaymentMethod();
        modePaie.setId(UUID.randomUUID());
        modePaie.setCode("VIR-GEN-INIT");
        paymentMethodRepository.save(modePaie);

        String json = mockMvc.perform(post("/api/dossiers").contentType(MediaType.APPLICATION_JSON).content("""
                        {"prospectionId":"%s","articleIds":["%s"],"prix":756000,"modePaieId":"%s","cin":"CIN-GEN"}"""
                        .formatted(prospection.getId(), article.getId(), modePaie.getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        entityManager.flush();
        return UUID.fromString(JsonPath.read(json, "$.id"));
    }

    private ComSettlementCategory categorie(String code, boolean frais, boolean avance) {
        ComSettlementCategory c = new ComSettlementCategory();
        c.setId(UUID.randomUUID());
        c.setCode(code);
        c.setDescription(code);
        c.setIsCredit(true);
        c.setIsSaleExpense(frais);
        c.setIsSaleAdvance(avance || "AVANCE".equals(code));
        c.setIsSaleBeforeReservation(avance);
        c.setIsSaleAutoFinancment("AUTO-FINANCEMENT".equals(code));
        c.setExcludeFromPrice(frais);
        c.setVatRatio(new BigDecimal("0.2"));
        c.setStkVocation(vocation);
        return settlementCategoryRepository.save(c);
    }
}

package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code POST /api/dossiers/simulation-financement} — prévisualisation du schéma de financement
 * avant création du dossier, sans rien persister.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class DossierSimulationFinancementTest {

    @Autowired
    private FixtureReferentielStk fixtureReferentielStk;
    @Autowired
    private MockMvc mockMvc;

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
    private StkVocationRepository stkVocationRepository;

    @Autowired
    private ComSettlementCategoryRepository settlementCategoryRepository;

    @Autowired
    private ComFolderRepository folderRepository;

    private ComSettlementCategory newCategory(String code, StkVocation vocation, Boolean excludeFromPrice) {
        ComSettlementCategory categorie = new ComSettlementCategory();
        categorie.setId(UUID.randomUUID());
        categorie.setCode(code);
        categorie.setDescription(code);
        categorie.setStkVocation(vocation);
        categorie.setExcludeFromPrice(excludeFromPrice);
        return settlementCategoryRepository.save(categorie);
    }

    @Test
    void simuler_avecPrixEtTrancheConfig_calculeLesMemesMontantsQueLaCreationReelle() throws Exception {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-SIM-TEST");
        stkVocationRepository.save(vocation);

        newCategory("AUTO-FINANCEMENT", vocation, false);
        newCategory("AVANCE", vocation, false);
        newCategory("FRAIS SYNDIC", vocation, true);

        CfgProject project = new CfgProject();
        project.setId(UUID.randomUUID());
        project.setCode("PRJ-SIM-TEST");
        projectRepository.save(project);

        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-SIM-TEST");
        tranche.setCfgProject(project);
        tranche.setAdvancePercent(new BigDecimal("0.300000"));
        tranche.setSyndicAmount(new BigDecimal("700.000000"));
        tranche.setSyndicPeriod(24);
        trancheRepository.save(tranche);

        StkItemType itemType = new StkItemType();
        itemType.setId(UUID.randomUUID());
        itemType.setCode("TYPE-SIM-TEST");
        itemType.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemTypeRepository.save(itemType);

        StkItemCategory itemCategory = new StkItemCategory();
        itemCategory.setId(UUID.randomUUID());
        itemCategory.setCode("CAT-SIM-TEST");
        itemCategory.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemCategoryRepository.save(itemCategory);

        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode("LOT-SIM-TEST");
        article.setStkItemType(itemType);
        article.setStkItemCategory(itemCategory);
        article.setStkVocation(vocation);
        article.setStkHierarchy(tranche);
        article.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemRepository.save(article);

        long dossiersAvant = folderRepository.count();

        String body = """
                {"articleIds":["%s"],"prix":1548000}""".formatted(article.getId());

        // 1 548 000 * 0.30 = 464 400 ; auto-financement = 1 548 000 - 464 400 = 1 083 600
        // frais syndic = 700 * 24 = 16 800 — mêmes montants que la création réelle (DossierFinancingSchemaTest).
        mockMvc.perform(post("/api/dossiers/simulation-financement")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lignes[?(@.categorie=='AVANCE')].montant").value(464400.0))
                .andExpect(jsonPath("$.lignes[?(@.categorie=='AUTO-FINANCEMENT')].montant").value(1083600.0))
                .andExpect(jsonPath("$.lignes[?(@.categorie=='FRAIS SYNDIC')].montant").value(16800.0))
                .andExpect(jsonPath("$.lignes[?(@.categorie=='FRAIS SYNDIC')].exclureDuPrix").value(true))
                .andExpect(jsonPath("$.totalGeneral").value(1548000.0 + 16800.0));

        // Rien n'a été persisté : ni dossier créé, ni article réservé.
        org.assertj.core.api.Assertions.assertThat(folderRepository.count()).isEqualTo(dossiersAvant);
        org.assertj.core.api.Assertions.assertThat(
                stkItemRepository.findById(article.getId()).orElseThrow().getComFolderId()).isNull();
    }

    @Test
    void simuler_avecRemisePercent_calculeSurLePrixApresRemise() throws Exception {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-SIM-REMISE");
        stkVocationRepository.save(vocation);

        newCategory("AUTO-FINANCEMENT", vocation, false);
        newCategory("AVANCE", vocation, false);
        newCategory("FRAIS SYNDIC", vocation, true);

        CfgProject project = new CfgProject();
        project.setId(UUID.randomUUID());
        project.setCode("PRJ-SIM-REMISE");
        projectRepository.save(project);

        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-SIM-REMISE");
        tranche.setCfgProject(project);
        tranche.setAdvancePercent(new BigDecimal("0.300000"));
        tranche.setSyndicAmount(new BigDecimal("700.000000"));
        tranche.setSyndicPeriod(24);
        trancheRepository.save(tranche);

        StkItemType itemType = new StkItemType();
        itemType.setId(UUID.randomUUID());
        itemType.setCode("TYPE-SIM-REMISE");
        itemType.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemTypeRepository.save(itemType);

        StkItemCategory itemCategory = new StkItemCategory();
        itemCategory.setId(UUID.randomUUID());
        itemCategory.setCode("CAT-SIM-REMISE");
        itemCategory.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemCategoryRepository.save(itemCategory);

        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode("LOT-SIM-REMISE");
        article.setStkItemType(itemType);
        article.setStkItemCategory(itemCategory);
        article.setStkVocation(vocation);
        article.setStkHierarchy(tranche);
        article.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemRepository.save(article);

        String body = """
                {"articleIds":["%s"],"prix":1548000,"remisePercent":10}""".formatted(article.getId());

        // Mêmes montants que la création réelle avec les mêmes paramètres
        // (DossierFinancingSchemaTest#create_withRemisePercent_schemaCalculeSurLePrixApresRemise) :
        // prix dû 1 393 200 => AVANCE 417 960, AUTO-FINANCEMENT 975 240, syndic inchangé.
        mockMvc.perform(post("/api/dossiers/simulation-financement")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lignes[?(@.categorie=='AVANCE')].montant").value(417960.0))
                .andExpect(jsonPath("$.lignes[?(@.categorie=='AUTO-FINANCEMENT')].montant").value(975240.0))
                .andExpect(jsonPath("$.lignes[?(@.categorie=='FRAIS SYNDIC')].montant").value(16800.0))
                .andExpect(jsonPath("$.totalGeneral").value(1393200.0 + 16800.0));
    }

    /** Article sans tranche (pas de barème) : seuls les montants de remise sont observés. */
    private StkItem articleAvecPrix(String code, String prixTtc) {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-" + code);
        stkVocationRepository.save(vocation);

        StkItemType itemType = new StkItemType();
        itemType.setId(UUID.randomUUID());
        itemType.setCode("TYPE-" + code);
        itemType.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemTypeRepository.save(itemType);

        StkItemCategory itemCategory = new StkItemCategory();
        itemCategory.setId(UUID.randomUUID());
        itemCategory.setCode("CAT-" + code);
        itemCategory.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemCategoryRepository.save(itemCategory);

        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode(code);
        article.setStkItemType(itemType);
        article.setStkItemCategory(itemCategory);
        article.setStkVocation(vocation);
        article.setStkKind(fixtureReferentielStk.natureParDefaut());
        article.setVat(new BigDecimal("0.20"));
        article.setComSalePrice(new BigDecimal(prixTtc));
        return stkItemRepository.save(article);
    }

    @Test
    void simuler_remiseExpliciteAvecAnnexe_remiseSurPrincipalPlusAnnexe() throws Exception {
        StkItem principal = articleAvecPrix("LOT-SIM-REM-PRINC", "1000000.00");
        StkItem annexe = articleAvecPrix("LOT-SIM-REM-PARK", "200000.00");

        String body = """
                {"articleIds":["%s","%s"],"prix":1000000,"remisePercent":5}"""
                .formatted(principal.getId(), annexe.getId());

        // (1 000 000 saisi + 200 000 annexe) × 5 % = 60 000 ; après remise 1 140 000.
        mockMvc.perform(post("/api/dossiers/simulation-financement")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prixAvantRemise").value(1200000.0))
                .andExpect(jsonPath("$.montantRemise").value(60000.0))
                .andExpect(jsonPath("$.remisePercent").value(5))
                .andExpect(jsonPath("$.prixApresRemise").value(1140000.0));
    }

    @Test
    void simuler_sansRemise_tauxDeduitDuPrixDeReference() throws Exception {
        StkItem principal = articleAvecPrix("LOT-SIM-DED-PRINC", "1000000.00");
        StkItem annexe = articleAvecPrix("LOT-SIM-DED-PARK", "200000.00");

        String body = """
                {"articleIds":["%s","%s"],"prix":1080000}"""
                .formatted(principal.getId(), annexe.getId());

        // Référence 1 200 000, prix négocié 1 080 000 : remise 120 000 = 10 % de 1 200 000.
        mockMvc.perform(post("/api/dossiers/simulation-financement")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prixAvantRemise").value(1200000.0))
                .andExpect(jsonPath("$.montantRemise").value(120000.0))
                .andExpect(jsonPath("$.remisePercent").value(10.0))
                .andExpect(jsonPath("$.prixApresRemise").value(1080000.0));
    }

    @Test
    void simuler_remisePercentHorsBornes_returns400() throws Exception {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-SIM-REMISE-KO");
        stkVocationRepository.save(vocation);

        StkItemType itemType = new StkItemType();
        itemType.setId(UUID.randomUUID());
        itemType.setCode("TYPE-SIM-REMISE-KO");
        itemType.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemTypeRepository.save(itemType);

        StkItemCategory itemCategory = new StkItemCategory();
        itemCategory.setId(UUID.randomUUID());
        itemCategory.setCode("CAT-SIM-REMISE-KO");
        itemCategory.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemCategoryRepository.save(itemCategory);

        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode("LOT-SIM-REMISE-KO");
        article.setStkItemType(itemType);
        article.setStkItemCategory(itemCategory);
        article.setStkVocation(vocation);
        article.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemRepository.save(article);

        mockMvc.perform(post("/api/dossiers/simulation-financement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"articleIds\":[\"" + article.getId() + "\"],\"prix\":100000,\"remisePercent\":150}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void simuler_articleOccupe_fonctionneQuandMeme() throws Exception {
        // Contrairement à la création réelle, la simulation n'exige pas que l'article soit
        // DISPONIBLE — c'est un aperçu, rien n'est réservé.
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-SIM-OCCUPE");
        stkVocationRepository.save(vocation);

        StkItemType itemType = new StkItemType();
        itemType.setId(UUID.randomUUID());
        itemType.setCode("TYPE-SIM-OCCUPE");
        itemType.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemTypeRepository.save(itemType);

        StkItemCategory itemCategory = new StkItemCategory();
        itemCategory.setId(UUID.randomUUID());
        itemCategory.setCode("CAT-SIM-OCCUPE");
        itemCategory.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemCategoryRepository.save(itemCategory);

        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode("LOT-SIM-OCCUPE");
        article.setStkItemType(itemType);
        article.setStkItemCategory(itemCategory);
        article.setStkVocation(vocation);
        article.setComFolderId(UUID.randomUUID());
        article.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemRepository.save(article);

        String body = """
                {"articleIds":["%s"],"prix":900000}""".formatted(article.getId());

        mockMvc.perform(post("/api/dossiers/simulation-financement")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @Test
    void simuler_sansPrix_returns400() throws Exception {
        StkItemType itemType = new StkItemType();
        itemType.setId(UUID.randomUUID());
        itemType.setCode("TYPE-SIM-NOPRIX");
        itemType.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemTypeRepository.save(itemType);

        StkItemCategory itemCategory = new StkItemCategory();
        itemCategory.setId(UUID.randomUUID());
        itemCategory.setCode("CAT-SIM-NOPRIX");
        itemCategory.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemCategoryRepository.save(itemCategory);

        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-SIM-NOPRIX");
        stkVocationRepository.save(vocation);

        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode("LOT-SIM-NOPRIX");
        article.setStkItemType(itemType);
        article.setStkItemCategory(itemCategory);
        article.setStkVocation(vocation);
        article.setStkKind(fixtureReferentielStk.natureParDefaut());
        stkItemRepository.save(article);

        mockMvc.perform(post("/api/dossiers/simulation-financement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"articleIds\":[\"" + article.getId() + "\"]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void simuler_articleIdsVide_returns400() throws Exception {
        mockMvc.perform(post("/api/dossiers/simulation-financement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"articleIds\":[],\"prix\":100000}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void simuler_articleInconnu_returns400() throws Exception {
        mockMvc.perform(post("/api/dossiers/simulation-financement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"articleIds\":[\"" + UUID.randomUUID() + "\"],\"prix\":100000}"))
                .andExpect(status().isBadRequest());
    }
}

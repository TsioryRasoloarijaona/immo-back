package com.tsix.immocrm.controller.dossier;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
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
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
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
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementTypeRepository;
import com.tsix.immocrm.services.dossier.RecuReglementService;
import com.tsix.immocrm.services.dossier.ReferencesOmran;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Règlement écrit comme OMRAN l'écrit : FEATURE-interop-omran-ecritures.md §5. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ReglementInteropOmranTest {

    private static final String REGLEMENTS = "/api/dossiers/{id}/echeancier/{echeanceId}/reglements";
    private static final String AUTEUR = "Agent Reglement";

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private FixtureReferentielStk fixtureReferentielStk;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private ComDocumentRepository documentRepository;
    @Autowired private StkVocationRepository vocationRepository;
    @Autowired private StkItemTypeRepository itemTypeRepository;
    @Autowired private StkItemCategoryRepository itemCategoryRepository;
    @Autowired private StkItemRepository itemRepository;
    @Autowired private CfgCompanyRepository companyRepository;
    @Autowired private CfgProjectRepository projectRepository;
    @Autowired private CfgTrancheRepository trancheRepository;
    @Autowired private CfgTierRepository tierRepository;
    @Autowired private StlSettlementTypeRepository settlementTypeRepository;
    @Autowired private StlSettlementRepository settlementRepository;
    @Autowired private AdmUserRepository userRepository;
    @Autowired private RecuReglementService recuReglementService;
    @Autowired private java.time.Clock clock;

    private CfgCompany societe;
    private CfgTier client;
    private ComFolder dossier;
    private ComDocument echeance;
    private AdmUser agent;

    @BeforeEach
    void setUp() {
        societe = new CfgCompany();
        societe.setId(UUID.randomUUID());
        societe.setCode("SOC-REG-OMRAN");
        societe.setDescription("Société règlements");
        societe.setIsActive(true);
        companyRepository.save(societe);
        CfgProject projet = new CfgProject();
        projet.setId(UUID.randomUUID());
        projet.setCode("PRJ-REG-OMRAN");
        projet.setCfgCompany(societe);
        projet = projectRepository.save(projet);
        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-REG-OMRAN");
        tranche.setCfgProject(projet);
        tranche = trancheRepository.save(tranche);

        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-REG-OMRAN");
        vocationRepository.save(vocation);
        StkItemType type = new StkItemType();
        type.setId(UUID.randomUUID());
        type.setCode("TYPE-REG-OMRAN");
        type.setStkKind(fixtureReferentielStk.natureParDefaut());
        itemTypeRepository.save(type);
        StkItemCategory categorie = new StkItemCategory();
        categorie.setId(UUID.randomUUID());
        categorie.setCode("CAT-REG-OMRAN");
        categorie.setStkKind(fixtureReferentielStk.natureParDefaut());
        itemCategoryRepository.save(categorie);
        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode("LOT-REG-OMRAN");
        article.setStkItemType(type);
        article.setStkItemCategory(categorie);
        article.setStkVocation(vocation);
        article.setStkKind(fixtureReferentielStk.natureParDefaut());
        article.setStkHierarchy(tranche);
        article = itemRepository.save(article);

        client = tiers("CLI-REG-OMRAN", "Client Principal");
        dossier = new ComFolder();
        dossier.setId(UUID.randomUUID());
        dossier.setCode("DOS-REG-OMRAN");
        dossier.setStkVocation(vocation);
        dossier.setStkItem(article);
        dossier.setCfgCustomer(client);
        dossier = folderRepository.save(dossier);

        echeance = new ComDocument();
        echeance.setId(UUID.randomUUID());
        echeance.setComFolder(dossier);
        echeance.setAmount(new BigDecimal("100000"));
        echeance = documentRepository.save(echeance);

        agent = new AdmUser();
        agent.setId(UUID.randomUUID());
        agent.setCode("agent.reg." + UUID.randomUUID());
        agent.setDescription(AUTEUR);
        agent.setDisabled(false);
        agent = userRepository.save(agent);
    }

    private CfgTier tiers(String code, String nom) {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode(code);
        tier.setDescription(nom);
        return tierRepository.save(tier);
    }

    private StlSettlementType typeReglement(String code, String libelle) {
        StlSettlementType type = new StlSettlementType();
        type.setId(UUID.randomUUID());
        type.setCode(code);
        type.setDescription(libelle);
        return settlementTypeRepository.save(type);
    }

    private StlSettlement creer(String corps) throws Exception {
        String json = mockMvc.perform(post(REGLEMENTS, dossier.getId(), echeance.getId()).with(connecte(agent))
                        .contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        entityManager.flush();
        entityManager.clear();
        return settlementRepository.findById(UUID.fromString(JsonPath.read(json, "$.id"))).orElseThrow();
    }

    private String aujourdHui() {
        return LocalDate.now(clock).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    @Test
    void addReglement_ecritToutesLesColonnesDOmran() throws Exception {
        StlSettlementType type = typeReglement("CHQ-REG-OMRAN", "CHEQUE");
        CfgTier payeur = tiers("PAY-REG-OMRAN", "Tiers Payeur");
        CfgTier banque = tiers("BMCE", "Banque BMCE");

        StlSettlement s = creer("""
                {"montant":40000,"dateEcheance":"2026-12-05T10:00:00","typeId":"%s","payeurId":"%s",\
                "banqueId":"%s","reference":"0012345"}""".formatted(type.getId(), payeur.getId(), banque.getId()));

        assertAll(
                () -> assertThat(s.getAdmModuleId()).isEqualTo(ReferencesOmran.MODULE_COMMERCIAL),
                () -> assertThat(s.getStrEntityId()).isEqualTo(ReferencesOmran.ENTITE_REGLEMENT_ENTREE),
                () -> assertThat(s.getStlCurrencyId()).isEqualTo(ReferencesOmran.DEVISE_MAD),
                () -> assertThat(s.getStlCurrencyAffectationId()).isEqualTo(ReferencesOmran.DEVISE_MAD),
                () -> assertThat(s.getConvertValue()).isEqualByComparingTo("1"),
                () -> assertThat(s.getConvertUnit()).isEqualTo(1),
                () -> assertThat(s.getAmountBase()).isEqualByComparingTo("40000"),
                () -> assertThat(s.getAmountBaseConverted()).isEqualByComparingTo("40000"),
                () -> assertThat(s.getAmountRS()).isEqualByComparingTo("0"),
                () -> assertThat(s.getRsRatio()).isEqualByComparingTo("0"),
                () -> assertThat(s.getAmountStamp()).isEqualByComparingTo("0"),
                () -> assertThat(s.getRSAmountSettled()).isFalse(),
                () -> assertThat(s.getIsAccounted()).isFalse(),
                () -> assertThat(s.getIsDcoumentDeliveredToCenter()).isFalse(),
                () -> assertThat(s.getCategory()).isEqualTo("None"),
                () -> assertThat(s.getItemNumber()).isEqualTo("Non définie"),
                () -> assertThat(s.getItemDescription()).isEqualTo("Non définie"),
                () -> assertThat(s.getCfgCompany().getId()).isEqualTo(societe.getId()),
                () -> assertThat(s.getTier().getId()).isEqualTo(client.getId()),
                () -> assertThat(s.getTierPayer().getId()).isEqualTo(payeur.getId()),
                () -> assertThat(s.getIsCredit()).isTrue(),
                () -> assertThat(s.getStatus()).isEqualTo("IN_WORK"),
                () -> assertThat(s.getDocDate()).isEqualTo(LocalDate.now(clock).atStartOfDay()),
                () -> assertThat(s.getEcheanceDate()).isEqualTo(LocalDateTime.of(2026, 12, 5, 10, 0)),
                () -> assertThat(s.getDescription()).isEqualTo("CHEQUE BMCE N° 0012345 du " + aujourdHui()),
                () -> assertThat(s.getUserCreate()).isEqualTo(AUTEUR),
                () -> assertThat(s.getUserUpdate()).isEqualTo(AUTEUR),
                () -> assertThat(s.getUserUpdateDateTime()).isNotNull(),
                () -> assertThat(s.getSlipCode()).isEmpty(),
                () -> assertThat(s.getReceiptCode()).isEmpty());
    }

    @Test
    void sansPayeur_lePayeurEstLeClient_descriptionSansBanqueNiReference() throws Exception {
        StlSettlementType type = typeReglement("ESP-REG-OMRAN", "ESPECES");

        StlSettlement s = creer("""
                {"montant":1000,"dateEcheance":"2026-12-05T00:00:00","typeId":"%s"}""".formatted(type.getId()));

        assertAll(
                () -> assertThat(s.getTier().getId()).isEqualTo(client.getId()),
                () -> assertThat(s.getTierPayer().getId()).isEqualTo(client.getId()),
                () -> assertThat(s.getDescription()).isEqualTo("ESPECES du " + aujourdHui()));
    }

    @Test
    void sansPayeurNiClient_returns400() throws Exception {
        dossier.setCfgCustomer(null);
        folderRepository.save(dossier);
        StlSettlementType type = typeReglement("ESP-REG-OMRAN-2", "ESPECES");

        mockMvc.perform(post(REGLEMENTS, dossier.getId(), echeance.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"montant":1000,"dateEcheance":"2026-12-05T00:00:00","typeId":"%s"}"""
                                .formatted(type.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("payeurId")));
    }

    @Test
    void reglementDtoEtRecu_montrentLePayeur_etPasLeClient() throws Exception {
        StlSettlementType type = typeReglement("VIR-REG-OMRAN", "VIREMENT");
        CfgTier payeur = tiers("PAY-REG-OMRAN-2", "Payeur Recu");

        String json = mockMvc.perform(post(REGLEMENTS, dossier.getId(), echeance.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"montant":1000,"dateEcheance":"2026-12-05T00:00:00","typeId":"%s","payeurId":"%s"}"""
                                .formatted(type.getId(), payeur.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payeurId").value(payeur.getId().toString()))
                .andReturn().getResponse().getContentAsString();
        UUID reglementId = UUID.fromString(JsonPath.read(json, "$.id"));

        assertThat(recuReglementService.collect(dossier.getId(), reglementId).beneficiaire().nom())
                .isEqualTo("Payeur Recu");
    }

    @Test
    void changementDeStatut_signeLaModification() throws Exception {
        StlSettlementType type = typeReglement("VIR-REG-OMRAN-3", "VIREMENT");
        StlSettlement cree = creer("""
                {"montant":1000,"dateEcheance":"2026-12-05T00:00:00","typeId":"%s"}""".formatted(type.getId()));
        AdmUser autre = new AdmUser();
        autre.setId(UUID.randomUUID());
        autre.setCode("agent.reg.autre." + UUID.randomUUID());
        autre.setDescription("Autre Agent");
        autre.setDisabled(false);
        userRepository.save(autre);

        mockMvc.perform(patch("/api/dossiers/{id}/reglements/{reglementId}/statut", dossier.getId(), cree.getId())
                        .with(connecte(autre))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"statut\":\"CANCELED\"}"))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();
        StlSettlement relu = settlementRepository.findById(cree.getId()).orElseThrow();
        assertThat(relu.getUserUpdate()).isEqualTo("Autre Agent");
        assertThat(relu.getUserCreate()).isEqualTo(AUTEUR);
    }
}

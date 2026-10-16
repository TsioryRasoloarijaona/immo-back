package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComSettlementCategory;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.entities.stk.StkItemCategory;
import com.tsix.immocrm.entities.stk.StkItemType;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
import com.tsix.immocrm.repositories.adm.AdmUserRepository;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.repositories.cfg.CfgProjectRepository;
import com.tsix.immocrm.repositories.cfg.CfgTrancheRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComSettlementCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemCategoryRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import com.tsix.immocrm.repositories.stk.StkItemTypeRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.services.dossier.ReferencesOmran;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class EcheancierTest {

    private static final String ECHEANCIER = "/api/dossiers/{id}/echeancier";
    private static final String ECHEANCE = "/api/dossiers/{id}/echeancier/{echeanceId}";
    private static final String AUTEUR = "Agent Echeancier";

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private ComDocumentRepository documentRepository;
    @Autowired private StkVocationRepository stkVocationRepository;
    @Autowired private ComSettlementCategoryRepository categoryRepository;
    @Autowired private CfgCompanyRepository companyRepository;
    @Autowired private CfgProjectRepository projectRepository;
    @Autowired private CfgTrancheRepository trancheRepository;
    @Autowired private StkItemRepository stkItemRepository;
    @Autowired private StkItemTypeRepository stkItemTypeRepository;
    @Autowired private StkItemCategoryRepository stkItemCategoryRepository;
    @Autowired private AdmUserRepository userRepository;
    @Autowired private FixtureReferentielStk fixtureReferentielStk;

    private ComFolder newFolder(String code) {
        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode(code);
        folder.setStkVocation(newVocation());
        return folderRepository.save(folder);
    }

    private StkVocation newVocation() {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-ECH-" + UUID.randomUUID());
        return stkVocationRepository.save(vocation);
    }

    /** Dossier dont l'article principal mène à une société : article → tranche → projet → société. */
    private ComFolder newFolderAvecSociete(String code, CfgCompany societe) {
        CfgProject projet = new CfgProject();
        projet.setId(UUID.randomUUID());
        projet.setCode("PRJ-" + code);
        projet.setCfgCompany(societe);
        projet = projectRepository.save(projet);
        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-" + code);
        tranche.setCfgProject(projet);
        tranche = trancheRepository.save(tranche);
        StkVocation vocation = newVocation();
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
        article.setCode("LOT-" + code);
        article.setStkItemType(type);
        article.setStkItemCategory(categorie);
        article.setStkVocation(vocation);
        article.setStkKind(fixtureReferentielStk.natureParDefaut());
        article.setStkHierarchy(tranche);
        article = stkItemRepository.save(article);

        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode(code);
        folder.setStkVocation(vocation);
        folder.setStkItem(article);
        return folderRepository.save(folder);
    }

    private CfgCompany newSociete(String code) {
        CfgCompany societe = new CfgCompany();
        societe.setId(UUID.randomUUID());
        societe.setCode(code);
        societe.setDescription("Société " + code);
        societe.setIsActive(true);
        return companyRepository.save(societe);
    }

    private ComSettlementCategory newCategorie(String code, boolean exclueDuPrix) {
        ComSettlementCategory categorie = new ComSettlementCategory();
        categorie.setId(UUID.randomUUID());
        categorie.setCode(code);
        categorie.setDescription("Libellé " + code);
        categorie.setIsCredit(true);
        categorie.setVatRatio(new BigDecimal("0.2"));
        categorie.setExcludeFromPrice(exclueDuPrix);
        return categoryRepository.save(categorie);
    }

    private AdmUser newAgent() {
        AdmUser agent = new AdmUser();
        agent.setId(UUID.randomUUID());
        agent.setCode("agent.ech." + UUID.randomUUID());
        agent.setDescription(AUTEUR);
        agent.setDisabled(false);
        return userRepository.save(agent);
    }

    private ComDocument relire(UUID id) {
        entityManager.flush();
        entityManager.clear();
        return documentRepository.findById(id).orElseThrow();
    }

    @Test
    void findEcheances_returnsExistingLine() throws Exception {
        ComFolder folder = newFolder("DOS-ECH-1");
        ComDocument doc = new ComDocument();
        doc.setId(UUID.randomUUID());
        doc.setComFolder(folder);
        doc.setDescription("Avance");
        documentRepository.save(doc);

        mockMvc.perform(get(ECHEANCIER, folder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + doc.getId() + "')]").exists());
    }

    @Test
    void findEcheances_unknownDossier_returns404() throws Exception {
        mockMvc.perform(get(ECHEANCIER, UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void addEcheance_happyPath_returns201() throws Exception {
        ComFolder folder = newFolder("DOS-ECH-2");
        ComSettlementCategory categorie = newCategorie("AVANCE-ECH-2", false);
        String body = """
                {"montant":150000.00,"description":"Premiere echeance","categorieId":"%s"}"""
                .formatted(categorie.getId());

        mockMvc.perform(post(ECHEANCIER, folder.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Premiere echeance"))
                .andExpect(jsonPath("$.montant").value(150000.00));
    }

    /** OMRAN n'écrit jamais d'échéance sans catégorie (FEATURE-interop-omran-ecritures.md §4). */
    @Test
    void addEcheance_sansCategorie_returns400_etRienNEstEcrit() throws Exception {
        ComFolder folder = newFolder("DOS-ECH-2B");

        mockMvc.perform(post(ECHEANCIER, folder.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"montant\":1000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("categorieId")));

        assertThat(documentRepository.findByComFolderIdOrderByDocDateAsc(folder.getId())).isEmpty();
    }

    @Test
    void addEcheance_ecritLesColonnesDOmran() throws Exception {
        CfgCompany societe = newSociete("SOC-ECH-6");
        ComFolder folder = newFolderAvecSociete("DOS-ECH-6", societe);
        ComSettlementCategory categorie = newCategorie("FRAIS-ECH-6", true);
        AdmUser agent = newAgent();
        String body = """
                {"montant":100000.00,"dateEcheance":"2026-11-15T14:35:00","categorieId":"%s"}"""
                .formatted(categorie.getId());

        String json = mockMvc.perform(post(ECHEANCIER, folder.getId()).with(connecte(agent))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        ComDocument d = relire(UUID.fromString(com.jayway.jsonpath.JsonPath.read(json, "$.id")));
        assertAll(
                () -> assertThat(d.getCfgCompany().getId()).isEqualTo(societe.getId()),
                () -> assertThat(d.getAdmModuleId()).isEqualTo(ReferencesOmran.MODULE_COMMERCIAL),
                () -> assertThat(d.getVATRatio()).isEqualByComparingTo("0.2"),
                () -> assertThat(d.getAmountTTC()).isEqualByComparingTo("100000"),
                () -> assertThat(d.getAmountHT()).isEqualByComparingTo("83333.333333"),
                () -> assertThat(d.getDiscountRatio()).isEqualByComparingTo("0"),
                () -> assertThat(d.getExcludeFromPrice()).isTrue(),
                () -> assertThat(d.getDocDate()).isEqualTo(LocalDateTime.of(2026, 11, 15, 0, 0)),
                () -> assertThat(d.getDescription()).isEqualTo("Libellé FRAIS-ECH-6"),
                () -> assertThat(d.getUserCreate()).isEqualTo(AUTEUR),
                () -> assertThat(d.getUserUpdate()).isEqualTo(AUTEUR),
                () -> assertThat(d.getUserCreateDateTime()).isNotNull(),
                () -> assertThat(d.getUserUpdateDateTime()).isNotNull(),
                () -> assertThat(d.getCode()).isEmpty(),
                () -> assertThat(d.getQuittanceCode()).isEmpty());
    }

    @Test
    void updateEcheance_recalculeHTEtSigneLaModification() throws Exception {
        CfgCompany societe = newSociete("SOC-ECH-7");
        ComFolder folder = newFolderAvecSociete("DOS-ECH-7", societe);
        ComSettlementCategory categorie = newCategorie("AVANCE-ECH-7", false);
        ComDocument doc = new ComDocument();
        doc.setId(UUID.randomUUID());
        doc.setComFolder(folder);
        doc.setAmount(new BigDecimal("50000"));
        doc.setComSettlementCategory(categorie);
        documentRepository.save(doc);
        AdmUser agent = newAgent();

        mockMvc.perform(patch(ECHEANCE, folder.getId(), doc.getId()).with(connecte(agent))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"montant\":120000}"))
                .andExpect(status().isOk());

        ComDocument d = relire(doc.getId());
        assertAll(
                () -> assertThat(d.getAmountTTC()).isEqualByComparingTo("120000"),
                () -> assertThat(d.getAmountHT()).isEqualByComparingTo("100000"),
                () -> assertThat(d.getCfgCompany().getId()).isEqualTo(societe.getId()),
                () -> assertThat(d.getAdmModuleId()).isEqualTo(ReferencesOmran.MODULE_COMMERCIAL),
                () -> assertThat(d.getUserUpdate()).isEqualTo(AUTEUR),
                () -> assertThat(d.getUserUpdateDateTime()).isNotNull());
    }

    @Test
    void addEcheance_unknownDossier_returns404() throws Exception {
        String body = """
                {"montant":1000}""";

        mockMvc.perform(post(ECHEANCIER, UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateEcheance_happyPath_appliesPartialChanges() throws Exception {
        ComFolder folder = newFolder("DOS-ECH-3");
        ComDocument doc = new ComDocument();
        doc.setId(UUID.randomUUID());
        doc.setComFolder(folder);
        doc.setDescription("Ancienne description");
        doc.setComSettlementCategory(newCategorie("AVANCE-ECH-3", false));
        documentRepository.save(doc);

        String body = """
                {"description":"Nouvelle description"}""";

        mockMvc.perform(patch(ECHEANCE, folder.getId(), doc.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Nouvelle description"));
    }

    @Test
    void updateEcheance_ligneSansCategorie_returns400() throws Exception {
        ComFolder folder = newFolder("DOS-ECH-3B");
        ComDocument doc = new ComDocument();
        doc.setId(UUID.randomUUID());
        doc.setComFolder(folder);
        documentRepository.save(doc);

        mockMvc.perform(patch(ECHEANCE, folder.getId(), doc.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"montant\":1000}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateEcheance_belongsToDifferentDossier_returns404() throws Exception {
        ComFolder folder1 = newFolder("DOS-ECH-4A");
        ComFolder folder2 = newFolder("DOS-ECH-4B");
        ComDocument doc = new ComDocument();
        doc.setId(UUID.randomUUID());
        doc.setComFolder(folder1);
        documentRepository.save(doc);

        mockMvc.perform(patch(ECHEANCE, folder2.getId(), doc.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateEcheance_unknownEcheance_returns404() throws Exception {
        ComFolder folder = newFolder("DOS-ECH-5");

        mockMvc.perform(patch(ECHEANCE, folder.getId(), UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }
}

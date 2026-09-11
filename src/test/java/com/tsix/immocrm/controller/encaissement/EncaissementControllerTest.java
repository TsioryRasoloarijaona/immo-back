package com.tsix.immocrm.controller.encaissement;

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
import com.tsix.immocrm.entities.stl.StlAccount;
import com.tsix.immocrm.entities.stl.StlDeposit;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.fixtures.FixtureReferentielStk;
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
import com.tsix.immocrm.repositories.stl.StlAccountRepository;
import com.tsix.immocrm.repositories.stl.StlDepositRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementTypeRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/encaissements} — reproduit l'écran legacy "Encaissements Vente". Voir
 * {@code EncaissementService} pour le détail de calcul de montantBrutRestant/montantNet et la
 * restriction "au moins une ligne rattachée à une échéance de Dossier de vente".
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class EncaissementControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StkVocationRepository vocationRepository;
    @Autowired
    private ComFolderRepository folderRepository;
    @Autowired
    private ComDocumentRepository documentRepository;
    @Autowired
    private CfgCompanyRepository companyRepository;
    @Autowired
    private CfgTierRepository tierRepository;
    @Autowired
    private StlSettlementTypeRepository settlementTypeRepository;
    @Autowired
    private StlAccountRepository accountRepository;
    @Autowired
    private StlDepositRepository depositRepository;
    @Autowired
    private StlSettlementRepository settlementRepository;
    @Autowired
    private StlSettlementLineRepository settlementLineRepository;
    @Autowired
    private CfgProjectRepository projectRepository;
    @Autowired
    private CfgTrancheRepository trancheRepository;
    @Autowired
    private StkItemRepository itemRepository;
    @Autowired
    private StkItemTypeRepository itemTypeRepository;
    @Autowired
    private StkItemCategoryRepository itemCategoryRepository;
    @Autowired
    private FixtureReferentielStk fixtureReferentielStk;

    private ComDocument newEcheance(String suffix) {
        return newEcheance(suffix, null);
    }

    /**
     * Échéance d'un dossier dont le lot appartient à {@code tranche} — {@code null} pour un
     * dossier sans article, cas de la majorité des tests de ce fichier.
     */
    private ComDocument newEcheance(String suffix, CfgTranche tranche) {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-ENC-" + suffix);
        vocationRepository.save(vocation);

        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode("DOS-ENC-" + suffix);
        folder.setStkVocation(vocation);
        if (tranche != null) {
            folder.setStkItem(newArticle(suffix, vocation, tranche));
        }
        folderRepository.save(folder);

        ComDocument doc = new ComDocument();
        doc.setId(UUID.randomUUID());
        doc.setComFolder(folder);
        doc.setAmount(new BigDecimal("1000"));
        return documentRepository.save(doc);
    }

    private CfgTranche newTranche(String suffix) {
        CfgProject project = new CfgProject();
        project.setId(UUID.randomUUID());
        project.setCode("PRJ-ENC-" + suffix);
        project.setDescription("Projet " + suffix);
        projectRepository.save(project);

        CfgTranche tranche = new CfgTranche();
        tranche.setId(UUID.randomUUID());
        tranche.setCode("TR-ENC-" + suffix);
        tranche.setDescription("Tranche " + suffix);
        tranche.setCfgProject(project);
        return trancheRepository.save(tranche);
    }

    private StkItem newArticle(String suffix, StkVocation vocation, CfgTranche tranche) {
        StkItemType type = new StkItemType();
        type.setId(UUID.randomUUID());
        type.setCode("TYPE-ENC-" + suffix);
        type.setStkKind(fixtureReferentielStk.natureParDefaut());
        itemTypeRepository.save(type);

        StkItemCategory category = new StkItemCategory();
        category.setId(UUID.randomUUID());
        category.setCode("CAT-ENC-" + suffix);
        category.setStkKind(fixtureReferentielStk.natureParDefaut());
        itemCategoryRepository.save(category);

        StkItem article = new StkItem();
        article.setId(UUID.randomUUID());
        article.setCode("ART-ENC-" + suffix);
        article.setStkVocation(vocation);
        article.setStkItemType(type);
        article.setStkItemCategory(category);
        article.setStkHierarchy(tranche);
        article.setStkKind(fixtureReferentielStk.natureParDefaut());
        return itemRepository.save(article);
    }

    private StlSettlement newSettlement(String suffix, BigDecimal montant, String status, boolean comptabilise) {
        return newSettlement(suffix, montant, status, comptabilise, true);
    }

    /**
     * {@code credit} à faux produit un décaissement (remboursement) : la liste ne doit jamais le
     * renvoyer. Le défaut est vrai, qui est aussi la valeur posée par {@code EcheancierService} à
     * la création et la seule présente sur les encaissements réels.
     */
    private StlSettlement newSettlement(String suffix, BigDecimal montant, String status,
                                        boolean comptabilise, boolean credit) {
        StlSettlement settlement = new StlSettlement();
        settlement.setId(UUID.randomUUID());
        settlement.setCode("REF-ENC-" + suffix);
        settlement.setDocDate(LocalDateTime.now());
        settlement.setAmount(montant);
        settlement.setStatus(status);
        settlement.setIsAccounted(comptabilise);
        settlement.setIsCredit(credit);
        return settlementRepository.save(settlement);
    }

    private void linkToEcheance(StlSettlement settlement, ComDocument echeance, BigDecimal montantAffecte) {
        StlSettlementLine line = new StlSettlementLine();
        line.setId(UUID.randomUUID());
        line.setStlSettlement(settlement);
        line.setComDocument(echeance);
        line.setAmount(montantAffecte);
        settlementLineRepository.save(line);
    }

    @Test
    void findAll_excludesSettlementsNotLinkedToAVenteEcheance() throws Exception {
        ComDocument echeance = newEcheance("1");
        StlSettlement venteSettlement = newSettlement("1", new BigDecimal("1000"), "IN_WORK", false);
        linkToEcheance(venteSettlement, echeance, new BigDecimal("1000"));

        StlSettlement horsVente = newSettlement("2", new BigDecimal("500"), "IN_WORK", false);
        // Pas de StlSettlementLine du tout -> ne doit jamais apparaître.

        mockMvc.perform(get("/api/encaissements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + venteSettlement.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + horsVente.getId() + "')]").doesNotExist());
    }

    /**
     * Clause {@code AND IsCredit = 1} de l'écran legacy (trace du 11/09/2026). Sur les données
     * réelles une seule pièce en profitait, un remboursement de −470 000 DH qui figurait dans la
     * liste des encaissements et faussait tout total.
     */
    @Test
    void findAll_excluLeDecaissement() throws Exception {
        ComDocument echeanceCredit = newEcheance("CRED");
        StlSettlement encaissement = newSettlement("CRED", new BigDecimal("1000"), "IN_WORK", false, true);
        linkToEcheance(encaissement, echeanceCredit, new BigDecimal("1000"));

        ComDocument echeanceDebit = newEcheance("DEB");
        StlSettlement decaissement = newSettlement("DEB", new BigDecimal("-470000"), "IN_WORK", false, false);
        linkToEcheance(decaissement, echeanceDebit, new BigDecimal("-470000"));

        mockMvc.perform(get("/api/encaissements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + encaissement.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + decaissement.getId() + "')]").doesNotExist());
    }

    /**
     * Sélecteur de tranches de l'écran legacy, que la trace montre alimenté par
     * {@code StlSettlementRelationView.CfgTrancheId}. Nous l'implémentons par le chemin des lignes
     * (échéance → dossier → lot → tranche), mesuré équivalent côté Vente.
     */
    @Test
    void findAll_filtreParTranche() throws Exception {
        CfgTranche trancheA = newTranche("A");
        CfgTranche trancheB = newTranche("B");

        ComDocument echeanceA = newEcheance("TRA", trancheA);
        StlSettlement settlementA = newSettlement("TRA", new BigDecimal("100"), "IN_WORK", false);
        linkToEcheance(settlementA, echeanceA, new BigDecimal("100"));

        ComDocument echeanceB = newEcheance("TRB", trancheB);
        StlSettlement settlementB = newSettlement("TRB", new BigDecimal("100"), "IN_WORK", false);
        linkToEcheance(settlementB, echeanceB, new BigDecimal("100"));

        mockMvc.perform(get("/api/encaissements").param("trancheId", trancheA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + settlementA.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + settlementB.getId() + "')]").doesNotExist());

        // Répétable, OR — les deux tranches citées ramènent les deux règlements.
        mockMvc.perform(get("/api/encaissements")
                        .param("trancheId", trancheA.getId().toString())
                        .param("trancheId", trancheB.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + settlementA.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + settlementB.getId() + "')]").exists());
    }

    @Test
    void findAll_computesMontantBrutRestant() throws Exception {
        ComDocument echeance = newEcheance("3");
        StlSettlement settlement = newSettlement("3", new BigDecimal("1000"), "IN_WORK", false);
        linkToEcheance(settlement, echeance, new BigDecimal("400"));

        mockMvc.perform(get("/api/encaissements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + settlement.getId() + "')].montantBrut").value(Matchers.hasItem(1000)))
                .andExpect(jsonPath("$.content[?(@.id=='" + settlement.getId() + "')].montantBrutRestant").value(Matchers.hasItem(Matchers.closeTo(600.0, 0.01))));
    }

    @Test
    void findAll_filtreParEntite() throws Exception {
        CfgCompany entiteA = new CfgCompany();
        entiteA.setId(UUID.randomUUID());
        entiteA.setCode("ENT-ENC-A");
        entiteA.setDescription("Entité A");
        entiteA.setIsActive(true);
        entiteA = companyRepository.save(entiteA);

        CfgCompany entiteB = new CfgCompany();
        entiteB.setId(UUID.randomUUID());
        entiteB.setCode("ENT-ENC-B");
        entiteB.setDescription("Entité B");
        entiteB.setIsActive(true);
        entiteB = companyRepository.save(entiteB);

        ComDocument echeanceA = newEcheance("4A");
        StlSettlement settlementA = newSettlement("4A", new BigDecimal("100"), "IN_WORK", false);
        settlementA.setCfgCompany(entiteA);
        settlementRepository.save(settlementA);
        linkToEcheance(settlementA, echeanceA, new BigDecimal("100"));

        ComDocument echeanceB = newEcheance("4B");
        StlSettlement settlementB = newSettlement("4B", new BigDecimal("100"), "IN_WORK", false);
        settlementB.setCfgCompany(entiteB);
        settlementRepository.save(settlementB);
        linkToEcheance(settlementB, echeanceB, new BigDecimal("100"));

        mockMvc.perform(get("/api/encaissements").param("entiteId", entiteA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + settlementA.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + settlementB.getId() + "')]").doesNotExist());
    }

    @Test
    void findAll_filtreParEtatEtComptabilise() throws Exception {
        ComDocument echeance1 = newEcheance("5A");
        StlSettlement pointe = newSettlement("5A", new BigDecimal("100"), "POINTED", true);
        linkToEcheance(pointe, echeance1, new BigDecimal("100"));

        ComDocument echeance2 = newEcheance("5B");
        StlSettlement enCours = newSettlement("5B", new BigDecimal("100"), "IN_WORK", false);
        linkToEcheance(enCours, echeance2, new BigDecimal("100"));

        mockMvc.perform(get("/api/encaissements").param("etat", "POINTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + pointe.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + enCours.getId() + "')]").doesNotExist());

        mockMvc.perform(get("/api/encaissements").param("comptabilise", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + pointe.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id=='" + enCours.getId() + "')]").doesNotExist());
    }

    @Test
    void findAll_exposeTypeCompteDepotBanqueEtTier() throws Exception {
        StlSettlementType type = new StlSettlementType();
        type.setId(UUID.randomUUID());
        type.setCode("TYPE-ENC-1");
        type.setDescription("VIREMENT ENC TEST");
        type = settlementTypeRepository.save(type);

        StlAccount compte = new StlAccount();
        compte.setId(UUID.randomUUID());
        compte.setCode("COMPTE-ENC-1");
        compte.setDescription("Compte ENC test");
        compte = accountRepository.save(compte);

        StlDeposit depot = new StlDeposit();
        depot.setId(UUID.randomUUID());
        depot.setCode("DEPOT-ENC-1");
        depot.setDescription("CLIENT");
        depot = depositRepository.save(depot);

        CfgTier payeur = new CfgTier();
        payeur.setId(UUID.randomUUID());
        payeur.setCode("PAYEUR-ENC-1");
        payeur.setDescription("Karim Test Encaissement");
        payeur = tierRepository.save(payeur);

        ComDocument echeance = newEcheance("6");
        StlSettlement settlement = newSettlement("6", new BigDecimal("100"), "IN_WORK", false);
        settlement.setStlSettlementType(type);
        settlement.setStlAccount(compte);
        settlement.setStlDeposit(depot);
        settlement.setTier(payeur);
        settlementRepository.save(settlement);
        linkToEcheance(settlement, echeance, new BigDecimal("100"));

        mockMvc.perform(get("/api/encaissements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + settlement.getId() + "')].typeLibelle").value("VIREMENT ENC TEST"))
                .andExpect(jsonPath("$.content[?(@.id=='" + settlement.getId() + "')].compteCode").value("COMPTE-ENC-1"))
                .andExpect(jsonPath("$.content[?(@.id=='" + settlement.getId() + "')].depotCode").value("DEPOT-ENC-1"))
                .andExpect(jsonPath("$.content[?(@.id=='" + settlement.getId() + "')].tierNom").value("Karim Test Encaissement"))
                .andExpect(jsonPath("$.content[?(@.id=='" + settlement.getId() + "')].dossierCode").exists());
    }

    @Test
    void findById_returnsSameShapeAsListItem() throws Exception {
        ComDocument echeance = newEcheance("7");
        StlSettlement settlement = newSettlement("7", new BigDecimal("1000"), "IN_WORK", true);
        linkToEcheance(settlement, echeance, new BigDecimal("400"));

        mockMvc.perform(get("/api/encaissements/" + settlement.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(settlement.getId().toString()))
                .andExpect(jsonPath("$.reference").value("REF-ENC-7"))
                .andExpect(jsonPath("$.montantBrut").value(1000))
                .andExpect(jsonPath("$.montantBrutRestant").value(600))
                .andExpect(jsonPath("$.comptabilise").value(true))
                .andExpect(jsonPath("$.dossierCode").exists());
    }

    @Test
    void findById_returns404WhenUnknown() throws Exception {
        mockMvc.perform(get("/api/encaissements/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}

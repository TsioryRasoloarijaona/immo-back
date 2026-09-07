package com.tsix.immocrm.controller.encaissement;

import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.stl.StlAccount;
import com.tsix.immocrm.entities.stl.StlDeposit;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.repositories.cfg.CfgCompanyRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
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

    private ComDocument newEcheance(String suffix) {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-ENC-" + suffix);
        vocationRepository.save(vocation);

        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode("DOS-ENC-" + suffix);
        folder.setStkVocation(vocation);
        folderRepository.save(folder);

        ComDocument doc = new ComDocument();
        doc.setId(UUID.randomUUID());
        doc.setComFolder(folder);
        doc.setAmount(new BigDecimal("1000"));
        return documentRepository.save(doc);
    }

    private StlSettlement newSettlement(String suffix, BigDecimal montant, String status, boolean comptabilise) {
        StlSettlement settlement = new StlSettlement();
        settlement.setId(UUID.randomUUID());
        settlement.setCode("REF-ENC-" + suffix);
        settlement.setDocDate(LocalDateTime.now());
        settlement.setAmount(montant);
        settlement.setStatus(status);
        settlement.setIsAccounted(comptabilise);
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

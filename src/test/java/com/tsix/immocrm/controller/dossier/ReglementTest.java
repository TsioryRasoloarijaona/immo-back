package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.cfg.CfgTierType;
import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.stl.StlSettlement;
import com.tsix.immocrm.entities.stl.StlSettlementLine;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.cfg.CfgTierTypeRepository;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementLineRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementTypeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ReglementTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComFolderRepository folderRepository;

    @Autowired
    private ComDocumentRepository documentRepository;

    @Autowired
    private StkVocationRepository stkVocationRepository;

    @Autowired
    private StlSettlementRepository settlementRepository;

    @Autowired
    private StlSettlementLineRepository settlementLineRepository;

    @Autowired
    private StlSettlementTypeRepository settlementTypeRepository;

    @Autowired
    private CfgTierRepository tierRepository;

    @Autowired
    private CfgTierTypeRepository tierTypeRepository;

    private ComFolder newFolder(String code) {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-REG-" + UUID.randomUUID());
        stkVocationRepository.save(vocation);

        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode(code);
        folder.setStkVocation(vocation);
        return folderRepository.save(folder);
    }

    private ComDocument newEcheance(ComFolder folder) {
        ComDocument doc = new ComDocument();
        doc.setId(UUID.randomUUID());
        doc.setComFolder(folder);
        doc.setAmount(new java.math.BigDecimal("100000"));
        return documentRepository.save(doc);
    }

    @Test
    void addReglement_happyPath_returns201() throws Exception {
        ComFolder folder = newFolder("DOS-REG-1");
        ComDocument echeance = newEcheance(folder);

        String body = "{\"montant\":100000.00}";

        mockMvc.perform(post("/api/dossiers/{id}/echeancier/{echeanceId}/reglements", folder.getId(), echeance.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.montant").value(100000.00))
                .andExpect(jsonPath("$.echeanceId").value(echeance.getId().toString()))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void addReglement_unknownDossier_returns404() throws Exception {
        mockMvc.perform(post("/api/dossiers/{id}/echeancier/{echeanceId}/reglements", UUID.randomUUID(), UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"montant\":1000}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addReglement_unknownEcheance_returns404() throws Exception {
        ComFolder folder = newFolder("DOS-REG-2");

        mockMvc.perform(post("/api/dossiers/{id}/echeancier/{echeanceId}/reglements", folder.getId(), UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"montant\":1000}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addReglement_echeanceBelongsToDifferentDossier_returns404() throws Exception {
        ComFolder folder1 = newFolder("DOS-REG-3A");
        ComFolder folder2 = newFolder("DOS-REG-3B");
        ComDocument echeance = newEcheance(folder1);

        mockMvc.perform(post("/api/dossiers/{id}/echeancier/{echeanceId}/reglements", folder2.getId(), echeance.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"montant\":1000}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addReglement_missingMontant_returns400() throws Exception {
        ComFolder folder = newFolder("DOS-REG-4");
        ComDocument echeance = newEcheance(folder);

        mockMvc.perform(post("/api/dossiers/{id}/echeancier/{echeanceId}/reglements", folder.getId(), echeance.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addReglement_setsReferenceAndDefaultsDateEcheanceToNow() throws Exception {
        // from_front.md gap: `reference` (StlSettlement.Code) was exposed on ReglementDto but
        // never settable on creation; `dateEcheance` (StlSettlement.EcheanceDate) wasn't exposed
        // at all despite being populated on >99% of real règlements — see CreerReglementRequest.
        ComFolder folder = newFolder("DOS-REG-10");
        ComDocument echeance = newEcheance(folder);

        String body = "{\"montant\":100000.00,\"reference\":\"CHQ-REG-10\"}";

        mockMvc.perform(post("/api/dossiers/{id}/echeancier/{echeanceId}/reglements", folder.getId(), echeance.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reference").value("CHQ-REG-10"))
                .andExpect(jsonPath("$.dateEcheance").exists());
    }

    @Test
    void addReglement_typeRequiresReference_missingReference_returns400() throws Exception {
        ComFolder folder = newFolder("DOS-REG-11");
        ComDocument echeance = newEcheance(folder);

        StlSettlementType type = new StlSettlementType();
        type.setId(UUID.randomUUID());
        type.setCode("CHEQUE-TEST");
        type.setDescription("CHEQUE TEST");
        type.setIsReferenceMandatory(true);
        settlementTypeRepository.save(type);

        String body = "{\"montant\":100000.00,\"typeId\":\"" + type.getId() + "\"}";

        mockMvc.perform(post("/api/dossiers/{id}/echeancier/{echeanceId}/reglements", folder.getId(), echeance.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addReglement_setsPayeur() throws Exception {
        // payeurId was already readable (ReglementDto.payeurId/payeurNom, via StlSettlement.TierId,
        // "réglé par" — confirmed populated on >99% of real règlements) but never settable on
        // creation until now.
        ComFolder folder = newFolder("DOS-REG-13");
        ComDocument echeance = newEcheance(folder);

        CfgTier payeur = new CfgTier();
        payeur.setId(UUID.randomUUID());
        payeur.setCode("PAYEUR-REG-13");
        payeur.setDescription("Nadia BENJELLOUN");
        tierRepository.save(payeur);

        String body = "{\"montant\":100000.00,\"payeurId\":\"" + payeur.getId() + "\"}";

        mockMvc.perform(post("/api/dossiers/{id}/echeancier/{echeanceId}/reglements", folder.getId(), echeance.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.payeurId").value(payeur.getId().toString()))
                .andExpect(jsonPath("$.payeurNom").value("Nadia BENJELLOUN"));
    }

    @Test
    void addReglement_unknownPayeur_returns400() throws Exception {
        ComFolder folder = newFolder("DOS-REG-14");
        ComDocument echeance = newEcheance(folder);

        String body = "{\"montant\":100000.00,\"payeurId\":\"" + UUID.randomUUID() + "\"}";

        mockMvc.perform(post("/api/dossiers/{id}/echeancier/{echeanceId}/reglements", folder.getId(), echeance.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addReglement_typeRequiresReference_referenceProvided_returns201() throws Exception {
        ComFolder folder = newFolder("DOS-REG-12");
        ComDocument echeance = newEcheance(folder);

        StlSettlementType type = new StlSettlementType();
        type.setId(UUID.randomUUID());
        type.setCode("CHEQUE-TEST-2");
        type.setDescription("CHEQUE TEST 2");
        type.setIsReferenceMandatory(true);
        settlementTypeRepository.save(type);

        String body = "{\"montant\":100000.00,\"typeId\":\"" + type.getId() + "\",\"reference\":\"CHQ-REG-12\"}";

        mockMvc.perform(post("/api/dossiers/{id}/echeancier/{echeanceId}/reglements", folder.getId(), echeance.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reference").value("CHQ-REG-12"));
    }

    @Test
    void getReglements_returnsFullHistoryWithBankAndTypeAndPayeur() throws Exception {
        ComFolder folder = newFolder("DOS-REG-5");
        ComDocument echeance = newEcheance(folder);

        StlSettlementType type = new StlSettlementType();
        type.setId(UUID.randomUUID());
        type.setCode("VIREMENT-TEST");
        type.setDescription("VIREMENT TEST");
        type.setIsBankMandatory(true);
        settlementTypeRepository.save(type);

        CfgTierType typeBanque = new CfgTierType();
        typeBanque.setId(UUID.randomUUID());
        typeBanque.setCode("BAN-REG-TEST");
        typeBanque.setDescription("BANQUE");
        tierTypeRepository.save(typeBanque);

        CfgTier banque = new CfgTier();
        banque.setId(UUID.randomUUID());
        banque.setCode("BANQUE-REG-1");
        banque.setDescription("Banque Populaire");
        banque.setCfgTierType(typeBanque);
        tierRepository.save(banque);

        CfgTier payeur = new CfgTier();
        payeur.setId(UUID.randomUUID());
        payeur.setCode("PAYEUR-REG-1");
        payeur.setDescription("Karim ALAMI");
        tierRepository.save(payeur);

        StlSettlement settlement = new StlSettlement();
        settlement.setId(UUID.randomUUID());
        settlement.setCode("CHQ-000123");
        settlement.setDocDate(LocalDateTime.now());
        settlement.setEcheanceDate(LocalDateTime.now());
        settlement.setSettlementDate(LocalDateTime.now());
        settlement.setAmount(new java.math.BigDecimal("50000"));
        settlement.setStlSettlementType(type);
        settlement.setCfgBank(banque);
        settlement.setTier(payeur);
        settlement.setNotes("Versement partiel");
        settlementRepository.save(settlement);

        StlSettlementLine line = new StlSettlementLine();
        line.setId(UUID.randomUUID());
        line.setStlSettlement(settlement);
        line.setComDocument(echeance);
        line.setAmount(new java.math.BigDecimal("50000"));
        settlementLineRepository.save(line);

        mockMvc.perform(get("/api/dossiers/{id}/echeancier/{echeanceId}/reglements", folder.getId(), echeance.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].echeanceId").value(echeance.getId().toString()))
                .andExpect(jsonPath("$[0].reference").value("CHQ-000123"))
                .andExpect(jsonPath("$[0].dateEcheance").exists())
                .andExpect(jsonPath("$[0].dateReglement").exists())
                .andExpect(jsonPath("$[0].montant").value(50000))
                .andExpect(jsonPath("$[0].typeCode").value("VIREMENT-TEST"))
                .andExpect(jsonPath("$[0].typeLibelle").value("VIREMENT TEST"))
                .andExpect(jsonPath("$[0].banqueNom").value("Banque Populaire"))
                .andExpect(jsonPath("$[0].payeurNom").value("Karim ALAMI"))
                .andExpect(jsonPath("$[0].notes").value("Versement partiel"));
    }

    @Test
    void getReglements_noneYet_returnsEmptyArray() throws Exception {
        ComFolder folder = newFolder("DOS-REG-6");
        ComDocument echeance = newEcheance(folder);

        mockMvc.perform(get("/api/dossiers/{id}/echeancier/{echeanceId}/reglements", folder.getId(), echeance.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getReglements_unknownEcheance_returns404() throws Exception {
        ComFolder folder = newFolder("DOS-REG-7");

        mockMvc.perform(get("/api/dossiers/{id}/echeancier/{echeanceId}/reglements", folder.getId(), UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getReglements_settlementSplitAcrossEcheances_idIsSettlementPKeyAndMontantIsLineShare() throws Exception {
        // Repro of instructions/from_front.md #8: one StlSettlement split across two échéances
        // (e.g. FRAIS DOSSIER + FRAIS SYNDIC) — each ReglementDto must carry its own line's amount
        // (not the settlement's total) and an `id` that IS the StlSettlement.PKey, since that id is
        // sent as-is to GET .../documents/recu.
        ComFolder folder = newFolder("DOS-REG-9");
        ComDocument echeanceFrais = newEcheance(folder);
        ComDocument echeanceSyndic = newEcheance(folder);

        StlSettlement settlement = new StlSettlement();
        settlement.setId(UUID.randomUUID());
        settlement.setCode("VIR-000456");
        settlement.setDocDate(LocalDateTime.now());
        settlement.setSettlementDate(LocalDateTime.now());
        settlement.setAmount(new java.math.BigDecimal("16800"));
        settlementRepository.save(settlement);

        StlSettlementLine ligneFrais = new StlSettlementLine();
        ligneFrais.setId(UUID.randomUUID());
        ligneFrais.setStlSettlement(settlement);
        ligneFrais.setComDocument(echeanceFrais);
        ligneFrais.setAmount(new java.math.BigDecimal("8400"));
        settlementLineRepository.save(ligneFrais);

        StlSettlementLine ligneSyndic = new StlSettlementLine();
        ligneSyndic.setId(UUID.randomUUID());
        ligneSyndic.setStlSettlement(settlement);
        ligneSyndic.setComDocument(echeanceSyndic);
        ligneSyndic.setAmount(new java.math.BigDecimal("8400"));
        settlementLineRepository.save(ligneSyndic);

        mockMvc.perform(get("/api/dossiers/{id}/echeancier/{echeanceId}/reglements",
                        folder.getId(), echeanceFrais.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(settlement.getId().toString()))
                .andExpect(jsonPath("$[0].montant").value(8400));

        // The `id` returned above (StlSettlement.PKey) must resolve directly against the reçu
        // endpoint — previously it was the StlSettlementLine PKey instead, causing a 404
        // "Règlement introuvable" on an id copied straight out of this DTO (from_front.md #8).
        mockMvc.perform(get("/api/dossiers/{id}/reglements/{reglementId}/documents/recu",
                        folder.getId(), settlement.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void getReglements_echeanceBelongsToDifferentDossier_returns404() throws Exception {
        ComFolder folder1 = newFolder("DOS-REG-8A");
        ComFolder folder2 = newFolder("DOS-REG-8B");
        ComDocument echeance = newEcheance(folder1);

        mockMvc.perform(get("/api/dossiers/{id}/echeancier/{echeanceId}/reglements", folder2.getId(), echeance.getId()))
                .andExpect(status().isNotFound());
    }
}

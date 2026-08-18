package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.stl.StlSettlementType;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.stl.StlSettlementTypeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MontantsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComFolderRepository folderRepository;

    @Autowired
    private ComDocumentRepository documentRepository;

    @Autowired
    private StkVocationRepository stkVocationRepository;

    @Autowired
    private StlSettlementTypeRepository settlementTypeRepository;

    private ComFolder newFolder(String code) {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-MONT-" + UUID.randomUUID());
        stkVocationRepository.save(vocation);

        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode(code);
        folder.setStkVocation(vocation);
        return folderRepository.save(folder);
    }

    private StlSettlementType newType(String code) {
        StlSettlementType type = new StlSettlementType();
        type.setId(UUID.randomUUID());
        type.setCode(code);
        return settlementTypeRepository.save(type);
    }

    private void newLine(ComFolder folder, StlSettlementType type, BigDecimal montant, boolean excludeFromPrice) {
        ComDocument doc = new ComDocument();
        doc.setId(UUID.randomUUID());
        doc.setComFolder(folder);
        doc.setStlSettlementType(type);
        doc.setAmount(montant);
        doc.setExcludeFromPrice(excludeFromPrice);
        documentRepository.save(doc);
    }

    @Test
    void montants_groupsByCategoryAndExcludesFraisFromTtcTotal() throws Exception {
        ComFolder folder = newFolder("DOS-MONT-1");
        StlSettlementType avance = newType("AVANCE-TEST");
        StlSettlementType frais = newType("FRAIS-TEST");

        newLine(folder, avance, new BigDecimal("500000"), false);
        newLine(folder, avance, new BigDecimal("200000"), false);
        newLine(folder, frais, new BigDecimal("15000"), true);

        mockMvc.perform(get("/api/dossiers/{id}/montants", folder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalGeneral").value(715000))
                .andExpect(jsonPath("$.totalSoumisReglePrixTTC").value(700000))
                .andExpect(jsonPath("$.parCategorie[?(@.categorie=='AVANCE-TEST')]").exists())
                .andExpect(jsonPath("$.parCategorie[?(@.categorie=='AVANCE-TEST' && @.montant==700000 && @.nombreLignes==2)]").exists())
                .andExpect(jsonPath("$.parCategorie[?(@.categorie=='FRAIS-TEST' && @.montant==15000)]").exists());
    }

    @Test
    void montants_unknownDossier_returns404() throws Exception {
        mockMvc.perform(get("/api/dossiers/{id}/montants", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}

package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.entities.com.ComDocument;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.repositories.com.ComDocumentRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReglementTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComFolderRepository folderRepository;

    @Autowired
    private ComDocumentRepository documentRepository;

    @Autowired
    private StkVocationRepository stkVocationRepository;

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
}

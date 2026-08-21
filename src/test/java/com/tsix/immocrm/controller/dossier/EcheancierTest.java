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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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
        vocation.setCode("VOC-ECH-" + UUID.randomUUID());
        stkVocationRepository.save(vocation);

        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode(code);
        folder.setStkVocation(vocation);
        return folderRepository.save(folder);
    }

    @Test
    void findEcheances_returnsExistingLine() throws Exception {
        ComFolder folder = newFolder("DOS-ECH-1");
        ComDocument doc = new ComDocument();
        doc.setId(UUID.randomUUID());
        doc.setComFolder(folder);
        doc.setDescription("Avance");
        documentRepository.save(doc);

        mockMvc.perform(get("/api/dossiers/{id}/echeancier", folder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + doc.getId() + "')]").exists());
    }

    @Test
    void findEcheances_unknownDossier_returns404() throws Exception {
        mockMvc.perform(get("/api/dossiers/{id}/echeancier", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void addEcheance_happyPath_returns201() throws Exception {
        ComFolder folder = newFolder("DOS-ECH-2");
        String body = """
                {"montant":150000.00,"description":"Premiere echeance"}""";

        mockMvc.perform(post("/api/dossiers/{id}/echeancier", folder.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Premiere echeance"))
                .andExpect(jsonPath("$.montant").value(150000.00));
    }

    @Test
    void addEcheance_unknownDossier_returns404() throws Exception {
        String body = """
                {"montant":1000}""";

        mockMvc.perform(post("/api/dossiers/{id}/echeancier", UUID.randomUUID())
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
        documentRepository.save(doc);

        String body = """
                {"description":"Nouvelle description"}""";

        mockMvc.perform(patch("/api/dossiers/{id}/echeancier/{echeanceId}", folder.getId(), doc.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Nouvelle description"));
    }

    @Test
    void updateEcheance_belongsToDifferentDossier_returns404() throws Exception {
        ComFolder folder1 = newFolder("DOS-ECH-4A");
        ComFolder folder2 = newFolder("DOS-ECH-4B");
        ComDocument doc = new ComDocument();
        doc.setId(UUID.randomUUID());
        doc.setComFolder(folder1);
        documentRepository.save(doc);

        mockMvc.perform(patch("/api/dossiers/{id}/echeancier/{echeanceId}", folder2.getId(), doc.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateEcheance_unknownEcheance_returns404() throws Exception {
        ComFolder folder = newFolder("DOS-ECH-5");

        mockMvc.perform(patch("/api/dossiers/{id}/echeancier/{echeanceId}", folder.getId(), UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }
}

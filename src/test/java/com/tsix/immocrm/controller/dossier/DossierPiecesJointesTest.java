package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.entities.adm.AdmAttachedFile;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.repositories.adm.AdmAttachedFileRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/dossiers/{id}/pieces-jointes[/...]} — AdmAttachedFile has no declared FK to
 * ComFolder; the linkage ({@code CategoryId = ComFolder.PKey}, scoped by the "Dossier de
 * vente" category prefix) was confirmed directly against real data. This test pins that down.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DossierPiecesJointesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComFolderRepository folderRepository;

    @Autowired
    private StkVocationRepository stkVocationRepository;

    @Autowired
    private AdmAttachedFileRepository attachedFileRepository;

    private ComFolder newFolder(String code) {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-PJ-" + UUID.randomUUID());
        stkVocationRepository.save(vocation);

        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode(code);
        folder.setStkVocation(vocation);
        return folderRepository.save(folder);
    }

    private AdmAttachedFile newDossierAttachment(UUID categoryId, byte[] bytes, String extension, String description) {
        AdmAttachedFile file = new AdmAttachedFile();
        file.setId(UUID.randomUUID());
        file.setCategoryId(categoryId);
        file.setCategory("Dossier de vente N° TEST du 01/01/2026 *");
        file.setDescription(description);
        file.setExtension(extension);
        file.setAttachedFile(bytes);
        return attachedFileRepository.save(file);
    }

    @Test
    void getPiecesJointes_returnsOnlyThisDossiersAttachments() throws Exception {
        ComFolder folder = newFolder("DOS-PJ-1");
        ComFolder otherFolder = newFolder("DOS-PJ-2");

        AdmAttachedFile matching = newDossierAttachment(folder.getId(), "PDF-CONTENT".getBytes(StandardCharsets.UTF_8),
                ".pdf", "CIN CLIENT TEST");
        newDossierAttachment(otherFolder.getId(), "OTHER".getBytes(StandardCharsets.UTF_8), ".pdf", "Autre dossier");

        mockMvc.perform(get("/api/dossiers/{id}/pieces-jointes", folder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + matching.getId() + "')]").exists())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getPiecesJointes_ignoresAttachmentsOfOtherObjectTypesSharingTheCategoryId() throws Exception {
        ComFolder folder = newFolder("DOS-PJ-CATCHECK");

        // Same CategoryId as the dossier, but not a "Dossier de vente" attachment — must be
        // excluded, since CategoryId is a generic, unconstrained parent reference reused across
        // several unrelated object types in this table.
        AdmAttachedFile invoice = new AdmAttachedFile();
        invoice.setId(UUID.randomUUID());
        invoice.setCategoryId(folder.getId());
        invoice.setCategory("Yes.Stl.Debit.Invoice");
        invoice.setExtension(".pdf");
        invoice.setAttachedFile("INVOICE".getBytes(StandardCharsets.UTF_8));
        attachedFileRepository.save(invoice);

        mockMvc.perform(get("/api/dossiers/{id}/pieces-jointes", folder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getFichier_returnsBytesWithResolvedContentType() throws Exception {
        ComFolder folder = newFolder("DOS-PJ-3");
        byte[] contenu = "PDF-CONTENT".getBytes(StandardCharsets.UTF_8);
        AdmAttachedFile file = newDossierAttachment(folder.getId(), contenu, ".pdf", "Piece de test");

        mockMvc.perform(get("/api/dossiers/{id}/pieces-jointes/{pieceId}/fichier", folder.getId(), file.getId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
                .andExpect(content().bytes(contenu));
    }

    @Test
    void getFichier_wrongDossier_returns404() throws Exception {
        ComFolder folder = newFolder("DOS-PJ-4");
        ComFolder otherFolder = newFolder("DOS-PJ-5");
        AdmAttachedFile file = newDossierAttachment(folder.getId(), "X".getBytes(StandardCharsets.UTF_8), ".pdf", "Test");

        mockMvc.perform(get("/api/dossiers/{id}/pieces-jointes/{pieceId}/fichier", otherFolder.getId(), file.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPiecesJointes_unknownDossier_returns404() throws Exception {
        mockMvc.perform(get("/api/dossiers/{id}/pieces-jointes", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}

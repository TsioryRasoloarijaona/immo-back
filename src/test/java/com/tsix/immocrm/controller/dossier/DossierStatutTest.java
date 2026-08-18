package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DossierStatutTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComFolderRepository folderRepository;

    @Autowired
    private StkVocationRepository stkVocationRepository;

    @Autowired
    private StrStatusRepository strStatusRepository;

    private StkVocation newVocation() {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-STATUT-TEST-" + UUID.randomUUID());
        return stkVocationRepository.save(vocation);
    }

    private StrStatus newDossierStatus(String code) {
        StrStatus status = new StrStatus();
        status.setId(UUID.randomUUID());
        status.setCode(code);
        status.setStrConfigEntityId(com.tsix.immocrm.domain.StatutDossier.CONFIG_ENTITY_ID);
        return strStatusRepository.save(status);
    }

    @Test
    void changerStatut_validTransition_returns200() throws Exception {
        newDossierStatus("RESERVATION");
        StrStatus formalise = newDossierStatus("FORMALISE");

        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode("DOS-STATUT-VALID");
        folder.setStkVocation(newVocation());
        // Bootstrap: current status directly set to RESERVATION's row created above via lookup
        folder.setStrStatus(strStatusRepository.findByStrConfigEntityIdAndCode(
                com.tsix.immocrm.domain.StatutDossier.CONFIG_ENTITY_ID, "RESERVATION"));
        folderRepository.save(folder);

        String body = "{\"statut\":\"FORMALISE\"}";

        mockMvc.perform(patch("/api/dossiers/{id}/statut", folder.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("FORMALISE"));
    }

    @Test
    void changerStatut_invalidTransition_returns409WithAllowedList() throws Exception {
        newDossierStatus("RESERVATION");
        newDossierStatus("MINUTE");

        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode("DOS-STATUT-INVALID");
        folder.setStkVocation(newVocation());
        folder.setStrStatus(strStatusRepository.findByStrConfigEntityIdAndCode(
                com.tsix.immocrm.domain.StatutDossier.CONFIG_ENTITY_ID, "RESERVATION"));
        folderRepository.save(folder);

        // RESERVATION -> MINUTE is not in TransitionsValidees.
        String body = "{\"statut\":\"MINUTE\"}";

        mockMvc.perform(patch("/api/dossiers/{id}/statut", folder.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.transitionsAutorisees").isArray())
                .andExpect(jsonPath("$.transitionsAutorisees[?(@=='FORMALISE')]").exists())
                .andExpect(jsonPath("$.transitionsAutorisees[?(@=='MINUTE')]").doesNotExist());
    }

    @Test
    void changerStatut_unknownDossier_returns404() throws Exception {
        mockMvc.perform(patch("/api/dossiers/{id}/statut", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"statut\":\"FORMALISE\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void changerStatut_unknownStatutCode_returns400() throws Exception {
        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode("DOS-STATUT-UNKNOWN");
        folder.setStkVocation(newVocation());
        folderRepository.save(folder);

        mockMvc.perform(patch("/api/dossiers/{id}/statut", folder.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"statut\":\"NIMPORTEQUOI\"}"))
                .andExpect(status().isBadRequest());
    }
}

package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionClosingReason;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.repositories.com.ComProspectionClosingReasonRepository;
import com.tsix.immocrm.repositories.com.ComProspectionKindRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ProspectionEtapeArchiverTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComProspectionRepository prospectionRepository;

    @Autowired
    private ComProspectionKindRepository prospectionKindRepository;

    @Autowired
    private ComProspectionClosingReasonRepository closingReasonRepository;

    private ComProspection newProspection(String code) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode(code);
        return prospectionRepository.save(p);
    }

    @Test
    void changerEtape_happyPath() throws Exception {
        ComProspection prospection = newProspection("PSN-ETAPE-A");
        ComProspectionKind kind = new ComProspectionKind();
        kind.setId(UUID.randomUUID());
        kind.setCode("ETAPE-A-TEST");
        kind.setDescription("ETAPE-A-TEST");
        prospectionKindRepository.save(kind);

        String body = "{\"etapeId\":\"" + kind.getId() + "\"}";

        mockMvc.perform(patch("/api/prospections/{id}/etape", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etape").value("ETAPE-A-TEST"));
    }

    @Test
    void changerEtape_unknownProspection_returns404() throws Exception {
        String body = "{\"etapeId\":\"" + UUID.randomUUID() + "\"}";

        mockMvc.perform(patch("/api/prospections/{id}/etape", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void changerEtape_unknownEtapeId_returns400() throws Exception {
        ComProspection prospection = newProspection("PSN-ETAPE-B");
        String body = "{\"etapeId\":\"" + UUID.randomUUID() + "\"}";

        mockMvc.perform(patch("/api/prospections/{id}/etape", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void archiver_happyPath_withReasonText() throws Exception {
        ComProspection prospection = newProspection("PSN-ARCH-A");
        String body = "{\"raisonTexte\":\"Ne répond plus\"}";

        mockMvc.perform(patch("/api/prospections/{id}/archiver", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fermee").value(true))
                .andExpect(jsonPath("$.raisonFermetureTexte").value("Ne répond plus"))
                .andExpect(jsonPath("$.dateFermeture").exists());
    }

    @Test
    void archiver_happyPath_withReasonId() throws Exception {
        ComProspection prospection = newProspection("PSN-ARCH-B");
        ComProspectionClosingReason reason = new ComProspectionClosingReason();
        reason.setId(UUID.randomUUID());
        reason.setCode("RAISON-TEST");
        reason.setDescription("Raison de test");
        closingReasonRepository.save(reason);

        String body = "{\"raisonId\":\"" + reason.getId() + "\"}";

        mockMvc.perform(patch("/api/prospections/{id}/archiver", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fermee").value(true))
                .andExpect(jsonPath("$.raisonFermeture").value("Raison de test"));
    }

    @Test
    void archiver_unknownProspection_returns404() throws Exception {
        mockMvc.perform(patch("/api/prospections/{id}/archiver", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"raisonTexte\":\"x\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void archiver_missingReason_returns400() throws Exception {
        ComProspection prospection = newProspection("PSN-ARCH-C");

        mockMvc.perform(patch("/api/prospections/{id}/archiver", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}

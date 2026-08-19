package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
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
class ProspectionInteractionsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComProspectionRepository prospectionRepository;

    @Autowired
    private ComActionMessageRepository actionMessageRepository;

    private ComProspection newProspection(String code) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode(code);
        return prospectionRepository.save(p);
    }

    @Test
    void findInteractions_returnsExistingJournalEntry() throws Exception {
        ComProspection prospection = newProspection("PSN-INTER-1");

        ComActionMessage message = new ComActionMessage();
        message.setId(UUID.randomUUID());
        message.setComProspection(prospection);
        message.setDocDate(LocalDateTime.now());
        message.setDescription("Appel initial");
        message.setIsIn(true);
        actionMessageRepository.save(message);

        mockMvc.perform(get("/api/prospections/{id}/interactions", prospection.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + message.getId() + "')]").exists());
    }

    @Test
    void findInteractions_unknownProspection_returns404() throws Exception {
        mockMvc.perform(get("/api/prospections/{id}/interactions", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void addInteraction_happyPath_returns201() throws Exception {
        ComProspection prospection = newProspection("PSN-INTER-2");
        String body = """
                {"description":"Relance telephonique","entrante":false}""";

        mockMvc.perform(post("/api/prospections/{id}/interactions", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Relance telephonique"))
                .andExpect(jsonPath("$.entrante").value(false))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void addInteraction_unknownProspection_returns404() throws Exception {
        String body = """
                {"description":"x"}""";

        mockMvc.perform(post("/api/prospections/{id}/interactions", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void addInteraction_missingDescription_returns400() throws Exception {
        ComProspection prospection = newProspection("PSN-INTER-3");

        mockMvc.perform(post("/api/prospections/{id}/interactions", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}

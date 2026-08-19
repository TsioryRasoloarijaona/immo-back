package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
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

/** {@code PATCH /api/prospections/{id}} — GAP-03. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProspectionModifierTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComProspectionRepository prospectionRepository;

    @Autowired
    private StkVocationRepository vocationRepository;

    @Test
    void modifierProspection_happyPath_appliesPartialChanges() throws Exception {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-MODIF-TEST");
        vocationRepository.save(vocation);

        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-MODIF-1");
        prospection.setNotes("Notes initiales");
        prospectionRepository.save(prospection);

        String body = """
                {"notes":"Notes mises a jour","prix":250000,"vocationId":"%s"}""".formatted(vocation.getId());

        mockMvc.perform(patch("/api/prospections/{id}", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("Notes mises a jour"))
                .andExpect(jsonPath("$.prix").value(250000))
                .andExpect(jsonPath("$.vocationId").value(vocation.getId().toString()));
    }

    @Test
    void modifierProspection_unknownId_returns404() throws Exception {
        mockMvc.perform(patch("/api/prospections/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }
}

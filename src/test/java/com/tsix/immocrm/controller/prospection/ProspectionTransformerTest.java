package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ProspectionTransformerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComProspectionRepository prospectionRepository;

    @Autowired
    private CfgTierRepository tierRepository;

    private CfgTier newTier(String code) {
        CfgTier tier = new CfgTier();
        tier.setId(UUID.randomUUID());
        tier.setCode(code);
        tier.setLastName("Nom-" + code);
        return tierRepository.save(tier);
    }

    @Test
    void transformer_happyPath_assignsCommercial() throws Exception {
        CfgTier prospect = newTier("PROS-TRANS-1");
        CfgTier commercial = newTier("COMM-TRANS-1");

        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-TRANS-1");
        prospection.setCfgProspect(prospect);
        prospectionRepository.save(prospection);

        String body = "{\"commercialId\":\"" + commercial.getId() + "\"}";

        mockMvc.perform(post("/api/prospections/{id}/transformer", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commercialId").value(commercial.getId().toString()));
    }

    @Test
    void transformer_unknownProspection_returns404() throws Exception {
        String body = "{\"commercialId\":\"" + UUID.randomUUID() + "\"}";

        mockMvc.perform(post("/api/prospections/{id}/transformer", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void transformer_withoutProspect_returns400() throws Exception {
        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-TRANS-NOPROSPECT");
        prospectionRepository.save(prospection);

        String body = "{\"commercialId\":\"" + UUID.randomUUID() + "\"}";

        mockMvc.perform(post("/api/prospections/{id}/transformer", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transformer_missingCommercialId_returns400() throws Exception {
        CfgTier prospect = newTier("PROS-TRANS-2");
        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-TRANS-2");
        prospection.setCfgProspect(prospect);
        prospectionRepository.save(prospection);

        mockMvc.perform(post("/api/prospections/{id}/transformer", prospection.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}

package com.tsix.immocrm.controller.calendrier;

import com.tsix.immocrm.entities.adm.AdmEvent;
import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComActionMessageCategory;
import com.tsix.immocrm.entities.com.ComActionMessageSubCategory;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.repositories.adm.AdmEventRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageCategoryRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageSubCategoryRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class CalendrierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComProspectionRepository prospectionRepository;

    @Autowired
    private ComActionMessageRepository actionMessageRepository;

    @Autowired
    private ComActionMessageCategoryRepository actionMessageCategoryRepository;

    @Autowired
    private ComActionMessageSubCategoryRepository actionMessageSubCategoryRepository;

    @Autowired
    private AdmEventRepository admEventRepository;

    @Test
    void evenements_returnsPlannedVisiteWithinRange() throws Exception {
        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-CAL-1");
        prospectionRepository.save(prospection);

        ComActionMessageCategory category = new ComActionMessageCategory();
        category.setId(UUID.randomUUID());
        category.setCode("Prospection-TEST-" + UUID.randomUUID());
        actionMessageCategoryRepository.save(category);

        ComActionMessageSubCategory resultat = new ComActionMessageSubCategory();
        resultat.setId(UUID.randomUUID());
        resultat.setCode("Le prospect veut faire une visite du projet");
        resultat.setDescription("Le prospect veut faire une visite du projet");
        resultat.setComActionMessageCategory(category);
        actionMessageSubCategoryRepository.save(resultat);

        AdmEvent event = new AdmEvent();
        event.setId(UUID.randomUUID());
        event.setSubject("Visite du showroom");
        event.setStartHour(LocalDateTime.of(2026, 9, 15, 10, 0));
        event.setComActionMessageSubCategory(resultat);
        admEventRepository.save(event);

        ComActionMessage message = new ComActionMessage();
        message.setId(UUID.randomUUID());
        message.setComProspection(prospection);
        message.setDocDate(LocalDateTime.now());
        message.setDescription("Visite du showroom");
        message.setIsIn(true);
        message.setComActionMessageSubCategory(resultat);
        message.setAdmEvent(event);
        actionMessageRepository.save(message);

        mockMvc.perform(get("/api/calendrier/evenements")
                        .param("debut", "2026-09-01T00:00:00")
                        .param("fin", "2026-09-30T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.interactionId=='" + message.getId() + "')]").exists())
                .andExpect(jsonPath("$[?(@.interactionId=='" + message.getId() + "')].resultatCode")
                        .value("Le prospect veut faire une visite du projet"));
    }

    /** FEATURE-nom-pre-prospection-relances : pas de fiche tiers → le nom saisi sur la prospection. */
    @Test
    void evenements_preProspectionSansFiche_prospectNomEstLeTempDescription() throws Exception {
        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PSN-CAL-NOM");
        prospection.setTempDescription("Lead Calendrier");
        prospectionRepository.save(prospection);

        AdmEvent event = new AdmEvent();
        event.setId(UUID.randomUUID());
        event.setSubject("Visite lead");
        event.setStartHour(LocalDateTime.of(2026, 9, 16, 10, 0));
        admEventRepository.save(event);

        ComActionMessage message = new ComActionMessage();
        message.setId(UUID.randomUUID());
        message.setComProspection(prospection);
        message.setDocDate(LocalDateTime.now());
        message.setDescription("Visite lead");
        message.setIsIn(true);
        message.setAdmEvent(event);
        actionMessageRepository.save(message);

        mockMvc.perform(get("/api/calendrier/evenements")
                        .param("debut", "2026-09-01T00:00:00")
                        .param("fin", "2026-09-30T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.interactionId=='" + message.getId() + "')].prospectNom")
                        .value("Lead Calendrier"));
    }

    @Test
    void evenements_outsideRange_excludesEvent() throws Exception {
        mockMvc.perform(get("/api/calendrier/evenements")
                        .param("debut", "2020-01-01T00:00:00")
                        .param("fin", "2020-01-02T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void evenements_missingParams_returns400() throws Exception {
        mockMvc.perform(get("/api/calendrier/evenements"))
                .andExpect(status().isBadRequest());
    }
}

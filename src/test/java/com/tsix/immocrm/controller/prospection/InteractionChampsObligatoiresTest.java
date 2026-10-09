package com.tsix.immocrm.controller.prospection;

import com.tsix.immocrm.entities.com.ComActionMessage;
import com.tsix.immocrm.entities.com.ComActionMessageCategory;
import com.tsix.immocrm.entities.com.ComActionMessageSubCategory;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.fixtures.FixtureInteraction;
import com.tsix.immocrm.repositories.com.ComActionMessageCategoryRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageRepository;
import com.tsix.immocrm.repositories.com.ComActionMessageSubCategoryRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.services.prospection.ContexteInteraction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FEATURE-lot4 §4, règle OMRAN GEN0005 : champs obligatoires d'une interaction. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class InteractionChampsObligatoiresTest {

    private static final String DETAIL = "$.detail";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FixtureInteraction fixtureInteraction;
    @Autowired
    private ComProspectionRepository prospectionRepository;
    @Autowired
    private ComActionMessageRepository actionMessageRepository;
    @Autowired
    private ComActionMessageCategoryRepository categoryRepository;
    @Autowired
    private ComActionMessageSubCategoryRepository subCategoryRepository;

    private ComProspection prospection() {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode("PSN-GEN5-" + UUID.randomUUID());
        return prospectionRepository.save(p);
    }

    private ComActionMessageSubCategory resultat(String code, boolean avecAlerte) {
        ComActionMessageCategory categorie = new ComActionMessageCategory();
        categorie.setId(UUID.randomUUID());
        categorie.setCode("CAT-GEN5-" + UUID.randomUUID());
        categoryRepository.save(categorie);

        ComActionMessageSubCategory resultat = new ComActionMessageSubCategory();
        resultat.setId(UUID.randomUUID());
        resultat.setCode(code);
        resultat.setDescription(code);
        resultat.setComActionMessageCategory(categorie);
        resultat.setIsRevivalMandatory(avecAlerte);
        return subCategoryRepository.save(resultat);
    }

    private ResultActions poster(ComProspection p, String corps) throws Exception {
        return mockMvc.perform(post("/api/prospections/{id}/interactions", p.getId())
                .contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private UUID idCree(ResultActions resultat) throws Exception {
        String json = resultat.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(com.jayway.jsonpath.JsonPath.read(json, "$.id"));
    }

    @Test
    void sansAction_400() throws Exception {
        poster(prospection(), "{\"description\":\"appel\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString("actionTypeId")));
    }

    @Test
    void actionAutreQueNote_sansResultat_400() throws Exception {
        String corps = "{\"description\":\"appel\",\"actionTypeId\":\"%s\"}"
                .formatted(fixtureInteraction.typeTelephone().getId());

        poster(prospection(), corps)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString("résultat")));
    }

    @Test
    void note_sansMessage_400() throws Exception {
        String corps = "{\"actionTypeId\":\"%s\"}".formatted(fixtureInteraction.typeNote().getId());

        poster(prospection(), corps)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString("message")));
    }

    @Test
    void note_sansResultat_201_avecLaCategorieProspection() throws Exception {
        fixtureInteraction.categorieProspection();
        String corps = "{\"description\":\"note libre\",\"actionTypeId\":\"%s\"}"
                .formatted(fixtureInteraction.typeNote().getId());

        UUID id = idCree(poster(prospection(), corps));

        ComActionMessage ecrite = actionMessageRepository.findById(id).orElseThrow();
        assertThat(ecrite.getComActionMessageCategory()).isNotNull()
                .extracting(ComActionMessageCategory::getId).isEqualTo(ContexteInteraction.CATEGORIE_PROSPECTION);
    }

    @Test
    void resultatAvecAlerte_sansDateAlerte_400() throws Exception {
        String corps = "{\"actionTypeId\":\"%s\",\"resultatId\":\"%s\"}"
                .formatted(fixtureInteraction.typeTelephone().getId(), resultat("A rappeler", true).getId());

        poster(prospection(), corps)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(DETAIL).value(containsString("dateAlerte")));
    }

    @Test
    void resultatAvecAlerte_avecDateAlerte_201() throws Exception {
        String corps = "{\"actionTypeId\":\"%s\",\"resultatId\":\"%s\",\"dateAlerte\":\"2026-10-20T10:00:00\"}"
                .formatted(fixtureInteraction.typeTelephone().getId(), resultat("A rappeler", true).getId());

        idCree(poster(prospection(), corps));
    }

    @Test
    void actionAvecResultat_sansMessage_201_categorieDuResultat() throws Exception {
        ComActionMessageSubCategory hors = resultat("Hors Budget", false);
        String corps = "{\"actionTypeId\":\"%s\",\"resultatId\":\"%s\"}"
                .formatted(fixtureInteraction.typeTelephone().getId(), hors.getId());

        UUID id = idCree(poster(prospection(), corps));

        assertThat(actionMessageRepository.findById(id).orElseThrow().getComActionMessageCategory())
                .extracting(ComActionMessageCategory::getId)
                .isEqualTo(hors.getComActionMessageCategory().getId());
    }

    @Test
    void noteEnMinuscules_estReconnueCommeUneNote() throws Exception {
        fixtureInteraction.typeNote().setCode("note");
        String corps = "{\"description\":\"ok\",\"actionTypeId\":\"%s\"}"
                .formatted(fixtureInteraction.typeNote().getId());

        idCree(poster(prospection(), corps));
    }
}

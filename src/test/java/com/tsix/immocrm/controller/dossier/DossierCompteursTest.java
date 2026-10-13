package com.tsix.immocrm.controller.dossier;

import com.tsix.immocrm.domain.StatutDossier;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.stk.StkItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** {@code GET/PATCH /api/dossiers/{id}/compteurs} — FEATURE-vente-fiche.md §B3. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class DossierCompteursTest {

    private static final String COMPTEURS = "/api/dossiers/{id}/compteurs";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixtureDossierVente fixture;
    @Autowired private ComFolderRepository folderRepository;
    @Autowired private StkItemRepository stkItemRepository;

    @Test
    void modifierPuisLire_ecritSurLArticle_nullInchange_videEfface() throws Exception {
        ComFolder dossier = fixture.dossier("DOS-CPT", fixture.tranche("CPT-TR", true), StatutDossier.RESERVATION);
        StkItem article = dossier.getStkItem();
        article.setGasMeter("GAZ-ANCIEN");
        article.setWaterMeterPolice("POL-EAU-ANCIENNE");
        stkItemRepository.save(article);

        mockMvc.perform(patch(COMPTEURS, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"referenceSteg\":\" STEG-42 \",\"policeEau\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceSteg").value("STEG-42"))
                .andExpect(jsonPath("$.policeEau").doesNotExist())
                .andExpect(jsonPath("$.compteurGaz").value("GAZ-ANCIEN"));

        mockMvc.perform(get(COMPTEURS, dossier.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceSteg").value("STEG-42"));
        assertThat(folderRepository.findById(dossier.getId()).orElseThrow().getUserUpdateDateTime()).isNotNull();
    }

    @Test
    void modifier_champTropLong_400() throws Exception {
        ComFolder dossier = fixture.dossier("DOS-CPT2", fixture.tranche("CPT-TR2", true), StatutDossier.RESERVATION);

        mockMvc.perform(patch(COMPTEURS, dossier.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"compteurEau\":\"" + "9".repeat(51) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void lire_dossierSansArticle_409() throws Exception {
        ComFolder dossier = fixture.dossier("DOS-CPT3", fixture.tranche("CPT-TR3", true), StatutDossier.RESERVATION);
        dossier.setStkItem(null);
        folderRepository.save(dossier);

        mockMvc.perform(get(COMPTEURS, dossier.getId()))
                .andExpect(status().isConflict());
    }

    @Test
    void lire_dossierInconnu_404() throws Exception {
        mockMvc.perform(get(COMPTEURS, UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}

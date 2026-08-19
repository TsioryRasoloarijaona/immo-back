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

import java.util.Optional;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code PATCH .../statut}'s {@code date} field writes onto the target status's confirmed
 * "milestone" {@code ComFolder} column, and {@code GET .../statut/etapes-suivantes} surfaces
 * which column that is for each reachable next status. See {@code EtapeMilestone} for how the
 * mapping was confirmed against the real database (legacy printed report + status history).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DossierStatutEtapesTest {

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
        vocation.setCode("VOC-ETAPES-TEST-" + UUID.randomUUID());
        return stkVocationRepository.save(vocation);
    }

    private StrStatus newDossierStatus(String code) {
        return Optional.ofNullable(strStatusRepository.findByStrConfigEntityIdAndCode(
                        com.tsix.immocrm.domain.StatutDossier.CONFIG_ENTITY_ID, code))
                .orElseGet(() -> {
                    StrStatus status = new StrStatus();
                    status.setId(UUID.randomUUID());
                    status.setCode(code);
                    status.setStrConfigEntityId(com.tsix.immocrm.domain.StatutDossier.CONFIG_ENTITY_ID);
                    return strStatusRepository.save(status);
                });
    }

    private ComFolder newDossier(String code, String statutActuel) {
        newDossierStatus(statutActuel);
        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode(code);
        folder.setStkVocation(newVocation());
        folder.setStrStatus(strStatusRepository.findByStrConfigEntityIdAndCode(
                com.tsix.immocrm.domain.StatutDossier.CONFIG_ENTITY_ID, statutActuel));
        return folderRepository.save(folder);
    }

    @Test
    void changerStatut_versConcretisation_ecritAdvanceDate() throws Exception {
        newDossierStatus("CONCRETISATION");
        ComFolder folder = newDossier("DOS-ETAPE-CONCRET", "RESERVATION");

        String body = "{\"statut\":\"CONCRETISATION\",\"date\":\"2024-01-15T00:00:00\"}";
        mockMvc.perform(patch("/api/dossiers/{id}/statut", folder.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        ComFolder updated = folderRepository.findById(folder.getId()).orElseThrow();
        assertThat(updated.getAdvanceDate()).isEqualTo(java.time.LocalDateTime.of(2024, 1, 15, 0, 0));
    }

    @Test
    void changerStatut_versMinute_ecritAgreementSavingDate() throws Exception {
        newDossierStatus("ENVOI AU NOTAIRE");
        newDossierStatus("MINUTE");
        ComFolder folder = newDossier("DOS-ETAPE-MINUTE", "ENVOI AU NOTAIRE");

        String body = "{\"statut\":\"MINUTE\",\"date\":\"2024-02-20T00:00:00\"}";
        mockMvc.perform(patch("/api/dossiers/{id}/statut", folder.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        ComFolder updated = folderRepository.findById(folder.getId()).orElseThrow();
        assertThat(updated.getAgreementSavingDate()).isEqualTo(java.time.LocalDateTime.of(2024, 2, 20, 0, 0));
    }

    @Test
    void changerStatut_versAnnulation_ecritCancelDateEtCause() throws Exception {
        ComFolder folder = newDossier("DOS-ETAPE-ANNULATION", "RESERVATION");

        String body = "{\"statut\":\"ANNULATION\",\"date\":\"2024-03-01T00:00:00\",\"note\":\"faute de frappe, changement appt\"}";
        mockMvc.perform(patch("/api/dossiers/{id}/statut", folder.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        ComFolder updated = folderRepository.findById(folder.getId()).orElseThrow();
        assertThat(updated.getCancelDate()).isEqualTo(java.time.LocalDateTime.of(2024, 3, 1, 0, 0));
        assertThat(updated.getCancelDescription()).isEqualTo("faute de frappe, changement appt");
        // ANNULATION routes the note to CancelDescription, not the generic Notes field.
        assertThat(updated.getNotes()).isNull();
    }

    @Test
    void getEtapesSuivantes_returnsMilestoneFieldPerReachableStatus() throws Exception {
        newDossierStatus("FORMALISE");
        newDossierStatus("MINUTE");
        newDossierStatus("LIVRAISON PROVISOIRE");
        ComFolder folder = newDossier("DOS-ETAPE-SUIVANTES", "FORMALISE");

        String json = mockMvc.perform(get("/api/dossiers/{id}/statut/etapes-suivantes", folder.getId()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat((String) JsonPath.<net.minidev.json.JSONArray>read(json,
                "$[?(@.statut=='MINUTE')].champDate").get(0)).isEqualTo("agreementSavingDate");
        assertThat((String) JsonPath.<net.minidev.json.JSONArray>read(json,
                "$[?(@.statut=='MINUTE')].confiance").get(0)).isEqualTo("CONFIRME");
        assertThat((String) JsonPath.<net.minidev.json.JSONArray>read(json,
                "$[?(@.statut=='LIVRAISON PROVISOIRE')].confiance").get(0)).isEqualTo("AUCUN");
    }
}

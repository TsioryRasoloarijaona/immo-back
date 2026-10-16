package com.tsix.immocrm.controller.prospection;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionOrigin;
import com.tsix.immocrm.repositories.com.ComProspectionOriginRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.services.prospection.DiscriminantPreProspection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** {@code GET /api/pre-prospections/totaux} — voir {@code instructions/FEATURE-totaux-pre-prospections.md}. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class PreProspectionTotauxTest {

    private static final String URL_TOTAUX = "/api/pre-prospections/totaux";
    private static final String URL_LISTE = "/api/pre-prospections";
    private static final String AGREGATS = "agregats";
    private static final String SEARCH = "search";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ComProspectionRepository prospectionRepository;
    @Autowired
    private ComProspectionOriginRepository originRepository;

    private ComProspectionOrigin newOrigine(String code) {
        ComProspectionOrigin origin = new ComProspectionOrigin();
        origin.setId(UUID.randomUUID());
        origin.setCode(code);
        return originRepository.save(origin);
    }

    private ComProspection newPreProspection(String code, ComProspectionOrigin origine, BigDecimal surface) {
        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode(code);
        p.setStrEntityId(DiscriminantPreProspection.ID_PRE_PROSPECTION);
        p.setComProspectionOrigin(origine);
        p.setArea(surface);
        p.setDocDate(LocalDateTime.of(2026, 9, 16, 8, 30, 15));
        return prospectionRepository.save(p);
    }

    private long totalElementsDeLaListe(MockHttpServletRequestBuilder requete) throws Exception {
        String corps = mockMvc.perform(requete).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(corps, "$.totalElements")).longValue();
    }

    private long nombreTotalDesTotaux(MockHttpServletRequestBuilder requete) throws Exception {
        String corps = mockMvc.perform(requete).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(corps, "$.nombreTotal")).longValue();
    }

    // ------------------------------------------------------------------ R1 — mêmes lignes que la liste

    @Test
    void nombreTotal_egaleLeTotalElementsDeLaListe_avecFiltres() throws Exception {
        ComProspectionOrigin visee = newOrigine("TOTPP-ORIG-VISEE");
        ComProspectionOrigin autre = newOrigine("TOTPP-ORIG-AUTRE");
        newPreProspection("TOTPA-1", visee, null);
        newPreProspection("TOTPA-2", visee, null);
        newPreProspection("TOTPA-3", autre, null);

        long liste = totalElementsDeLaListe(get(URL_LISTE).param(SEARCH, "TOTPA-").param("origine", "TOTPP-ORIG-VISEE"));
        long totaux = nombreTotalDesTotaux(get(URL_TOTAUX).param(SEARCH, "TOTPA-").param("origine", "TOTPP-ORIG-VISEE"));

        assertThat(totaux).isEqualTo(liste).isEqualTo(2L);
    }

    @Test
    void nombreTotal_egaleLeTotalElementsDeLaListe_sansFiltre() throws Exception {
        newPreProspection("TOTPB-1", null, null);

        long liste = totalElementsDeLaListe(get(URL_LISTE));
        long totaux = nombreTotalDesTotaux(get(URL_TOTAUX));

        assertThat(totaux).isEqualTo(liste).isPositive();
    }

    @Test
    void sansAgregats_seulLeNombreTotal() throws Exception {
        newPreProspection("TOTPC-1", null, BigDecimal.TEN);
        newPreProspection("TOTPC-2", null, BigDecimal.ONE);

        mockMvc.perform(get(URL_TOTAUX).param(SEARCH, "TOTPC-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreTotal").value(2))
                .andExpect(jsonPath("$.totaux").isEmpty());
    }

    // ------------------------------------------------------------------ R3 — toutes les lignes, pas une page

    @Test
    void agregats_portentSurToutesLesLignes_pasSurLaPage() throws Exception {
        for (int i = 1; i <= 12; i++) {
            newPreProspection("TOTPD-%02d".formatted(i), null, BigDecimal.valueOf(i));
        }

        mockMvc.perform(get(URL_LISTE).param(SEARCH, "TOTPD-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(10))
                .andExpect(jsonPath("$.totalElements").value(12));

        mockMvc.perform(get(URL_TOTAUX).param(SEARCH, "TOTPD-")
                        .param(AGREGATS, "surface", "surface:AVG", "surface:MIN", "surface:MAX", "code:COUNT",
                                "dateDocument:MIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreTotal").value(12))
                .andExpect(jsonPath("$.totaux.surface").value(78.0))
                .andExpect(jsonPath("$.totaux['surface:AVG']").value(6.5))
                .andExpect(jsonPath("$.totaux['surface:MIN']").value(1.0))
                .andExpect(jsonPath("$.totaux['surface:MAX']").value(12.0))
                .andExpect(jsonPath("$.totaux['code:COUNT']").value(12))
                .andExpect(jsonPath("$.totaux['dateDocument:MIN']").value("2026-09-16T08:30:15"));
    }

    @Test
    void extremumEtMoyenne_surColonneVide_rendentNull() throws Exception {
        newPreProspection("TOTPE-1", null, null);
        newPreProspection("TOTPE-2", null, null);

        mockMvc.perform(get(URL_TOTAUX).param(SEARCH, "TOTPE-")
                        .param(AGREGATS, "surface:MIN", "surface:MAX", "surface:AVG", "surface:COUNT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreTotal").value(2))
                .andExpect(jsonPath("$.totaux['surface:MIN']").value(nullValue()))
                .andExpect(jsonPath("$.totaux['surface:MAX']").value(nullValue()))
                .andExpect(jsonPath("$.totaux['surface:AVG']").value(nullValue()))
                .andExpect(jsonPath("$.totaux['surface:COUNT']").value(0));
    }

    // ------------------------------------------------------------------ R4 / R5 — jetons refusés

    @Test
    void sommeSurUneDate_rend400() throws Exception {
        mockMvc.perform(get(URL_TOTAUX).param(AGREGATS, "dateDocument:SUM"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nombreSurUnTexte_rend200() throws Exception {
        newPreProspection("TOTPF-1", null, null);

        mockMvc.perform(get(URL_TOTAUX).param(SEARCH, "TOTPF-").param(AGREGATS, "origine:COUNT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totaux['origine:COUNT']").value(0));
    }

    @Test
    void jetonInconnu_rend400() throws Exception {
        mockMvc.perform(get(URL_TOTAUX).param(AGREGATS, "colonneQuiNExistePas"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(URL_TOTAUX).param(AGREGATS, "surface:MEDIANE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void colonneCalculeeParPage_rend400() throws Exception {
        mockMvc.perform(get(URL_TOTAUX).param(AGREGATS, "joursInactivite"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(URL_TOTAUX).param(AGREGATS, "statutDossier:COUNT"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nbVisites_estAccepteeCommeDansLesGroupes() throws Exception {
        newPreProspection("TOTPG-1", null, null);

        mockMvc.perform(get(URL_TOTAUX).param(SEARCH, "TOTPG-").param(AGREGATS, "nbVisites", "derniereVisiteDate:MAX"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreTotal").value(1))
                .andExpect(jsonPath("$.totaux.nbVisites").value(0));
    }
}

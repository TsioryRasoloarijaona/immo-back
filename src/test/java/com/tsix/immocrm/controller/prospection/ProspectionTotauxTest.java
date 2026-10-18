package com.tsix.immocrm.controller.prospection;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.com.ComProspectionKind;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComProspectionKindRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import org.hamcrest.Matchers;
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

/** {@code GET /api/prospections/totaux-colonnes} — voir {@code instructions/FEATURE-totaux-listes.md}. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ProspectionTotauxTest {

    private static final String URL_TOTAUX = "/api/prospections/totaux-colonnes";
    private static final String URL_LISTE = "/api/prospections";
    private static final String AGREGATS = "agregats";
    private static final String SEARCH = "search";
    private static final String NOMBRE_TOTAL = "$.nombreTotal";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ComProspectionRepository prospectionRepository;
    @Autowired
    private ComProspectionKindRepository kindRepository;
    @Autowired
    private CfgTierRepository tierRepository;

    private ComProspectionKind newEtape(String code) {
        ComProspectionKind kind = new ComProspectionKind();
        kind.setId(UUID.randomUUID());
        kind.setCode(code);
        return kindRepository.save(kind);
    }

    /** Qualifiée : porte un prospect, donc relève de {@code /api/prospections}. */
    private ComProspection newProspection(String code, ComProspectionKind etape, Boolean ancienClient,
                                          BigDecimal surface) {
        CfgTier prospect = new CfgTier();
        prospect.setId(UUID.randomUUID());
        prospect.setCode("TIER-" + code);
        prospect.setLastName("Nom-" + code);
        prospect = tierRepository.save(prospect);

        ComProspection p = new ComProspection();
        p.setId(UUID.randomUUID());
        p.setCode(code);
        p.setComProspectionKind(etape);
        p.setCfgProspect(prospect);
        p.setIsOldCustomer(ancienClient);
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
        return ((Number) JsonPath.read(corps, NOMBRE_TOTAL)).longValue();
    }

    // ------------------------------------------------------------------ R1 — mêmes lignes que la liste

    @Test
    void r1_nombreTotal_egaleLeTotalElementsDeLaListe_avecFiltres() throws Exception {
        ComProspectionKind visee = newEtape("TCP-ETAPE-VISEE");
        ComProspectionKind autre = newEtape("TCP-ETAPE-AUTRE");
        newProspection("TCPA-1", visee, null, null);
        newProspection("TCPA-2", visee, null, null);
        newProspection("TCPA-3", autre, null, null);
        String etape = visee.getCode();

        long liste = totalElementsDeLaListe(get(URL_LISTE).param(SEARCH, "TCPA-").param("etape", etape));
        assertThat(liste).isEqualTo(2L);
        long totaux = nombreTotalDesTotaux(get(URL_TOTAUX).param(SEARCH, "TCPA-").param("etape", etape));

        assertThat(totaux).isEqualTo(liste).isEqualTo(2L);
    }

    /** R1 + « filtres avancés » : le {@code @ModelAttribute FiltresAvancesProspection} fait partie du filtre. */
    @Test
    void r1_nombreTotal_suitLesFiltresAvances() throws Exception {
        newProspection("TCPB-1", null, true, BigDecimal.TEN);
        newProspection("TCPB-2", null, true, BigDecimal.ONE);
        newProspection("TCPB-3", null, false, new BigDecimal("100"));

        long liste = totalElementsDeLaListe(get(URL_LISTE).param(SEARCH, "TCPB-").param("ancienClient", "true"));
        assertThat(liste).isEqualTo(2L);

        mockMvc.perform(get(URL_TOTAUX).param(SEARCH, "TCPB-").param("ancienClient", "true")
                        .param(AGREGATS, "surface"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(NOMBRE_TOTAL).value(liste))
                .andExpect(jsonPath(NOMBRE_TOTAL).value(2))
                .andExpect(jsonPath("$.totaux.surface").value(11.0));
    }

    @Test
    void r1_nombreTotal_egaleLeTotalElementsDeLaListe_sansFiltre() throws Exception {
        newProspection("TCPC-1", null, null, null);

        long liste = totalElementsDeLaListe(get(URL_LISTE));
        long totaux = nombreTotalDesTotaux(get(URL_TOTAUX));

        assertThat(totaux).isEqualTo(liste).isPositive();
    }

    @Test
    void sansAgregats_seulLeNombreTotal() throws Exception {
        newProspection("TCPD-1", null, null, BigDecimal.TEN);
        newProspection("TCPD-2", null, null, BigDecimal.ONE);

        mockMvc.perform(get(URL_TOTAUX).param(SEARCH, "TCPD-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(NOMBRE_TOTAL).value(2))
                .andExpect(jsonPath("$.totaux").isEmpty());
    }

    // ------------------------------------------------------------------ R3 — toutes les lignes, pas une page

    @Test
    void r3_agregats_portentSurToutesLesLignes_pasSurLaPage() throws Exception {
        for (int i = 1; i <= 12; i++) {
            newProspection("TCPE-%02d".formatted(i), null, null, BigDecimal.valueOf(i));
        }

        mockMvc.perform(get(URL_LISTE).param(SEARCH, "TCPE-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(10))
                .andExpect(jsonPath("$.totalElements").value(12));

        mockMvc.perform(get(URL_TOTAUX).param(SEARCH, "TCPE-")
                        .param(AGREGATS, "surface", "surface:AVG", "surface:MIN", "surface:MAX", "code:COUNT",
                                "dateDocument:MIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(NOMBRE_TOTAL).value(12))
                .andExpect(jsonPath("$.totaux.surface").value(78.0))
                .andExpect(jsonPath("$.totaux['surface:AVG']").value(6.5))
                .andExpect(jsonPath("$.totaux['surface:MIN']").value(1.0))
                .andExpect(jsonPath("$.totaux['surface:MAX']").value(12.0))
                .andExpect(jsonPath("$.totaux['code:COUNT']").value(12))
                .andExpect(jsonPath("$.totaux['dateDocument:MIN']").value(Matchers.startsWith("2026-09-16")));
    }

    @Test
    void extremumEtMoyenne_surColonneVide_rendentNull() throws Exception {
        newProspection("TCPF-1", null, null, null);
        newProspection("TCPF-2", null, null, null);

        mockMvc.perform(get(URL_TOTAUX).param(SEARCH, "TCPF-")
                        .param(AGREGATS, "surface:MIN", "surface:MAX", "surface:AVG", "surface:COUNT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(NOMBRE_TOTAL).value(2))
                .andExpect(jsonPath("$.totaux['surface:MIN']").value(nullValue()))
                .andExpect(jsonPath("$.totaux['surface:MAX']").value(nullValue()))
                .andExpect(jsonPath("$.totaux['surface:AVG']").value(nullValue()))
                .andExpect(jsonPath("$.totaux['surface:COUNT']").value(0));
    }

    // ------------------------------------------------------------------ R4 / R5 — jetons refusés

    @Test
    void r4_sommeSurUneDate_rend400() throws Exception {
        mockMvc.perform(get(URL_TOTAUX).param(AGREGATS, "dateDocument:SUM"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void r4_nombreSurUnTexte_rend200() throws Exception {
        newProspection("TCPG-1", newEtape("TCP-ETAPE-COUNT"), null, null);

        mockMvc.perform(get(URL_TOTAUX).param(SEARCH, "TCPG-").param(AGREGATS, "etape:COUNT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totaux['etape:COUNT']").value(1));
    }

    @Test
    void jetonInconnu_rend400() throws Exception {
        mockMvc.perform(get(URL_TOTAUX).param(AGREGATS, "colonneQuiNExistePas"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(URL_TOTAUX).param(AGREGATS, "surface:MEDIANE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fonctionsValides", Matchers.hasItems("SUM", "AVG", "MIN", "MAX", "COUNT")));
    }

    @Test
    void r5_colonneSansChemin_rend400() throws Exception {
        mockMvc.perform(get(URL_TOTAUX).param(AGREGATS, "joursInactivite"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(URL_TOTAUX).param(AGREGATS, "statutDossier:COUNT"))
                .andExpect(status().isBadRequest());
    }
}

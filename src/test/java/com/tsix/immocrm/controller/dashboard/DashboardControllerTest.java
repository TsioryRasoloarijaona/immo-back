package com.tsix.immocrm.controller.dashboard;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.cfg.CfgTier;
import com.tsix.immocrm.entities.com.ComFolder;
import com.tsix.immocrm.entities.com.ComProspection;
import com.tsix.immocrm.entities.stk.StkVocation;
import com.tsix.immocrm.entities.str.StrStatus;
import com.tsix.immocrm.repositories.cfg.CfgTierRepository;
import com.tsix.immocrm.repositories.com.ComFolderRepository;
import com.tsix.immocrm.repositories.com.ComProspectionRepository;
import com.tsix.immocrm.repositories.stk.StkVocationRepository;
import com.tsix.immocrm.repositories.str.StrStatusRepository;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComFolderRepository folderRepository;

    @Autowired
    private StkVocationRepository stkVocationRepository;

    @Autowired
    private StrStatusRepository strStatusRepository;

    @Autowired
    private ComProspectionRepository prospectionRepository;

    @Autowired
    private CfgTierRepository tierRepository;

    private StkVocation newVocation() {
        StkVocation vocation = new StkVocation();
        vocation.setId(UUID.randomUUID());
        vocation.setCode("VOC-DASH-" + UUID.randomUUID());
        return stkVocationRepository.save(vocation);
    }

    private StrStatus newStatus(String code) {
        StrStatus status = new StrStatus();
        status.setId(UUID.randomUUID());
        status.setCode(code);
        status.setStrConfigEntityId(com.tsix.immocrm.domain.StatutDossier.CONFIG_ENTITY_ID);
        return strStatusRepository.save(status);
    }

    private String getDashboardJson() throws Exception {
        return mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void dashboard_activeStatutIncludedInPipeline_exitStatutExcluded() throws Exception {
        BigDecimal pipelineBefore = new BigDecimal(JsonPath.parse(getDashboardJson()).read("$.valeurPipeline").toString());

        StrStatus reservation = newStatus("RESERVATION");
        ComFolder actif = new ComFolder();
        actif.setId(UUID.randomUUID());
        actif.setCode("DOS-DASH-ACTIF");
        actif.setStkVocation(newVocation());
        actif.setStrStatus(reservation);
        actif.setTotalToPay(new BigDecimal("500000"));
        folderRepository.save(actif);

        StrStatus annulation = newStatus("ANNULATION");
        ComFolder annule = new ComFolder();
        annule.setId(UUID.randomUUID());
        annule.setCode("DOS-DASH-ANNULE");
        annule.setStkVocation(newVocation());
        annule.setStrStatus(annulation);
        annule.setTotalToPay(new BigDecimal("999999"));
        folderRepository.save(annule);

        BigDecimal pipelineAfter = new BigDecimal(JsonPath.parse(getDashboardJson()).read("$.valeurPipeline").toString());

        // Only the RESERVATION (active) dossier's amount should have been added; the
        // ANNULATION (exit) dossier's amount must be excluded.
        assertThat(pipelineAfter.subtract(pipelineBefore)).isEqualByComparingTo("500000");
    }

    @Test
    void dashboard_dossiersParEtape_countsIncreaseForUsedStatut() throws Exception {
        long countBefore = countForEtape(getDashboardJson(), "FORMALISE");

        StrStatus formalise = newStatus("FORMALISE");
        ComFolder folder = new ComFolder();
        folder.setId(UUID.randomUUID());
        folder.setCode("DOS-DASH-FORMALISE");
        folder.setStkVocation(newVocation());
        folder.setStrStatus(formalise);
        folderRepository.save(folder);

        long countAfter = countForEtape(getDashboardJson(), "FORMALISE");

        assertThat(countAfter).isEqualTo(countBefore + 1);
    }

    @Test
    void dashboard_overdueRevival_addsDossierToRelancesEnRetard() throws Exception {
        JSONArray before = JsonPath.parse(getDashboardJson()).read("$.relancesEnRetard");
        int countBefore = before.size();

        UUID folderId = UUID.randomUUID();
        ComFolder folder = new ComFolder();
        folder.setId(folderId);
        folder.setCode("DOS-DASH-RELANCE");
        folder.setStkVocation(newVocation());
        folder.setRevival1(LocalDateTime.now().minusDays(10));
        folder.setRevival1ValidityDate(LocalDateTime.now().minusDays(3));
        folderRepository.save(folder);

        String json = getDashboardJson();
        JSONArray after = JsonPath.parse(json).read("$.relancesEnRetard");
        assertThat(after).hasSize(countBefore + 1);

        JSONArray match = JsonPath.parse(json)
                .read("$.relancesEnRetard[?(@.dossierId=='" + folderId + "')]");
        assertThat(match).hasSize(1);
        assertThat(((Number) ((java.util.Map<?, ?>) match.get(0)).get("joursRetard")).longValue())
                .isEqualTo(3L);
    }

    @Test
    void dashboard_prospectionCounts_increaseForRealProspection() throws Exception {
        Long ouvertesBefore = JsonPath.parse(getDashboardJson()).read("$.prospectionsOuvertes", Long.class);
        Long totalBefore = JsonPath.parse(getDashboardJson()).read("$.prospectionsTotal", Long.class);

        CfgTier commercial = new CfgTier();
        commercial.setId(UUID.randomUUID());
        commercial.setCode("COMM-DASH-" + UUID.randomUUID());
        tierRepository.save(commercial);

        ComProspection prospection = new ComProspection();
        prospection.setId(UUID.randomUUID());
        prospection.setCode("PROS-DASH-TEST");
        prospection.setCfgCommercial(commercial);
        prospection.setIsClosed(false);
        prospectionRepository.save(prospection);

        Long ouvertesAfter = JsonPath.parse(getDashboardJson()).read("$.prospectionsOuvertes", Long.class);
        Long totalAfter = JsonPath.parse(getDashboardJson()).read("$.prospectionsTotal", Long.class);

        assertThat(ouvertesAfter).isEqualTo(ouvertesBefore + 1);
        assertThat(totalAfter).isEqualTo(totalBefore + 1);
    }

    private static long countForEtape(String dashboardJson, String etape) {
        JSONArray matches = JsonPath.parse(dashboardJson)
                .read("$.dossiersParEtape[?(@.etape=='" + etape + "')].nombre");
        return matches.isEmpty() ? 0L : ((Number) matches.get(0)).longValue();
    }
}

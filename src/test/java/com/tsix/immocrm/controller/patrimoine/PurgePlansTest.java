package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.entities.stk.StkFloor;
import com.tsix.immocrm.entities.stk.StkItem;
import com.tsix.immocrm.fixtures.FixtureArticleFiche;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixturePlans;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * « Puger les plans » — FEATURE-patrimoine-plans.md §2 ({@code DELETE /api/tranches/{id}/plans} : tracés puis plans de
 * la tranche, 204) et piège 1 (un étage est partagé entre tranches : la purge ne touche pas les autres).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PurgePlansTest {

    private static final String PURGE = "/api/tranches/{id}/plans";
    private static final String D = "M 0,0 h 10 v 10 z";
    private static final String PLANS = "SELECT COUNT(*) FROM StkHierarchyPlan WHERE StkHierarchyId = :id";
    private static final String TRACES = """
            SELECT COUNT(*) FROM StkHierarchyPath p JOIN StkItem i ON i.PKey = p.StkItemId
            WHERE i.StkHierarchyId = :id""";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixturePlans plans;
    @Autowired private FixtureArticleFiche lecture;

    private AdmUser admin;
    private String jeton;
    private CfgTranche tranche;
    private CfgTranche voisine;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        jeton = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        tranche = plans.tranche("PUR-" + jeton, true);
        voisine = plans.tranche("PUR-V-" + jeton, true);
        StkFloor etage1 = plans.etage("PE1-" + jeton);
        StkFloor etage2 = plans.etage("PE2-" + jeton);

        StkItem a = plans.article(tranche, "PA-" + jeton, false, etage1);
        StkItem b = plans.article(tranche, "PB-" + jeton, false, etage2);
        plans.trace(a.getId(), etage1.getId(), D);
        plans.trace(b.getId(), etage2.getId(), D);
        StkItem v = plans.article(voisine, "PV-" + jeton, false, etage1);
        plans.trace(v.getId(), etage1.getId(), D);

        plans.plan(tranche.getId(), etage1.getId(), "Etage 1", FixturePlans.dataUriPng());
        plans.plan(tranche.getId(), etage2.getId(), "Etage 2", FixturePlans.dataUriPng());
        plans.plan(voisine.getId(), etage1.getId(), "Etage 1", FixturePlans.dataUriPng());
    }

    @Test
    void r2_purge_204_supprimeTracesEtPlansDeLaTranche() throws Exception {
        mockMvc.perform(delete(PURGE, tranche.getId()).with(connecte(admin)))
                .andExpect(status().isNoContent());

        assertAll(
                () -> assertThat(lecture.compter(PLANS, tranche.getId())).as("plans").isZero(),
                () -> assertThat(lecture.compter(TRACES, tranche.getId())).as("tracés").isZero());
    }

    @Test
    void piege1_purge_gardeLesPlansEtTracesDUneAutreTrancheSurLeMemeEtage() throws Exception {
        mockMvc.perform(delete(PURGE, tranche.getId()).with(connecte(admin)))
                .andExpect(status().isNoContent());

        assertAll(
                () -> assertThat(lecture.compter(PLANS, voisine.getId())).as("plans voisins").isEqualTo(1),
                () -> assertThat(lecture.compter(TRACES, voisine.getId())).as("tracés voisins").isEqualTo(1));
    }
}

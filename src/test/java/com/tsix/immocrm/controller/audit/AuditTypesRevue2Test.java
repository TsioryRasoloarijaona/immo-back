package com.tsix.immocrm.controller.audit;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmWebHistorics;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.adm.AdmWebHistoricsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixturePatrimoine.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Revue 2 FEATURE-patrimoine-audit-actions.md §10 point 1 : aucune ligne dont l'{@code ObjectType} est hors registre,
 * même rattachée (ParentObjectType du registre) à une fiche lisible.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuditTypesRevue2Test {

    private static final String SECRET = "DETAIL-ETRANGER-REVUE2";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private AdmWebHistoricsRepository repository;

    @Test
    void enfantDeTypeEtranger_sousParentDuRegistre_jamaisServi() throws Exception {
        AdmUser lecteur = fixture.utilisateur("LECT-AUDIT-R2");
        UUID tranche = fixture.tranche("TR-AUDIT-R2",
                fixture.projet("PRJ-AUDIT-R2", fixture.entite("ENT-AUDIT-R2", true))).getId();

        AdmWebHistorics h = new AdmWebHistorics();
        h.setId(UUID.randomUUID());
        h.setObjectId(UUID.randomUUID());
        h.setObjectType("Yes.Com.Prospection.Prospection");
        h.setParentObjectId(tranche);
        h.setParentObjectType("Yes.Cfg.CfgTranche");
        h.setActionDateTime(LocalDateTime.of(2026, 10, 1, 9, 0));
        h.setAction("Modified");
        h.setObjectTypeLabel("Prospection");
        h.setObjectLabel("PSN-R2");
        h.setDescription("PSN-R2");
        h.setDetails("[[\"BudgetMax\",\"\",\"" + SECRET + "\"]]");
        repository.save(h);

        String corps = mockMvc.perform(get("/api/audit").param("objetId", tranche.toString())
                        .with(connecte(lecteur)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(corps)
                .as("ligne d'ObjectType hors registre servie via son ParentObjectType")
                .doesNotContain(SECRET)
                .doesNotContain("Yes.Com.Prospection.Prospection");
    }
}

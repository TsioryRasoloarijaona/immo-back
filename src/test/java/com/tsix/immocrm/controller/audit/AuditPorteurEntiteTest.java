package com.tsix.immocrm.controller.audit;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.fixtures.FixtureAudit;
import com.tsix.immocrm.fixtures.FixtureAudit.LigneAudit;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FEATURE-entites-juridiques.md §7.7 : nouveau porteur {@code ENTITE} de {@code GET /api/audit} (onglet Audit de la
 * fiche entité) ; la ligne de répartition ({@code CfgCompanyShareholder}) a la société pour parent.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuditPorteurEntiteTest {

    private static final String TYPE_ENTITE = "Yes.Cfg.CfgCompany";
    private static final String TYPE_REPARTITION = "Yes.Cfg.CfgCompanyShareholder";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureAudit audit;

    @Test
    void r7_7_auditDUneEntite_200_sesLignesEtCellesDeSaRepartition() throws Exception {
        AdmUser admin = fixture.administrateur();
        String jeton = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        CfgCompany entite = fixture.entite("ENT-AUDP-" + jeton, true);
        UUID ajout = audit.ligne(entite.getId(), TYPE_ENTITE, "Entité juridique", "Added",
                LocalDateTime.of(2026, 1, 5, 10, 0), "[[\"Code\",\"\",\"ENT\"]]");
        UUID enfant = audit.ligne(new LigneAudit(UUID.randomUUID(), TYPE_REPARTITION, "Répartition", TYPE_ENTITE,
                entite.getId(), "Added", LocalDateTime.of(2026, 1, 6, 10, 0), "[]"));

        mockMvc.perform(get("/api/audit").param("objetId", entite.getId().toString()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id").value(containsInAnyOrder(ajout.toString(), enfant.toString())));
    }
}

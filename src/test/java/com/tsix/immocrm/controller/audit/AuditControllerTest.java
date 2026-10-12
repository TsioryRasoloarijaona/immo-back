package com.tsix.immocrm.controller.audit;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.adm.AdmWebHistorics;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.repositories.adm.AdmWebHistoricsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.FixturePatrimoine.connecte;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FEATURE-patrimoine-audit-actions.md §2.3 et §8. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuditControllerTest {

    private static final String URL = "/api/audit";
    private static final String OBJET_ID = "objetId";
    private static final LocalDateTime T0 = LocalDateTime.of(2026, 10, 1, 9, 0);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private AdmWebHistoricsRepository repository;

    private AdmUser lecteur;
    private final UUID tranche = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lecteur = fixture.utilisateur("LECT-AUDIT");
    }

    private AdmWebHistorics ligne(UUID objetId, UUID parentId, LocalDateTime date, String libelle, String details) {
        AdmWebHistorics h = new AdmWebHistorics();
        h.setId(UUID.randomUUID());
        h.setObjectId(objetId);
        h.setParentObjectId(parentId);
        h.setParentObjectType(parentId == null ? null : "Yes.Cfg.CfgTranche");
        h.setActionDateTime(date);
        h.setAction("Modified");
        h.setObjectType("Yes.Cfg.CfgTranche");
        h.setObjectTypeLabel("Tranche");
        h.setObjectLabel(libelle);
        h.setDescription(libelle);
        h.setUserDescription("Khadija ELKORRI");
        h.setDetails(details);
        return repository.save(h);
    }

    @Test
    void objetEtLignesEnfants_plusRecentesDAbord() throws Exception {
        ligne(tranche, null, T0, "ancienne", "[[\"Code\",\"A\",\"B\"]]");
        ligne(UUID.randomUUID(), tranche, T0.plusDays(1), "terrain", "[]");
        ligne(tranche, null, T0.plusDays(2), "recente", "[[\"Description\",\"x\",\"y\",\"Stk.Description\"]]");
        ligne(UUID.randomUUID(), null, T0.plusDays(3), "autre objet", "[]");

        mockMvc.perform(get(URL).param(OBJET_ID, tranche.toString()).with(connecte(lecteur)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[*].libelleObjet").value(contains("recente", "terrain", "ancienne")))
                .andExpect(jsonPath("$.content[0].utilisateur").value("Khadija ELKORRI"))
                .andExpect(jsonPath("$.content[0].action").value("Modified"))
                .andExpect(jsonPath("$.content[0].details[0].propriete").value("Description"))
                .andExpect(jsonPath("$.content[0].details[0].nouvelleValeur").value("y"))
                .andExpect(jsonPath("$.content[1].parentTypeObjet").value("Yes.Cfg.CfgTranche"));
    }

    @Test
    void pagination() throws Exception {
        for (int i = 0; i < 3; i++) {
            ligne(tranche, null, T0.plusHours(i), "l" + i, "[]");
        }

        mockMvc.perform(get(URL).param(OBJET_ID, tranche.toString()).param("page", "1").param("size", "2")
                        .with(connecte(lecteur)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].libelleObjet").value("l0"));
    }

    @Test
    void detailsIllisibles_listeVideSansErreur() throws Exception {
        ligne(tranche, null, T0, "illisible", "pas du json");

        mockMvc.perform(get(URL).param(OBJET_ID, tranche.toString()).with(connecte(lecteur)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].details", hasSize(0)));
    }

    @Test
    void objetSansLigne_pageVide() throws Exception {
        mockMvc.perform(get(URL).param(OBJET_ID, UUID.randomUUID().toString()).with(connecte(lecteur)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void objetIdAbsent_400() throws Exception {
        mockMvc.perform(get(URL).with(connecte(lecteur))).andExpect(status().isBadRequest());
    }

    @Test
    void objetIdMalForme_400() throws Exception {
        mockMvc.perform(get(URL).param(OBJET_ID, "pas-un-uuid").with(connecte(lecteur)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tailleHorsBornes_400() throws Exception {
        mockMvc.perform(get(URL).param(OBJET_ID, tranche.toString()).param("size", "0").with(connecte(lecteur)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonAuthentifie_401() throws Exception {
        mockMvc.perform(get(URL).param(OBJET_ID, tranche.toString())).andExpect(status().isUnauthorized());
    }
}

package com.tsix.immocrm.controller.audit;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixtureAudit;
import com.tsix.immocrm.fixtures.FixtureAudit.LigneAudit;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureAudit.TYPE_PROGRAMME;
import static com.tsix.immocrm.fixtures.FixtureAudit.TYPE_TRANCHE;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Revue 1 §10.1 (fuite) — FEATURE-patrimoine-audit-actions.md : le journal ne rend que les types du registre, pour un
 * objet patrimoine visible par l'appelant ; inconnu ou invisible → 404. Ne pas affaiblir.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuditPerimetreRevueTest {

    private static final String AUDIT = "/api/audit";
    private static final String OBJET_ID = "objetId";
    private static final String TYPE_HORS_REGISTRE = "Yes.Com.ComFolder";
    private static final String LIBELLE_PROGRAMME = "Programme immobilier";
    private static final String DETAILS_VIDES = "[]";
    private static final LocalDateTime JANVIER = LocalDateTime.of(2026, 1, 5, 8, 0);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureAudit audit;

    private AdmUser admin;
    private CfgProject projet;

    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        projet = fixture.projet("PRJ-AUD-PERIM", fixture.entite("ENT-AUD-PERIM", true));
    }

    /** Une prospection, une facture… : des lignes existent, mais l'objet n'est pas du patrimoine. */
    @Test
    void r10_1_objetHorsRegistre_404() throws Exception {
        UUID prospection = UUID.randomUUID();
        audit.ligne(prospection, "Yes.Com.ComProspection", "Prospection", "Modified", JANVIER,
                "[[\"Notes\",\"a\",\"b\"]]");

        mockMvc.perform(get(AUDIT).param(OBJET_ID, prospection.toString()).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void r10_1_objetInconnu_404() throws Exception {
        mockMvc.perform(get(AUDIT).param(OBJET_ID, UUID.randomUUID().toString()).with(connecte(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void r10_1_objetVisibleSansLigne_200Vide() throws Exception {
        mockMvc.perform(get(AUDIT).param(OBJET_ID, projet.getId().toString()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    /** Ligne forgée sur l'id d'un programme visible mais d'un type hors registre : jamais rendue. */
    @Test
    void r10_1_ligneDUnTypeHorsRegistre_surUnObjetVisible_nonRendue() throws Exception {
        UUID legitime = audit.ligne(projet.getId(), TYPE_PROGRAMME, LIBELLE_PROGRAMME, "Added", JANVIER, DETAILS_VIDES);
        audit.ligne(projet.getId(), TYPE_HORS_REGISTRE, "Dossier de vente", "Modified", JANVIER.plusDays(1),
                DETAILS_VIDES);

        mockMvc.perform(get(AUDIT).param(OBJET_ID, projet.getId().toString()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[*].id").value(contains(legitime.toString())));
    }

    /** Ligne enfant dont le parent est l'objet visible mais dont les types sont hors registre : jamais rendue. */
    @Test
    void r10_1_ligneEnfantHorsRegistre_nonRendue() throws Exception {
        UUID legitime = audit.ligne(projet.getId(), TYPE_PROGRAMME, LIBELLE_PROGRAMME, "Added", JANVIER, DETAILS_VIDES);
        audit.ligne(new LigneAudit(UUID.randomUUID(), "Yes.Com.ComFolderItem", "Article du dossier",
                TYPE_HORS_REGISTRE, projet.getId(), "Added", JANVIER.plusDays(2), DETAILS_VIDES));

        mockMvc.perform(get(AUDIT).param(OBJET_ID, projet.getId().toString()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[*].id").value(contains(legitime.toString())));
    }

    /** Une ligne enfant légitime (type et parent du registre) reste rendue avec la tranche visible. */
    @Test
    void r10_1_ligneEnfantDuRegistre_rendue() throws Exception {
        CfgTranche tranche = fixture.tranche("TR-AUD-PERIM", projet);
        UUID enfant = audit.ligne(new LigneAudit(UUID.randomUUID(), "Yes.Cfg.CfgTrancheStkItem", "Tranche",
                TYPE_TRANCHE, tranche.getId(), "Added", JANVIER, DETAILS_VIDES));

        mockMvc.perform(get(AUDIT).param(OBJET_ID, tranche.getId().toString()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id").value(contains(enfant.toString())));
    }
}

package com.tsix.immocrm.controller.audit;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.FixtureAudit;
import com.tsix.immocrm.fixtures.FixtureAudit.LigneAudit;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureAudit.TYPE_TRANCHE;
import static com.tsix.immocrm.fixtures.FixtureAudit.TYPE_TRANCHE_ARTICLE;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Lecture du journal d'audit — FEATURE-patrimoine-audit-actions.md §2.3 et §8 (AuditControllerTest). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuditControllerTest {

    private static final String AUDIT = "/api/audit";
    private static final String OBJET_ID = "objetId";
    private static final String LIBELLE_TRANCHE = "Tranche";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureAudit audit;

    private AdmUser admin;
    private CfgTranche tranche;
    private UUID ajout;
    private UUID enfant;
    private UUID modification;

    /** Trois lignes sur la tranche : son ajout (janvier), une ligne enfant (février), une modification (mars). */
    @BeforeEach
    void setUp() {
        admin = fixture.administrateur();
        CfgProject projet = fixture.projet("PRJ-AUD-CTRL", fixture.entite("ENT-AUD-CTRL", true));
        tranche = fixture.tranche("TR-AUD-CTRL", projet);
        ajout = audit.ligne(tranche.getId(), TYPE_TRANCHE, LIBELLE_TRANCHE, "Added",
                LocalDateTime.of(2026, 1, 10, 9, 0), "[[\"Code\",\"\",\"TR-AUD-CTRL\"]]");
        enfant = audit.ligne(new LigneAudit(UUID.randomUUID(), TYPE_TRANCHE_ARTICLE, LIBELLE_TRANCHE,
                TYPE_TRANCHE, tranche.getId(), "Added", LocalDateTime.of(2026, 2, 10, 9, 0), "[]"));
        modification = audit.ligne(tranche.getId(), TYPE_TRANCHE, LIBELLE_TRANCHE, "Modified",
                LocalDateTime.of(2026, 3, 10, 9, 0), "[[\"Description\",\"Ancienne\",\"Nouvelle\"]]");
    }

    private static RequestPostProcessor commercial(AdmUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getCode(), null, false, true, 0);
        return authentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Commercial"))));
    }

    // ------------------------------------------------------------------ §2.3 ObjectId OR ParentObjectId

    @Test
    void r2_3_lignesDeLObjetEtDeSesEnfants() throws Exception {
        mockMvc.perform(get(AUDIT).param(OBJET_ID, tranche.getId().toString()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[*].id").value(containsInAnyOrder(
                        ajout.toString(), enfant.toString(), modification.toString())));
    }

    @Test
    void r2_3_ligneEnfant_exposeLeTypeDuParent() throws Exception {
        mockMvc.perform(get(AUDIT).param(OBJET_ID, tranche.getId().toString()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + enfant + "')].parentTypeObjet")
                        .value(contains(TYPE_TRANCHE)))
                .andExpect(jsonPath("$.content[?(@.id=='" + enfant + "')].typeObjet")
                        .value(contains(TYPE_TRANCHE_ARTICLE)));
    }

    // ------------------------------------------------------------------ §2.3 tri

    @Test
    void r2_3_triParDateDecroissante() throws Exception {
        mockMvc.perform(get(AUDIT).param(OBJET_ID, tranche.getId().toString()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id").value(contains(
                        modification.toString(), enfant.toString(), ajout.toString())));
    }

    // ------------------------------------------------------------------ §2.3 pagination

    @Test
    void r2_3_tailleParDefaut50() throws Exception {
        mockMvc.perform(get(AUDIT).param(OBJET_ID, tranche.getId().toString()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(50))
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void r2_3_pagination_secondePageDeTaille2() throws Exception {
        mockMvc.perform(get(AUDIT).param(OBJET_ID, tranche.getId().toString())
                        .param("page", "1").param("size", "2").with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[*].id").value(contains(ajout.toString())));
    }

    // ------------------------------------------------------------------ §2.3 DTO

    @Test
    void r2_3_dto_colonnesEtDetailsDecodes() throws Exception {
        String ligne = "$.content[?(@.id=='" + modification + "')]";
        mockMvc.perform(get(AUDIT).param(OBJET_ID, tranche.getId().toString()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(ligne + ".action").value(contains("Modified")))
                .andExpect(jsonPath(ligne + ".utilisateur").value(contains(FixtureAudit.UTILISATEUR_OMRAN)))
                .andExpect(jsonPath(ligne + ".typeObjet").value(contains(TYPE_TRANCHE)))
                .andExpect(jsonPath(ligne + ".libelleTypeObjet").value(contains(LIBELLE_TRANCHE)))
                .andExpect(jsonPath(ligne + ".libelleObjet").value(contains("OBJ-" + modification.toString().substring(0, 8))))
                .andExpect(jsonPath(ligne + ".date").value(contains("2026-03-10T09:00:00")))
                .andExpect(jsonPath(ligne + ".details[*].propriete").value(contains("Description")))
                .andExpect(jsonPath(ligne + ".details[*].ancienneValeur").value(contains("Ancienne")))
                .andExpect(jsonPath(ligne + ".details[*].nouvelleValeur").value(contains("Nouvelle")));
    }

    /** §2.3 : détails illisibles (JSON invalide, ancien format) → details vide, pas d'erreur. */
    @Test
    void r2_3_detailsIllisibles_listeVideSansErreur() throws Exception {
        UUID illisible = audit.ligne(tranche.getId(), TYPE_TRANCHE, LIBELLE_TRANCHE, "Modified",
                LocalDateTime.of(2026, 4, 10, 9, 0), "Code: TR-AUD-CTRL -> TR-AUD-2 [");

        mockMvc.perform(get(AUDIT).param(OBJET_ID, tranche.getId().toString()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id=='" + illisible + "')].details[*]").value(empty()))
                .andExpect(jsonPath("$.content[?(@.id=='" + illisible + "')]").exists());
    }

    // ------------------------------------------------------------------ §0.3 lecture ouverte

    @Test
    void r0_3_lectureOuverteATousLesAuthentifies() throws Exception {
        mockMvc.perform(get(AUDIT).param(OBJET_ID, tranche.getId().toString()).with(commercial(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    // ------------------------------------------------------------------ §2.3 400

    @Test
    void r2_3_objetIdAbsent_400() throws Exception {
        mockMvc.perform(get(AUDIT).with(connecte(admin))).andExpect(status().isBadRequest());
    }

    @Test
    void r2_3_objetIdMalForme_400() throws Exception {
        mockMvc.perform(get(AUDIT).param(OBJET_ID, "pas-un-uuid").with(connecte(admin)))
                .andExpect(status().isBadRequest());
    }
}

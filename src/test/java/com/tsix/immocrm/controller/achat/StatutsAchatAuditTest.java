package com.tsix.immocrm.controller.achat;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureStatutsAchat;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.json.JsonMapper;

import javax.sql.DataSource;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.DESCRIPTION;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.DUREE;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.OK_PAIEMENT;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.corps;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.creation;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.id;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.modification;
import static com.tsix.immocrm.controller.achat.EcritureStatutsAchat.suppression;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Audit {@code AdmWebHistorics} — FEATURE-statuts-achat-parite.md règle 10 et R10 : une trace par écriture,
 * {@code ObjectType = 'Yes.Buy.Config.BuyDocumentStatus'}, Added / Modified / Deleted, {@code ObjectLabel} = description,
 * {@code Details} = {@code [["Propriété","avant","après"]]}.
 * <p>Sans {@code @Transactional} : la trace n'est écrite qu'à la validation. Données validées, effacées en {@code @AfterEach}.
 */
@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
class StatutsAchatAuditTest {

    private static final String TYPE = "Yes.Buy.Config.BuyDocumentStatus";
    private static final String ADDED = "Added";
    private static final String MODIFIED = "Modified";
    private static final String DELETED = "Deleted";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper json;
    @Autowired private DataSource dataSource;
    @Autowired private FixtureStatutsAchat statuts;

    private final String suffixe = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    private final Deque<String[]> creees = new ArrayDeque<>();
    private final List<UUID> statutsCrees = new ArrayList<>();

    private JdbcTemplate jdbc;
    private UUID type;
    private RequestPostProcessor admin;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        statuts.tables();
        type = inserer("AdmWorkFlowStateType", "Code, Description, Sorting", "Nouveau K2A-" + suffixe, "Nouveau", 1);
        // Rôle propre porteur du droit (pas un rôle « Administrateur » validé, que d'autres tests liraient).
        UUID role = inserer("AdmRole", "Code, Description", "Statuts K2A-" + suffixe, "Statuts K2A");
        inserer("AdmRight", "Code, Description, AdmRoleId", FixtureStatutsAchat.DROIT_STATUTS,
                FixtureStatutsAchat.DROIT_STATUTS, role.toString());
        UUID compte = inserer("AdmUser", "Code, Description, Disabled", "k2.audit." + suffixe, "Compte K2A", false);
        inserer("AdmUserRole", "AdmUserId, AdmRoleId", compte.toString(), role.toString());
        AdmUser principal = new AdmUser();
        principal.setId(compte);
        principal.setCode("k2.audit." + suffixe);
        admin = FixtureDossierVente.connecte(principal, FixtureStatutsAchat.ROLE_JWT);
    }

    @AfterEach
    void nettoyer() {
        for (UUID id : statutsCrees) {
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId = ?", id);
            effacer("DELETE FROM BuyDocumentStatusDocumentToAttach WHERE BuyDocumentStatusId = ?", id);
            effacer("DELETE FROM BuyDocumentStatus WHERE PKey = ?", id);
        }
        while (!creees.isEmpty()) {
            String[] ligne = creees.pop();
            effacer("DELETE FROM " + ligne[0] + " WHERE PKey = ?", ligne[1]);
        }
    }

    /** Nettoyage au mieux : une FK de CI inattendue ne doit pas faire échouer un test déjà jugé. */
    private void effacer(String sql, Object parametre) {
        try {
            jdbc.update(sql, parametre.toString());
        } catch (DataAccessException e) {
            log.warn("Nettoyage incomplet ({}) : {}", sql, e.getMessage());
        }
    }

    private UUID inserer(String table, String colonnes, Object... valeurs) {
        UUID id = UUID.randomUUID();
        Object[] parametres = new Object[valeurs.length + 1];
        parametres[0] = id.toString();
        System.arraycopy(valeurs, 0, parametres, 1, valeurs.length);
        String marques = String.join(",", Collections.nCopies(parametres.length, "?"));
        jdbc.update("INSERT INTO " + table + " (PKey, " + colonnes + ") VALUES (" + marques + ")", parametres);
        creees.push(new String[] {table, id.toString()});
        return id;
    }

    private Map<String, Object> corpsDeBase() {
        Map<String, Object> c = corps("K2A-" + suffixe, type);
        c.put(DESCRIPTION, "Audit K2A " + suffixe);
        c.put(OK_PAIEMENT, true);
        return c;
    }

    private UUID creer() throws Exception {
        UUID id = id(mockMvc.perform(creation(json, corpsDeBase()).with(admin)).andExpect(status().isCreated()).andReturn());
        statutsCrees.add(id);
        return id;
    }

    private List<Map<String, Object>> traces(UUID objet, String action) {
        return jdbc.queryForList("""
                SELECT Action, ObjectType, ObjectLabel, CAST(Details AS nvarchar(max)) AS Details
                FROM AdmWebHistorics WHERE ObjectId = ? AND Action = ?""", objet.toString(), action);
    }

    private Map<String, Object> seuleTrace(UUID objet, String action) {
        List<Map<String, Object>> t = traces(objet, action);
        assertThat(t).as("traces %s de %s", action, objet).hasSize(1);
        return t.get(0);
    }

    private static List<List<Object>> details(Map<String, Object> trace) {
        return JsonPath.read((String) trace.get("Details"), "$");
    }

    /** R10 : création → une trace Added, type et libellé d'OMRAN. */
    @Test
    void r10_creation_uneTraceAdded() throws Exception {
        UUID id = creer();

        Map<String, Object> trace = seuleTrace(id, ADDED);
        assertThat(trace).containsEntry("ObjectType", TYPE).containsEntry("ObjectLabel", "Audit K2A " + suffixe);
    }

    /** R10 : modification d'IsOkForPayement → une trace Modified dont Details contient ["IsOkForPayement","True","False"]. */
    @Test
    void r10_modification_uneTraceModified_detailsFormatOmran() throws Exception {
        UUID id = creer();
        Map<String, Object> c = corpsDeBase();
        c.put(OK_PAIEMENT, false);

        mockMvc.perform(modification(json, id, c).with(admin)).andExpect(status().isOk());

        Map<String, Object> trace = seuleTrace(id, MODIFIED);
        assertAll(
                () -> assertThat(trace).containsEntry("ObjectType", TYPE).containsEntry("ObjectLabel", "Audit K2A " + suffixe),
                () -> assertThat(details(trace)).anySatisfy(t -> assertThat(t)
                        .containsExactly("IsOkForPayement", "True", "False")));
    }

    /** R10 : suppression → une trace Deleted. */
    @Test
    void r10_suppression_uneTraceDeleted() throws Exception {
        UUID id = creer();

        mockMvc.perform(suppression(id).with(admin)).andExpect(status().isNoContent());

        assertThat(seuleTrace(id, DELETED)).containsEntry("ObjectType", TYPE);
    }

    /** Un refus 400 n'écrit aucune trace Modified ; témoin : la trace Added de la création. */
    @Test
    void r10_refus400_aucuneTraceModified() throws Exception {
        UUID id = creer();
        Map<String, Object> c = corpsDeBase();
        c.put(DUREE, -1);

        mockMvc.perform(modification(json, id, c).with(admin)).andExpect(status().isBadRequest());

        assertAll(
                () -> assertThat(traces(id, MODIFIED)).isEmpty(),
                () -> assertThat(traces(id, ADDED)).hasSize(1));
    }
}

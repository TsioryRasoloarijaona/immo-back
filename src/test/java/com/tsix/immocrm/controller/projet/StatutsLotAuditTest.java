package com.tsix.immocrm.controller.projet;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.tsix.immocrm.fixtures.FixtureStatutsAchat;
import com.tsix.immocrm.fixtures.FixtureStatutsLot;
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

import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.DESCRIPTION;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.DUREE;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.STATUT;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.STATUTS;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.TRANSITION;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.TRANSITIONS;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.transition;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.corps;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.creation;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.id;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.modification;
import static com.tsix.immocrm.controller.projet.EcritureStatutsLot.suppression;
import static com.tsix.immocrm.fixtures.FixtureStatutsLot.CONCESSIONNAIRES;
import static com.tsix.immocrm.fixtures.FixtureStatutsLot.TYPE_INTERMEDIAIRE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Audit {@code AdmWebHistorics} des statuts de lot — FEATURE-restes-projet-achat.md §6 : une trace par écriture,
 * {@code ObjectType = 'Yes.Str.StrStatus'}, {@code Details} = {@code [["Propriété","avant","après"]]}.
 * <p>Sans {@code @Transactional} : la trace n'est écrite qu'à la validation. Données validées, effacées en {@code @AfterEach}.
 */
@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
class StatutsLotAuditTest {

    private static final String TYPE = "Yes.Str.StrStatus";
    private static final String ADDED = "Added";
    private static final String MODIFIED = "Modified";
    private static final String DELETED = "Deleted";
    private static final String TYPE_TRANSITION = "Yes.Str.StrWorkFlow";
    private static final String LIBELLE = "ObjectLabel";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper json;
    @Autowired private DataSource dataSource;

    private final String suffixe = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    private final Deque<String[]> creees = new ArrayDeque<>();
    private final List<UUID> statutsCrees = new ArrayList<>();
    private final List<UUID> transitionsCreees = new ArrayList<>();

    private JdbcTemplate jdbc;
    private RequestPostProcessor compte;
    private UUID depart;
    private UUID arrivee;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        // Parents de prod, créés seulement s'ils manquent (yesRemsTest n'a aucun genre de lot) et alors effacés à la fin.
        assurer("StrEntity", CONCESSIONNAIRES, "Code, Description", "PrjMarketDealers", "Lot Concessionnaires");
        assurer("StrStatusType", TYPE_INTERMEDIAIRE, "Code, Description, Sorting", "Intermédiaire", "Intermédiaire", 1);
        UUID role = inserer("AdmRole", "Code, Description", "Statuts F1A-" + suffixe, "Statuts F1A");
        inserer("AdmRight", "Code, Description, AdmRoleId", FixtureStatutsLot.DROIT_STATUTS,
                FixtureStatutsLot.DROIT_STATUTS, role.toString());
        inserer("AdmRight", "Code, Description, AdmRoleId", FixtureStatutsLot.DROIT_TRANSITIONS,
                FixtureStatutsLot.DROIT_TRANSITIONS, role.toString());
        depart = inserer("StrStatus", "Code, Description, Sorting, StrConfigEntityId, StrStatusTypeId, ColorName, Delay",
                "F1AD-" + suffixe, "F1AD-" + suffixe, 1, CONCESSIONNAIRES.toString(), TYPE_INTERMEDIAIRE.toString(), "LimeGreen", 0);
        arrivee = inserer("StrStatus", "Code, Description, Sorting, StrConfigEntityId, StrStatusTypeId, ColorName, Delay",
                "F1AA-" + suffixe, "F1AA-" + suffixe, 2, CONCESSIONNAIRES.toString(), TYPE_INTERMEDIAIRE.toString(), "LimeGreen", 0);
        UUID utilisateur = inserer("AdmUser", "Code, Description, Disabled", "f1.audit." + suffixe, "Compte F1A", false);
        inserer("AdmUserRole", "AdmUserId, AdmRoleId", utilisateur.toString(), role.toString());
        AdmUser principal = new AdmUser();
        principal.setId(utilisateur);
        principal.setCode("f1.audit." + suffixe);
        compte = FixtureDossierVente.connecte(principal, FixtureStatutsAchat.ROLE_JWT);
    }

    @AfterEach
    void nettoyer() {
        for (UUID id : transitionsCreees) {
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId = ?", id);
            effacer("DELETE FROM StrWorkFlowIntervener WHERE StrWorkFlowId = ?", id);
            effacer("DELETE FROM StrWorkFlow WHERE PKey = ?", id);
        }
        for (UUID id : statutsCrees) {
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId = ?", id);
            effacer("DELETE FROM StrStatusMandatoryField WHERE StrStatusId = ?", id);
            effacer("DELETE FROM StrStatus WHERE PKey = ?", id);
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
        poser(table, id, colonnes, valeurs);
        return id;
    }

    private void assurer(String table, UUID id, String colonnes, Object... valeurs) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE PKey = ?", Integer.class, id.toString());
        if (n == null || n == 0) {
            poser(table, id, colonnes, valeurs);
        }
    }

    private void poser(String table, UUID id, String colonnes, Object... valeurs) {
        Object[] parametres = new Object[valeurs.length + 1];
        parametres[0] = id.toString();
        System.arraycopy(valeurs, 0, parametres, 1, valeurs.length);
        String marques = String.join(",", Collections.nCopies(parametres.length, "?"));
        jdbc.update("INSERT INTO " + table + " (PKey, " + colonnes + ") VALUES (" + marques + ")", parametres);
        creees.push(new String[] {table, id.toString()});
    }

    private Map<String, Object> corpsDeBase() {
        Map<String, Object> c = corps(CONCESSIONNAIRES, "F1A-" + suffixe, TYPE_INTERMEDIAIRE);
        c.put(DESCRIPTION, "Audit F1A " + suffixe);
        return c;
    }

    private UUID creer() throws Exception {
        UUID id = id(mockMvc.perform(creation(json, STATUTS, corpsDeBase()).with(compte))
                .andExpect(status().isCreated()).andReturn());
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

    /** Création → une trace Added, type OMRAN, libellé = le code (contrat 16/11). */
    @Test
    void creation_uneTraceAdded_typeStrStatus_libelleCode() throws Exception {
        UUID id = creer();

        assertThat(seuleTrace(id, ADDED)).containsEntry("ObjectType", TYPE).containsEntry(LIBELLE, "F1A-" + suffixe);
    }

    // ------------------------------------------------------------------ transitions de lot

    private UUID creerTransition() throws Exception {
        mockMvc.perform(creation(json, TRANSITIONS, transition(CONCESSIONNAIRES, depart, arrivee, codeTransition())).with(compte))
                .andExpect(status().isCreated());
        Map<String, Object> l = jdbc.queryForMap("SELECT CONVERT(varchar(36), PKey) AS id FROM StrWorkFlow WHERE Description = ?",
                codeTransition());
        UUID id = UUID.fromString((String) l.get("id"));
        transitionsCreees.add(id);
        return id;
    }

    private String codeTransition() {
        return "T-F1A-" + suffixe;
    }

    /** Contrat 16/11 : transition créée → une trace Added, {@code Yes.Str.StrWorkFlow}, libellé = le code. */
    @Test
    void transition_creation_uneTraceAdded_typeStrWorkFlow_libelleCode() throws Exception {
        UUID id = creerTransition();

        assertThat(seuleTrace(id, ADDED)).containsEntry("ObjectType", TYPE_TRANSITION).containsEntry(LIBELLE, codeTransition());
    }

    @Test
    void transition_modification_uneTraceModified() throws Exception {
        UUID id = creerTransition();
        Map<String, Object> c = transition(CONCESSIONNAIRES, depart, arrivee, codeTransition());
        c.put(DESCRIPTION, "Transition modifiée " + suffixe);

        mockMvc.perform(modification(json, TRANSITION, id, c).with(compte)).andExpect(status().isOk());

        assertThat(seuleTrace(id, MODIFIED)).containsEntry("ObjectType", TYPE_TRANSITION).containsEntry(LIBELLE, codeTransition());
    }

    @Test
    void transition_suppression_uneTraceDeleted() throws Exception {
        UUID id = creerTransition();

        mockMvc.perform(suppression(TRANSITION, id).with(compte)).andExpect(status().isNoContent());

        assertThat(seuleTrace(id, DELETED)).containsEntry("ObjectType", TYPE_TRANSITION).containsEntry(LIBELLE, codeTransition());
    }

    /** Modification de la description → une trace Modified dont Details contient ["Description", avant, après]. */
    @Test
    void modification_uneTraceModified_detailsFormatOmran() throws Exception {
        UUID id = creer();
        Map<String, Object> c = corpsDeBase();
        c.put(DESCRIPTION, "Audit F1A modifié " + suffixe);

        mockMvc.perform(modification(json, STATUT, id, c).with(compte)).andExpect(status().isOk());

        Map<String, Object> trace = seuleTrace(id, MODIFIED);
        assertAll(
                () -> assertThat(trace).containsEntry("ObjectType", TYPE).containsEntry(LIBELLE, "F1A-" + suffixe),
                () -> assertThat(details(trace)).anySatisfy(t -> assertThat(t)
                        .containsExactly("Description", "Audit F1A " + suffixe, "Audit F1A modifié " + suffixe)));
    }

    /** Suppression → une trace Deleted. */
    @Test
    void suppression_uneTraceDeleted() throws Exception {
        UUID id = creer();

        mockMvc.perform(suppression(STATUT, id).with(compte)).andExpect(status().isNoContent());

        assertThat(seuleTrace(id, DELETED)).containsEntry("ObjectType", TYPE);
    }

    /** Un refus 400 n'écrit aucune trace Modified ; témoin : la trace Added de la création. */
    @Test
    void refus400_aucuneTraceModified() throws Exception {
        UUID id = creer();
        Map<String, Object> c = corpsDeBase();
        c.put(DUREE, -1);

        mockMvc.perform(modification(json, STATUT, id, c).with(compte)).andExpect(status().isBadRequest());

        assertAll(
                () -> assertThat(traces(id, MODIFIED)).isEmpty(),
                () -> assertThat(traces(id, ADDED)).hasSize(1));
    }
}

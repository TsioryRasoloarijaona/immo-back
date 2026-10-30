package com.tsix.immocrm.controller.administration;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureReferentielsAdmin;
import com.tsix.immocrm.fixtures.FixtureRendezVous;
import com.tsix.immocrm.fixtures.FixtureTaches;
import com.tsix.immocrm.fixtures.FixtureTachesAdmin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureRendezVous.FICHE;
import static com.tsix.immocrm.fixtures.FixtureRendezVous.LIBELLE_AUDIT;
import static com.tsix.immocrm.fixtures.FixtureRendezVous.RACINE;
import static com.tsix.immocrm.fixtures.FixtureRendezVous.TIER_ID;
import static com.tsix.immocrm.fixtures.FixtureRendezVous.TYPE_AUDIT;
import static com.tsix.immocrm.fixtures.FixtureTaches.DATE_PREVUE;
import static com.tsix.immocrm.fixtures.FixtureTaches.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureTaches.ENTITE_RENDEZ_VOUS;
import static com.tsix.immocrm.fixtures.FixtureTaches.SEMIS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Règle 14 de FEATURE-admin-rendez-vous.md. Sans {@code @Transactional} : la trace n'existe qu'après validation ; tout ce
 * qui est validé est effacé en {@code @AfterEach}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RendezVousAuditTest {

    private static final String AUDIT = "/api/audit";
    private static final String OBJET = "objetId";
    private static final String AJOUT = "Added";
    private static final String MODIFICATION = "Modified";
    private static final String SUPPRESSION = "Deleted";
    private static final LocalDateTime JOUR = LocalDateTime.of(2031, 3, 10, 9, 30);

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private FixturePatrimoine patrimoine;

    private final String jeton = FixtureReferentielsAdmin.jeton();
    private final List<UUID> objets = new ArrayList<>();
    private AdmUser admin;
    private UUID tier;

    @BeforeEach
    void setUp() {
        FixtureTachesAdmin.TABLES_REFERENTIELS.forEach(jdbc::execute);
        SEMIS.forEach(jdbc::execute);
        admin = patrimoine.administrateur();
        tier = UUID.randomUUID();
        jdbc.update("INSERT INTO CfgTier (PKey, Code, Description) VALUES (?, ?, ?)", tier.toString(), "TIE-" + jeton, "Tier");
    }

    @AfterEach
    void nettoyer() {
        objets.forEach(id -> jdbc.update("DELETE FROM AdmWebHistorics WHERE ObjectId = ?", id.toString()));
        FixtureTaches.NETTOYAGE.forEach(jdbc::execute);
        jdbc.update("DELETE FROM CfgTier WHERE PKey = ?", tier.toString());
        jdbc.update("DELETE FROM AdmUserHabilitation WHERE AdmUserId = ?", admin.getId().toString());
        jdbc.update("DELETE FROM AdmUser WHERE PKey = ?", admin.getId().toString());
    }

    private UUID suivi(String json) {
        UUID id = UUID.fromString(JsonPath.read(json, "$.id"));
        objets.add(id);
        return id;
    }

    private UUID creer() throws Exception {
        return suivi(mockMvc.perform(post(RACINE).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec(DESCRIPTION, "Audit " + jeton).avec(TIER_ID, tier)
                                .avec(DATE_PREVUE, "2031-03-10T09:30:00").json()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    /** Validé avant la requête : écrit par OMRAN, sans trace. */
    private UUID rendezVousValide() {
        UUID id = UUID.randomUUID();
        Timestamp debut = Timestamp.valueOf(JOUR);
        jdbc.update("""
                        INSERT INTO AdmTask (PKey, Code, Description, StrEntityId, CfgProspectId, StartDatePrev, DueDatePrev)
                        VALUES (?, ?, ?, ?, ?, ?, ?)""",
                id.toString(), "RAU-" + jeton, "Libelle " + jeton, ENTITE_RENDEZ_VOUS.toString(), tier.toString(), debut, debut);
        objets.add(id);
        return id;
    }

    private void journal(UUID id, String action) throws Exception {
        ResultActions reponse = mockMvc.perform(get(AUDIT).param(OBJET, id.toString()).with(connecte(admin)))
                .andExpect(status().isOk());
        String ligne = "$.content[?(@.action == '" + action + "')].";
        reponse.andExpect(jsonPath(ligne + "typeObjet", hasItem(TYPE_AUDIT)))
                .andExpect(jsonPath(ligne + "libelleTypeObjet", hasItem(LIBELLE_AUDIT)));
    }

    private int traces(UUID id, String action, String type, String libelle) {
        Integer n = jdbc.queryForObject("""
                SELECT COUNT(*) FROM AdmWebHistorics
                WHERE ObjectId = ? AND Action = ? AND ObjectType = ? AND ObjectTypeLabel = ?""",
                Integer.class, id.toString(), action, type, libelle);
        return n == null ? 0 : n;
    }

    @Test
    void r14_creation_tracee_lisibleParApiAudit() throws Exception {
        UUID id = creer();

        journal(id, AJOUT);
    }

    @Test
    void r14_modification_tracee_lisibleParApiAudit() throws Exception {
        UUID id = rendezVousValide();

        mockMvc.perform(patch(FICHE, id).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec(DESCRIPTION, "Modifie " + jeton).json()))
                .andExpect(status().isOk());

        journal(id, MODIFICATION);
    }

    /** L'objet n'existe plus : la trace se relit dans le journal, pas par {@code /api/audit}. */
    @Test
    void r14_suppression_tracee() throws Exception {
        UUID id = rendezVousValide();

        mockMvc.perform(delete(FICHE, id).with(connecte(admin))).andExpect(status().isNoContent());

        assertThat(traces(id, SUPPRESSION, TYPE_AUDIT, LIBELLE_AUDIT)).isEqualTo(1);
    }

    /** « /taches ne change pas » : le type se choisit par ligne, une tâche garde le sien. */
    @Test
    void tache_resteTraceeSousSonType() throws Exception {
        UUID id = suivi(mockMvc.perform(post(FixtureTaches.RACINE).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(corps().avec(DESCRIPTION, "Tache " + jeton).json()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));

        assertThat(traces(id, AJOUT, "Yes.Adm.AdmTask", "Tâche")).isEqualTo(1);
    }
}

package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixturePlans;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Purge et journal P0 — FEATURE-patrimoine-plans.md, « Réponses du manager » n° 4 : une ligne d'audit {@code Deleted}
 * par plan purgé. Sans {@code @Transactional} : {@code AdmWebHistorics} n'est écrite qu'à la validation. Lignes posées
 * en JDBC (validées), effacées en {@code @AfterEach}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PurgePlansAuditTest {

    private static final Logger log = LoggerFactory.getLogger(PurgePlansAuditTest.class);
    private static final String DELETED = "Deleted";

    @Autowired private MockMvc mockMvc;
    @Autowired private DataSource dataSource;

    private final String jeton = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    private final Deque<String[]> creees = new ArrayDeque<>();

    private JdbcTemplate jdbc;
    private AdmUser admin;
    private UUID tranche;
    private UUID plan1;
    private UUID plan2;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        admin = new AdmUser();
        admin.setId(inserer("AdmUser", "Code, Description, Disabled", "admin.plans." + jeton, "Admin plans", false));
        admin.setCode("admin.plans." + jeton);
        UUID entite = inserer("CfgCompany", "Code, Description, IsActive", "ENT-PPA-" + jeton, "Entite", true);
        UUID projet = inserer("CfgProject", "Code, Description, CfgCompanyId", "PRJ-PPA-" + jeton, "Projet", entite.toString());
        tranche = inserer("CfgTranche", "Code, Description, CfgProjectId", "TR-PPA-" + jeton, "Tranche", projet.toString());
        UUID etage1 = inserer("StkFloor", "Code, Description", "PPA1-" + jeton, "Etage 1");
        UUID etage2 = inserer("StkFloor", "Code, Description", "PPA2-" + jeton, "Etage 2");
        plan1 = plan(etage1);
        plan2 = plan(etage2);
    }

    @AfterEach
    void nettoyer() {
        effacer("DELETE FROM AdmWebHistorics WHERE ObjectId IN (?, ?, ?)", plan1, plan2, tranche);
        while (!creees.isEmpty()) {
            String[] ligne = creees.pop();
            effacer("DELETE FROM " + ligne[0] + " WHERE PKey = ?", ligne[1]);
        }
    }

    private UUID plan(UUID etage) {
        return inserer("StkHierarchyPlan",
                "Code, Description, PlanImage, StkHierarchyId, StkFloorId, UserCreate, UserCreateDateTime, width, height, X, Y",
                "Etage", "Etage", FixturePlans.dataUriPng(), tranche.toString(), etage.toString(), "Fixture",
                LocalDateTime.of(2026, 1, 15, 10, 0), FixturePlans.LARGEUR, FixturePlans.HAUTEUR, FixturePlans.X,
                FixturePlans.Y);
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

    /** Nettoyage au mieux : une ligne déjà supprimée par la purge ne doit pas faire échouer le test. */
    private void effacer(String sql, Object... parametres) {
        try {
            jdbc.update(sql, Arrays.stream(parametres).map(String::valueOf).toArray());
        } catch (DataAccessException e) {
            log.warn("Nettoyage incomplet ({}) : {}", sql, e.getMessage());
        }
    }

    private List<String> actions(UUID objet) {
        return jdbc.queryForList("SELECT Action FROM AdmWebHistorics WHERE ObjectId = ? AND Action = ?", String.class,
                objet.toString(), DELETED);
    }

    @Test
    void reponse4_purge_uneLigneDeletedParPlan() throws Exception {
        mockMvc.perform(delete("/api/tranches/{id}/plans", tranche).with(connecte(admin)))
                .andExpect(status().isNoContent());

        assertAll(
                () -> assertThat(actions(plan1)).as("audit plan 1").hasSize(1),
                () -> assertThat(actions(plan2)).as("audit plan 2").hasSize(1),
                () -> assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM StkHierarchyPlan WHERE StkHierarchyId = ?",
                        Integer.class, tranche.toString())).as("plans restants").isZero());
    }
}

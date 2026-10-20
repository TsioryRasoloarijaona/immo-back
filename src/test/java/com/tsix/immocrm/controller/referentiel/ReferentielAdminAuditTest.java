package com.tsix.immocrm.controller.referentiel;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.CATEGORIES_CHARGE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.CODE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.CONTENANCES;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.DESCRIPTION;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.ETAGES;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.FACADES;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.IMMEUBLES;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.LIGNE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.NIVEAUX_TERRAIN;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.ORIENTATIONS;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.RUBRIQUES_TRANCHE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.SORTING;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.STANDINGS;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.TABLE;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.VOCATIONS;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.VOCATIONS_TERRAIN;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.VUES;
import static com.tsix.immocrm.fixtures.FixtureReferentielsAdmin.jeton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Audit P0 des référentiels — FEATURE-patrimoine-referentiels.md §0 et §5.1.7 : une ligne {@code AdmWebHistorics} par
 * écriture, {@code ObjectType} mesuré en prod ou, à défaut, nom de classe OMRAN. Sans {@code @Transactional} : la ligne
 * n'existe qu'après validation ; tout ce qui est validé est effacé en {@code @AfterEach}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReferentielAdminAuditTest {

    private static final Logger log = LoggerFactory.getLogger(ReferentielAdminAuditTest.class);

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private DataSource dataSource;

    private final String jeton = jeton();
    private final List<String[]> lignesCreees = new ArrayList<>();

    private JdbcTemplate jdbc;
    private AdmUser admin;

    record Audite(String slug, String table, boolean avecTri, String objectType) {
        @Override
        public String toString() {
            return slug + " → " + objectType;
        }
    }

    static Stream<Audite> tables() {
        return Stream.of(
                new Audite(VOCATIONS, "StkVocation", false, "Yes.Stk.Config.Vocation"),
                new Audite(FACADES, "StkFacade", false, "Yes.Stk.Config.Facade"),
                new Audite(VUES, "StkVue", false, "Yes.Stk.Config.Vue"),
                new Audite(CATEGORIES_CHARGE, "StkCostCategory", false, "Yes.Stk.Config.CostCategory"),
                new Audite(CONTENANCES, "StkContenance", false, "Yes.Stk.Config.Contenance"),
                new Audite(STANDINGS, "CfgTrancheStanding", false, "Yes.Cfg.TrancheStanding"),
                new Audite(VOCATIONS_TERRAIN, "LndVocation", false, "Yes.Lnd.Config.Vocation"),
                new Audite(NIVEAUX_TERRAIN, "LndLevel", false, "Yes.Lnd.Config.Level"),
                new Audite(RUBRIQUES_TRANCHE, "StkTrancheHeading", false, "Yes.Stk.Config.TrancheHeading"),
                new Audite(IMMEUBLES, "StkBuilding", true, "Yes.Stk.Config.StkBuilding"),
                new Audite(ETAGES, "StkFloor", true, "Yes.Stk.Config.StkFloor"),
                new Audite(ORIENTATIONS, "StkOrientation", true, "Yes.Stk.Config.StkOrientation"));
    }

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        admin = patrimoine.administrateur();
    }

    @AfterEach
    void nettoyer() {
        lignesCreees.forEach(l -> {
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId = ?", l[1]);
            effacer("DELETE FROM " + l[0] + " WHERE PKey = ?", l[1]);
        });
        effacer("DELETE FROM AdmUserHabilitation WHERE AdmUserId = ?", admin.getId().toString());
        effacer("DELETE FROM AdmUser WHERE PKey = ?", admin.getId().toString());
    }

    private void effacer(String sql, String id) {
        try {
            jdbc.update(sql, id);
        } catch (DataAccessException e) {
            log.warn("Nettoyage incomplet ({}) : {}", sql, e.getMessage());
        }
    }

    private UUID creer(Audite t, String code) throws Exception {
        var corps = corps().avec(CODE, code).avec(DESCRIPTION, "Libelle " + code);
        if (t.avecTri()) {
            corps.avec(SORTING, 3);
        }
        String json = mockMvc.perform(post(TABLE, t.slug()).with(connecte(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(corps.json()))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(JsonPath.read(json, "$.id"));
        lignesCreees.add(new String[]{t.table(), id.toString()});
        return id;
    }

    private List<String> types(UUID objetId, String action) {
        return jdbc.queryForList("SELECT ObjectType FROM AdmWebHistorics WHERE ObjectId = ? AND Action = ?",
                String.class, objetId.toString(), action);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tables")
    void s0_creation_uneLigneAdded_objectTypeOmran(Audite t) throws Exception {
        UUID id = creer(t, "AUD-" + jeton);

        assertThat(types(id, "Added")).containsExactly(t.objectType());
    }

    @Test
    void s0_modification_uneLigneModified() throws Exception {
        Audite orientation = tables().filter(t -> ORIENTATIONS.equals(t.slug())).findFirst().orElseThrow();
        UUID id = creer(orientation, "AUDM-" + jeton);

        mockMvc.perform(patch(LIGNE, ORIENTATIONS, id).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(corps().avec(DESCRIPTION, "Modifiee " + jeton).json()))
                .andExpect(status().isOk());

        assertThat(types(id, "Modified")).containsExactly(orientation.objectType());
    }

    @Test
    void s0_suppression_uneLigneDeleted() throws Exception {
        Audite orientation = tables().filter(t -> ORIENTATIONS.equals(t.slug())).findFirst().orElseThrow();
        UUID id = creer(orientation, "AUDD-" + jeton);

        mockMvc.perform(delete(LIGNE, ORIENTATIONS, id).with(connecte(admin))).andExpect(status().isNoContent());

        List<Map<String, Object>> lignes = jdbc.queryForList(
                "SELECT ObjectType FROM AdmWebHistorics WHERE ObjectId = ? AND Action = 'Deleted'", id.toString());
        assertThat(lignes).singleElement().satisfies(l -> assertThat(l).containsEntry("ObjectType",
                orientation.objectType()));
    }
}

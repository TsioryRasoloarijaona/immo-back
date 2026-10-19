package com.tsix.immocrm.controller.audit;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Registre d'audit du lot P5 — FEATURE-entites-juridiques.md §0 et §7.7 : {@code CfgCompany} (« Entité juridique »,
 * mesuré en prod, libellé = Code) et tables d'actionnaires ({@code ObjectType} = classe OMRAN, aucune ligne en prod ;
 * ligne de groupe → parent groupe ; {@code CfgCompanyShareholder} → parent société).
 *
 * <p>Sans {@code @Transactional}, comme AuditOmranEcouteurTest : la ligne d'audit n'existe qu'après validation. Tout
 * ce qui est validé est effacé en {@code @AfterEach} — aucune entité ne reste en base, active ou non.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuditEntitesEcouteurTest {

    private static final Logger log = LoggerFactory.getLogger(AuditEntitesEcouteurTest.class);

    private static final String TYPE_ENTITE = "Yes.Cfg.CfgCompany";
    private static final String TYPE_ACTIONNAIRE = "Yes.Cfg.CfgShareholder";
    private static final String TYPE_GROUPE = "Yes.Cfg.CfgGroupShareholder";
    private static final String TYPE_LIGNE_GROUPE = "Yes.Cfg.CfgGroupShareholderLine";
    private static final String TYPE_REPARTITION = "Yes.Cfg.CfgCompanyShareholder";
    private static final String ADDED = "Added";
    private static final String ACTION = "Action";
    private static final String OBJECT_TYPE = "ObjectType";
    private static final String PARENT_TYPE = "ParentObjectType";
    private static final String PARENT_ID = "ParentObjectId";
    private static final String CODE = "code";
    private static final String DESCRIPTION = "description";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private DataSource dataSource;

    private final String jeton = UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    private final List<UUID> entitesCreees = new ArrayList<>();
    private final List<UUID> groupesCrees = new ArrayList<>();
    private final List<UUID> actionnairesCrees = new ArrayList<>();
    private final List<UUID> objets = new ArrayList<>();

    private JdbcTemplate jdbc;
    private AdmUser admin;
    private UUID devise;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        admin = fixture.administrateur();
        devise = UUID.randomUUID();
        jdbc.update("INSERT INTO StlCurrency (PKey, Code, Description) VALUES (?, ?, ?)",
                devise.toString(), "DEV-AUDE-" + jeton, "Devise audit");
    }

    @AfterEach
    void nettoyer() {
        objets.forEach(id -> effacer("DELETE FROM AdmWebHistorics WHERE ObjectId = ? OR ParentObjectId = ?", id, id));
        entitesCreees.forEach(id -> {
            effacer("DELETE FROM AdmWebHistorics WHERE ParentObjectId = ?", id);
            effacer("DELETE FROM CfgCompanyShareholder WHERE CfgCompanyId = ?", id);
            effacer("DELETE FROM AdmSequence WHERE CfgCompanyId = ?", id);
            effacer("DELETE FROM AdmUserHabilitation WHERE CfgCompanyId = ?", id);
            effacer("DELETE FROM CfgCompany WHERE PKey = ?", id);
        });
        groupesCrees.forEach(id -> {
            effacer("DELETE FROM AdmWebHistorics WHERE ParentObjectId = ?", id);
            effacer("DELETE FROM CfgGroupShareholderLine WHERE CfgGroupShareholderId = ?", id);
            effacer("DELETE FROM CfgGroupShareholder WHERE PKey = ?", id);
        });
        actionnairesCrees.forEach(id -> effacer("DELETE FROM CfgShareholder WHERE PKey = ?", id));
        effacer("DELETE FROM StlCurrency WHERE PKey = ?", devise);
        if (admin != null) {
            effacer("DELETE FROM AdmUserHabilitation WHERE AdmUserId = ?", admin.getId());
            effacer("DELETE FROM AdmUser WHERE PKey = ?", admin.getId());
        }
    }

    /** Nettoyage au mieux : une table annexe (FK en CI) ne doit pas faire échouer un test déjà jugé. */
    private void effacer(String sql, Object... parametres) {
        try {
            jdbc.update(sql, Arrays.stream(parametres).map(Object::toString).toArray());
        } catch (DataAccessException e) {
            log.warn("Nettoyage incomplet ({}) : {}", sql, e.getMessage());
        }
    }

    // ------------------------------------------------------------------ outils

    private ResultActions envoyer(MockHttpServletRequestBuilder requete, String json) throws Exception {
        return mockMvc.perform(requete.with(connecte(admin)).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private UUID creer(String url, String json, List<UUID> suivi) throws Exception {
        String reponse = envoyer(post(url), json).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(JsonPath.read(reponse, "$.id"));
        suivi.add(id);
        objets.add(id);
        return id;
    }

    private UUID creerEntite(String code) throws Exception {
        return creer("/api/entites-juridiques", "{\"code\":\"" + code + "\",\"description\":\"Entite " + code
                + "\",\"deviseId\":\"" + devise + "\"}", entitesCreees);
    }

    private UUID creerActionnaire(String code) throws Exception {
        return creer("/api/actionnaires", "{\"" + CODE + "\":\"" + code + "\",\"" + DESCRIPTION + "\":\"Actionnaire "
                + code + "\"}", actionnairesCrees);
    }

    private List<Map<String, Object>> lignes(UUID objetId) {
        return jdbc.queryForList("""
                        SELECT Action, ObjectType, ObjectTypeLabel, ObjectLabel, ParentObjectType,
                               CONVERT(varchar(36), ParentObjectId) AS ParentObjectId
                        FROM AdmWebHistorics WHERE ObjectId = ?""", objetId.toString());
    }

    private List<Map<String, Object>> enfants(UUID parentId, String type) {
        return jdbc.queryForList("""
                        SELECT Action, ObjectType, ParentObjectType, CONVERT(varchar(36), ParentObjectId) AS ParentObjectId
                        FROM AdmWebHistorics WHERE ParentObjectId = ? AND ObjectType = ?""", parentId.toString(), type);
    }

    private Map<String, Object> seuleLigne(UUID objetId, String action) {
        List<Map<String, Object>> trouvees = lignes(objetId).stream().filter(l -> action.equals(l.get(ACTION))).toList();
        assertThat(trouvees).as("lignes %s de %s", action, objetId).hasSize(1);
        return trouvees.get(0);
    }

    // ------------------------------------------------------------------ CfgCompany

    @Test
    void r7_7_creationEntite_ligneAdded_entiteJuridique_libelleCode() throws Exception {
        String code = "EJ-AUD-" + jeton;
        UUID id = creerEntite(code);

        Map<String, Object> ligne = seuleLigne(id, ADDED);
        assertAll(
                () -> assertThat(ligne.get(OBJECT_TYPE)).isEqualTo(TYPE_ENTITE),
                () -> assertThat(ligne.get("ObjectTypeLabel")).isEqualTo("Entité juridique"),
                () -> assertThat(ligne.get("ObjectLabel")).isEqualTo(code),
                () -> assertThat(ligne.get(PARENT_TYPE)).isNull());
    }

    @Test
    void r7_7_modificationEntite_ligneModified() throws Exception {
        UUID id = creerEntite("EJ-AUDM-" + jeton);

        envoyer(patch("/api/entites-juridiques/{id}", id), "{\"description\":\"Nouveau libelle\"}")
                .andExpect(status().isOk());

        assertThat(seuleLigne(id, "Modified").get(OBJECT_TYPE)).isEqualTo(TYPE_ENTITE);
    }

    // ------------------------------------------------------------------ actionnaires et groupes

    @Test
    void r7_7_creationActionnaire_ligneAdded_classeOmran() throws Exception {
        UUID id = creerActionnaire("ACT-AUD-" + jeton);

        assertThat(seuleLigne(id, ADDED).get(OBJECT_TYPE)).isEqualTo(TYPE_ACTIONNAIRE);
    }

    @Test
    void r7_7_creationGroupe_ligneAddedDuGroupe_lignesDeGroupeAParentGroupe() throws Exception {
        UUID actionnaire = creerActionnaire("ACT-AUDG-" + jeton);
        UUID groupe = creer("/api/groupes-actionnaires", "{\"code\":\"GRP-AUD-" + jeton
                + "\",\"description\":\"Groupe audit\",\"lignes\":[{\"actionnaireId\":\"" + actionnaire
                + "\",\"pourcentage\":1}]}", groupesCrees);

        assertThat(seuleLigne(groupe, ADDED).get(OBJECT_TYPE)).isEqualTo(TYPE_GROUPE);
        assertThat(enfants(groupe, TYPE_LIGNE_GROUPE)).singleElement().satisfies(l -> assertAll(
                () -> assertThat(l.get(ACTION)).isEqualTo(ADDED),
                () -> assertThat(l.get(PARENT_TYPE)).isEqualTo(TYPE_GROUPE),
                () -> assertThat(FixturePatrimoine.guid(l.get(PARENT_ID))).isEqualTo(FixturePatrimoine.guid(groupe))));
    }

    @Test
    void r7_7_repartition_lignesCfgCompanyShareholderAParentSociete() throws Exception {
        UUID entite = creerEntite("EJ-AUDR-" + jeton);
        UUID actionnaire = creerActionnaire("ACT-AUDR-" + jeton);
        jdbc.update("UPDATE CfgCompany SET SharingNumber = 10, TotalSharing = 1 WHERE PKey = ?", entite.toString());

        envoyer(put("/api/entites-juridiques/{id}/repartition", entite),
                "[{\"actionnaireId\":\"" + actionnaire + "\",\"nombreParts\":10}]").andExpect(status().isOk());

        assertThat(enfants(entite, TYPE_REPARTITION)).singleElement().satisfies(l -> assertAll(
                () -> assertThat(l.get(ACTION)).isEqualTo(ADDED),
                () -> assertThat(l.get(PARENT_TYPE)).isEqualTo(TYPE_ENTITE),
                () -> assertThat(FixturePatrimoine.guid(l.get(PARENT_ID))).isEqualTo(FixturePatrimoine.guid(entite))));
    }
}

package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.fixtures.ApiProspectionsTerrain;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.Contexte;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.Statut;
import com.tsix.immocrm.fixtures.FixtureTerrains;
import com.tsix.immocrm.fixtures.NettoyageProspectionsTerrain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ApiProspectionsTerrain.corpsProspection;
import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.CorpsJson.corps;
import static com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Audit P0 des prospections terrain — FEATURE-patrimoine-prospections-terrain.md §6.2 et §5.7 : type
 * {@code Yes.Lnd.Prospection} « Prospection terrain », libellé = Description ; Added, Modified [[propriété, ancienne,
 * nouvelle]] (modification, transition), Deleted ; transformation : terrain Added, prospection Modified ; /api/audit rend
 * la trace. Sans {@code @Transactional} : la ligne n'existe qu'après validation ; tout est effacé en {@code @AfterEach}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProspectionTerrainAuditTest {

    private static final String TYPE = "Yes.Lnd.Prospection";
    private static final String ADDED = "Added";
    private static final String MODIFIED = "Modified";
    private static final String OBJECT_TYPE = "ObjectType";
    private static final String DETAILS = "Details";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureProspectionsTerrain fixture;
    @Autowired private FixtureTerrains terrains;
    @Autowired private DataSource dataSource;
    @Autowired private PlatformTransactionManager transactionManager;

    private NettoyageProspectionsTerrain nettoyage;
    private AdmUser admin;
    private ApiProspectionsTerrain api;
    private Contexte c;

    @BeforeEach
    void setUp() {
        nettoyage = new NettoyageProspectionsTerrain(dataSource, transactionManager);
        nettoyage.noterConfigurationAbsente();
        admin = nettoyage.valider(patrimoine::administrateur);
        nettoyage.suivre("AdmUser", admin.getId());
        c = nettoyage.commeAdmin(admin, () -> fixture.contexte("P7H"));
        nettoyage.suivre(c);
        api = new ApiProspectionsTerrain(mockMvc, admin);
    }

    @AfterEach
    void nettoyer() {
        nettoyage.nettoyer();
    }

    private UUID creer(String suffixe) throws Exception {
        UUID id = api.creerOk(corpsProspection(c, suffixe + "-" + c.jeton()));
        nettoyage.suivreProspection(id);
        return id;
    }

    private List<Map<String, Object>> lignes(UUID objet, String action) {
        return nettoyage.jdbc().queryForList("""
                SELECT ObjectType, ObjectTypeLabel, ObjectLabel, CAST(Details AS nvarchar(max)) AS Details
                FROM AdmWebHistorics WHERE ObjectId = ? AND Action = ?""", objet.toString(), action);
    }

    private Map<String, Object> seuleLigne(UUID objet, String action) {
        List<Map<String, Object>> lignes = lignes(objet, action);
        assertThat(lignes).as("lignes %s de %s", action, objet).hasSize(1);
        return lignes.get(0);
    }

    /** Propriétés citées par les Details [[propriété, ancienne, nouvelle], …] d'une ligne. */
    private static List<String> proprietes(Map<String, Object> ligne) {
        List<List<Object>> details = JsonPath.read((String) ligne.get(DETAILS), "$");
        return details.stream().map(d -> String.valueOf(d.get(0))).toList();
    }

    @Test
    void r6_2_creation_added_typeEtLibelleOmran() throws Exception {
        UUID id = creer("AUA");

        assertThat(seuleLigne(id, ADDED))
                .containsEntry(OBJECT_TYPE, TYPE)
                .containsEntry("ObjectTypeLabel", "Prospection terrain")
                .containsEntry("ObjectLabel", "Prospection AUA-" + c.jeton());
    }

    @Test
    void r6_2_modification_modified_proprieteAncienneNouvelle() throws Exception {
        UUID id = creer("AUM");

        api.modifier(id, corps().avec(DESCRIPTION, "Renommee")).andExpect(status().isOk());

        Map<String, Object> ligne = seuleLigne(id, MODIFIED);
        List<List<Object>> details = JsonPath.read((String) ligne.get(DETAILS), "$");
        assertAll(
                () -> assertThat(ligne).containsEntry(OBJECT_TYPE, TYPE),
                () -> assertThat(details).contains(List.of("Description", "Prospection AUM-" + c.jeton(), "Renommee")));
    }

    /** §6.2 : la transition écrit un Modified citant le statut (navigation StrStatus, écart accepté). */
    @Test
    void r6_2_transition_modified_statut() throws Exception {
        UUID id = creer("AUT");

        api.executer(id, T_EN_COURS_VALIDE).andExpect(status().isOk());

        assertThat(proprietes(seuleLigne(id, MODIFIED))).anyMatch(p -> p.startsWith("StrStatus"));
    }

    @Test
    void r6_2_suppression_deleted() throws Exception {
        UUID id = creer("AUD");

        api.supprimer(id).andExpect(status().isNoContent());

        assertThat(seuleLigne(id, "Deleted")).containsEntry(OBJECT_TYPE, TYPE);
    }

    /** §6.2 : types() contient Yes.Lnd.Prospection — /api/audit?objetId= rend la trace (piège P6). */
    @Test
    void r6_2_apiAudit_rendLaTrace() throws Exception {
        UUID id = creer("AUR");

        mockMvc.perform(get("/api/audit").param("objetId", id.toString()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(TYPE)));
    }

    /** §5.7 : transformation → terrain Added (Yes.Stk.LandProjectItem), prospection Modified IsInArchive / IsTransformedToLand. */
    @Test
    void r5_7_transformation_terrainAdded_prospectionModified() throws Exception {
        nettoyage.commeAdmin(admin, () -> {
            patrimoine.referentielArticle();
            terrains.configurationTerrain();
            return null;
        });
        CfgCompany societe = nettoyage.commeAdmin(admin, () -> patrimoine.entite("ENT-" + c.jeton(), true));
        nettoyage.suivre("CfgCompany", societe.getId());
        UUID categorie = nettoyage.commeAdmin(admin,
                () -> patrimoine.categorie("CTE-" + c.jeton(), terrains.natureTer(), false).getId());
        nettoyage.suivre("StkItemCategory", categorie);
        UUID statut = nettoyage.commeAdmin(admin,
                () -> fixture.statut("TRF-" + c.jeton(), 250, TYPE_SUCCES, new Statut(false, true, true, 0)));
        nettoyage.suivre("StrStatus", statut);
        UUID id = nettoyage.commeAdmin(admin, () -> fixture.prospection(c, "AUX-" + c.jeton(), statut));
        nettoyage.suivreProspection(id);

        String json = api.transformer(id, corps().avec(ENTITE_ID, societe.getId())
                        .avec(VOCATION_ID, FixturePatrimoine.VOCATION_PROJET).avec(CATEGORIE_ID, categorie))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID terrain = UUID.fromString(JsonPath.read(json, "$.terrainId"));

        assertAll(
                () -> assertThat(seuleLigne(terrain, ADDED)).containsEntry(OBJECT_TYPE, "Yes.Stk.LandProjectItem"),
                () -> assertThat(proprietes(seuleLigne(id, MODIFIED))).contains("IsInArchive", "IsTransformedToLand"));
    }
}

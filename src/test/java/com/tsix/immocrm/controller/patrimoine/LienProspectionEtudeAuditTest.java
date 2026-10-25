package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.fixtures.ApiFaisabilite;
import com.tsix.immocrm.fixtures.FixtureFaisabilite;
import com.tsix.immocrm.fixtures.FixtureLienProspection;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain;
import com.tsix.immocrm.fixtures.FixtureProspectionsTerrain.Contexte;
import com.tsix.immocrm.fixtures.NettoyageProspectionsTerrain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.ENTITE_ETUDE;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.ETAT_NOUVEAU;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.STATUT_EN_ETUDE;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.fiche;
import static com.tsix.immocrm.fixtures.FixtureLienProspection.LIEN;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * R8 et R9 de FEATURE-patrimoine-etude-prospection.md : lier / délier écrit une trace « Modified » de l'étude, lisible par
 * {@code /api/audit?objetId=<étude>}. Sans {@code @Transactional} : la trace n'existe qu'après validation ; tout ce qui
 * est validé est effacé en {@code @AfterEach} (études d'abord, la FK LndLandId pointe la prospection).
 */
@SpringBootTest
@AutoConfigureMockMvc
class LienProspectionEtudeAuditTest {

    private static final String AUDIT = "/api/audit";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureFaisabilite faisabilite;
    @Autowired private FixtureProspectionsTerrain prospections;
    @Autowired private DataSource dataSource;
    @Autowired private PlatformTransactionManager transactionManager;

    private final List<UUID> etudes = new ArrayList<>();
    private final List<String[]> semisFaisabilite = new ArrayList<>();

    private NettoyageProspectionsTerrain nettoyage;
    private AdmUser admin;
    private ApiFaisabilite api;
    private CfgCompany societe;
    private Contexte c;
    private UUID prospection;

    @BeforeEach
    void setUp() {
        nettoyage = new NettoyageProspectionsTerrain(dataSource, transactionManager);
        nettoyage.noterConfigurationAbsente();
        // Ordre d'effacement : le statut avant son état, l'entité de workflow en dernier.
        noterSemisAbsent("StkFeasibilityStudyStatus", STATUT_EN_ETUDE);
        noterSemisAbsent("AdmWorkFlowStateType", ETAT_NOUVEAU);
        noterSemisAbsent("StrEntity", ENTITE_ETUDE);
        nettoyage.valider(() -> {
            faisabilite.parametrage();
            return null;
        });
        admin = nettoyage.valider(patrimoine::administrateur);
        nettoyage.suivre("AdmUser", admin.getId());
        api = new ApiFaisabilite(mockMvc, admin);
        c = nettoyage.commeAdmin(admin, () -> prospections.contexte("LPA"));
        nettoyage.suivre(c);
        prospection = nettoyage.commeAdmin(admin, () -> prospections.prospection(c, "LPA-" + c.jeton()));
        nettoyage.suivreProspection(prospection);
        societe = nettoyage.commeAdmin(admin, () -> patrimoine.entite("ENT-" + c.jeton(), true));
        nettoyage.suivre("CfgCompany", societe.getId());
    }

    @AfterEach
    void nettoyer() {
        etudes.forEach(this::effacerEtude);
        effacer("DELETE FROM AdmUserHabilitation WHERE AdmUserId = ?", admin.getId().toString());
        nettoyage.nettoyer();
        semisFaisabilite.forEach(s -> effacer("DELETE FROM " + s[0] + " WHERE PKey = ?", s[1]));
    }

    private void effacerEtude(UUID id) {
        String etude = id.toString();
        effacer("DELETE FROM StkFeasibilityStudyStatusHistory WHERE StkFeasibilityStudyId = ?", etude);
        effacer("DELETE FROM AdmWebHistorics WHERE ObjectId = ?", etude);
        effacer("DELETE FROM StkFeasibilityStudy WHERE PKey = ?", etude);
    }

    /** Nettoyage au mieux : une table absente ne doit pas faire échouer un test déjà jugé. */
    private void effacer(String sql, String parametre) {
        try {
            nettoyage.jdbc().update(sql, parametre);
        } catch (DataAccessException e) {
            // ligne déjà absente ou table hors schéma : rien à effacer
        }
    }

    private void noterSemisAbsent(String table, UUID id) {
        try {
            Integer n = nettoyage.jdbc().queryForObject("SELECT COUNT(*) FROM " + table + " WHERE PKey = ?", Integer.class,
                    id.toString());
            if (n != null && n == 0) {
                semisFaisabilite.add(new String[]{table, id.toString()});
            }
        } catch (DataAccessException e) {
            // table absente : rien à semer ni à effacer
        }
    }

    private UUID etude(String suffixe) throws Exception {
        UUID id = api.etude(fiche(suffixe + "-" + c.jeton(), societe.getId()));
        etudes.add(id);
        return id;
    }

    private void lier(UUID etude) throws Exception {
        mockMvc.perform(put(LIEN, etude).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                        .content(FixtureLienProspection.lien(prospection)))
                .andExpect(status().isOk());
    }

    @Test
    void r8_lier_traceModifiedDeLEtude_lisibleParApiAudit() throws Exception {
        UUID id = etude("LAL");

        lier(id);

        mockMvc.perform(get(AUDIT).param("objetId", id.toString()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].action").value(hasItem("Modified")))
                .andExpect(content().string(containsString("LndLand")));
    }

    @Test
    void r8_delier_traceModifiedDeLEtude_lisibleParApiAudit() throws Exception {
        UUID id = etude("LAD");
        lier(id);
        nettoyage.jdbc().update("DELETE FROM AdmWebHistorics WHERE ObjectId = ? AND Action = 'Modified'", id.toString());

        mockMvc.perform(delete(LIEN, id).with(connecte(admin))).andExpect(status().isOk());

        mockMvc.perform(get(AUDIT).param("objetId", id.toString()).with(connecte(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].action").value(hasItem("Modified")));
    }
}

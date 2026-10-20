package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.entities.cfg.CfgProject;
import com.tsix.immocrm.entities.cfg.CfgTranche;
import com.tsix.immocrm.fixtures.ApiFaisabilite;
import com.tsix.immocrm.fixtures.FixtureFaisabilite;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.security.AuthenticatedUser;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static com.tsix.immocrm.fixtures.ApiFaisabilite.d;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Audit P0 des études — FEATURE-patrimoine-faisabilite.md §2.6 : une ligne {@code AdmWebHistorics} « Added » par type,
 * {@code ObjectType} mesuré (ou nom de classe OMRAN à défaut), sans parent. Sans {@code @Transactional} : la ligne n'existe
 * qu'après validation ; tout ce qui est validé est effacé en {@code @AfterEach}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EtudeFaisabiliteAuditTest {

    private static final Logger log = LoggerFactory.getLogger(EtudeFaisabiliteAuditTest.class);
    private static final String ADDED = "Added";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureFaisabilite fixture;
    @Autowired private DataSource dataSource;
    @Autowired private PlatformTransactionManager transactionManager;

    private final String jeton = jeton();
    private final Deque<String[]> creees = new ArrayDeque<>();
    private final List<UUID> etudes = new ArrayList<>();
    private final List<String[]> semis = new ArrayList<>();

    private JdbcTemplate jdbc;
    private TransactionTemplate transactions;
    private AdmUser admin;
    private ApiFaisabilite api;
    private CfgCompany societe;
    private CfgTranche tranche;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        transactions = new TransactionTemplate(transactionManager);
        // Ordre d'effacement : le statut avant son état, l'entité de workflow en dernier.
        noterSemisAbsent("StkFeasibilityStudyStatus", STATUT_EN_ETUDE);
        noterSemisAbsent("AdmWorkFlowStateType", ETAT_NOUVEAU);
        noterSemisAbsent("StrEntity", ENTITE_ETUDE);
        transactions.executeWithoutResult(s -> fixture.parametrage());
        admin = patrimoine.administrateur();
        suivre("AdmUser", admin.getId());
        api = new ApiFaisabilite(mockMvc, admin);
        societe = commeAdmin(() -> patrimoine.entite("ENT-" + jeton, true));
        suivre("CfgCompany", societe.getId());
        CfgProject projet = commeAdmin(() -> patrimoine.projet("PRJ-" + jeton, societe));
        suivre("CfgProject", projet.getId());
        tranche = commeAdmin(() -> patrimoine.tranche("TR-" + jeton, projet));
        suivre("CfgTranche", tranche.getId());
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
        etudes.forEach(this::effacerEtude);
        while (!creees.isEmpty()) {
            String[] ligne = creees.pop();
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId = ?", ligne[1]);
            effacer("DELETE FROM " + ligne[0] + " WHERE PKey = ?", ligne[1]);
        }
        effacer("DELETE FROM AdmUserHabilitation WHERE AdmUserId = ?", admin.getId().toString());
        semis.forEach(s -> effacer("DELETE FROM " + s[0] + " WHERE PKey = ?", s[1]));
    }

    private void effacerEtude(UUID id) {
        String etude = id.toString();
        for (String table : List.of("StkFeasibilityStudyCfgTrancheStkBaseUnit", "StkFeasibilityStudyCfgTranche",
                "StkFSBaseUnitStkFeasibilityStudy")) {
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId IN (SELECT PKey FROM " + table + " WHERE FeasibilityStudyId = ?)",
                    etude);
            effacer("DELETE FROM " + table + " WHERE FeasibilityStudyId = ?", etude);
        }
        effacer("DELETE FROM StkFeasibilityStudyStatusHistory WHERE StkFeasibilityStudyId = ?", etude);
        effacer("DELETE FROM AdmWebHistorics WHERE ObjectId = ?", etude);
        effacer("DELETE FROM StkFeasibilityStudy WHERE PKey = ?", etude);
    }

    /** Nettoyage au mieux : une table absente (base sans schéma 122) ne doit pas faire échouer un test déjà jugé. */
    private void effacer(String sql, String parametre) {
        try {
            jdbc.update(sql, parametre);
        } catch (DataAccessException e) {
            log.warn("Nettoyage incomplet ({}) : {}", sql, e.getMessage());
        }
    }

    private void noterSemisAbsent(String table, UUID id) {
        try {
            Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE PKey = ?", Integer.class, id.toString());
            if (n != null && n == 0) {
                semis.add(new String[]{table, id.toString()});
            }
        } catch (DataAccessException e) {
            log.warn("Table de semis absente ({}) : {}", table, e.getMessage());
        }
    }

    private void suivre(String table, UUID id) {
        creees.push(new String[]{table, id.toString()});
    }

    private <T> T commeAdmin(Supplier<T> ecriture) {
        AuthenticatedUser principal = new AuthenticatedUser(admin.getId(), admin.getCode(), null, false, true, 0);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Administrateur"))));
        return transactions.execute(statut -> ecriture.get());
    }

    private UUID etude() throws Exception {
        UUID id = api.etude(yestest(FixtureFaisabilite.fiche("AUD-" + jeton, societe.getId())));
        etudes.add(id);
        return id;
    }

    private UUID unite(String code) throws Exception {
        UUID id = api.unite(code, false, null);
        suivre("StkFSBaseUnit", id);
        return id;
    }

    private UUID ligneUnite(UUID etude, UUID unite) {
        return UUID.fromString(jdbc.queryForObject("SELECT CONVERT(varchar(36), PKey) FROM StkFSBaseUnitStkFeasibilityStudy "
                + "WHERE FeasibilityStudyId = ? AND StkFSBaseUnitId = ?", String.class, etude.toString(), unite.toString()));
    }

    private UUID uneLigne(String sql, UUID etude) {
        return UUID.fromString(jdbc.queryForObject(sql, String.class, etude.toString()));
    }

    private Map<String, Object> ajout(UUID objet) {
        List<Map<String, Object>> lignes = jdbc.queryForList("""
                SELECT ObjectType, ObjectTypeLabel, ObjectLabel, ParentObjectId FROM AdmWebHistorics
                WHERE ObjectId = ? AND Action = ?""", objet.toString(), ADDED);
        assertThat(lignes).as("lignes Added de %s", objet).hasSize(1);
        return lignes.get(0);
    }

    /** Étude avec une unité ventilée sur une tranche : les quatre types écrits. */
    private UUID etudeAvecGrilles(UUID unite) throws Exception {
        UUID etude = etude();
        api.unites(etude, lignes(ligneUniteCorps(1, unite, d("100")))).andExpect(status().isOk());
        api.tranches(etude, lignes(tranche(ligneTrancheCorps("Tranche 1", 1, tranche.getId(), d("1")),
                ventilation(ligneUnite(etude, unite), d("100"))))).andExpect(status().isOk());
        return etude;
    }

    // ------------------------------------------------------------------ un test Added par type

    @Test
    void r2_6_etude_added_etudeDeFaisabilite_libelleDescription() throws Exception {
        UUID id = etude();

        assertThat(ajout(id))
                .containsEntry("ObjectType", "Yes.Stk.StkFeasibilityStudy")
                .containsEntry("ObjectTypeLabel", "Etude de faisabilité")
                .containsEntry("ObjectLabel", "Etude AUD-" + jeton)
                .containsEntry("ParentObjectId", null);
    }

    @Test
    void r2_6_ligneDUnite_added_prevision_libelleCodeDeLUnite() throws Exception {
        UUID unite = unite("UAU-" + jeton);
        UUID etude = etudeAvecGrilles(unite);

        assertThat(ajout(ligneUnite(etude, unite)))
                .containsEntry("ObjectType", "Yes.Stk.BalanceSheet.FSBaseUnitStkFeasibilityStudy")
                .containsEntry("ObjectTypeLabel", "Prévision")
                .containsEntry("ObjectLabel", "UAU-" + jeton)
                .containsEntry("ParentObjectId", null);
    }

    @Test
    void r2_6_trancheDEtude_added_libelleConstantTranche() throws Exception {
        UUID etude = etudeAvecGrilles(unite("UTR-" + jeton));

        assertThat(ajout(uneLigne("SELECT CONVERT(varchar(36), PKey) FROM StkFeasibilityStudyCfgTranche "
                + "WHERE FeasibilityStudyId = ?", etude)))
                .containsEntry("ObjectType", "Yes.Stk.BalanceSheet.FeasibilityStudyCfgTranche")
                .containsEntry("ObjectTypeLabel", "Tranche")
                .containsEntry("ObjectLabel", "Tranche")
                .containsEntry("ParentObjectId", null);
    }

    @Test
    void r2_6_ventilation_added_nomDeClasseOmran() throws Exception {
        UUID etude = etudeAvecGrilles(unite("UVE-" + jeton));

        assertThat(ajout(uneLigne("SELECT CONVERT(varchar(36), PKey) FROM StkFeasibilityStudyCfgTrancheStkBaseUnit "
                + "WHERE FeasibilityStudyId = ?", etude)))
                .containsEntry("ObjectType", "Yes.Stk.BalanceSheet.FeasibilityStudyCfgTrancheStkBaseUnit")
                .containsEntry("ParentObjectId", null);
    }

    @Test
    void r2_6_uniteDeBase_added_nomDeClasseOmran() throws Exception {
        UUID unite = unite("UCF-" + jeton);

        assertThat(ajout(unite)).containsEntry("ObjectType", "Yes.Stk.BalanceSheet.FSBaseUnit");
    }

    @Test
    void r2_6_fonctionSpecifique_added_nomDeClasseOmran() throws Exception {
        UUID fonction = api.fonction("FCF-" + jeton, true, false, false, false, false);
        suivre("SpecificFunction", fonction);

        assertThat(ajout(fonction)).containsEntry("ObjectType", "Yes.Stk.Config.SpecificFunction");
    }
}

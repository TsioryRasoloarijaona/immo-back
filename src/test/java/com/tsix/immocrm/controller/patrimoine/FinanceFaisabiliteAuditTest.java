package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.entities.cfg.CfgCompany;
import com.tsix.immocrm.fixtures.ApiFaisabilite;
import com.tsix.immocrm.fixtures.CorpsJson;
import com.tsix.immocrm.fixtures.FixtureFaisabilite;
import com.tsix.immocrm.fixtures.FixtureFaisabiliteFinance;
import com.tsix.immocrm.fixtures.FixtureFaisabiliteFinance.Categories;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static com.tsix.immocrm.fixtures.ApiFaisabilite.d;
import static com.tsix.immocrm.fixtures.FixtureFaisabilite.*;
import static com.tsix.immocrm.fixtures.FixtureFaisabiliteFinance.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Audit P0 des écritures P8b-1 — FEATURE-patrimoine-faisabilite-workflow.md §0 (audit de toute table écrite), §3 et décision 9 :
 * {@code FSFunding} et {@code FSReimbursement} Added/Modified/Deleted, {@code FSPrevision} audité par ligne modifiée, lignes
 * dérivées ({@code StkFSPrevisionStkTranche}, {@code StkFSPrevisionLine}) jamais auditées. Sans {@code @Transactional} :
 * l'audit n'existe qu'après validation ; tout ce qui est validé est effacé en {@code @AfterEach}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FinanceFaisabiliteAuditTest {

    private static final Logger log = LoggerFactory.getLogger(FinanceFaisabiliteAuditTest.class);
    private static final String AUDIT = "SELECT COUNT(*) FROM AdmWebHistorics WHERE ObjectId = ? AND Action = ?";

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureFaisabilite fixture;
    @Autowired private FixtureFaisabiliteFinance finance;
    @Autowired private DataSource dataSource;
    @Autowired private PlatformTransactionManager transactionManager;

    private final String jeton = jeton();
    private final List<UUID> categories = new ArrayList<>();
    private final List<UUID> racinesCreees = new ArrayList<>();

    private JdbcTemplate jdbc;
    private TransactionTemplate transactions;
    private AdmUser admin;
    private ApiFaisabilite api;
    private CfgCompany societe;
    private Categories cat;
    private UUID etude;
    private UUID uniteBase;

    @BeforeEach
    void setUp() throws Exception {
        jdbc = new JdbcTemplate(dataSource);
        transactions = new TransactionTemplate(transactionManager);
        for (UUID racine : List.of(RACINE_DEPENSE, RACINE_RECETTE)) {
            if (compter("SELECT COUNT(*) FROM StlCategory WHERE PKey = ?", racine.toString()) == 0) {
                racinesCreees.add(racine);
            }
        }
        transactions.executeWithoutResult(s -> fixture.parametrage());
        admin = patrimoine.administrateur();
        api = new ApiFaisabilite(mockMvc, admin);
        societe = commeAdmin(() -> patrimoine.entite("ENT-" + jeton, true));
        cat = transactions.execute(s -> finance.categoriesDeTest(jeton));
        categories.addAll(List.of(cat.grosOeuvres(), cat.architecte(), cat.travaux(), cat.honoraires(), cat.divers(),
                cat.sansPrevision(), cat.ventes()));
        SecurityContextHolder.clearContext();
        etude = api.etude(phases(fiche("AUD-" + jeton, societe.getId()), "2026-01-01", 0, 0, 0)
                .avec(ECART_TYPE_TRAVAUX, d("1")));
        uniteBase = api.unite("UAU-" + jeton, false, null);
        api.unites(etude, lignes(ligneUniteCorps(1, uniteBase, d("1")))).andExpect(status().isOk());
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
        String id = etude == null ? null : etude.toString();
        if (id != null) {
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId IN (SELECT r.PKey FROM FSReimbursement r JOIN FSFunding f "
                    + "ON f.PKey = r.FSFundingId WHERE f.FeasibilityStudyId = ?)", id);
            effacer("DELETE FROM FSReimbursement WHERE FSFundingId IN (SELECT PKey FROM FSFunding WHERE FeasibilityStudyId = ?)", id);
            for (String table : List.of("FSFunding", "FSPrevision", "StkFSPrevisionStkTranche",
                    "StkFSBaseUnitStkFeasibilityStudy", "StkFeasibilityStudyCfgTranche")) {
                effacer("DELETE FROM AdmWebHistorics WHERE ObjectId IN (SELECT PKey FROM " + table
                        + " WHERE FeasibilityStudyId = ?)", id);
                effacer("DELETE FROM " + table + " WHERE FeasibilityStudyId = ?", id);
            }
            effacer("DELETE FROM StkFSPrevisionLine WHERE StkFeasibilityStudyId = ?", id);
            effacer("DELETE FROM StkFeasibilityStudyStatusHistory WHERE StkFeasibilityStudyId = ?", id);
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId = ?", id);
            effacer("DELETE FROM StkFeasibilityStudy WHERE PKey = ?", id);
        }
        if (uniteBase != null) {
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId = ?", uniteBase.toString());
            effacer("DELETE FROM StkFSBaseUnit WHERE PKey = ?", uniteBase.toString());
        }
        categories.forEach(c -> effacer("DELETE FROM StlCategory WHERE PKey = ?", c.toString()));
        racinesCreees.forEach(c -> effacer("DELETE FROM StlCategory WHERE PKey = ?", c.toString()));
        if (societe != null) {
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId = ?", societe.getId().toString());
            effacer("DELETE FROM CfgCompany WHERE PKey = ?", societe.getId().toString());
        }
        effacer("DELETE FROM AdmUserHabilitation WHERE AdmUserId = ?", admin.getId().toString());
        effacer("DELETE FROM AdmUser WHERE PKey = ?", admin.getId().toString());
    }

    /** Nettoyage au mieux : une table absente ne doit pas faire échouer un test déjà jugé. */
    private void effacer(String sql, String parametre) {
        try {
            jdbc.update(sql, parametre);
        } catch (DataAccessException e) {
            log.warn("Nettoyage incomplet ({}) : {}", sql, e.getMessage());
        }
    }

    private int compter(String sql, Object... parametres) {
        Integer n = jdbc.queryForObject(sql, Integer.class, parametres);
        return n == null ? 0 : n;
    }

    private int audit(String objet, String action) {
        return compter(AUDIT, objet, action);
    }

    private <T> T commeAdmin(Supplier<T> ecriture) {
        AuthenticatedUser principal = new AuthenticatedUser(admin.getId(), admin.getCode(), null, false, true, 0);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Administrateur"))));
        return transactions.execute(statut -> ecriture.get());
    }

    private String pkey(String sql) {
        return jdbc.queryForObject(sql, String.class, etude.toString());
    }

    private String financements(CorpsJson... credits) throws Exception {
        return api.remplacer(FINANCEMENTS, etude, lignes(credits)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    // ------------------------------------------------------------------ crédits et remboursements

    @Test
    void r1_2_creditEtRemboursement_added_modified_deleted() throws Exception {
        String cree = financements(credit("2026-01-15", "1000", 1, 1, 0, "0")
                .avec(REMBOURSEMENTS, List.of(remboursement("2026-01-15", "1000", "0"))));
        String credit = JsonPath.read(cree, "$[0].id");
        String rembourse = JsonPath.read(cree, "$[0].remboursements[0].id");
        int ajoutCredit = audit(credit, "Added");
        int ajoutRemboursement = audit(rembourse, "Added");

        financements(credit("2026-01-15", "2000", 1, 1, 0, "0").avec(ID, UUID.fromString(credit))
                .avec(REMBOURSEMENTS, List.of(remboursement("2026-01-15", "1000", "0").avec(ID, UUID.fromString(rembourse)))));
        int modifCredit = audit(credit, "Modified");
        financements();

        assertAll(
                () -> assertThat(ajoutCredit).as("crédit Added").isEqualTo(1),
                () -> assertThat(ajoutRemboursement).as("remboursement Added").isEqualTo(1),
                () -> assertThat(modifCredit).as("crédit Modified").isEqualTo(1),
                () -> assertThat(audit(credit, "Deleted")).as("crédit Deleted").isEqualTo(1),
                () -> assertThat(audit(rembourse, "Deleted")).as("remboursement Deleted").isEqualTo(1));
    }

    // ------------------------------------------------------------------ prévisions (décision 9)

    /** Seule la ligne FSPrevision dont la saisie change est auditée « Modified » ; son ancêtre inchangé ne l'est pas. */
    @Test
    void d9_prevision_auditeeParLigneModifiee_seulement() throws Exception {
        UUID ligneUnite = UUID.fromString(pkey("SELECT CONVERT(varchar(36), PKey) FROM StkFSBaseUnitStkFeasibilityStudy "
                + "WHERE FeasibilityStudyId = ?"));
        api.remplacer(PREVISIONS, etude, lignes(parUnite(cat.grosOeuvres(), ligneUnite, "1000", "0"))).andExpect(status().isOk());
        String go = pkey("SELECT CONVERT(varchar(36), PKey) FROM FSPrevision WHERE FeasibilityStudyId = ? AND StlCategoryId = '"
                + cat.grosOeuvres() + "'");
        String travaux = pkey("SELECT CONVERT(varchar(36), PKey) FROM FSPrevision WHERE FeasibilityStudyId = ? "
                + "AND StlCategoryId = '" + cat.travaux() + "'");
        int ajout = audit(go, "Added");

        api.remplacer(PREVISIONS, etude, lignes(parUnite(cat.grosOeuvres(), ligneUnite, "2000", "0"))).andExpect(status().isOk());

        assertAll(
                () -> assertThat(ajout).as("Added").isEqualTo(1),
                () -> assertThat(audit(go, "Modified")).as("ligne modifiée").isEqualTo(1),
                () -> assertThat(audit(travaux, "Modified")).as("ancêtre inchangé").isZero());
    }

    /** §3 : les lignes dérivées (par tranche, trésorerie) ne sont jamais auditées. */
    @Test
    void r3_lignesDerivees_nonAuditees() throws Exception {
        UUID ligneUnite = UUID.fromString(pkey("SELECT CONVERT(varchar(36), PKey) FROM StkFSBaseUnitStkFeasibilityStudy "
                + "WHERE FeasibilityStudyId = ?"));

        api.remplacer(PREVISIONS, etude, lignes(parUnite(cat.grosOeuvres(), ligneUnite, "1000", "0"))).andExpect(status().isOk());

        assertAll(
                () -> assertThat(compter("SELECT COUNT(*) FROM StkFSPrevisionLine WHERE StkFeasibilityStudyId = ?",
                        etude.toString())).as("une ligne Expense au moins").isPositive(),
                () -> assertThat(compter("SELECT COUNT(*) FROM AdmWebHistorics WHERE ObjectId IN (SELECT PKey FROM "
                        + "StkFSPrevisionLine WHERE StkFeasibilityStudyId = ?)", etude.toString())).isZero(),
                () -> assertThat(compter("SELECT COUNT(*) FROM AdmWebHistorics WHERE ObjectId IN (SELECT PKey FROM "
                        + "StkFSPrevisionStkTranche WHERE FeasibilityStudyId = ?)", etude.toString())).isZero());
    }
}

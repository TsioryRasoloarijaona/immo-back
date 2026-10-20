package com.tsix.immocrm.controller.patrimoine;

import com.jayway.jsonpath.JsonPath;
import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureTerrains;
import com.tsix.immocrm.fixtures.FixtureTerrains.Contexte;
import com.tsix.immocrm.fixtures.FixtureTerrains.TypeFrais;
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureTerrains.VALIDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * « Valider pour règlement » en transactions réelles — FEATURE-patrimoine-terrains.md §3 (« une seule transaction »),
 * §3.3 (incrément dans la transaction unique), §3.2 (audit : exactement 2 lignes), §6.2 (rollback complet, test de
 * concurrence). Sans {@code @Transactional} : chaque appel valide ou annule pour de bon ; tout est effacé en
 * {@code @AfterEach}. La panne est provoquée par un trigger de test (après l'INSERT de la facture, ou après la mise à
 * jour du frais) qui ne vise que les frais dont les notes portent {@value #MARQUEUR}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ValiderFraisTerrainAtomiciteTest {

    private static final Logger log = LoggerFactory.getLogger(ValiderFraisTerrainAtomiciteTest.class);

    private static final String MARQUEUR = "PANNE-P7A";
    private static final String TRIGGER_FACTURE = "TRG_TEST_P7A_PANNE_FACTURE";
    private static final String TRIGGER_FRAIS = "TRG_TEST_P7A_PANNE_FRAIS";
    private static final String CREER_TRIGGER_FACTURE = """
            CREATE OR ALTER TRIGGER dbo.TRG_TEST_P7A_PANNE_FACTURE ON dbo.BuyDocument AFTER INSERT AS
            BEGIN
                SET NOCOUNT ON;
                IF EXISTS (SELECT 1 FROM inserted i JOIN dbo.StkExpense e ON e.Pkey = i.StkExpenseId
                           WHERE e.Notes LIKE N'%PANNE-P7A%')
                    THROW 50001, N'Panne de test apres l''ecriture de la facture', 1;
            END""";
    private static final String CREER_TRIGGER_FRAIS = """
            CREATE OR ALTER TRIGGER dbo.TRG_TEST_P7A_PANNE_FRAIS ON dbo.StkExpense AFTER UPDATE AS
            BEGIN
                SET NOCOUNT ON;
                IF EXISTS (SELECT 1 FROM inserted WHERE IsValidated = 1 AND Notes LIKE N'%PANNE-P7A%')
                    THROW 50002, N'Panne de test apres la validation du frais', 1;
            END""";
    private static final String NB_FACTURES = "SELECT COUNT(*) FROM BuyDocument WHERE StkExpenseId = ?";
    private static final String VALIDE = "SELECT CAST(IsValidated AS int) FROM StkExpense WHERE Pkey = ?";
    private static final String SEQUENCE =
            "SELECT TOP 1 SeqValue FROM AdmSequence WHERE Code = N'Facture fournisseur' ORDER BY PKey ASC";
    private static final BigDecimal MONTANT = new BigDecimal("8000");

    @Autowired private MockMvc mockMvc;
    @Autowired private FixturePatrimoine fixture;
    @Autowired private FixtureTerrains terrains;
    @Autowired private DataSource dataSource;
    @Autowired private PlatformTransactionManager transactionManager;

    private JdbcTemplate jdbc;
    private AdmUser admin;
    private Contexte c;
    private UUID terrain;
    private TypeFrais type;
    private UUID fournisseur;
    private UUID temoin;
    private UUID panne;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        TransactionTemplate transactions = new TransactionTemplate(transactionManager);
        admin = transactions.execute(statut -> fixture.administrateur());
        AuthenticatedUser principal = new AuthenticatedUser(admin.getId(), admin.getCode(), null, false, true, 0);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Administrateur"))));
        transactions.executeWithoutResult(statut -> {
            c = terrains.contexte("P7X");
            terrains.configurationFacture();
            terrain = terrains.terrain(c, "TER-" + c.jeton());
            type = terrains.typeFrais(c.jeton(), true, true);
            fournisseur = terrains.fournisseur("FRN-" + c.jeton(), null);
            temoin = terrains.frais(terrain, type.id(), fournisseur, MONTANT, false);
            panne = terrains.frais(terrain, type.id(), fournisseur, MONTANT, false, MARQUEUR + "-" + c.jeton());
        });
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
        effacer("DROP TRIGGER IF EXISTS dbo." + TRIGGER_FACTURE);
        effacer("DROP TRIGGER IF EXISTS dbo." + TRIGGER_FRAIS);
        String factures = "SELECT PKey FROM BuyDocument WHERE StkExpenseId IN (SELECT Pkey FROM StkExpense WHERE StkItemId = ?)";
        String terrainId = terrain.toString();
        effacer("DELETE FROM AdmWebHistorics WHERE ObjectId IN (" + factures + ")", terrainId);
        effacer("DELETE FROM AdmWebHistorics WHERE ObjectId IN (SELECT Pkey FROM StkExpense WHERE StkItemId = ?)",
                terrainId);
        for (String enfant : List.of("BuyDocumentStatusHistory", "BuyVentilationCategory", "BuyDocumentLine")) {
            effacer("DELETE FROM " + enfant + " WHERE BuyDocumentId IN (" + factures + ")", terrainId);
        }
        effacer("DELETE FROM BuyDocument WHERE StkExpenseId IN (SELECT Pkey FROM StkExpense WHERE StkItemId = ?)",
                terrainId);
        effacer("DELETE FROM StkExpense WHERE StkItemId = ?", terrainId);
        List<String[]> lignes = List.of(new String[]{"StkItem", terrainId},
                new String[]{"StkExpenseType", type.id().toString()},
                new String[]{"StlCategory", type.categorieComptable().toString()},
                new String[]{"PrjMarketType", type.typeMarche().toString()},
                new String[]{"CfgTier", fournisseur.toString()},
                new String[]{"AdmCity", c.ville().toString()},
                new String[]{"AdmCountry", c.pays().getId().toString()},
                new String[]{"StkItemCategory", c.categorie().getId().toString()},
                new String[]{"StkItemType", c.type().getId().toString()},
                new String[]{"CfgCompany", c.entite().getId().toString()},
                new String[]{"AdmUser", admin.getId().toString()});
        for (String[] ligne : lignes) {
            effacer("DELETE FROM AdmWebHistorics WHERE ObjectId = ? OR ParentObjectId = ?", ligne[1], ligne[1]);
            effacer("DELETE FROM " + ligne[0] + " WHERE PKey = ?", ligne[1]);
        }
    }

    /** Nettoyage au mieux : une table absente de yesRemsTest avant le lot ne doit pas faire échouer un test jugé. */
    private void effacer(String sql, Object... parametres) {
        try {
            jdbc.update(sql, parametres);
        } catch (DataAccessException e) {
            log.warn("Nettoyage incomplet ({}) : {}", sql, e.getMessage());
        }
    }

    private int valider(UUID fraisId) throws Exception {
        return mockMvc.perform(post(VALIDER, terrain, fraisId).with(connecte(admin))).andReturn().getResponse()
                .getStatus();
    }

    private int compter(String sql, Object... parametres) {
        Integer n = jdbc.queryForObject(sql, Integer.class, parametres);
        return n == null ? 0 : n;
    }

    private int sequence() {
        return compter(SEQUENCE);
    }

    /** Témoin : la route valide un frais sans marqueur — prouve que le refus suivant vient de la panne. */
    private int validerLeTemoin() throws Exception {
        mockMvc.perform(post(VALIDER, terrain, temoin).with(connecte(admin))).andExpect(status().isOk());
        return sequence();
    }

    private void verifierPanneSansTrace(int sequenceAvant) {
        String id = panne.toString();
        assertAll(
                () -> assertThat(compter(NB_FACTURES, id)).as("facture du frais en panne").isZero(),
                () -> assertThat(compter(VALIDE, id)).as("IsValidated du frais en panne").isZero(),
                () -> assertThat(sequence()).as("SeqValue").isEqualTo(sequenceAvant),
                () -> assertThat(compter("SELECT COUNT(*) FROM BuyDocumentLine WHERE Description = ?",
                        "Frais : " + type.description())).as("lignes de facture").isEqualTo(1),
                () -> assertThat(compter("SELECT COUNT(*) FROM BuyVentilationCategory WHERE StlCategoryId = ?",
                        type.categorieComptable().toString())).as("ventilations").isEqualTo(1),
                () -> assertThat(compter("SELECT COUNT(*) FROM BuyDocumentStatusHistory WHERE AdmUserId = ?",
                        admin.getId().toString())).as("historiques").isEqualTo(1));
    }

    // ------------------------------------------------------------------ §3 une seule transaction

    /** §3 / §6.2 : panne après l'écriture de la facture → rollback complet, SeqValue inchangé (aucun trou). */
    @Test
    void r3_panneApresLaFacture_rollbackComplet_sequenceInchangee() throws Exception {
        int avant = validerLeTemoin();
        jdbc.execute(CREER_TRIGGER_FACTURE);

        int statut = valider(panne);

        assertThat(statut).as("la panne ne peut pas répondre 2xx").isGreaterThanOrEqualTo(400);
        verifierPanneSansTrace(avant);
    }

    /** §3 : défaut OMRAN non reproduit — si IsValidated ne peut pas être posé, la facture n'existe pas non plus. */
    @Test
    void r3_panneALaValidationDuFrais_factureAnnulee_sequenceInchangee() throws Exception {
        int avant = validerLeTemoin();
        jdbc.execute(CREER_TRIGGER_FRAIS);

        int statut = valider(panne);

        assertThat(statut).as("la panne ne peut pas répondre 2xx").isGreaterThanOrEqualTo(400);
        verifierPanneSansTrace(avant);
    }

    // ------------------------------------------------------------------ §3.1.1 / §3.1.2 concurrence

    /** §3.1.1 verrou PESSIMISTIC_WRITE : deux validations simultanées du même frais → un 200, un 409, une facture. */
    @Test
    void r3_1_2_deuxValidationsSimultanees_unSeulSucces_uneSeuleFacture() throws Exception {
        int avant = sequence();
        CountDownLatch depart = new CountDownLatch(1);
        Callable<Integer> appel = () -> {
            if (!depart.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("départ non donné");
            }
            return valider(temoin);
        };
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Integer> statuts = new ArrayList<>();
        try {
            Future<Integer> premier = pool.submit(appel);
            Future<Integer> second = pool.submit(appel);
            depart.countDown();
            statuts.add(premier.get(60, TimeUnit.SECONDS));
            statuts.add(second.get(60, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }

        String id = temoin.toString();
        assertAll(
                () -> assertThat(statuts).containsExactlyInAnyOrder(200, 409),
                () -> assertThat(compter(NB_FACTURES, id)).isEqualTo(1),
                () -> assertThat(compter(VALIDE, id)).isEqualTo(1),
                () -> assertThat(sequence()).isEqualTo(avant + 1));
    }

    // ------------------------------------------------------------------ §3.2 audit P0

    /** §3.2 : exactement 2 lignes AdmWebHistorics — la facture (Added, libellé = code) puis le frais (Modified). */
    @Test
    void r3_2_audit_deuxLignes_factureAddedEtFraisModified() throws Exception {
        String json = mockMvc.perform(post(VALIDER, terrain, temoin).with(connecte(admin)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String factureId = JsonPath.read(json, "$.factureId");
        String code = JsonPath.read(json, "$.code");

        List<Map<String, Object>> facture = jdbc.queryForList("""
                SELECT ObjectType, ObjectTypeLabel, Action, ObjectLabel FROM AdmWebHistorics WHERE ObjectId = ?""",
                factureId);
        List<Map<String, Object>> frais = jdbc.queryForList("""
                SELECT ObjectType, ObjectTypeLabel, Action, CAST(Details AS nvarchar(max)) AS Details
                FROM AdmWebHistorics WHERE ObjectId = ?""", temoin.toString());
        int compagnes = compter("""
                SELECT COUNT(*) FROM AdmWebHistorics WHERE ObjectId IN (
                    SELECT Pkey FROM BuyDocumentLine WHERE BuyDocumentId = ?
                    UNION SELECT PKey FROM BuyVentilationCategory WHERE BuyDocumentId = ?
                    UNION SELECT PKey FROM BuyDocumentStatusHistory WHERE BuyDocumentId = ?)""",
                factureId, factureId, factureId);
        assertAll(
                () -> assertThat(facture).singleElement().satisfies(l -> assertThat(l)
                        .containsEntry("ObjectType", "Yes.Stl.Debit.Invoice")
                        .containsEntry("ObjectTypeLabel", "Facture")
                        .containsEntry("Action", "Added")
                        .containsEntry("ObjectLabel", code)),
                () -> assertThat(frais).singleElement().satisfies(l -> assertThat(l)
                        .containsEntry("ObjectType", "Yes.Stk.Expense.Expense")
                        .containsEntry("ObjectTypeLabel", "Frais de Terrain")
                        .containsEntry("Action", "Modified")
                        .containsEntry("Details", "[[\"IsValidated\",\"False\",\"True\"]]")),
                () -> assertThat(compagnes).as("lignes compagnes hors registre").isZero());
    }
}

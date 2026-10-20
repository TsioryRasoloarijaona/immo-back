package com.tsix.immocrm.controller.patrimoine;

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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureTerrains.FRAIS;
import static com.tsix.immocrm.fixtures.FixtureTerrains.VALIDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Revue (2e tour) du verrou des frais au PUT (FEATURE-patrimoine-terrains.md §2, §3.1.1) : PUT et validation concurrents
 * sur le même frais, en transactions réelles. Un trigger de test ralentit l'une des deux transactions pendant qu'elle tient
 * ses verrous ; quel que soit l'ordre, la facture porte le montant du frais, {@code IsValidated} reste 1, aucun interblocage.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FraisTerrainVerrouRevueTest {

    private static final Logger log = LoggerFactory.getLogger(FraisTerrainVerrouRevueTest.class);

    private static final String MARQUEUR = "LENT-P7A";
    private static final String TRIGGER_FACTURE = "TRG_TEST_P7A_LENT_FACTURE";
    private static final String TRIGGER_FRAIS = "TRG_TEST_P7A_LENT_FRAIS";
    private static final String CREER_TRIGGER_FACTURE = """
            CREATE OR ALTER TRIGGER dbo.TRG_TEST_P7A_LENT_FACTURE ON dbo.BuyDocument AFTER INSERT AS
            BEGIN
                SET NOCOUNT ON;
                IF EXISTS (SELECT 1 FROM inserted i JOIN dbo.StkExpense e ON e.Pkey = i.StkExpenseId
                           WHERE e.Notes LIKE N'%LENT-P7A%')
                    WAITFOR DELAY '00:00:03';
            END""";
    private static final String CREER_TRIGGER_FRAIS = """
            CREATE OR ALTER TRIGGER dbo.TRG_TEST_P7A_LENT_FRAIS ON dbo.StkExpense AFTER UPDATE AS
            BEGIN
                SET NOCOUNT ON;
                IF EXISTS (SELECT 1 FROM inserted WHERE IsValidated = 0 AND Notes LIKE N'%LENT-P7A%')
                    WAITFOR DELAY '00:00:03';
            END""";
    private static final BigDecimal MONTANT = new BigDecimal("8000");
    private static final BigDecimal NOUVEAU = new BigDecimal("9000");

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
    private UUID frais;
    private String notes;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        TransactionTemplate transactions = new TransactionTemplate(transactionManager);
        admin = transactions.execute(statut -> fixture.administrateur());
        AuthenticatedUser principal = new AuthenticatedUser(admin.getId(), admin.getCode(), null, false, true, 0);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_Administrateur"))));
        transactions.executeWithoutResult(statut -> {
            c = terrains.contexte("P7V");
            terrains.configurationFacture();
            terrain = terrains.terrain(c, "TER-" + c.jeton());
            type = terrains.typeFrais(c.jeton(), true, true);
            fournisseur = terrains.fournisseur("FRN-" + c.jeton(), null);
            notes = MARQUEUR + "-" + c.jeton();
            frais = terrains.frais(terrain, type.id(), fournisseur, MONTANT, false, notes);
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

    private void effacer(String sql, Object... parametres) {
        try {
            jdbc.update(sql, parametres);
        } catch (DataAccessException e) {
            log.warn("Nettoyage incomplet ({}) : {}", sql, e.getMessage());
        }
    }

    private int valider() throws Exception {
        return mockMvc.perform(post(VALIDER, terrain, frais).with(connecte(admin))).andReturn().getResponse().getStatus();
    }

    private int modifierLeMontant() throws Exception {
        String corps = """
                {"lignes":[{"id":"%s","date":"2026-03-15","montant":%s,"notes":"%s","typeFraisId":"%s","fournisseurId":"%s"}]}"""
                .formatted(frais, NOUVEAU.toPlainString(), notes, type.id(), fournisseur);
        return mockMvc.perform(put(FRAIS, terrain).with(connecte(admin)).contentType(MediaType.APPLICATION_JSON)
                .content(corps)).andReturn().getResponse().getStatus();
    }

    private BigDecimal montantFrais() {
        return jdbc.queryForObject("SELECT Amount FROM StkExpense WHERE Pkey = ?", BigDecimal.class, frais.toString());
    }

    private List<BigDecimal> montantsFactures() {
        return jdbc.queryForList("SELECT AmountTTC FROM BuyDocument WHERE StkExpenseId = ?", BigDecimal.class,
                frais.toString());
    }

    private int valide() {
        Integer v = jdbc.queryForObject("SELECT CAST(IsValidated AS int) FROM StkExpense WHERE Pkey = ?", Integer.class,
                frais.toString());
        return v == null ? 0 : v;
    }

    /** La validation tient le frais pendant 3 s ; le PUT qui change le montant attend, puis voit un frais validé → 409. */
    @Test
    void validationEnCours_putConcurrent_attendPuisRefuse_validationIntacte() throws Exception {
        jdbc.execute(CREER_TRIGGER_FACTURE);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        int statutValidation;
        int statutPut;
        try {
            Future<Integer> validation = pool.submit(this::valider);
            Thread.sleep(1000);
            Future<Integer> modification = pool.submit(this::modifierLeMontant);
            statutValidation = validation.get(60, TimeUnit.SECONDS);
            statutPut = modification.get(60, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        int s1 = statutValidation;
        int s2 = statutPut;
        assertAll(
                () -> assertThat(s1).as("validation").isEqualTo(200),
                () -> assertThat(s2).as("PUT d'un frais validé entre-temps").isEqualTo(409),
                () -> assertThat(valide()).as("IsValidated").isEqualTo(1),
                () -> assertThat(montantFrais()).as("montant du frais").isEqualByComparingTo(MONTANT),
                () -> assertThat(montantsFactures()).singleElement().satisfies(
                        m -> assertThat(m).as("montant facturé").isEqualByComparingTo(MONTANT)));
    }

    /** Le PUT tient le frais pendant 3 s ; la validation attend, puis facture le nouveau montant. */
    @Test
    void putEnCours_validationConcurrente_attendPuisFactureLeNouveauMontant() throws Exception {
        jdbc.execute(CREER_TRIGGER_FRAIS);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        int statutValidation;
        int statutPut;
        try {
            Future<Integer> modification = pool.submit(this::modifierLeMontant);
            Thread.sleep(1000);
            Future<Integer> validation = pool.submit(this::valider);
            statutPut = modification.get(60, TimeUnit.SECONDS);
            statutValidation = validation.get(60, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        int s1 = statutValidation;
        int s2 = statutPut;
        assertAll(
                () -> assertThat(s2).as("PUT").isEqualTo(200),
                () -> assertThat(s1).as("validation").isEqualTo(200),
                () -> assertThat(valide()).as("IsValidated").isEqualTo(1),
                () -> assertThat(montantFrais()).as("montant du frais").isEqualByComparingTo(NOUVEAU),
                () -> assertThat(montantsFactures()).singleElement().satisfies(
                        m -> assertThat(m).as("montant facturé").isEqualByComparingTo(NOUVEAU)));
    }
}

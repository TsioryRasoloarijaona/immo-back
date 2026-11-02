package com.tsix.immocrm.config;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureFichesVerrouillees;
import com.tsix.immocrm.fixtures.FixturePatrimoine;
import com.tsix.immocrm.fixtures.FixtureSessions;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FEATURE-admin-fiches-verrouillees.md, règles 7, 9 et 11 : écritures durables sur la source, relues en SQL natif, sans
 * trace d'audit. Non transactionnel : une trace s'écrirait au commit. La table créée ici reste dans yesRemsTest (idempotent).
 */
@SpringBootTest
@AutoConfigureMockMvc
class RoutageEcrituresFichesVerrouilleesTest {

    private static final String LECTURE = "copie";
    private static final String ECRITURE = "source";
    private static final String EN_ECRITURE = " / transaction d'écriture";
    private static final String URL = "/api/administration/fiches-verrouillees";
    private static final String URL_DEVERROUILLER = URL + "/{id}/deverrouiller";
    private static final String URL_TOUT = URL + "/deverrouiller-tout";
    private static final String TRACES = "SELECT COUNT(*) FROM AdmWebHistorics WHERE ObjectId = ?";
    private static final String TRACES_DE_VERROU =
            "SELECT COUNT(*) FROM AdmWebHistorics WHERE ObjectId = ? OR ObjectType LIKE '%LockEntit%'";
    private static final String LIBEREE = "SELECT COUNT(*) FROM AdmWebLockEntities WHERE PKey = ? AND State = 2";
    private static final int APPELS = 4;
    private static final List<String> JOURNAL = new CopyOnWriteArrayList<>();

    @TestConfiguration
    static class DeuxBasesFichesVerrouillees {

        @Bean(autowireCandidate = false)
        HikariDataSource poolCopieFiches(Environment env) {
            return pool(env, "fiches-copie");
        }

        @Bean(autowireCandidate = false)
        HikariDataSource poolSourceFiches(Environment env) {
            return pool(env, "fiches-source");
        }

        @Bean
        @Primary
        DataSource dataSource(Environment env) {
            SourceDeVeriteProperties p = new SourceDeVeriteProperties();
            p.setHost("base-de-test");
            return new DataSourceConfig().dataSource(new Journalisee(poolCopieFiches(env), LECTURE),
                    new Journalisee(poolSourceFiches(env), ECRITURE), p);
        }

        private static HikariDataSource pool(Environment env, String nom) {
            HikariDataSource ds = new HikariDataSource();
            ds.setJdbcUrl(env.getRequiredProperty("spring.datasource.url"));
            ds.setUsername(env.getRequiredProperty("spring.datasource.username"));
            ds.setPassword(env.getRequiredProperty("spring.datasource.password"));
            ds.setMaximumPoolSize(APPELS + 2);
            ds.setPoolName(nom);
            return ds;
        }
    }

    /** Note, pour chaque instruction préparée, la cible qui la reçoit et le mode de la transaction en cours. */
    static final class Journalisee extends DelegatingDataSource {

        private final String nom;

        Journalisee(DataSource cible, String nom) {
            super(cible);
            this.nom = nom;
        }

        @Override
        public Connection getConnection() throws SQLException {
            Connection c = super.getConnection();
            return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                    (proxy, methode, args) -> {
                        if (methode.getName().startsWith("prepare") || "createStatement".equals(methode.getName())) {
                            JOURNAL.add(nom + (TransactionSynchronizationManager.isCurrentTransactionReadOnly()
                                    ? " / lecture seule" : EN_ECRITURE));
                        }
                        try {
                            return methode.invoke(c, args);
                        } catch (InvocationTargetException e) {
                            throw e.getCause();
                        }
                    });
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private FixturePatrimoine patrimoine;
    @Autowired private FixtureSessions sessions;
    @Autowired private FixtureFichesVerrouillees fixture;
    @Autowired private PlatformTransactionManager transactions;

    private AdmUser admin;
    private UUID session;
    private UUID verrou;
    private UUID autreVerrou;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        LocalDateTime date = LocalDateTime.of(2026, 3, 10, 9, 15, 30);
        new TransactionTemplate(transactions).executeWithoutResult(t -> {
            sessions.assurerTables();
            fixture.assurerTable();
            session = sessions.session(admin, date).inserer();
            verrou = fixture.verrou(session, UUID.randomUUID().toString(), date).inserer();
            autreVerrou = fixture.verrou(session, UUID.randomUUID().toString(), date).inserer();
        });
        JOURNAL.clear();
    }

    @AfterEach
    void nettoyer() {
        jdbc.update("DELETE FROM AdmWebHistorics WHERE ObjectId = ?", admin.getId().toString());
        jdbc.update("DELETE FROM AdmWebLockEntities WHERE PKey IN (?, ?)", verrou.toString(), autreVerrou.toString());
        jdbc.update("DELETE FROM AdmWebSessions WHERE PKey = ?", session.toString());
        jdbc.update("DELETE FROM AdmUserHabilitation WHERE AdmUserId = ?", admin.getId().toString());
        jdbc.update("DELETE FROM AdmUser WHERE PKey = ?", admin.getId().toString());
    }

    private void deverrouiller() throws Exception {
        mockMvc.perform(post(URL_DEVERROUILLER, verrou).with(connecte(admin))).andExpect(status().isOk());
    }

    private void toutDeverrouiller() throws Exception {
        mockMvc.perform(post(URL_TOUT).with(connecte(admin))).andExpect(status().isOk());
    }

    private int compter(String sql, UUID id) {
        Integer n = jdbc.queryForObject(sql, Integer.class, id.toString());
        return n == null ? 0 : n;
    }

    private void partiSurLaSource() {
        assertThat(List.copyOf(JOURNAL)).contains(ECRITURE + EN_ECRITURE).doesNotContain(LECTURE + EN_ECRITURE);
    }

    // ------------------------------------------------------------------ règle 11 : écritures durables

    @Test
    void r11_deverrouiller_partSurLaSource_ligneRelueEnSqlNatif() throws Exception {
        deverrouiller();

        assertAll(
                this::partiSurLaSource,
                () -> assertThat(compter(LIBEREE, verrou)).isEqualTo(1),
                () -> assertThat(compter(LIBEREE, autreVerrou)).isZero());
    }

    @Test
    void r11_deverrouillerTout_partSurLaSource_lignesReluesEnSqlNatif() throws Exception {
        toutDeverrouiller();

        assertAll(
                this::partiSurLaSource,
                () -> assertThat(compter(LIBEREE, verrou)).isEqualTo(1),
                () -> assertThat(compter(LIBEREE, autreVerrou)).isEqualTo(1));
    }

    // ------------------------------------------------------------------ règle 9 : aucune trace

    @Test
    void r9_deverrouiller_aucuneLigneAdmWebHistorics() throws Exception {
        deverrouiller();

        assertThat(compter(TRACES_DE_VERROU, verrou)).isZero();
    }

    @Test
    void r9_deverrouillerTout_aucuneLigneAdmWebHistorics() throws Exception {
        toutDeverrouiller();

        assertAll(
                () -> assertThat(compter(TRACES_DE_VERROU, verrou)).isZero(),
                () -> assertThat(compter(TRACES_DE_VERROU, autreVerrou)).isZero());
    }

    // Sans ce témoin, le zéro de r9 passerait avec un écouteur d'audit muet dans ce contexte.
    @Test
    void r9_temoin_ecouteurAuditActifDansCeContexte() {
        assertThat(compter(TRACES, admin.getId())).isPositive();
    }

    // ------------------------------------------------------------------ règle 7 : sous concurrence réelle

    // Lot 2.5 : la seconde fermeture simultanée ne doit pas réussir ; même contrat de 409 pour ce lot.
    @Test
    void r7_deverrouillagesSimultanes_unSeulReussit_lesAutres409() throws Exception {
        CountDownLatch depart = new CountDownLatch(1);
        Callable<Integer> appel = () -> {
            depart.await();
            return mockMvc.perform(post(URL_DEVERROUILLER, verrou).with(connecte(admin)))
                    .andReturn().getResponse().getStatus();
        };
        List<Integer> statuts = new ArrayList<>();
        try (ExecutorService fils = Executors.newFixedThreadPool(APPELS)) {
            List<Future<Integer>> reponses = new ArrayList<>();
            for (int i = 0; i < APPELS; i++) {
                reponses.add(fils.submit(appel));
            }
            depart.countDown();
            for (Future<Integer> reponse : reponses) {
                statuts.add(reponse.get());
            }
        }

        assertThat(statuts).containsOnly(200, 409).filteredOn(s -> s == 200).hasSize(1);
    }
}

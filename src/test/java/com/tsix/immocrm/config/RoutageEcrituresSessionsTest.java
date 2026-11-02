package com.tsix.immocrm.config;

import com.tsix.immocrm.entities.adm.AdmUser;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FEATURE-admin-sessions.md, règles 7, 8 et 10 : fermeture durable sur la source, relue en SQL natif, sans trace d'audit.
 * Non transactionnel : une trace s'écrirait au commit. La table créée ici reste dans yesRemsTest (idempotent).
 */
@SpringBootTest
@AutoConfigureMockMvc
class RoutageEcrituresSessionsTest {

    private static final String LECTURE = "copie";
    private static final String ECRITURE = "source";
    private static final String EN_ECRITURE = " / transaction d'écriture";
    private static final String URL_FERMER = "/api/administration/sessions/{id}/fermer";
    private static final String TRACES = "SELECT COUNT(*) FROM AdmWebHistorics WHERE ObjectId = ?";
    private static final List<String> JOURNAL = new CopyOnWriteArrayList<>();

    @TestConfiguration
    static class DeuxBasesSessions {

        @Bean(autowireCandidate = false)
        HikariDataSource poolCopieSessions(Environment env) {
            return pool(env, "sessions-copie");
        }

        @Bean(autowireCandidate = false)
        HikariDataSource poolSourceSessions(Environment env) {
            return pool(env, "sessions-source");
        }

        @Bean
        @Primary
        DataSource dataSource(Environment env) {
            SourceDeVeriteProperties p = new SourceDeVeriteProperties();
            p.setHost("base-de-test");
            return new DataSourceConfig().dataSource(new Journalisee(poolCopieSessions(env), LECTURE),
                    new Journalisee(poolSourceSessions(env), ECRITURE), p);
        }

        private static HikariDataSource pool(Environment env, String nom) {
            HikariDataSource ds = new HikariDataSource();
            ds.setJdbcUrl(env.getRequiredProperty("spring.datasource.url"));
            ds.setUsername(env.getRequiredProperty("spring.datasource.username"));
            ds.setPassword(env.getRequiredProperty("spring.datasource.password"));
            ds.setMaximumPoolSize(4);
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
    @Autowired private FixtureSessions fixture;
    @Autowired private PlatformTransactionManager transactions;

    private AdmUser admin;
    private UUID session;

    @BeforeEach
    void setUp() {
        admin = patrimoine.administrateur();
        session = new TransactionTemplate(transactions).execute(t -> {
            fixture.assurerTables();
            return fixture.session(admin, LocalDateTime.of(2026, 3, 10, 9, 15, 30)).inserer();
        });
        JOURNAL.clear();
    }

    @AfterEach
    void nettoyer() {
        jdbc.update("DELETE FROM AdmWebHistorics WHERE ObjectId IN (?, ?)", session.toString(), admin.getId().toString());
        jdbc.update("DELETE FROM AdmWebSessions WHERE PKey = ?", session.toString());
        jdbc.update("DELETE FROM AdmUserHabilitation WHERE AdmUserId = ?", admin.getId().toString());
        jdbc.update("DELETE FROM AdmUser WHERE PKey = ?", admin.getId().toString());
    }

    private void fermer() throws Exception {
        mockMvc.perform(post(URL_FERMER, session).with(connecte(admin))).andExpect(status().isOk());
    }

    private int compter(String sql, UUID id) {
        Integer n = jdbc.queryForObject(sql, Integer.class, id.toString());
        return n == null ? 0 : n;
    }

    @Test
    void r10_fermer_partSurLaSource_ligneRelueEnSqlNatif() throws Exception {
        fermer();

        assertAll(
                () -> assertThat(List.copyOf(JOURNAL)).contains(ECRITURE + EN_ECRITURE).doesNotContain(LECTURE + EN_ECRITURE),
                () -> assertThat(compter(
                        "SELECT COUNT(*) FROM AdmWebSessions WHERE PKey = ? AND State = 4 AND EndDate IS NOT NULL", session))
                        .isEqualTo(1));
    }

    @Test
    void r8_fermer_aucuneLigneAdmWebHistorics() throws Exception {
        fermer();

        assertThat(compter(TRACES, session)).isZero();
    }

    // Sans ce témoin, le zéro de r8 passerait avec un écouteur d'audit muet dans ce contexte.
    @Test
    void r8_temoin_ecouteurAuditActifDansCeContexte() {
        assertThat(compter(TRACES, admin.getId())).isPositive();
    }
}

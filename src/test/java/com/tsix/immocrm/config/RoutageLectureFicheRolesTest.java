package com.tsix.immocrm.config;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureRoles;
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
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.tsix.immocrm.fixtures.ConnexionDeTest.connecte;
import static com.tsix.immocrm.fixtures.FixtureRoles.FICHE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Décision 5 de FEATURE-admin-roles.md : la fiche d'un rôle lit la source (AdmRight n'est pas sous Change Tracking), jamais
 * la copie.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RoutageLectureFicheRolesTest {

    private static final String COPIE = "copie";
    private static final String SOURCE = "source";
    private static final List<String> JOURNAL = new CopyOnWriteArrayList<>();

    @TestConfiguration
    static class DeuxBasesFiche {

        @Bean(autowireCandidate = false)
        HikariDataSource poolCopieFiche(Environment env) {
            return pool(env, "fiche-copie");
        }

        @Bean(autowireCandidate = false)
        HikariDataSource poolSourceFiche(Environment env) {
            return pool(env, "fiche-source");
        }

        @Bean
        @Primary
        DataSource dataSource(Environment env) {
            SourceDeVeriteProperties p = new SourceDeVeriteProperties();
            p.setHost("base-de-test");
            return new DataSourceConfig().dataSource(new Journalisee(poolCopieFiche(env), COPIE),
                    new Journalisee(poolSourceFiche(env), SOURCE), p);
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

    /** Note la cible qui reçoit chaque instruction préparée. */
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
                            JOURNAL.add(nom);
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
    @Autowired private FixtureRoles fixture;

    private String jeton;
    private AdmUser admin;

    @BeforeEach
    void setUp() {
        jeton = fixture.preparer();
        admin = fixture.compte();
    }

    @AfterEach
    void nettoyer() {
        fixture.nettoyer();
    }

    @Test
    void fiche_litLaSource_pasLaCopie() throws Exception {
        UUID role = fixture.role("RLF-" + jeton);
        JOURNAL.clear();

        mockMvc.perform(get(FICHE, role).with(connecte(admin))).andExpect(status().isOk());

        assertThat(List.copyOf(JOURNAL)).contains(SOURCE).doesNotContain(COPIE);
    }
}

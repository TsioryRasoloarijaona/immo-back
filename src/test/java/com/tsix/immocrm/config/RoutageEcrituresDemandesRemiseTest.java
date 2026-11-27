package com.tsix.immocrm.config;

import com.tsix.immocrm.entities.adm.AdmUser;
import com.tsix.immocrm.fixtures.FixtureDossierVente;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Décisions du siège sur une demande de remise (A3) en mode deux bases : le contrôle des droits lus en base précède
 * l'écriture, qui doit partir sur la source (même piège que RoutageEcrituresCommissionsTest).
 */
@SpringBootTest
@AutoConfigureMockMvc
class RoutageEcrituresDemandesRemiseTest {

    private static final String LECTURE = "copie";
    private static final String ECRITURE = "source";
    private static final String EN_ECRITURE = " / transaction d'écriture";
    private static final String DIRECTION_FONCIERE = "Direction fonciere";
    private static final String DROIT_NIVEAU_2 = "Yes.Com.Front.Sales.Monitoring.toolModifyAmounts1";
    private static final List<String> JOURNAL = new CopyOnWriteArrayList<>();

    @TestConfiguration
    static class DeuxBases {

        @Bean(autowireCandidate = false)
        HikariDataSource poolCopie(Environment env) {
            return pool(env, "test-copie");
        }

        @Bean(autowireCandidate = false)
        HikariDataSource poolSource(Environment env) {
            return pool(env, "test-source");
        }

        @Bean
        @Primary
        DataSource dataSource(Environment env) {
            SourceDeVeriteProperties p = new SourceDeVeriteProperties();
            p.setHost("base-de-test");
            return new DataSourceConfig().dataSource(new Cible(poolCopie(env), LECTURE),
                    new Cible(poolSource(env), ECRITURE), p);
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

    static final class Cible extends DelegatingDataSource {

        private final String nom;

        Cible(DataSource cible, String nom) {
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

    private final UUID role = UUID.randomUUID();
    private final UUID compte = UUID.randomUUID();
    private AdmUser utilisateur;

    @BeforeEach
    void compteDirectionFonciere() {
        jdbc.update("INSERT INTO AdmRole (PKey, Code, Description) VALUES (?, ?, ?)", role.toString(),
                DIRECTION_FONCIERE, "TST routage demandes de remise");
        jdbc.update("INSERT INTO AdmUser (PKey, Code, Description, Disabled) VALUES (?, ?, ?, 0)", compte.toString(),
                "tst.routage.remise." + compte, "TST routage demandes de remise");
        jdbc.update("INSERT INTO AdmUserRole (PKey, AdmUserId, AdmRoleId) VALUES (?, ?, ?)",
                UUID.randomUUID().toString(), compte.toString(), role.toString());
        jdbc.update("INSERT INTO AdmRight (PKey, AdmRoleId, Code) VALUES (?, ?, ?)", UUID.randomUUID().toString(),
                role.toString(), DROIT_NIVEAU_2);
        utilisateur = new AdmUser();
        utilisateur.setId(compte);
        utilisateur.setCode("tst.routage.remise." + compte);
        JOURNAL.clear();
    }

    @AfterEach
    void nettoyer() {
        jdbc.update("DELETE FROM AdmRight WHERE AdmRoleId = ?", role.toString());
        jdbc.update("DELETE FROM AdmUserRole WHERE AdmUserId = ?", compte.toString());
        jdbc.update("DELETE FROM AdmUser WHERE PKey = ?", compte.toString());
        jdbc.update("DELETE FROM AdmRole WHERE PKey = ?", role.toString());
    }

    static Stream<MockHttpServletRequestBuilder> decisions() {
        String inconnue = UUID.randomUUID().toString();
        return Stream.of(
                post("/api/demandes-remise/{id}/acceptation", inconnue).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"),
                post("/api/demandes-remise/{id}/refus", inconnue).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motif\":\"x\"}"));
    }

    /** Droits vérifiés en base, puis transaction d'écriture (404 sur la demande inconnue) : rien sur la copie. */
    @ParameterizedTest
    @MethodSource("decisions")
    void decisionApresControleDesDroits_partSurLaSource_jamaisSurLaCopie(MockHttpServletRequestBuilder requete)
            throws Exception {
        mockMvc.perform(requete.with(FixtureDossierVente.connecte(utilisateur, DIRECTION_FONCIERE)))
                .andExpect(status().isNotFound());

        assertThat(List.copyOf(JOURNAL))
                .contains(ECRITURE + EN_ECRITURE)
                .doesNotContain(LECTURE + EN_ECRITURE);
    }
}

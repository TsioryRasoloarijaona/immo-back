package com.tsix.immocrm.web;

import com.microsoft.sqlserver.jdbc.SQLServerException;
import com.tsix.immocrm.config.ConnexionSourceSuivie;
import com.tsix.immocrm.config.SourceDeVeriteProperties;
import org.hibernate.exception.JDBCConnectionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** FEATURE-stabilite-connexion-source.md §2 : vrai {@link GlobalExceptionHandler}, résolu par Spring MVC. */
class LiaisonSourceHandlerTest {

    private static final String DETAIL_A = "La base OMRAN est injoignable : rien n'a été enregistré. "
            + "Réessayez dans quelques minutes.";
    private static final String DETAIL_B = "La liaison avec la base OMRAN a été coupée pendant l'opération. "
            + "Vérifiez si la saisie a été enregistrée avant de la refaire.";

    private static SQLTransientConnectionException poolSansConnexion() {
        return new SQLTransientConnectionException(
                "hikari-source-de-verite - Connection is not available, request timed out after 5001ms.", "08S01",
                new SQLException("The TCP/IP connection to the host 127.0.0.1, port 1 has failed.", "08S01"));
    }

    // Constructeurs (message, cause) non publics dans mssql-jdbc.
    private static SQLServerException sqlServerException(String message) throws Exception {
        Constructor<SQLServerException> c = SQLServerException.class.getDeclaredConstructor(String.class, Throwable.class);
        c.setAccessible(true);
        return c.newInstance(message, null);
    }

    private static SQLException connexionReinitialisee() {
        return new SQLException("Connection reset", "08S01");
    }

    @RestController
    static class Lanceur {

        private final DataSource source;

        Lanceur(DataSource source) {
            this.source = source;
        }

        @PostMapping("/a/jdbc")
        void aJdbc() {
            throw new CannotGetJdbcConnectionException("Failed to obtain JDBC Connection", poolSansConnexion());
        }

        @PostMapping("/a/hibernate")
        void aHibernate() {
            throw new DataAccessResourceFailureException("Unable to acquire JDBC Connection",
                    new JDBCConnectionException("Unable to acquire JDBC Connection", poolSansConnexion()));
        }

        @PostMapping("/a/transaction")
        void aTransaction() {
            throw new CannotCreateTransactionException("Could not open JPA EntityManager for transaction",
                    poolSansConnexion());
        }

        @PostMapping("/a/apres-connexion")
        void aApresConnexion() throws SQLException {
            new ConnexionSourceSuivie(source).getConnection().close();
            throw new CannotGetJdbcConnectionException("Failed to obtain JDBC Connection", poolSansConnexion());
        }

        @PostMapping("/b/requete")
        void bRequete() {
            throw new DataAccessResourceFailureException("could not execute statement",
                    new JDBCConnectionException("could not execute statement", connexionReinitialisee()));
        }

        @PostMapping("/b/commit")
        void bCommit() {
            throw new TransactionSystemException("Could not commit JPA transaction", connexionReinitialisee());
        }

        @PostMapping("/ordinaire")
        void ordinaire() {
            throw new DataIntegrityViolationException("could not execute statement",
                    new SQLException("Violation of PRIMARY KEY constraint 'PK_ComFolder'.", "23000", 2627));
        }
    }

    private MockMvc mockMvc;

    /** Deux-bases : en mono-base, le gestionnaire ne classe rien (voir LiaisonSourceMonoBaseRevueTest). */
    private static SourceDeVeriteProperties sourceConfiguree() {
        SourceDeVeriteProperties p = new SourceDeVeriteProperties();
        p.setHost("197.0.2.10");
        p.setNom("yesRems");
        return p;
    }

    @BeforeEach
    void preparer() throws SQLException {
        DataSource source = mock(DataSource.class);
        Connection connexion = mock(Connection.class);
        when(source.getConnection()).thenReturn(connexion);
        mockMvc = MockMvcBuilders.standaloneSetup(new Lanceur(source))
                .setControllerAdvice(new GlobalExceptionHandler(
                        new StaticListableBeanFactory(java.util.Map.of("source", sourceConfiguree()))
                                .getBeanProvider(SourceDeVeriteProperties.class)))
                .build();
    }

    private void attendre503(String chemin, String code, String detail) throws Exception {
        mockMvc.perform(post(chemin))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.detail").value(detail));
    }

    @Test
    void casA_aucuneConnexionObtenue_503SourceInjoignable() throws Exception {
        attendre503("/a/jdbc", "SOURCE_INJOIGNABLE", DETAIL_A);
        attendre503("/a/hibernate", "SOURCE_INJOIGNABLE", DETAIL_A);
        attendre503("/a/transaction", "SOURCE_INJOIGNABLE", DETAIL_A);
    }

    @Test
    void casB_liaisonCoupeeEnCoursDOperation_503LiaisonCoupee() throws Exception {
        attendre503("/b/requete", "LIAISON_COUPEE", DETAIL_B);
        attendre503("/b/commit", "LIAISON_COUPEE", DETAIL_B);
    }

    /** R7 : une connexion à la source a servi plus tôt dans la requête, on ne promet plus « rien n'a été enregistré ». */
    @Test
    void r7_echecDeConnexionApresUneConnexionObtenue_estB() throws Exception {
        attendre503("/a/apres-connexion", "LIAISON_COUPEE", DETAIL_B);
    }

    /** R8 : relancée telle quelle, donc traitée comme avant (500 du conteneur), jamais 503. */
    @Test
    void r8_erreurSqlOrdinaire_pasDe503() {
        assertThatThrownBy(() -> mockMvc.perform(post("/ordinaire")))
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void classer_horsClasse08_null() {
        assertThat(LiaisonSource.classer(new DataIntegrityViolationException("x",
                new SQLException("Incorrect syntax near 'FROM'.", "42000", 102)), false)).isNull();
        assertThat(LiaisonSource.classer(new DataAccessResourceFailureException("x",
                new SQLException("sans état", (String) null)), false)).isNull();
    }

    /** Sans SQLState : seul le message d'une {@code SQLServerException} (classe exacte) désigne la coupure. */
    @Test
    void classer_sqlServerExceptionSansEtat_parLeMessage() throws Exception {
        assertThat(LiaisonSource.classer(new DataAccessResourceFailureException("x",
                sqlServerException("Connection reset")), true))
                .isEqualTo(LiaisonSource.LIAISON_COUPEE);
        assertThat(LiaisonSource.classer(new DataAccessResourceFailureException("x",
                sqlServerException("Incorrect syntax near 'FROM'.")), true))
                .isNull();
        assertThat(LiaisonSource.classer(new DataAccessResourceFailureException("x",
                new SQLException("Connection reset", (String) null)), true)).isNull();
    }
}

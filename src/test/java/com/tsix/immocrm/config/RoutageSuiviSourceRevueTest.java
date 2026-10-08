package com.tsix.immocrm.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Revue FEATURE-stabilite-connexion-source.md : l'aiguillage readOnly → copie tient toujours avec le proxy reconfiguré
 * (défauts déclarés, source suivie), et seule une connexion réellement obtenue sur la source marque la requête.
 */
class RoutageSuiviSourceRevueTest {

    private DataSource lecture;
    private DataSource ecriture;
    private DataSource proxy;

    private static DataSource cibleFactice() throws SQLException {
        DataSource ds = mock(DataSource.class);
        Connection con = mock(Connection.class);
        Statement st = mock(Statement.class);
        when(ds.getConnection()).thenReturn(con);
        when(con.createStatement()).thenReturn(st);
        when(st.execute(anyString())).thenReturn(false);
        return ds;
    }

    @BeforeEach
    void preparer() throws SQLException {
        lecture = cibleFactice();
        ecriture = cibleFactice();
        SourceDeVeriteProperties p = new SourceDeVeriteProperties();
        p.setHost("127.0.0.1");
        proxy = new DataSourceConfig().dataSource(lecture, ecriture, p);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void nettoyer() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void executer(boolean lectureSeule) {
        TransactionTemplate tx = new TransactionTemplate(new DataSourceTransactionManager(proxy));
        tx.setReadOnly(lectureSeule);
        tx.executeWithoutResult(s -> new JdbcTemplate(proxy).execute("SELECT 1"));
    }

    @Test
    void transactionLectureSeule_vaSurLaCopie_neTouchePasLaSource_neMarquePas() throws SQLException {
        executer(true);

        verify(lecture).getConnection();
        verify(ecriture, never()).getConnection();
        assertThat(ConnexionSourceSuivie.obtenueDansLaRequete()).isFalse();
    }

    @Test
    void transactionEcriture_vaSurLaSource_etMarqueLaRequete() throws SQLException {
        executer(false);

        verify(ecriture).getConnection();
        verify(lecture, never()).getConnection();
        assertThat(ConnexionSourceSuivie.obtenueDansLaRequete()).isTrue();
    }

    @Test
    void echecDObtentionSurLaSource_neMarquePas() throws SQLException {
        when(ecriture.getConnection()).thenThrow(new SQLTransientConnectionException("pool vide", "08001"));

        org.assertj.core.api.Assertions.catchThrowable(() -> executer(false));

        assertThat(ConnexionSourceSuivie.obtenueDansLaRequete()).isFalse();
    }

    @Test
    void transactionSansInstruction_nObtientAucuneConnexion() throws SQLException {
        TransactionTemplate tx = new TransactionTemplate(new DataSourceTransactionManager(proxy));
        tx.executeWithoutResult(s -> { });

        verify(ecriture, never()).getConnection();
        verify(lecture, never()).getConnection();
    }
}

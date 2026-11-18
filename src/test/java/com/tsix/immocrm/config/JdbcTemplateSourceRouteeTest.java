package com.tsix.immocrm.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** FEATURE-jdbc-template-source.md : en deux bases, le JDBC injecté par type suit l'aiguillage readOnly comme JPA. */
class JdbcTemplateSourceRouteeTest {

    private final ApplicationContextRunner deuxBases = new ApplicationContextRunner()
            .withPropertyValues(
                    "app.source-de-verite.host=source.test",
                    "app.source-de-verite.nom=yesRems",
                    "spring.datasource.url=jdbc:sqlserver://localhost:1;databaseName=copie",
                    "spring.datasource.username=test")
            .withUserConfiguration(DataSourceConfig.class)
            .withConfiguration(AutoConfigurations.of(JdbcTemplateAutoConfiguration.class));

    @AfterEach
    void nettoyer() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void contexteDeuxBases_jdbcParType_surLaSourceRoutee_jdbcLectureSurLaCopie() {
        deuxBases.run(contexte -> {
            Object routee = contexte.getBean("dataSource");
            Object copie = contexte.getBean("lecture");

            assertThat(contexte.getBean(JdbcTemplate.class).getDataSource()).isSameAs(routee);
            assertThat(contexte.getBeansOfType(NamedParameterJdbcTemplate.class)).hasSize(1);
            assertThat(contexte.getBean(NamedParameterJdbcTemplate.class).getJdbcTemplate())
                    .isSameAs(contexte.getBean(JdbcTemplate.class));
            assertThat(contexte.getBean("jdbcLecture", JdbcTemplate.class).getDataSource()).isSameAs(copie);
        });
    }

    @Test
    void transactionEcriture_requeteParDefaut_partSurLaSource() throws SQLException {
        DataSource lecture = cibleFactice();
        DataSource ecriture = cibleFactice();

        executer(lecture, ecriture, false);

        verify(ecriture).getConnection();
        verify(lecture, never()).getConnection();
    }

    @Test
    void transactionLectureSeule_requeteParDefaut_partSurLaCopie() throws SQLException {
        DataSource lecture = cibleFactice();
        DataSource ecriture = cibleFactice();

        executer(lecture, ecriture, true);

        verify(lecture).getConnection();
        verify(ecriture, never()).getConnection();
    }

    private static void executer(DataSource lecture, DataSource ecriture, boolean lectureSeule) {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        SourceDeVeriteProperties p = new SourceDeVeriteProperties();
        p.setHost("127.0.0.1");
        DataSourceConfig config = new DataSourceConfig();
        DataSource routee = config.dataSource(lecture, ecriture, p);
        NamedParameterJdbcTemplate jdbc = config.namedParameterJdbcTemplate(config.jdbcTemplate(routee));

        TransactionTemplate tx = new TransactionTemplate(new DataSourceTransactionManager(routee));
        tx.setReadOnly(lectureSeule);
        tx.executeWithoutResult(s -> jdbc.queryForList("SELECT :a", Map.of("a", 1)));
    }

    private static DataSource cibleFactice() throws SQLException {
        DataSource ds = mock(DataSource.class);
        Connection con = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(ds.getConnection()).thenReturn(con);
        when(con.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(mock(ResultSet.class));
        return ds;
    }
}

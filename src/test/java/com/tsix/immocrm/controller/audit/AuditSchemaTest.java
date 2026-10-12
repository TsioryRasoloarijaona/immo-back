package com.tsix.immocrm.controller.audit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Échoue tôt et clairement si la base de test n'a pas les tables du lot P0 (ci/schema/README.md § Base locale). */
@SpringBootTest
class AuditSchemaTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void lesTablesAuditEtNotesExistentDansLaBaseDeTest() {
        List<String> presentes = jdbc.queryForList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'dbo'", String.class);

        assertThat(presentes)
                .as("tables du lot P0 absentes de la base de test : DDL de ci/schema/060_audit_tables.sql")
                .contains("AdmWebHistorics", "AdmMessage");
    }

    @Test
    void detailsEstEnTextCommeEnProduction() {
        String type = jdbc.queryForObject("SELECT DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_NAME = 'AdmWebHistorics' AND COLUMN_NAME = 'Details'", String.class);

        assertThat(type).isEqualTo("text");
    }
}

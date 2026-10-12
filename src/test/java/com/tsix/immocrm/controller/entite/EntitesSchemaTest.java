package com.tsix.immocrm.controller.entite;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Échoue tôt et clairement si la base de test n'a pas les tables du lot P5 (ci/schema/README.md § Base locale). */
@SpringBootTest
class EntitesSchemaTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void lesTablesActionnairesExistentDansLaBaseDeTest() {
        List<String> presentes = jdbc.queryForList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'dbo'", String.class);

        assertThat(presentes)
                .as("tables du lot P5 absentes de la base de test : DDL de ci/schema/066_entites_tables.sql")
                .contains("CfgCompany", "CfgShareholder", "CfgGroupShareholder", "CfgGroupShareholderLine",
                        "CfgCompanyShareholder", "AdmSequence");
    }

    @Test
    void lePourcentageDeGroupeEstDecimalEtLesPartsEntieres() {
        List<String> types = jdbc.queryForList("SELECT TABLE_NAME + '.' + DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE COLUMN_NAME = 'SharingNumber' AND TABLE_NAME IN ('CfgGroupShareholderLine', "
                + "'CfgCompanyShareholder', 'CfgCompany') ORDER BY TABLE_NAME ASC", String.class);

        assertThat(types).containsExactly("CfgCompany.int", "CfgCompanyShareholder.int",
                "CfgGroupShareholderLine.decimal");
    }
}

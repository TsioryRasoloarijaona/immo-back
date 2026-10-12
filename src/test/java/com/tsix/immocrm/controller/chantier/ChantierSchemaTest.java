package com.tsix.immocrm.controller.chantier;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Échoue tôt et clairement si la base de test n'a pas les tables du module (ci/schema/README.md § Base locale). */
@SpringBootTest
class ChantierSchemaTest {

    private static final List<String> TABLES = List.of("PrjProject", "PrjMarketType", "PrjMarket", "PrjMarketLine",
            "PrjMarketLineGrouping", "PrjMarketLineEstimation", "PrjWorkingOut", "PrjWorkingOutLine",
            "PrjWorkingOutLineExtra", "PrjWorkingOutRetainedLine", "BuyDocument", "BuyDocumentStatus");

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void lesDouzeTablesDuModuleExistentDansLaBaseDeTest() {
        List<String> presentes = jdbc.queryForList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'dbo'", String.class);

        assertThat(presentes)
                .as("tables Chantier absentes de la base de test : créer leur DDL (ci/schema/010_tables.sql)")
                .containsAll(TABLES);
    }
}

package com.tsix.immocrm.controller.patrimoine;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Échoue tôt si la base de test n'a pas les tables du lot P1b (ci/schema/064_tranche-onglets_tables.sql). */
@SpringBootTest
class TrancheOngletsSchemaTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void lesTablesDesOngletsExistentDansLaBaseDeTest() {
        List<String> presentes = jdbc.queryForList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'dbo'", String.class);

        assertThat(presentes)
                .as("tables du lot P1b absentes : DDL de ci/schema/064_tranche-onglets_tables.sql (README § Base locale)")
                .contains("CfgTrancheCoveredFloor", "CfgTrancheSettlementCategory", "CfgTrancheLevel",
                        "CfgTrancheStkItem");
    }

    @Test
    void laColonneDeTrancheDesFraisGardeSaCasseDeProduction() {
        List<String> colonnes = jdbc.queryForList("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_NAME = 'CfgTrancheSettlementCategory'", String.class);

        assertThat(colonnes).contains("CfgtrancheId", "ComSettlementCategoryId", "AmountHT", "AmountTTC");
    }
}

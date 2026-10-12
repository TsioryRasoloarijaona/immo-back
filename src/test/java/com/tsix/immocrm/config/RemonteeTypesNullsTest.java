package com.tsix.immocrm.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le {@code MERGE} réel sur une table à colonne {@code image} nulle : SQL Server refuse un NULL non typé (envoyé
 * en nvarchar) dans une colonne {@code image}. Source et copie sont ici la même base, la transaction est annulée.
 */
@SpringBootTest
@Transactional
class RemonteeTypesNullsTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void zoneSansImage_fusionneeSansConflitDeType() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        UUID pkey = UUID.randomUUID();
        jdbc.update("INSERT INTO dbo.CfgProjectZone (PKey, Code, Description, Leveling, Sorting) VALUES (?, ?, ?, 0, 1)",
                pkey, "TST-Z", "Zone de test");
        RemonteeLecture remontee = new RemonteeLecture(jdbc, dataSource);

        boolean presente = remontee.fusionner(new LigneEcrite("CfgProjectZone", pkey));

        assertThat(presente).isTrue();
    }
}

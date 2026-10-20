package com.tsix.immocrm.controller.patrimoine;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import com.tsix.immocrm.fixtures.NommageSchemaCi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Schéma du lot P1b — FEATURE-patrimoine-tranche-onglets.md §8.1, §7 (TrancheOngletsSchemaTest) : la seule table
 * absente de la CI, {@code CfgTrancheCoveredFloor}, créée par 104/105 avec les types de yesRemsProd.
 */
@SpringBootTest
@Transactional
class TrancheOngletsSchemaTest {

    private static final Path SCHEMA_CI = Path.of("ci", "schema");
    private static final Path TABLES = SCHEMA_CI.resolve("104_tranche-onglets_tables.sql");
    private static final Path FK = SCHEMA_CI.resolve("105_tranche-onglets_fk.sql");
    private static final String PLANCHER = "CfgTrancheCoveredFloor";
    private static final String UNIQUEIDENTIFIER = "uniqueidentifier";

    @Autowired private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    private Map<String, String> colonnes(String table) {
        List<Tuple> lignes = entityManager.createNativeQuery("""
                        SELECT COLUMN_NAME AS colonne,
                               DATA_TYPE + ISNULL('(' + CAST(NUMERIC_PRECISION AS varchar) + ',' + CAST(NUMERIC_SCALE AS varchar) + ')', '')
                                   AS type
                        FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = :table""", Tuple.class)
                .setParameter("table", table)
                .getResultList();
        return lignes.stream().collect(Collectors.toMap(t -> (String) t.get("colonne"), t -> (String) t.get("type")));
    }

    private static String lire(Path fichier) throws IOException {
        return Files.readString(fichier, StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------ §8.1 tables présentes (yesRemsTest et CI)

    @Test
    void r8_1_plancherCouvert_colonnesEtTypesDeProd() {
        assertThat(colonnes(PLANCHER))
                .containsEntry("PKey", UNIQUEIDENTIFIER)
                .containsEntry("CfgTrancheId", UNIQUEIDENTIFIER)
                .containsEntry("StkItemCategoryId", UNIQUEIDENTIFIER)
                .containsEntry("Area", "decimal(26,16)")
                .containsEntry("UserCreate", "nvarchar")
                .containsEntry("UserCreateDateTime", "datetime")
                .containsEntry("UserUpdate", "nvarchar")
                .containsEntry("UserUpdateDateTime", "datetime");
    }

    @Test
    void r8_3_etages_colonneLndLevelIdPresente() {
        assertThat(colonnes("CfgTrancheLevel"))
                .containsEntry("CfgTrancheId", UNIQUEIDENTIFIER)
                .containsEntry("LndLevelId", UNIQUEIDENTIFIER);
    }

    // ------------------------------------------------------------------ §8.1 fichiers de CI 104 / 105

    @Test
    void r8_1_fichier104_creeLePlancherCouvert_idempotent() throws IOException {
        assertThat(TABLES).exists();
        String sql = lire(TABLES);

        assertAll(
                () -> assertThat(sql).containsPattern("(?i)CREATE\\s+TABLE\\s+(\\[?dbo]?\\.)?\\[?" + PLANCHER),
                () -> assertThat(sql).containsPattern("(?i)OBJECT_ID\\s*\\(\\s*'(\\[?dbo]?\\.)?\\[?" + PLANCHER),
                () -> assertThat(sql).containsPattern("(?i)\\[?Area]?\\s+\\[?decimal]?\\s*\\(\\s*26\\s*,\\s*16\\s*\\)"));
    }

    @Test
    void r8_1_fichier105_clesVersTrancheEtCategorie_idempotent() throws IOException {
        assertThat(FK).exists();
        String sql = lire(FK);

        assertAll(
                () -> assertThat(sql).containsPattern("(?i)REFERENCES\\s+(\\[?dbo]?\\.)?\\[?CfgTranche]?\\s*\\("),
                () -> assertThat(sql).containsPattern("(?i)REFERENCES\\s+(\\[?dbo]?\\.)?\\[?StkItemCategory]?\\s*\\("),
                () -> assertThat(sql).containsPattern("(?i)(IF\\s+NOT\\s+EXISTS|OBJECT_ID)"));
    }

    @Test
    void r8_1_readmeDuSchemaCi_citeLesFichiers104Et105() throws IOException {
        NommageSchemaCi.verifier(104, "tranche-onglets");
    }
}

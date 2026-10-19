package com.tsix.immocrm.controller.patrimoine;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
 * Schéma du lot P1c — FEATURE-patrimoine-tranche-regles.md §7 (« Schéma »), §9.1, §9.4, §10 : les trois tables
 * absentes de la CI ({@code ComSalesCommission}, {@code CfgTrancheFolderEntity}, {@code CfgPrevision}) créées par
 * 108/109, idempotents, avec les types de yesRemsProd.
 */
@SpringBootTest
@Transactional
class TrancheReglesSchemaTest {

    private static final Path SCHEMA_CI = Path.of("ci", "schema");
    private static final Path TABLES = SCHEMA_CI.resolve("108_tranche-regles_tables.sql");
    private static final Path FK = SCHEMA_CI.resolve("109_tranche-regles_fk.sql");
    private static final String UNIQUEIDENTIFIER = "uniqueidentifier";
    private static final String DATETIME = "datetime";
    private static final String ENTIER = "int(10,0)";
    private static final String TABLE = "(\\[?dbo]?\\.)?\\[?";

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

    // ------------------------------------------------------------------ tables présentes, types de prod

    @Test
    void r9_4_commissions_colonnesEtTypesDeProd() {
        assertThat(colonnes("ComSalesCommission"))
                .containsEntry("PKey", UNIQUEIDENTIFIER)
                .containsEntry("CfgTrancheId", UNIQUEIDENTIFIER)
                .containsEntry("StartDate", DATETIME)
                .containsEntry("EndDate", DATETIME)
                .containsEntry("SaleCommissionAmount", "decimal(26,16)")
                .containsEntry("DeliveryCommissionAmountExtern", "decimal(26,16)")
                .containsEntry("DeliveryCommissionAmount2", "decimal(26,6)")
                .containsEntry("SaleCommissionPercent", "decimal(26,6)")
                .containsEntry("SaleCommissionBasedOnAmount", "bit")
                .containsEntry("DeliveryCommissionExtern2BasedOnAmount", "bit")
                .containsEntry("StkItemCategoryId", UNIQUEIDENTIFIER)
                .containsEntry("CfgTierId", UNIQUEIDENTIFIER)
                .containsEntry("ComEmployeeId", UNIQUEIDENTIFIER)
                .containsEntry("ComActionId", UNIQUEIDENTIFIER)
                .containsEntry("UserUpdateDateTime", DATETIME);
    }

    @Test
    void r9_4_finalisations_colonnesEtTypesDeProd() {
        assertThat(colonnes("CfgTrancheFolderEntity"))
                .containsEntry("PKey", UNIQUEIDENTIFIER)
                .containsEntry("CfgTrancheId", UNIQUEIDENTIFIER)
                .containsEntry("StrFolderEntityId", UNIQUEIDENTIFIER)
                .containsEntry("StrStatusDeliveryId1", UNIQUEIDENTIFIER)
                .containsEntry("StrStatusDeliveryId2", UNIQUEIDENTIFIER)
                .containsEntry("UserCreate", "nvarchar")
                .containsEntry("UserUpdateDateTime", DATETIME);
    }

    /** §9.4 : ni CfgTrancheId, ni Code/Description, ni trace ; lien par StkHierarchyId. */
    @Test
    void r9_4_previsions_colonnesEtTypesDeProd_sansCfgTrancheId() {
        assertThat(colonnes("CfgPrevision"))
                .containsEntry("PKey", UNIQUEIDENTIFIER)
                .containsEntry("StkHierarchyId", UNIQUEIDENTIFIER)
                .containsEntry("DocDate", DATETIME)
                .containsEntry("Year", ENTIER)
                .containsEntry("Month", ENTIER)
                .containsEntry("Quantity", ENTIER)
                .containsEntry("TurnoverQuantity", ENTIER)
                .containsEntry("Amount", "decimal(26,6)")
                .containsEntry("AchievementAmount", "decimal(26,6)")
                .containsEntry("TurnoverAmount", "decimal(26,6)")
                .containsEntry("Turnover", "decimal(26,6)")
                .containsEntry("Notes", "nvarchar")
                .doesNotContainKeys("CfgTrancheId", "UserCreate");
    }

    // ------------------------------------------------------------------ fichiers de CI 108 / 109

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"ComSalesCommission", "CfgTrancheFolderEntity", "CfgPrevision"})
    void r9_1_fichier108_creeLaTable_idempotent(String table) throws IOException {
        assertThat(TABLES).exists();
        String sql = lire(TABLES);

        assertAll(
                () -> assertThat(sql).containsPattern("(?i)CREATE\\s+TABLE\\s+" + TABLE + table),
                () -> assertThat(sql).containsPattern("(?i)OBJECT_ID\\s*\\(\\s*N?'" + TABLE + table));
    }

    @Test
    void r9_1_fichier109_clesVersLesParents_idempotent() throws IOException {
        assertThat(FK).exists();
        String sql = lire(FK);

        assertAll(
                () -> assertThat(sql).containsPattern("(?i)REFERENCES\\s+" + TABLE + "CfgTranche]?\\s*\\("),
                () -> assertThat(sql).containsPattern("(?i)REFERENCES\\s+" + TABLE + "StrEntity]?\\s*\\("),
                () -> assertThat(sql).containsPattern("(?i)REFERENCES\\s+" + TABLE + "StrStatus]?\\s*\\("),
                () -> assertThat(sql).containsPattern("(?i)REFERENCES\\s+" + TABLE + "CfgTier]?\\s*\\("),
                () -> assertThat(sql).containsPattern("(?i)REFERENCES\\s+" + TABLE + "ComAction]?\\s*\\("),
                () -> assertThat(sql).containsPattern("(?i)REFERENCES\\s+" + TABLE + "StkItemCategory]?\\s*\\("),
                () -> assertThat(sql).containsPattern("(?i)OBJECT_ID\\s*\\(\\s*N?'" + TABLE + "StrEntity"));
    }

    @Test
    void r9_1_readmeDuSchemaCi_citeLesFichiers108Et109() throws IOException {
        assertThat(lire(SCHEMA_CI.resolve("README.md")))
                .contains("108_tranche-regles_tables.sql", "109_tranche-regles_fk.sql");
    }
}

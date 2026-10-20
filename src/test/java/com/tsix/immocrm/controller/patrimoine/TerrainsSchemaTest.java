package com.tsix.immocrm.controller.patrimoine;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
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
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Schéma du lot P7a — FEATURE-patrimoine-terrains.md §6.1 (TerrainsSchemaTest) : BuyVentilationCategory,
 * BuyDocumentStatusHistory, BuyDocumentWorkFlow (et BuyDocumentLine, absente de yesRemsTest) créées par
 * {@code 120_terrains_tables.sql} avec les types de yesRemsProd, leurs FK dans {@code 121_terrains_fk.sql}, README à jour.
 * Noms de colonnes comparés sans la casse (prod : {@code BuyDocumentStatusHistory.buyDocumentWorkFlowId}).
 */
@SpringBootTest
@Transactional
class TerrainsSchemaTest {

    private static final Path SCHEMA_CI = Path.of("ci", "schema");
    private static final Path TABLES = SCHEMA_CI.resolve("120_terrains_tables.sql");
    private static final Path FK = SCHEMA_CI.resolve("121_terrains_fk.sql");
    private static final String UNIQUEIDENTIFIER = "uniqueidentifier";
    private static final String NVARCHAR = "nvarchar";
    private static final String DATETIME = "datetime";
    private static final String BIT = "bit";
    private static final String ENTIER = "int(10,0)";
    private static final String DECIMAL_6 = "decimal(26,6)";
    private static final String DECIMAL_16 = "decimal(26,16)";
    private static final String PKEY = "PKey";
    private static final String BUY_DOCUMENT_ID = "BuyDocumentId";
    private static final String STL_CATEGORY_ID = "StlCategoryId";
    private static final List<String> TABLES_120 = List.of("BuyVentilationCategory", "BuyDocumentStatusHistory",
            "BuyDocumentWorkFlow", "BuyDocumentLine");

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
        Map<String, String> parNom = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        lignes.forEach(t -> parNom.put((String) t.get("colonne"), (String) t.get("type")));
        return parNom;
    }

    private static String lire(Path fichier) throws IOException {
        return Files.readString(fichier, StandardCharsets.UTF_8);
    }

    private static Map<String, String> trace() {
        return Map.of("UserCreate", NVARCHAR, "UserCreateDateTime", DATETIME, "UserUpdate", NVARCHAR,
                "UserUpdateDateTime", DATETIME);
    }

    // ------------------------------------------------------------------ tables présentes (yesRemsTest et CI)

    @Test
    void r6_1_ventilation_4ColonnesDeProd() {
        assertThat(colonnes("BuyVentilationCategory"))
                .hasSize(4)
                .containsEntry(PKEY, UNIQUEIDENTIFIER)
                .containsEntry(STL_CATEGORY_ID, UNIQUEIDENTIFIER)
                .containsEntry(BUY_DOCUMENT_ID, UNIQUEIDENTIFIER)
                .containsEntry("VentilationRate", DECIMAL_6);
    }

    @Test
    void r6_1_historiqueDeStatut_7ColonnesDeProd() {
        assertThat(colonnes("BuyDocumentStatusHistory"))
                .hasSize(7)
                .containsEntry(PKEY, UNIQUEIDENTIFIER)
                .containsEntry("DocDate", DATETIME)
                .containsEntry("BuyDocumentStatusId", UNIQUEIDENTIFIER)
                .containsEntry(BUY_DOCUMENT_ID, UNIQUEIDENTIFIER)
                .containsEntry("AdmUserId", UNIQUEIDENTIFIER)
                .containsEntry("Notes", NVARCHAR)
                .containsEntry("buyDocumentWorkFlowId", UNIQUEIDENTIFIER);
    }

    @Test
    void r6_1_workflowDesFactures_19ColonnesDeProd() {
        assertThat(colonnes("BuyDocumentWorkFlow"))
                .hasSize(19)
                .containsEntry(PKEY, UNIQUEIDENTIFIER)
                .containsEntry("Sorting", ENTIER)
                .containsEntry("StartStatusId", UNIQUEIDENTIFIER)
                .containsEntry("EndStatusId", UNIQUEIDENTIFIER)
                .containsEntry("StrEntityId", UNIQUEIDENTIFIER)
                .containsEntry("StrWorkFlowId", UNIQUEIDENTIFIER)
                .containsEntry("Description", NVARCHAR)
                .containsEntry("NotesMandatory", BIT)
                .containsAllEntriesOf(trace());
    }

    @Test
    void r6_1_ligneDeFacture_colonnesEcritesParLaValidation() {
        assertThat(colonnes("BuyDocumentLine"))
                .containsEntry("Pkey", UNIQUEIDENTIFIER)
                .containsEntry(BUY_DOCUMENT_ID, UNIQUEIDENTIFIER)
                .containsEntry("Code", NVARCHAR)
                .containsEntry("Description", NVARCHAR)
                .containsEntry("Unity", NVARCHAR)
                .containsEntry("Quantity", DECIMAL_6)
                .containsEntry("DiscountRatio", DECIMAL_16)
                .containsEntry("VatRatio", DECIMAL_6)
                .containsEntry("FODECRatio", DECIMAL_16)
                .containsEntry("Sorting", ENTIER)
                .containsEntry("UnitPriceHT", DECIMAL_6)
                .containsEntry("UnitPriceTTC", DECIMAL_6)
                .containsEntry("QuantityOld", DECIMAL_6)
                .containsEntry("QuantityOrigin", DECIMAL_6)
                .containsEntry("Coefficient", DECIMAL_6)
                .containsEntry(STL_CATEGORY_ID, UNIQUEIDENTIFIER)
                .containsEntry("Notes", NVARCHAR)
                .containsAllEntriesOf(trace());
    }

    // ------------------------------------------------------------------ fichiers de CI 120 / 121

    @Test
    void r6_1_fichier120_creeLesQuatreTables_idempotent() throws IOException {
        assertThat(TABLES).exists();
        String sql = lire(TABLES);

        assertThat(TABLES_120).isNotEmpty().allSatisfy(table -> assertAll(
                () -> assertThat(sql).containsPattern("(?i)CREATE\\s+TABLE\\s+(\\[?dbo]?\\.)?\\[?" + table + "]?\\s*\\("),
                () -> assertThat(sql).containsPattern("(?i)OBJECT_ID\\s*\\(\\s*N?'(\\[?dbo]?\\.)?\\[?" + table + "]?'")));
    }

    @Test
    void r6_1_fichier121_clesEtrangeresEtActionsDeProd() throws IOException {
        assertThat(FK).exists();
        String sql = lire(FK);

        assertAll(
                () -> assertThat(sql).containsPattern(reference("BuyDocument") + "\\s*ON\\s+DELETE\\s+CASCADE"),
                () -> assertThat(sql).containsPattern(reference("StlCategory") + "\\s*ON\\s+DELETE\\s+CASCADE"),
                () -> assertThat(sql).containsPattern(reference("BuyDocumentStatus")),
                () -> assertThat(sql).containsPattern(reference("AdmUser") + "\\s*ON\\s+DELETE\\s+CASCADE"),
                () -> assertThat(sql).containsPattern(reference("StrEntity") + "\\s*ON\\s+DELETE\\s+CASCADE"),
                () -> assertThat(sql).containsPattern(reference("StrWorkFlow")),
                () -> assertThat(sql).containsPattern("(?i)(IF\\s+NOT\\s+EXISTS|OBJECT_ID)"));
    }

    private static String reference(String table) {
        return "(?i)REFERENCES\\s+(\\[?dbo]?\\.)?\\[?" + table + "]?\\s*\\(\\s*\\[?PKey]?\\s*\\)";
    }

    @Test
    void r6_1_readmeDuSchemaCi_citeLesFichiers120Et121() throws IOException {
        assertThat(lire(SCHEMA_CI.resolve("README.md")))
                .contains("120_terrains_tables.sql", "121_terrains_fk.sql");
    }
}

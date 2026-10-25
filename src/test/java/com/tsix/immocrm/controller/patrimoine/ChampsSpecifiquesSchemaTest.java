package com.tsix.immocrm.controller.patrimoine;

import com.tsix.immocrm.fixtures.NommageSchemaCi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Schéma du lot P9d — FEATURE-patrimoine-relances-champs.md « Schéma et rang » et FEATURE-patrimoine-import-export.md §4.2 :
 * les 4 tables {@code AdmSpecificField*} (absentes de yesRemsTest et de la CI) créées par {@code 144}, idempotent, avec les
 * colonnes de yesRemsProd ; les 3 FK de prod (noms et actions) dans {@code 145} ; commandes yesRemsTest dans le README.
 */
class ChampsSpecifiquesSchemaTest {

    private static final Path SCHEMA_CI = Path.of("ci", "schema");
    private static final Path TABLES = SCHEMA_CI.resolve("144_champs-specifiques_tables.sql");
    private static final Path FK = SCHEMA_CI.resolve("145_champs-specifiques_fk.sql");
    private static final String TABLE_VALEUR = "AdmSpecificFieldValue";

    private static String lire(Path fichier) throws IOException {
        return Files.readString(fichier, StandardCharsets.UTF_8);
    }

    @Test
    void schema_nommage144Et145() throws IOException {
        NommageSchemaCi.verifier(144, "champs-specifiques");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"AdmSpecificFieldEntity", "AdmSpecificFieldDataType", "AdmSpecificField", TABLE_VALEUR})
    void schema_fichier144_creeLaTableSiAbsente(String table) throws IOException {
        String sql = lire(TABLES);

        assertAll(
                () -> assertThat(sql).containsPattern("(?i)CREATE\\s+TABLE\\s+(\\[?dbo]?\\.)?\\[?" + table + "]?\\s*\\("),
                () -> assertThat(sql).containsPattern("(?i)OBJECT_ID\\s*\\(\\s*N?'(\\[?dbo]?\\.)?\\[?" + table + "]?'"));
    }

    /** Colonnes de prod qui portent la règle : valeur texte nvarchar(1000) + colonnes typées, porteur {@code NameSpaceId}. */
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"NameSpaceId", "AdmSpecificFieldId", "DateValue", "BoolValue", "DecimalValue", "IntValue",
            "ComboValue", "AdmSpecificFieldEntityId", "AdmSpecificFieldDataTypeId", "Category", "Caption", "DefaultValue",
            "Required", "CategorySorting", "Sorting", "UserCreate", "UserUpdateDateTime"})
    void schema_fichier144_colonnesDeProd(String colonne) throws IOException {
        assertThat(lire(TABLES)).containsPattern("(?i)\\[?" + colonne + "]?\\s+");
    }

    @Test
    void schema_fichier144_typesDeProd() throws IOException {
        String sql = lire(TABLES);

        assertAll(
                () -> assertThat(sql).containsPattern("(?i)\\[?Value]?\\s+\\[?nvarchar]?\\s*\\(\\s*1000\\s*\\)"),
                () -> assertThat(sql).containsPattern("(?i)\\[?DecimalValue]?\\s+\\[?decimal]?\\s*\\(\\s*26\\s*,\\s*6\\s*\\)"));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "AdmSpecificField_AdmSpecificFieldEntity_FK, AdmSpecificFieldEntityId, AdmSpecificFieldEntity",
            "AdmSpecificField_AdmSpecificFieldDataType_FK, AdmSpecificFieldDataTypeId, AdmSpecificFieldDataType",
            "AdmSpecificFieldValue_AdmSpecificField_FK1, AdmSpecificFieldId, AdmSpecificField"})
    void schema_fichier145_fkDeProd(String nom, String colonne, String cible) throws IOException {
        String sql = lire(FK);

        assertAll(
                () -> assertThat(sql).contains(nom),
                () -> assertThat(sql).containsPattern("(?i)FOREIGN\\s+KEY\\s*\\(\\s*\\[?" + colonne
                        + "]?\\s*\\)\\s*REFERENCES\\s+(\\[?dbo]?\\.)?\\[?" + cible + "]?\\s*\\(\\s*\\[?PKey]?\\s*\\)"),
                () -> assertThat(sql).containsPattern("(?i)(IF\\s+NOT\\s+EXISTS|OBJECT_ID)"));
    }

    /** Seule la valeur suit sa définition en cascade (C2) ; Field → Entity et Field → DataType restent NO ACTION. */
    @Test
    void schema_fichier145_seuleLaValeurEnCascade() throws IOException {
        String sql = lire(FK);

        assertThat(sql.split("(?i)ON\\s+DELETE\\s+CASCADE", -1)).hasSize(2);
        assertThat(sql).containsPattern("(?is)AdmSpecificFieldValue_AdmSpecificField_FK1.*?ON\\s+DELETE\\s+CASCADE");
    }

    @Test
    void schema_readme_commandesYesRemsTest() throws IOException {
        assertThat(lire(SCHEMA_CI.resolve("README.md"))).contains("144_champs-specifiques_tables.sql")
                .contains("145_champs-specifiques_fk.sql");
    }
}

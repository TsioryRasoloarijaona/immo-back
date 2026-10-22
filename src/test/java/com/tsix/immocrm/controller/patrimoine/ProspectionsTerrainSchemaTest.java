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
 * Schéma du lot P7b — FEATURE-patrimoine-prospections-terrain.md §6.1 (ProspectionsTerrainSchemaTest) : LndLand et
 * LndLandZone présentes avec les types de yesRemsProd (138 les crée si absentes, idempotent), FK de la zone dans 139.
 */
@SpringBootTest
@Transactional
class ProspectionsTerrainSchemaTest {

    private static final Path SCHEMA_CI = Path.of("ci", "schema");
    private static final Path TABLES = SCHEMA_CI.resolve("138_prospections-terrain_tables.sql");
    private static final Path FK = SCHEMA_CI.resolve("139_prospections-terrain_fk.sql");
    private static final String UNIQUEIDENTIFIER = "uniqueidentifier";
    private static final String NVARCHAR = "nvarchar";
    private static final String DATETIME = "datetime";
    private static final String SMALLDATETIME = "smalldatetime";
    private static final String BIT = "bit";
    private static final String ENTIER = "int(10,0)";
    private static final String DECIMAL_6 = "decimal(26,6)";

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

    /** §1.1 : les 67 colonnes de production de LndLand, dont celles que le lot écrit. */
    @Test
    void r6_1_lndLand_colonnesDeProd() {
        assertThat(colonnes("LndLand"))
                .hasSize(67)
                .containsEntry("PKey", UNIQUEIDENTIFIER)
                .containsEntry("DocDate", DATETIME)
                .containsEntry("Adress", NVARCHAR)
                .containsEntry("Area", DECIMAL_6)
                .containsEntry("AreaNet", DECIMAL_6)
                .containsEntry("EquipementsCoef", DECIMAL_6)
                .containsEntry("AlinmentStop", ENTIER)
                .containsEntry("RegistryDate", SMALLDATETIME)
                .containsEntry("ImmatriculationDate", SMALLDATETIME)
                .containsEntry("LndCategoryId", UNIQUEIDENTIFIER)
                .containsEntry("LndTypeId", UNIQUEIDENTIFIER)
                .containsEntry("CfgMiddlemanId", UNIQUEIDENTIFIER)
                .containsEntry("CfgLawyerId", UNIQUEIDENTIFIER)
                .containsEntry("StatusValidityDate", DATETIME)
                .containsEntry("IsInArchive", BIT)
                .containsEntry("IsTransformedToLand", BIT)
                .containsEntry("IsFinished", BIT)
                .containsEntry("IsTopographicSurvey", BIT)
                .containsEntry("StrStatusId", UNIQUEIDENTIFIER)
                .containsEntry("StrEntityId", UNIQUEIDENTIFIER)
                .containsEntry("ZoneIdentification", NVARCHAR)
                .containsEntry("DealerConnectionConditions", NVARCHAR)
                .containsEntry("LandBase", NVARCHAR)
                .containsAllEntriesOf(trace());
    }

    /** §6.1 : LndLandZone, 24 colonnes de production. */
    @Test
    void r6_1_lndLandZone_24ColonnesDeProd() {
        assertThat(colonnes("LndLandZone"))
                .hasSize(24)
                .containsEntry("PKey", UNIQUEIDENTIFIER)
                .containsEntry("Identification", NVARCHAR)
                .containsEntry("Area", DECIMAL_6)
                .containsEntry("Cos", DECIMAL_6)
                .containsEntry("Cuf", DECIMAL_6)
                .containsEntry("AreaCos", DECIMAL_6)
                .containsEntry("AreaCuf", DECIMAL_6)
                .containsEntry("SalableArea", DECIMAL_6)
                .containsEntry("CoefSaleableArea", DECIMAL_6)
                .containsEntry("CommerceCoef", DECIMAL_6)
                .containsEntry("CommerceArea", DECIMAL_6)
                .containsEntry("OfficeCoef", DECIMAL_6)
                .containsEntry("OfficeArea", DECIMAL_6)
                .containsEntry("HabitatArea", DECIMAL_6)
                .containsEntry("UndergroundParkingArea", DECIMAL_6)
                .containsEntry("UndergroundParkingCoef", DECIMAL_6)
                .containsEntry("AlinmentStop", ENTIER)
                .containsEntry("MaxHeight", ENTIER)
                .containsEntry("ProspectionId", UNIQUEIDENTIFIER)
                .containsEntry("Notes", NVARCHAR)
                .containsAllEntriesOf(trace());
    }

    // ------------------------------------------------------------------ fichiers de CI 138 / 139

    @Test
    void r6_1_fichier138_creeLndLandEtLndLandZone_idempotent() throws IOException {
        assertThat(TABLES).exists();
        String sql = lire(TABLES);

        assertThat(List.of("LndLand", "LndLandZone")).isNotEmpty().allSatisfy(table -> assertAll(
                () -> assertThat(sql).containsPattern("(?i)CREATE\\s+TABLE\\s+(\\[?dbo]?\\.)?\\[?" + table + "]?\\s*\\("),
                () -> assertThat(sql).containsPattern("(?i)OBJECT_ID\\s*\\(\\s*N?'(\\[?dbo]?\\.)?\\[?" + table + "]?'")));
    }

    @Test
    void r6_1_fichier139_zoneVersProspection_nomDeProd() throws IOException {
        assertThat(FK).exists();
        String sql = lire(FK);

        assertAll(
                () -> assertThat(sql).contains("LndLandZone__Prospection_FK1"),
                () -> assertThat(sql).containsPattern(
                        "(?i)FOREIGN\\s+KEY\\s*\\(\\s*\\[?ProspectionId]?\\s*\\)\\s*REFERENCES\\s+(\\[?dbo]?\\.)?\\[?LndLand]?"
                                + "\\s*\\(\\s*\\[?PKey]?\\s*\\)"),
                () -> assertThat(sql).doesNotContainPattern("(?i)ON\\s+DELETE\\s+CASCADE"),
                () -> assertThat(sql).containsPattern("(?i)(IF\\s+NOT\\s+EXISTS|OBJECT_ID)"));
    }
}

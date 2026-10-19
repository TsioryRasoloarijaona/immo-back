package com.tsix.immocrm.controller.audit;

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
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Schéma de l'audit et des notes — FEATURE-patrimoine-audit-actions.md §6 amendé par §11.1-3 (AuditSchemaTest) :
 * tables présentes avec les types de yesRemsProd, fichiers de CI 100/101, boucle de construction du schéma.
 */
@SpringBootTest
@Transactional
class AuditSchemaTest {

    private static final Path SCHEMA_CI = Path.of("ci", "schema");
    private static final String AUDIT = "AdmWebHistorics";
    private static final String NOTES = "AdmMessage";

    @Autowired private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    private Map<String, String> colonnes(String table) {
        List<Tuple> lignes = entityManager.createNativeQuery("""
                        SELECT COLUMN_NAME AS colonne, DATA_TYPE AS type FROM INFORMATION_SCHEMA.COLUMNS
                        WHERE TABLE_NAME = :table""", Tuple.class)
                .setParameter("table", table)
                .getResultList();
        return lignes.stream().collect(Collectors.toMap(t -> (String) t.get("colonne"), t -> (String) t.get("type")));
    }

    private static String lire(Path fichier) throws IOException {
        return Files.readString(fichier, StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------ tables (présentes dans yesRemsTest et en CI)

    @Test
    void r6_admWebHistorics_colonnesEtTypesDeProd() {
        assertThat(colonnes(AUDIT))
                .containsEntry("PKey", "uniqueidentifier")
                .containsEntry("UserDescription", "nvarchar")
                .containsEntry("ActionDateTime", "datetime")
                .containsEntry("ObjectId", "uniqueidentifier")
                .containsEntry("LockEntityId", "uniqueidentifier")
                .containsEntry("Action", "nvarchar")
                .containsEntry("ObjectType", "nvarchar")
                .containsEntry("ObjectTypeLabel", "nvarchar")
                .containsEntry("ObjectLabel", "nvarchar")
                .containsEntry("Description", "nvarchar")
                .containsEntry("Details", "text")
                .containsEntry("ParentObjectType", "nvarchar")
                .containsEntry("ParentObjectId", "uniqueidentifier")
                .containsEntry("SystemVersion", "varchar")
                .containsEntry("AdmWebSessionsId", "uniqueidentifier");
    }

    @Test
    void r6_admMessage_colonnesEtTypesDeProd() {
        assertThat(colonnes(NOTES))
                .containsEntry("PKey", "uniqueidentifier")
                .containsEntry("DocDate", "datetime")
                .containsEntry("Notes", "nvarchar")
                .containsEntry("CategoryId", "uniqueidentifier")
                .containsEntry("Description", "nvarchar")
                .containsEntry("UserName", "nvarchar")
                .containsEntry("IsIn", "bit")
                .containsEntry("Origin", "nvarchar")
                .containsEntry("ReminderDate", "datetime");
    }

    // ------------------------------------------------------------------ §11.2-3 fichiers de CI

    @Test
    void r11_3_fichier100_creeLesDeuxTables_detailsEnText() throws IOException {
        Path tables = SCHEMA_CI.resolve("100_audit_tables.sql");

        assertThat(tables).exists();
        String sql = lire(tables);
        assertAll(
                () -> assertThat(sql).containsIgnoringCase("CREATE TABLE").contains(AUDIT, NOTES),
                () -> assertThat(sql.toLowerCase(Locale.ROOT)).containsPattern("\\[?details]?\\s+\\[?text\\b"));
    }

    @Test
    void r11_3_fichier101_existe_sansClefVersAdmWebSessions() throws IOException {
        Path fk = SCHEMA_CI.resolve("101_audit_fk.sql");

        assertThat(fk).exists();
        assertThat(lire(fk)).doesNotContainPattern("(?i)REFERENCES\\s+(\\[?dbo]?\\.)?\\[?AdmWebSessions");
    }

    // ------------------------------------------------------------------ §11.1 boucle triée

    @Test
    void r11_1_ciConstruitLeSchemaParUneBoucleSurCiSchema() throws IOException {
        String ci = lire(Path.of(".github", "workflows", "ci.yml"));

        assertAll(
                () -> assertThat(ci).contains("ci/schema/*.sql"),
                () -> assertThat(ci).doesNotContain("080_offres_schema_financement", "010_tables 020_foreign_keys"));
    }
}

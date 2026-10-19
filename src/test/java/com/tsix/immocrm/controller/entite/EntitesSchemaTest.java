package com.tsix.immocrm.controller.entite;

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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Schéma du lot P5 — FEATURE-entites-juridiques.md §7.1 et §7.2 : tables d'actionnaires aux types de yesRemsProd,
 * 13 colonnes de {@code CfgCompany}, fichiers de CI {@code 106_entites_tables.sql} / {@code 107_entites_fk.sql}.
 */
@SpringBootTest
@Transactional
class EntitesSchemaTest {

    private static final Path SCHEMA_CI = Path.of("ci", "schema");
    private static final Path TABLES = SCHEMA_CI.resolve("106_entites_tables.sql");
    private static final Path CLES = SCHEMA_CI.resolve("107_entites_fk.sql");
    private static final String ACTIONNAIRE = "CfgShareholder";
    private static final String LIGNE_GROUPE = "CfgGroupShareholderLine";
    private static final String REPARTITION = "CfgCompanyShareholder";
    private static final String GUID = "uniqueidentifier";
    private static final String TEXTE = "nvarchar";
    private static final String DATE = "datetime";
    private static final String DECIMAL = "decimal";
    private static final String ENTIER = "int";

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

    /** Un bloc par {@code ALTER TABLE} : la clé étrangère cherchée doit tenir dans un seul ordre. */
    private static List<String> ordresAlter(String sql) {
        return Arrays.stream(sql.split("(?i)(?=ALTER\\s+TABLE)")).toList();
    }

    private static Pattern cle(String table, String colonne, String cible) {
        return Pattern.compile("(?is)^ALTER\\s+TABLE\\s+(\\[?dbo]?\\.)?\\[?" + table + "]?\\s.*?FOREIGN\\s+KEY\\s*\\(\\s*\\[?"
                + colonne + "]?\\s*\\)\\s*REFERENCES\\s+(\\[?dbo]?\\.)?\\[?" + cible + "]?\\s*\\(.*");
    }

    private static List<String> ordresDeLaCle(String sql, String table, String colonne, String cible) {
        Pattern motif = cle(table, colonne, cible);
        return ordresAlter(sql).stream().filter(o -> motif.matcher(o.trim()).matches()).toList();
    }

    // ------------------------------------------------------------------ §7.1 tables (yesRemsTest et CI)

    @Test
    void r7_1_cfgShareholder_colonnesEtTypesDeProd() {
        assertThat(colonnes(ACTIONNAIRE))
                .containsEntry("PKey", GUID)
                .containsEntry("Code", TEXTE)
                .containsEntry("Description", TEXTE)
                .containsEntry("Notes", TEXTE)
                .containsEntry("UserCreate", TEXTE)
                .containsEntry("UserCreateDateTime", DATE)
                .containsEntry("UserUpdate", TEXTE)
                .containsEntry("UserUpdateDateTime", DATE);
    }

    @Test
    void r7_1_cfgGroupShareholderLine_colonnesEtTypesDeProd() {
        assertThat(colonnes(LIGNE_GROUPE))
                .containsEntry("PKey", GUID)
                .containsEntry("CfgGroupShareholderId", GUID)
                .containsEntry("CfgShareholderId", GUID)
                .containsEntry("SharingNumber", DECIMAL)
                .containsEntry("UserCreate", TEXTE)
                .containsEntry("UserUpdateDateTime", DATE);
    }

    @Test
    void r7_1_cfgCompanyShareholder_colonnesEtTypesDeProd() {
        assertThat(colonnes(REPARTITION))
                .containsEntry("PKey", GUID)
                .containsEntry("CfgCompanyId", GUID)
                .containsEntry("CfgShareholderId", GUID)
                .containsEntry("SharingNumber", ENTIER)
                .containsEntry("UserCreate", TEXTE)
                .containsEntry("UserUpdateDateTime", DATE);
    }

    @Test
    void r7_2_cfgCompany_lesTreizeColonnesDuLot() {
        assertThat(colonnes("CfgCompany"))
                .containsEntry("FiscalNumber", TEXTE)
                .containsEntry("CommercialRegister", TEXTE)
                .containsEntry("CreationDate", DATE)
                .containsEntry("Capital", DECIMAL)
                .containsEntry("Cnss", TEXTE)
                .containsEntry("Patent", TEXTE)
                .containsEntry("ICE", TEXTE)
                .containsEntry("TotalSharing", ENTIER)
                .containsEntry("SharingNumber", ENTIER)
                .containsEntry("LiquidationDate", DATE)
                .containsEntry("HomePart", DECIMAL)
                .containsEntry("CfgGroupShareholderId", GUID)
                .containsEntry("StlCurrencyId", GUID);
    }

    // ------------------------------------------------------------------ §7.1 fichiers de CI

    @Test
    void r7_1_fichier106_creeLesTroisTablesManquantes_idempotent() throws IOException {
        assertThat(TABLES).exists();
        String sql = lire(TABLES);

        assertAll(
                () -> assertThat(sql).containsPattern("(?i)IF\\s+OBJECT_ID\\s*\\(\\s*'(dbo\\.)?" + ACTIONNAIRE + "'")
                        .containsPattern("(?i)CREATE\\s+TABLE\\s+(\\[?dbo]?\\.)?\\[?" + ACTIONNAIRE + "\\b"),
                () -> assertThat(sql).containsPattern("(?i)IF\\s+OBJECT_ID\\s*\\(\\s*'(dbo\\.)?" + LIGNE_GROUPE + "'")
                        .containsPattern("(?i)CREATE\\s+TABLE\\s+(\\[?dbo]?\\.)?\\[?" + LIGNE_GROUPE + "\\b"),
                () -> assertThat(sql).containsPattern("(?i)IF\\s+OBJECT_ID\\s*\\(\\s*'(dbo\\.)?" + REPARTITION + "'")
                        .containsPattern("(?i)CREATE\\s+TABLE\\s+(\\[?dbo]?\\.)?\\[?" + REPARTITION + "\\b"));
    }

    @Test
    void r7_1_fichier106_neRecreePasLesTablesDejaPresentesEnCi() throws IOException {
        String sql = lire(TABLES);

        assertThat(sql)
                .doesNotContainPattern("(?i)CREATE\\s+TABLE\\s+(\\[?dbo]?\\.)?\\[?CfgGroupShareholder]?\\s*\\(")
                .doesNotContainPattern("(?i)CREATE\\s+TABLE\\s+(\\[?dbo]?\\.)?\\[?(AdmSequence|AdmUserHabilitation|StlCurrency|CfgCompany)]?\\s*\\(");
    }

    @Test
    void r7_1_fichier106_typesDeProd_notesEn500_partsDeGroupeEnDecimal26_6() throws IOException {
        String sql = lire(TABLES).replaceAll("\\s+", " ");

        assertAll(
                () -> assertThat(sql).containsPattern("(?i)\\[?Notes]? \\[?nvarchar]? ?\\( ?500 ?\\)"),
                () -> assertThat(sql).containsPattern("(?i)\\[?SharingNumber]? \\[?decimal]? ?\\( ?26 ?, ?6 ?\\)"),
                () -> assertThat(sql).containsPattern("(?i)\\[?SharingNumber]? \\[?int]?\\b"));
    }

    @Test
    void r7_1_fichier107_lesQuatreClesEtrangeresManquantes() throws IOException {
        assertThat(CLES).exists();
        String sql = lire(CLES);

        assertAll(
                () -> assertThat(ordresDeLaCle(sql, REPARTITION, "CfgCompanyId", "CfgCompany")).isNotEmpty(),
                () -> assertThat(ordresDeLaCle(sql, REPARTITION, "CfgShareholderId", ACTIONNAIRE)).isNotEmpty(),
                () -> assertThat(ordresDeLaCle(sql, LIGNE_GROUPE, "CfgGroupShareholderId", "CfgGroupShareholder"))
                        .isNotEmpty(),
                () -> assertThat(ordresDeLaCle(sql, LIGNE_GROUPE, "CfgShareholderId", ACTIONNAIRE)).isNotEmpty());
    }

    /** Actions de suppression de yesRemsProd : la CI doit refuser ce que la production refuse. */
    @Test
    void r7_1_fichier107_actionsDeSuppressionDeProd() throws IOException {
        String sql = lire(CLES);

        assertAll(
                () -> assertThat(ordresDeLaCle(sql, REPARTITION, "CfgCompanyId", "CfgCompany"))
                        .singleElement().asString().containsPattern("(?i)ON\\s+DELETE\\s+CASCADE"),
                () -> assertThat(ordresDeLaCle(sql, LIGNE_GROUPE, "CfgGroupShareholderId", "CfgGroupShareholder"))
                        .singleElement().asString().containsPattern("(?i)ON\\s+DELETE\\s+CASCADE"),
                () -> assertThat(ordresDeLaCle(sql, LIGNE_GROUPE, "CfgShareholderId", ACTIONNAIRE))
                        .singleElement().asString().containsPattern("(?i)ON\\s+DELETE\\s+CASCADE"),
                () -> assertThat(ordresDeLaCle(sql, REPARTITION, "CfgShareholderId", ACTIONNAIRE))
                        .singleElement().asString().doesNotContainPattern("(?i)ON\\s+DELETE\\s+CASCADE"));
    }

    @Test
    void r7_1_fichier107_idempotent() throws IOException {
        assertThat(lire(CLES)).containsPattern("(?i)(sys\\.foreign_keys|OBJECT_ID\\s*\\()");
    }

    @Test
    void r7_1_readmeDuSchema_decritLes2Fichiers() throws IOException {
        assertThat(lire(SCHEMA_CI.resolve("README.md")))
                .contains("106_entites_tables.sql", "107_entites_fk.sql");
    }
}
